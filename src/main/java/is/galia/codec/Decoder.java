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

import is.galia.image.ComponentOrder;
import is.galia.image.Size;
import is.galia.image.EmptyMetadata;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.image.Orientation;
import is.galia.image.Region;
import is.galia.image.ReductionFactor;
import is.galia.plugin.Plugin;

import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

/**
 * <p>Image decoder.</p>
 *
 * <p>Decoders are instantiated per request and need not be thread-safe. But
 * any library they call into <em>does</em> need to support simultaneous usage
 * from multiple threads.</p>
 *
 * <h2>Notes</h2>
 *
 * <h3>Initialization and disposal</h3>
 *
 * <p>Instances should not use constructors to initialize themselves. Instead
 * they may implement {@link Plugin#initializePlugin()}, or they may defer
 * initialization until first use.</p>
 *
 * <p>Likewise, for class-level initialization, implementations should use
 * {@link Plugin#onApplicationStart()} instead of a static initializer.</p>
 *
 * <p>Instances should release all necessary resources using {@link
 * #close}.</p>
 *
 * <h3>Image indexes</h3>
 *
 * <p>Several methods accept an {@code imageIndex} argument, which is meant to
 * enable access to multiple images contained within the same file, as in the
 * case of e.g. a client "page" query. How this works is:</p>
 *
 * <ul>
 *     <li>For basic still images, there is only the main image at index {@code
 *     0}.</li>
 *     <li>For formats like JPEG2000 that support different resolution levels
 *     of the same image, there is still only the main image at index {@code
 *     0}.</li>
 *     <li>For formats like TIFF and PDF that support multiple sibling images
 *     or pages, these are accessible by their index. Some special
 *     considerations for TIFF:
 *         <ul>
 *             <li>TIFF also supports subimages of subimages, which this
 *             interface does not.)</li>
 *             <li>The sibling images in a TIFF may be different resolution
 *             levels, as in a pyramidal TIFF. In this case, they will
 *             <em>not</em> be accessible by their index.</li>
 *         </ul>
 *     </li>
 *     <li>For formats like HEIF that support <em>unordered</em> subimages,
 *     each subimage will have an unpredictable index assigned by the codec
 *     library. (The primary image will generally be at index 0.)</li>
 *     <li>For animated image formats like GIF, each frame has its own
 *     index.</li>
 *     <li>For video formats, the index refers to each second of the
 *     video.</li>
 * </ul>
 *
 * <p>The value passed to an {@code imageIndex} argument must never be greater
 * than the return value of {@code {@link #getNumImages()} - 1}&mdash;otherwise
 * an {@link IndexOutOfBoundsException} is thrown.</p>
 *
 * <h3>Default implementations</h3>
 *
 * <p>Some methods have default implementations. These correspond with features
 * that may be nice to have in some circumstances but are nevertheless
 * nonessential.</p>
 */
public interface Decoder extends AutoCloseable {

    /**
     * <p>Implementations that make use of the Foreign Function &amp; Memory
     * API should do their native work in this instance. They should not
     * close it.</p>
     *
     * <p>The default implementation does nothing.</p>
     *
     * @param arena Arena in which to allocate foreign memory.
     */
    default void setArena(Arena arena) {
    }

    /**
     * Sets the source to a file.
     *
     * @param imageFile File to read from.
     */
    void setSource(Path imageFile);

    /**
     * Sets the source to a stream.
     *
     * @param inputStream Stream to read from, which will not be closed.
     */
    void setSource(ImageInputStream inputStream);

    /**
     * <p>Called after a source has been set.</p>
     *
     * <p>If reading from a {@link #setSource(ImageInputStream)} stream},
     * the stream position is reset to 0 before returning.</p>
     *
     * <p>Unlike many of the other interface methods, implementations should
     * <em>not</em> throw a {@link SourceFormatException}, and instead just
     * return {@link Format#UNKNOWN}.</p>
     *
     * @return Format of the image to decode, if supported by the instance;
     *         otherwise {@link Format#UNKNOWN}. The implementation must create
     *         the instance from scratch and <em>not</em> obtain it from the
     *         {@link is.galia.image.FormatRegistry format registry}, which may
     *         be empty at the time this method is invoked.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws IOException if there is some other error reading the image.
     */
    Format detectFormat() throws IOException;

