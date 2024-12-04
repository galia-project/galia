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

import is.galia.Application;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

class OkHttpClientAdapter implements Client {

    private OkHttpClient client;
    private String entity;
    private boolean followRedirects = false;
    private final Headers headers = new Headers();
    private File keyStore;
    private String keyStorePassword = "password";
    private Method method = Method.GET;
    private String realm;
    private String secret;
    private Transport transport = Transport.HTTP1_1;
    private boolean trustAll;
    private Reference uri;
    private String username;

    public Headers getHeaders() {
        return headers;
    }

    public Method getMethod() {
        return method;
    }

    public Transport getTransport() {
        return transport;
    }

    public Reference getURI() {
        return uri;
    }

    public Response send() throws IOException {
        if (client == null) {
            client = newClient();
        }
        Request request = newRequest();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            Response customResponse = new OkHttpResponseWrapper(response);
            if (customResponse.getStatus().isError()) {
                throw new HTTPException(customResponse);
            }
            return customResponse;
        } catch (UnknownHostException e) {
            throw new ConnectException(e.getMessage());
        } catch (ProtocolException e) {
            if (e.getMessage() != null &&
                    e.getMessage().startsWith("Too many follow-up requests")) {
                MutableResponse customResponse = new MutableResponse();
                customResponse.setStatus(Status.UNAUTHORIZED);
                throw new HTTPException(customResponse);
            } else {
                throw e;
            }
        }
    }

    private OkHttpClient newClient() {
        List<Protocol> protocols;
        if (Transport.HTTP1_1.equals(getTransport())) {
            protocols = List.of(Protocol.HTTP_1_1);
        } else {
            if ("http".equals(getURI().getScheme())) {
                protocols = List.of(Protocol.H2_PRIOR_KNOWLEDGE);
            } else {
                protocols = List.of(Protocol.HTTP_2, Protocol.HTTP_1_1);
            }
        }
        okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder()
                .followRedirects(followRedirects)
                .protocols(protocols);
        if (Application.isTesting()) {
            // helpful when debugging
            clientBuilder.readTimeout(10, TimeUnit.MINUTES)
                    .writeTimeout(10, TimeUnit.MINUTES);
        } else if (Application.isTesting()) {
            clientBuilder.readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS);
        }
        if (trustAll) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                X509TrustManager trustManager = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain,
                                                   String authType) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain,
                                                   String authType) {
                    }
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                sslContext.init(null,
                        new X509TrustManager[] { trustManager },
                        new SecureRandom());
                clientBuilder
                        .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                        .hostnameVerifier((hostname, session) -> true);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        // Set Basic auth info
        if (username != null && secret != null) {
            clientBuilder.authenticator((route, okResponse) -> {
                String credential = Credentials.basic(username, secret);
                return okResponse.request().newBuilder()
                        .header("Authorization", credential).build();
            });
        }
        return clientBuilder.build();
    }

    private Request newRequest() {
        Request.Builder builder = new Request.Builder().url(uri.toString());
        if (!Method.HEAD.equals(method) && !Method.GET.equals(method)) {
            RequestBody body = RequestBody.create(entity != null ?
                    entity.getBytes(StandardCharsets.UTF_8) :
                    new byte[] {});
            builder.method(method.toString(), body);
        }

        // Send the authorization header preemptively without a challenge,
        if (username != null && secret != null) {
            builder.header("Authorization", basicAuthToken());
        }

        // Add headers
        for (Header header : headers) {
            builder.addHeader(header.name(), header.value());
        }
        return builder.build();
    }

    private String basicAuthToken() {
        byte[] bytes = (username + ":" + secret).getBytes(StandardCharsets.UTF_8);
        return "Basic " + Base64.getEncoder().encodeToString(bytes);
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void setKeyStore(File keyStore) {
        this.keyStore = keyStore;
    }

    public void setKeyStorePassword(String password) {
        this.keyStorePassword = password;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public void setRealm(String realm) {
        this.realm = realm;

    }
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setTransport(Transport transport) {
        client = null;
        this.transport = transport;
    }

    public void setTrustAll(boolean trustAll) {
        client = null;
        this.trustAll = trustAll;
    }

    public void setURI(Reference uri) {
        this.uri = uri;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void stop() throws Exception {
    }

}