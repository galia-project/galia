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

package is.galia.status;

import is.galia.image.Identifier;
import is.galia.source.Source;
import is.galia.source.SourceFactory;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceUsageTest extends BaseTest {

    private SourceUsage instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Source source = SourceFactory.newSource("FilesystemSource");
        source.setIdentifier(new Identifier("cats"));
        instance = new SourceUsage(source);
    }

    @Test
    void equalsWithEqualInstances() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                SourceFactory.newSource("FilesystemSource"));
        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithSameSourceClassesButDifferentIdentifiers() throws Exception {
        Source source2 = SourceFactory.newSource("FilesystemSource");
        source2.setIdentifier(new Identifier("different"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertEquals(instance, instance2);
    }

    @Test
    void equalsWithDifferentSourceClasses() throws Exception {
        Source source2 = SourceFactory.newSource("HTTPSource");
        source2.setIdentifier(new Identifier("cats"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertNotEquals(instance, instance2);
    }

    @Test
    void hashCodeWithEqualInstances() throws Exception {
        SourceUsage instance2 = new SourceUsage(
                SourceFactory.newSource("FilesystemSource"));
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithSameSourceClassesButDifferentIdentifiers()
            throws Exception {
        Source source2 = SourceFactory.newSource("FilesystemSource");
        source2.setIdentifier(new Identifier("different"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void hashCodeWithDifferentSources() throws Exception {
        Source source2 = SourceFactory.newSource("HTTPSource");
        source2.setIdentifier(new Identifier("cats"));
        SourceUsage instance2 = new SourceUsage(source2);
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("cats -> is.galia.source.FilesystemSource",
                instance.toString());
    }

}