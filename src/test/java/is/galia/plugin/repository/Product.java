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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonSerialize(using = Product.ProductSerializer.class)
public class Product extends Entity {

    public static class ProductSerializer extends JsonSerializer<Product> {
        @Override
        public void serialize(Product product,
                              JsonGenerator generator,
                              SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField("name", product.getName());
            generator.writeStringField("uri", product.getURL());
            generator.writeNullField("website");
            generator.writeNullField("scm_url");
            generator.writeStringField("plugin_of", product.getURL());
            // versions
            generator.writeFieldName("versions");
            generator.writeStartArray();
            for (ProductVersion version : product.getVersions().reversed()) {
                generator.writeObject(version);
            }
            generator.writeEndArray();
            // plugins
            generator.writeFieldName("plugins");
            generator.writeStartArray();
            for (Product plugin : product.getPlugins()) {
                if (plugin.isPublic()) {
                    generator.writeObject(plugin);
                }
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }
    }

    private final String name;
    private final boolean isPublic;
    private long pluginOfID;
    private final List<Long> productVersionIDs = new ArrayList<>();
    private final Set<Long> pluginIDs          = new HashSet<>();

    public Product(String name, boolean isPublic) {
        this.name     = name;
        this.isPublic = isPublic;
    }

    public void addPlugin(Product plugin) {
        pluginIDs.add(plugin.getID());
        getDatabase().add(plugin);
        plugin.pluginOfID = getID();
    }

    public void addVersion(ProductVersion version) {
        productVersionIDs.add(version.getID());
        getDatabase().add(version);
        version.setProduct(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Product product) {
            return Objects.equals(name, product.name);
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public Product getPluginOf() {
        return (Product) getDatabase().find(pluginOfID);
    }

    public Set<Product> getPlugins() {
        return pluginIDs.stream()
                .map(id -> (Product) getDatabase().find(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public String getURLPath() {
        return "/products/" + getName();
    }

    public List<ProductVersion> getVersions() {
        return productVersionIDs.stream()
                .map(id -> (ProductVersion) getDatabase().find(id))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public boolean isPublic() {
        return isPublic;
    }

}
