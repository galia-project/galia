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

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MockJettyResponse implements org.eclipse.jetty.server.Response {

    private int status;
    private final List<HttpField> headers = new ArrayList<>();

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public boolean hasLastWrite() {
        return false;
    }

    @Override
    public boolean isCompletedSuccessfully() {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public CompletableFuture<Void> writeInterim(int i, HttpFields httpFields) {
        return null;
    }

    @Override
    public void write(boolean b, ByteBuffer byteBuffer, Callback callback) {
        callback.succeeded();
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public HttpFields.Mutable getHeaders() {
        return headers::listIterator;
    }

    @Override
    public Supplier<HttpFields> getTrailersSupplier() {
        return null;
    }

    @Override
    public void setTrailersSupplier(Supplier<HttpFields> supplier) {
    }

}
