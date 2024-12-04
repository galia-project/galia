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
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MetaIdentifierTransformerFactoryTest extends BaseTest {

    private MetaIdentifierTransformerFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MetaIdentifierTransformerFactory();
    }

    @Test
    void allImplementations() {
        Set<Class<?>> expected = Set.of(
                StandardMetaIdentifierTransformer.class,
                DelegateMetaIdentifierTransformer.class);
        assertEquals(expected,
                MetaIdentifierTransformerFactory.allImplementations());
    }

    @Test
    void newInstanceReturnsACorrectInstance() {
        Configuration config = Configuration.forApplication();
        Delegate delegate = TestUtils.newDelegate();

        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());
        MetaIdentifierTransformer xformer = instance.newInstance(delegate);
        assertInstanceOf(StandardMetaIdentifierTransformer.class, xformer);

        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                DelegateMetaIdentifierTransformer.class.getSimpleName());
        xformer = instance.newInstance(delegate);
        assertInstanceOf(DelegateMetaIdentifierTransformer.class, xformer);
    }

}