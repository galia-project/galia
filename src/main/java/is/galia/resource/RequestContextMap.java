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

import is.galia.http.Reference;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.image.Metadata;
import is.galia.image.ScaleConstraint;
import is.galia.operation.OperationList;
import is.galia.util.Rational;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * <p>Unmodifiable {@link Map} interface to a {@link RequestContext}. Changes
 * to the backing {@link RequestContext} are reflected immediately by the
 * accessors.</p>
 *
 * <p>This class could be thought of as a facade of a {@link RequestContext}.
 * It is used for exposing data in standard, simply-typed formats over the
 * JRuby Embed bridge. This simplifies the delegate API, thereby reducing the
 * learning curve for delegate programmers. It also helps to decouple internal
 * interfaces from delegate code.</p>
 */
@SuppressWarnings("unchecked")
final class RequestContextMap<K, V> implements Map<K, V> {

    /**
     * Implementation that supports null values.
     */
    private static class NullableEntry<K,V> implements Entry<K,V> {

        private final K key;
        private final V value;

        NullableEntry(K key, V value) {
            this.key   = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            return Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^
                    (value == null ? 0 : value.hashCode());
        }

    }

    /**
     * Supports "live values" in the {@link #cachedMap internal backing map}.
     */
    private class ValueGetter {
        private final String key;

        ValueGetter(String key) {
            this.key = key;
        }

        V get() {
            return switch (key) {
                case CLIENT_IP_KEY        -> clientIP();
                case COOKIES_KEY          -> cookies();
                case FULL_SIZE_KEY        -> fullSize();
                case IDENTIFIER_KEY       -> identifier();
                case LOCAL_URI_KEY        -> localURI();
                case METADATA_KEY         -> metadata();
                case OPERATIONS_KEY       -> operations();
                case OUTPUT_FORMAT_KEY    -> outputFormat();
                case PAGE_COUNT_KEY       -> pageCount();
                case PAGE_NUMBER_KEY      -> pageNumber();
                case REQUEST_HEADERS_KEY  -> requestHeaders();
                case REQUEST_URI_KEY      -> requestURI();
                case RESOURCE_CLASS_KEY   -> resourceClass();
                case RESULTING_SIZE_KEY   -> resultingSize();
                case SCALE_CONSTRAINT_KEY -> scaleConstraint();
                default -> null;
            };
        }

        boolean isPresent() {
            return get() != null;
        }
    }

    static final String CLIENT_IP_KEY        = "client_ip";
    static final String COOKIES_KEY          = "cookies";
    static final String FULL_SIZE_KEY        = "full_size";
    static final String IDENTIFIER_KEY       = "identifier";
    static final String LOCAL_URI_KEY        = "local_uri";
    static final String METADATA_KEY         = "metadata";
    static final String OPERATIONS_KEY       = "operations";
    static final String OUTPUT_FORMAT_KEY    = "output_format";
    static final String PAGE_COUNT_KEY       = "page_count";
    static final String PAGE_NUMBER_KEY      = "page_number";
    static final String REQUEST_HEADERS_KEY  = "request_headers";
    static final String REQUEST_URI_KEY      = "request_uri";
    static final String RESOURCE_CLASS_KEY   = "resource_class";
    static final String RESULTING_SIZE_KEY   = "resulting_size";
    static final String SCALE_CONSTRAINT_KEY = "scale_constraint";

    private static final Set<String> KEY_SET = Set.of(
            CLIENT_IP_KEY, COOKIES_KEY, FULL_SIZE_KEY, IDENTIFIER_KEY,
            LOCAL_URI_KEY, METADATA_KEY, OPERATIONS_KEY, OUTPUT_FORMAT_KEY,
            PAGE_COUNT_KEY, PAGE_NUMBER_KEY, REQUEST_HEADERS_KEY,
            REQUEST_URI_KEY, RESOURCE_CLASS_KEY, RESULTING_SIZE_KEY,
            SCALE_CONSTRAINT_KEY);

    private final RequestContext backingContext;
    private transient Map<String,ValueGetter> cachedMap;

    RequestContextMap(RequestContext backingContext) {
        this.backingContext = backingContext;
    }

    @Override
    public void clear() {
        throwUnmodifiable();
    }

