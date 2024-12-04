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

/**
 * Seeds {@link MockArtifactRepository} with fixture data.
 */
class Seeder {

    public void seed(Database database) {
        Product plugin;
        ProductVersion version;

        /////////////////////////// accounts ///////////////////////////////

        Account account = new Account("Acme Inc.", MockArtifactRepository.CUSTOMER_KEY);
        database.add(account);

        /////////////////////////// products ///////////////////////////////

        // Galia
        Product galia = new Product("galia", true);
        database.add(galia);
        version = new ProductVersion("1.0", "1.0");
        galia.addVersion(version);
        account.addLicensedProduct(galia);

        ///////////////////////// public plugins ///////////////////////////

        { // public-plugin
            plugin = new Product("public-plugin", true);
            galia.addPlugin(plugin);
            // 1.0
            version = new ProductVersion("1.0", "1.0");
            plugin.addVersion(version);
            version.addArtifact(new Artifact("public-plugin-1.0.zip",
                    "4f998a4dc22e5947350a9171132d318e"));
            // 1.1
            version = new ProductVersion("1.1", "1.0");
            plugin.addVersion(version);
            version.addArtifact(new Artifact("public-plugin-1.1.zip",
                    "a2f4981c0b129cf3f7f292e192b9bf4a"));
            // 1.2-SNAPSHOT
            version = new ProductVersion("1.2-SNAPSHOT", "1.0");
            plugin.addVersion(version);
            // 2.0
            version = new ProductVersion("2.0", "2.0");
            plugin.addVersion(version);
            version.addArtifact(new Artifact("public-plugin-2.0.zip",
                    "4b84482cde377385aa421ed4be497a74"));
        }
        { // public-plugin-no-versions
            plugin = new Product("public-plugin-no-versions", true);
            galia.addPlugin(plugin);
        }
        { // public-plugin-null-spec-version
            plugin = new Product("public-plugin-null-spec-version", true);
            galia.addPlugin(plugin);
            version = new ProductVersion("1.0", null);
            plugin.addVersion(version);
            version.addArtifact(new Artifact("public-plugin-1.0.zip",
                    "4f998a4dc22e5947350a9171132d318e"));
        }
        { // public-plugin-no-artifact
            plugin = new Product("public-plugin-no-artifact", true);
            galia.addPlugin(plugin);
            version = new ProductVersion("1.0", "1.0");
            plugin.addVersion(version);
        }
        { // public-plugin-null-artifact-filename
            plugin = new Product("public-plugin-null-artifact-filename", true);
            galia.addPlugin(plugin);
            version = new ProductVersion("1.0", "1.0");
            plugin.addVersion(version);
            version.addArtifact(new Artifact(null, "2f969c5d785228e7513b2aa05180391e"));
        }
        { // public-plugin-null-artifact-md5
            plugin = new Product("public-plugin-null-artifact-md5", true);
            galia.addPlugin(plugin);
            version = new ProductVersion("1.0", "1.0");
            plugin.addVersion(version);
            version.addArtifact(new Artifact("public-plugin-1.0.zip", null));
        }
        { // public-plugin-incorrect-checksum
            plugin = new Product("public-plugin-incorrect-checksum", true);
            galia.addPlugin(plugin);
            version = new ProductVersion("1.1", "1.0");
            plugin.addVersion(version);
            version.addArtifact(
                    new Artifact("public-plugin-incorrect-checksum-1.1.zip",
                    "bogus"));
        }

        //////////////////////// private plugins ///////////////////////////

        { // private-plugin
            plugin = new Product("private-plugin", false);
            galia.addPlugin(plugin);
            account.addLicensedProduct(plugin);
            version = new ProductVersion("1.0", "1.0");
            plugin.addVersion(version);
            version.addArtifact(new Artifact("private-plugin-1.0.zip",
                    "1844f2429f128c1d225337ffb518e7e7"));
        }
        { // private-plugin-no-versions
            plugin = new Product("private-plugin-no-versions", false);
            galia.addPlugin(plugin);
            account.addLicensedProduct(plugin);
        }
        { // private-plugin-no-artifact
            plugin = new Product("private-plugin-no-artifact", false);
            galia.addPlugin(plugin);
            account.addLicensedProduct(plugin);
            version = new ProductVersion("1.0", "1.0");
            plugin.addVersion(version);
        }
        { // private-plugin-incorrect-checksum
            plugin = new Product("private-plugin-incorrect-checksum", false);
            galia.addPlugin(plugin);
            account.addLicensedProduct(plugin);
            version = new ProductVersion("1.0", "1.0");
            plugin.addVersion(version);
            version.addArtifact(
                    new Artifact("private-plugin-incorrect-checksum-1.0.zip",
                    "bogus"));
        }
    }

}
