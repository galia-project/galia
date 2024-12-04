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

package is.galia.auth;

import is.galia.delegate.Delegate;
import is.galia.image.Identifier;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizerFactoryTest extends BaseTest {

    private AuthorizerFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new AuthorizerFactory();
    }

    @Test
    void newAuthorizerWithNoArguments() {
        assertTrue(instance.newAuthorizer() instanceof PermissiveAuthorizer);
    }

    @Test
    void newAuthorizerWithNullArguments() {
        assertTrue(instance.newAuthorizer(null, null) instanceof PermissiveAuthorizer);
    }

    @Test
    void newAuthorizerWithArgument() {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(
                new Identifier("forbidden-code-no-reason.jpg"));

        assertTrue(instance.newAuthorizer(delegate) instanceof DelegateAuthorizer);
    }

}
