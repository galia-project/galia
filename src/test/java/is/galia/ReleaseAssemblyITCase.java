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

package is.galia;

import is.galia.async.ThreadPool;
import is.galia.test.BaseITCase;
import is.galia.util.ArchiveUtils;
import is.galia.util.DeletingFileVisitor;
import is.galia.util.MavenUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Tests the release archive.
 */
class ReleaseAssemblyITCase extends BaseITCase {

    @Test
    void testAssembledZipFile() throws Exception {
        final String expectedPackageFilename =
                Application.getName().toLowerCase() + "-" +
                        Application.getVersion() + ".zip";
        final Path zipFile                 = MavenUtils.assemblePackage(expectedPackageFilename);
        final String zipBasename           = zipFile.getFileName().toString();
        final String zipBasenameWithoutExt = zipBasename.replace(".zip", "");
        final List<ZipEntry> entries       = ArchiveUtils.entries(zipFile);
        final List<String> files           = entries.stream()
                .map(e -> e.getName().replace(zipBasenameWithoutExt + "/", ""))
                .toList();
        assertTrue(files.contains("bin/start.sh"));
        assertTrue(files.contains("config/config.yml"));
        assertTrue(files.contains("config/jvm.options"));
        assertTrue(files.contains("lib/" + zipBasenameWithoutExt + ".jar"));
        assertTrue(files.contains("log/"));
        assertTrue(files.contains("plugins/"));
        assertTrue(files.contains("CHANGES.md"));
        assertTrue(files.contains("LICENSE.txt"));
        assertTrue(files.contains("LICENSE-3RD-PARTY.txt"));
        assertTrue(files.contains("README.md"));
        assertTrue(files.contains("UPGRADING.md"));
    }

    @Test
    void testUnixConfigtestScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/configtest.sh");
            assertTrue(output.contains("Configuration OK"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixInstallPluginScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/install_plugin.sh");
            assertTrue(output.contains("Usage:"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixListFontsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/list_fonts.sh");
            assertTrue(output.contains("SansSerif"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixListFormatsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/list_formats.sh");
            assertTrue(output.contains("JPEG"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixListPluginsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/list_plugins.sh");
            assertTrue(output.contains("SOURCES"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixRemovePluginScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/remove_plugin.sh");
            assertTrue(output.contains("Usage:"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixStartScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/start.sh");
            assertTrue(output.contains("Started ServerConnector"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixUpdatePluginScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/update_plugin.sh");
            assertTrue(output.contains("Usage:"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testUnixUpdatePluginsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_UNIX);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/update_plugins.sh");
            assertTrue(output.isEmpty());
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsConfigtestScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/configtest.cmd");
            assertTrue(output.contains("Configuration OK"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsInstallPluginScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/install_plugin.cmd");
            assertTrue(output.contains("Usage:"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsListFontsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/list_fonts.cmd");
            assertTrue(output.contains("SansSerif"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsListFormatsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/list_formats.cmd");
            assertTrue(output.contains("JPEG"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsListPluginsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/list_plugins.cmd");
            assertTrue(output.contains("SOURCES"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsRemovePluginScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/remove_plugin.cmd");
            assertTrue(output.contains("Usage:"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsStartScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/start.cmd");
            assertTrue(output.contains("Started ServerConnector"));
        } finally {
            try {
                Files.walkFileTree(workingDir, new DeletingFileVisitor());
            } catch (FileSystemException e) {
                // This has been known to happen in Windows sometimes with the
                // message: "...\lib\annotations-13.0.jar: The process cannot
                // access the file because it is being used by another process"
                e.printStackTrace();
            }
        }
    }

    @Test
    void testWindowsUpdatePluginScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/update_plugin.cmd");
            assertTrue(output.contains("Usage:"));
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    @Test
    void testWindowsUpdatePluginsScript() throws Exception {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        Path workingDir = installPackage();
        try {
            String output = run(workingDir, "bin/update_plugins.cmd");
            assertTrue(output.isEmpty());
        } finally {
            Files.walkFileTree(workingDir, new DeletingFileVisitor());
        }
    }

    //endregion
    //region Private methods

    /**
     * @return Application root directory.
     */
    private Path installPackage() throws IOException {
        final String expectedPackageFilename =
                Application.getName().toLowerCase() + "-" +
                Application.getVersion() + ".zip";
        final Path zipFile = MavenUtils.assemblePackage(expectedPackageFilename);
        final Path tempDir = Application.getTempDir();
        ArchiveUtils.unzip(zipFile, tempDir);

        final String zipBasename           = zipFile.getFileName().toString();
        final String zipBasenameWithoutExt = zipBasename.replace(".zip", "");
        final Path workingDir              = tempDir.resolve(zipBasenameWithoutExt);
        return workingDir;
    }

    /**
     * @param workingDir Root directory of the application.
     * @param command    Script path relative to {@code workingDir}, and
     *                   arguments.
     * @return           Command output.
     */
    private String run(Path workingDir,
                       String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);
        builder.command(command);
        Process process = builder.start();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ThreadPool.getInstance().submit(() -> {
            try (InputStream is = process.getInputStream()) {
                is.transferTo(os);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        Thread.sleep(3000);
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        return os.toString(StandardCharsets.UTF_8);
    }

}
