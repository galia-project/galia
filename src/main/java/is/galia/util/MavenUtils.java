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

import is.galia.async.ThreadPool;
import org.apache.commons.lang3.SystemUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains various Maven helper methods.
 */
public final class MavenUtils {

    /** Lazy-initialized by {@link #readPOM()} */
    private static Document pom;

    /**
     * Assembles a release package by running {@code mvn package}.
     *
     * @return Path to the assembled zip file.
     */
    public static Path assemblePackage(String expectedPackageFilename)
            throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.redirectErrorStream(true);
        List<String> command = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            command.add("cmd");
            command.add("/c");
        }
        command.add("mvn");
        command.add("package");
        command.add("-DskipTests");
        builder.command(command);
        Process process = builder.start();
        ThreadPool.getInstance().submit(() -> {
            try {
                process.getInputStream().transferTo(OutputStream.nullOutputStream());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            int status = process.waitFor();
            if (status != 0) {
                throw new IOException("mvn command failed with status code " + status);
            }
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        Path targetPath = Path.of("target", expectedPackageFilename);
        if (!Files.exists(targetPath)) {
            throw new NoSuchFileException("" + targetPath);
        }
        return targetPath;
    }

    public static String readArtifactIDFromPOM() throws Exception {
        Document document = readPOM();
        XPath xPath       = XPathFactory.newInstance().newXPath();
        String expression = "/project/artifactId";
        Node node         = (Node) xPath.compile(expression).evaluate(
                document, XPathConstants.NODE);
        return node.getTextContent();
    }

    public static String readSpecificationVersionFromPOM() throws Exception {
        Document document = readPOM();
        XPath xPath       = XPathFactory.newInstance().newXPath();
        String expression = "/project/properties/specification-version";
        Node node         = (Node) xPath.compile(expression).evaluate(
                document, XPathConstants.NODE);
        return node.getTextContent();
    }

    public static String readVersionFromPOM() throws Exception {
        Document document = readPOM();
        XPath xPath       = XPathFactory.newInstance().newXPath();
        String expression = "/project/version";
        Node node         = (Node) xPath.compile(expression).evaluate(
                document, XPathConstants.NODE);
        return node.getTextContent();
    }

    private static synchronized Document readPOM() throws Exception {
        if (pom == null) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            pom = builder.parse(new File("pom.xml"));
        }
        return pom;
    }

    private MavenUtils() {}

}
