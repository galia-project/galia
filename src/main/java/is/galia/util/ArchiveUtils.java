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
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ArchiveUtils {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ArchiveUtils.class);

    /**
     * Lists all entries in the given zip file without extracting it.
     *
     * @param file Zip file.
     * @return All entries in the given file.
     */
    public static List<ZipEntry> entries(Path file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            final List<ZipEntry> entries = new ArrayList<>();
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                entries.add(zipEntries.nextElement());
            }
            return entries;
        }
    }

    /**
     * Unzips a zip file. Permissions are preserved on platforms that support
     * it.
     *
     * @param zipFile File to unzip.
     * @param destDir Directory to unzip the file into.
     */
    public static void unzip(Path zipFile, Path destDir) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(zipFile)) {
            try (Stream<Path> zipEntries = Files.walk(fs.getPath("/"))) {
                for (Path zipPath : zipEntries.toList()) {
                    if ("/".equals(zipPath.toString())) {
                        continue;
                    }
                    Path newPath = confine(zipPath, destDir);
                    if (Files.isDirectory(zipPath)) {
                        Files.createDirectories(newPath);
                    } else {
                        // Create the parent directory if it doesn't exist
                        if (newPath.getParent() != null &&
                                Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                        // Copy the file to its destination
                        Files.copy(zipPath, newPath,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    }
                    // Copy permissions (not supported in Windows)
                    try {
                        @SuppressWarnings("unchecked")
                        Set<PosixFilePermission> perms =
                                (Set<PosixFilePermission>) Files.getAttribute(zipPath, "zip:permissions");
                        Files.setPosixFilePermissions(newPath, perms);
                    } catch (UnsupportedOperationException e) {
                        LOGGER.warn("unzip(): failed to preserve permissions for {}",
                                zipPath);
                    }
                }
            }
        }
    }

    /**
     * Prevents a "zip slip" whereby an entry in the zip inventory points to a
     * file outside the target directory.
     *
     * @throws IOException if the entry contains an unsafe target.
     */
    private static Path confine(Path zipEntry,
                                Path targetDir) throws IOException {
        Path zipPath = Path.of(targetDir.toString(), zipEntry.toString());
        if (!zipPath.startsWith(targetDir)) {
            throw new IOException("Unsafe zip entry: " + zipEntry);
        }
        return zipPath;
    }

    private ArchiveUtils() {}

}
