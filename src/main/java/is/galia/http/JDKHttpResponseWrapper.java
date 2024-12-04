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
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Adapts a {@link java.net.http.HttpResponse} to comply with {@link Response}.
 */
public final class JDKHttpResponseWrapper implements Response {

    private final HttpResponse<InputStream> jdkResponse;

    public JDKHttpResponseWrapper(HttpResponse<InputStream> jdkResponse) {
        this.jdkResponse = jdkResponse;
    }

    @Override
    public void close() {
    }

    @Override
    public byte[] getBody() throws IOException {
        try (InputStream is = getBodyAsStream()) {
            return is.readAllBytes();
        }
    }

    @Override
    public InputStream getBodyAsStream() {
        return jdkResponse.body();
    }

    @Override
    public String getBodyAsString() throws IOException {
        return new String(getBody(), StandardCharsets.UTF_8);
    }

    @Override
    public Headers getHeaders() {
        final Headers headers = new Headers();
        jdkResponse.headers().map().forEach((name, list) ->
                list.forEach(h -> headers.add(name, h)));
        return headers;
    }

    @Override
    public Status getStatus() {
        return new Status(jdkResponse.statusCode());
    }

    @Override
    public Transport getTransport() {
        return switch (jdkResponse.version()) {
            case HttpClient.Version.HTTP_2 -> Transport.HTTP2_0;
            default                        -> Transport.HTTP1_1;
        };
    }

}
