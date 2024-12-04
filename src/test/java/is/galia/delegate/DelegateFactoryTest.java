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

package is.galia.delegate;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.resource.RequestContext;
import is.galia.test.BaseTest;
import is.galia.test.TestDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelegateFactoryTest extends BaseTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Configuration config = Configuration.forApplication();
        // This is a special config key for testing only--otherwise we
        // wouldn't have a way of testing a no-delegate-installed situation.
        config.setProperty(Key.DELEGATE_ENABLED, true);
    }

    /* isDelegateAvailable() */

    @Test
    void isDelegateAvailableWithDelegateAvailable() {
        assertTrue(DelegateFactory.isDelegateAvailable());
    }

    @Test
    void isDelegateAvailableWithDelegateNotAvailable() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DELEGATE_ENABLED, false);
        assertFalse(DelegateFactory.isDelegateAvailable());
    }

    /* newDelegate() */

    @Test
    void newDelegateWithDelegateAvailable() throws Exception {
        RequestContext context = new RequestContext();
        Delegate actual = DelegateFactory.newDelegate(context);
        assertNotNull(actual);
        assertNotNull(actual.getRequestContext());
    }

    @Test
    void newDelegateWithDelegateNotAvailable() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DELEGATE_ENABLED, false);

        RequestContext context = new RequestContext();
        assertThrows(DelegateNotAvailableException.class,
                () -> DelegateFactory.newDelegate(context));
    }

    @Test
    void newDelegateInitializesDelegate() throws Exception {
        RequestContext context = new RequestContext();
        TestDelegate delegate =
                (TestDelegate) DelegateFactory.newDelegate(context);
        assertTrue(delegate.isPluginInitialized);
    }

}