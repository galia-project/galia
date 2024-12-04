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

import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class ArchiveUtilsTest extends BaseTest {

    private static final Path FIXTURE = TestUtils.getFixture("zip.zip");

    /* entries() */

    @Test
    void entries() throws Exception {
        List<ZipEntry> entries = ArchiveUtils.entries(FIXTURE);
        assertFalse(entries.isEmpty());
    }

    /* unzip() */

    @Test
    void unzipWithMissingFile() throws Exception {
        Path tempDir = Files.createTempDirectory(getClass().getSimpleName());
        try {
            assertThrows(NoSuchFileException.class,
                    () -> ArchiveUtils.unzip(Path.of("bogus", "bogus"), tempDir));
        } finally {
            Files.delete(tempDir);
        }
    }

    @Test
    void unzipWithNonZipFile() throws Exception {
        Path tempDir = Files.createTempDirectory(getClass().getSimpleName());
        try {
            assertThrows(ProviderNotFoundException.class,
                    () -> ArchiveUtils.unzip(TestUtils.getFixture("text.txt"), tempDir));
        } finally {
            Files.delete(tempDir);
        }
    }

    @Test
    void unzipCreatesFiles() throws Exception {
        Path tempDir = Files.createTempDirectory(getClass().getSimpleName());
        try {
            ArchiveUtils.unzip(TestUtils.getFixture("zip.zip"), tempDir);
            assertTrue(Files.exists(tempDir.resolve("executable")));
            assertTrue(Files.exists(tempDir.resolve("hello_world_1.txt")));
            assertTrue(Files.exists(tempDir.resolve("hello_world_2.txt")));
            assertTrue(Files.exists(tempDir.resolve("readonly")));
            assertTrue(Files.exists(tempDir.resolve("subdirectory")));
            assertTrue(Files.exists(tempDir.resolve("subdirectory/hello_world_3.txt")));
        } finally {
            Files.walkFileTree(tempDir, new DeletingFileVisitor());
        }
    }

    @Test
    void unzipPreservesPermissions() throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        Path tempDir = Files.createTempDirectory(getClass().getSimpleName());
        try {
            ArchiveUtils.unzip(TestUtils.getFixture("zip.zip"), tempDir);
            {
                Path path = tempDir.resolve("executable");
                Set<PosixFilePermission> expectedPerms = Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE);
                assertEquals(expectedPerms, Files.getPosixFilePermissions(path));
            }
            {
                Path path = tempDir.resolve("hello_world_1.txt");
                Set<PosixFilePermission> expectedPerms = Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.OTHERS_READ);
                assertEquals(expectedPerms, Files.getPosixFilePermissions(path));
            }
            {
                Path path = tempDir.resolve("readonly");
                Set<PosixFilePermission> expectedPerms = Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.OTHERS_READ);
                assertEquals(expectedPerms, Files.getPosixFilePermissions(path));
            }
            {
                Path path = tempDir.resolve("subdirectory");
                Set<PosixFilePermission> expectedPerms = Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE);
                assertEquals(expectedPerms, Files.getPosixFilePermissions(path));
            }
        } finally {
            Files.walkFileTree(tempDir, new DeletingFileVisitor());
        }
    }

}
