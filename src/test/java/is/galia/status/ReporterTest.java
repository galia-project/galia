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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Reference;
import is.galia.plugin.repository.MockArtifactRepository;
import is.galia.test.BaseTest;
import is.galia.util.SocketUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReporterTest extends BaseTest {

    private Reporter instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Reporter();
    }

    /* reportErrorToHQ() */

    @Test
    void reportErrorToHQAgainstUnknownHost() {
        instance.setBaseURI(new Reference("http://bogus.example.org"));
        Exception error = new RuntimeException();
        assertThrows(ConnectException.class,
                () -> instance.reportErrorToHQ(error));
    }

    @Test
    void reportErrorToHQAgainstClosedPort() {
        instance.setBaseURI(new Reference("http://localhost:" + SocketUtils.getOpenPort()));
        Exception error = new RuntimeException();
        assertThrows(ConnectException.class,
                () -> instance.reportErrorToHQ(error));
    }

    @Test
    void reportErrorToHQAgainstWorkingServer() throws Exception {
        MockArtifactRepository repo = new MockArtifactRepository();
        try {
            repo.start();
            instance.setBaseURI(repo.getURI());
            Exception error = new RuntimeException();
            instance.reportErrorToHQ(error);
        } finally {
            repo.stop();
        }
    }

    /* generateReport() */

    @SuppressWarnings("unchecked")
    @Test
    void generateReport() {
        Exception e = new RuntimeException("Test exception");
        Map<String,Object> report = instance.generateReport(e);
        assertEquals(report.get("customerKey"),
                Configuration.forApplication().getString(Key.CUSTOMER_KEY));
        assertTrue(((String) report.get("timestamp")).endsWith("Z"));
        {
            List<Map<String, Object>> plugins =
                    (List<Map<String, Object>>) report.get("installedPlugins");
            assertFalse(plugins.isEmpty());
            Map<String,Object> plugin = plugins.getFirst();
            assertNotNull(plugin.get("name"));
            assertNotNull(plugin.get("specVersion"));
            assertNotNull(plugin.get("version"));
        }
        {
            Map<String,Object> product = (Map<String,Object>) report.get("product");
            assertNotNull(product.get("name"));
            assertNotNull(product.get("specVersion"));
            assertNotNull(product.get("version"));
        }
        {
            Map<String, Object> jvm = (Map<String, Object>) report.get("jvm");
            assertNotNull(jvm.get("vendor"));
            assertNotNull(jvm.get("info"));
            assertNotNull(jvm.get("name"));
            assertNotNull(jvm.get("version"));
        }
        {
            Map<String, Object> system = (Map<String, Object>) report.get("system");
            assertNotNull(system.get("cores"));
            assertNotNull(system.get("uptimeSeconds"));
            assertNotNull(system.get("freeHeap"));
            assertNotNull(system.get("maxHeap"));
            assertNotNull(system.get("totalHeap"));
            assertNotNull(system.get("os"));
        }
        {
            Map<String, Object> error = (Map<String, Object>) report.get("error");
            assertEquals(e.getClass().getName(), error.get("class"));
            assertEquals(e.getMessage(), error.get("message"));
            List<Map<String,Object>> stack = (List<Map<String,Object>>) error.get("stackTrace");
            Map<String,Object> frame = stack.getFirst();
            assertNotNull(frame.get("classLoaderName"));
            assertNotNull(frame.get("fileName"));
            assertNotNull(frame.get("className"));
            assertNotNull(frame.get("methodName"));
            assertNotNull(frame.get("lineNumber"));
        }
    }

}
