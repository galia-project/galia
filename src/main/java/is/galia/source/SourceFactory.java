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

package is.galia.source;

import is.galia.config.Configuration;
import is.galia.config.ConfigurationException;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.image.Identifier;
import is.galia.plugin.Plugin;
import is.galia.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static is.galia.source.SourceFactory.SelectionStrategy.DELEGATE_SCRIPT;

/**
 * Used to obtain an instance of a {@link Source} defined in the
 * configuration, or returned by a delegate method.
 */
public final class SourceFactory {

    /**
     * How sources are chosen by {@link #newSource}.
     */
    public enum SelectionStrategy {

        /**
         * A global source is specified using the {@link Key#SOURCE_STATIC}
         * configuration key.
         */
        STATIC,

        /**
         * A source specific to the request is acquired from the {@link
         * Delegate#getSource()} delegate method.
         */
        DELEGATE_SCRIPT

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SourceFactory.class);

    private static final Set<Class<? extends Source>> BUILT_IN_SOURCES = Set.of(
            FilesystemSource.class,
            HTTPSource.class);

    /**
     * @return Set of instances of each unique {@link Source}.
     */
    public static Set<Source> getAllSources() {
        final Set<Source> sources = new HashSet<>();
        for (Class<?> class_ : BUILT_IN_SOURCES) {
            try {
                sources.add((Source) class_.getDeclaredConstructors()[0].newInstance());
            } catch (Exception e) {
                LOGGER.error("getAllSources(): failed to instantiate {}: {}",
                        class_, e.getMessage());
            }
        }
        sources.addAll(getPluginSources());
        return sources;
    }

    /**
     * @param name Plugin name.
     * @return     Instance whose {@link Plugin#getPluginName() name} matches
     *             the given name.
     */
    static Source getPluginSourceByName(String name) {
        Source source = getPluginSources().stream()
                .map(s -> (Plugin) s)
                .filter(p -> p.getPluginName().equals(name))
                .map(p -> (Source) p)
                .findFirst()
                .orElse(null);
        if (source != null) {
            ((Plugin) source).initializePlugin();
        }
        return source;
    }

    /**
     * @return All implementations provided by plugins. The instances have not
     *         been {@link Plugin#initializePlugin() initialized}.
     */
    static Set<Source> getPluginSources() {
        return PluginManager.getPlugins()
                .stream()
                .filter(Source.class::isInstance)
                .map(p -> (Source) p)
                .collect(Collectors.toSet());
    }

    /**
     * @return How sources are chosen by {@link #newSource}.
     */
    public static SelectionStrategy getSelectionStrategy() {
        final Configuration config = Configuration.forApplication();
        return config.getBoolean(Key.SOURCE_DELEGATE, false) ?
                DELEGATE_SCRIPT : SelectionStrategy.STATIC;
    }

    /**
     * @param unqualifiedName Unqualified class name.
     * @return                Qualified class name (package name + class name).
     */
    private static String qualifyName(String unqualifiedName) {
        return unqualifiedName.contains(".") ?
                unqualifiedName :
                SourceFactory.class.getPackage().getName() + "." +
                        unqualifiedName;
    }

    /**
     * Retrieves an instance by name.
     *
     * @param name Class name of the built-in source, or plugin name of the
     *             plugin source.
     * @return     Instance with the given name.
     */
    public static Source newSource(final String name)
            throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Source source = getPluginSourceByName(name);
        if (source == null) {
            String qualifiedName = qualifyName(name);
            Class<?> implClass = Class.forName(qualifiedName);
            source = (Source) implClass.getDeclaredConstructor().newInstance();
        }
        return source;
    }

    /**
     * <p>If {@link Key#SOURCE_STATIC} is not set, uses a delegate method to
     * return an instance of a source for the given identifier. Otherwise,
     * returns an instance of the source specified in {@link
     * Key#SOURCE_STATIC}.</p>
     *
     * <p>Sources are matched in the following order:</p>
     *
     * <ol>
     *     <li>Plugin sources with matching {@link Plugin#getPluginName()
     *     names}</li>
     *     <li>Built-in sources with matching simple names</li>
     *     <li>Built-in sources with matching fully qualified names</li>
     * </ol>
     *
     * @param identifier Identifier of the source image.
     * @param delegate   Delegate. May be {@code null} if {@link
     *                   #getSelectionStrategy()} returns {@link
     *                   SelectionStrategy#STATIC}.
     * @return Instance of the appropriate source for the given identifier,
     *         with identifier already set.
     * @throws IllegalArgumentException if the {@literal proxy} argument is
     *                                  {@code null} while using {@link
     *                                  SelectionStrategy#DELEGATE_SCRIPT}.
     */
    public static Source newSource(Identifier identifier,
                                   Delegate delegate) throws Exception {
        switch (getSelectionStrategy()) {
            case DELEGATE_SCRIPT:
                if (delegate == null) {
                    throw new IllegalArgumentException("The " +
                            Delegate.class.getSimpleName() +
                            " argument must be non-null when using " +
                            getSelectionStrategy() + ".");
                }
                Source source = newDynamicSource(identifier, delegate);
                LOGGER.debug("Delegate returned a {} for {}",
                        source.getClass().getSimpleName(),
                        identifier);
                return source;
            default:
                final Configuration config = Configuration.forApplication();
                final String sourceName = config.getString(Key.SOURCE_STATIC);
                if (sourceName != null) {
                    return newSource(sourceName, identifier, delegate);
                } else {
                    throw new ConfigurationException(Key.SOURCE_STATIC +
                            " is not set to a valid source.");
                }
        }
    }

    private static Source newSource(
            String name,
            Identifier identifier,
            Delegate delegate) throws ClassNotFoundException,
            NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Source source = newSource(name);
        source.setIdentifier(identifier);
        source.setDelegate(delegate);
        return source;
    }

    /**
     * @param identifier Identifier to return a source for.
     * @param delegate   Delegate from which to acquire the source name.
     * @return           Source as returned from the given delegate.
     * @throws IOException if the lookup script configuration key is undefined.
     */
    private static Source newDynamicSource(Identifier identifier,
                                           Delegate delegate) throws Exception {
        return newSource(delegate.getSource(), identifier, delegate);
    }

    private SourceFactory() {}

}
