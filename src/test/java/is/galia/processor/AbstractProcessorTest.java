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

package is.galia.processor;

import is.galia.codec.Decoder;
import is.galia.codec.DecoderFactory;
import is.galia.codec.Encoder;
import is.galia.codec.EncoderFactory;
import is.galia.codec.SourceFormatException;
import is.galia.image.Format;
import is.galia.image.MetaIdentifier;
import is.galia.operation.ColorTransform;
import is.galia.operation.Crop;
import is.galia.operation.CropByPercent;
import is.galia.operation.CropByPixels;
import is.galia.operation.CropToSquare;
import is.galia.operation.Encode;
import is.galia.operation.OperationList;
import is.galia.operation.Rotate;
import is.galia.operation.ScaleByPercent;
import is.galia.operation.ScaleByPixels;
import is.galia.operation.Transpose;
import is.galia.stream.ByteArrayImageInputStream;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.Path;

import static is.galia.test.Assert.ImageAssert.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Contains tests common to all {@link Processor}s.
 */
public abstract class AbstractProcessorTest extends BaseTest {

    protected static final double DELTA = 0.00000001;
    protected static final Path FIXTURE          =
            TestUtils.getSampleImage("jpg/rgb-64x56x8-baseline.jpg");
    protected static final Format FIXTURE_FORMAT = Format.get("jpg");
    protected static final int FIXTURE_WIDTH     = 64;
    protected static final int FIXTURE_HEIGHT    = 56;

    protected abstract Processor newInstance();

    private byte[] processToBytes(Decoder decoder,
                                  Encoder encoder,
                                  Path fixture,
                                  OperationList opList) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            decoder.setSource(fixture);
            Processor processor = newInstance();
            processor.setDecoder(decoder);
            processor.setEncoder(encoder);

