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

package is.galia.source;

import is.galia.image.Format;
import is.galia.image.StatResult;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Iterator;

public class AccessDeniedSource extends AbstractSource implements Source {

    @Override
    public StatResult stat() throws IOException {
        throw new AccessDeniedException("");
    }

    @Override
    public Iterator<Format> getFormatIterator() {
        return null;
    }

    @Override
    public ImageInputStream newInputStream() {
        return null;
    }

}
