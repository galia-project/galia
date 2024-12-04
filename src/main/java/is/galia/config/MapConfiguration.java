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

import is.galia.util.StringUtils;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Implementation backed by a {@link Map}.</p>
 *
 * <p>Mutability and thread safety are dictated by the backing map.</p>
 */
public class MapConfiguration implements Configuration {

    private final Map<String,Object> backingMap;

    /**
     * Initializes an instance with a new empty, mutable, thread-safe backing
     * map.
     */
    public MapConfiguration() {
        backingMap = new ConcurrentHashMap<>();
    }

    /**
     * Enables the use of a custom backing map, essentially providing a facade
     * to it.
     *
     * @param backingMap Backing map.
     */
    public MapConfiguration(Map<String,Object> backingMap) {
        this.backingMap = backingMap;
    }

    @Override
    public void clear() {
        backingMap.keySet().clear();
    }

    @Override
    public void clearProperty(String key) {
        backingMap.remove(key);
    }

    public Map<String,Object> getBackingMap() {
        return backingMap;
    }

    @Override
    public boolean getBoolean(String key) {
        Object value = backingMap.get(key);
        if (value != null) {
            return StringUtils.toBoolean(value.toString());
        } else {
            throw new NoSuchElementException(key);
        }
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
        Object value = backingMap.get(key);
        if (value != null) {
            return Double.parseDouble(value.toString());
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
        Object value = backingMap.get(key);
        if (value != null) {
            return Float.parseFloat(value.toString());
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
        Object value = backingMap.get(key);
        if (value != null) {
            return Integer.parseInt(value.toString());
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
        return backingMap.keySet().iterator();
    }

    @Override
    public long getLong(String key) {
        Object value = backingMap.get(key);
        if (value != null) {
            return Long.parseLong(value.toString());
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
        return backingMap.get(key);
    }

    @Override
    public String getString(String key) {
        Object value = backingMap.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
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
     * No-op.
     */
    @Override
    public void reload() {}

    @Override
    public void setProperty(String key, Object value) {
        backingMap.put(key, value);
    }

}
