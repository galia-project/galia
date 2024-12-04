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

package is.galia.codec;

import is.galia.image.Size;
import is.galia.image.Metadata;
import is.galia.image.Region;
import is.galia.image.Format;
import is.galia.image.ReductionFactor;
import is.galia.processor.Java2DUtils;
import is.galia.util.IOUtils;
import org.slf4j.Logger;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Abstract class that can be extended by a {@link Decoder} that wraps an
 * {@link ImageReader}. Supports resolution levels and tiling.
 */
public abstract class AbstractImageIODecoder extends AbstractDecoder {

    private static final double DELTA = 0.00000001;

    /**
     * Assigned by {@link #createReader()}.
     */
    protected ImageReader iioReader;

    private boolean isUsingOurOwnInputStream;

    protected static void handle(IIOException e) throws IOException {
        // Image I/O ImageReaders don't distinguish between errors resulting
        // from an incompatible format and other kinds of errors, so we have to
        // check message strings, which are plugin-specific.
        if (e.getMessage().startsWith("Unexpected block type") ||                 // GIF
                e.getMessage().startsWith("I/O error reading header") ||          // ???
                e.getMessage().startsWith("I/O error reading image metadata!") || // TIFF
                e.getMessage().startsWith("I/O error reading PNG header") ||      // PNG
                e.getMessage().startsWith("Not a JPEG file") ||                   // JPEG
                (e.getCause() != null &&
                        e.getCause().getMessage() != null &&
                        (e.getCause().getMessage().startsWith("Invalid magic value") || // ???
                                e.getCause().getMessage().equals("I/O error reading PNG header!")))) { // PNG
            throw new SourceFormatException(e.getMessage());
        }
        throw e;
    }

    private List<ImageReader> availableIIOReaders() {
        final List<ImageReader> iioReaders = new ArrayList<>();
        getSupportedFormats().forEach(format -> {
            format.mediaTypes().forEach(mediaType -> {
                Iterator<ImageReader> it =
                        ImageIO.getImageReadersByMIMEType(mediaType.toString());
                while (it.hasNext()) {
                    iioReaders.add(it.next());
                }
            });
            format.extensions().forEach(extension -> {
                Iterator<ImageReader> it =
                        ImageIO.getImageReadersBySuffix(extension);
                while (it.hasNext()) {
                    iioReaders.add(it.next());
                }
            });
        });
        return iioReaders;
    }

    abstract public boolean canSeek();

    public void close() {
        if (isUsingOurOwnInputStream) {
            IOUtils.closeQuietly(inputStream);
        }
        dispose();
    }

    public void initializeInputStream() throws IOException {
        if (inputStream != null) {
            // Already set, do nothing
        } else if (imageFile != null) {
            // Will be null if the file does not exist
            inputStream = ImageIO.createImageInputStream(imageFile.toFile());
            if (inputStream == null) {
                throw new NoSuchFileException(imageFile.toString());
            }
            isUsingOurOwnInputStream = true;
        } else {
            throw new IOException("No source set.");
        }
    }

    private void createReader() throws IOException {
        if (iioReader != null) {
            return;
        }
        initializeInputStream();

        iioReader = negotiateIIOReader();
        if (iioReader != null) {
            getLogger().debug("Using {}", iioReader.getClass().getName());
            iioReader.setInput(inputStream, false, false);
        } else {
            throw new SourceFormatException("Unable to determine the format " +
                    "of the source image.");
        }
    }

    /**
     * Should be called when the instance is no longer needed.
     */
    public void dispose() {
        if (iioReader != null) {
            iioReader.dispose();
            iioReader = null;
        }
    }

    abstract protected Logger getLogger();

    /**
     * @return Number of images contained inside the source image.
     */
    public int getNumImages() throws IOException {
        // Throw any contract-required exceptions.
        getSize(0);
        // The boolean argument tells whether to scan for images, which seems
        // to be necessary for some readers, but is slower.
        int numImages = iioReader.getNumImages(false);
        if (numImages == -1) {
            numImages = iioReader.getNumImages(true);
        }
        return numImages;
    }

