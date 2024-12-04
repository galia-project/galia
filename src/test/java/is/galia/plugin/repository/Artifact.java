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

package is.galia.plugin.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class Artifact extends Entity {

    private final String filename, md5;
    private long productVersionID;

    public Artifact(String filename, String md5) {
        this.filename = filename;
        this.md5      = md5;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Artifact other) {
            return Objects.equals(filename, other.filename) &&
                    Objects.equals(md5, other.md5) &&
                    Objects.equals(productVersionID, other.productVersionID);
        }
        return false;
    }

    @Override
    String getBaseURL() {
        return getProductVersion().getBaseURL();
    }

    public String getFilename() {
        return filename;
    }

    public String getMD5() {
        return md5;
    }

    @JsonIgnore
    public ProductVersion getProductVersion() {
        return (ProductVersion) getDatabase().find(productVersionID);
    }

    public long getSize() {
        try {
            return (filename != null) ?
                    Files.size(MockArtifactRepository.FIXTURE_DIR.resolve(filename)) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public String getURLPath() {
        return getProductVersion().getURLPath() + "/artifact";
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, md5, productVersionID);
    }

    public void setProductVersion(ProductVersion productVersion) {
        this.productVersionID = productVersion.getID();
        getDatabase().add(productVersion);
    }

}
