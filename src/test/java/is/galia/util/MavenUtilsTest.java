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

package is.galia.util;

import is.galia.Application;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MavenUtilsTest extends BaseTest {

    @Test
    void assemblePackage() throws Exception {
        String expectedPackageFilename = Application.getName().toLowerCase() +
                "-" + Application.getVersion() + ".zip";
        Path packagePath = MavenUtils.assemblePackage(expectedPackageFilename);
        assertTrue(Files.exists(packagePath));
        Files.deleteIfExists(packagePath);
    }

    @Test
    void readArtifactIDFromPOM() throws Exception {
        String value = MavenUtils.readArtifactIDFromPOM();
        assertTrue(value.length() >= 5);
    }

    @Test
    void readSpecificationVersionFromPOM() throws Exception {
        String value = MavenUtils.readSpecificationVersionFromPOM();
        assertTrue(value.length() >= 3);
    }

    @Test
    void readVersionFromPOM() throws Exception {
        String value = MavenUtils.readVersionFromPOM();
        assertTrue(value.length() >= 3);
    }

}
