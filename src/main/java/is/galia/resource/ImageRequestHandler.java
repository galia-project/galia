/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.galia.resource;

import is.galia.auth.Authorizer;
import is.galia.cache.CacheFacade;
import is.galia.cache.VariantCache;
import is.galia.codec.Decoder;
import is.galia.codec.DecoderConfigurationException;
import is.galia.codec.DecoderFactory;
import is.galia.codec.Encoder;
import is.galia.codec.EncoderFactory;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.StatResult;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.processor.Processor;
import is.galia.codec.SourceFormatException;
import is.galia.processor.ProcessorFactory;
import is.galia.status.HealthChecker;
import is.galia.source.Source;
import is.galia.source.SourceFactory;
import is.galia.stream.CompletableOutputStream;
import is.galia.stream.TeeOutputStream;
import is.galia.util.IOUtils;
import is.galia.util.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

/**
 * <p>High-level image request handler. Use the return value of {@link
 * #builder()} to create new instances.</p>
 *
 * <p>This class provides a simple interface that image-serving endpoints can
 * use to convert client arguments into images. Simplicity is achieved by
 * abstracting away as much of the tedium of image request handling (caching,
 * format detection, connecting {@link Source sources} to {@link Processor
 * processors}, etc.) as possible. There is also no coupling to any particular
 * image-serving protocol.</p>
 */
public class ImageRequestHandler extends AbstractRequestHandler {

    /**
     * Builds {@link ImageRequestHandler} instances.
     */
    public static final class Builder {

        private final ImageRequestHandler handler = new ImageRequestHandler();

        /**
         * @param callback Callback to receive events during request handling.
         */
        public Builder withCallback(Callback callback) {
            handler.callback = callback;
            return this;
        }

        public Builder withDelegate(Delegate delegate) {
            handler.delegate = delegate;
            return this;
        }

        public Builder withOperationList(OperationList opList) {
            handler.operationList = opList;
            return this;
        }

        public Builder withReference(Reference reference) {
            handler.reference = reference;
            return this;
        }

        public Builder withRequestContext(RequestContext requestContext) {
            handler.requestContext = requestContext;
            return this;
        }

        /**
         * @return New instance.
         * @throws NullPointerException if any of the required builder methods
         *                              have not been called.
         */
        public ImageRequestHandler build() {
            if (handler.reference == null) {
                throw new NullPointerException("Reference cannot be null.");
            } else if (handler.operationList == null) {
                throw new NullPointerException("Operation list cannot be null.");
            } else if (handler.requestContext == null) {
                throw new NullPointerException("Request context cannot be null.");
            }
            return handler;
        }

    }

    /**
     * Callback for various events that occur during a call to {@link
     * ImageRequestHandler#handle(Response)}.
     */
    public interface Callback {

        /**
         * <p>Performs pre-authorization using an {@link Authorizer}.</p>
         *
         * <p>{@link #willProcessImage(Info)} has not yet been called.</p>
         *
         * @return Authorization result.
         */
        boolean authorizeBeforeAccess() throws Exception;

        /**
         * <p>Performs authorization using an {@link Authorizer}.</p>
         *
         * <p>{@link #willProcessImage(Info)} has not yet been called.</p>
         *
         * @return Authorization result.
         */
        boolean authorize() throws Exception;

        /**
         * <p>Called immediately after the source image has first been accessed
         * in the request cycle. (In the case of cached variant images, when
         * {@link Key#CACHE_SERVER_RESOLVE_FIRST} is set to {@code false}, it
         * may never be&mdash;in that case, see {@link
         * #willStreamImageFromVariantCache(StatResult)}.)</p>
         *
         * @param result Information about the source or cached variant image.
         */
        void sourceAccessed(StatResult result);