            processor.process(opList, os);
            return os.toByteArray();
        }
    }

    private BufferedImage processToImage(Decoder decoder,
                                         Encoder encoder,
                                         Path fixture,
                                         OperationList opList) throws IOException {
        final byte[] imageBytes = processToBytes(
                decoder, encoder, fixture, opList);
        try (ByteArrayInputStream is = new ByteArrayInputStream(imageBytes)) {
            return ImageIO.read(is);
        }
    }

    @Test
    public void processWithMinimalOperations() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_WIDTH, image.getWidth());
            assertEquals(FIXTURE_HEIGHT, image.getHeight());
        }
    }

    @Test
    public void processWithOnlyNoOpOperations() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(
                            new ScaleByPercent(),
                            new Rotate(0),
                            encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_WIDTH, image.getWidth());
            assertEquals(FIXTURE_HEIGHT, image.getHeight());
        }
    }

    @Test
    public void processWithNonzeroOrientation() throws Exception {
        Format sourceFormat = Format.get("jpg");
        Format outputFormat = sourceFormat;
        Encode encode       = new Encode(outputFormat);
        Path fixture        = TestUtils.getSampleImage("jpg/exif-orientation-270.jpg");

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(sourceFormat, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new ScaleByPercent(), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, fixture, opList);

            assertEquals(FIXTURE_HEIGHT, image.getWidth());
            assertEquals(FIXTURE_WIDTH, image.getHeight());
        }
    }

    @Test
    public void processWithScaleConstraint() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withMetaIdentifier(MetaIdentifier.builder()
                            .withIdentifier("cats")
                            .withScaleConstraint(1, 2)
                            .build())
                    .withOperations(new ScaleByPercent(), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(32, image.getWidth());
            assertEquals(28, image.getHeight());
        }
    }

    @Test
    public void processWithSquareCropOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new CropToSquare(), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_HEIGHT, image.getWidth());
            assertEquals(FIXTURE_HEIGHT, image.getHeight());
        }
    }

    @Test
    public void processWithCropByPixelsOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(
                            new CropByPixels(10, 10, 35, 30),
                            encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(35, image.getWidth());
            assertEquals(30, image.getHeight());
        }
    }

    @Test
    public void processWithCropByPercentOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            final double width = 0.2, height = 0.2;
            Crop crop = new CropByPercent(0.2, 0.2, width, height);
            OperationList opList = OperationList.builder()
                    .withOperations(crop, encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            long expectedW = Math.round(FIXTURE_WIDTH * width);
            long expectedH = Math.round(FIXTURE_HEIGHT * height);
            assertTrue(Math.abs(expectedW - image.getWidth()) < 2);
            assertTrue(Math.abs(expectedH - image.getHeight()) < 2);
        }
    }

    @Test
    public void processWithNullScaleOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new ScaleByPercent(), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_WIDTH, image.getWidth());
            assertEquals(FIXTURE_HEIGHT, image.getHeight());
        }
    }

    @Test
    public void processWithScaleAspectFitWidthOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(
                            new ScaleByPixels(20, null, ScaleByPixels.Mode.ASPECT_FIT_WIDTH),
                            encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            double expectedHeight = 20 / (double) FIXTURE_WIDTH * FIXTURE_HEIGHT;
            assertEquals(20, image.getWidth());
            assertTrue(Math.abs(expectedHeight - image.getHeight()) < 1);
        }
    }

    @Test
    public void processWithScaleAspectFitHeightOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(
                            new ScaleByPixels(null, 20, ScaleByPixels.Mode.ASPECT_FIT_HEIGHT),
                            encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            double expectedWidth = 20 / (double) FIXTURE_HEIGHT * FIXTURE_WIDTH;
            assertTrue(Math.abs(expectedWidth - image.getWidth()) < 1);
            assertEquals(20, image.getHeight());
        }
    }

    @Test
    public void processWithDownscaleByPercentageOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new ScaleByPercent(0.5), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_WIDTH * 0.5, image.getWidth(), DELTA);
            assertEquals(FIXTURE_HEIGHT * 0.5, image.getHeight(), DELTA);
        }
    }

    @Test
    public void processWithUpscaleByPercentageOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new ScaleByPercent(1.5), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_WIDTH * 1.5, image.getWidth(), DELTA);
            assertEquals(FIXTURE_HEIGHT * 1.5, image.getHeight(), DELTA);
        }
    }

    @Test
    public void processWithAspectFitInsideScaleOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(
                            new ScaleByPixels(20, 20, ScaleByPixels.Mode.ASPECT_FIT_INSIDE),
                            encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            double expectedW = 20;
            double expectedH = FIXTURE_HEIGHT / (double) FIXTURE_WIDTH * 20;
            assertTrue(Math.abs(expectedW - image.getWidth()) < 1);
            assertTrue(Math.abs(expectedH - image.getHeight()) < 1);
        }
    }

    @Test
    public void processWithNonAspectFillScaleOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(
                            new ScaleByPixels(20, 20, ScaleByPixels.Mode.NON_ASPECT_FILL),
                            encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(20, image.getWidth());
            assertEquals(20, image.getHeight());
        }
    }

    @Test
    public void processWithTransposeOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(Transpose.HORIZONTAL, encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_WIDTH, image.getWidth(), DELTA);
            assertEquals(FIXTURE_HEIGHT, image.getHeight(), DELTA);
        }
    }

    @Test
    public void processWithRotate0DegreesOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new Rotate(0), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            assertEquals(FIXTURE_WIDTH, image.getWidth(), DELTA);
            assertEquals(FIXTURE_HEIGHT, image.getHeight(), DELTA);
        }
    }

    @Test
    public void processWithRotate275DegreesOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new Rotate(275), encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);

            final double radians = Math.toRadians(275);
            double expectedW = Math.abs(FIXTURE_WIDTH * Math.cos(radians)) +
                    Math.abs(FIXTURE_HEIGHT * Math.sin(radians));
            double expectedH = Math.abs(FIXTURE_WIDTH * Math.sin(radians)) +
                    Math.abs(FIXTURE_HEIGHT * Math.cos(radians));
            expectedW = Math.round(expectedW);
            expectedH = Math.round(expectedH);

            assertEquals(expectedW, image.getWidth());
            assertEquals(expectedH, image.getHeight());
        }
    }

    @Test
    public void processWithBitonalFilterOperation() throws Exception {
        Encode encode = new Encode(Format.get("png"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(ColorTransform.BITONAL, encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);
            assertBitonal(image);
        }
    }

    @Test
    public void processWithGrayscaleFilterOperation() throws Exception {
        Encode encode = new Encode(Format.get("jpg"));

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(FIXTURE_FORMAT, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(ColorTransform.GRAY, encode)
                    .build();
            BufferedImage image = processToImage(
                    decoder, encoder, FIXTURE, opList);
            assertGray(image);
        }
    }

    @Test
    public void processWithActualFormatDifferentFromSetFormat() throws Exception {
        Path fixture        = TestUtils.getFixture("unknown");
        Format sourceFormat = DecoderFactory.getAllSupportedFormats()
                .stream()
                .filter(f -> !"mock".equals(f.key()))
                .findAny()
                .orElseThrow();
        Format outputFormat = EncoderFactory.getAllSupportedFormats()
                .stream()
                .filter(f -> !"mock".equals(f.key()))
                .findAny()
                .orElseThrow();
        Encode encode = new Encode(outputFormat);

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(sourceFormat, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(encode)
                    .build();
            assertThrows(SourceFormatException.class, () ->
                    processToImage(decoder, encoder, fixture, opList));
        }
    }

    @Test
    public void processWithAlreadyOrientedDecoderHint() {
        // TODO: write this
    }

    @Test
    public void processWithIgnoredRegionDecoderHint() {
        // TODO: write this
    }

    @Test
    public void processWithIgnoredScaleDecoderHint() {
        // TODO: write this
    }

    @Test
    public void processWithNeedsDifferentialScaleDecoderHint() {
        // TODO: write this
    }

    @Test
    public void processWithAnimatedGIF() throws Exception {
        Path fixture  = TestUtils.getSampleImage("gif/animated-looping.gif");
        Format format = Format.get("gif");
        Encode encode = new Encode(format);

        try (Arena arena = Arena.ofConfined();
             Decoder decoder = DecoderFactory.newDecoder(format, arena);
             Encoder encoder = EncoderFactory.newEncoder(encode, arena)) {
            OperationList opList = OperationList.builder()
                    .withOperations(new Encode(format))
                    .build();
            byte[] imageBytes = processToBytes(decoder, encoder, fixture, opList);
            try (ImageInputStream is = new ByteArrayImageInputStream(imageBytes);
                 Decoder decoder2 = DecoderFactory.newDecoder(format, arena)) {
                decoder2.setSource(is);
                assertEquals(2, decoder2.getNumImages());
            }
        }
    }

    @Test
    public void setSourceWithUnsupportedSourceFormat() {
        Path fixture = TestUtils.getFixture("unknown");
        for (Format format : Format.all()) {
            try (Arena arena = Arena.ofConfined();
                 Decoder decoder = DecoderFactory.newDecoder(format, arena)) {
                decoder.setSource(fixture);
                Processor processor = newInstance();
                processor.setDecoder(decoder);
                if (EncoderFactory.getAllSupportedFormats().isEmpty()) {
                    fail("Expected exception");
                }
            } catch (SourceFormatException e) {
                // pass
            }
        }
    }

}
