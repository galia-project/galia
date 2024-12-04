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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Transforms meta-identifiers to or from one of the following formats:</p>
 *
 * <ul>
 *     <li>{@code Identifier}</li>
 *     <li>{@code Identifier;PageNumber}</li>
 *     <li>{@code Identifier;ScaleConstraint}</li>
 *     <li>{@code Identifier;PageNumber;ScaleConstraint}</li>
 * </ul>
 *
 * <p>(The {@code ;} character is customizable via the {@link
 * Key#STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER} configuration key.)</p>
 */
public final class StandardMetaIdentifierTransformer
        implements MetaIdentifierTransformer {

    private static final String DEFAULT_COMPONENT_DELIMITER = ";";

    private static String getComponentDelimiter() {
        Configuration config = Configuration.forApplication();
        return config.getString(
                Key.STANDARD_META_IDENTIFIER_TRANSFORMER_DELIMITER,
                DEFAULT_COMPONENT_DELIMITER);
    }

    private static Pattern getReverseMetaIdentifierPattern() {
        final String separator = StringUtils.reverse(getComponentDelimiter());
        return Pattern.compile("^((?<sc>\\d+:\\d+)" + separator +
                ")?((?<page>\\d+)" + separator + ")?(?<id>.+)");
    }

    /**
     * Breaks apart the given meta-identifier into its constituent components.
     */
    @Override
    public MetaIdentifier deserialize(final String metaIdentifier) {
        // Reversing the string enables it to be easily parsed using a regex.
        // Otherwise it would be a lot harder to parse any component delimiters
        // present in the identifier portion.
        final String reversedMetaID = StringUtils.reverse(metaIdentifier);
        final Matcher matcher       = getReverseMetaIdentifierPattern().matcher(reversedMetaID);
        final MetaIdentifier.Builder builder = MetaIdentifier.builder();
        if (matcher.matches()) {
            String idStr = StringUtils.reverse(matcher.group("id"));
            builder.withIdentifier(idStr);
            if (matcher.group("page") != null) {
                String pageStr = StringUtils.reverse(matcher.group("page"));
                int pageNumber = Integer.parseInt(pageStr);
                builder.withPageNumber(pageNumber);
            }
            if (matcher.group("sc") != null) {
                String scStr = StringUtils.reverse(matcher.group("sc"));
                String[] parts = scStr.split(":");
                if (parts.length == 2) {
                    builder.withScaleConstraint(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]));
                }
            }
            return builder.build();
        }
        return builder.withIdentifier(metaIdentifier).build();
    }

    /**
     * Joins the give instance into a meta-identifier string.
     *
     * @param metaIdentifier Instance to serialize.
     */
    @Override
    public String serialize(MetaIdentifier metaIdentifier) {
        return serialize(metaIdentifier, true);
    }

    /**
     * Joins the give instance into a meta-identifier string.
     *
     * @param metaIdentifier Instance to serialize.
     * @param normalize Whether to omit redundant information (such as a page
     *                  number of 1) from the result. This is used in testing.
     */
    public String serialize(MetaIdentifier metaIdentifier, boolean normalize) {
        final String separator = getComponentDelimiter();
        final StringBuilder builder = new StringBuilder();
        builder.append(metaIdentifier.identifier());
        if (metaIdentifier.pageNumber() != null) {
            if (!normalize || metaIdentifier.pageNumber() != 1) {
                builder.append(separator);
                builder.append(metaIdentifier.pageNumber());
            }
        }
        if (metaIdentifier.scaleConstraint() != null) {
            if (!normalize || metaIdentifier.scaleConstraint().hasEffect()) {
                builder.append(separator);
                builder.append(metaIdentifier.scaleConstraint());
            }
        }
        return builder.toString();
    }

}
