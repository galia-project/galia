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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sequence of images in temporal order, to support video or slide-show image
 * content, such as animated GIFs.
 */
public class BufferedImageSequence implements Iterable<BufferedImage> {

    private final List<BufferedImage> images = new ArrayList<>();

    public void add(BufferedImage image) {
        images.add(image);
    }

    public BufferedImage get(int index) {
        return images.get(index);
    }

    @Override
    public Iterator<BufferedImage> iterator() {
        return images.iterator();
    }

    public int length() {
        return images.size();
    }

    public void set(int index, BufferedImage image) {
        images.set(index, image);
    }

}
