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

package is.galia.resource.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract RPC command superclass. JSON objects will be deserialized into
 * subclass instances based on their {@code verb} property.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "verb")
@JsonSubTypes({
        @JsonSubTypes.Type(
                name = "Sleep",
                value = SleepCommand.class),
        @JsonSubTypes.Type(
                name = "PurgeCache",
                value = PurgeCacheCommand.class),
        @JsonSubTypes.Type(
                name = "EvictInfosFromCache",
                value = EvictInfosFromCacheCommand.class),
        @JsonSubTypes.Type(
                name = "EvictInvalidFromCache",
                value = EvictInvalidFromCacheCommand.class),
        @JsonSubTypes.Type(
                name = "EvictItemFromCache",
                value = EvictItemFromCacheCommand.class)
})
abstract class Command {

    abstract String getVerb();

}
