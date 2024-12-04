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

package is.galia.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Status;
import is.galia.plugin.repository.MockArtifactRepository;
import is.galia.test.BaseTest;
import is.galia.util.DeletingFileVisitor;
import is.galia.util.SoftwareVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactRepositoryClientTest extends BaseTest {

    private final MockArtifactRepository repo = new MockArtifactRepository();
    private ArtifactRepositoryClient instance;
    private Path pluginsDir;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        pluginsDir = Files.createTempDirectory(getClass().getSimpleName());

        PluginManager.setPluginsDir(pluginsDir);
        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY,
                MockArtifactRepository.CUSTOMER_KEY);

        repo.start();

        instance = new ArtifactRepositoryClient();
        instance.setApplicationSpecificationVersion(new SoftwareVersion(1));
        instance.setBaseURI(repo.getURI());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        repo.stop();
        Files.walkFileTree(pluginsDir, new DeletingFileVisitor());
        PluginManager.setPluginsDir(PluginManager.DEFAULT_PLUGINS_DIR);
    }

    /**
     * Uncomment to dump the response JSON to the console.
     */
/*
    @Test
    void dumpJSON() {
        final String path = "/products/galia";
        Client client = new Client();
        client.getHeaders().set("Authorization",
                "Bearer " + MockArtifactRepository.CUSTOMER_KEY);
        client.setURI(repository.getURI().resolvePath(path));
        try (Response response = client.send()) {
            System.out.println(response.getBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/
    /* getBaseURI() */

    @Test
    void getBaseURIReturnsIVarValueWhenSet() {
        Configuration.forApplication().setProperty(Key.ARTIFACT_REPOSITORY_BASE_URI,
                "http://should-not-be-used");
        Reference uri = new Reference("http://example.org/cats");
        instance.setBaseURI(uri);
        assertEquals(uri, instance.getBaseURI());
    }

    @Test
    void getBaseURIFallsBackToConfigurationKey() {
        instance.setBaseURI(null);
        String uri = "http://example.org/cats";
        Configuration.forApplication().setProperty(Key.ARTIFACT_REPOSITORY_BASE_URI, uri);
        assertEquals(new Reference(uri), instance.getBaseURI());
    }

    @Test
    void getBaseURIFallsBackToDefaultValue() {
        instance.setBaseURI(null);
        assertEquals(new Reference(ArtifactRepositoryClient.DEFAULT_BASE_URI),
                instance.getBaseURI());
    }

    /* findProduct(String) */

    @Test
    void findProductWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> instance.findProduct(null));
    }

    @Test
    void findProductWithUnknownHost() {
        instance.setBaseURI(new Reference("http://bogus.example.org"));
        assertThrows(ConnectException.class,
                () -> instance.findProduct("what"));
    }

    @Test
    void findProductWithPrivateProductWithMissingCustomerKey()
            throws Exception {
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        assertNull(instance.findProduct("private-plugin"));
    }

    @Test
    void findProductWithPublicProductWithMissingCustomerKey() throws Exception {
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        assertNotNull(instance.findProduct("public-plugin"));
    }

    @Test
    void findProductWithPrivateProductWithBogusCustomerKey() {
        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY, "bogus");
        AccountNotFoundException e = assertThrows(
                AccountNotFoundException.class,
                () -> instance.findProduct("private-plugin"));
        assertEquals("Account not found. Check the value of your " +
                Key.CUSTOMER_KEY + " configuration key. If you don't have a " +
                "key, leave it blank.", e.getMessage());
    }

    @Test
    void findProductWithPublicProductWithBogusCustomerKey() {
        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY, "bogus");
        AccountNotFoundException e = assertThrows(
                AccountNotFoundException.class,
                () -> instance.findProduct("public-plugin"));
        assertEquals("Account not found. Check the value of your " +
                Key.CUSTOMER_KEY + " configuration key. If you don't have a " +
                "key, leave it blank.", e.getMessage());
    }

    @Test
    void findProductWithInvalidPluginName() throws Exception {
        assertNull(instance.findProduct("bogus"));
    }

    @Test
    void findProductInAccountIndex() throws Exception {
        assertNotNull(instance.findProduct("private-plugin"));
    }

    @Test
    void findProductInPublicIndex() throws Exception {
        assertNotNull(instance.findProduct("public-plugin"));
    }

    /* findLatestCompatibleVersion() */

    @Test
    void findLatestCompatibleVersionWithNullProductNode() {
        assertThrows(NullPointerException.class,
                () -> instance.findLatestCompatibleVersion(null));
    }

    @Test
    void findLatestCompatibleVersionWithNoVersions() throws Exception {
        final String pluginName    = "public-plugin-no-versions";
        final JsonNode productNode = instance.findProduct(pluginName);
        VersionNotFoundException e = assertThrows(
                VersionNotFoundException.class,
                () -> instance.findLatestCompatibleVersion(productNode));
        assertEquals("No releases found for plugin: " + pluginName,
                e.getMessage());
    }

    @Test
    void findLatestCompatibleVersionWithNullSpecVersion() throws Exception {
        final String pluginName    = "public-plugin-null-spec-version";
        final JsonNode productNode = instance.findProduct(pluginName);
        APIException e = assertThrows(APIException.class,
                () -> instance.findLatestCompatibleVersion(productNode));
        assertEquals("Version node does not have a specification version.",
                e.getMessage());
    }

    @Test
    void findLatestCompatibleVersionWithNoCompatibleVersions() throws Exception {
        instance.setApplicationSpecificationVersion(new SoftwareVersion(99, 99));
        final String pluginName    = "public-plugin";
        final JsonNode productNode = instance.findProduct(pluginName);
        CompatibilityException e = assertThrows(
                CompatibilityException.class,
                () -> instance.findLatestCompatibleVersion(productNode));
        assertEquals("The plugin " + pluginName + " has no compatible releases.",
                e.getMessage());
    }

    @Test
    void findLatestCompatibleVersionWithNoArtifact() throws Exception {
        final String pluginName    = "public-plugin-no-artifact";
        final JsonNode productNode = instance.findProduct(pluginName);
        ArtifactNotFoundException e = assertThrows(
                ArtifactNotFoundException.class,
                () -> instance.findLatestCompatibleVersion(productNode));
        assertEquals("The plugin " + pluginName + " (version 1.0) has no " +
                "artifacts to download.",
                e.getMessage());
    }

    @Test
    void findLatestCompatibleVersionWithNullArtifactFilename() throws Exception {
        final String pluginName    = "public-plugin-null-artifact-filename";
        final JsonNode productNode = instance.findProduct(pluginName);
        APIException e = assertThrows(
                APIException.class,
                () -> instance.findLatestCompatibleVersion(productNode));
        assertEquals("Artifact filename property is null", e.getMessage());
    }

    @Test
    void findLatestCompatibleVersionWithNullArtifactMD5() throws Exception {
        final String pluginName    = "public-plugin-null-artifact-md5";
        final JsonNode productNode = instance.findProduct(pluginName);
        APIException e = assertThrows(
                APIException.class,
                () -> instance.findLatestCompatibleVersion(productNode));
        assertEquals("Artifact md5 property is null", e.getMessage());
    }

    @Test
    void findLatestCompatibleVersionWithNullArtifactURL() {
        // This is difficult to test due to the design of
        // MockArtifactRepository, but also highly unlikely to ever happen,
        // due to the design of the real repository.
    }

    @Test
    void findLatestCompatibleVersion() throws Exception {
        final String pluginName    = "public-plugin";
        final JsonNode productNode = instance.findProduct(pluginName);
        assertNotNull(instance.findLatestCompatibleVersion(productNode));
    }

    /* findVersion() */

    @Test
    void findVersionWithNullProductNode() {
        assertThrows(NullPointerException.class,
                () -> instance.findVersion(null, "99.99"));
    }

    @Test
    void findVersionWithNullVersion() throws Exception {
        final String pluginName    = "public-plugin";
        final JsonNode productNode = instance.findProduct(pluginName);
        assertThrows(NullPointerException.class,
                () -> instance.findVersion(productNode, null));
    }

    @Test
    void findVersionWithNoMatchingVersion() throws Exception {
        final String pluginName    = "public-plugin";
        final JsonNode productNode = instance.findProduct(pluginName);
        VersionNotFoundException e = assertThrows(
                VersionNotFoundException.class,
                () -> instance.findVersion(productNode, "99.99"));
        assertEquals("The plugin " + pluginName +
                        " does not have a 99.99 version.", e.getMessage());
    }

    @Test
    void findVersionWithIncompatibleVersion() throws Exception {
        instance.setApplicationSpecificationVersion(new SoftwareVersion(99, 99));
        final String pluginName    = "public-plugin";
        final JsonNode productNode = instance.findProduct(pluginName);
        CompatibilityException e = assertThrows(
                CompatibilityException.class,
                () -> instance.findVersion(productNode, "1.0"));
        assertEquals("The plugin " + pluginName +
                " has no compatible releases.", e.getMessage());
    }

    @Test
    void findVersionWithNullSpecVersion() throws Exception {
        final String pluginName    = "public-plugin-null-spec-version";
        final JsonNode productNode = instance.findProduct(pluginName);
        APIException e = assertThrows(APIException.class,
                () -> instance.findVersion(productNode, "1.0"));
        assertEquals("Version node does not have a specification version.",
                e.getMessage());
    }

    @Test
    void findVersionWithNoArtifact() throws Exception {
        final String pluginName    = "public-plugin-no-artifact";
        final JsonNode productNode = instance.findProduct(pluginName);
        ArtifactNotFoundException e = assertThrows(
                ArtifactNotFoundException.class,
                () -> instance.findVersion(productNode, "1.0"));
        assertEquals("The plugin " + pluginName + " (version 1.0) has no " +
                        "artifacts to download.",
                e.getMessage());
    }

    @Test
    void findVersionWithNullArtifactFilename() throws Exception {
        final String pluginName    = "public-plugin-null-artifact-filename";
        final JsonNode productNode = instance.findProduct(pluginName);
        APIException e = assertThrows(
                APIException.class,
                () -> instance.findVersion(productNode, "1.0"));
        assertEquals("Artifact filename property is null", e.getMessage());
    }

    @Test
    void findVersionWithNullArtifactMD5() throws Exception {
        final String pluginName    = "public-plugin-null-artifact-md5";
        final JsonNode productNode = instance.findProduct(pluginName);
        APIException e = assertThrows(
                APIException.class,
                () -> instance.findVersion(productNode, "1.0"));
        assertEquals("Artifact md5 property is null", e.getMessage());
    }

    @Test
    void findVersionWithNullArtifactURL() {
        // This is difficult to test, due to the design of
        // MockArtifactRepository, but also highly unlikely to ever happen,
        // due to the design of the real repository.
    }

    @Test
    void findVersionWithCompatibleVersion() throws Exception {
        final String pluginName    = "public-plugin";
        final JsonNode productNode = instance.findProduct(pluginName);
        assertNotNull(instance.findVersion(productNode, "1.0"));
    }

    /* downloadArtifact() */

    @Test
    void downloadArtifactWithChecksumMismatch() throws Exception {
        JsonNode productNode   = instance.findProduct("public-plugin-incorrect-checksum");
        JsonNode versionNode   = instance.findLatestCompatibleVersion(productNode);
        JsonNode artifactsNode = versionNode.get("artifacts");
        JsonNode artifactNode  = artifactsNode.get(0);
        assertThrows(ArtifactDigestException.class,
                () -> instance.downloadArtifact(artifactNode));
    }

    @Test
    void downloadArtifactWithPrivateArtifactWithoutCustomerKey() throws Exception {
        JsonNode productNode   = instance.findProduct("private-plugin");
        JsonNode versionNode   = instance.findLatestCompatibleVersion(productNode);
        JsonNode artifactsNode = versionNode.get("artifacts");
        JsonNode artifactNode  = artifactsNode.get(0);

        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        HTTPException e = assertThrows(HTTPException.class,
                () -> instance.downloadArtifact(artifactNode));
        assertEquals(Status.FORBIDDEN, e.getStatus());
    }

    @Test
    void downloadArtifactWithPrivateArtifactWithBogusCustomerKey()
            throws Exception {
        JsonNode productNode   = instance.findProduct("private-plugin");
        JsonNode versionNode   = instance.findLatestCompatibleVersion(productNode);
        JsonNode artifactsNode = versionNode.get("artifacts");
        JsonNode artifactNode  = artifactsNode.get(0);

        Configuration.forApplication().setProperty(Key.CUSTOMER_KEY, "bogus");
        HTTPException e = assertThrows(HTTPException.class,
                () -> instance.downloadArtifact(artifactNode));
        assertEquals(Status.FORBIDDEN, e.getStatus());
    }

    @Test
    void downloadArtifactWithPrivateArtifactWithCustomerKey() throws Exception {
        JsonNode productNode   = instance.findProduct("private-plugin");
        JsonNode versionNode   = instance.findLatestCompatibleVersion(productNode);
        JsonNode artifactsNode = versionNode.get("artifacts");
        JsonNode artifactNode  = artifactsNode.get(0);
        Path artifact = instance.downloadArtifact(artifactNode);
        try {
            assertTrue(Files.exists(artifact));
            assertTrue(Files.size(artifact) > 0);
        } finally {
            Files.deleteIfExists(artifact);
        }
    }

    @Test
    void downloadArtifactWithPublicArtifact() throws Exception {
        Configuration.forApplication().clearProperty(Key.CUSTOMER_KEY);
        JsonNode productNode   = instance.findProduct("public-plugin");
        JsonNode versionNode   = instance.findLatestCompatibleVersion(productNode);
        JsonNode artifactsNode = versionNode.get("artifacts");
        JsonNode artifactNode  = artifactsNode.get(0);
        Path artifact = instance.downloadArtifact(artifactNode);
        try {
            assertTrue(Files.exists(artifact));
            assertTrue(Files.size(artifact) > 0);
        } finally {
            Files.deleteIfExists(artifact);
        }
    }

}