        /**
         * Called when image information is available; always before {@link
         * #willProcessImage(Info)} and {@link
         * #willStreamImageFromVariantCache(StatResult)}.
         *
         * <p>The {@link Builder#withOperationList(OperationList) operatlon
         * list} can still be modified at this point.</p>
         *
         * @param info Efficiently obtained instance.
         */
        void infoAvailable(Info info) throws Exception;

        /**
         * <p>Called when a hit is found in the variant cache. In this case, no
         * further processing will be necessary and the streaming will begin
         * very soon after this method returns.</p>
         *
         * <p>If a hit is not found in the variant cache, this method is not
         * called.</p>
         *
         * <p>This method tends to be called relatively early. No other
         * callback methods will be called after this one.</p>
         */
        void willStreamImageFromVariantCache(StatResult result)
                throws Exception;

        /**
         * <p>All setup is complete and processing will begin very soon after
         * this method returns.</p>
         *
         * <p>The {@link Builder#withOperationList(OperationList) operatlon
         * list} cannot be modified.</p>
         *
         * <p>This method tends to be called last.</p>
         *
         * @param info Efficiently obtained instance.
         */
        void willProcessImage(Info info) throws Exception;

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImageRequestHandler.class);

    // No-op callback to avoid having to check for one.
    private Callback callback = new Callback() {
        @Override
        public boolean authorizeBeforeAccess() {
            return true;
        }
        @Override
        public boolean authorize() {
            return true;
        }
        @Override
        public void sourceAccessed(StatResult result) {
        }
        @Override
        public void willStreamImageFromVariantCache(StatResult result) {
        }
        @Override
        public void infoAvailable(Info info) {
        }
        @Override
        public void willProcessImage(Info info) {
        }
    };
    private Info info;
    private OperationList operationList;
    private boolean haveInformedCallbackOfSourceAccessed, isSourceStattedYet;
    // One or the other of these will get set.
    private Path sourceFile;
    private ImageInputStream sourceStream;
    private Processor processor;

    public static Builder builder() {
        return new Builder();
    }

