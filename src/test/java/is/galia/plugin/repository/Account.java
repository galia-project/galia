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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Account extends Entity {

    private final Set<Long> licensedProductIDs = new HashSet<>();
    private final String customerKey, name;

    public Account(String name, String customerKey) {
        this.name        = name;
        this.customerKey = customerKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Account account) {
            return Objects.equals(name, account.name);
        }
        return false;
    }

    public void addLicensedProduct(Product product) {
        licensedProductIDs.add(product.getID());
        getDatabase().add(product);
    }

    @JsonIgnore
    public String getCustomerKey() {
        return customerKey;
    }

    @JsonProperty("licenses")
    public Set<Product> getLicensedProducts() {
        return licensedProductIDs.stream()
                .map(id -> (Product) getDatabase().find(id))
                .collect(Collectors.toSet());
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    @Override
    public String getURL() {
        return super.getURL();
    }

    @Override
    public String getURLPath() {
        return "/my-account";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

}
