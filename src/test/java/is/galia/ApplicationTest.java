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

import is.galia.codec.MockDecoderPlugin;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.ClientFactory;
import is.galia.test.TestDelegate;
import is.galia.http.Client;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import is.galia.util.SocketUtils;
import is.galia.util.SoftwareVersion;
import is.galia.util.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static is.galia.CommandLineArgumentHandler.CONFIG_ARGUMENT;
import static is.galia.CommandLineArgumentHandler.USAGE_ARGUMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class ApplicationTest extends BaseTest {

    private static final PrintStream CONSOLE_OUTPUT = System.out;
    private static final PrintStream CONSOLE_ERROR  = System.err;
    private static final int HTTP_PORT              = SocketUtils.getOpenPort();

    private final Client httpClient = ClientFactory.newClient();

    private final ByteArrayOutputStream redirectedStdout =
            new ByteArrayOutputStream();
    private final ByteArrayOutputStream redirectedStderr =
            new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        SystemUtils.clearExitRequest();
        redirectOutput();

        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.HTTP_ENABLED, true);
        config.setProperty(Key.HTTP_PORT, HTTP_PORT);
        config.setProperty(Key.HTTPS_ENABLED, false);

        httpClient.setURI(new Reference("http://localhost:" + HTTP_PORT + "/"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        Application.getAppServer().stop();
        httpClient.stop();
        resetOutput();
    }

    /**
     * Redirects stdout/stderr output to byte arrays for analysis.
     */
    private void redirectOutput() {
        System.setOut(new PrintStream(redirectedStdout));
        System.setErr(new PrintStream(redirectedStderr));
    }

    private void resetOutput() {
        System.setOut(CONSOLE_OUTPUT);
        System.setErr(CONSOLE_ERROR);
    }

    /* getJARFile() */

    @Test
    void getJARFile() {
        assertEquals("classes", Application.getJARFile().getFileName().toString());
    }

    /* getName() */

    /**
     * {@link Application#getName()} is not fully testable as it returns a
     * different value when the app is running from a JAR.
     */
    @Test
    void getName() {
        assertEquals("galia", Application.getName());
    }

    /* getSpecificationVersion() */

    /**
     * {@link Application#getSpecificationVersion()} is not fully testable as
     * it returns a different value when the app is running from a JAR. But we
     * can at least test that it falls back to the version from {@literal
     * pom.xml}.
     */
    @Test
    void getSpecificationVersion() {
        assertEquals(new SoftwareVersion(1),
                Application.getSpecificationVersion());
    }

    /* getTempDir() */

    @Test
    void getTempDirSetInConfiguration() throws IOException {
        Path expectedDir = Files.createTempDirectory("test");
        Configuration.forApplication()
                .setProperty(Key.TEMP_PATHNAME, expectedDir.toString());

        Path actualDir = Application.getTempDir();
        assertEquals(expectedDir, actualDir);
    }

    @Test
    void getTempDirFallsBackToDefault() {
        Path expectedDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Configuration.forApplication().clearProperty(Key.TEMP_PATHNAME);

        Path actualDir = Application.getTempDir();
        assertEquals(expectedDir, actualDir);
    }

    @Test
    void getTempDirCreatesDirectory() {
        Path expectedDir = Paths.get(System.getProperty("java.io.tmpdir"),
                "cats", "cats", "cats");
        Configuration.forApplication()
                .setProperty(Key.TEMP_PATHNAME, expectedDir.toString());

        Application.getTempDir();
        assertTrue(Files.exists(expectedDir));
    }

    /* getVersion() */

    /**
     * {@link Application#getVersion()} is not fully testable as it returns a
     * different value when the app is running from a JAR. But we can at least
     * test that it falls back to the version from {@literal pom.xml}.
     */
    @Test
    void getVersion() throws Exception {
        String pomVersion       = null;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(false);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document document       = builder.parse(Path.of("pom.xml").toFile());
        XPath xPath             = XPathFactory.newInstance().newXPath();
        String expression       = "/project/version";
        Node node = (Node) xPath.compile(expression).evaluate(document, XPathConstants.NODE);
        if (node != null) {
            pomVersion = node.getTextContent();
        }
        SoftwareVersion expectedVersion = SoftwareVersion.parse(pomVersion);
        SoftwareVersion actualVersion   = Application.getVersion();
        assertEquals(expectedVersion, actualVersion);
    }

    /* isTesting() */

    @Test
    void isTestingWhenTesting() {
        assertTrue(Application.isTesting());
    }

    @Test
    void isTestingWhenNotTesting() {
        try {
            System.clearProperty(Application.ENVIRONMENT_VM_ARGUMENT);
            assertFalse(Application.isTesting());
        } finally {
            System.setProperty(Application.ENVIRONMENT_VM_ARGUMENT,
                    Application.TEST_ENVIRONMENT);
        }
    }

    /* main() */

    /**
     * N.B.: Argument handling is tested more thoroughly in {@link
     * CommandLineArgumentHandlerTest}.
     */
    @Test
    void mainWithUsageArgument() {
        Application.main("-" + USAGE_ARGUMENT);
        assertTrue(redirectedStdout.toString().contains("Usage:"));
        assertTrue(SystemUtils.exitRequested());
        assertEquals(0, SystemUtils.requestedExitCode());
    }

    @Test
    void mainWithValidConfigArgumentStartsServer() throws Exception {
        // TODO: this fails in Windows
        assumeFalse(org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS);
        Application.main("-" + CONFIG_ARGUMENT,
                TestUtils.getFixture("config.yml").toString());
        try (Response response = httpClient.send()) {
            assertEquals(Status.OK, response.getStatus());
        }
    }

    @Test
    void mainCallsOnApplicationStartMethodOnAllPlugins() {
        Application.main("-" + CONFIG_ARGUMENT,
                TestUtils.getFixture("config.yml").toString());
        assertTrue(MockDecoderPlugin.isApplicationStarted);
        assertTrue(TestDelegate.isApplicationStarted);
        // Probably safe to assume that all the rest are OK too.
    }

    @Disabled // TODO: this passes 100% of the time when run on its own, but fails often when run with other tests
    @Test
    void mainWithFailingToBindToPortExits() throws Exception {
        // Create a new config file containing our used port.
        Path configFile = Files.createTempFile(getClass().getSimpleName(), ".yml");
        int port = SocketUtils.getOpenPort();
        try (ServerSocket socket = new ServerSocket(port)) {
            String configYaml = Key.HTTP_ENABLED + ": true\n" +
                    Key.HTTP_PORT + ": " + port + "\n";
            Files.writeString(configFile, configYaml);

            Application.main("-" + CONFIG_ARGUMENT, configFile.toString());

            assertTrue(SystemUtils.exitRequested());
            assertEquals(-1, SystemUtils.requestedExitCode());
            assertTrue(redirectedStderr.toString().contains(
                    "Failed to bind to"));
        } finally {
            Files.delete(configFile);
        }
    }

}
