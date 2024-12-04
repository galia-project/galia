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

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusTest extends BaseTest {

    private Status instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Status(200);
    }

    /* isClientError() */

    @Test
    void isClientErrorWithNonClientErrors() {
        instance = new Status(399);
        assertFalse(instance.isClientError());
        instance = new Status(500);
        assertFalse(instance.isClientError());
    }

    @Test
    void isClientErrorWithClientError() {
        instance = new Status(400);
        assertTrue(instance.isClientError());
        instance = new Status(499);
        assertTrue(instance.isClientError());
    }

    /* isError() */

    @Test
    void isErrorWithNonErrorCode() {
        assertFalse(instance.isError());
    }

    @Test
    void isErrorWithErrorCode() {
        instance = new Status(400);
        assertTrue(instance.isError());
    }

    /* isServerError() */

    @Test
    void isServerErrorWithNonServerErrors() {
        instance = new Status(499);
        assertFalse(instance.isServerError());
    }

    @Test
    void isServerErrorWithServerError() {
        instance = new Status(500);
        assertTrue(instance.isServerError());
        instance = new Status(599);
        assertTrue(instance.isServerError());
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("200 OK", instance.toString());
    }

}