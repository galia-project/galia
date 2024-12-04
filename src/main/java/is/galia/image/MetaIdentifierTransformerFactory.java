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

package is.galia.image;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * Used to obtain new {@link MetaIdentifierTransformer}s.
 */
public final class MetaIdentifierTransformerFactory {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MetaIdentifierTransformerFactory.class);

    private static final Set<Class<?>> ALL_IMPLEMENTATIONS = Set.of(
            StandardMetaIdentifierTransformer.class,
            DelegateMetaIdentifierTransformer.class);

    public static Set<Class<?>> allImplementations() {
        return ALL_IMPLEMENTATIONS;
    }

    /**
     * @param unqualifiedName Unqualified class name.
     * @return                Qualified class name (package name + class name).
     */
    private static String getQualifiedName(String unqualifiedName) {
        return unqualifiedName.contains(".") ?
                unqualifiedName :
                MetaIdentifierTransformerFactory.class.getPackage().getName() +
                        "." + unqualifiedName;
    }

    public MetaIdentifierTransformer newInstance(Delegate delegate) {
        Configuration config = Configuration.forApplication();
        String xformerName = config.getString(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());
        try {
            return newInstance(xformerName, delegate);
        } catch (Exception e) {
            MetaIdentifierTransformer xformer =
                    new StandardMetaIdentifierTransformer();
            LOGGER.error("newInstance(): {} (falling back to returning a {})",
                    e.getMessage(), xformer.getClass().getSimpleName());
            return xformer;
        }
    }

    /**
     * Retrieves an instance by name.
     *
     * @param name     Class name. If the package name is omitted, it is
     *                 assumed to be the current package.
     * @param delegate
     * @return              Instance with the given name.
     */
    private static MetaIdentifierTransformer newInstance(String name,
                                                         Delegate delegate)
            throws ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException {
        String qualifiedName = getQualifiedName(name);
        Class<?> implClass = Class.forName(qualifiedName);
        MetaIdentifierTransformer xformer =
                (MetaIdentifierTransformer) implClass.getDeclaredConstructor().newInstance();
        if (xformer instanceof DelegateMetaIdentifierTransformer) {
            DelegateMetaIdentifierTransformer delegateXformer =
                    (DelegateMetaIdentifierTransformer) xformer;
            delegateXformer.setDelegate(delegate);
        }
        return xformer;
    }

}
