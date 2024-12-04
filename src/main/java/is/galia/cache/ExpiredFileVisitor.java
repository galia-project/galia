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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Used by {@link Files#walkFileTree} to delete all expired files within
 * a directory.
 */
class ExpiredFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ExpiredFileVisitor.class);

    private long deletedFileCount = 0;
    private long deletedFileSize = 0;

    long getDeletedFileCount() {
        return deletedFileCount;
    }

    long getDeletedFileSize() {
        return deletedFileSize;
    }

    @Override
    public FileVisitResult visitFile(Path path,
                                     BasicFileAttributes attrs) {
        try {
            final boolean delete =
                    (Files.isRegularFile(path) && FilesystemCache.isExpired(path));

            LOGGER.trace("{}: [last accessed: {}] [last modified: {}] " +
                            "[effective last accessed: {}] [delete? {}]",
                    path,
                    Files.getAttribute(path, "lastAccessTime"),
                    Files.getLastModifiedTime(path),
                    FilesystemCache.getLastAccessedTime(path),
                    delete);

            if (delete) {
                final long size = Files.size(path);
                Files.deleteIfExists(path);
                deletedFileCount++;
                deletedFileSize += size;
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        LOGGER.warn("visitFileFailed(): {}", e.getMessage());
        return FileVisitResult.CONTINUE;
    }

}
