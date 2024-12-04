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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilsTest extends BaseTest {

    @Test
    void testMillisecondsToHumanTime() {
        // seconds
        assertEquals("0 seconds", TimeUtils.millisecondsToHumanTime(50));
        assertEquals("1 second", TimeUtils.millisecondsToHumanTime(1000));
        assertEquals("2 seconds", TimeUtils.millisecondsToHumanTime(2000));

        // minutes
        assertEquals("1 minute, 1 second",
                TimeUtils.millisecondsToHumanTime(61000));
        assertEquals("2 minutes, 2 seconds",
                TimeUtils.millisecondsToHumanTime(122000));

        // hours
        assertEquals("1 hour",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60));
        assertEquals("1 hour, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 + 1000));
        assertEquals("1 hour, 2 minutes, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 + 120 * 1000 + 1000));

        // days
        assertEquals("1 day",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 * 24));
        assertEquals("1 day, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 * 24 + 1000));
        assertEquals("1 day, 2 hours, 2 minutes, 1 second",
                TimeUtils.millisecondsToHumanTime(1000 * 60 * 60 * 24 + 60 * 60 * 2 * 1000 + 120 * 1000 + 1000));
    }

    @Test
    void testToHMSWithIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> TimeUtils.toHMS(-324));
    }

    @Test
    void testToHMS() {
        assertEquals("00:00:15", TimeUtils.toHMS(15));
        assertEquals("00:01:15", TimeUtils.toHMS(75));
        assertEquals("03:47:59", TimeUtils.toHMS(13679));
    }

}
