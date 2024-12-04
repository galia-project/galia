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
import is.galia.image.Format;
import is.galia.image.Identifier;
import is.galia.operation.Color;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.awt.font.TextAttribute;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class DelegateOverlayServiceTest extends BaseTest {

    private DelegateOverlayService instance;

    /* isAvailable() */

    @Test
    void isAvailableWhenAvailable() {
        TestUtils.newDelegate();
        instance = new DelegateOverlayService(null);
        assertTrue(instance.isAvailable());
    }

    @Test
    void isAvailableWhenNotAvailable() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.DELEGATE_ENABLED, false);
        instance = new DelegateOverlayService(null);
        assertFalse(instance.isAvailable());
    }

    /* newOverlay() */

    @Test
    void newOverlayReturningImageOverlay() throws Exception {
        final Identifier identifier = new Identifier("image");
        final OperationList opList  = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        delegate.getRequestContext().setOperationList(opList);

        instance = new DelegateOverlayService(delegate);

        final ImageOverlay overlay = (ImageOverlay) instance.newOverlay();
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(new URI("file:///C:/dev/cats"), overlay.getURI());
        } else {
            assertEquals(new URI("file:///dev/cats"), overlay.getURI());
        }
        assertEquals((long) 5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
    }

    @Test
    void newOverlayReturningStringOverlay() throws Exception {
        final Identifier identifier = new Identifier("string");
        final OperationList opList  = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        delegate.getRequestContext().setOperationList(opList);

        instance = new DelegateOverlayService(delegate);

        final StringOverlay overlay = (StringOverlay) instance.newOverlay();
        assertEquals("dogs\ndogs", overlay.getString());
        assertEquals("SansSerif", overlay.getFont().getName());
        assertEquals(20, overlay.getFont().getSize());
        assertEquals(11, overlay.getMinSize());
        assertEquals(1.5f, overlay.getFont().getAttributes().get(TextAttribute.WEIGHT));
        assertEquals(0.1f, overlay.getFont().getAttributes().get(TextAttribute.TRACKING));
        assertEquals((long) 5, overlay.getInset());
        assertEquals(Position.BOTTOM_LEFT, overlay.getPosition());
        assertEquals(Color.RED, overlay.getColor());
        assertEquals(Color.BLUE, overlay.getStrokeColor());
        assertEquals(new Color(12, 23, 34, 45), overlay.getBackgroundColor());
        assertEquals(3, overlay.getStrokeWidth(), 0.00001f);
        assertFalse(overlay.isWordWrap());
    }

    @Test
    void newOverlayReturningNull() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("whatever"));
        instance = new DelegateOverlayService(delegate);

        Overlay overlay = instance.newOverlay();
        assertNull(overlay);
    }

}
