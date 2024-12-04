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

import is.galia.Application;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SystemUtils {

    private static final AtomicBoolean exitRequested     = new AtomicBoolean();
    private static final AtomicInteger requestedExitCode = new AtomicInteger();

    /**
     * Clears any exit request created by {@link #exit(int)}.
     */
    public static void clearExitRequest() {
        exitRequested.set(false);
        requestedExitCode.set(0);
    }

    /**
     * <p>Conditionally exits depending on the return value of {@link
     * Application#isTesting()}:</p>
     *
     * <ol>
     *     <li>If that method returns {@code false}, {@link System#exit(int)}
     *     is called.</li>
     *     <li>Otherwise, it is not called, but subsequent calls to {@link
     *     #exitRequested()} will return {@code true}, and {@link
     *     #requestedExitCode()} will return the requested exit code.</li>
     * </ol>
     *
     * @param code Status code.
     */
    public static void exit(int code) {
        if (Application.isTesting()) {
            exitRequested.set(true);
            requestedExitCode.set(code);
        } else {
            System.exit(code);
        }
    }

    /**
     * @return Whether {@link #exit(int)} has been invoked.
     */
    public static boolean exitRequested() {
        return exitRequested.get();
    }

    /**
     * @return Exit code passed to {@link #exit(int)}. This is meaningless
     *         unless {@link #exitRequested()} returns {@code true}.
     */
    public static int requestedExitCode() {
        return requestedExitCode.get();
    }

    private SystemUtils() {}

}
