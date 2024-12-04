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

package is.galia.codec.png;

import is.galia.codec.ImageIOMetadata;
import is.galia.codec.xmp.XMPUtils;
import is.galia.image.NativeMetadata;
import org.w3c.dom.NodeList;

import javax.imageio.metadata.IIOMetadataNode;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class PNGMetadata extends ImageIOMetadata {

    private boolean checkedForNativeMetadata, checkedForXMP;

    /**
     * Creates an instance with conventional accessors.
     */
    PNGMetadata() {
        this(null, null);
    }

    /**
     * Creates an instance whose getters ignore the setters and read from the
     * supplied arguments instead.
     */
    PNGMetadata(javax.imageio.metadata.IIOMetadata metadata,
                String formatName) {
        super(metadata, formatName);
    }

    @Override
    public Optional<NativeMetadata> getNativeMetadata() {
        if (nativeMetadata == null && !checkedForNativeMetadata &&
                iioMetadata != null) {
            checkedForNativeMetadata = true;
            final NodeList itxtNodes = getAsTree().getElementsByTagName("tEXt");
            if (itxtNodes.getLength() > 0) {
                nativeMetadata = new PNGNativeMetadata();
                for (int i = 0; i < itxtNodes.getLength(); i++) {
                    final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
                    final NodeList entries = itxtNode.getElementsByTagName("tEXtEntry");
                    for (int j = 0; j < entries.getLength(); j++) {
                        final IIOMetadataNode node = ((IIOMetadataNode) entries.item(j));
                        final String keyword = node.getAttribute("keyword");
                        ((PNGNativeMetadata) nativeMetadata).put(keyword, node.getAttribute("value"));
                    }
                }
            }
        }
        return Optional.ofNullable(nativeMetadata);
    }

    @Override
    public Optional<String> getXMP() {
        if (xmp == null && !checkedForXMP && iioMetadata != null) {
            checkedForXMP = true;
            final NodeList itxtNodes = getAsTree().getElementsByTagName("iTXt");
            for (int i = 0; i < itxtNodes.getLength(); i++) {
                final IIOMetadataNode itxtNode = (IIOMetadataNode) itxtNodes.item(i);
                final NodeList entries = itxtNode.getElementsByTagName("iTXtEntry");
                for (int j = 0; j < entries.getLength(); j++) {
                    final String keyword = ((IIOMetadataNode) entries.item(j)).
                            getAttribute("keyword");
                    if ("XML:com.adobe.xmp".equals(keyword)) {
                        byte[] xmpBytes = ((IIOMetadataNode) entries.item(j))
                                .getAttribute("text")
                                .getBytes(StandardCharsets.UTF_8);
                        xmp = new String(xmpBytes, StandardCharsets.UTF_8);
                        xmp = XMPUtils.trimXMP(xmp);
                    }
                }
            }
        }
        return Optional.ofNullable(xmp);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String,Object> map = super.toMap();
        Optional<NativeMetadata> optMetadata = getNativeMetadata();
        if (optMetadata.isPresent()) {
            NativeMetadata metadata = optMetadata.get();
            map = new HashMap<>(super.toMap());
            map.put("native", metadata);
            return Collections.unmodifiableMap(map);
        }
        return map;
    }

}
