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

import is.galia.codec.tiff.Directory;
import is.galia.image.MutableMetadata;

class JPEGMetadata extends MutableMetadata {

    private Directory thumbDir;
    private int thumbDirOffset;

    Directory getThumbnailDir() {
        return thumbDir;
    }

    int getThumbnailDirOffset() {
        return thumbDirOffset;
    }

    void setThumbnailDir(Directory dir) {
        this.thumbDir = dir;
    }

    void setThumbnailDirOffset(int offset) {
        this.thumbDirOffset = offset;
    }

}