    @Override
    public boolean containsKey(Object key) {
        return map().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map().values()
                .stream()
                .anyMatch(v -> Objects.equals(v.get(), value));
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map().entrySet()
                .stream()
                .map(e -> new NullableEntry<>((K) e.getKey(), e.getValue().get()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public V get(Object key) {
        return map().get(key).get();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<K> keySet() {
        return (Set<K>) KEY_SET;
    }

    private Map<String,ValueGetter> map() {
        if (cachedMap == null) {
            cachedMap = Map.ofEntries(
                    Map.entry(CLIENT_IP_KEY, new ValueGetter(CLIENT_IP_KEY)),
                    Map.entry(COOKIES_KEY, new ValueGetter(COOKIES_KEY)),
                    Map.entry(FULL_SIZE_KEY, new ValueGetter(FULL_SIZE_KEY)),
                    Map.entry(IDENTIFIER_KEY, new ValueGetter(IDENTIFIER_KEY)),
                    Map.entry(LOCAL_URI_KEY, new ValueGetter(LOCAL_URI_KEY)),
                    Map.entry(METADATA_KEY, new ValueGetter(METADATA_KEY)),
                    Map.entry(OPERATIONS_KEY, new ValueGetter(OPERATIONS_KEY)),
                    Map.entry(OUTPUT_FORMAT_KEY, new ValueGetter(OUTPUT_FORMAT_KEY)),
                    Map.entry(PAGE_COUNT_KEY, new ValueGetter(PAGE_COUNT_KEY)),
                    Map.entry(PAGE_NUMBER_KEY, new ValueGetter(PAGE_NUMBER_KEY)),
                    Map.entry(REQUEST_HEADERS_KEY, new ValueGetter(REQUEST_HEADERS_KEY)),
                    Map.entry(REQUEST_URI_KEY, new ValueGetter(REQUEST_URI_KEY)),
                    Map.entry(RESOURCE_CLASS_KEY, new ValueGetter(RESOURCE_CLASS_KEY)),
                    Map.entry(RESULTING_SIZE_KEY, new ValueGetter(RESULTING_SIZE_KEY)),
                    Map.entry(SCALE_CONSTRAINT_KEY, new ValueGetter(SCALE_CONSTRAINT_KEY)));
        }
        return cachedMap;
    }

    @Override
    public V put(K key, V value) {
        throwUnmodifiable();
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throwUnmodifiable();
    }

    @Override
    public V remove(Object key) {
        throwUnmodifiable();
        return null;
    }

    @Override
    public int size() {
        return map().size();
    }

    @Override
    public Collection<V> values() {
        return map().values()
                .stream()
                .map(ValueGetter::get)
                .collect(Collectors.toList());
    }

    private V clientIP() {
        return (V) backingContext.getClientIP();
    }

    private V cookies() {
        Map<String,String> cookies = backingContext.getCookies();
        return (cookies != null) ?
                (V) Collections.unmodifiableMap(backingContext.getCookies()) :
                null;
    }

    private V fullSize() {
        Size size = backingContext.getFullSize();
        return (size != null) ? (V) toMap(size) : null;
    }

    private V identifier() {
        Identifier identifier = backingContext.getIdentifier();
        return (identifier != null) ? (V) identifier.toString() : null;
    }

    private V localURI() {
        Reference uri = backingContext.getLocalURI();
        return (uri != null) ? (V) uri.toString() : null;
    }

    private V metadata() {
        Metadata metadata = backingContext.getMetadata();
        return (metadata != null) ? (V) metadata.toMap() : null;
    }

    private V operations() {
        OperationList opList = backingContext.getOperationList();
        return (opList != null) ?
                (V) opList.toMap(backingContext.getFullSize()).get(OPERATIONS_KEY) : null;
    }

    private V outputFormat() {
        Format format = backingContext.getOutputFormat();
        return (format != null) ?
                (V) format.getPreferredMediaType().toString() : null;
    }

    private V pageCount() {
        return (V) backingContext.getPageCount();
    }

    private V pageNumber() {
        return (V) backingContext.getPageNumber();
    }

    private V requestHeaders() {
        Map<String,String> headers = backingContext.getRequestHeaders();
        if (headers != null) {
            Map<String,String> headersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headersMap.putAll(headers);
            return (V) Collections.unmodifiableMap(headersMap);
        }
        return null;
    }

    private V requestURI() {
        Reference uri = backingContext.getRequestURI();
        return (uri != null) ? (V) uri.toString() : null;
    }

    private V resourceClass() {
        return (V) backingContext.getResourceClass();
    }

    private V resultingSize() {
        Size size = backingContext.getResultingSize();
        return (size != null) ? (V) toMap(size) : null;
    }

    private V scaleConstraint() {
        ScaleConstraint constraint = backingContext.getScaleConstraint();
        if (constraint != null) {
            Rational rational = constraint.rational();
            return (V) List.of(rational.numerator(), rational.denominator());
        }
        return null;
    }

    /**
     * Dumps the map contents into a formatted newline-separated list of
     * colon-separated key-value pairs, which works nicely in a Ruby {@code
     * puts} statement.
     */
    @Override
    public String toString() {
        final int keyPadding = 2 + map().keySet().stream()
                .map(String::length)
                .max(Integer::compare)
                .orElse(0);
        return map().entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map(e -> String.format("%" + keyPadding + "s", e.getKey() + ": ") +
                        (e.getValue().isPresent() ? e.getValue().get() : ""))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static Map<String,Long> toMap(Size size) {
        return Map.of("width", size.longWidth(),
                "height", size.longHeight());
    }

    private void throwUnmodifiable() {
        throw new UnsupportedOperationException("This implementation is " +
                "backed by a " + RequestContext.class.getSimpleName() +
                " and is unmodifiable.");
    }

}
