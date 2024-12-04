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

package is.galia.codec.tiff;

import is.galia.codec.BufferedImageSequence;
import is.galia.codec.Decoder;
import is.galia.image.Size;
import is.galia.image.MediaType;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.Format;
import is.galia.image.ReductionFactor;
import is.galia.codec.SourceFormatException;
import is.galia.codec.AbstractImageIODecoder;
import is.galia.codec.DecoderHint;
import is.galia.processor.Java2DUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O TIFF {@link
 * javax.imageio.ImageReader}.
 */
public final class TIFFDecoder extends AbstractImageIODecoder
        implements Decoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TIFFDecoder.class);

    private static final double DELTA = 0.00000001;

    static final Format FORMAT = new Format(
            "tif",                                   // key
            "TIFF",                                  // name
            List.of(new MediaType("image", "tiff")), // media types
            List.of("tif", "tiff", "ptif", "tf8"),   // extensions
            true,                                    // isRaster
            false,                                   // isVideo
            true);                                   // supportsTransparency

    private List<Directory> exifIFDs;

    @Override
    public boolean canSeek() {
        return true;
    }

    @Override
    public Format detectFormat() throws IOException {
        initializeInputStream();
        inputStream.seek(0);
        byte[] magicBytes = new byte[4];
        int b, i = 0;
        while ((b = inputStream.read()) != -1 && i < magicBytes.length) {
            magicBytes[i] = (byte) b;
            i++;
        }
        inputStream.seek(0);
        if ((magicBytes[0] == 0x49 && magicBytes[1] == 0x49 &&
                magicBytes[2] == 0x2a && magicBytes[3] == 0x00) ||
                (magicBytes[0] == 0x4d && magicBytes[1] == 0x4d &&
                        magicBytes[2] == 0x00 && magicBytes[3] == 0x2a)) {
            return FORMAT;
        }
        return Format.UNKNOWN;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Only a pyramidal TIFF will have more than one resolution; but there is
     * no simple flag in a pyramidal TIFF that indicates that it's pyramidal.
     * For files that contain multiple images, this method checks the
     * dimensions of each, and if they look like they comprise a pyramid based
     * on their dimensions, returns their count. If not, it returns {@code 1}.
     */
    @Override
    public int getNumResolutions() throws IOException {
        return isPyramidal() ? getNumImages() : 1;
    }

    @Override
    protected String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.tiff.TIFFImageReader" };
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(FORMAT);
    }

    private boolean isPyramidal() throws IOException {
        final int numImages         = getNumImages();
        final List<Size> sizes = new ArrayList<>(numImages);
        for (int i = 0; i < numImages; i++) {
            sizes.add(getSize(i));
        }
        return Size.isPyramid(sizes);
    }

    /**
     * <p>Override that is multi-resolution- and tile-aware.</p>
     *
     * <p>After reading, clients should check the decoder hints to see whether
     * the returned image will require additional scaling.</p>
     */
    @Override
    public BufferedImage decode(int imageIndex,
                                Region region,
                                double[] scales,
                                final ReductionFactor reductionFactor,
                                double[] diffScales,
                                final Set<DecoderHint> hints) throws IOException {
        region = region.oriented(getSize(imageIndex),
                readMetadata(imageIndex).getOrientation());
        BufferedImage image;
        if (getNumResolutions() > 1) {
            try {
                image = readSmallestUsableSubimage(
                        region, scales, reductionFactor);
                diffScales[0] = reductionFactor.findDifferentialScale(scales[0]);
                diffScales[1] = reductionFactor.findDifferentialScale(scales[1]);
                if (Math.abs(diffScales[0] - 1) > DELTA || Math.abs(diffScales[1] - 1) > DELTA) {
                    hints.add(DecoderHint.NEEDS_DIFFERENTIAL_SCALE);
                }
            } catch (IndexOutOfBoundsException e) {
                throw new SourceFormatException();
            }
        } else {
            image = readMonoResolution(imageIndex, region);
            hints.add(DecoderHint.IGNORED_SCALE);
        }
        if (image == null) {
            throw new SourceFormatException(iioReader.getFormatName());
        }
        // Convert CMYK to RGB if necessary
        if (image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
            image = Java2DUtils.convertCMYKToRGB(image);
        }
        return image;
    }

    @Override
    public Metadata readMetadata(int imageIndex) throws IOException {
        // Throw any contract-required exceptions.
        getSize(imageIndex);

        if (exifIFDs == null) {
            DirectoryReader reader = new DirectoryReader();
            reader.setSource(inputStream);
            TagSet tagSet = new EXIFBaselineTIFFTagSet();
            tagSet.addTag(TIFFMetadata.IPTC_POINTER_TAG);
            tagSet.addTag(TIFFMetadata.XMP_POINTER_TAG);
            reader.addTagSet(tagSet);
            inputStream.seek(0);
            exifIFDs = reader.readAll();
        }
        Directory dir = exifIFDs.get(imageIndex);
        return new TIFFMetadata(dir);
    }

    @Override
    public BufferedImageSequence decodeSequence() throws IOException {
        throw new UnsupportedOperationException();
    }

}
