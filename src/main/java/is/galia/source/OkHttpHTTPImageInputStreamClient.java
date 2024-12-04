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

import is.galia.http.OkHttpResponseWrapper;
import is.galia.http.Range;
import is.galia.http.Reference;
import is.galia.http.Response;
import is.galia.stream.HTTPImageInputStreamClient;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.Map;

/**
 * Implementation backed by an {@link OkHttpClient}.
 */
class OkHttpHTTPImageInputStreamClient implements HTTPImageInputStreamClient {

    private final HTTPRequestInfo requestInfo;

    /**
     * @return New instance corresponding to the argument.
     */
    private static Response toResponse(okhttp3.Response okHttpResponse) {
        return new OkHttpResponseWrapper(okHttpResponse);
    }

    OkHttpHTTPImageInputStreamClient(HTTPRequestInfo requestInfo) {
        this.requestInfo = requestInfo;
    }

    @Override
    public Reference getReference() {
        return new Reference(requestInfo.getURI());
    }

    @Override
    public Response sendHEADRequest() throws IOException {
        okhttp3.Response response = HTTPSource.request(requestInfo, "HEAD");
        return toResponse(response);
    }

    @Override
    public Response sendGETRequest(Range range) throws IOException {
        Map<String,String> extraHeaders = Map.of(
                "Range", "bytes=" + range.start() + "-" + range.end());
        okhttp3.Response okHttpResponse =
                HTTPSource.request(requestInfo, "GET", extraHeaders);
        if (okHttpResponse.code() == 200 || okHttpResponse.code() == 206) {
            return toResponse(okHttpResponse);
        } else {
            throw new IOException("Unexpected HTTP response code: " +
                    okHttpResponse.code());
        }
    }

}
