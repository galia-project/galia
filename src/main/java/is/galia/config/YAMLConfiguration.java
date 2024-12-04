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
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.reader.ReaderException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.StampedLock;

final class YAMLConfiguration implements Configuration {

    private final StampedLock lock = new StampedLock();
    private Map<String,Object> yamlDoc = new TreeMap<>();
    private Path yamlDocPath;

    /**
     * Checksum of the configuration contents. When the {@link
     * ConfigurationFileWatcher} receives a change event from the filesystem,
     * it will compute the new checksum and reload only if they don't match.
     * (That's because there are often multiple events per change.)
     */
    private byte[] checksumBytes = new byte[0];

    /**
     * @param path Path of a YAML document.
     * @return New instance.
     */
    static YAMLConfiguration fromPath(Path path) throws IOException {
        if (!Application.isTesting()) {
            System.out.println("Loading configuration from file: " + path);
        }
        YAMLConfiguration config = new YAMLConfiguration();
        config.yamlDocPath       = path;
        Yaml yaml = new Yaml();
        try (Reader reader = Files.newBufferedReader(path)) {
            config.yamlDoc = yaml.load(reader);
        } catch (ReaderException | ParserException e) {
            throw new IOException(e);
        }
        return config;
    }

    private static String readString(Path file) throws IOException {
        return Files.readString(file);
    }

    YAMLConfiguration() {}

