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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.operation.ColorTransform;
import is.galia.operation.Crop;
import is.galia.operation.CropToSquare;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.operation.Rotate;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPercent;
import is.galia.util.DeletingFileVisitor;
import is.galia.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static is.galia.test.Assert.PathAssert.*;
import static org.junit.jupiter.api.Assertions.*;

class FilesystemCacheTest extends AbstractCacheTest {

    private Path fixturePath;
    private FilesystemCache instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        fixturePath = Files.createTempDirectory("test").resolve("cache");
        instance    = newVariantCache();
    }

    @Override
    @AfterEach
    public void tearDown() throws IOException {
        try {
            Files.walkFileTree(fixturePath, new DeletingFileVisitor());
        } catch (NoSuchFileException | DirectoryNotEmptyException |
                 AccessDeniedException e) {
            // These are known to happen in Windows sometimes; not yet known
            // why, but it shouldn't result in a test failure.
            e.printStackTrace();
        }
    }

    @Override
    InfoCache newInfoCache() {
        return newVariantCache();
    }

    @Override
    FilesystemCache newVariantCache() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 3);
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 2);
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                fixturePath.toString());
        return new FilesystemCache();
    }

    private void createEmptyFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }

    private void writeStringToFile(Path path,
                                   String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
    }

    @Test
    void hashedPathFragment() {
        // depth = 2, length = 3
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 2);
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_NAME_LENGTH, 3);
        assertEquals(
                String.format("083%s2c1", File.separator),
                FilesystemCache.hashedPathFragment("cats"));

        // depth = 0
        config.setProperty(Key.FILESYSTEMCACHE_DIRECTORY_DEPTH, 0);
        assertEquals("", FilesystemCache.hashedPathFragment("cats"));
    }

    @Test
    void variantImageFile() {
        String pathname = Configuration.forApplication().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");

        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(
                        new ScaleByPercent(0.905),
                        new Encode(Format.get("tif")))
                .build();

        final Path expected = Paths.get(
                pathname,
                "image",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                ops.toFilename());
        assertEquals(expected, FilesystemCache.variantImageFile(ops));
    }

    @Test
    void variantImageTempFile() {
        String pathname = Configuration.forApplication().
                getString(Key.FILESYSTEMCACHE_PATHNAME);

        Identifier identifier    = new Identifier("cats_~!@#$%^&*()");
        Crop crop                = new CropToSquare();
        Scale scale              = new ScaleByPercent(0.905);
        Rotate rotate            = new Rotate(10);
        ColorTransform transform = ColorTransform.BITONAL;
        Encode encode            = new Encode(Format.get("tif"));

        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(crop, scale, rotate, transform, encode)
                .build();

        final Path expected = Paths.get(
                pathname,
                "image",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                ops.toFilename() + FilesystemCache.tempFileSuffix());
        assertEquals(expected, FilesystemCache.variantImageTempFile(ops));
    }

    @Test
    void infoFile() {
        final String pathname = Configuration.forApplication().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtils.md5(identifier.toString()) + ".json");
        assertEquals(expected, FilesystemCache.infoFile(identifier));
    }

    @Test
    void infoTempFile() {
        final String pathname = Configuration.forApplication().
                getString(Key.FILESYSTEMCACHE_PATHNAME);
        final Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        final Path expected = Paths.get(
                pathname,
                "info",
                FilesystemCache.hashedPathFragment(identifier.toString()),
                StringUtils.md5(identifier.toString()) + ".json"
                        + FilesystemCache.tempFileSuffix());
        assertEquals(expected, FilesystemCache.infoTempFile(identifier));
    }

    @Test
    void tempFileSuffix() {
        assertEquals("_" + Thread.currentThread().getName() + ".tmp",
                FilesystemCache.tempFileSuffix());
    }

    /* cleanUp() */

    @Test
    void cleanUpDoesNotDeleteValidFiles() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"));

        // create a new variant image file
        Path variantImageFile = FilesystemCache.variantImageFile(ops);
        Files.createDirectories(variantImageFile.getParent());
        writeStringToFile(variantImageFile, "not empty");

        // create a new info file
        Path infoFile = FilesystemCache.infoFile(ops.getIdentifier());
        Files.createDirectories(infoFile.getParent());
        writeStringToFile(infoFile, "not empty");

        // create some temp files
        Path variantImageTempFile = FilesystemCache.variantImageTempFile(ops);
        writeStringToFile(variantImageTempFile, "not empty");

        Path infoTempFile = FilesystemCache.infoTempFile(ops.getIdentifier());
        writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        Path root = FilesystemCache.rootVariantImagePath();
        Path subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootInfoPath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        instance.setMinCleanableAge(10000);
        instance.cleanUp();

        assertRecursiveFileCount(fixturePath, 8);
    }

    @Test
    void cleanUpDeletesInvalidFiles() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"));

        // create a new variant image file
        Path variantImageFile = FilesystemCache.variantImageFile(ops);
        writeStringToFile(variantImageFile, "not empty");

        // create a new info file
        Path infoFile = FilesystemCache.infoFile(ops.getIdentifier());
        writeStringToFile(infoFile, "not empty");

        // create some temp files
        Path variantImageTempFile = FilesystemCache.variantImageTempFile(ops);
        writeStringToFile(variantImageTempFile, "not empty");

        Path infoTempFile = FilesystemCache.infoTempFile(ops.getIdentifier());
        writeStringToFile(infoTempFile, "not empty");

        // create some empty files
        Path root = FilesystemCache.rootVariantImagePath();
        Path subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        root = FilesystemCache.rootInfoPath();
        subdir = root.resolve("bogus");
        Files.createDirectories(subdir);
        Files.createFile(subdir.resolve("empty"));
        Files.createFile(subdir.resolve("empty2"));

        instance.setMinCleanableAge(10);

        Thread.sleep(1000);

        instance.cleanUp();

        assertRecursiveFileCount(fixturePath, 2);
    }

    /* getVariantImageFiles() */

    @Test
    void getVariantImageFiles() throws Exception {
        Identifier identifier = new Identifier("dogs");
        OperationList ops = new OperationList(identifier);

        Path imageFile = FilesystemCache.variantImageFile(ops);
        createEmptyFile(imageFile);

        ops.add(new Rotate(15));
        imageFile = FilesystemCache.variantImageFile(ops);
        createEmptyFile(imageFile);

        ops.add(ColorTransform.GRAY);
        imageFile = FilesystemCache.variantImageFile(ops);
        createEmptyFile(imageFile);

        assertEquals(3, instance.getVariantImageFiles(identifier).size());
    }

}
