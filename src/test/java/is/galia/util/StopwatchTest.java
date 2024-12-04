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

package is.galia.util;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StopwatchTest extends BaseTest {

    private Stopwatch instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Stopwatch();
    }

    @Test
    void reset() throws Exception {
        Thread.sleep(1);
        instance.reset();
        assertTrue(instance.timeElapsed() < 0.1);
    }

    @Test
    void timeElapsed() throws Exception {
        Thread.sleep(1);
        assertTrue(instance.timeElapsed() >= 1);
    }

    @Test
    void testToString() {
        assertTrue(instance.toString().matches("\\d+ ms"));
    }

}