    /**
     * @return {@code 1}.
     */
    public int getNumResolutions() throws IOException {
        getSize(0); // Throw any contract-required exceptions.
        return 1;
    }

    public int getNumThumbnails(int imageIndex) throws IOException {
        getSize(imageIndex); // Throw any contract-required exceptions.
        return iioReader.getNumThumbnails(imageIndex);
    }

    /**
     * N.B.: This method returns a list of strings rather than {@link Class
     * classes} because some readers reside under the {@code com.sun} package,
     * which is encapsulated.
     *
     * @return Preferred reader implementation classes, in order of highest to
     *         lowest priority, or an empty array if there is no preference.
     */
    abstract protected String[] getPreferredIIOImplementations();

    /**
     * @return Pixel dimensions of the image at the given index.
     */
    public Size getSize(int imageIndex) throws IOException {
        createReader();
        try {
            final int width  = iioReader.getWidth(imageIndex);
            final int height = iioReader.getHeight(imageIndex);
            return new Size(width, height);
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    abstract public Set<Format> getSupportedFormats();

    /**
     * @return Size of the thumbnail at the given coordinates.
     */
    public Size getThumbnailSize(int imageIndex,
                                 int thumbIndex) throws IOException {
        getSize(imageIndex); // Throw any contract-required exceptions.
        try {
            int width  = iioReader.getThumbnailWidth(imageIndex, thumbIndex);
            int height = iioReader.getThumbnailHeight(imageIndex, thumbIndex);
            return new Size(width, height);
        } catch (UnsupportedOperationException e) {
            return null;
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    /**
     * @return Tile size of the image at the given index, or the full image
     *         dimensions if the image is not tiled (or is mono-tiled).
     */
    public Size getTileSize(int imageIndex) throws IOException {
        createReader();
        try {
            final int width = iioReader.getTileWidth(imageIndex);
            int height;
            // If the tile width == the full image width, the image is almost
            // certainly not tiled, or at least not in a way that is useful,
            // and getTileHeight() may return 1 to indicate a strip height, or
            // some other wonky value. In that case, set the tile height to the
            // full image height.
            if (width == iioReader.getWidth(imageIndex)) {
                height = iioReader.getHeight(imageIndex);
            } else {
                height = iioReader.getTileHeight(imageIndex);
            }
            return new Size(width, height);
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    /**
     * Chooses the most appropriate ImageIO reader to use based on the return
     * value of {@link #getPreferredIIOImplementations()}.
     */
    private ImageReader negotiateIIOReader() {
        ImageReader negotiatedReader = null;
        final List<ImageReader> iioReaders =
                availableIIOReaders();
        if (!iioReaders.isEmpty()) {
            final String[] preferredImpls = getPreferredIIOImplementations();

            getLogger().trace("ImageIO plugin preferences: {}",
                    (preferredImpls.length > 0) ?
                            String.join(", ", preferredImpls) : "none");
            Found:
            for (String implClass : preferredImpls) {
                for (ImageReader candidateReader : iioReaders) {
                    if (implClass.equals(candidateReader.getClass().getName())) {
                        negotiatedReader = candidateReader;
                        break Found;
                    }
                }
            }
            if (negotiatedReader == null) {
                negotiatedReader = iioReaders.getFirst();
            }
        }
        return negotiatedReader;
    }

    /**
     * Expedient but not necessarily efficient method that reads a whole image
     * (excluding subimages) in one shot.
     */
    public BufferedImage decode(int imageIndex) throws IOException {
        createReader();
        try {
            BufferedImage image = iioReader.read(imageIndex);
            // Convert CMYK to RGB if necessary
            if (image.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_CMYK) {
                image = Java2DUtils.convertCMYKToRGB(image);
            }
            return image;
        } catch (IIOException e) {
            handle(e);
            throw e;
        }
    }

    /**
     * <p>Attempts to read an image as efficiently as possible, exploiting its
     * tile layout, if possible.</p>
     *
     * <p>This implementation is optimized for mono-resolution images.</p>
     *
     * <p>After reading, clients should check the decoder hints to see whether
     * the returned image will require cropping.</p>
     *
     * @param imageIndex      Image index.
     * @param region          May be {@code null}.
     * @param scales          May be {@code null}.
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of the
     *                        returned image.
     * @param diffScales      Two-element array that will be populated with the
     *                        X and Y axis differential scales used during
     *                        reading (if the decoder supports this).
     * @param decoderHints    Will be populated by information returned from
     *                        the reader.
     * @return                Image best matching the given arguments.
     */
    public BufferedImage decode(final int imageIndex,
                                Region region,
                                final double[] scales,
                                final ReductionFactor reductionFactor,
                                final double[] diffScales,
                                final Set<DecoderHint> decoderHints) throws IOException {
        createReader();
        region = region.oriented(getSize(imageIndex),
                readMetadata(imageIndex).getOrientation());
        BufferedImage image;
        try {
            if (region != null) {
                image = tileAwareRead(imageIndex, region);
            } else {
                image = iioReader.read(imageIndex);
            }
            if (image == null) {
                throw new SourceFormatException(iioReader.getFormatName());
            }
            decoderHints.add(DecoderHint.IGNORED_SCALE);
            return image;
        } catch (IIOException e) {
            handle(e);
            return null;
        }
    }

    abstract public Metadata readMetadata(int imageIndex) throws IOException;

    /**
     * Reads a particular image from a multi-image file.
     *
     * @param imageIndex Image index.
     * @param region     Requested region of interest.
     * @return           Smallest image fitting the requested operations.
     * @see              #readSmallestUsableSubimage
     */
    protected BufferedImage readMonoResolution(
            final int imageIndex,
            final Region region) throws IOException {
        createReader();
        return tileAwareRead(imageIndex, region);
    }

    /**
     * Reads the smallest image that can fulfill the given ROI and scale from a
     * multi-image file.
     *
     * @param region          Requested region of interest.
     * @param scales          Requested X and Y axis scales.
     * @param reductionFactor Will be set to the reduction factor of the
     *                        returned image.
     * @return                Smallest image fitting the requested operations.
     * @see                   #readMonoResolution
     */
    protected BufferedImage readSmallestUsableSubimage(
            final Region region,
            final double[] scales,
            final ReductionFactor reductionFactor) throws IOException {
        createReader();
        final Size fullSize = new Size(
                iioReader.getWidth(0), iioReader.getHeight(0));
        BufferedImage bestImage = null;

        if (Math.abs(scales[0] - 1) < DELTA) {
            bestImage = tileAwareRead(0, region);
            getLogger().debug("readSmallestUsableSubimage(): using a {}x{} " +
                            "source image (0x reduction factor)",
                    bestImage.getWidth(), bestImage.getHeight());
        } else {
            // Pyramidal TIFFs will have > 1 image, each with half the
            // dimensions of the previous one. The boolean parameter tells
            // getNumImages() whether to scan for images, which seems to be
            // necessary for at least some files, but is slower. If it is
            // false, and getNumImages() can't find anything, it will return -1.
            int numImages = iioReader.getNumImages(false);
            if (numImages > 1) {
                getLogger().debug("Detected {} subimage(s)", numImages);
            } else if (numImages == -1) {
                numImages = iioReader.getNumImages(true);
                if (numImages > 1) {
                    getLogger().debug("Scan revealed {} subimage(s)", numImages);
                }
            }
            // At this point, we know how many images are available.
            if (numImages == 1) {
                bestImage = tileAwareRead(0, region);
                getLogger().debug("readSmallestUsableSubimage(): using a " +
                                "{}x{} source image (0x reduction factor)",
                        bestImage.getWidth(), bestImage.getHeight());
            } else if (numImages > 1) {
                // Loop through the reduced images to find the smallest one
                // that can supply the requested scale.
                for (int i = numImages - 1; i >= 0; i--) {
                    final int subimageWidth   = iioReader.getWidth(i);
                    final int subimageHeight  = iioReader.getHeight(i);
                    final double reducedScale =
                            (double) subimageWidth / fullSize.width();
                    if (fits(fullSize, scales, reducedScale)) {
                        reductionFactor.factor =
                                ReductionFactor.forScale(reducedScale).factor;
                        getLogger().debug("Subimage {}: {}x{} - fits! " +
                                        "({}x reduction factor)",
                                i + 1, subimageWidth, subimageHeight,
                                reductionFactor.factor);
                        final Region reducedRegion = new Region(
                                region.x() * reducedScale,
                                region.y() * reducedScale,
                                region.width() * reducedScale,
                                region.height() * reducedScale);
                        bestImage = tileAwareRead(i, reducedRegion);
                        break;
                    } else {
                        getLogger().trace("Subimage {}: {}x{} - too small",
                                i + 1, subimageWidth, subimageHeight);
                    }
                }
            }
        }
        return bestImage;
    }

    /**
     * <p>Returns an image for the requested source area by reading the tiles
     * (or strips) of the source image and joining them into a single image.</p>
     *
     * <p>This method is intended to be compatible with all source images, no
     * matter the data layout (tiled, striped, etc.).</p>
     *
     * @param imageIndex Index of the image to read from the ImageReader.
     * @param region     Image region to retrieve. The returned image will be
     *                   this size or smaller if it would overlap the right or
     *                   bottom edge of the source image.
     */
    private BufferedImage tileAwareRead(
            final int imageIndex,
            final Region region) throws IOException {
        final Size imageSize = getSize(imageIndex);
        final Size tileSize  = getTileSize(imageIndex);

        if (region.isFull()) {
            getLogger().debug(
                    "Acquiring full region from {}x{} image",
                    imageSize.intWidth(), imageSize.intHeight());
        } else if (tileSize.equals(imageSize)) {
            getLogger().debug(
                    "Acquiring region {},{}/{}x{} from {}x{} mono-tiled/mono-striped image",
                    region.intX(), region.intY(),
                    region.intWidth(), region.intHeight(),
                    imageSize.intWidth(), imageSize.intHeight());
        } else if (tileSize.intWidth() == imageSize.intWidth()) {
            getLogger().debug(
                    "Acquiring region {},{}/{}x{} from {}x{} image ({}x{} strip size)",
                    region.intX(), region.intY(),
                    region.intWidth(), region.intHeight(),
                    imageSize.intWidth(), imageSize.intHeight(),
                    tileSize.intWidth(), tileSize.intHeight());
        } else {
            getLogger().debug(
                    "Acquiring region {},{}/{}x{} from {}x{} image ({}x{} tile size)",
                    region.intX(), region.intY(),
                    region.intWidth(), region.intHeight(),
                    imageSize.intWidth(), imageSize.intHeight(),
                    tileSize.intWidth(), tileSize.intHeight());
        }

        final ImageReadParam param = iioReader.getDefaultReadParam();
        if (!region.isFull()) {
            param.setSourceRegion(region.toAWTRectangle());
        }
        return iioReader.read(imageIndex, param);
    }

    public BufferedImageSequence decodeSequence() throws IOException {
        createReader();
        BufferedImageSequence seq = new BufferedImageSequence();
        for (int i = 0, count = getNumImages(); i < count; i++) {
            seq.add(decode(i));
        }
        return seq;
    }

    public BufferedImage readThumbnail(int imageIndex,
                                       int thumbIndex) throws IOException {
        createReader();
        return iioReader.readThumbnail(imageIndex, thumbIndex);
    }

    /**
     * @param fullSize     Size of the base pyramid image.
     * @param scales       Requested X and Y axis scales.
     * @param reducedScale Reduced scale of a pyramid level.
     * @return             Whether the given source image region can be
     *                     satisfied by the given reduced scale at the
     *                     requested scale.
     */
    private boolean fits(final Size fullSize,
                         final double[] scales,
                         final double reducedScale) {
        final double tolerance      = 1 / Math.max(fullSize.width(), fullSize.height());
        final double maxNeededScale = Math.max(scales[0], scales[1]);
        final double cappedScale    = (maxNeededScale > 1 + DELTA) ? 1 : maxNeededScale;
        return (cappedScale <= reducedScale + tolerance);
    }

}
