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

package is.galia.image;

import is.galia.delegate.Delegate;

/**
 * <p>Converts a {@link MetaIdentifier} to and from a string.</p>
 *
 * <p>Note that this tool on its own is not sufficient to create a
 * serialization suitable for a URI path component, nor to deserialize a URI
 * path component, as it does not perform URL coding or anything else. Look to
 * {@link MetaIdentifier#fromURI(String, Delegate)} and
 * {@link MetaIdentifier#forURI(Delegate)} instead.</p>
 */
public interface MetaIdentifierTransformer {

    /**
     * <p>Deserializes the given meta-identifier string into its component
     * parts.</p>
     *
     * <p>If the string cannot be parsed, it is set as the returned instance's
     * {@link MetaIdentifier#identifier()} identifier property}.</p>
     *
     * @param metaIdentifier Meta-identifier string to deserialize. The string
     *                       has already been URL-decoded and had slashes
     *                       substituted.
     */
    MetaIdentifier deserialize(String metaIdentifier);

    /**
     * <p>Serializes an instance.</p>
     *
     * @param metaIdentifier Instance to serialize.
     * @return Serialized instance, not yet URL-encoded or with slashes
     *         substituted.
     */
    String serialize(MetaIdentifier metaIdentifier);

}
