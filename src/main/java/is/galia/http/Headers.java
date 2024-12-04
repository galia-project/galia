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

package is.galia.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a collection of request or response headers.
 */
public final class Headers implements Iterable<Header> {

    private final List<Header> headers = new ArrayList<>();

    public Headers() {}

    /**
     * Copy constructor.
     */
    public Headers(Headers headers) {
        headers.forEach(other -> add(new Header(other)));
    }

    public void add(String name, String value) {
        add(new Header(name, value));
    }

    public void add(Header header) {
        headers.add(header);
    }

    public void addAll(Headers headers) {
        headers.forEach(this::add);
    }

    public void clear() {
        headers.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Headers other) {
            List<Header> allOthers = other.getAll();
            return allOthers.equals(getAll());
        }
        return super.equals(obj);
    }

    public List<Header> getAll() {
        return new ArrayList<>(headers);
    }

    public List<Header> getAll(String name) {
        return headers.stream().
                filter(h -> h.name().equalsIgnoreCase(name)).
                collect(Collectors.toList());
    }

    public String getFirstValue(String name) {
        Optional<String> header = headers.stream().
                filter(h -> h.name().equalsIgnoreCase(name)).
                map(Header::value).
                findFirst();
        return header.orElse(null);
    }

    public String getFirstValue(String name, String defaultValue) {
        Optional<String> header = headers.stream()
                .filter(h -> h.name().equalsIgnoreCase(name))
                .map(h -> (h.value() != null && !h.value().isEmpty()) ?
                        h.value() : defaultValue)
                .findFirst();
        return header.orElse(defaultValue);
    }

    @Override
    public int hashCode() {
        return stream()
                .map(Header::toString)
                .collect(Collectors.joining())
                .hashCode();
    }

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }

    public void removeAll(String name) {
        final Collection<Header> toRemove = new ArrayList<>(headers.size());
        for (Header header : headers) {
            if (name.equals(header.name())) {
                toRemove.add(header);
            }
        }
        headers.removeAll(toRemove);
    }

    /**
     * Replaces all headers with the given name with a single header.
     *
     * @see #add
     */
    public void set(String name, String value) {
        removeAll(name);
        add(name, value);
    }

    public int size() {
        return headers.size();
    }

    public Stream<Header> stream() {
        return headers.stream();
    }

    /**
     * @return Headers as a map of name-value pairs. Multiple same-named
     *         headers will be lost.
     */
    public Map<String,String> toMap() {
        final Map<String,String> map = new HashMap<>();
        headers.forEach(h -> map.put(h.name(), h.value()));
        return map;
    }

    @Override
    public String toString() {
        if (headers.isEmpty()) {
            return "(none)";
        }
        return stream().map(Header::toString).collect(Collectors.joining(" | "));
    }

}
