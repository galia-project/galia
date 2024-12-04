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
import is.galia.plugin.Plugin;
import is.galia.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class ResourceFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ResourceFactory.class);

    /**
     * N.B.: Performance-critical resources are earlier in the list so that
     * they will be matched sooner.
     */
    private static final List<Class<? extends Resource>> BUILT_IN_RESOURCES = List.of(
            is.galia.resource.deepzoom.TileResource.class,
            is.galia.resource.deepzoom.InformationResource.class,
            is.galia.resource.iiif.v3.ImageResource.class,
            is.galia.resource.iiif.v2.ImageResource.class,
            is.galia.resource.iiif.v1.ImageResource.class,
            is.galia.resource.iiif.v3.InformationResource.class,
            is.galia.resource.iiif.v2.InformationResource.class,
            is.galia.resource.iiif.v1.InformationResource.class,
            is.galia.resource.iiif.v3.IdentifierResource.class,
            is.galia.resource.iiif.v2.IdentifierResource.class,
            is.galia.resource.iiif.v1.IdentifierResource.class,
            is.galia.resource.FileResource.class,
            is.galia.resource.health.HealthResource.class,
            is.galia.resource.LandingResource.class,
            is.galia.resource.TrailingSlashResource.class,
            is.galia.resource.api.StatusResource.class,
            is.galia.resource.api.ConfigurationResource.class,
            is.galia.resource.api.TaskResource.class,
            is.galia.resource.api.TasksResource.class);

    private static final List<Resource> RESOURCES = getAllResources();

    /**
     * @param method        Request method.
     * @param path          Full URI path.
     * @param pathArguments Mutable instance to which all path arguments will
     *                      be added.
     * @return New instance capable of handling the given path using the given
     *         HTTP method.
     */
    static Resource newResource(Method method,
                                String path,
                                List<String> pathArguments) {
        for (Resource resourceArchetype : RESOURCES) {
            for (Route route : resourceArchetype.getRoutes()) {
                if (!Method.OPTIONS.equals(method) &&
                        !route.requestMethods().contains(method)) {
                    continue;
                }
                for (Pattern pattern : route.pathPatterns()) {
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.find()) {
                        Resource resource = newResource(resourceArchetype.getClass());
                        if (resource instanceof Plugin plugin) {
                            plugin.initializePlugin();
                        }
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            pathArguments.add(matcher.group(i));
                        }
                        return resource;
                    }
                }
            }
        }
        return null;
    }

    private static Resource newResource(Class<? extends Resource> clazz) {
        try {
            return (Resource) Arrays.stream(
                    clazz.getConstructors()).findFirst().get().newInstance();
        } catch (Exception e) {
            LOGGER.error("newResource(): failed to instantiate {}: {}",
                    clazz, e.getMessage());
            return null;
        }
    }

    /**
     * @return Instances of all registered resources.
     */
    public static List<Resource> getAllResources() {
        final List<Resource> resources = new ArrayList<>();
        for (Class<? extends Resource> class_ : BUILT_IN_RESOURCES) {
            resources.add(newResource(class_));
        }
        resources.addAll(getPluginResources());
        return List.copyOf(resources); // immutable
    }

    /**
     * @return Set of instances of all resources provided by plugins. The
     *         instances have not been {@link Plugin#initializePlugin()
     *         initialized}.
     */
    public static Set<Resource> getPluginResources() {
        return PluginManager.getPlugins()
                .stream()
                .filter(Resource.class::isInstance)
                .map(p -> (Resource) p)
                .collect(Collectors.toSet());
    }

    private ResourceFactory() {}

}
