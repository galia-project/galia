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

package is.galia.resource.iiif.v2;

import is.galia.image.Format;

import java.util.HashSet;
import java.util.Set;

/**
 * IIIF compliance level.
 *
 * @see <a href="http://iiif.io/api/image/2.0/compliance.html">Compliance
 * Levels</a>
 */
enum ComplianceLevel {

    LEVEL_0("http://iiif.io/api/image/2/level0.json"),
    LEVEL_1("http://iiif.io/api/image/2/level1.json"),
    LEVEL_2("http://iiif.io/api/image/2/level2.json");

    private static final Set<Feature> LEVEL_1_FEATURES      = new HashSet<>();
    private static final Set<Format> LEVEL_1_OUTPUT_FORMATS = new HashSet<>();
    private static final Set<Feature> LEVEL_2_FEATURES      = new HashSet<>();
    private static final Set<Format> LEVEL_2_OUTPUT_FORMATS = new HashSet<>();

    private final String uri;

    static {
        LEVEL_1_FEATURES.add(ServiceFeature.SIZE_BY_WHITELISTED);
        LEVEL_1_FEATURES.add(ServiceFeature.BASE_URI_REDIRECT);
        LEVEL_1_FEATURES.add(ServiceFeature.CORS);
        LEVEL_1_FEATURES.add(ServiceFeature.JSON_LD_MEDIA_TYPE);
        LEVEL_1_OUTPUT_FORMATS.add(Format.get("jpg"));

        LEVEL_2_FEATURES.addAll(LEVEL_1_FEATURES);
        LEVEL_2_OUTPUT_FORMATS.addAll(LEVEL_1_OUTPUT_FORMATS);
        LEVEL_2_OUTPUT_FORMATS.add(Format.get("png"));
    }

    /**
     * @return The effective compliance level corresponding to the given
     *         arguments.
     */
    public static ComplianceLevel getLevel(Set<ServiceFeature> serviceFeatures,
                                           Set<Format> outputFormats) {
        Set<Feature> allFeatures = new HashSet<>(serviceFeatures);

        ComplianceLevel level = LEVEL_0;
        if (allFeatures.containsAll(LEVEL_1_FEATURES) &&
                outputFormats.containsAll(LEVEL_1_OUTPUT_FORMATS)) {
            level = LEVEL_1;
            if (allFeatures.containsAll(LEVEL_2_FEATURES) &&
                    outputFormats.containsAll(LEVEL_2_OUTPUT_FORMATS)) {
                level = LEVEL_2;
            }
        }
        return level;
    }

    ComplianceLevel(String uri) {
        this.uri = uri;
    }

    public String getURI() {
        return this.uri;
    }

}
