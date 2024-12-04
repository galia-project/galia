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

package is.galia.operation.overlay;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.image.Identifier;
import is.galia.operation.Color;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OverlayFactoryTest extends BaseTest {

    private OverlayFactory instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration config = Configuration.forApplication();
        config.setProperty(Key.OVERLAY_ENABLED, true);
        config.setProperty(Key.OVERLAY_STRATEGY, "BasicStrategy");
        config.setProperty(Key.OVERLAY_TYPE, "image");
        config.setProperty(Key.OVERLAY_INSET, 10);
        config.setProperty(Key.OVERLAY_POSITION, "top left");
        config.setProperty(Key.OVERLAY_IMAGE, "/dev/null");

        instance = new OverlayFactory();
    }

    @Test
    void constructor() {
        assertEquals(OverlayFactory.Strategy.BASIC, instance.getStrategy());
    }

    @Test
    void newOverlayWithBasicImageStrategy() throws Exception {
        Optional<Overlay> result = instance.newOverlay(null);
        ImageOverlay overlay = (ImageOverlay) result.get();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/null"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/null"), overlay.getURI());
        }
        assertEquals(10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
    }

    @Test
    void newOverlayWithBasicStringStrategy() throws Exception {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.OVERLAY_TYPE, "string");
        config.setProperty(Key.OVERLAY_STRING_STRING, "cats");
        config.setProperty(Key.OVERLAY_STRING_COLOR, "green");
        instance = new OverlayFactory();

        Optional<Overlay> result = instance.newOverlay(null);
        StringOverlay overlay = (StringOverlay) result.get();
        assertEquals("cats", overlay.getString());
        assertEquals(10, overlay.getInset());
        assertEquals(Position.TOP_LEFT, overlay.getPosition());
        assertEquals(new Color(0, 128, 0), overlay.getColor());
    }

    @Test
    void newOverlayWithDelegateStrategyReturningImageOverlay() throws Exception {
        instance.setStrategy(OverlayFactory.Strategy.DELEGATE_METHOD);

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("image"));

        Optional<Overlay> result = instance.newOverlay(delegate);
        ImageOverlay overlay = (ImageOverlay) result.get();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/cats"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/cats"), overlay.getURI());
        }
        assertEquals(5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    void newOverlayWithDelegateStrategyReturningStringOverlay()
            throws Exception {
        instance.setStrategy(OverlayFactory.Strategy.DELEGATE_METHOD);

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("string"));

        Optional<Overlay> result = instance.newOverlay(delegate);
        StringOverlay overlay = (StringOverlay) result.get();
        assertEquals("dogs\ndogs", overlay.getString());
        assertEquals(5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    void newOverlayWithDelegateStrategyReturningNil() throws Exception {
        instance.setStrategy(OverlayFactory.Strategy.DELEGATE_METHOD);

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("bogus"));

        Optional<Overlay> result = instance.newOverlay(delegate);
        assertFalse(result.isPresent());
    }

}
