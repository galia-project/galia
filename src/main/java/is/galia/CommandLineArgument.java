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

package is.galia;

/**
 * Represents a recognized/allowed command-line argument.
 *
 * @param name        Argument name, which must be prepended by a dash when
 *                    supplied.
 * @param type        Argument value data type.
 * @param required    Whether the argument must be supplied or not.
 * @param independent If true, when supplied, no other arguments are
 *                    required.
 * @param help        Help text for the argument.
 */
record CommandLineArgument(String name,
                           Type type,
                           boolean required,
                           boolean independent,
                           String help) {
    public enum Type {
        BOOLEAN, INTEGER, STRING, FLAG_ONLY
    }
}
