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

import is.galia.operation.Encode;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractImageIOEncoder {

    protected Arena arena;
    protected ImageWriter iioWriter;
    protected Encode encode;

    /**
     * <p>Embeds metadata from {@link #encode} into the given tree.</p>
     *
     * <p>Writers for formats that don't support metadata may simply do
     * nothing.</p>
     *
     * @param baseTree Tree to embed the metadata into.
     */
    abstract protected void addMetadata(IIOMetadataNode baseTree)
            throws IOException;

    private List<ImageWriter> availableIIOWriters() {
        final List<ImageWriter> iioWriters = new ArrayList<>();
        final Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(
                encode.getFormat().getPreferredMediaType().toString());
        while (it.hasNext()) {
            iioWriters.add(it.next());
        }
        return iioWriters;
    }

    private void createWriter() {
        this.iioWriter = negotiateImageWriter();
    }

    /**
     * Should be called when the instance is no longer needed.
     */
    public void close() {
        if (iioWriter != null) {
            iioWriter.dispose();
            iioWriter = null;
        }
    }

    abstract protected Logger getLogger();

    /**
     * @param writeParam Write parameters on which to base the metadata.
     * @param image      Image to apply the metadata to.
     * @return           Image metadata with added metadata corresponding to
     *                   any writer-specific operations applied.
     */
    protected IIOMetadata getMetadata(final ImageWriteParam writeParam,
                                      final RenderedImage image) throws IOException {
        final IIOMetadata variant = iioWriter.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(image),
                writeParam);
        final String formatName = variant.getNativeMetadataFormatName();
        final IIOMetadataNode baseTree =
                (IIOMetadataNode) variant.getAsTree(formatName);

        addMetadata(baseTree);

        variant.mergeTree(formatName, baseTree);
        return variant;
    }

    /**
     * N.B.: This method returns a list of strings rather than {@link Class
     * classes} because some readers reside under the {@literal com.sun}
     * package, which is encapsulated in Java 9.
     *
     * @return Plugins preferred by the application, in order of most to least
     *         preferred, or an empty array if there is no preference.
     */
    abstract protected String[] getPreferredIIOImplementations();

    private ImageWriter negotiateImageWriter() {
        ImageWriter negotiatedWriter = null;

        final List<ImageWriter> iioWriters = availableIIOWriters();

        if (!iioWriters.isEmpty()) {
            final String[] preferredImplClasses = getPreferredIIOImplementations();

            getLogger().debug("ImageIO plugin preferences: {}",
                    (preferredImplClasses.length > 0) ?
                            String.join(", ", preferredImplClasses) : "none");

            Found:
            for (String implClass : preferredImplClasses) {
                for (javax.imageio.ImageWriter candidateWriter : iioWriters) {
                    if (implClass.equals(candidateWriter.getClass().getName())) {
                        negotiatedWriter = candidateWriter;
                        break Found;
                    }
                }
            }

            if (negotiatedWriter == null) {
                negotiatedWriter = iioWriters.getFirst();
            }
        }
        return negotiatedWriter;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public void setEncode(Encode encode) {
        this.encode = encode;
        createWriter();
    }

}
