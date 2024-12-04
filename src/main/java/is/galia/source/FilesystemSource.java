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

package is.galia.source;

import is.galia.codec.FormatDetector;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.DelegateException;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.StatResult;
import is.galia.stream.PathImageInputStream;
import is.galia.util.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Provides access to source content located on a locally attached
 * filesystem. Identifiers are mapped to filesystem paths.</p>
 *
 * <h1>Format Determination</h1>
 *
 * <p>See {@link FormatIterator}.</p>
 *
 * <h1>Lookup Strategies</h1>
 *
 * <p>Two distinct lookup strategies are supported, defined by
 * {@link Key#FILESYSTEMSOURCE_LOOKUP_STRATEGY}. BasicLookupStrategy locates
 * images by concatenating a pre-defined path prefix and/or suffix.
 * DelegateLookupStrategy invokes a delegate method to retrieve a pathname
 * dynamically.</p>
 */
class FilesystemSource extends AbstractSource implements Source {

    /**
     * <ol>
     *     <li>If the file's filename contains an extension, the format is
     *     inferred from that.</li>
     *     <li>If unsuccessful, and the identifier contains an extension, the
     *     format is inferred from that.</li>
     *     <li>If unsuccessful, the format is inferred from the file's magic
     *     bytes.</li>
     * </ol>
     */
    class FormatIterator<T> implements Iterator<T> {

        /**
         * Infers a {@link Format} based on image magic bytes.
         */
        private class ByteChecker implements FormatChecker {
            @Override
            public Format check() throws IOException {
                return FormatDetector.detect(getFile());
            }
        }

        private FormatChecker formatChecker;

        @Override
        public boolean hasNext() {
            return (formatChecker == null ||
                    formatChecker instanceof NameFormatChecker ||
                    formatChecker instanceof IdentifierFormatChecker);
        }

        @Override
        public T next() {
            if (formatChecker == null) {
                try {
                    formatChecker = new NameFormatChecker(getFile().getFileName().toString());
                } catch (IOException e) {
                    LOGGER.warn("FormatIterator.next(): {}", e.getMessage(), e);
                    formatChecker = new NameFormatChecker("***BOGUS***");
                    return next();
                }
            } else if (formatChecker instanceof NameFormatChecker) {
                formatChecker = new IdentifierFormatChecker(getIdentifier());
            } else if (formatChecker instanceof IdentifierFormatChecker) {
                formatChecker = new ByteChecker();
            } else {
                throw new NoSuchElementException();
            }
            try {
                //noinspection unchecked
                return (T) formatChecker.check();
            } catch (NoSuchFileException e) {
                LOGGER.warn("File does not exist: {}", e.getMessage());
                //noinspection unchecked
                return (T) Format.UNKNOWN;
            } catch (IOException e) {
                LOGGER.warn("Error checking format: {}", e.getMessage());
                //noinspection unchecked
                return (T) Format.UNKNOWN;
            }
        }
    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FilesystemSource.class);

    private static final String UNIX_PATH_SEPARATOR    = "/";
    private static final String WINDOWS_PATH_SEPARATOR = "\\";

    private FormatIterator<Format> formatIterator = new FormatIterator<>();

    /**
     * Lazy-loaded by {@link #getFile}.
     */
    private Path path;

    @Override
    public StatResult stat() throws IOException {
        final Path file = getFile();
        if (!Files.exists(file)) {
            throw new NoSuchFileException("Failed to resolve " +
                    identifier + " to " + file);
        } else if (!Files.isReadable(file)) {
            throw new AccessDeniedException("File is not readable: " + file);
        }
        StatResult result = new StatResult();
        result.setLastModified(Files.getLastModifiedTime(file).toInstant());
        return result;
    }

    /**
     * @return Path corresponding to the given identifier according to the
     *         current lookup strategy
     *         ({@link Key#FILESYSTEMSOURCE_LOOKUP_STRATEGY}). The result is
     *         cached.
     */
    @Override
    public Path getFile() throws IOException {
        if (path == null) {
            final LookupStrategy strategy =
                    LookupStrategy.from(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY);
            //noinspection SwitchStatementWithTooFewBranches
            switch (strategy) {
                case DELEGATE_SCRIPT:
                    try {
                        path = getPathWithDelegateStrategy();
                    } catch (DelegateException e) {
                        LOGGER.error(e.getMessage(), e);
                        throw new IOException(e);
                    }
                    break;
                default:
                    path = getPathWithBasicStrategy();
                    break;
            }
            LOGGER.debug("Resolved {} to {}", identifier, path);
        }
        return path;
    }

    @Override
    public FormatIterator<Format> getFormatIterator() {
        return formatIterator;
    }

    private Path getPathWithBasicStrategy() {
        final Configuration config = Configuration.forApplication();
        final String prefix =
                config.getString(Key.FILESYSTEMSOURCE_PATH_PREFIX, "");
        final String suffix =
                config.getString(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");
        final Identifier sanitizedId = sanitizedIdentifier();
        return Paths.get(prefix + sanitizedId + suffix);
    }

    /**
     * @return Pathname of the file corresponding to the identifier passed to
     *         {@link #setIdentifier(Identifier)}.
     * @throws NoSuchFileException if the delegate method indicated that there
     *                             is no file corresponding to the given
     *                             identifier.
     * @throws DelegateException if the method invocation failed.
     */
    private Path getPathWithDelegateStrategy() throws NoSuchFileException,
            DelegateException {
        String pathname = getDelegate().getFilesystemSourcePathname();
        if (pathname == null) {
            throw new NoSuchFileException(
                    "Delegate returned null for " + identifier);
        }
        return Paths.get(pathname);
    }

    @Override
    public ImageInputStream newInputStream() throws IOException {
        return new PathImageInputStream(getFile());
    }

    /**
     * Recursively filters out {@literal fileseparator..} and
     * {@literal ..fileseparator} to prevent moving up a directory tree.
     *
     * @return Sanitized identifier.
     */
    private Identifier sanitizedIdentifier() {
        String idStr = identifier.toString();
        if (SystemUtils.IS_OS_WINDOWS) {
            idStr = idStr.replace("/", "\\");
        } else {
            idStr = idStr.replace("\\", "/");
        }
        final String sanitized = StringUtils.sanitize(
                idStr,
                UNIX_PATH_SEPARATOR + "..",
                ".." + UNIX_PATH_SEPARATOR,
                WINDOWS_PATH_SEPARATOR + "..",
                ".." + WINDOWS_PATH_SEPARATOR);
        return new Identifier(sanitized);
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        super.setIdentifier(identifier);
        reset();
    }

    private void reset() {
        path           = null;
        formatIterator = new FormatIterator<>();
    }

    @Override
    public boolean supportsFileAccess() {
        return true;
    }

}
