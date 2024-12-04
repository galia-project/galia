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

package is.galia.image;

import is.galia.codec.iptc.DataSet;
import is.galia.codec.tiff.Directory;
import org.apache.jena.rdf.model.Model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation whose accessors all return empty values.
 */
public class EmptyMetadata implements Metadata {

    @Override
    public Optional<Directory> getEXIF() {
        return Optional.empty();
    }

    @Override
    public List<DataSet> getIPTC() {
        return List.of();
    }

    @Override
    public Optional<NativeMetadata> getNativeMetadata() {
        return Optional.empty();
    }

    @Override
    public Orientation getOrientation() {
        return Orientation.ROTATE_0;
    }

    @Override
    public Optional<String> getXMP() {
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getXMPElements() {
        return Map.of();
    }

    @Override
    public Optional<Model> getXMPModel() {
        return Optional.empty();
    }

}
