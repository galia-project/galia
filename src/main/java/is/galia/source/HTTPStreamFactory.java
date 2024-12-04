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

package is.galia.source;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Headers;
import is.galia.stream.ClosingFileCacheImageInputStream;
import is.galia.stream.HTTPImageInputStream;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

import static is.galia.source.HTTPSource.LOGGER;
import static is.galia.source.HTTPSource.getHTTPClient;

/**
 * Source of streams for {@link HTTPSource},
 */
final class HTTPStreamFactory {

    private static final int DEFAULT_CHUNK_SIZE       = (int) Math.pow(2, 19);
    private static final int DEFAULT_CHUNK_CACHE_SIZE = (int) Math.pow(1024, 2);

    private final HTTPRequestInfo requestInfo;
    private final long contentLength;
    private final boolean serverAcceptsRanges;

    HTTPStreamFactory(HTTPRequestInfo requestInfo,
                      long contentLength,
                      boolean serverAcceptsRanges) {
        this.requestInfo         = requestInfo;
        this.contentLength       = contentLength;
        this.serverAcceptsRanges = serverAcceptsRanges;
    }

    public ImageInputStream newSeekableStream() throws IOException {
        if (isChunkingEnabled()) {
            if (serverAcceptsRanges) {
                final int chunkSize = getChunkSize();
                LOGGER.debug("newSeekableStream(): using {}-byte chunks",
                        chunkSize);
                OkHttpHTTPImageInputStreamClient rangingClient =
                        new OkHttpHTTPImageInputStreamClient(requestInfo);

                HTTPImageInputStream stream = new HTTPImageInputStream(
                        rangingClient, contentLength);
                stream.setWindowSize(chunkSize);
                return stream;
            } else {
                LOGGER.debug("newSeekableStream(): chunking is enabled, but " +
                        "won't be used because the server's HEAD response " +
                        "didn't include an Accept-Ranges header.");
            }
        } else {
            LOGGER.debug("newSeekableStream(): chunking is disabled");
        }
        return new ClosingFileCacheImageInputStream(newInputStream());
    }

    private InputStream newInputStream() throws IOException {
        final Headers extraHeaders = requestInfo.getHeaders();

        Request.Builder builder = new Request.Builder()
                .url(requestInfo.getURI());
        extraHeaders.forEach(h -> builder.addHeader(h.name(), h.value()));

        if (requestInfo.getUsername() != null &&
                requestInfo.getSecret() != null) {
            builder.addHeader("Authorization",
                    "Basic " + requestInfo.getBasicAuthToken());
        }

        Request request = builder.build();

        LOGGER.debug("Requesting GET {} [extra headers: {}]",
                requestInfo.getURI(), HTTPSource.toString(request.headers()));

        Response response = getHTTPClient().newCall(request).execute();
        ResponseBody body = response.body();
        return (body != null) ? body.byteStream() : null;
    }

    private boolean isChunkingEnabled() {
        return Configuration.forApplication().getBoolean(
                Key.HTTPSOURCE_CHUNKING_ENABLED, true);
    }

    private int getChunkSize() {
        return (int) Configuration.forApplication().getLongBytes(
                Key.HTTPSOURCE_CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
    }

}
