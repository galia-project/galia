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

package is.galia.codec.jpeg;

import is.galia.codec.Decoder;
import is.galia.codec.DecoderHint;
import is.galia.codec.iptc.DataSet;
import is.galia.codec.iptc.IIMReader;
import is.galia.codec.tiff.DirectoryIterator;
import is.galia.codec.tiff.DirectoryReader;
import is.galia.codec.tiff.EXIFBaselineTIFFTagSet;
import is.galia.codec.tiff.EXIFGPSTagSet;
import is.galia.codec.tiff.EXIFInteroperabilityTagSet;
import is.galia.codec.tiff.EXIFTagSet;
import is.galia.image.Size;
import is.galia.image.Format;
import is.galia.image.MediaType;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.codec.SourceFormatException;
import is.galia.codec.AbstractImageIODecoder;
import is.galia.processor.Java2DUtils;
import is.galia.stream.ByteArrayImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.color.ICC_Profile;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Implementation wrapping the default JDK Image I/O JPEG {@link
 * javax.imageio.ImageReader}.
 */
public final class JPEGDecoder extends AbstractImageIODecoder
        implements Decoder {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JPEGDecoder.class);

    static final Format FORMAT = new Format(
            "jpg",                                               // key
            "JPEG",                                              // name
            List.of(new MediaType("image", "jpeg")),             // media types
            List.of("jpg", "jpeg", "jpe", "jif", "jfif", "jfi"), // extensions
            true,                                                // isRaster
            false,                                               // isVideo
            false);                                              // supportsTransparency

    /**
     * There are a huge number of possible compressions. For now, we only
     * support the ones that can be decoded with the standard Image I/O
     * plugins.
     */
    private static final Set<Integer> SUPPORTED_THUMBNAIL_COMPRESSIONS = Set.of(
            6,      // JPEG (old-style)
            7,      // JPEG
            99,     // JPEG
            34892); // PNG

    private final JPEGMetadataReader metadataReader = new JPEGMetadataReader();
    private JPEGMetadata metadata;
    private transient BufferedImage cachedThumb;

    @Override
    public boolean canSeek() {
        return false;
    }

    @Override
    public Format detectFormat() throws IOException {
        initializeInputStream();
        inputStream.seek(0);
        byte[] magic = new byte[12];
        int b, i = 0;
        while ((b = inputStream.read()) != -1 && i < magic.length) {
            magic[i] = (byte) b;
            i++;
        }
        inputStream.seek(0);
        if ((magic[0] == (byte) 0xff && magic[1] == (byte) 0xd8 &&
                magic[2] == (byte) 0xff && magic[3] == (byte) 0xdb) ||
                (magic[0] == (byte) 0xff &&
                        magic[1] == (byte) 0xd8 &&
                        magic[2] == (byte) 0xff &&
                        magic[3] == (byte) 0xe0 &&
                        magic[4] == 0x00 &&
                        magic[5] == 0x10 &&
                        magic[6] == 0x4a &&
                        magic[7] == 0x46 &&
                        magic[8] == 0x49 &&
                        magic[9] == 0x46 &&
                        magic[10] == 0x00 &&
                        magic[11] == 0x01) ||
                (magic[0] == (byte) 0xff &&
                        magic[1] == (byte) 0xd8 &&
                        magic[2] == (byte) 0xff &&
                        magic[3] == (byte) 0xee) ||
                (magic[0] == (byte) 0xff &&
                        magic[1] == (byte) 0xd8 &&
                        magic[2] == (byte) 0xff &&
                        magic[3] == (byte) 0xe1 &&
                        magic[6] == 0x45 &&
                        magic[7] == 0x78 &&
                        magic[8] == 0x69 &&
                        magic[9] == 0x66 &&
                        magic[10] == 0x00 &&
                        magic[11] == 0x00) ||
                (magic[0] == (byte) 0xff &&
                        magic[1] == (byte) 0xd8 &&
                        magic[2] == (byte) 0xff &&
                        magic[3] == (byte) 0xe0)) {
            return FORMAT;
        }
        return Format.UNKNOWN;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Overridden because the parent will include the thumbnail and other
     * embedded images in the count, which we don't want.
     *
     * @return {@code 1}, always.
     */
    @Override
    public int getNumImages() throws IOException {
        return 1;
    }

    @Override
    public int getNumThumbnails(int imageIndex) throws IOException {
        getSize(imageIndex);
        metadataReader.setSource(inputStream);
        int compression = metadataReader.getThumbnailCompression();
        return SUPPORTED_THUMBNAIL_COMPRESSIONS.contains(compression) ? 1 : 0;
    }

    @Override
    protected String[] getPreferredIIOImplementations() {
        return new String[] { "com.sun.imageio.plugins.jpeg.JPEGImageReader" };
    }

    @Override
    public Set<Format> getSupportedFormats() {
        return Set.of(FORMAT);
    }

    @Override
    public Size getThumbnailSize(int imageIndex,
                                 int thumbIndex) throws IOException {
        BufferedImage image = readThumbnail(imageIndex, thumbIndex);
        if (image != null) {
            return new Size(image.getWidth(), image.getHeight());
        }
        return null;
    }

    /**
     * Expedient but not necessarily efficient method that reads a whole image
     * (excluding subimages) in one shot.
     */
    public BufferedImage decode(int imageIndex) throws IOException {
        BufferedImage image = null;
        try {
            image = super.decode(imageIndex);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Numbers of source Raster bands " +
                    "and source color space components do not match")) {
                ImageReadParam readParam = iioReader.getDefaultReadParam();
                image = readGrayscaleWithIncompatibleICCProfile(readParam);
            } else {
                throw e;
            }
        } catch (IIOException e) {
            if ("Unsupported Image Type".equals(e.getMessage())) {
                ImageReadParam readParam = iioReader.getDefaultReadParam();
                image = readCMYK(readParam);
            } else {
                handle(e);
            }
        }
        return image;
    }

    /**
     * Override to handle images with incompatible ICC profiles, and also
     * CMYK images, neither of which the Sun JPEGImageReader can do.
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
        BufferedImage image = readRegion(region);
        if (image == null) {
            throw new SourceFormatException(iioReader.getFormatName());
        }
        hints.add(DecoderHint.IGNORED_SCALE);
        return image;
    }

    @Override
    public Metadata readMetadata(int imageIndex) throws IOException {
        if (metadata != null) {
            return metadata;
        }

        getSize(imageIndex); // perform setup
        metadataReader.setSource(inputStream);

        metadata = new JPEGMetadata();
        { // EXIF
            byte[] exifBytes = metadataReader.getEXIF();
            if (exifBytes != null) {
                metadata.setThumbnailDirOffset(metadataReader.getEXIFOffset());
                DirectoryReader exifReader = new DirectoryReader();
                exifReader.addTagSet(new EXIFBaselineTIFFTagSet());
                exifReader.addTagSet(new EXIFTagSet());
                exifReader.addTagSet(new EXIFGPSTagSet());
                exifReader.addTagSet(new EXIFInteroperabilityTagSet());
                try (ImageInputStream is = new ByteArrayImageInputStream(exifBytes)) {
                    exifReader.setSource(is);
                    DirectoryIterator it = exifReader.iterator();
                    if (it.hasNext()) {
                        metadata.setEXIF(it.next());
                    }
                    if (it.hasNext()) {
                        metadata.setThumbnailDir(it.next());
                    }
                }
            }
        }
        { // IPTC
            byte[] iptcBytes = metadataReader.getIPTC();
            if (iptcBytes != null) {
                IIMReader iptcReader = new IIMReader();
                iptcReader.setSource(iptcBytes);
                List<DataSet> dataSets = iptcReader.read();
                metadata.setIPTC(dataSets);
            }
        }
        { // XMP
            String xmpStr = metadataReader.getXMP();
            metadata.setXMP(xmpStr);
        }
        return metadata;
    }

    @Override
    public BufferedImage readThumbnail(int imageIndex,
                                       int thumbIndex) throws IOException {
        if (thumbIndex != 0) {
            throw new IndexOutOfBoundsException("Invalid thumbnail index");
        }
        getSize(imageIndex);
        if (cachedThumb == null) {
            metadataReader.setSource(inputStream);
            byte[] data = metadataReader.getThumbnailData();
            if (data != null) {
                try (ImageInputStream thumbIS = new ByteArrayImageInputStream(data)) {
                    cachedThumb = ImageIO.read(thumbIS);
                }
            }
        }
        return cachedThumb;
    }

    private BufferedImage readRegion(final Region region) throws IOException {
        final Size imageSize = getSize(0);

        getLogger().debug("Acquiring region {},{}/{}x{} from {}x{} image",
                region.intX(), region.intY(),
                region.intWidth(), region.intHeight(),
                imageSize.intWidth(), imageSize.intHeight());

        final ImageReadParam param = iioReader.getDefaultReadParam();
        if (!region.isFull()) {
            param.setSourceRegion(region.toAWTRectangle());
        }

        BufferedImage image = null;
        try {
            image = iioReader.read(0, param);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Numbers of source Raster bands " +
                    "and source color space components do not match")) {
                image = readGrayscaleWithIncompatibleICCProfile(param);
            } else {
                throw e;
            }
        } catch (IIOException e) {
            if ("Unsupported Image Type".equals(e.getMessage())) {
                image = readCMYK(param);
            } else {
                handle(e);
            }
        }
        return image;
    }

    /**
     * Used for images whose embedded ICC profile is incompatible with the
     * source image data. (The Sun JPEGImageReader is not very lenient.)
     *
     * @see <a href="https://github.com/cantaloupe-project/cantaloupe/issues/41">
     *     GitHub issue</a>
     */
    private BufferedImage readGrayscaleWithIncompatibleICCProfile(ImageReadParam readParam)
            throws IOException {
        // To deal with this, we will try reading again, ignoring the embedded
        // ICC profile. We need to reset the reader, and then read into a
        // grayscale BufferedImage.
        long pos = inputStream.getStreamPosition();
        inputStream.seek(0);

        final Iterator<ImageTypeSpecifier> imageTypes = iioReader.getImageTypes(0);
        while (imageTypes.hasNext()) {
            final ImageTypeSpecifier imageTypeSpecifier = imageTypes.next();
            final int bufferedImageType = imageTypeSpecifier.getBufferedImageType();
            if (bufferedImageType == BufferedImage.TYPE_BYTE_GRAY) {
                readParam.setDestinationType(imageTypeSpecifier);
                break;
            }
        }
        inputStream.seek(pos);
        return iioReader.read(0, readParam);
    }

    /**
     * Reads an image with CMYK color, which the Sun JPEGImageReader doesn't
     * support, as of Java 9.
     */
    private BufferedImage readCMYK(ImageReadParam readParam) throws IOException {
        /*
        The steps involved here are:

        1. Extract the profile and color info from the image NOT using ImageIO
        2. Read the image into a Raster using ImageIO
        3. If the image is YCCK, convert it to CMYK
        4. If the image has an Adobe APP14 marker segment, invert its colors
        5. Convert the CMYK Raster to an RGB BufferedImage
        */
        final ICC_Profile profile = metadataReader.getICCProfile();
        final JPEGMetadataReader.AdobeColorTransform transform =
                metadataReader.getColorTransform();
        final WritableRaster raster =
                (WritableRaster) iioReader.readRaster(0, readParam);
        if (JPEGMetadataReader.AdobeColorTransform.YCCK.equals(transform)) {
            Java2DUtils.convertYCCKToCMYK(raster);
        }
        if (metadataReader.hasAdobeSegment()) {
            Java2DUtils.invertColor(raster);
        }
        return Java2DUtils.convertCMYKToRGB(raster, profile);
    }

}
