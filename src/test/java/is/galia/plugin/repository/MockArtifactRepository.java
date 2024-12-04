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

package is.galia.plugin.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Server;
import is.galia.http.Status;
import is.galia.test.TestUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.Callback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Minimal mock repository service backed by a graph of POJOs in a {@link
 * Database}, which get serialized to JSON.</p>
 *
 * <h3>Usage</h3>
 *
 * <ol>
 *     <li>{@link #start() Start the server}</li>
 *     <li>Connect via HTTP using {@link #getURI()}, supplying an {@code
 *     Authorization} header with a value of {@code Bearer {@link
 *     #CUSTOMER_KEY} in requests that require authentication</li>
 *     <li>{@link #stop() Stop the server}</li>
 * </ol>
 *
 * <p>New instances are pre-seeded with fixture data. {@link #getDatabase()}
 * accesses the database containing this data.</p>
 */
public final class MockArtifactRepository {

    private static class RequestHandler extends DefaultHandler {

        private static final Map<Pattern,String> ROUTES = new HashMap<>();

        private final Database database;
        private Request request;
        private Response response;
        private Callback callback;

        static {
            ROUTES.put(Pattern.compile("^/errors$"),
                    "handleErrors");
            ROUTES.put(Pattern.compile("^/my-account$"),
                    "handleMyAccount");
            ROUTES.put(Pattern.compile("^/products/([^/]+)$"),
                    "handleProduct");
            ROUTES.put(Pattern.compile("^/products/([^/]+)/versions/([^/]+)/artifact$"),
                    "handleArtifact");
        }

        RequestHandler(Database database) {
            this.database = database;
        }

        @Override
        public boolean handle(Request request,
                              Response response,
                              Callback callback) {
            this.request  = request;
            this.response = response;
            this.callback = callback;

            // Match the URL path to a handler and parse URL params.
            String handler = null;
            final List<String> params = new ArrayList<>();
            for (Map.Entry<Pattern,String> entry : ROUTES.entrySet()) {
                final Matcher matcher = entry.getKey().matcher(
                        request.getHttpURI().getPath());
                if (matcher.find()) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        params.add(matcher.group(i));
                    }
                    handler = entry.getValue();
                }
            }
            // Invoke the handler.
            try {
                if (handler != null) {
                    final Class<?>[] paramTypes = new Class[params.size()];
                    Arrays.fill(paramTypes, String.class);
                    Method method = getClass().getDeclaredMethod(handler, paramTypes);
                    method.invoke(this, params.toArray());
                } else {
                    response.setStatus(404);
                }
            } catch (Exception e) {
                response.setStatus(500);
                if (e.getCause() instanceof HTTPException re) {
                    response.setStatus(re.getStatus().code());
                }
                if (e.getMessage() != null) {
                    byte[] messageBytes = e.getMessage().getBytes(StandardCharsets.UTF_8);
                    ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
                    response.write(true, buffer, callback);
                }
            }
            callback.succeeded();
            return true;
        }

        @SuppressWarnings("unused")
        private void handleErrors() throws Exception {
            response.setStatus(Status.NO_CONTENT.code());
        }

        @SuppressWarnings("unused")
        private void handleMyAccount() throws Exception {
            validateAuthToken();

            Account account = database.stream()
                    .filter(e -> e instanceof Account)
                    .map(e -> (Account) e)
                    .filter(a -> a.getCustomerKey().equals(getAuthToken()))
                    .findFirst()
                    .orElse(null);
            if (account == null) {
                response.setStatus(404);
                return;
            }
            response.getHeaders().add("Content-Type", "application/json");
            write(account);
        }

        @SuppressWarnings("unused")
        private void handleProduct(String name) throws Exception {
            Product product = database.stream()
                    .filter(e -> e instanceof Product)
                    .map(e -> (Product) e)
                    .filter(p -> p.getName().equals(name))
                    .findFirst()
                    .orElse(null);
            if (product == null) {
                response.setStatus(404);
                return;
            } else if (!product.isPublic()) {
                validateAuthToken();
            }
            response.getHeaders().add("Content-Type", "application/json");
            write(product);
        }

        @SuppressWarnings("unused")
        private void handleArtifact(String productName,
                                    String versionName) throws Exception {
            Product product = database.stream()
                    .filter(e -> e instanceof Product)
                    .map(e -> (Product) e)
                    .filter(p -> p.getName().equals(productName))
                    .findFirst()
                    .orElse(null);
            if (product == null) {
                response.setStatus(404);
                return;
            } else if (!product.isPublic()) {
                validateAuthToken();
            }
            ProductVersion version = product.getVersions().stream()
                    .filter(v -> v.getName().equals(versionName))
                    .findFirst()
                    .orElse(null);
            if (version == null) {
                response.setStatus(404);
                return;
            }
            Artifact artifact = version.getArtifacts().getFirst();
            if (artifact == null) {
                response.setStatus(404);
                return;
            }

            Path artifactFile = FIXTURE_DIR.resolve(artifact.getFilename());
            byte[] bytes      = Files.readAllBytes(artifactFile);
            response.getHeaders().add("Content-Type", "application/zip");
            write(bytes);
        }

        private String getAuthToken() {
            String authHeader = request.getHeaders().get("Authorization");
            if (authHeader.startsWith("Bearer ")) {
                String[] parts = authHeader.split(" ");
                if (parts.length == 2) {
                    return parts[1];
                }
            }
            return null;
        }

        private void validateAuthToken() throws HTTPException {
            String authHeader = request.getHeaders().get("Authorization");
            String expectedAuthHeader = "Bearer " + CUSTOMER_KEY;
            if (!expectedAuthHeader.equals(authHeader)) {
                throw new HTTPException(Status.FORBIDDEN);
            }
        }

        private void write(Object serializable) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                mapper.writer().withDefaultPrettyPrinter()
                        .writeValue(outputStream, serializable);
                write(outputStream.toByteArray());
            }
        }

        private void write(byte[] entity) {
            ByteBuffer buffer = ByteBuffer.wrap(entity);
            response.write(true, buffer, callback);
        }

    }

    /**
     * N.B.: Customer key is really a property of an {@link Account account},
     * not the repository itself. This is merely a convenience since the
     * repository is seeded with only one account.
     */
    public static final String CUSTOMER_KEY =
            "47848fd22b4dbc2e877f390196994f47";

    static final Path FIXTURE_DIR = TestUtils.getFixturePath()
            .resolve("MockArtifactRepository");

    private final Server wrappedServer = new Server();
    private final Database database;

    public MockArtifactRepository() {
        wrappedServer.setHTTPS1Enabled(false);
        wrappedServer.setHTTPS2Enabled(false);
        database = new Database(getURI());
        wrappedServer.setHandler(new RequestHandler(database));
        new Seeder().seed(database);
    }

    public Database getDatabase() {
        return database;
    }

    public Reference getURI() {
        return wrappedServer.getHTTPURI();
    }

    public void start() throws Exception {
        wrappedServer.start();
    }

    public void stop() throws Exception {
        wrappedServer.stop();
    }

}
