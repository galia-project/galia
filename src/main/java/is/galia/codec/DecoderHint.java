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

import is.galia.image.Region;
import is.galia.image.ReductionFactor;

import java.util.Set;

/**
 * Hint provided to or returned from {@link Decoder#decode(int, Region,
 * double[], ReductionFactor, double[], Set)}.
 */
public enum DecoderHint {

    /**
     * Returned from a decoder. The decoder has already transformed the image
     * according to its embedded orientation property.
     */
    ALREADY_ORIENTED,

    /**
     * Returned from a decoder. The decoder has ignored the {@code region}
     * argument and is returning the full region.
     */
    IGNORED_REGION,

    /**
     * Returned from a decoder. The decoder has ignored the {@code scales}
     * argument and is returning the full scale.
     */
    IGNORED_SCALE,

    /**
     * Returned from a decoder. The differential scales have not been applied
     * yet, so will need to be.
     */
    NEEDS_DIFFERENTIAL_SCALE

}
