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

package is.galia.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.Application;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Client;
import is.galia.http.ClientFactory;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-safe error reporter.
 */
public final class Reporter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Reporter.class);

    static final String DEFAULT_BASE_URI = "https://get.bairdcreek.software";

    private final Client client = ClientFactory.newClient();

    /**
     * Can be overridden with {@link #setBaseURI(Reference)} for testing.
     */
    private Reference baseURI;

    public Reporter() {
        client.setFollowRedirects(true);
    }

    synchronized Reference getBaseURI() {
        if (baseURI == null) {
            String uri = Configuration.forApplication()
                    .getString(Key.ARTIFACT_REPOSITORY_BASE_URI, DEFAULT_BASE_URI);
            baseURI = new Reference(uri);
        }
        return baseURI;
    }

    public void reportErrorToHQ(Throwable throwable) throws IOException {
        Reference uri = getBaseURI().rebuilder().appendPath("/errors").build();
        LOGGER.debug("reportError(): POSTing to {}", uri);
        client.getHeaders().set("Content-Type", "application/json");
        client.setMethod(Method.POST);
        client.setURI(uri);
        Map<String,Object> report = generateReport(throwable);
        String json = serializeReportAsJSON(report);
        client.setEntity(json);
        Response response = client.send();
    }

    /**
     * @param throwable Error to report.
     * @return Map representation of the error.
     */
    Map<String,Object> generateReport(Throwable throwable) {
        final Runtime runtime             = Runtime.getRuntime();
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        final Map<String, Object> report  = new LinkedHashMap<>();
        report.put("timestamp", Instant.now().toString());
        report.put("customerKey",
                Configuration.forApplication().getString(Key.CUSTOMER_KEY));
        { // product
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("name", Application.getName().toString());
            product.put("version", Application.getVersion().toString());
            product.put("specVersion", Application.getSpecificationVersion().toString());
            report.put("product", product);
        }
        { // installedPlugins
            report.put("installedPlugins", PluginManager.getPlugins().stream()
                    .map(p -> Map.of(
                            "name", p.getPluginName(),
                            "version", (p.getPluginVersion() != null) ?
                                    p.getPluginVersion().toString() : "",
                            "specVersion", (p.getPluginSpecificationVersion() != null) ?
                                    p.getPluginSpecificationVersion().toString() : ""))
                    .toList());
        }
        { // jvm
            report.put("jvm", Map.of(
                    "name", System.getProperty("java.vm.name"),
                    "info", System.getProperty("java.vm.info"),
                    "vendor", System.getProperty("java.vendor"),
                    "version", System.getProperty("java.version")));
        }
        { // system
            report.put("system", Map.of(
                    "os", System.getProperty("os.name"),
                    "cores", runtime.availableProcessors(),
                    "totalHeap", runtime.totalMemory(),
                    "freeHeap", runtime.freeMemory(),
                    "maxHeap", runtime.maxMemory(),
                    "uptimeSeconds", runtimeMXBean.getUptime() / 1000));
        }
        { // error
            Map<String,Object> error = new LinkedHashMap<>();
            error.put("class", throwable.getClass().getName());
            error.put("message", throwable.getMessage());
            error.put("stackTrace", Arrays.stream(throwable.getStackTrace())
                    .map(ste -> Map.of(
                            "fileName", (ste.getFileName() != null) ? ste.getFileName() : "",
                            "classLoaderName", (ste.getClassLoaderName() != null) ? ste.getClassLoaderName() : "",
                            "className", ste.getClassName(),
                            "methodName", ste.getMethodName(),
                            "lineNumber", ste.getLineNumber())).toList());
            report.put("error", error);
        }
        return report;
    }

    /**
     * <p>Generates a JSON error report in the following format:</p>
     *
     * {@snippet lang=json
     * {
     *     "timestamp": "ISO-8601 string",
     *     "customerKey": "customer key or null",
     *     "product": {
     *         "name: "",
     *         "version": "",
     *         "specVersion": ""
     *     },
     *     "installedPlugins": [
     *         {
     *             "name": "",
     *             "version": "",
     *             "specVersion": ""
     *         }
     *     ],
     *     "jvm": {
     *         "name": "",
     *         "info": "",
     *         "vendor": "",
     *         "version": ""
     *     },
     *     "system": {
     *         "os": "",
     *         "cores": 0,
     *         "totalHeap": 0,
     *         "freeHeap": 0,
     *         "maxHeap": 0,
     *         "uptimeSeconds": 0
     *     },
     *     "error": {
     *         "class": "",
     *         "message": "",
     *         "stackTrace": [
     *             {
     *                 "fileName": "",
     *                 "classLoaderName": "",
     *                 "className": "",
     *                 "methodName": "",
     *                 "lineNumber" 0
     *             }
     *         ]
     *     }
     * }
     * }
     *
     * @param report Return value of {@link #generateReport(Throwable)}.
     * @return JSON string.
     */
    private String serializeReportAsJSON(Map<String,Object> report) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writer().writeValueAsString(report);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize error report as JSON (this is a bug): {}",
                    e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    synchronized void setBaseURI(Reference baseURI) {
        this.baseURI = baseURI;
    }

}
