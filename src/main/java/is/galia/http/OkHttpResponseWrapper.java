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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Adapts a {@link okhttp3.Response} to comply with {@link Response}.
 */
public final class OkHttpResponseWrapper implements Response {

    private final okhttp3.Response okResponse;

    public OkHttpResponseWrapper(okhttp3.Response okResponse) {
        this.okResponse = okResponse;
    }

    @Override
    public void close() {
        okResponse.close();
    }

    @Override
    public byte[] getBody() throws IOException {
        return okResponse.body().bytes();
    }

    @Override
    public InputStream getBodyAsStream() {
        return okResponse.body().byteStream();
    }

    @Override
    public String getBodyAsString() throws IOException {
        return new String(getBody(), StandardCharsets.UTF_8);
    }

    @Override
    public Headers getHeaders() {
        final Headers headers = new Headers();
        okResponse.headers().toMultimap().forEach((name, list) ->
                list.forEach(h -> headers.add(name, h)));
        return headers;
    }

    @Override
    public Status getStatus() {
        return new Status(okResponse.code());
    }

    @Override
    public Transport getTransport() {
        return switch (okResponse.protocol()) {
            case H2_PRIOR_KNOWLEDGE -> Transport.HTTP2_0;
            case HTTP_2             -> Transport.HTTP2_0;
            default                 -> Transport.HTTP1_1;
        };
    }

}
