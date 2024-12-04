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
import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Client;
import is.galia.http.ClientFactory;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;
import is.galia.http.Status;
import is.galia.util.SoftwareVersion;
import is.galia.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Objects;

/**
 * <p>High-level artifact repository client.</p>
 *
 * <h3>Usage</h3>
 *
 * <ol>
 *     <li>{@link #findProduct(String) Find a plugin}</li>
 *     <li>Find a version using either {@link #findVersion(JsonNode, String)}
 *     or {@link #findLatestCompatibleVersion(JsonNode)}</li>
 *     <li>{@link #downloadArtifact(JsonNode) Download that version's
 *     artifact}</li>
 * </ol>
 */
final class ArtifactRepositoryClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ArtifactRepositoryClient.class);

    static final Reference DEFAULT_BASE_URI =
            new Reference("https://get.bairdcreek.software");

    /**
     * Can be overridden by {@link
     * #setApplicationSpecificationVersion(SoftwareVersion)} for testing.
     */
    private SoftwareVersion appSpecVersion =
            Application.getSpecificationVersion();

    private final Client client = ClientFactory.newClient();

    /**
     * Can be overridden with {@link #setBaseURI(Reference)} for
     * testing.
     */
    private Reference baseURI;

    /**
     * @return Value of {@link Key#CUSTOMER_KEY} in the application
     *         configuration.
     */
    private static String getCustomerKey() {
        Configuration config = Configuration.forApplication();
        return config.getString(Key.CUSTOMER_KEY, "");
    }

    ArtifactRepositoryClient() {
        client.setFollowRedirects(true);
    }

    Reference getBaseURI() {
        if (baseURI != null) {
            return baseURI;
        }
        String uri = Configuration.forApplication().getString(Key.ARTIFACT_REPOSITORY_BASE_URI);
        if (uri != null) {
            return new Reference(uri);
        }
        return DEFAULT_BASE_URI;
    }

    /**
     * N.B.: The {@code GET} request for this URI must include an {@code
     * Authorization} header with a value of {@code Bearer <customer key>}.
     *
     * @return URI at which the customer's JSON plugin index is hosted.
     */
    private Reference getCustomerAccountURI() {
        return getBaseURI().rebuilder().appendPath("/my-account").build();
    }

    private Reference getPublicPluginIndexURI() {
        return getBaseURI().rebuilder().appendPath("/products/galia").build();
    }

    private JsonNode fetchAccountInventory()
            throws IOException, AccountNotFoundException {
        final String customerKey = getCustomerKey();
        if (customerKey.isBlank()) {
            throw new AccountNotFoundException("Customer key is not set");
        }
        final Reference uri = getCustomerAccountURI();
        client.getHeaders().set("Accept", "application/json");
        client.getHeaders().set("Authorization", "Bearer " + customerKey);
        client.setURI(uri);
        LOGGER.debug("Fetching customer account inventory");
        try (Response response = client.send()) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());
        } catch (HTTPException e) {
            if (Status.FORBIDDEN.equals(e.getStatus())) {
                throw new AccountNotFoundException(
                        "Account not found. Check the value of your " +
                                Key.CUSTOMER_KEY + " configuration key. " +
                                "If you don't have a key, leave it blank.");
            }
            throw e;
        }
    }

    private JsonNode fetchPublicPluginIndex() throws IOException {
        final Reference uri = getPublicPluginIndexURI();
        client.getHeaders().set("Accept", "application/json");
        client.setURI(uri);
        LOGGER.debug("Fetching public plugin index");
        try (Response response = client.send()) {
            ObjectMapper mapper = new ObjectMapper();
            String body = response.getBodyAsString();
            return mapper.readTree(body);
        }
    }

    /**
     * <p>Tries to find the JSON node of the plugin with the given name. If
     * {@link Key#CUSTOMER_KEY} is set in the application configuration, the
     * first resort is to search the account inventory. If not, or as a last
     * resort, the public plugin index is searched.</p>
     *
     * <p>The returned product node is not validated, i.e. it might not have
     * any compatible versions, artifacts, etc.</p>
     *
     * @param pluginName Plugin/product name.
     * @return Product node, or {@code null} if not found.
     */
    JsonNode findProduct(String pluginName)
            throws PluginException, IOException {
        JsonNode root;
        Iterator<JsonNode> productList;
        JsonNode productNode = null;
        String customerKey   = getCustomerKey();
        if (!customerKey.isBlank()) {
            root        = fetchAccountInventory();
            productList = root.get("licenses").elements();
            LOGGER.debug("Searching for " + pluginName +
                    " in the account inventory");
            productNode = findProductInList(pluginName, productList);
        }
        if (productNode == null) {
            root        = fetchPublicPluginIndex();
            productList = root.get("plugins").elements();
            LOGGER.debug("Searching for " + pluginName +
                    " in the public plugin index");
            productNode = findProductInList(pluginName, productList);
        }
        return productNode;
    }

    /**
     * @param pluginName Plugin/product name.
     * @param plugins    List of plugin nodes from the repository.
     * @return           Product node.
     */
    private JsonNode findProductInList(String pluginName,
                                       Iterator<JsonNode> plugins) {
        Objects.requireNonNull(pluginName);
        while (plugins.hasNext()) {
            JsonNode node = plugins.next();
            String productName = node.get("name").textValue().trim();
            if (pluginName.equalsIgnoreCase(productName)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Finds the latest version with a compatible specification version.
     * The version's properties are validated to ensure that it has a
     * compatible artifact.
     *
     * @param productNode Product/plugin node.
     * @return            Version node from the repository, which is guaranteed
     *                    to have a valid and compatible artifact node
     *                    attached.
     */
    JsonNode findLatestCompatibleVersion(JsonNode productNode)
            throws PluginException {
        Objects.requireNonNull(productNode);
        final String pluginName     = productNode.get("name").textValue();
        LOGGER.debug("Finding the latest compatible version of {}", pluginName);
        Iterator<JsonNode> versions = productNode.get("versions").elements();
        if (!versions.hasNext()) {
            throw new VersionNotFoundException(
                    "No releases found for plugin: " + pluginName);
        }
        JsonNode versionNode          = null;
        PluginException lastException = null;
        while (versions.hasNext()) {
            JsonNode candidateNode = versions.next();
            try {
                validateVersion(pluginName, candidateNode);
                versionNode = candidateNode;
                break;
            } catch (PluginException e) {
                LOGGER.debug("{} version {}: {}",
                        pluginName, candidateNode.get("name"), e.getMessage());
                lastException = e;
            }
        }

        if (versionNode == null) {
            throw lastException;
        }
        LOGGER.debug("Latest compatible version of {}: {}",
                pluginName, versionNode.get("name").textValue());
        return versionNode;
    }

    /**
     * Finds a specific version. The version's properties are validated to
     * ensure that it has a compatible artifact.
     *
     * @param productNode      Product/plugin node.
     * @param requestedVersion Requested product/plugin version.
     * @return                 Version node from the repository, which is
     *                         guaranteed to have a valid and compatible
     *                         artifact node attached.
     */
    JsonNode findVersion(JsonNode productNode,
                         String requestedVersion) throws PluginException {
        final String pluginName     = productNode.get("name").textValue();
        LOGGER.debug("Evaluating version {} of {}",
                requestedVersion, pluginName);
        Iterator<JsonNode> versions = productNode.get("versions").elements();
        if (!versions.hasNext()) {
            throw new VersionNotFoundException(
                    "No releases found for plugin: " + pluginName);
        }
        JsonNode versionNode = null;
        while (versions.hasNext()) {
            JsonNode candidateNode = versions.next();
            String versionName     = candidateNode.get("name").textValue();
            if (requestedVersion.equals(versionName)) {
                versionNode = candidateNode;
                break;
            }
        }
        if (versionNode == null) {
            throw new VersionNotFoundException(
                    "The plugin " + pluginName + " does not have a " +
                            requestedVersion + " version.");
        }
        validateVersion(pluginName, versionNode);
        return versionNode;
    }

    /**
     * Finds the best-match artifact from the given array, which is simply the
     * one with a {@code .zip} extension. As of this writing, there are no
     * artifact lists that contain more than one artifact, and they all end in
     * {@code .zip}.
     *
     * @param artifactsNode {@code artifacts} array node.
     * @return              Artifact node from the repository.
     */
    JsonNode findArtifact(JsonNode artifactsNode) {
        Iterator<JsonNode> it = artifactsNode.elements();
        JsonNode artifactNode = null;
        while (it.hasNext()) {
            artifactNode = it.next();
            String filename = artifactNode.get("filename").textValue();
            if (filename != null && filename.toLowerCase().endsWith(".zip")) {
                break;
            }
        }
        return artifactNode;
    }

    /**
     * Checks whether the given version node has a compatible specification
     * version and valid, downloadable artifact.
     */
    private void validateVersion(String pluginName,
                                 JsonNode versionNode) throws PluginException {
        final String versionName = versionNode.get("name").textValue();
        final String specVersion = versionNode.get("spec_version").textValue();
        if (specVersion == null) {
            throw new APIException(
                    "Version node does not have a specification version.");
        } else if (!isCompatible(specVersion)) {
            throw new CompatibilityException(
                    "The plugin " + pluginName + " has no compatible releases.");
        }

        final JsonNode artifactsNode = versionNode.get("artifacts");
        if (artifactsNode == null || artifactsNode.isNull() || artifactsNode.isEmpty()) {
            throw new ArtifactNotFoundException("The plugin " + pluginName +
                    " (version " + versionName + ") has no artifacts to download.");
        }

        // Currently, there is only one artifact.
        final JsonNode artifactNode = findArtifact(artifactsNode);
        final String filename       = artifactNode.get("filename").textValue();
        final String md5            = artifactNode.get("md5").textValue();
        final String url            = artifactNode.get("url").textValue();
        if (filename == null) {
            throw new APIException("Artifact filename property is null");
        } else if (md5 == null) {
            throw new APIException("Artifact md5 property is null");
        } else if (url == null) {
            throw new APIException("Artifact url property is null");
        }
    }

    /**
     * @return Path to the downloaded file.
     */
    Path downloadArtifact(JsonNode artifactNode)
            throws IOException, ArtifactDigestException {
        final String filename    = artifactNode.get("filename").textValue();
        final String md5         = artifactNode.get("md5").textValue();
        final String url         = artifactNode.get("url").textValue();
        final Path pluginsDir    = PluginManager.getPluginsDir();
        final Path pluginZipFile = pluginsDir.resolve(filename);
        LOGGER.debug("Downloading {} from {} to {}",
                filename, url, pluginZipFile);

        client.setURI(new Reference(url));
        if (!getCustomerKey().isBlank()) {
            client.getHeaders().set("Authorization", "Bearer " + getCustomerKey());
        } else {
            client.getHeaders().removeAll("Authorization");
        }
        try (Response response = client.send()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (DigestInputStream inputStream =
                         new DigestInputStream(response.getBodyAsStream(), md);
                 OutputStream outputStream =
                         Files.newOutputStream(pluginZipFile)) {
                inputStream.transferTo(outputStream);
            }
            String checksum = StringUtils.toHex(md.digest()).toLowerCase();
            if (!checksum.equals(md5)) {
                Files.delete(pluginZipFile);
                throw new ArtifactDigestException(filename);
            }
            return pluginZipFile;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isCompatible(String specVersion) {
        SoftwareVersion pluginSpecVersion = SoftwareVersion.parse(specVersion);
        return (appSpecVersion.major() == pluginSpecVersion.major() &&
                appSpecVersion.minor() >= pluginSpecVersion.minor());
    }

    void setApplicationSpecificationVersion(SoftwareVersion version) {
        this.appSpecVersion = version;
    }

    void setBaseURI(Reference baseURI) {
        this.baseURI = baseURI;
    }

}
