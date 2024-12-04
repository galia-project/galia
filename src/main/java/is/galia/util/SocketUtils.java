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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public final class SocketUtils {

    public static int getOpenPort() {
        return SocketUtils.getOpenPorts(1)[0];
    }

    public static int[] getOpenPorts(int howMany) {
        final Set<ServerSocket> triedSockets = new HashSet<>();
        final int[] ports = new int[howMany];

        for (int i = 0; i < howMany; i++) {
            try {
                ServerSocket socket = new ServerSocket(0);
                ports[i] = socket.getLocalPort();
                triedSockets.add(socket);
                // Leave it open for now so it isn't returned again on the next
                // iteration.
            } catch (IOException e) {
                System.err.println("TestUtil.getOpenPorts(): " + e.getMessage());
            }
        }

        for (ServerSocket socket : triedSockets) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("TestUtil.getOpenPort(): " + e.getMessage());
            }
        }

        return ports;
    }

    private SocketUtils() {}

}
