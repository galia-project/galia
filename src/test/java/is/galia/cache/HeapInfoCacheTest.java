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

import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeapInfoCacheTest extends BaseTest {

    private HeapInfoCache instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new HeapInfoCache();
    }

    /* evict(Identifier) */

    @Test
    void evictWithIdentifier() {
        final Identifier id1 = new Identifier("cats");
        final Identifier id2 = new Identifier("dogs");
        final Info info = new Info();
        instance.put(id1, info);
        instance.put(id2, info);
        assertEquals(2, instance.size());

        instance.evict(id1);
        assertEquals(1, instance.size());
    }

    /* get() */

    @Test
    void getWithHit() {
        final Identifier identifier = new Identifier("jpg");
        final Info info = new Info();

        instance.put(identifier, info);

        Info actualInfo = instance.get(identifier);
        assertEquals(info, actualInfo);
    }

    @Test
    void getWithMiss() {
        final Identifier identifier = new Identifier("jpg");

        Info actualInfo = instance.get(identifier);
        assertNull(actualInfo);
    }

    /* maxSize() */

    @Test
    void maxSize() {
        assertTrue(instance.maxSize() > 1000);
    }

    /* purge() */

    @Test
    void purge() {
        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();
        instance.put(identifier, info);
        assertEquals(1, instance.size());

        instance.purge();
        assertEquals(0, instance.size());
    }

    /* put() */

    @Test
    void put() {
        assertEquals(0, instance.size());

        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();
        instance.put(identifier, info);
        assertEquals(1, instance.size());
    }

    /* size() */

    @Test
    void size() {
        assertEquals(0, instance.size());

        final Identifier identifier = new Identifier("cats");
        final Info info = new Info();
        instance.put(identifier, info);
        assertEquals(1, instance.size());
    }

}
