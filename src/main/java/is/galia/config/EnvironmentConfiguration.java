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

package is.galia.config;

import is.galia.Application;
import is.galia.util.StringUtils;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * <p>Read-only configuration backed by the environment.</p>
 *
 * <p>To make key names environment-safe, they are converted to uppercase,
 * prefixed with the application name, and all non-alphanumerics are replaced
 * with underscores. Example:</p>
 *
 * <pre>this_is_a_key.name &rArr; GALIA_THIS_IS_A_KEY_NAME</pre>
 */
class EnvironmentConfiguration implements Configuration {

    static final String KEY_PREFIX = Application.getName() + "_";
    private static final String ENVIRONMENT_KEY_REPLACEMENT = "[^A-Za-z0-9]";

    static String toEnvironmentKey(String key) {
        return KEY_PREFIX +
                key.toUpperCase().replaceAll(ENVIRONMENT_KEY_REPLACEMENT, "_");
    }

    /**
     * Does nothing, as this implementation is not writable.
     */
    @Override
    public void clear() {}

    /**
     * Does nothing, as this implementation is not writable.
     */
    @Override
    public void clearProperty(String key) {}

    @Override
    public boolean getBoolean(String key) {
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            return StringUtils.toBoolean(value);
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String key) {
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            return Double.parseDouble(value);
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            return getDouble(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Optional<Path> getFile() {
        return Optional.empty();
    }

    @Override
    public float getFloat(String key) {
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            return Float.parseFloat(value);
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        try {
            return getFloat(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key) {
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            return Integer.parseInt(value);
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Iterator<String> getKeys() {
        return System.getenv().keySet()
                .stream()
                .filter(k -> k.startsWith(KEY_PREFIX))
                .iterator();
    }

    @Override
    public long getLong(String key) {
        key = toEnvironmentKey(key);
        String value = System.getenv(key);
        if (value != null) {
            return Long.parseLong(value);
        }
        throw new NoSuchElementException(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            return getLong(key);
        } catch (NoSuchElementException | NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Object getProperty(String key) {
        return getString(key);
    }

    @Override
    public String getString(String key) {
        key = toEnvironmentKey(key);
        return System.getenv(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String string = getString(key);
        if (string != null) {
            return string;
        }
        return defaultValue;
    }

    /**
     * Does nothing, as this implementation does not maintain state.
     */
    @Override
    public void reload() {}

    /**
     * Does nothing, as this implementation is not writable.
     */
    @Override
    public void setProperty(String key, Object value) {
    }

}
