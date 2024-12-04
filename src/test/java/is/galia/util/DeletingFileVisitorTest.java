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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class DeletingFileVisitorTest extends BaseTest {

    private DeletingFileVisitor instance;
    private Path tempPath;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        tempPath = Files.createTempDirectory("test");
        instance = new DeletingFileVisitor();
        instance.addExclude(tempPath);
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        Files.walkFileTree(tempPath, instance);
    }

    @Test
    void testGetDeletedFileCount() throws IOException {
        assertEquals(0, instance.getDeletedFileCount());

        Files.createDirectory(tempPath.resolve("cats"));
        Files.createFile(tempPath.resolve("cats/file"));
        Files.createDirectory(tempPath.resolve("dogs"));
        Files.createFile(tempPath.resolve("dogs/file"));
        Files.walkFileTree(tempPath, instance);

        assertEquals(2, instance.getDeletedFileCount());
    }

    @Test
    void testGetDeletedFileSize() throws IOException {
        assertEquals(0, instance.getDeletedFileSize());

        Files.createDirectory(tempPath.resolve("cats"));
        Files.write(tempPath.resolve("cats/file"), "bla".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(tempPath.resolve("dogs"));
        Files.write(tempPath.resolve("dogs/file"), "bla".getBytes(StandardCharsets.UTF_8));
        Files.walkFileTree(tempPath, instance);

        assertEquals(6, instance.getDeletedFileSize());
    }

    @Test
    void testVisitFile() throws IOException {
        Path file = tempPath.resolve("file");
        Files.createFile(file);

        instance.visitFile(file, fileAttributesForFile());
        assertFalse(Files.exists(file));
    }

    @Test
    void testPostVisitDirectory() throws IOException {
        Path dir = tempPath.resolve("dir");
        Files.createDirectory(dir);

        FileVisitResult result = instance.postVisitDirectory(dir,
                new IOException());
        assertEquals(FileVisitResult.CONTINUE, result);
        assertFalse(Files.exists(dir));
    }

    @Test
    void testPostVisitDirectoryWithRootDirWhenExcludingRootDir()
            throws IOException {
        instance.addExclude(tempPath);
        instance.postVisitDirectory(tempPath, new IOException());
        assertTrue(Files.exists(tempPath));
    }

    private BasicFileAttributes fileAttributesForFile() {
        return new BasicFileAttributes() {
            @Override
            public FileTime lastModifiedTime() {
                return null;
            }

            @Override
            public FileTime lastAccessTime() {
                return null;
            }

            @Override
            public FileTime creationTime() {
                return null;
            }

            @Override
            public boolean isRegularFile() {
                return true;
            }

            @Override
            public boolean isDirectory() {
                return false;
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public boolean isOther() {
                return false;
            }

            @Override
            public long size() {
                return 0;
            }

            @Override
            public Object fileKey() {
                return null;
            }
        };
    }

}