    @Override
    public void clear() {
        final long stamp = lock.writeLock();
        try {
            yamlDoc.clear();
            checksumBytes = new byte[0];
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public void clearProperty(final String key) {
        final long stamp = lock.writeLock();
        try {
            yamlDoc.remove(key);
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public Optional<Path> getFile() {
        return Optional.of(yamlDocPath);
    }

    @Override
    public boolean getBoolean(final String key) {
        Boolean bool = readBooleanOptimistically(key);
        if (bool != null) {
            return bool;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            Boolean bool = readBooleanOptimistically(key);
            return (bool != null) ? bool : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Boolean readBooleanOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Boolean bool = readBoolean(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                bool = readBoolean(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return bool;
    }

    private Boolean readBoolean(String key) {
        Boolean bool = null;
        if (yamlDoc.containsKey(key)) {
            Object value = yamlDoc.get(key);
            if (value != null) {
                if (value instanceof Boolean boolValue) {
                    bool = boolValue;
                } else {
                    bool = StringUtils.toBoolean(yamlDoc.get(key).toString());
                }
            }
        }
        return bool;
    }

    @Override
    public double getDouble(String key) {
        Double dub = readDoubleOptimistically(key);
        if (dub != null) {
            return dub;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        try {
            Double dub = readDoubleOptimistically(key);
            return (dub != null) ? dub : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Double readDoubleOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Double dub = readDouble(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                dub = readDouble(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return dub;
    }

    private Double readDouble(String key) {
        Double dub = null;
        if (yamlDoc.containsKey(key)) {
            Object value = yamlDoc.get(key);
            if (value != null) {
                if (value instanceof Double doubleValue) {
                    dub = doubleValue;
                } else if (value instanceof Float floatValue) {
                    dub = Double.valueOf(floatValue);
                } else {
                    dub = Double.parseDouble(yamlDoc.get(key).toString());
                }
            }
        }
        return dub;
    }

    @Override
    public float getFloat(String key) {
        Float flo = readFloatOptimistically(key);
        if (flo != null) {
            return flo;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        try {
            Float flo = readFloatOptimistically(key);
            return (flo != null) ? flo : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Float readFloatOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Float flo = readFloat(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                flo = readFloat(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return flo;
    }

    private Float readFloat(String key) {
        Float flo = null;
        if (yamlDoc.containsKey(key)) {
            Object value = yamlDoc.get(key);
            if (value != null) {
                if (value instanceof Float floatValue) {
                    flo = floatValue;
                } else {
                    flo = Float.parseFloat(yamlDoc.get(key).toString());
                }
            }
        }
        return flo;
    }

    @Override
    public int getInt(String key) {
        Integer integer = readIntegerOptimistically(key);
        if (integer != null) {
            return integer;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        try {
            Integer integer = readIntegerOptimistically(key);
            return (integer != null) ? integer : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer readIntegerOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Integer integer = readInteger(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                integer = readInteger(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return integer;
    }

    private Integer readInteger(String key) {
        Integer integer = null;
        if (yamlDoc.containsKey(key)) {
            Object value = yamlDoc.get(key);
            if (value != null) {
                if (value instanceof Integer intValue) {
                    integer = intValue;
                } else {
                    integer = Integer.parseInt(yamlDoc.get(key).toString());
                }
            }
        }
        return integer;
    }

    /**
     * @return Iterator through all keys.
     */
    @Override
    public Iterator<String> getKeys() {
        final long stamp = lock.readLock();
        try {
            return yamlDoc.keySet().iterator();
        } finally {
            lock.unlock(stamp);
        }
    }

    @Override
    public long getLong(String key) {
        Long lon = readLongOptimistically(key);
        if (lon != null) {
            return lon;
        }
        throw new NoSuchElementException("No such key: " + key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        try {
            Long lon = readLongOptimistically(key);
            return (lon != null) ? lon : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long readLongOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Long lon = readLong(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                lon = readLong(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return lon;
    }

    private Long readLong(String key) {
        Long lon = null;
        if (yamlDoc.containsKey(key)) {
            Object value = yamlDoc.get(key);
            if (value != null) {
                if (value instanceof Long longValue) {
                    lon = longValue;
                } else if (value instanceof Integer intValue) {
                    lon = Long.valueOf(intValue);
                } else {
                    lon = Long.parseLong(yamlDoc.get(key).toString());
                }
            }
        }
        return lon;
    }

    @Override
    public Object getProperty(String key) {
        return readPropertyOptimistically(key);
    }

    private Object readPropertyOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        Object prop = readProperty(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                prop = readProperty(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return prop;
    }

    private Object readProperty(String key) {
        Object prop = null;
        if (yamlDoc.containsKey(key)) {
            prop = yamlDoc.get(key);
        }
        return prop;
    }

    @Override
    public String getString(String key) {
        return readStringOptimistically(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String str = readStringOptimistically(key);
        return (str != null) ? str : defaultValue;
    }

    private String readStringOptimistically(String key) {
        long stamp = lock.tryOptimisticRead();

        String str = readString(key);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                str = readString(key);
            } finally {
                lock.unlock(stamp);
            }
        }
        return str;
    }

    private String readString(String key) {
        String str = null;
        if (yamlDoc.containsKey(key)) {
            Object value = yamlDoc.get(key);
            if (value != null) {
                str = value.toString();
            }
        }
        return str;
    }

    @Override
    public void reload() {
        try {
            String yamlStr;
            if (getFile().isPresent()) {
                yamlStr = readString(getFile().get());
            } else {
                throw new IllegalStateException(
                        "No configuration source present");
            }
            final long stamp = lock.writeLock();
            try {
                // Calculate the checksum of the file contents and compare it
                // to what has already been loaded. If the sums match, skip the
                // reload.
                final MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] newChecksumBytes =
                        md.digest(yamlStr.getBytes(StandardCharsets.UTF_8));
                if (Arrays.equals(newChecksumBytes, checksumBytes)) {
                    return;
                }
                checksumBytes = newChecksumBytes;
                Yaml yaml = new Yaml();
                yamlDoc = yaml.load(yamlStr);
            } catch (NoSuchAlgorithmException e) {
                System.err.println("reload(): " + e.getMessage());
            } finally {
                lock.unlock(stamp);
            }
        } catch (IOException e) {
            System.err.println("reload(): " + e.getMessage());
        }
    }

    @Override
    public void setProperty(String key, Object value) {
        final long stamp = lock.writeLock();
        try {
            yamlDoc.put(key, value);
        } finally {
            lock.unlock(stamp);
        }
    }

}
