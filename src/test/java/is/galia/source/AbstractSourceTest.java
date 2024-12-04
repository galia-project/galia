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

package is.galia.source;

import is.galia.image.Identifier;
import is.galia.image.StatResult;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.NoSuchFileException;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractSourceTest extends BaseTest {

    abstract void destroyEndpoint() throws Exception;
    abstract void initializeEndpoint() throws Exception;
    abstract Source newInstance();
    abstract void useBasicLookupStrategy();
    abstract void useDelegateLookupStrategy();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        useBasicLookupStrategy();
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorConsecutiveInvocationsReturnSameInstance() {
        Source instance = newInstance();
        var it = instance.getFormatIterator();
        assertSame(it, instance.getFormatIterator());
    }

    /* newInputStream() */

    @Test
    void testNewInputStreamInvokedMultipleTimes() throws Exception {
        Source instance = newInstance();
        try {
            initializeEndpoint();
            instance.newInputStream().close();
            instance.newInputStream().close();
            instance.newInputStream().close();
        } finally {
            destroyEndpoint();
        }
    }

    /* stat() */

    @Test
    void testStatUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        try {
            initializeEndpoint();

            newInstance().stat();
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    void testStatUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.setIdentifier(new Identifier("bogus"));
            assertThrows(NoSuchFileException.class, instance::stat);
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    void testStatReturnsCorrectInstance() throws Exception {
        try {
            initializeEndpoint();

            StatResult result = newInstance().stat();
            assertNotNull(result.getLastModified());
        } finally {
            destroyEndpoint();
        }
    }

    /**
     * Tests that {@link Source#stat()} can be invoked multiple times without
     * throwing an exception.
     */
    @Test
    void testStatInvokedMultipleTimes() throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.stat();
            instance.stat();
            instance.stat();
        } finally {
            destroyEndpoint();
        }
    }

}
