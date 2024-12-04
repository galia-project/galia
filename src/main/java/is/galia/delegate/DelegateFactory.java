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

package is.galia.delegate;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.plugin.Plugin;
import is.galia.plugin.PluginManager;
import is.galia.resource.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to obtain {@link Delegate} instances.
 */
public final class DelegateFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateFactory.class);

    /**
     * Returns all installed delegates. Note that only one delegate is supposed
     * to be installed at a time.
     *
     * @return Set of {@link Delegate}s.
     */
    private static Set<Delegate> getPluginDelegates() {
        return PluginManager.getPlugins()
                .stream()
                .filter(Delegate.class::isInstance)
                .map(p -> (Delegate) p)
                .collect(Collectors.toSet());
    }

    /**
     * @return Whether a delegate is available, i.e. whether {@link
     *         #newDelegate(RequestContext)} will return one.
     */
    public static boolean isDelegateAvailable() {
        return isDelegateEnabled() &&
                PluginManager.getPlugins().stream()
                        .anyMatch(Delegate.class::isInstance);
    }

    /**
     * @return Whether the delegate system is enabled. This is always {@code
     *         true} except in testing.
     */
    private static boolean isDelegateEnabled() {
        Configuration config = Configuration.forApplication();
        return config.getBoolean(Key.DELEGATE_ENABLED, true);
    }

    /**
     * <p>Returns a new {@link Delegate} instance.</p>
     *
     * <p>Normally this would be called only once at the beginning of a request
     * lifecycle, and the returned instance passed around wherever it is
     * needed.</p>
     *
     * @param context Request context.
     * @return        New instance.
     * @throws DelegateException if there was an error parsing the script,
     *         instantiating a delegate object, etc.
     */
    public static Delegate newDelegate(RequestContext context)
            throws DelegateException {
        if (!isDelegateEnabled()) {
            throw new DelegateNotAvailableException();
        }
        Set<Delegate> delegates = getPluginDelegates();
        if (!delegates.isEmpty()) {
            Delegate delegate = delegates.iterator().next();
            if (delegates.size() > 1) {
                LOGGER.warn("There are {} different delegates installed. " +
                                "{} has been randomly chosen to handle this " +
                                "request. Please choose only one delegate " +
                                "to use and uninstall all the others.",
                        delegates.size(), delegate.getClass().getSimpleName());
            }
            ((Plugin) delegate).initializePlugin();
            delegate.setRequestContext(context);
            return delegate;
        }
        throw new DelegateNotAvailableException();
    }

    private DelegateFactory() {}

}