    protected ImageRequestHandler() {}

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * Handles an image request.
     *
     * @param response Response to write the resulting image to.
     */
    public void handle(Response response) throws Exception {
        requestContext.setOperationList(operationList);
        requestContext.setOutputFormat(operationList.getOutputFormat());

        if (!callback.authorizeBeforeAccess()) {
            return;
        }

        final Identifier identifier       = operationList.getIdentifier();
        final CacheFacade cacheFacade     = new CacheFacade();
        Iterator<Format> formatIterator   = Collections.emptyIterator();
        final Source source               = SourceFactory.newSource(identifier, delegate);
        final boolean sourceSupportsFiles = source.supportsFileAccess();
        boolean isFormatKnownYet          = false;

        if (isResolvingFirst()) {
            stat(source);
        }

        // If we are using a cache:
        // 1. If the cache contains an image matching the request, skip all the
        //    setup and just return the cached image.
        // 2. Otherwise, if the cache contains a relevant info, fetch it now to
        //    avoid having to fetch it from a source later.
        if (!isBypassingCache() && !isBypassingCacheRead()) {
            final Optional<Info> optInfo = cacheFacade.fetchInfo(identifier);
            if (optInfo.isPresent()) {
                info = optInfo.get();
                operationList.applyNonEndpointMutations(info, delegate);
                callback.infoAvailable(info);

                InputStream cacheStream = null;
                StatResult statResult   = new StatResult();
                try {
                    cacheStream = cacheFacade.newVariantImageInputStream(
                            operationList, statResult);
                } catch (IOException e) {
                    // Don't rethrow--it's still possible to service the
                    // request.
                    LOGGER.error(e.getMessage());
                }

                if (cacheStream != null) {
                    informCallbackOfSourceAccessedOnce(statResult);
                    callback.willStreamImageFromVariantCache(statResult);
                    try (OutputStream os = response.openBodyStream()) {
                        new InputStreamRepresentation(cacheStream).write(os);
                    }
                    return;
                } else {
                    Format infoFormat = info.getSourceFormat();
                    if (infoFormat != null) {
                        formatIterator = Collections.singletonList(infoFormat).iterator();
                        isFormatKnownYet = true;
                    }
                }
            }
        }

        if (!isSourceStattedYet) {
            stat(source);
        }
        if (!isFormatKnownYet) {
            formatIterator = source.getFormatIterator();
        }

        while (formatIterator.hasNext()) {
            final Format format = formatIterator.next();
            final Encode encode = (Encode) operationList.getFirst(Encode.class);
            try (Arena arena = Arena.ofConfined();
                 Decoder decoder = DecoderFactory.newDecoder(format, arena);
                 Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
                if (sourceSupportsFiles) {
                    sourceFile = source.getFile();
                    decoder.setSource(sourceFile);
                } else {
                    sourceStream = source.newInputStream();
                    decoder.setSource(sourceStream);
                }
                final StatResult statResult = new StatResult();
                info = getOrReadInfo(
                        operationList.getIdentifier(), format, decoder, statResult);
                informCallbackOfSourceAccessedOnce(statResult);

                Size fullSize;
                try {
                    callback.infoAvailable(info);
                    fullSize = info.getSize(operationList.getPageIndex());
                    requestContext.setMetadata(info.getMetadata());
                    operationList.applyNonEndpointMutations(info, delegate);
                    operationList.freeze();
                    requestContext.setFullSize(fullSize);
                    requestContext.setResultingSize(operationList.getResultingSize(fullSize));
                    requestContext.setPageCount(info.getNumPages());
                    if (!callback.authorize()) {
                        return;
                    }
                    operationList.validate(fullSize, format);
                    callback.willProcessImage(info);
                } catch (IllegalArgumentException |
                         IndexOutOfBoundsException e) {
                    throw new IllegalClientArgumentException(e);
                }

                processor = ProcessorFactory.newProcessor();
                processor.setArena(arena);
                processor.setDecoder(decoder);
                processor.setEncoder(encoder);

                try (OutputStream outputStream = response.openBodyStream()) {
                    copyOrProcess(outputStream);
                }

                // Notify the health checker of a successful response.
                HealthChecker.addSourceUsage(source);
                return;
            } catch (SourceFormatException e) {
                String message = "The format of {} inferred by {} ({}) is incorrect";
                if (formatIterator.hasNext()) {
                    message += "; trying again";
                } else {
                    message += "; out of options";
                }
                LOGGER.debug(message,
                        identifier, source.getClass().getSimpleName(), format);
            } catch (DecoderConfigurationException e) {
                throw new ResourceException(
                        Status.INTERNAL_SERVER_ERROR, e.getMessage());
            } finally {
                if (sourceStream != null) {
                    sourceStream.close();
                }
            }
        }
        throw new SourceFormatException();
    }

    private void informCallbackOfSourceAccessedOnce(StatResult statResult) {
        if (!haveInformedCallbackOfSourceAccessed) {
            callback.sourceAccessed(statResult);
            haveInformedCallbackOfSourceAccessed = true;
        }
    }

    private void stat(Source source) throws IOException {
        try {
            StatResult statResult = source.stat();
            informCallbackOfSourceAccessedOnce(statResult);
            isSourceStattedYet    = true;
        } catch (NoSuchFileException e) { // this needs to be rethrown!
            if (Configuration.forApplication()
                    .getBoolean(Key.CACHE_SERVER_EVICT_MISSING, false)) {
                new CacheFacade().evictAsync(operationList.getIdentifier());
            }
            throw e;
        }
    }

