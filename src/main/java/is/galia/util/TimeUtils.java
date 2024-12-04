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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {

    public static String millisecondsToHumanTime(long msec) {
        final long days = TimeUnit.MILLISECONDS.toDays(msec);
        msec -= TimeUnit.DAYS.toMillis(days);
        final long hours = TimeUnit.MILLISECONDS.toHours(msec);
        msec -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(msec);
        msec -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(msec);

        final List<String> parts = new ArrayList<>(4);

        if (days > 0) {
            parts.add(days + pluralize(" day", days));
        }
        if (hours > 0) {
            parts.add(hours + pluralize(" hour", hours));
        }
        if (minutes > 0) {
            parts.add(minutes + pluralize(" minute", minutes));
        }
        if (seconds > 0 || (days < 1 && hours < 1 && minutes < 1)) {
            parts.add(seconds + pluralize(" second", seconds));
        }

        return String.join(", ", parts);
    }

    private static String pluralize(String unit, long number) {
        if (number != 1) {
            unit += "s";
        }
        return unit;
    }

    /**
     * @param seconds Number of seconds.
     * @return String in {@code hh:mm:ss} format.
     * @throws IllegalArgumentException if the argument is negative.
     */
    public static String toHMS(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Argument is negative");
        }
        int h = (int) Math.floor(seconds / 60.0 / 60.0);
        int m = (int) Math.floor(seconds / 60.0) - h * 60;
        int s = seconds - m * 60 - h * 60 * 60;
        return (String.format("%2s", h) + ":" + String.format("%2s", m) + ":" +
                String.format("%2s", s)).replace(' ', '0');
    }

    private TimeUtils() {}

}