    /**
     * Note that for many video formats, a duration is not encoded in the
     * beginning of the file and may need to be found by some other (expensive)
     * means. In this case it's better to return {@link Integer#MAX_VALUE} than
     * to find the exact duration.
     *
     * @return Number of images available in the image container. See the
     *         {@link Decoder class documentation} and the note above.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the number of
     *         images.
     */
    int getNumImages() throws IOException;

    /**
     * <p>Returns the number of resolutions available in the image.</p>
     *
     * <ul>
     *     <li>For conventional formats, this is {@code 1}.</li>
     *     <li>For pyramidal TIFF, this is the number of embedded images, equal
     *     to {@link #getNumImages()}.</li>
     *     <li>For JPEG2000, it is {@code (number of decomposition) levels + 1}.
     *     </li>
     * </ul>
     *
     * @return Number of resolutions available in the image.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the number of
     *         resolutions.
     */
    int getNumResolutions() throws IOException;

    /**
     * The default implementation returns {@code 0}.
     *
     * @return Number of thumbnail images available for the image at the given
     *         index. (See {@link Decoder class documentation}.)
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the number of
     *         thumbnails.
     */
    default int getNumThumbnails(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * @param imageIndex See the {@link Decoder class documentation.}.
     * @return Dimensions of the image at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the size.
     */
    Size getSize(int imageIndex) throws IOException;

    /**
     * @return All formats supported by the implementation. The instances must
     *         be created from scratch and <strong>not</strong> obtained from
     *         the {@link is.galia.image.FormatRegistry format registry}, which
     *         may be empty at the time this method is invoked.
     */
    Set<Format> getSupportedFormats();

    /**
     * <p>The default implementation throws an {@link
     * UnsupportedOperationException}.</p>
     *
     * @param imageIndex See the {@link Decoder class documentation.}.
     * @param thumbnailIndex Value between 0 and {@link
     *        #getNumThumbnails(int)} - 1.
     * @return Dimensions of the thumbnail image at the given index.
     * @throws UnsupportedOperationException if the implementation does not
     *         support thumbnails.
     * @throws IndexOutOfBoundsException if either of the given indices are out
     *         of bounds.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is an error reading the size.
     */
    default Size getThumbnailSize(int imageIndex,
                                  int thumbnailIndex) throws IOException {
        throw new UnsupportedOperationException("No thumbnail available");
    }

    /**
     * @param imageIndex See the {@link Decoder class documentation}.
     * @return Size of the tiles in the image at the given index, or the full
     *         image dimensions if the image is not tiled.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the size.
     */
    Size getTileSize(int imageIndex) throws IOException;

    /**
     * <p>Decodes an entire image into memory.</p>
     *
     * <p>This implementation wraps {@link #decode(int, Region, double[],
     * ReductionFactor, double[], Set)}.</p>
     */
    default BufferedImage decode(int imageIndex) throws IOException {
        Size size = getSize(imageIndex);
        return decode(0,
                new Region(0, 0, size.width(), size.height(), true),
                new double[] {1, 1},
                new ReductionFactor(),
                new double[2],
                EnumSet.noneOf(DecoderHint.class));
    }

    /**
     * <p>Decodes the region of the image corresponding to the given arguments.
     * The image has &le; 8-bit RGB(A) color.</p>
     *
     * <p>For image formats that support an embedded TIFF/EXIF orientation,
     * implementations must reorient any related arguments (in particular,
     * {@code region}). However, they need not rotate the resulting image. If
     * they are able to efficiently render an oriented image without rotating
     * in a later step, they should add {@link DecoderHint#ALREADY_ORIENTED} to
     * the {@code decoderHints}.</p>
     *
     * <p>Implementations are encouraged to return an instance whose {@link
     * java.awt.image.Raster} is backed by a {@link
     * java.awt.image.DataBufferByte}. (This is the case for the standard
     * {@link BufferedImage} types {@link BufferedImage#TYPE_4BYTE_ABGR},
     * {@link BufferedImage#TYPE_3BYTE_BGR}, and {@link
     * BufferedImage#TYPE_BYTE_GRAY}. Also, {@link
     * is.galia.processor.Java2DUtils#newImage(int, int, byte[],
     * ComponentOrder)} can be used to create such an instance.) This will
     * enable more efficient copying to/from native memory (if necessary).</p>
     *
     * @param imageIndex      See the {@link Decoder class documentation}.
     *                        Also note that for pyramidal images, this is
     *                        <em>not</em> used to choose a pyramid level.
     *                        Instead, the {@code scales} argument is.
     * @param region          <p>Region to read relative to the full-sized
     *                        <em>oriented</em> source image. {@link
     *                        Region#oriented(Size, Orientation)} can be used
     *                        to translate this to physical source image
     *                        coordinates.</p>
     *                        <p>If the region exceeds the image bounds, it is
     *                        silently clipped to the image bounds.</p>
     *                        <p>If an implementation is not able to utilize
     *                        this argument efficiently, it should add {@link
     *                        DecoderHint#IGNORED_REGION} to the {@code
     *                        decoderHints}.</p>
     * @param scales          <p>Two-element array of X and Y axis scales.
     *                        Never {@code null}. Applied after {@code
     *                        crop}.</p>
     *                        <p>If an implementation is not able to utilize
     *                        this argument efficiently, it should add {@link
     *                        DecoderHint#IGNORED_SCALE} to the {@code
     *                        decoderHints}.</p>
     * @param reductionFactor The {@link ReductionFactor#factor} property will
     *                        be modified to reflect the reduction factor of
     *                        the returned image, i.e. the number of times its
     *                        dimensions have been halved relative to the
     *                        full-sized image when a &le; 0.5 scale has been
     *                        requested.
     * @param diffScales      <p>Two-element array that will be populated with
     *                        the X and Y axis differential scales computed
     *                        during reading.</p>
     *                        <p>Differential scales here are scales relative
     *                        to the reduced image (see {@code
     *                        reductionFactor}). For example, if a client has
     *                        requested a scale of 45%, and the implementation
     *                        is reading an image at a reduction factor of
     *                        {@code 1} (50%), the differential scale is the
     *                        amount that the returned image will need to
     *                        be downscaled further, e.g. {@code
     *                        0.45 / 0.5}.</p>
     *                        <p>Implementations that don't support reading at
     *                        arbitrary scales (probably most of them) should
     *                        simply set these two elements to {@code 1}.</p>
     * @param decoderHints    Hints provided to and/or returned from the
     *                        method.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the image.
     */
    BufferedImage decode(int imageIndex,
                         Region region,
                         double[] scales,
                         ReductionFactor reductionFactor,
                         double[] diffScales,
                         Set<DecoderHint> decoderHints) throws IOException;

    /**
     * <p>Decodes a sequence of images into memory</p>
     *
     * <p>This method is meant to support short animations (e.g. animated GIF).
     * It is not meant to support video formats, which could fill up all
     * available memory and then some. These should simply throw an {@link
     * UnsupportedOperationException}, which is what this default
     * implementation does.</p>
     *
     * @throws UnsupportedOperationException if the implementation does not
     *         support sequences.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the sequence.
     */
    default BufferedImageSequence decodeSequence() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads metadata pertaining to the image at the given index.
     *
     * @return Metadata for the image at the given index. If there is none, an
     *         {@link EmptyMetadata} can be returned.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the metadata.
     */
    Metadata readMetadata(int imageIndex) throws IOException;

    /**
     * <p>Reads a thumbnail image into memory.</p>
     *
     * <p>Implementations are encouraged to return an instance whose {@link
     * java.awt.image.Raster} is backed by a {@link
     * java.awt.image.DataBufferByte}. See the documentation of {@link
     * #decode(int, Region, double[], ReductionFactor, double[], Set)} for more
     * information.</p>
     *
     * <p>The default implementation throws an {@link
     * UnsupportedOperationException}.</p>
     *
     * @param imageIndex See the class documentation.
     * @param thumbnailIndex Value between 0 and {@link
     *        #getNumThumbnails(int)} - 1.
     * @throws UnsupportedOperationException if the implementation does not
     *         support thumbnails.
     * @throws IndexOutOfBoundsException if either of the given indices are out
     *         of bounds.
     * @throws NoSuchFileException if the source file does not exist.
     * @throws SourceFormatException if the image format is not supported.
     * @throws IOException if there is some other error reading the image.
     */
    default BufferedImage readThumbnail(int imageIndex,
                                        int thumbnailIndex) throws IOException {
        throw new UnsupportedOperationException("No thumbnail available");
    }

    /**
     * Releases all resources when an instance is no longer needed, but does
     * <em>not</em> close the stream supplied to {@link
     * #setSource(ImageInputStream)}.
     */
    @Override
    void close();

}
