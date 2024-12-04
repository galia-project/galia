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

package is.galia.codec.gif;

import is.galia.codec.xmp.XMPUtils;
import is.galia.image.MutableMetadata;
import is.galia.image.Metadata;
import is.galia.image.NativeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class GIFMetadata extends MutableMetadata implements Metadata {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GIFMetadata.class);

    private final GIFMetadataReader reader;
    private boolean checkedForXMP;
    private GIFNativeMetadata cachedNativeMetadata;

    GIFMetadata(GIFMetadataReader reader) {
        this.reader = reader;
    }

    @Override
    public Optional<NativeMetadata> getNativeMetadata() {
        if (cachedNativeMetadata == null) {
            cachedNativeMetadata = new GIFNativeMetadata();
            try {
                cachedNativeMetadata.setDelayTime(reader.getDelayTime());
                cachedNativeMetadata.setLoopCount(reader.getLoopCount());
            } catch (IOException e) {
                LOGGER.warn("getNativeMetadata(): {}", e.getMessage());
            }
        }
        return Optional.of(cachedNativeMetadata);
    }

    @Override
    public Optional<String> getXMP() {
        if (!checkedForXMP) {
            checkedForXMP = true;
            try {
                xmp = reader.getXMP();
                if (xmp != null) {
                    xmp = XMPUtils.trimXMP(xmp);
                }
            } catch (IOException e) {
                LOGGER.warn("getXMP(): {}", e.getMessage());
            }
        }
        return Optional.ofNullable(xmp);
    }

    @Override
    public Map<String, Object> toMap() {
        final Map<String,Object> map = new HashMap<>(super.toMap());
        getNativeMetadata().ifPresent(m -> map.put("native", m.toMap()));
        return Collections.unmodifiableMap(map);
    }

}
