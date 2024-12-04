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

package is.galia.source;

import is.galia.config.Configuration;
import is.galia.config.Key;

public enum LookupStrategy {

    BASIC, DELEGATE_SCRIPT, UNDEFINED;

    public static LookupStrategy from(Key key) {
        return from(key.key());
    }

    public static LookupStrategy from(String key) {
        final Configuration config = Configuration.forApplication();
        return switch (config.getString(key, "")) {
            case "BasicLookupStrategy"    -> BASIC;
            case "DelegateLookupStrategy" -> DELEGATE_SCRIPT;
            default                       -> UNDEFINED;
        };
    }

}
