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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a collection of request or response cookies.
 */
public final class Cookies implements Iterable<Cookie> {

    private final List<Cookie> cookies = new ArrayList<>();

    /**
     * @param headerValue HTTP header value.
     * @return New instance.
     * @throws IllegalArgumentException if the given header value cannot be
     *         parsed.
     */
    public static Cookies fromHeaderValue(String headerValue) {
        final Cookies cookieJar = new Cookies();
        for (String cookiePattern : headerValue.split(";")) {
            Cookie cookie;
            String[] pair = cookiePattern.split("=");
            if (pair.length > 1) {
                cookie = new Cookie(pair[0].trim(), pair[1].trim());
            } else {
                // cookie does not contain an `=`; treat it as a nameless value
                cookie = new Cookie("", pair[0].trim());
            }
            cookieJar.add(cookie);
        }
        return cookieJar;
    }

    public Cookies() {}

    /**
     * Copy constructor.
     */
    public Cookies(Cookies cookies) {
        cookies.forEach(other -> add(new Cookie(other)));
    }

    public void add(String name, String value) {
        add(new Cookie(name, value));
    }

    public void add(Cookie cookie) {
        cookies.add(cookie);
    }

    public void addAll(Cookies cookies) {
        cookies.forEach(this::add);
    }

    public void clear() {
        cookies.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Cookies other) {
            List<Cookie> allOthers = other.getAll();
            return allOthers.equals(getAll());
        }
        return super.equals(obj);
    }

    /**
     * @return Unmodifiable instance.
     */
    public List<Cookie> getAll() {
        return Collections.unmodifiableList(cookies);
    }

    public List<Cookie> getAll(String name) {
        return cookies.stream().
                filter(h -> h.name().equalsIgnoreCase(name)).
                collect(Collectors.toList());
    }

    public String getFirstValue(String name) {
        Optional<String> cookie = cookies.stream().
                filter(h -> h.name().equalsIgnoreCase(name)).
                map(Cookie::value).
                findFirst();
        return cookie.orElse(null);
    }

    public String getFirstValue(String name, String defaultValue) {
        Optional<String> cookie = cookies.stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .map(c -> (c.value() != null && !c.value().isEmpty()) ?
                        c.value() : defaultValue)
                .findFirst();
        return cookie.orElse(defaultValue);
    }

    @Override
    public int hashCode() {
        return stream()
                .map(Cookie::toString)
                .collect(Collectors.joining())
                .hashCode();
    }

    @Override
    public Iterator<Cookie> iterator() {
        return cookies.iterator();
    }

    public void removeAll(String name) {
        final Collection<Cookie> toRemove = new ArrayList<>(cookies.size());
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.name())) {
                toRemove.add(cookie);
            }
        }
        cookies.removeAll(toRemove);
    }

    /**
     * Replaces all cookies with the given name with a single cookie.
     *
     * @see #add
     */
    public void set(String name, String value) {
        removeAll(name);
        add(name, value);
    }

    public int size() {
        return cookies.size();
    }

    public Stream<Cookie> stream() {
        return cookies.stream();
    }

    /**
     * @return Cookies as a map of name-value pairs. Multiple same-named
     *         cookies will be lost.
     */
    public Map<String,String> toMap() {
        final Map<String,String> map = new HashMap<>();
        cookies.forEach(c -> map.put(c.name(), c.value()));
        return map;
    }

}
