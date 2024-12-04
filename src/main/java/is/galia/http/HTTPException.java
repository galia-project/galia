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

import java.io.IOException;

public class HTTPException extends IOException {

    private final Status status;
    private Response response;

    public HTTPException(Status status) {
        this.status = status;
    }

    public HTTPException(Response response) {
        super("HTTP " + response.getStatus());
        this.status   = response.getStatus();
        this.response = response;
    }

    /**
     * @return The response, if the {@link
     *         HTTPException#HTTPException(Response)} constructor was
     *         used.
     */
    public Response getResponse() {
        return response;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "HTTP " + status;
    }

}
