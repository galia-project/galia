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

package is.galia.resource.iiif.v3;

import is.galia.delegate.Delegate;
import is.galia.image.Format;
import is.galia.image.Size;
import is.galia.operation.Crop;
import is.galia.operation.CropByPixels;
import is.galia.operation.Encode;
import is.galia.operation.Operation;
import is.galia.operation.OperationList;
import is.galia.operation.Rotate;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPercent;
import is.galia.resource.IllegalClientArgumentException;
import is.galia.resource.iiif.FormatException;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ParametersTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private Parameters instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Parameters(
                "identifier", "0,0,200,200", "pct:50", "5", "default", "jpg");
    }

    @Test
    void fromURI() {
        instance = Parameters.fromURI("bla/20,20,50,50/pct:90/15/bitonal.jpg");
        assertEquals("bla", instance.getIdentifier());
        assertEquals("20,20,50,50", instance.getRegion().toString());
        assertEquals(90, instance.getSize().getPercent(), DELTA);
        assertEquals(15, instance.getRotation().getDegrees(), DELTA);
        assertEquals(Quality.BITONAL, instance.getQuality());
        assertEquals(new OutputFormat("jpg"), instance.getOutputFormat());
    }

    @Test
    void fromURIWithInvalidURI1() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Parameters.fromURI("bla/20,20,50,50/15/bitonal.jpg"));
    }

    @Test
    void fromURIWithInvalidURI2() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Parameters.fromURI("bla/20,20,50,50/pct:90/15/bitonal"));
    }

    @Test
    void copyConstructor() {
        Parameters copy = new Parameters(instance);
        assertEquals(copy.getIdentifier(), instance.getIdentifier());
        assertEquals(copy.getRegion(), instance.getRegion());
        assertEquals(copy.getSize(), instance.getSize());
        assertEquals(copy.getRotation(), instance.getRotation());
        assertEquals(copy.getQuality(), instance.getQuality());
        assertEquals(copy.getOutputFormat(), instance.getOutputFormat());
        assertEquals(copy.getQuery(), instance.getQuery());
    }

    @Test
    void constructor3WithUnsupportedQuality() {
        assertThrows(IllegalClientArgumentException.class,
                () -> new Parameters(
                        "identifier", "0,0,200,200", "pct:50", "5", "bogus", "jpg"));
    }

    @Test
    void constructor3WithEmptyFormat() {
        assertThrows(FormatException.class, () ->
                new Parameters(
                        "identifier", "0,0,200,200", "pct:50", "5", "default", ""));
    }

    @Test
    void toOperationList() {
        Delegate delegate    = TestUtils.newDelegate();
        OperationList opList = instance.toOperationList(delegate, 1);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().identifier().toString());
        Iterator<Operation> it = opList.iterator();
        // Crop
        CropByPixels crop = (CropByPixels) it.next();
        assertEquals(0, crop.getX());
        assertEquals(0, crop.getY());
        assertEquals(200, crop.getWidth());
        assertEquals(200, crop.getHeight());
        // Scale
        ScaleByPercent scale = (ScaleByPercent) it.next();
        assertEquals(0.5, scale.getPercent());
        // Rotate
        Rotate rotate = (Rotate) it.next();
        assertEquals(5, rotate.getDegrees());
        // Format
        Encode encode = (Encode) it.next();
        assertEquals(Format.get("jpg"), encode.getFormat());

        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void toOperationListOmitsCropIfRegionIsFull() {
        Delegate delegate = TestUtils.newDelegate();
        instance = new Parameters(
                "identifier", "full", "pct:50", "5", "default", "jpg");
        OperationList opList = instance.toOperationList(delegate, 1);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().identifier().toString());
        Iterator<Operation> it = opList.iterator();
        assertInstanceOf(Scale.class, it.next());
        assertInstanceOf(Rotate.class, it.next());
    }

    @Test
    void toOperationListOmitsScaleIfSizeIsMax() {
        Delegate delegate = TestUtils.newDelegate();
        instance = new Parameters(
                "identifier", "0,0,30,30", "max", "5", "default", "jpg");
        OperationList opList = instance.toOperationList(delegate, 1);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().identifier().toString());
        Iterator<Operation> it = opList.iterator();
        assertInstanceOf(Crop.class, it.next());
        assertInstanceOf(Rotate.class, it.next());
    }

    @Test
    void toOperationListOmitsRotateIfRotationIsZero() {
        Delegate delegate = TestUtils.newDelegate();
        instance = new Parameters(
                "identifier", "0,0,30,30", "max", "0", "default", "jpg");
        OperationList opList = instance.toOperationList(delegate, 1);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().identifier().toString());
        Iterator<Operation> it = opList.iterator();
        assertInstanceOf(Crop.class, it.next());
        assertInstanceOf(Encode.class, it.next());
    }

    /**
     * N.B.: the individual path components are tested more thoroughly in the
     * specific component classes (e.g. {@link is.galia.resource.iiif.v3.Size} etc.).
     */
    @Test
    void toCanonicalString() {
        final is.galia.image.Size fullSize = new Size(1000, 800);
        assertEquals("identifier/0,0,200,200/500,400/5/default.jpg",
                instance.toCanonicalString(fullSize));
    }

    @Test
    void testToString() {
        assertEquals("identifier/0,0,200,200/pct:50/5/default.jpg",
                instance.toString());
    }

}
