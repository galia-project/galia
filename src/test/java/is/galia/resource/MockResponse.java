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

package is.galia.resource;

import is.galia.http.Cookie;
import is.galia.http.Headers;
import is.galia.http.Status;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class MockResponse implements Response {

    private ByteArrayOutputStream outputStream;

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public void addHeader(String name, String value) {
    }

    @Override
    public Headers getHeaders() {
        return null;
    }

    @Override
    public Status getStatus() {
        return null;
    }

    @Override
    public OutputStream openBodyStream() {
        outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    @Override
    public void setHeader(String name, String value) {
    }

    @Override
    public void setStatus(int status) {
    }

    @Override
    public void setStatus(Status status) {
    }

    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

}
