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

package is.galia.resource;

import is.galia.http.Method;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceFactoryTest extends BaseTest {

    /* newResource() */

    @Test
    void newResourceWithNoMatchingMethod() {
        assertNull(ResourceFactory.newResource(Method.PUT, "/admin", new ArrayList<>()));
    }

    @Test
    void newResourceWithOPTIONSMethod() {
        assertInstanceOf(is.galia.resource.api.TasksResource.class,
                ResourceFactory.newResource(Method.OPTIONS, "/tasks", new ArrayList<>()));
    }

    @Test
    void newResourceWithNoMatchingPath() {
        assertNull(ResourceFactory.newResource(Method.GET, "/cats", new ArrayList<>()));
    }

    @Test
    void newResourceWithBuiltInResources() {
        assertInstanceOf(FileResource.class,
                ResourceFactory.newResource(Method.GET, "/static/", new ArrayList<>()));
        assertInstanceOf(LandingResource.class,
                ResourceFactory.newResource(Method.GET, "", new ArrayList<>()));
        assertInstanceOf(LandingResource.class,
                ResourceFactory.newResource(Method.GET, "/", new ArrayList<>()));
        assertInstanceOf(TrailingSlashResource.class,
                ResourceFactory.newResource(Method.GET, "/cats/", new ArrayList<>()));

        assertInstanceOf(is.galia.resource.api.ConfigurationResource.class,
                ResourceFactory.newResource(Method.GET, "/configuration", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.api.StatusResource.class,
                ResourceFactory.newResource(Method.GET, "/status", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.api.TaskResource.class,
                ResourceFactory.newResource(Method.GET, "/tasks/cats", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.api.TasksResource.class,
                ResourceFactory.newResource(Method.POST, "/tasks", new ArrayList<>()));

        assertInstanceOf(is.galia.resource.health.HealthResource.class,
                ResourceFactory.newResource(Method.GET, "/health", new ArrayList<>()));

        assertInstanceOf(is.galia.resource.deepzoom.TileResource.class,
                ResourceFactory.newResource(Method.GET, "/dzi/cats_files/0/0_0.jpg", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.deepzoom.InformationResource.class,
                ResourceFactory.newResource(Method.GET, "/dzi/cats.dzi", new ArrayList<>()));

        assertInstanceOf(is.galia.resource.iiif.v1.IdentifierResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/1/cats", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.iiif.v1.ImageResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/1/cats/full/full/0/native.jpg", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.iiif.v1.InformationResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/1/cats/info.json", new ArrayList<>()));

        assertInstanceOf(is.galia.resource.iiif.v2.IdentifierResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/2/cats", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.iiif.v2.ImageResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/2/cats/full/full/0/default.jpg", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.iiif.v2.InformationResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/2/cats/info.json", new ArrayList<>()));

        assertInstanceOf(is.galia.resource.iiif.v3.IdentifierResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/3/cats", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.iiif.v3.ImageResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/3/cats/full/max/0/default.jpg", new ArrayList<>()));
        assertInstanceOf(is.galia.resource.iiif.v3.InformationResource.class,
                ResourceFactory.newResource(Method.GET, "/iiif/3/cats/info.json", new ArrayList<>()));
    }

    @Test
    void newResourceWithPluginResource() {
        final List<String> params = new ArrayList<>();
        Resource resource =
                ResourceFactory.newResource(Method.GET, "/MockResourcePlugin", params);
        assertNotNull(resource);
    }

    @Test
    void newResourceWithUnmodifiableListArgument() {
        assertThrows(UnsupportedOperationException.class,
                () -> ResourceFactory.newResource(Method.GET, "/tasks/cats", List.of()));
    }

    @Test
    void newResourceExtractsPathMatchGroups() {
        final List<String> params = new ArrayList<>();
        {
            ResourceFactory.newResource(Method.GET, "/tasks/cats", params);
            assertEquals(1, params.size());
            assertEquals("cats", params.getFirst());
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/1/cats", params);
            assertEquals(1, params.size());
            assertEquals("cats", params.getFirst());
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/1/cats/full/full/0/native.jpg", params);
            assertEquals(6, params.size());
            assertEquals("cats", params.get(0));
            assertEquals("full", params.get(1));
            assertEquals("full", params.get(2));
            assertEquals("0", params.get(3));
            assertEquals("native", params.get(4));
            assertEquals("jpg", params.get(5));
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/1/cats/info.json", params);
            assertEquals(1, params.size());
            assertEquals("cats", params.getFirst());
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/2/cats", params);
            assertEquals(1, params.size());
            assertEquals("cats", params.getFirst());
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/2/cats/full/full/0/default.jpg", params);
            assertEquals(6, params.size());
            assertEquals("cats", params.get(0));
            assertEquals("full", params.get(1));
            assertEquals("full", params.get(2));
            assertEquals("0", params.get(3));
            assertEquals("default", params.get(4));
            assertEquals("jpg", params.get(5));
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/2/cats/info.json", params);
            assertEquals(1, params.size());
            assertEquals("cats", params.getFirst());
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/3/cats", params);
            assertEquals(1, params.size());
            assertEquals("cats", params.getFirst());
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/3/cats/full/max/0/default.jpg", params);
            assertEquals(6, params.size());
            assertEquals("cats", params.get(0));
            assertEquals("full", params.get(1));
            assertEquals("max", params.get(2));
            assertEquals("0", params.get(3));
            assertEquals("default", params.get(4));
            assertEquals("jpg", params.get(5));
        }
        params.clear();
        {
            ResourceFactory.newResource(Method.GET, "/iiif/3/cats/info.json", params);
            assertEquals(1, params.size());
            assertEquals("cats", params.getFirst());
        }
    }

    @Test
    void newResourceInitializesPlugin() {
        final List<String> params = new ArrayList<>();
        MockResourcePlugin resource = (MockResourcePlugin)
                ResourceFactory.newResource(Method.GET, "/MockResourcePlugin", params);
        assertTrue(resource.isInitialized);
    }

    /* getAllResources() */

    @Test
    void getAllResources() {
        assertTrue(ResourceFactory.getAllResources().size() > 1);
    }

    /* getPluginResources() */

    @Test
    void getPluginResources() {
        assertEquals(1, ResourceFactory.getPluginResources().size());
    }

}
