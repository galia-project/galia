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

package is.galia.processor.resample;

/**
 * @see <a href="https://johncostella.com/magic/">The magic kernel</a>
 */
class MagicKernelSharp2013Filter implements ResampleFilter {

    private static final float ONE_EIGHTH           = 1 / 8f;
    private static final float ONE_QUARTER          = 1 / 4f;
    private static final float ONE_HALF             = 1 / 2f;
    private static final float THREE_HALVES         = 3 / 2f;
    private static final float FIVE_HALVES          = 5 / 2f;
    private static final float SEVEN_QUARTERS       = 7 / 4f;
    private static final float SEVENTEEN_SIXTEENTHS = 17 / 16f;

    public float apply(float value) {
        if (-FIVE_HALVES <= value && value <= -THREE_HALVES) {
            return -ONE_EIGHTH * (float) Math.pow(value + FIVE_HALVES, 2);
        }
        if (-THREE_HALVES <= value && value <= -ONE_HALF) {
            return ONE_QUARTER * (4 * (float) Math.pow(value, 2) + (11 * value) + 7);
        }
        if (-ONE_HALF <= value && value <= ONE_HALF) {
            return SEVENTEEN_SIXTEENTHS - SEVEN_QUARTERS * (float) Math.pow(value, 2);
        }
        if (ONE_HALF <= value && value <= THREE_HALVES) {
            return ONE_QUARTER * (4 * (float) Math.pow(value, 2) - (11 * value) + 7);
        }
        if (THREE_HALVES <= value && value <= FIVE_HALVES) {
            return -ONE_EIGHTH * (float) Math.pow(value - FIVE_HALVES, 2);
        }
        return 0;
    }

    public float getSamplingRadius() {
        return 3.0f;
    }

}
