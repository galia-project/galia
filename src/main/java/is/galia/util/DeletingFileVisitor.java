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

package is.galia.util;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

/**
 * Enables {@link Files#walkFileTree(Path, FileVisitor)} to recursively delete
 * a directory's contents.
 */
public class DeletingFileVisitor extends SimpleFileVisitor<Path> {

    private Logger logger;
    private long deletedFileCount = 0;
    private long deletedFileSize = 0;
    private final Set<Path> excludedPaths = new HashSet<>();

    /**
     * @return Total number of deleted files.
     */
    public long getDeletedFileCount() {
        return deletedFileCount;
    }

    /**
     * @return Total byte size of all deleted files.
     */
    public long getDeletedFileSize() {
        return deletedFileSize;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * @param path Path to exclude from the deletion.
     */
    public void addExclude(Path path) {
        excludedPaths.add(path);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
            throws IOException {
        if (attributes.isRegularFile() && !excludedPaths.contains(file)) {
            if (logger != null) {
                logger.debug("Deleting file: {}", file);
            }
            final long size = Files.size(file);
            Files.delete(file);
            deletedFileSize += size;
            deletedFileCount++;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e)
            throws IOException {
        if (!excludedPaths.contains(dir)) {
            if (logger != null) {
                logger.debug("Deleting {}", dir);
            }
            Files.delete(dir);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) {
        if (logger != null) {
            logger.warn("Failed to delete file: {}", e.getMessage());
        }
        return FileVisitResult.CONTINUE;
    }

}