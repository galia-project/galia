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

import is.galia.async.ThreadPool;
import is.galia.codec.BufferedImageSequence;
import is.galia.codec.Decoder;
import is.galia.codec.DecoderHint;
import is.galia.codec.Encoder;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.image.Format;
import is.galia.image.Metadata;
import is.galia.image.Orientation;
import is.galia.image.ReductionFactor;
import is.galia.image.Region;
import is.galia.image.ScaleConstraint;
import is.galia.image.Size;
import is.galia.operation.ColorTransform;
import is.galia.operation.Crop;
import is.galia.operation.CropByPercent;
import is.galia.operation.Operation;
import is.galia.operation.OperationList;
import is.galia.operation.Rotate;
import is.galia.operation.Scale;
import is.galia.operation.Sharpen;
import is.galia.operation.Transpose;
import is.galia.operation.overlay.Overlay;
import is.galia.operation.redaction.Redaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation using Java 2D.
 */
class Java2DProcessor implements Processor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Java2DProcessor.class);

    private Decoder decoder;
    private Encoder encoder;

    //region Processor methods

    @Override
    public void process(final OperationList ops,
                        final OutputStream outputStream) throws IOException {
        checkDecoder();

        final ReductionFactor rf     = new ReductionFactor();
        final Set<DecoderHint> hints = EnumSet.noneOf(DecoderHint.class);

        // If the source and output formats are both GIF, the source may
        // contain multiple frames, in which case the post-processing steps
        // will have to be different. (No problem if it only contains one
        // frame, though.)
        Format gif = Format.get("gif");
        if (gif.equals(decoder.detectFormat()) &&
                gif.equals(ops.getOutputFormat())) {
            BufferedImageSequence seq = decoder.decodeSequence();
            postProcess(seq, decoder.getSize(0), ops, decoder.readMetadata(0));
            encoder.encode(seq, outputStream);
        } else {
            int pageIndex      = ops.getPageIndex();
            ScaleConstraint sc = ops.getScaleConstraint();
            Size size          = decoder.getSize(pageIndex);
            Crop crop          = (Crop) ops.getFirst(Crop.class);
            if (crop == null) {
                crop = new CropByPercent();
            }
            Region roi         = crop.getRegion(size, rf, sc);
            Scale scale        = (Scale) ops.getFirst(Scale.class);
            double scScale     = sc.rational().doubleValue();
            double[] scales;
            if (scale != null) {
                scales = scale.getResultingScales(roi.size(), sc);
            } else {
                scales = new double[] { scScale, scScale };
            }
            double[] diffScales = { 1, 1 };

            // Find the best thumbnail to read (if we are using thumbnails),
            // i.e. the smallest one that is larger than the requested
            // scale.
            int thumbIndex = -1;
            if (Configuration.forApplication().getBoolean(Key.PROCESSOR_USE_EMBEDDED_THUMBNAILS, true)) {
                Size bestSize = new Size(0, 0);
                for (int i = 0, numThumbs = decoder.getNumThumbnails(pageIndex);
                     i < numThumbs; i++) {
                    Size thumbSize = decoder.getThumbnailSize(pageIndex, i);
                    if (size.width() * scales[0] <= thumbSize.width() &&
                            size.height() * scales[1] <= thumbSize.height()) {
                        if (thumbSize.width() < bestSize.width()) {
                            bestSize = thumbSize;
                            thumbIndex = i;
                        }
                    }
                }
            }

            BufferedImage image;
            if (thumbIndex > -1) {
                LOGGER.debug("Reading thumbnail {} of image {}",
                        pageIndex, thumbIndex);
                image = decoder.readThumbnail(pageIndex, thumbIndex);
                hints.add(DecoderHint.IGNORED_REGION);
                hints.add(DecoderHint.ALREADY_ORIENTED);
                hints.add(DecoderHint.NEEDS_DIFFERENTIAL_SCALE);
                rf.factor     = ReductionFactor.forScale(Math.max(scales[0], scales[1])).factor;
                diffScales[0] = (size.width() * scales[0]) / (double) image.getWidth();
                diffScales[1] = (size.height() * scales[1]) / (double) image.getHeight();
            } else {
                LOGGER.debug("Reading image {} [region {}] [scale {}/{}]",
                        pageIndex, roi, scales[0], scales[1]);
                image = decoder.decode(
                        pageIndex, roi, scales, rf, diffScales, hints);
            }
            image = postProcess(image, size, hints, ops, diffScales,
                    decoder.readMetadata(pageIndex), rf);
            encoder.encode(image, outputStream);
        }
    }

    @Override
    public void setArena(Arena arena) {
        // we aren't using this currently
    }

    @Override
    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    //endregion
    //region Private methods

    private void checkDecoder() {
        if (decoder == null) {
            throw new IllegalStateException(
                    "Decoder is null (was setDecoder() called?)");
        }
    }

    /**
     * Can be used for all images but not {@link BufferedImageSequence image
     * sequences}&mdash;for those, use {@link
     * #postProcess(BufferedImageSequence, Size, OperationList, Metadata)}.
     *
     * @param image           Image to process.
     * @param fullSize        Full source image size.
     * @param decoderHints    Hints from the image decoder.
     * @param opList          Operations to apply to the image.
     * @param diffScales      Differential scales.
     * @param metadata        Metadata to embed. May be {@code null}.
     * @param reductionFactor Reduction factor.
     */
    private static BufferedImage postProcess(BufferedImage image,
                                             Size fullSize,
                                             Set<DecoderHint> decoderHints,
                                             OperationList opList,
                                             double[] diffScales,
                                             Metadata metadata,
                                             ReductionFactor reductionFactor) {
        image = Java2DUtils.reduceTo8Bits(image);

        // N.B.: Any Crop or Rotate operations present in the operation list
        // have already been corrected for this orientation, but we also need
        // to account for operation lists that don't include one or both of
        // those.
        Orientation orientation = Orientation.ROTATE_0;
        if (!decoderHints.contains(DecoderHint.ALREADY_ORIENTED)) {
            if (metadata != null) {
                orientation = metadata.getOrientation();
            }
        }

        // Apply the crop operation, if present and necessary, and retain a
        // reference to it for subsequent operations to refer to.
        Crop crop = new CropByPercent();
        for (Operation op : opList) {
            if (op instanceof Crop) {
                crop = (Crop) op;
                if (crop.hasEffect(fullSize, opList) &&
                        decoderHints.contains(DecoderHint.IGNORED_REGION)) {
                    image = Java2DUtils.crop(image, crop, reductionFactor,
                            opList.getScaleConstraint());
                }
            }
        }

        if (!decoderHints.contains(DecoderHint.ALREADY_ORIENTED) &&
                !Orientation.ROTATE_0.equals(orientation)) {
            image = Java2DUtils.rotate(image, orientation);
        }

        // Apply redactions.
        final Set<Redaction> redactions = opList.stream()
                .filter(op -> op instanceof Redaction &&
                        op.hasEffect(fullSize, opList))
                .map(op -> (Redaction) op)
                .collect(Collectors.toSet());
        Java2DUtils.applyRedactions(image, fullSize, crop, diffScales,
                reductionFactor, opList.getScaleConstraint(), redactions);

        // Apply remaining operations.
        for (Operation op : opList) {
            if (!op.hasEffect(fullSize, opList)) {
                continue;
            }
            if (op instanceof Scale scale &&
                    (decoderHints.contains(DecoderHint.IGNORED_SCALE) ||
                            decoderHints.contains(DecoderHint.NEEDS_DIFFERENTIAL_SCALE))) {
                final boolean isLinear = scale.isLinear() &&
                        !scale.isUp(fullSize, opList.getScaleConstraint());
                if (isLinear) {
                    image = Java2DUtils.convertColorToLinearRGB(image);
                }
                image = Java2DUtils.scale(image, scale,
                        opList.getScaleConstraint(), reductionFactor, isLinear);
                if (isLinear) {
                    image = Java2DUtils.convertColorToSRGB(image);
                }
            } else if (op instanceof Transpose) {
                image = Java2DUtils.transpose(image, (Transpose) op);
            } else if (op instanceof Rotate) {
                image = Java2DUtils.rotate(image, (Rotate) op);
            } else if (op instanceof ColorTransform) {
                image = Java2DUtils.transformColor(image, (ColorTransform) op);
            } else if (op instanceof Sharpen) {
                image = Java2DUtils.sharpen(image, (Sharpen) op);
            } else if (op instanceof Overlay) {
                Java2DUtils.applyOverlay(image, (Overlay) op);
            }
        }
        return image;
    }

    /**
     * For processing {@link BufferedImageSequence image sequences}, such as to
     * support animated GIFs.
     *
     * @param sequence Sequence containing one or more images, which will be
     *                 replaced with the post-processed variants.
     * @param fullSize Full source image size.
     * @param opList   Operations to apply to each image in the sequence.
     * @param metadata Metadata to embed.
     * @throws IllegalArgumentException if the sequence is empty.
     */
    private static void postProcess(final BufferedImageSequence sequence,
                                    final Size fullSize,
                                    final OperationList opList,
                                    final Metadata metadata) throws IOException {
        final int numFrames = sequence.length();

        // 1. If the sequence contains no frames, throw an exception.
        // 2. If it contains only one frame, process the frame in the current
        //    thread.
        // 3. If it contains more than one frame, spread the work across one
        //    thread per CPU.
        if (numFrames < 1) {
            throw new IllegalArgumentException("Empty sequence");
        } else if (numFrames == 1) {
            BufferedImage image = sequence.get(0);
            image = postProcess(
                    image, fullSize, EnumSet.noneOf(DecoderHint.class), opList,
                    new double[] { 1, 1 }, metadata, null);
            sequence.set(0, image);
        } else {
            final int numThreads = Math.min(
                    numFrames, Runtime.getRuntime().availableProcessors());
            final int framesPerThread =
                    (int) Math.ceil(numFrames / (float) numThreads);
            final CountDownLatch latch = new CountDownLatch(numFrames);

            LOGGER.debug("Processing {} frames in {} threads ({} frames/thread)",
                    numFrames, numThreads, framesPerThread);

            // Create a list containing numThreads queues. Each map will
            // contain { "frame": int, "image": BufferedImage }.
            final List<Queue<Map<String,Object>>> processingQueues =
                    new ArrayList<>(numThreads);
            for (short thread = 0; thread < numThreads; thread++) {
                final Queue<Map<String,Object>> queue = new LinkedList<>();
                processingQueues.add(queue);

                final int startFrame = thread * framesPerThread;
                final int endFrame = Math.min(startFrame + framesPerThread,
                        numFrames);
                for (int frameNum = startFrame; frameNum < endFrame; frameNum++) {
                    Map<String,Object> map = new HashMap<>();
                    map.put("frame", frameNum);
                    map.put("image", sequence.get(frameNum));
                    queue.add(map);
                }
            }

            // Process each queue in a separate thread.
            int i = 0;
            for (Queue<Map<String,Object>> queue : processingQueues) {
                final int queueNum = i;
                ThreadPool.getInstance().submit(() -> {
                    Map<String,Object> dict;
                    while ((dict = queue.poll()) != null) {
                        int frameNum = (int) dict.get("frame");
                        LOGGER.trace("Thread {}: processing frame {} (latch count: {})",
                                queueNum, frameNum, latch.getCount());
                        BufferedImage image = (BufferedImage) dict.get("image");
                        image = postProcess(
                                image,
                                fullSize,
                                EnumSet.noneOf(DecoderHint.class),
                                opList,
                                new double[] { 1, 1 },
                                metadata,
                                new ReductionFactor());
                        sequence.set(frameNum, image);
                        latch.countDown();
                    }
                    return null;
                });
                i++;
            }

            // Wait for all threads to finish.
            try {
                latch.await(1, TimeUnit.MINUTES); // hopefully not this long...
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }

}
