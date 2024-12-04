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

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HealthTest extends BaseTest {

    private Health instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Health();
    }

    @Test
    void testSetMinColor() {
        assertEquals(Health.Color.GREEN, instance.getColor());

        instance.setMinColor(Health.Color.YELLOW);
        assertEquals(Health.Color.YELLOW, instance.getColor());

        instance.setMinColor(Health.Color.RED);
        assertEquals(Health.Color.RED, instance.getColor());

        instance.setMinColor(Health.Color.GREEN);
        assertEquals(Health.Color.RED, instance.getColor());
    }

    @Test
    void testToString() {
        assertEquals("GREEN", instance.toString());

        instance.setMinColor(Health.Color.RED);
        instance.setMessage("cats");
        assertEquals("RED: cats", instance.toString());
    }

}
