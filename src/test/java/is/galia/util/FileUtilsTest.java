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

import org.junit.jupiter.api.Test;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest extends BaseTest {

    /* checkReadableFile() */

    @Test
    void checkReadableFileWithNonexistentFile() {
        final Path path = Paths.get("/bogus/bogus/bogus.txt");
        Exception e = assertThrows(NoSuchFileException.class,
                () -> FileUtils.checkReadableFile(path));
        assertEquals("File does not exist: " + path, e.getMessage());
    }

    @Test
    void checkReadableFileWithEmptyArgument() {
        final Path path = Paths.get("");
        Exception e = assertThrows(NoSuchFileException.class,
                () -> FileUtils.checkReadableFile(path));
        assertEquals("No pathname supplied", e.getMessage());
    }

    @Test
    void checkReadableFileWithNonRegularFile() {
        final Path path = TestUtils.getFixturePath();
        Exception e = assertThrows(IOException.class,
                () -> FileUtils.checkReadableFile(path));
        assertEquals("Not a regular file: " + path, e.getMessage());
    }

    @Test
    void checkReadableFileWithNonReadableFile() throws Exception {
        Path file = null;
        try {
            file = Files.createTempFile(getClass().getSimpleName(), ".tmp");
            setNotReadable(file);

            final Path finalFile = file;
            Exception e = assertThrows(AccessDeniedException.class,
                    () -> FileUtils.checkReadableFile(finalFile));
            assertEquals("Not readable: " + file, e.getMessage());
        } finally {
            if (file != null) {
                Files.delete(file);
            }
        }
    }

    @Test
    void checkReadableFileWithReadableFile() throws Exception {
        assertTrue(FileUtils.checkReadableFile(
                TestUtils.getFixture("config.yml")));
    }

    /* checkWritableDirectory() */

    @Test
    void checkWritableDirectoryWithNonexistentDirectory() {
        final Path path = Paths.get("/bogus/bogus/bogus");
        Exception e = assertThrows(NoSuchFileException.class,
                () -> FileUtils.checkWritableDirectory(path));
        assertEquals("Directory does not exist: " + path, e.getMessage());
    }

    @Test
    void checkWritableDirectoryWithEmptyArgument() {
        final Path path = Paths.get("");
        Exception e = assertThrows(NoSuchFileException.class,
                () -> FileUtils.checkWritableDirectory(path));
        assertEquals("No pathname supplied", e.getMessage());
    }

    @Test
    void checkWritableDirectoryWithNonDirectory() {
        final Path path = TestUtils.getFixture("text.txt");
        Exception e = assertThrows(IOException.class,
                () -> FileUtils.checkWritableDirectory(path));
        assertEquals("Not a directory: " + path, e.getMessage());
    }

    @Test
    void checkWritableDirectoryWithNonWritableDirectory() throws Exception {
        Path dir = null;
        try {
            dir = Files.createTempDirectory(getClass().getSimpleName());
            setNotWritable(dir);

            final Path finalDir = dir;
            Exception e = assertThrows(AccessDeniedException.class,
                    () -> FileUtils.checkWritableDirectory(finalDir));
            assertEquals("Not writable: " + dir, e.getMessage());
        } finally {
            if (dir != null) {
                Files.delete(dir);
            }
        }
    }

    @Test
    void checkWritableDirectoryWithWritableDirectory() throws Exception {
        assertTrue(FileUtils.checkWritableDirectory(
                TestUtils.getFixturePath()));
    }

    /* expandPath() */

    @Test
    void expandPath() {
        assertEquals(System.getProperty("user.home") + "/cats",
                FileUtils.expandPath("~/cats"));
    }

    /* locate() */

    @Test
    void locateWithAbsolutePath() {
        Path path = TestUtils.getFixture("jpg");
        assertEquals(path, FileUtils.locate(path.toString()));
    }

    @Test
    void locateWithTildePath() {
        Path expectedPath = Paths.get(FileUtils.expandPath("~"), "myFile").toAbsolutePath().normalize();
        assertEquals(expectedPath, FileUtils.locate("~" + File.separator + "myFile"));
    }

    @Test
    void locateWithFileInCurrentWorkingDirectory() {
        Path expectedPath = Paths.get(".", "myFile").toAbsolutePath().normalize();
        assertEquals(expectedPath, FileUtils.locate("myFile"));
    }

    /**
     * Tries to make a path not readable in a non-platform-specific way.
     */
    private void setNotReadable(Path path) throws IOException {
        FileStore fileStore = Files.getFileStore(TestUtils.getFixturePath());
        if (fileStore.supportsFileAttributeView(PosixFileAttributeView.class)) { // Linux & macOS
            Files.setPosixFilePermissions(path,
                    PosixFilePermissions.fromString("---------"));
        } else if (fileStore.supportsFileAttributeView(AclFileAttributeView.class)) { // Windows 10
            AclFileAttributeView view =
                    Files.getFileAttributeView(path, AclFileAttributeView.class);
            List<AclEntry> entries = new ArrayList<>();
            for (AclEntry acl : view.getAcl()) {
                Set<AclEntryPermission> perms = new HashSet<>(acl.permissions());
                perms.remove(AclEntryPermission.READ_DATA);
                AclEntry ae = AclEntry.newBuilder()
                        .setType(acl.type())
                        .setPrincipal(acl.principal())
                        .setPermissions(perms)
                        .setFlags(acl.flags())
                        .build();
                entries.add(ae);
            }
            view.setAcl(entries);
        } else {
            throw new IOException("Don't know how to change file permissions");
        }
    }

    /**
     * Tries to make a path not writable in a non-platform-specific way.
     */
    private void setNotWritable(Path path) throws IOException {
        FileStore fileStore = Files.getFileStore(TestUtils.getFixturePath());
        if (fileStore.supportsFileAttributeView(PosixFileAttributeView.class)) { // Linux & macOS
            Files.setPosixFilePermissions(
                    path, PosixFilePermissions.fromString("---------"));
        } else if (fileStore.supportsFileAttributeView(AclFileAttributeView.class)) { // Windows 10
            AclFileAttributeView view =
                    Files.getFileAttributeView(path, AclFileAttributeView.class);
            List<AclEntry> entries = new ArrayList<>();
            for (AclEntry acl : view.getAcl()) {
                Set<AclEntryPermission> perms = new HashSet<>(acl.permissions());
                perms.remove(AclEntryPermission.WRITE_DATA);
                perms.remove(AclEntryPermission.APPEND_DATA);
                perms.remove(AclEntryPermission.ADD_SUBDIRECTORY);
                AclEntry ae = AclEntry.newBuilder()
                        .setType(acl.type())
                        .setPrincipal(acl.principal())
                        .setPermissions(perms)
                        .setFlags(acl.flags())
                        .build();
                entries.add(ae);
            }
            view.setAcl(entries);
        } else {
            throw new IOException("Don't know how to change file permissions");
        }
    }

}