    /**
     * Writes the image requested in the constructor to the given output
     * stream, either retrieving it from the variant cache, or getting it from
     * a processor (and caching it if so configured) as appropriate.
     */
    private void copyOrProcess(OutputStream responseOS) throws IOException {
        // If we are bypassing the cache, write directly to the response.
        if (isBypassingCache()) {
            LOGGER.debug("Bypassing the cache and writing only to the response");
            doCopyOrProcess(responseOS);
            return;
        }

        // If no variant cache is available, write directly to the response.
        final CacheFacade cacheFacade = new CacheFacade();
        if (!cacheFacade.isVariantCacheEnabled()) {
            LOGGER.debug("Variant cache not available; writing directly " +
                    "to the response");
            doCopyOrProcess(responseOS);
            return;
        }

        // A variant cache is available. If we aren't bypassing cache reads,
        // try to copy the image from it to the response.
        if (!isBypassingCacheRead()) {
            final Optional<VariantCache> optCache = cacheFacade.getVariantCache();
            if (optCache.isPresent()) {
                VariantCache cache = optCache.get();
                try (InputStream cacheIS = cache.newVariantImageInputStream(operationList)) {
                    if (cacheIS != null) {
                        // The image is available, so write it to the response.
                        final Stopwatch watch = new Stopwatch();
                        cacheIS.transferTo(responseOS);
                        LOGGER.trace("Streamed from {} in {}: {}",
                                cache.getClass().getSimpleName(), watch, operationList);
                        return;
                    }
                } catch (IOException e) {
                    // Don't rethrow--it may still be possible to fulfill the
                    // request.
                    LOGGER.debug("Error while streaming from {}: {}",
                            cache.getClass().getSimpleName(),
                            e.getMessage());
                    doCopyOrProcess(responseOS);
                    return;
                }
            }
        }

        // At this point, a variant cache may be available, but it doesn't
        // contain an image that can fulfill the request. So, we will create a
        // TeeOutputStream to write to the response output stream and the cache
        // pseudo-simultaneously.
        try (CompletableOutputStream cacheOS =
                     cacheFacade.newVariantImageOutputStream(operationList)) {
            if (cacheOS != null) {
                OutputStream teeOS = new TeeOutputStream(responseOS, cacheOS);
                LOGGER.debug("Writing to the response & variant cache " +
                        "simultaneously");
                doCopyOrProcess(teeOS);
                cacheOS.flush();
                cacheOS.complete();
            } else {
                doCopyOrProcess(responseOS);
            }
        } catch (IOException e) {
            LOGGER.debug("copyOrProcess(): {}", e.getMessage(), e);
        } catch (Throwable t) {
            LOGGER.error("copyOrProcess(): {}", t.getMessage(), t);
            throw t;
        }
    }

    /**
     * If {@link #operationList} {@link OperationList#hasEffect(Size, Format)
     * has no effect}, streams the image from its source. Otherwise, invokes
     * {@link Processor#process}.
     *
     * @param responseOS Either the response output stream, or a tee stream
     *                   that writes to the response and cache pseudo-
     *                   simultaneously.
     */
    private void doCopyOrProcess(OutputStream responseOS) throws IOException {
        // If the operations are effectively a no-op, the source image can be
        // streamed through with no processing.
        if (!operationList.hasEffect(info.getSize(), info.getSourceFormat())) {
            copy(responseOS);
        } else {
            process(responseOS);
        }
    }

    /**
     * Copies a source resource directly to a given {@link OutputStream} with
     * no processing or caching.
     */
    private void copy(OutputStream responseOS) throws IOException {
        final Stopwatch watch = new Stopwatch();
        // If the copy fails here for any reason (source image doesn't exist,
        // isn't readable, etc.), responseOS will be incompletely written, and
        // it won't be possible to send an error page.
        if (sourceFile != null) {
            Files.copy(sourceFile, responseOS);
        } else {
            IOUtils.transfer(sourceStream, responseOS);
        }
        LOGGER.trace("Streamed with no processing in {}: {}",
                watch, operationList);
    }

    private void process(OutputStream outputStream) throws IOException {
        final Stopwatch watch = new Stopwatch();

        processor.process(operationList, outputStream);

        LOGGER.trace("{} processed in {}: {}",
                processor.getClass().getSimpleName(), watch, operationList);
    }

}
