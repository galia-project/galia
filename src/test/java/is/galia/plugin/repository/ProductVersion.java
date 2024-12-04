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
import com.fasterxml.jackson.annotation.JsonProperty;
import is.galia.util.SoftwareVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProductVersion extends Entity
        implements Comparable<ProductVersion> {

    private final String name, specVersion;
    private final List<Artifact> artifacts = new ArrayList<>();
    private long productID;

    public ProductVersion(String name, String specVersion) {
        this.name        = name;
        this.specVersion = specVersion;
    }

    public void addArtifact(Artifact artifact) {
        getDatabase().add(artifact);
        artifact.setProductVersion(this);
        artifacts.add(artifact);
    }

    @Override
    public int compareTo(ProductVersion other) {
        SoftwareVersion thisVersion = SoftwareVersion.parse(name);
        SoftwareVersion thatVersion = SoftwareVersion.parse(other.name);
        return thisVersion.compareTo(thatVersion);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ProductVersion other) {
            return Objects.equals(name, other.name) &&
                    Objects.equals(specVersion, other.specVersion) &&
                    Objects.equals(artifacts, other.artifacts) &&
                    Objects.equals(productID, other.productID);
        }
        return false;
    }

    public List<Artifact> getArtifacts() {
        return Collections.unmodifiableList(artifacts);
    }

    @Override
    String getBaseURL() {
        return getDatabase().find(productID).getBaseURL();
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public Product getProduct() {
        return (Product) getDatabase().find(productID);
    }

    @JsonProperty("spec_version")
    public String getSpecVersion() {
        return specVersion;
    }

    @JsonIgnore
    @Override
    public String getURL() {
        return super.getURL();
    }

    @Override
    public String getURLPath() {
        return getProduct().getURLPath() + "/versions/" + getName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, specVersion, artifacts, productID);
    }

    public void setProduct(Product product) {
        this.productID = product.getID();
        getDatabase().add(product);
    }

}
