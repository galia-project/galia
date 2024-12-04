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

/**
 * <p>Measures time elapsed. The timer starts at instantiation.</p>
 *
 * <p>Instances are thread-safe.</p>
 */
public final class Stopwatch {

    private long startTime;

    public Stopwatch() {
        reset();
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    /**
     * @return Number of milliseconds since the stopwatch was started.
     */
    public long timeElapsed() {
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public String toString() {
        return timeElapsed() + " ms";
    }

}
