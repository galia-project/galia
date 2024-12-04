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

import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthInfoTest extends BaseTest {

    // N.B.: inner classes are tested in separate test classes

    @Test
    void testIsAuthorized() {
        AuthInfo info = new AuthInfo.RestrictiveBuilder()
                .withResponseStatus(200)
                .build();
        assertTrue(info.isAuthorized());

        info = new AuthInfo.RestrictiveBuilder()
                .withResponseStatus(300)
                .withRedirectURI("http://example.org/")
                .build();
        assertFalse(info.isAuthorized());
    }

    @Test
    void testGetScaleConstraint() {
        AuthInfo info = new AuthInfo.RestrictiveBuilder()
                .withResponseStatus(302)
                .withRedirectScaleConstraint(2, 3)
                .build();
        assertEquals(new ScaleConstraint(2, 3), info.getScaleConstraint());
    }

}
