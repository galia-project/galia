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

package is.galia.cache;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.plugin.Plugin;
import is.galia.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to obtain {@link Cache} instances according to the application
 * configuration.
 */
public final class CacheFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CacheFactory.class);

    private static final Set<Class<? extends VariantCache>> BUILT_IN_VARIANT_CACHES = Set.of(
            FilesystemCache.class,
            HeapCache.class);
    private static final Set<Class<? extends InfoCache>> BUILT_IN_INFO_CACHES = Set.of(
            FilesystemCache.class,
            HeapCache.class);

    /**
     * Initialized by {@link #getVariantCache()}.
     */
    private static volatile VariantCache variantCache;

    /**
     * Initialized by {@link #getInfoCache()}.
     */
    private static volatile InfoCache infoCache;

    /**
     * @return Set of instances of all available info caches.
     */
    public static Set<InfoCache> getAllInfoCaches() {
        final Set<InfoCache> caches = new HashSet<>();
        for (Class<?> class_ : BUILT_IN_INFO_CACHES) {
            try {
                caches.add((InfoCache) class_.getDeclaredConstructors()[0].newInstance());
            } catch (Exception e) {
                LOGGER.error("getAllInfoCaches(): failed to instantiate {}: {}",
                        class_, e.getMessage());
            }
        }
        return caches;
    }

    /**
     * @param name Plugin name.
     * @return     Instance whose {@link Plugin#getPluginName() name} matches
     *             the given name.
     */
    static InfoCache getPluginInfoCacheByName(String name) {
        InfoCache cache = getPluginInfoCaches().stream()
                .map(s -> (Plugin) s)
                .filter(p -> p.getPluginName().equals(name))
                .map(p -> (InfoCache) p)
                .findFirst()
                .orElse(null);
        if (cache != null) {
            ((Plugin) cache).initializePlugin();
            cache.initialize();
        }
        return cache;
    }

    /**
     * @return Set of instances of all info caches provided by plugins. The
     *         instances have not been {@link Plugin#initializePlugin()
     *         initialized}.
     */
    public static Set<InfoCache> getPluginInfoCaches() {
        return PluginManager.getPlugins()
                .stream()
                .filter(InfoCache.class::isInstance)
                .map(p -> (InfoCache) p)
                .collect(Collectors.toSet());
    }

    /**
     * @return Set of instances of all available variant caches.
     */
    public static Set<VariantCache> getAllVariantCaches() {
        final Set<VariantCache> caches = new HashSet<>();
        for (Class<?> class_ : BUILT_IN_VARIANT_CACHES) {
            try {
                caches.add((VariantCache) class_.getDeclaredConstructors()[0].newInstance());
            } catch (Exception e) {
                LOGGER.error("getAllVariantCaches(): failed to instantiate {}: {}",
                        class_, e.getMessage());
            }
        }
        return caches;
    }

    /**
     * @param name Plugin name.
     * @return     Instance whose {@link Plugin#getPluginName() name} matches
     *             the given name.
     */
    static VariantCache getPluginVariantCacheByName(String name) {
        VariantCache cache = getPluginVariantCaches().stream()
                .map(s -> (Plugin) s)
                .filter(p -> p.getPluginName().equals(name))
                .map(p -> (VariantCache) p)
                .findFirst()
                .orElse(null);
        if (cache != null) {
            ((Plugin) cache).initializePlugin();
            cache.initialize();
        }
        return cache;
    }

    /**
     * @return Set of instances of all variant caches provided by plugins.
     */
    public static Set<VariantCache> getPluginVariantCaches() {
        return PluginManager.getPlugins()
                .stream()
                .filter(VariantCache.class::isInstance)
                .map(p -> (VariantCache) p)
                .collect(Collectors.toSet());
    }

    /**
     * <p>Provides access to the shared {@link InfoCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared instance, or {@code null} if a variant cache is not
     *         available.
     */
    public static Optional<InfoCache> getInfoCache() {
        InfoCache cache = null;
        if (isInfoCacheEnabled()) {
            final Configuration config = Configuration.forApplication();
            final String unqualifiedName = config.getString(Key.INFO_CACHE, "");

            if (!unqualifiedName.isEmpty()) {
                final String qualifiedName = qualifyName(unqualifiedName);
                cache = infoCache;
                if (cache == null ||
                        !cache.getClass().getName().equals(qualifiedName)) {
                    synchronized (CacheFactory.class) {
                        if (cache == null ||
                                !cache.getClass().getName().equals(qualifiedName)) {
                            LOGGER.debug("getInfoCache(): " +
                                    "implementation changed; creating a new " +
                                    "instance");
                            try {
                                cache = newInfoCache(qualifiedName);
                                setInfoCache(cache);
                            } catch (ClassNotFoundException e) {
                                cache = null;
                                LOGGER.error("Class not found: {}", e.getMessage());
                            } catch (Exception e) {
                                cache = null;
                                LOGGER.error(e.getMessage());
                            }
                        }
                    }
                }
            } else {
                LOGGER.warn("Info cache is enabled, but {} is not set",
                        Key.INFO_CACHE);
                shutdownInfoCache();
            }
        }
        return Optional.ofNullable(cache);
    }

    /**
     * <p>Provides access to the shared {@link VariantCache} instance.</p>
     *
     * <p>This method respects live changes in application configuration.</p>
     *
     * @return The shared instance, or {@code null} if a variant cache is not
     *         available.
     */
    public static Optional<VariantCache> getVariantCache() {
        VariantCache cache = null;
        if (isVariantCacheEnabled()) {
            final Configuration config = Configuration.forApplication();
            final String unqualifiedName = config.getString(Key.VARIANT_CACHE, "");

            if (!unqualifiedName.isEmpty()) {
                final String qualifiedName = qualifyName(unqualifiedName);
                cache = variantCache;
                if (cache == null ||
                        !cache.getClass().getName().equals(qualifiedName)) {
                    synchronized (CacheFactory.class) {
                        if (cache == null ||
                                !cache.getClass().getName().equals(qualifiedName)) {
                            LOGGER.debug("getVariantCache(): " +
                                    "implementation changed; creating a new " +
                                    "instance");
                            try {
                                cache = newVariantCache(qualifiedName);
                                setVariantCache(cache);
                            } catch (ClassNotFoundException e) {
                                cache = null;
                                LOGGER.error("Class not found: {}", e.getMessage());
                            } catch (Exception e) {
                                cache = null;
                                LOGGER.error(e.getMessage());
                            }
                        }
                    }
                }
            } else {
                LOGGER.warn("Variant cache is enabled, but {} is not set",
                        Key.VARIANT_CACHE);
                shutdownVariantCache();
            }
        }
        return Optional.ofNullable(cache);
    }

    private static String qualifyName(String unqualifiedName) {
        return unqualifiedName.contains(".") ?
                unqualifiedName :
                CacheFactory.class.getPackage().getName() + "." +
                        unqualifiedName;
    }

    private static boolean isInfoCacheEnabled() {
        final Configuration config = Configuration.forApplication();
        return config.getBoolean(Key.INFO_CACHE_ENABLED, false);
    }

    private static boolean isVariantCacheEnabled() {
        final Configuration config = Configuration.forApplication();
        return config.getBoolean(Key.VARIANT_CACHE_ENABLED, false);
    }

    private static InfoCache newInfoCache(String name)
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException,
            ClassNotFoundException {
        InfoCache cache = getPluginInfoCacheByName(name);
        if (cache == null) {
            final String qualifiedName = qualifyName(name);
            Class<?> class_ = Class.forName(qualifiedName);
            cache = (InfoCache) class_.getDeclaredConstructor().newInstance();
        }
        return cache;
    }

    private static VariantCache newVariantCache(String name)
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException,
            ClassNotFoundException {
        VariantCache cache = getPluginVariantCacheByName(name);
        if (cache == null) {
            final String qualifiedName = qualifyName(name);
            Class<?> class_ = Class.forName(qualifiedName);
            cache = (VariantCache) class_.getDeclaredConstructor().newInstance();
        }
        return cache;
    }

    /**
     * Shuts down any existing info cache, then sets the current info cache to
     * the given instance and initializes it.
     *
     * @param cache Info cache to use.
     */
    private static synchronized void setInfoCache(InfoCache cache) {
        if (infoCache != null) {
            LOGGER.debug("setInfoCache(): shutting down the current instance");
            infoCache.shutdown();
        }

        infoCache = cache;

        LOGGER.debug("setInfoCache(): initializing the new instance");
        infoCache.initialize();
    }

    /**
     * Shuts down any existing variant cache, then sets the current variant
     * cache to the given instance and initializes it.
     *
     * @param cache Variant cache to use.
     */
    private static synchronized void setVariantCache(VariantCache cache) {
        if (variantCache != null) {
            LOGGER.debug("setVariantCache(): shutting down the current instance");
            variantCache.shutdown();
        }

        variantCache = cache;

        LOGGER.debug("setVariantCache(): initializing the new instance");
        variantCache.initialize();
    }

    public static synchronized void shutdownCaches() {
        shutdownInfoCache();
        shutdownVariantCache();
    }

    private static synchronized void shutdownInfoCache() {
        LOGGER.debug("Shutting down the info cache");
        if (infoCache != null) {
            infoCache.shutdown();
            infoCache = null;
        }
    }

    private static synchronized void shutdownVariantCache() {
        LOGGER.debug("Shutting down the variant cache");
        if (variantCache != null) {
            variantCache.shutdown();
            variantCache = null;
        }
    }

    private CacheFactory() {}

}
