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

import is.galia.image.MediaType;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;

class JDKHttpClientAdapter implements Client {

    private HttpClient client;
    private String entity;
    private boolean followRedirects = false;
    private final Headers headers = new Headers();
    private File keyStore;
    private String keyStorePassword = "password";
    private Method method = Method.GET;
    private String realm;
    private String secret;
    private Transport transport = Transport.HTTP2_0;
    private boolean trustAll = false;
    private Reference uri;
    private String username;

    static {
        // The default is 3
        System.setProperty("jdk.httpclient.auth.retrylimit", "0");
    }

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
        HttpRequest request = newRequest();
        try {
            HttpResponse<InputStream> response = client.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            Response customResponse = new JDKHttpResponseWrapper(response);
            if (customResponse.getStatus().isError()) {
                throw new HTTPException(customResponse);
            }
            return customResponse;
        } catch (IOException e) {
            if (e.getMessage() != null &&
                    e.getMessage().contains("too many authentication attempts")) {
                throw new HTTPException(Status.UNAUTHORIZED);
            }
            throw e;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private HttpClient newClient() {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .followRedirects(followRedirects ?
                        HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER)
                .version(Transport.HTTP1_1.equals(getTransport()) ?
                        HttpClient.Version.HTTP_1_1 : HttpClient.Version.HTTP_2);
        if (trustAll) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                X509TrustManager trustManager = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain,
                                                   String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain,
                                                   String authType) {}
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };
                sslContext.init(null,
                        new X509TrustManager[] { trustManager },
                        new SecureRandom());
                clientBuilder.sslContext(sslContext);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        // Set Basic auth info
        if (username != null && secret != null) {
            clientBuilder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, secret.toCharArray());
                }
            });
        }
        return clientBuilder.build();
    }

    private HttpRequest newRequest() {
        // Assemble body
        HttpRequest.BodyPublisher pub = HttpRequest.BodyPublishers.noBody();
        if (entity != null) {
            pub = HttpRequest.BodyPublishers.ofString(entity);
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .method(method.toString(), pub)
                .uri(uri.toURI());

        // Send the authorization header preemptively without a challenge,
        // which the client does not do by default.
        if (username != null && secret != null) {
            requestBuilder.header("Authorization", basicAuthToken());
        }

        // Add headers
        for (Header header : headers) {
            requestBuilder.setHeader(header.name(), header.value());
        }
        return requestBuilder.build();
    }

    private String basicAuthToken() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + secret).getBytes());
    }

    /**
     * Adds a {@code Content-Type} header to the {@link #getHeaders()} map.
     */
    public void setContentType(MediaType type) {
        getHeaders().set("Content-Type", type.toString());
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

    /**
     * This is currently a no-op, but clients should call it anyway in case the
     * wrapped client ever changes to one that requires manual stoppage.
     */
    public void stop() throws Exception {
    }

}
