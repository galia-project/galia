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

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileUtils {

    /**
     * @param path Path of the file to check.
     * @return Whether the file at the given path exists, is a file, and is
     *         readable.
     * @throws AccessDeniedException if the file is not readable. The message
     *         is user-friendly.
     * @throws NoSuchFileException if the file does not exist. The message is
     *         user-friendly.
     * @throws IOException if the file does not exist, is not a file, or is not
     *         readable. The message is user-friendly.
     */
    public static boolean checkReadableFile(Path path) throws IOException {
        if (path.toString().isBlank()) {
            throw new NoSuchFileException("No pathname supplied");
        } else if (!Files.exists(path)) {
            throw new NoSuchFileException("File does not exist: " + path);
        } else if (!Files.isRegularFile(path)) {
            throw new IOException("Not a regular file: " + path);
        } else if (!Files.isReadable(path)) {
            throw new AccessDeniedException("Not readable: " + path);
        }
        return true;
    }

    /**
     * @param path Path of the directory to check.
     * @return Whether the directory at the given path exists, is a file, and
     *         is writable.
     *
     * @throws AccessDeniedException if the directory is not writable. The
     *         message is user-friendly.
     * @throws NoSuchFileException if the directory does not exist. The message
     *         is user-friendly.
     * @throws IOException if the file is not a directory. The message is
     *         user-friendly.
     */
    public static boolean checkWritableDirectory(Path path) throws IOException {
        if (path.toString().isBlank()) {
            throw new NoSuchFileException("No pathname supplied");
        } else if (!Files.exists(path)) {
            throw new NoSuchFileException("Directory does not exist: " + path);
        } else if (!Files.isDirectory(path)) {
            throw new IOException("Not a directory: " + path);
        } else if (!Files.isWritable(path)) {
            throw new AccessDeniedException("Not writable: " + path);
        }
        return true;
    }

    /**
     * Expands filesystem paths that start with a tilde ({@literal ~}).
     *
     * @param path Path to expand.
     * @return Expanded path.
     */
    public static String expandPath(String path) {
        String home = System.getProperty("user.home");
        home = home.replace("\\", "\\\\");
        return path.replaceFirst("^~", home);
    }

    /**
     * <p>Tries to resolve the location of the file with the given pathname.
     * Existence of the underlying file is not checked.</p>
     *
     * <ol>
     *     <li>If the pathname is absolute, it is returned as-is.</li>
     *     <li>If the pathname starts with a tilde ({@literal ~}, an expanded
     *     path is returned.</li>
     *     <li>If it is a filename or relative pathname, it is located
     *     (relative pathname intact) in the current working directory.</li>
     * </ol>
     *
     * @param file Absolute or relative pathname or filename.
     * @return Absolute path.
     */
    public static Path locate(String file) {
        Path path = Paths.get(file);
        if (!path.isAbsolute()) {
            if (path.startsWith("~")) {
                path = Path.of(expandPath(path.toString()));
            }
            path = path.toAbsolutePath();
        }
        return path.normalize();
    }

    private FileUtils() {}

}
