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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.delegate.Delegate;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class FilesystemSourceTest extends AbstractSourceTest {

    private static final Identifier IDENTIFIER =
            new Identifier("jpg/rgb-64x56x8-baseline.jpg");

    private FilesystemSource instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    void destroyEndpoint() {
        // nothing to do
    }

    @Override
    void initializeEndpoint() {
        // nothing to do
    }

    @Override
    FilesystemSource newInstance() {
        FilesystemSource instance = new FilesystemSource();
        instance.setIdentifier(IDENTIFIER);
        return instance;
    }

    @Override
    void useBasicLookupStrategy() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DELEGATE_ENABLED, false);
        config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX,
                TestUtils.getSampleImagesPath() + File.separator);
    }

    @Override
    void useDelegateLookupStrategy() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMSOURCE_LOOKUP_STRATEGY,
                "DelegateLookupStrategy");
    }

    /* checkAccess() */

    @Test
    void checkAccessUsingBasicLookupStrategyWithPresentUnreadableFile()
            throws Exception {
        Path path = instance.getFile();
        try {
            assumeTrue(path.toFile().setReadable(false));
            assertThrows(AccessDeniedException.class, instance::stat);
        } finally {
            path.toFile().setReadable(true);
        }
    }

    @Test
    void checkAccessUsingDelegateLookupStrategyWithPresentReadableFile()
            throws Exception {
        useDelegateLookupStrategy();

        Identifier identifier = new Identifier(
                TestUtils.getSampleImage(IDENTIFIER.toString()).toString());
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);

        instance.setIdentifier(identifier);
        instance.stat();
    }

    @Test
    void checkAccessUsingDelegateLookupStrategyWithPresentUnreadableFile()
            throws Exception {
        useDelegateLookupStrategy();

        Identifier identifier = new Identifier(
                TestUtils.getSampleImage(IDENTIFIER.toString()).toString());
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);

        Path path = instance.getFile();
        try {
            assumeTrue(path.toFile().setReadable(false));
            Files.setPosixFilePermissions(path, Collections.emptySet());
            assertThrows(AccessDeniedException.class, instance::stat);
        } finally {
            path.toFile().setReadable(true);
        }
    }

    @Test
    void checkAccessUsingDelegateLookupStrategyWithMissingFile() {
        useDelegateLookupStrategy();

        Identifier identifier = new Identifier("missing");
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        instance.setDelegate(delegate);
        instance.setIdentifier(identifier);
        assertThrows(NoSuchFileException.class, instance::stat);
    }

    /* getFormatIterator() */

    @Test
    void getFormatIteratorHasNext() {
        instance.setIdentifier(IDENTIFIER);

        FilesystemSource.FormatIterator<Format> it = instance.getFormatIterator();
        assertTrue(it.hasNext());
        it.next(); // object key
        assertTrue(it.hasNext());
        it.next(); // identifier extension
        assertTrue(it.hasNext());
        it.next(); // magic bytes
        assertFalse(it.hasNext());
    }

    @Test
    void getFormatIteratorNext() {
        Configuration.forApplication().setProperty(
                Key.FILESYSTEMSOURCE_PATH_PREFIX,
                TestUtils.getSampleImagesPath() + File.separator);
        instance.setIdentifier(new Identifier("jpg-incorrect-extension.png"));

        FilesystemSource.FormatIterator<Format> it =
                instance.getFormatIterator();
        assertEquals(Format.get("png"), it.next()); // object key
        assertEquals(Format.get("png"), it.next()); // identifier extension
        assertEquals(Format.get("jpg"), it.next()); // magic bytes
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* getFile() */

    @Test
    void getFile() throws Exception {
        assertNotNull(instance.getFile());
    }

    @Test
    void getFileUsingBasicLookupStrategyWithPrefix() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals(File.separator + "prefix" + File.separator + "id",
                instance.getFile().toString());
    }

    @Test
    void getFileUsingBasicLookupStrategyWithSuffix() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());
    }

    @Test
    void getFileUsingBasicLookupStrategyWithoutPrefixOrSuffix()
            throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "");

        instance.setIdentifier(new Identifier("id"));
        assertEquals("id", instance.getFile().toString());
    }

    /**
     * Tests that all instances of ../, ..\, /.., and \.. are removed
     * to disallow ascending up the directory tree.
     */
    @Test
    void getFileSanitization() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_PREFIX, "/prefix/");
        config.setProperty(Key.FILESYSTEMSOURCE_PATH_SUFFIX, "/suffix");

        instance.setIdentifier(new Identifier("id/../"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        instance.setIdentifier(new Identifier("/../id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        instance.setIdentifier(new Identifier("id\\..\\"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        instance.setIdentifier(new Identifier("\\..\\id"));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" + File.separator + "suffix",
                instance.getFile().toString());

        // test injection-safety
        instance.setIdentifier(new Identifier("/id/../cats\\..\\dogs/../..\\foxes/.\\...\\/....\\.."));
        assertEquals(
                File.separator + "prefix" + File.separator + "id" +
                        File.separator + "cats" + File.separator + "dogs" +
                        File.separator + "foxes" + File.separator + "suffix",
                instance.getFile().toString());
    }

    @Test
    void getFileWithDelegateLookupStrategy() throws Exception {
        useDelegateLookupStrategy();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(IDENTIFIER);
        instance.setDelegate(delegate);

        assertEquals(IDENTIFIER.toString().replace("/", File.separator),
                instance.getFile().toString());
    }

    /* newInputStream() */

    @Test
    void newInputStreamWithPresentReadableFile() throws Exception {
        try (ImageInputStream is = instance.newInputStream()) {
            assertNotNull(is);
        }
    }

}
