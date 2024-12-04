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

package is.galia.test.Assert;

import is.galia.http.Client;
import is.galia.http.ClientFactory;
import is.galia.http.Reference;
import is.galia.http.HTTPException;
import is.galia.http.Response;

import java.net.ConnectException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public final class HTTPAssert {

    public static void assertConnectionRefused(String uri) {
        Client client = newClient();
        client.setURI(new Reference(uri));
        try (Response response = client.send()) {
            fail("Connection not refused: " + uri);
        } catch (ConnectException e) {
            // pass
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRedirect(Reference fromURI,
                                      Reference toURI,
                                      int status) {
        Client client = newClient();
        client.setURI(fromURI);
        try (Response response = client.send()) {
            assertEquals(toURI.toString(),
                    response.getHeaders().getFirstValue("Location"));
            assertEquals(status, response.getStatus().code());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRepresentationContains(String contains,
                                                    String uri) {
        Client client = newClient();
        client.setURI(new Reference(uri));
        try (Response response = client.send()) {
            String body = response.getBodyAsString();
            assertTrue(body.contains(contains));
        } catch (HTTPException e) {
            try {
                String body = e.getResponse().getBodyAsString();
                assertTrue(body.contains(contains));
            } catch (Exception e1) {
                fail(e1.getMessage());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRepresentationContains(String contains,
                                                    Reference uri) {
        assertRepresentationContains(contains, uri.toString());
    }

    public static void assertRepresentationEquals(String expected,
                                                  String uri) {
        Client client = newClient();
        client.setURI(new Reference(uri));
        try (Response response = client.send()) {
            String body = response.getBodyAsString();
            assertEquals(expected, body);
        } catch (HTTPException e) {
            try {
                String body = e.getResponse().getBodyAsString();
                assertEquals(expected, body);
            } catch (Exception e1) {
                fail(e1.getMessage());
            }
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertRepresentationEquals(String equals, Reference uri) {
        assertRepresentationEquals(equals, uri.toString());
    }

    public static void assertRepresentationsNotSame(Reference uri1,
                                                    Reference uri2) {
        Client client = newClient();
        try {
            byte[] body1, body2;
            client.setURI(uri1);
            try (Response response = client.send()) {
                body1 = response.getBody();
            }
            client.setURI(uri2);
            try (Response response = client.send()) {
                body2 = response.getBody();
            }
            assertFalse(Arrays.equals(body1, body2));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertStatus(int expectedCode, String uri) {
        Client client = newClient();
        client.setURI(new Reference(uri));
        try (Response response = client.send()) {
            assertEquals(expectedCode, response.getStatus().code());
        } catch (HTTPException e) {
            assertEquals(expectedCode, e.getStatus().code());
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            stopQuietly(client);
        }
    }

    public static void assertStatus(int expectedCode, Reference uri) {
        assertStatus(expectedCode, uri.toString());
    }

    private static Client newClient() {
        Client client = ClientFactory.newClient();
        client.setTrustAll(true);
        client.setFollowRedirects(false);
        return client;
    }

    private static void stopQuietly(Client client) {
        if (client != null) {
            try {
                client.stop();
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    private HTTPAssert() {}

}
