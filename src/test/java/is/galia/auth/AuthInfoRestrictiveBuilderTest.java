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

import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthInfoRestrictiveBuilderTest extends BaseTest {

    private AuthInfo.RestrictiveBuilder instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new AuthInfo.RestrictiveBuilder();
    }

    @Test
    void testBuildWithMissingRedirectURIAndScaleConstraint() {
        assertThrows(IllegalStateException.class,
                () -> instance.withResponseStatus(301).build());
    }

    @Test
    void testBuildWithRedirectURIAnd2xxStatus() {
        assertThrows(IllegalStateException.class,
                () -> instance.withRedirectURI("http://example.org/")
                        .withResponseStatus(200)
                        .build());
    }

    @Test
    void testBuildWithRedirectURIAnd4xxStatus() {
        assertThrows(IllegalStateException.class,
                () -> instance.withRedirectURI("http://example.org/")
                        .withResponseStatus(401)
                        .build());
    }

    @Test
    void testBuildWith401StatusAndNullWWWAuthenticateValue() {
        assertThrows(IllegalStateException.class,
                () -> instance.withRedirectURI("http://example.org/")
                        .withResponseStatus(401)
                        .build());
    }

    @Test
    void testBuildWithWWWAuthenticateValueAndNon401Status() {
        assertThrows(IllegalStateException.class,
                () -> instance.withRedirectURI("http://example.org/")
                        .withChallengeValue("Basic")
                        .withResponseStatus(403)
                        .build());
    }

    @Test
    void testBuildWithRedirect() {
        AuthInfo info = instance.withResponseStatus(301)
                .withRedirectURI("http://example.org/")
                .build();
        assertEquals(301, info.getResponseStatus());
        assertEquals("http://example.org/", info.getRedirectURI());
    }

    @Test
    void testBuildWithScaleConstraint() {
        AuthInfo info = instance.withResponseStatus(302)
                .withRedirectScaleConstraint(1, 2)
                .build();
        assertEquals(302, info.getResponseStatus());
        assertEquals(1, info.getScaleConstraint().rational().numerator());
        assertEquals(2, info.getScaleConstraint().rational().denominator());
    }

    @Test
    void testBuildWithScaleConstraintAnd2xxStatus() {
        assertThrows(IllegalStateException.class,
                () -> instance.withRedirectScaleConstraint(1, 2)
                        .withResponseStatus(200)
                        .build());
    }

    @Test
    void testBuildWithScaleConstraintAnd4xxStatus() {
        assertThrows(IllegalStateException.class,
                () -> instance.withRedirectScaleConstraint(1, 2)
                        .withResponseStatus(401)
                        .build());
    }

    @Test
    void testBuildWithUnauthorized() {
        AuthInfo info = instance.withResponseStatus(401)
                .withChallengeValue("Basic")
                .build();
        assertEquals(401, info.getResponseStatus());
        assertEquals("Basic", info.getChallengeValue());
    }

}
