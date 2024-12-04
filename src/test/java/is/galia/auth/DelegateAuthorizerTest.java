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
import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DelegateAuthorizerTest extends BaseTest {

    private DelegateAuthorizer instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("default"));
        instance = new DelegateAuthorizer(delegate);
    }

    @Test
    void constructorWithEmptyArgument() {
        Assertions.assertThrows(IllegalArgumentException.class,
                DelegateAuthorizer::new);
    }

    /* authorize() */

    @Test
    void authorizeWithDelegateReturningTrue() throws Exception {
        AuthInfo info = instance.authorize();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void authorizeWithDelegateReturningFalse() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("forbidden.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorize();
        assertEquals(403, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void authorizeWithDelegateReturningUnauthorizedMap() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("unauthorized.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorize();
        assertEquals(401, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void authorizeWithDelegateReturningRedirectMap() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("redirect.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorize();
        assertEquals(303, info.getResponseStatus());
        assertEquals("http://example.org/", info.getRedirectURI());
    }

    @Test
    void authorizeWithDelegateReturningScaleConstraintMap() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("reduce.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorize();
        assertEquals(302, info.getResponseStatus());
        assertEquals(new ScaleConstraint(1, 2), info.getScaleConstraint());
    }

    /* authorizeBeforeAccess() */

    @Test
    void authorizeBeforeAccessWithDelegateReturningTrue() throws Exception {
        AuthInfo info = instance.authorizeBeforeAccess();
        assertEquals(200, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void authorizeBeforeAccessWithDelegateReturningFalse() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("forbidden.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorizeBeforeAccess();
        assertEquals(403, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void authorizeBeforeAccessWithDelegateReturningUnauthorizedMap() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("unauthorized.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorizeBeforeAccess();
        assertEquals(401, info.getResponseStatus());
        assertNull(info.getRedirectURI());
    }

    @Test
    void authorizeBeforeAccessWithDelegateReturningRedirectMap() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("redirect.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorizeBeforeAccess();
        assertEquals(303, info.getResponseStatus());
        assertEquals("http://example.org/", info.getRedirectURI());
    }

    @Test
    void authorizeBeforeAccessWithDelegateReturningScaleConstraintMap()
            throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("reduce.jpg"));
        instance = new DelegateAuthorizer(delegate);

        AuthInfo info = instance.authorizeBeforeAccess();
        assertEquals(302, info.getResponseStatus());
        assertEquals(new ScaleConstraint(1, 2), info.getScaleConstraint());
    }

}
