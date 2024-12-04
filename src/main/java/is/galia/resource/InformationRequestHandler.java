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
import is.galia.codec.Decoder;
import is.galia.codec.DecoderConfigurationException;
import is.galia.codec.DecoderFactory;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.StatResult;
import is.galia.codec.SourceFormatException;
import is.galia.source.Source;
import is.galia.source.SourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.NoSuchFileException;
import java.util.Iterator;
import java.util.Optional;

/**
 * <p>High-level information request handler. Use the return value of {@link
 * #builder()} to create new instances.</p>
 *
 * <p>This class provides a simple interface that endpoints can use to retrieve
 * {@link Info image information}, abstracting away much of the tedium of
 * request setup.</p>
 */
public class InformationRequestHandler extends AbstractRequestHandler {

    /**
     * Builds {@link InformationRequestHandler} instances.
     */
    public static final class Builder {

        private final InformationRequestHandler handler =
                new InformationRequestHandler();

        /**
         * @param callback Callback to receive events during request handling.
         */
        public Builder withCallback(InformationRequestHandler.Callback callback) {
            handler.callback = callback;
            return this;
        }

        /**
         * @param delegate Delegate. If set to a non-{@code null} value, a
         *                 {@link #withRequestContext(RequestContext) request
         *                 context must also be set}.
         */
        public Builder withDelegate(Delegate delegate) {
            handler.delegate = delegate;
            return this;
        }

        public Builder withIdentifier(Identifier identifier) {
            handler.identifier = identifier;
            return this;
        }

        public Builder withReference(Reference reference) {
            handler.reference = reference;
            return this;
        }

        /**
         * @param requestContext Request context. If set to a non-{@code null}
         *                       value, a {@link #withDelegate(Delegate)
         *                       delegate must also be set}.
         */
        public Builder withRequestContext(RequestContext requestContext) {
            handler.requestContext = requestContext;
            return this;
        }

        /**
         * @return New instance.
         * @throws IllegalArgumentException if any of the required builder
         *         methods have not been called.
         */
        public InformationRequestHandler build() {
            if (handler.reference == null) {
                throw new NullPointerException("Reference cannot be null.");
            } else if (handler.identifier == null) {
                throw new NullPointerException("Identifier cannot be null.");
            } else if (handler.requestContext == null) {
                throw new NullPointerException("Request context cannot be null.");
            }
            return handler;
        }

    }

    /**
     * <p>Callback for various events that occur during a call to {@link
     * #handle()}.</p>
     *
     * <p>Any exceptions thrown from these methods will bubble up through
     * {@link #handle()}. This feature can be used to support e.g. a {@link
     * ResourceException} with a custom status depending on the authorization
     * result.</p>
     */
    public interface Callback {

        /**
         * <p>Performs authorization using an {@link Authorizer}. The source
         * image has not been accessed yet, so no information about it is
         * available.</p>
         *
         * <p>This is the first callback to get called.</p>
         *
         * @return Authorization result.
         */
        boolean authorizeBeforeAccess() throws Exception;

        /**
         * <p>Performs authorization using an {@link Authorizer} <em>after</em>
         * the image has been accessed, so information about it is
         * available.</p>
         *
         * @return Authorization result.
         */
        boolean authorize() throws Exception;

        /**
         * Called immediately after a source image has been accessed. (In the
         * case of cached instances, when {@link
         * Key#CACHE_SERVER_RESOLVE_FIRST} is set to {@code false}, it may
         * never be&mdash;in that case, see {@link
         * #cacheAccessed(StatResult)}.)
         *
         * @param result Information about the source image.
         */
        void sourceAccessed(StatResult result);

        /**
         * Called immediately after a cached instance has been accessed.
         *
         * @param result Information about the cached instance.
         */
        void cacheAccessed(StatResult result);

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InformationRequestHandler.class);

    /** No-op callback to avoid having to check for one. */
    private InformationRequestHandler.Callback callback = new Callback() {
        @Override
        public boolean authorizeBeforeAccess() { return true; }
        @Override
        public boolean authorize() { return true; }
        @Override
        public void sourceAccessed(StatResult sourceAvailable) {}
        @Override
        public void cacheAccessed(StatResult sourceAvailable) {}
    };

    private Identifier identifier;
    private boolean haveInformedCallbackOfSourceAccessed, isSourceStattedYet;

    public static Builder builder() {
        return new Builder();
    }

    protected InformationRequestHandler() {}

    @Override
    Logger getLogger() {
        return LOGGER;
    }

    /**
     * Handles an information request.
     */
    public Info handle() throws Exception {
        if (!callback.authorizeBeforeAccess()) {
            return null;
        }

        final CacheFacade cacheFacade = new CacheFacade();
        final Source source           = SourceFactory.newSource(identifier, delegate);

        if (isResolvingFirst()) {
            stat(source);
        }

        // If we are using a cache, and the cache contains an info matching the
        // request, skip all the setup and just return the cached info.
        if (!isBypassingCache() && !isBypassingCacheRead()) {
            try {
                Optional<Info> optInfo = cacheFacade.fetchInfo(identifier);
                if (optInfo.isPresent()) {
                    Info info = optInfo.get();
                    StatResult statResult = new StatResult();
                    statResult.setLastModified(info.getSerializationTimestamp());
                    callback.cacheAccessed(statResult);
                    setRequestContextKeys(info);
                    if (!callback.authorize()) {
                        return null;
                    }
                    return info;
                }
            } catch (IOException e) {
                // Don't rethrow -- it's still possible to service the request.
                LOGGER.error(e.getMessage());
            }
        }

        if (!isSourceStattedYet) {
            stat(source);
        }

        // Get the format of the source image.
        Iterator<Format> formatIterator = source.getFormatIterator();

        while (formatIterator.hasNext()) {
            final Format format = formatIterator.next();
            // Obtain an instance of the decoder that handles this format.
            ImageInputStream inputStream = null;
            Arena arena = Arena.ofConfined();
            try (Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
                if (source.supportsFileAccess()) {
                    decoder.setSource(source.getFile());
                } else {
                    inputStream = source.newInputStream();
                    decoder.setSource(inputStream);
                }
                StatResult statResult = new StatResult();
                Info info = getOrReadInfo(identifier, format, decoder, statResult);
                informCallbackOfSourceAccessedOnce(statResult);
                setRequestContextKeys(info);
                if (!callback.authorize()) {
                    return null;
                }
                return info;
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
                arena.close();
                if (inputStream != null) {
                    inputStream.close();
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

    private void setRequestContextKeys(Info info) {
        requestContext.setFullSize(info.getSize());
        requestContext.setPageCount(info.getNumPages());
        requestContext.setMetadata(info.getMetadata());
    }

    private void stat(Source source) throws IOException {
        try {
            StatResult statResult = source.stat();
            informCallbackOfSourceAccessedOnce(statResult);
            isSourceStattedYet    = true;
        } catch (NoSuchFileException e) { // this needs to be rethrown!
            if (Configuration.forApplication()
                    .getBoolean(Key.CACHE_SERVER_EVICT_MISSING, false)) {
                new CacheFacade().evictAsync(identifier);
            }
            throw e;
        }
    }

}
