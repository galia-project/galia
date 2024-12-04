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

package is.galia.test;

import is.galia.delegate.Delegate;
import is.galia.delegate.DelegateException;
import is.galia.image.Format;
import is.galia.resource.RequestContext;
import is.galia.stream.PathImageInputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;

public final class TestUtils {

    private static File getCurrentWorkingDirectory() {
        try {
            return new File(".").getCanonicalFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return All sample images of the given format from the
     *         <a href="https://github.com/bairdcreek/sample-images">sample-images</a>
     *         submodule.
     */
    public static Collection<Path> getSampleImages(Format format)
            throws IOException {
        final Collection<Path> fixtures = new HashSet<>();

        Files.walkFileTree(getSampleImagesPath(), new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(format.getPreferredExtension())) {
                    fixtures.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        return fixtures;
    }

    public static Path getFixture(String filename) {
        return getFixturePath().resolve(filename);
    }

    /**
     * @return Path of the fixtures directory.
     */
    public static Path getFixturePath() {
        return Paths.get(getCurrentWorkingDirectory().getAbsolutePath(),
                "src", "test", "resources");
    }

    /**
     * @param name Image pathname relative to the root of the sample images
     *             directory.
     * @see #newSampleImageInputStream
     */
    public static Path getSampleImage(String name) {
        return getSampleImagesPath().resolve(name);
    }

    /**
     * @return Path of the sample image fixtures directory.
     */
    public static Path getSampleImagesPath() {
        return getFixturePath().resolve("sample-images");
    }

    public static Delegate newDelegate() {
        try {
            Delegate delegate = new TestDelegate();
            delegate.setRequestContext(new RequestContext());
            return delegate;
        } catch (DelegateException e) {
            throw new RuntimeException(e);
        }
    }

    public static ImageInputStream newSampleImageInputStream(String name)
            throws IOException {
        return new PathImageInputStream(getSampleImage(name));
    }


    /**
     * Saves the given image as a PNG in the user's desktop folder (if macOS)
     * or home folder.
     */
    public static void save(BufferedImage image) {
        // Get the calling class name
        StackWalker.StackFrame stackFrame = StackWalker.getInstance()
                .walk(s -> s.skip(1).findFirst())
                .get();
        String testClassName = stackFrame.getClassName();
        String[] parts = testClassName.split("\\.");
        testClassName = parts[parts.length - 1];
        // Get the user home dir
        String home = System.getProperty("user.home");
        Path parentDir = Path.of(home);
        // If we are on a Mac, use the desktop
        Path desktopDir = Path.of(home, "Desktop");
        if (Files.isDirectory(desktopDir)) {
            parentDir = desktopDir;
        }
        Path dir = parentDir.resolve(testClassName);
        try {
            Files.createDirectories(dir);
            ImageIO.write(image, "PNG",
                    dir.resolve(stackFrame.getMethodName() + ".png").toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Saves the given image bytes in the user's desktop folder (if macOS)
     * or home folder with the given extension.
     */
    public static void save(byte[] imageBytes, String extension) {
        // Get the calling class name
        StackWalker.StackFrame stackFrame = StackWalker.getInstance()
                .walk(s -> s.skip(1).findFirst())
                .get();
        String testClassName = stackFrame.getClassName();
        String[] parts = testClassName.split("\\.");
        testClassName = parts[parts.length - 1];
        // Get the user home dir
        String home = System.getProperty("user.home");
        Path parentDir = Path.of(home);
        // If we are on a Mac, use the desktop
        Path desktopDir = Path.of(home, "Desktop");
        if (Files.isDirectory(desktopDir)) {
            parentDir = desktopDir;
        }
        Path dir = parentDir.resolve(testClassName);
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(stackFrame.getMethodName() + "." + extension), imageBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private TestUtils() {}

}
