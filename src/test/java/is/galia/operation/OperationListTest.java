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

package is.galia.operation;

import is.galia.codec.VariantFormatException;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.delegate.Delegate;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.FormatRegistry;
import is.galia.image.Identifier;
import is.galia.image.Info;
import is.galia.image.MediaType;
import is.galia.image.MetaIdentifier;
import is.galia.image.MockNativeMetadata;
import is.galia.image.NativeMetadata;
import is.galia.image.Region;
import is.galia.image.ScaleConstraint;
import is.galia.operation.overlay.BasicStringOverlayServiceTest;
import is.galia.operation.overlay.Overlay;
import is.galia.operation.redaction.Redaction;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OperationListTest extends BaseTest {

    @Nested
    class BuilderTest extends BaseTest {

        private OperationList.Builder instance;

        @BeforeEach
        public void setUp() throws Exception {
            super.setUp();
            instance = OperationList.builder();
        }

        @Test
        void testBuildWithNoPropertiesSet() {
            OperationList opList = instance.build();
            assertNull(opList.getMetaIdentifier());
            assertNull(opList.getIdentifier());
            assertTrue(opList.getOperations().isEmpty());
            assertTrue(opList.getOptions().isEmpty());
            assertEquals(new ScaleConstraint(1, 1), opList.getScaleConstraint());
        }

        @Test
        void testBuildWithAllPropertiesSet() {
            // identifier
            Identifier identifier = new Identifier("cats");
            // meta-identifier
            int pageNumber = 4;
            MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                    .withIdentifier(identifier)
                    .withPageNumber(pageNumber)
                    .build();
            // operations
            List<Operation> operations = List.of(
                    new ScaleByPercent(0.5),
                    new ScaleByPercent(0.4));
            // options
            Map<String,String> options = Map.of("key", "value");

            OperationList opList = instance
                    .withIdentifier(identifier)
                    .withMetaIdentifier(metaIdentifier)
                    .withOperations(operations.toArray(Operation[]::new))
                    .withOptions(options)
                    .build();
            assertEquals(identifier, opList.getIdentifier());
            assertEquals(metaIdentifier, opList.getMetaIdentifier());
            assertEquals(operations, opList.getOperations());
            assertEquals(options, opList.getOptions());
            assertEquals(4, opList.getMetaIdentifier().pageNumber());
        }

    }

    private static final double DELTA = 0.00000001;

    private OperationList instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new OperationList();
    }

    /* OperationList() */

    @Test
    void noOpConstructor() {
        assertNotNull(instance.getOptions());
        assertFalse(instance.getScaleConstraint().hasEffect());
    }

    /* OperationList(Identifier) */

    @Test
    void identifierConstructor() {
        instance = new OperationList(new Identifier("cats"));
        assertEquals("cats", instance.getIdentifier().toString());
    }

    /* OperationList(MetaIdentifier) */

    @Test
    void metaIdentifierConstructor() {
        instance = new OperationList(new MetaIdentifier("cats"));
        assertEquals("cats", instance.getMetaIdentifier().toString());
    }

    /* add(Operation) */

    @Test
    void add1() {
        assertFalse(instance.iterator().hasNext());

        instance.add(new Rotate());
        assertTrue(instance.iterator().hasNext());
    }

    @Test
    void add1WithNullArgument() {
        instance.add(null);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void add1WhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.add(new Rotate()));
    }

    /* add(int, Operation) */

    @Test
    void add2() {
        instance.add(new Rotate());
        instance.add(new Rotate());
        instance.add(new Rotate());
        instance.add(1, new ScaleByPercent());

        Iterator<Operation> it = instance.iterator();
        it.next();
        assertInstanceOf(ScaleByPercent.class, it.next());
    }

    @Test
    void add2WithNullOperationArgument() {
        instance.add(0, null);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void add2WithIllegalIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.add(8, new Rotate()));
    }

    @Test
    void add2WhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.add(0, new Rotate()));
    }

    /* addAfter() */

    @Test
    void addAfterWithExistingClass() {
        instance = OperationList.builder()
                .withOperations(new Rotate())
                .build();
        instance.addAfter(new ScaleByPercent(), Rotate.class);
        Iterator<Operation> it = instance.iterator();

        assertInstanceOf(Rotate.class, it.next());
        assertInstanceOf(Scale.class, it.next());
    }

    @Test
    void addAfterWithExistingSuperclass() {
        instance.add(new MockOverlay());

        class SubMockOverlay extends MockOverlay {}

        instance.addAfter(new SubMockOverlay(), Overlay.class);
        Iterator<Operation> it = instance.iterator();
        assertInstanceOf(MockOverlay.class, it.next());
        assertInstanceOf(SubMockOverlay.class, it.next());
    }

    @Test
    void addAfterWithoutExistingClass() {
        instance.add(new Rotate());
        instance.addAfter(new ScaleByPercent(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertInstanceOf(Rotate.class, it.next());
        assertInstanceOf(Scale.class, it.next());
    }

    @Test
    void addAfterWithNullArgument() {
        instance.addAfter(null, Scale.class);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void addAfterWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.addAfter(new Rotate(), Crop.class));
    }

    /* addBefore() */

    @Test
    void addBeforeWithExistingClass() {
        instance.add(new Rotate());
        instance.addBefore(new ScaleByPercent(), Rotate.class);
        assertInstanceOf(Scale.class, instance.iterator().next());
    }

    @Test
    void addBeforeWithExistingSuperclass() {
        class SubMockOverlay extends MockOverlay {}

        instance.add(new MockOverlay());
        instance.addBefore(new SubMockOverlay(), MockOverlay.class);
        assertInstanceOf(SubMockOverlay.class, instance.iterator().next());
    }

    @Test
    void addBeforeWithoutExistingClass() {
        instance.add(new Rotate());
        instance.addBefore(new ScaleByPercent(), Crop.class);
        Iterator<Operation> it = instance.iterator();
        assertInstanceOf(Rotate.class, it.next());
        assertInstanceOf(Scale.class, it.next());
    }

    @Test
    void addBeforeWithNullArgument() {
        instance.addBefore(null, Scale.class);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void addBeforeWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.addBefore(new Rotate(), Crop.class));
    }

    /* applyNonEndpointMutations() */

    @Test
    void applyNonEndpointMutationsWithScaleConstraintAndNoScaleOperationAddsOne() {
        final Size fullSize = new Size(2000, 1000);
        final Info info = Info.builder()
                .withSize(fullSize)
                .build();
        final Identifier identifier = new Identifier("cats");
        final OperationList opList = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new CropByPixels(0, 0, 70, 30),
                        new Encode(Format.get("jpg")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Scale expectedScale = new ScaleByPercent();
        Scale actualScale = (Scale) opList.getFirst(Scale.class);
        assertEquals(expectedScale, actualScale);
    }

    @Test
    void applyNonEndpointMutationsWithBackgroundColor() {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.PROCESSOR_BACKGROUND_COLOR, "white");

        final Size fullSize   = new Size(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Rotate(45), new Encode(Format.get("jpg")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Encode encode = (Encode) opList.getFirst(Encode.class);
        assertEquals(Color.fromString("#FFFFFF"), encode.getBackgroundColor());
    }

    @Test
    void applyNonEndpointMutationsWithOverlay() {
        BasicStringOverlayServiceTest.setUpConfiguration();

        final Size fullSize   = new Size(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Overlay overlay = (Overlay) opList.getFirst(Overlay.class);
        assertEquals(10, overlay.getInset());
    }

    @Test
    void applyNonEndpointMutationsWithRedactions() {
        final Identifier identifier = new Identifier("redacted");
        final Size fullSize    = new Size(2000, 1000);
        final Info info             = Info.builder().withSize(fullSize).build();
        final OperationList opList  = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(new Encode(Format.get("jpg")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Redaction redaction = (Redaction) opList.getFirst(Redaction.class);
        assertEquals(new Region(0, 10, 50, 70), redaction.getRegion());
    }

    @Test
    void applyNonEndpointMutationsWithLinearScale() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.PROCESSOR_DOWNSCALE_LINEAR, true);

        final Size fullSize   = new Size(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(0.5),
                        new Encode(Format.get("jpg")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Iterator<Operation> it = opList.iterator();
        assertTrue(((Scale) it.next()).isLinear());
    }

    @Test
    void applyNonEndpointMutationsWithDownscaleFilter() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.PROCESSOR_DOWNSCALE_FILTER, "bicubic");

        final Size fullSize   = new Size(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(0.5),
                        new Encode(Format.get("jpg")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());
    }

    @Test
    void applyNonEndpointMutationsWithUpscaleFilter() {
        Configuration config = Configuration.forApplication();
        config.setProperty(Key.PROCESSOR_UPSCALE_FILTER, "triangle");

        final Size fullSize   = new Size(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new ScaleByPercent(1.5),
                        new Encode(Format.get("jpg")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.TRIANGLE, ((Scale) it.next()).getFilter());
    }

    @Test
    void applyNonEndpointMutationsWithSharpening() {
        final Configuration config = Configuration.forApplication();
        config.setProperty(Key.PROCESSOR_SHARPEN, 0.2f);

        final Size fullSize   = new Size(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        Iterator<Operation> it = opList.iterator();
        assertInstanceOf(Sharpen.class, it.next());

        Sharpen sharpen = (Sharpen) opList.getFirst(Sharpen.class);
        assertEquals(0.2, sharpen.getAmount(), DELTA);
    }

    @Test
    void applyNonEndpointMutationsWithXMPMetadata() {
        final Identifier identifier = new Identifier("metadata");
        final Size fullSize    = new Size(2000, 1000);
        final Info info             = Info.builder().withSize(fullSize).build();
        final Encode encode         = new Encode(Format.get("jpg"));
        final String xmp            = "<rdf:RDF>source metadata</rdf:RDF>";
        encode.setXMP(xmp);
        final OperationList opList = OperationList.builder()
                .withIdentifier(identifier)
                .withOperations(encode)
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(identifier);
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        assertEquals("<rdf:RDF>variant metadata</rdf:RDF>",
                encode.getXMP().orElseThrow());
    }

    @Test
    void applyNonEndpointMutationsWithNativeMetadata() {
        final Size fullSize     = new Size(2000, 1000);
        final Info info              = Info.builder().withSize(fullSize).build();
        final Encode encode          = new Encode(Format.get("jpg"));
        final NativeMetadata metadata = new MockNativeMetadata();
        encode.setNativeMetadata(metadata);
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("metadata"))
                .withOperations(encode)
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        assertSame(encode.getNativeMetadata().orElseThrow(), metadata);
    }

    @Test
    void applyNonEndpointMutationsCopiesEncoderOptionsFromAppConfiguration() {
        final Configuration config = Configuration.forApplication();
        final String key1          = Encode.OPTION_PREFIX + "option1";
        final String key2          = Encode.OPTION_PREFIX + "option2";
        config.setProperty(key1, "value");
        config.setProperty(key2, "value");
        final Size fullSize = new Size(2000, 1000);
        final Info info          = Info.builder().withSize(fullSize).build();
        final Encode encode      = new Encode(Format.get("jpg"));
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("metadata"))
                .withOperations(encode)
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        assertEquals("value", encode.getOptions().getProperty(key1));
        assertEquals("value", encode.getOptions().getProperty(key2));
    }

    @Test
    void applyNonEndpointMutationsDoesNotCopyNonEncoderOptionsFromAppConfiguration() {
        final Configuration config = Configuration.forApplication();
        final String key1          = "option1"; // missing required prefix
        final String key2          = "option2";
        config.setProperty(key1, "value");
        config.setProperty(key2, "value");
        final Size fullSize = new Size(2000, 1000);
        final Info info          = Info.builder().withSize(fullSize).build();
        final Encode encode      = new Encode(Format.get("jpg"));
        final OperationList opList = OperationList.builder()
                .withIdentifier(new Identifier("metadata"))
                .withOperations(encode)
                .build();

        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList);

        opList.applyNonEndpointMutations(info, delegate);

        assertFalse(encode.getOptions().getKeys().hasNext());
    }

    @Test
    void applyNonEndpointMutationsWhileFrozen() {
        final Size fullSize   = new Size(2000, 1000);
        final Info info            = Info.builder().withSize(fullSize).build();
        final OperationList opList = OperationList.builder()
                .withOperations(new CropByPixels(0, 0, 70, 30))
                .build();

        opList.freeze();
        Delegate delegate = TestUtils.newDelegate();

        assertThrows(IllegalStateException.class,
                () -> opList.applyNonEndpointMutations(info, delegate));
    }

    @Test
    void applyNonEndpointMutationsInvokedMultipleTimes() {
        BasicStringOverlayServiceTest.setUpConfiguration();
        final Size fullSize = new Size(2000, 1000);
        final Info info          = Info.builder().withSize(fullSize).build();

        OperationList opList1 = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setOperationList(opList1);
        opList1.applyNonEndpointMutations(info, delegate);

        OperationList opList2 = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("tif")))
                .build();
        delegate.getRequestContext().setOperationList(opList2);
        opList2.applyNonEndpointMutations(info, delegate);
        opList2.applyNonEndpointMutations(info, delegate);

        assertEquals(opList1, opList2);
    }

    /* clear() */

    @Test
    void clear() {
        instance.add(new CropByPixels(10, 10, 10, 10));
        instance.add(new ScaleByPercent(0.5));

        int opCount = 0;
        Iterator<Operation> it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(2, opCount);
        instance.clear();

        opCount = 0;
        it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(0, opCount);
    }

    @Test
    void clearWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class, () -> instance.clear());
    }

    /* equals() */

    @Test
    void equalsWithEqualInstance() {
        OperationList ops1 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        OperationList ops2 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        assertEquals(ops1, ops2);
    }

    @Test
    void equalsWithUnequalInstance() {
        OperationList ops1 = OperationList.builder()
                .withOperations(new Rotate(1)).build();
        OperationList ops2 = OperationList.builder()
                .withOperations(new Rotate(2)).build();
        assertNotEquals(ops1, ops2);
    }

    /* freeze() */

    @Test
    void freezeFreezesOperations() {
        instance.add(new CropByPixels(0, 0, 10, 10));
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> ((CropByPixels) instance.getFirst(CropByPixels.class)).setHeight(300));
    }

    /* getFirst() */

    @Test
    void getFirst() {
        instance.add(new ScaleByPercent(0.5));

        assertNull(instance.getFirst(Crop.class));
        assertNotNull(instance.getFirst(Scale.class));
    }

    @Test
    void getFirstWithSuperclass() {
        instance.add(new MockOverlay());

        Overlay overlay = (Overlay) instance.getFirst(Overlay.class);
        assertNotNull(overlay);
        assertInstanceOf(MockOverlay.class, overlay);
    }

    /* getIdentifier() */

    @Test
    void getIdentifierReturnsIdentifierIfSet() {
        final Identifier identifier = new Identifier("cats");
        instance.setIdentifier(identifier);
        instance.setMetaIdentifier(null);
        assertEquals(identifier, instance.getIdentifier());
    }

    @Test
    void getIdentifierFallsBacktoMetaIdentifierIdentifier() {
        instance.setIdentifier(null);
        instance.setMetaIdentifier(new MetaIdentifier("cats"));
        assertEquals(new Identifier("cats"), instance.getIdentifier());
    }

    /* getOperations() */

    @Test
    void getOperationsReturnedInstanceIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getOperations().add(new ScaleByPercent()));
    }

    /* getOptions() */

    @Test
    void getOptions() {
        assertNotNull(instance.getOptions());
    }

    @Test
    void getOptionsWhenFrozen() {
        instance.freeze();
        assertThrows(UnsupportedOperationException.class,
                () -> instance.getOptions().put("test", "test"));
    }

    /* getOutputFormat() */

    @Test
    void getOutputFormatReturnsEncodeFormatWhenPresent() {
        Format format = Format.get("jpg");
        instance.add(new Encode(format));
        assertEquals(format, instance.getOutputFormat());
    }

    @Test
    void getOutputFormatReturnsNullWhenEncodeNotPresent() {
        assertNull(instance.getOutputFormat());
    }

    /* getPageIndex() */

    @Test
    void getPageIndexReturns0WhenMetaIdentifierIsNotSet() {
        assertEquals(0, instance.getPageIndex());
    }

    @Test
    void getPageIndexReturns0WhenMetaIdentifierIsSetWithoutPageNumber() {
        instance.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier(new Identifier("cats"))
                .build());
        assertEquals(0, instance.getPageIndex());
    }

    @Test
    void getPageIndexReturnsThatOfMetaIdentifierWhenMetaIdentifierPageNumberIsSet() {
        instance.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier(new Identifier("cats"))
                .withPageNumber(5)
                .build());
        assertEquals(4, instance.getPageIndex());
    }

    /* getResultingSize() */

    @Test
    void getResultingSize() {
        Size fullSize   = new Size(300, 200);
        ScaleByPercent scale = new ScaleByPercent();
        Rotate rotate        = new Rotate();
        instance.add(scale);
        instance.add(rotate);
        assertEquals(fullSize, instance.getResultingSize(fullSize));

        instance  = new OperationList();
        Crop crop = new CropByPercent(0, 0, 0.5, 0.5);
        scale     = new ScaleByPercent(0.5);
        instance.add(crop);
        instance.add(scale);
        assertEquals(new Size(75, 50), instance.getResultingSize(fullSize));
    }

    /* getScaleConstraint() */

    @Test
    void getScaleConstraintReturnsOneWhenMetaIdentifierIsNotSet() {
        assertEquals(new ScaleConstraint(1, 1), instance.getScaleConstraint());
    }

    @Test
    void getScaleConstraintReturnsOneWhenNotSetOnMetaIdentifier() {
        instance.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier(new Identifier("cats"))
                .withScaleConstraint(1, 1)
                .build());
        assertEquals(new ScaleConstraint(1, 1), instance.getScaleConstraint());
    }

    @Test
    void getScaleConstraintReturnsScaleConstraintWhenSetOnMetaIdentifier() {
        instance.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2)
                .build());
        assertEquals(new ScaleConstraint(1, 2), instance.getScaleConstraint());
    }

    /* hasEffect() */

    @Test
    void hasEffectWithScaleConstraint() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        Size fullSize = new Size(100, 100);
        assertFalse(instance.hasEffect(fullSize, Format.get("gif")));
        instance.setMetaIdentifier(MetaIdentifier.builder()
                .withIdentifier("cats")
                .withScaleConstraint(1, 2)
                .build());
        assertTrue(instance.hasEffect(fullSize, Format.get("gif")));
    }

    @Test
    void hasEffectWithSameFormat() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        assertFalse(instance.hasEffect(new Size(100, 100), Format.get("gif")));
    }

    @Test
    void hasEffectWithDifferentFormats() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("gif")))
                .build();
        assertTrue(instance.hasEffect(new Size(100, 100), Format.get("jpg")));
    }

    @Test
    void hasEffectWithPDFSourceAndPDFOutputAndOverlay() {
        FormatRegistry.addFormat(
                new Format("pdf", "PDF",
                List.of(new MediaType("application", "pdf")),
                List.of("pdf"),
                false, false, true));
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("pdf")))
                .build();
        assertFalse(instance.hasEffect(new Size(100, 100), Format.get("pdf")));
    }

    @Test
    void hasEffectWithEncodeAndSameOutputFormat() {
        instance = OperationList.builder()
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        assertFalse(instance.hasEffect(new Size(100, 100), Format.get("jpg")));
    }

    /* hashCode() */

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    /* iterator() */

    @Test
    void iterator() {
        instance.add(new CropByPixels(10, 10, 10, 10));
        instance.add(new ScaleByPercent(0.5));

        int count = 0;
        Iterator<Operation> it = instance.iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void iteratorCannotRemoveWhileFrozen() {
        instance.add(new ScaleByPercent(50.5));
        instance.freeze();
        Iterator<Operation> it = instance.iterator();
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /* replace() */

    @Test
    void replaceWithNullSourceArgument() {
        assertThrows(NullPointerException.class,
                () -> instance.replace(null, new Rotate()));
    }

    @Test
    void replaceWithNullTargetArgument() {
        assertThrows(NullPointerException.class,
                () -> instance.replace(new Rotate(), null));
    }

    @Test
    void replaceWithSameSourceAndTargetArguments() {
        Operation op1 = new Rotate();
        Operation op2 = op1;
        instance.add(op1);
        instance.replace(op1, op2);
        assertSame(op1, op2);
    }

    @Test
    void replaceWithInvalidSourceArgument() {
        // The first op is not present in the list.
        assertThrows(IllegalArgumentException.class,
                () -> instance.replace(new Rotate(), new Rotate()));
    }

    @Test
    void replaceWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.replace(new Rotate(), new Rotate()));
    }

    @Test
    void replace() {
        Operation op1   = new Rotate();
        Operation op2   = new CropByPercent();
        Operation op3   = new CropByPixels(0, 0, 10, 10);
        Operation newOp = new ScaleByPercent();
        instance.add(op1);
        instance.add(op2);
        instance.add(op3);
        instance.replace(op2, newOp);
        assertSame(newOp, instance.getOperations().get(1));
        assertFalse(instance.getOperations().contains(op2));
    }

    /* remove() */

    @Test
    void removeWithPresentOperation() {
        Operation op = new Rotate();
        instance.add(op);
        instance.remove(op);
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void removeWithAbsentOperation() {
        assertFalse(instance.iterator().hasNext());
        instance.remove(new Rotate());
        assertFalse(instance.iterator().hasNext());
    }

    @Test
    void removeWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.remove(new Rotate()));
    }

    /* setIdentifier() */

    @Test
    void setIdentifierWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setIdentifier(new Identifier("alpaca")));
    }

    /* setMetaIdentifier() */

    @Test
    void setMetaIdentifier() {
        MetaIdentifier metaIdentifier = new MetaIdentifier("cats");
        instance.setMetaIdentifier(metaIdentifier);
        assertEquals(metaIdentifier, instance.getMetaIdentifier());

        instance.setMetaIdentifier(null);
        assertNull(instance.getMetaIdentifier());
    }

    @Test
    void setMetaIdentifierWhileFrozen() {
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setMetaIdentifier(new MetaIdentifier("cats")));
    }

    /* toFilename() */

    @Test
    void toFilename() {
        final Identifier identifier = new Identifier("identifier.jpg");
        instance = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withPageNumber(4)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new CropByPixels(5, 6, 20, 22),
                        new ScaleByPercent(0.4),
                        new Rotate(15),
                        ColorTransform.BITONAL,
                        new Encode(Format.get("jpg")))
                .withOptions(Map.of("animal", "cat"))
                .build();

        String expected = "50c63748527e634134449ae20b199cc0_a0d3874a8cb151268419132fc22c8621.jpg";
        assertEquals(expected, instance.toFilename());

        // Assert that changing an operation changes the filename
        CropByPixels crop = (CropByPixels) instance.getFirst(CropByPixels.class);
        crop.setX(12);
        assertNotEquals(expected, instance.toFilename());

        // Assert that changing an option changes the filename
        crop.setX(10);
        instance.getOptions().put("animal", "dog");
        assertNotEquals(expected, instance.toFilename());
    }

    /* toMap() */

    @Test
    @SuppressWarnings("unchecked")
    void toMap() {
        Identifier identifier = new Identifier("identifier.jpg");
        instance = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withPageNumber(4)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new CropByPixels(2, 4, 50, 50),
                        new ScaleByPercent(),
                        new Rotate(0),
                        Transpose.HORIZONTAL,
                        new Encode(Format.get("jpg")))
                .build();

        final Size fullSize = new Size(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals("identifier.jpg", map.get("identifier"));
        assertEquals(3, map.get("page_index"));
        assertEquals(4, ((List<?>) map.get("operations")).size());
        assertTrue(((Map<?, ?>) map.get("options")).isEmpty());
        assertEquals(1, (long) ((Map<String,Long>) map.get("scale_constraint")).get("numerator"));
        assertEquals(2, (long) ((Map<String,Long>) map.get("scale_constraint")).get("denominator"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void toMapWithUnsetProperties() {
        instance = new OperationList();
        final Size fullSize = new Size(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertNull(map.get("identifier"));
        assertEquals(0, map.get("page_index"));
        assertTrue(((List<?>) map.get("operations")).isEmpty());
        assertTrue(((Map<?, ?>) map.get("options")).isEmpty());
        assertEquals(1, (long) ((Map<String,Long>) map.get("scale_constraint")).get("numerator"));
        assertEquals(1, (long) ((Map<String,Long>) map.get("scale_constraint")).get("denominator"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Size fullSize = new Size(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    /* toString() */

    @Test
    void testToString() {
        final Identifier identifier = new Identifier("identifier.jpg");
        instance = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withPageNumber(3)
                        .withScaleConstraint(1, 2).build())
                .withOperations(
                        new CropByPixels(5, 6, 20, 22),
                        new ScaleByPercent(0.4),
                        new Rotate(15),
                        ColorTransform.BITONAL,
                        new Encode(Format.get("jpg")))
                .withOptions(Map.of("animal", "cat"))
                .build();
        String expected = "identifier.jpg_2_1:2_cropbypixels:5,6,20,22_scalebypercent:40%_rotate:15_colortransform:bitonal_encode:jpg_8_animal:cat";
        assertEquals(expected, instance.toString());
    }

    /* validate() */

    @Test
    void validateWithValidInstance() throws Exception {
        Size fullSize = new Size(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 100, 100),
                        new Encode(Format.get("jpg")))
                .build();
        ops.validate(fullSize, Format.get("png"));
    }

    @Test
    void validateWithMissingIdentifier() {
        Size fullSize = new Size(1000, 1000);
        OperationList ops = OperationList.builder()
                .withOperations(
                        new CropByPixels(0, 0, 100, 100),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(OperationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithMissingEncodeOperation() {
        Size fullSize = new Size(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new CropByPixels(0, 0, 100, 100))
                .build();
        assertThrows(OperationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithUnsupportedEncodeFormat() {
        Size fullSize = new Size(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 100, 100),
                        new Encode(Format.UNKNOWN))
                .build();
        assertThrows(VariantFormatException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithOutOfBoundsCrop() {
        Size fullSize = new Size(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(1001, 1001, 100, 100),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(OperationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithZeroResultingArea() {
        Size fullSize = new Size(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(
                        new CropByPixels(0, 0, 10, 10),
                        new ScaleByPercent(0.0001),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(OperationException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithScaleGreaterThanMaxAllowed() {
        Size fullSize = new Size(1000, 1000);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 8)
                        .build())
                .withOperations(
                        new ScaleByPercent(4),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(IllegalScaleException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithAllowedSmallerScale() throws Exception {
        Size fullSize = new Size(2000, 1000);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new ScaleByPixels(100, 50, ScaleByPixels.Mode.NON_ASPECT_FILL),
                        new Encode(Format.get("png")))
                .build();
        ops.validate(fullSize, Format.get("png"));
    }

    @Test
    void validateWithMaxAllowedScale() throws Exception {
        Size fullSize = new Size(2000, 1000);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new ScaleByPixels(1000, 500, ScaleByPixels.Mode.NON_ASPECT_FILL),
                        new Encode(Format.get("png")))
                .build();
        ops.validate(fullSize, Format.get("png"));
    }

    @Test
    void validateWithScaleGreaterThanMaxAllowedBy1Pixel() throws Exception {
        Size fullSize = new Size(639, 343);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new ScaleByPixels(320, 172, ScaleByPixels.Mode.NON_ASPECT_FILL),
                        new Encode(Format.get("png")))
                .build();
        ops.validate(fullSize, Format.get("png"));
    }

    @Test
    void validateWithScaleGreaterThanMaxAllowedBy2Pixels() {
        Size fullSize = new Size(639, 343);
        Identifier identifier = new Identifier("cats");
        OperationList ops = OperationList.builder()
                .withIdentifier(identifier)
                .withMetaIdentifier(MetaIdentifier.builder()
                        .withIdentifier(identifier)
                        .withScaleConstraint(1, 2)
                        .build())
                .withOperations(
                        new ScaleByPixels(321, 173, ScaleByPixels.Mode.NON_ASPECT_FILL),
                        new Encode(Format.get("jpg")))
                .build();
        assertThrows(IllegalScaleException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

    @Test
    void validateWithAreaGreaterThanMaxAllowed() {
        Configuration.forApplication().setProperty(Key.MAX_PIXELS, 100);
        Size fullSize = new Size(1000, 1000);
        OperationList ops = OperationList.builder()
                .withIdentifier(new Identifier("cats"))
                .withOperations(new Encode(Format.get("jpg")))
                .build();
        assertThrows(IllegalSizeException.class,
                () -> ops.validate(fullSize, Format.get("png")));
    }

}
