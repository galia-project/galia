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

package is.galia.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Used by {@link Files#walkFileTree} to delete all stale temporary and
 * zero-byte files within a directory.
 */
class DetritalFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DetritalFileVisitor.class);

    private long deletedFileCount = 0;
    private long deletedFileSize = 0;
    private final PathMatcher matcher;
    private final long minCleanableAge;

    DetritalFileVisitor(long minCleanableAge, String tempExtension) {
        this.minCleanableAge = minCleanableAge;
        matcher = FileSystems.getDefault().
                getPathMatcher("glob:*" + tempExtension);
    }

    private void delete(Path path) {
        try {
            final long size = Files.size(path);
            Files.deleteIfExists(path);
            deletedFileCount++;
            deletedFileSize += size;
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    long getDeletedFileCount() {
        return deletedFileCount;
    }

    long getDeletedFileSize() {
        return deletedFileSize;
    }

    private void test(Path path) {
        try {
            // Try to avoid matching temp files that may still be open for
            // writing by assuming that files last modified long enough ago
            // are closed.
            if (System.currentTimeMillis()
                    - Files.getLastModifiedTime(path).toMillis() > minCleanableAge) {
                // Delete temp files.
                if (matcher.matches(path.getFileName())) {
                    delete(path);
                } else {
                    // Delete zero-byte files.
                    if (Files.size(path) == 0) {
                        delete(path);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attrs) {
        test(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        LOGGER.warn("visitFileFailed(): {}", e.getMessage());
        return FileVisitResult.CONTINUE;
    }

}
