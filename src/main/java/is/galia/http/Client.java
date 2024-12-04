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

import java.io.File;
import java.io.IOException;

/**
 * <p>HTTP client.</p>
 *
 * <p>Obtain instances from {@link ClientFactory}.</p>
 */
public interface Client {

    Headers getHeaders();

    Method getMethod();

    Transport getTransport();

    Reference getURI();

    Response send() throws IOException;

    void setEntity(String entity);

    void setFollowRedirects(boolean followRedirects);

    void setKeyStore(File keyStore);

    void setKeyStorePassword(String password);

    void setMethod(Method method);

    void setRealm(String realm);

    void setSecret(String secret);

    void setTransport(Transport transport);

    void setTrustAll(boolean trustAll);

    void setURI(Reference uri);

    void setUsername(String username);

    void stop() throws Exception;

}