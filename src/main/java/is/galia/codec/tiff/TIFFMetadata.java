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

import is.galia.codec.iptc.DataSet;
import is.galia.codec.iptc.IIMReader;
import is.galia.codec.xmp.XMPUtils;
import is.galia.image.MutableMetadata;
import is.galia.image.NativeMetadata;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * N.B.: We don't extend {@link is.galia.codec.ImageIOMetadata} because the
 * higher-level metadata classes of the JDK TIFF plugin are interminably buggy
 * as of JDK 22.
 */
class TIFFMetadata extends MutableMetadata {

    static final Tag IPTC_POINTER_TAG = new Tag(33723, "IPTC", true);
    static final Tag XMP_POINTER_TAG  = new Tag(700, "XMP", true);

    private boolean checkedForIPTC, checkedForXMP;

    TIFFMetadata(Directory exifIFD) {
        this.exifIFD = exifIFD;
    }

    @Override
    public Optional<Directory> getEXIF() {
        return Optional.of(exifIFD);
    }

    @Override
    public List<DataSet> getIPTC() {
        if (!checkedForIPTC) {
            checkedForIPTC = true;
            if (exifIFD != null) {
                Field iptcField = exifIFD.getField(IPTC_POINTER_TAG);
                if (iptcField != null) {
                    byte[] bytes = (byte[]) iptcField.getFirstValue();
                    if (bytes != null) {
                        IIMReader reader = new IIMReader();
                        reader.setSource(bytes);
                        iptcDataSets = reader.read();
                    }
                }
            }
        }
        return iptcDataSets;
    }

    /**
     * This override returns empty. Although TIFF has lots of native metadata,
     * most of the baseline tags have been integrated into the {@link
     * #getEXIF() EXIF standard}; and since tags in non-EXIF sub-IFDs are not
     * supported, that leaves not much left for this method to do.
     */
    @Override
    public Optional<NativeMetadata> getNativeMetadata() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getXMP() {
        if (!checkedForXMP) {
            checkedForXMP = true;
            if (exifIFD != null) {
                Field xmpField = exifIFD.getField(XMP_POINTER_TAG);
                if (xmpField != null) {
                    byte[] bytes = (byte[]) xmpField.getFirstValue();
                    if (bytes != null) {
                        xmp = new String(bytes, StandardCharsets.UTF_8);
                        xmp = XMPUtils.trimXMP(xmp);
                    }
                }
            }
        }
        return Optional.ofNullable(xmp);
    }

}
