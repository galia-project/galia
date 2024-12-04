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

import com.fasterxml.jackson.annotation.JsonIgnore;
import is.galia.image.MutableMetadata;
import is.galia.image.Metadata;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

/**
 * Wraps a {@link javax.imageio.metadata.IIOMetadata}.
 */
public abstract class ImageIOMetadata extends MutableMetadata
        implements Metadata {

    protected IIOMetadata iioMetadata;
    private final String formatName;

    protected ImageIOMetadata(IIOMetadata iioMetadata, String formatName) {
        this.iioMetadata = iioMetadata;
        this.formatName = formatName;
    }

    @JsonIgnore
    public IIOMetadataNode getAsTree() {
        return (IIOMetadataNode) iioMetadata.getAsTree(formatName);
    }

}
