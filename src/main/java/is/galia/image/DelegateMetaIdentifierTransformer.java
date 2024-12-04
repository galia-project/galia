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
import is.galia.delegate.DelegateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms meta-identifiers to or from arbitrary formats using a delegate
 * method.
 */
public final class DelegateMetaIdentifierTransformer
        implements MetaIdentifierTransformer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DelegateMetaIdentifierTransformer.class);

    private static final String IDENTIFIER_KEY       = "identifier";
    private static final String PAGE_NUMBER_KEY      = "page_number";
    private static final String SCALE_CONSTRAINT_KEY = "scale_constraint";

    private Delegate delegate;

    /**
     * Breaks apart the given meta-identifier into its constituent components.
     */
    @Override
    public MetaIdentifier deserialize(String metaIdentifier) {
        try {
            final Map<String, Object> result =
                    delegate.deserializeMetaIdentifier(metaIdentifier);
            final MetaIdentifier.Builder builder = MetaIdentifier.builder();
            { // identifier
                builder.withIdentifier((String) result.get(IDENTIFIER_KEY));
            }
            { // page number
                Number pageNumber = (Number) result.get(PAGE_NUMBER_KEY);
                if (pageNumber != null) {
                    builder.withPageNumber(pageNumber.intValue());
                }
            }
            { // scale constraint
                @SuppressWarnings("unchecked")
                List<Number> scaleConstraint =
                        (List<Number>) result.get(SCALE_CONSTRAINT_KEY);
                if (scaleConstraint != null) {
                    builder.withScaleConstraint(
                            scaleConstraint.get(0).intValue(),
                            scaleConstraint.get(1).intValue());
                }
            }
            return builder.build();
        } catch (DelegateException e) {
            LOGGER.error("deserialize(): {}", e.getMessage());
        }
        return null;
    }

    /**
     * Joins the give instance into a meta-identifier string.
     */
    @Override
    public String serialize(MetaIdentifier metaIdentifier) {
        final Map<String,Object> map = new HashMap<>();
        { // identifier
            map.put(IDENTIFIER_KEY, metaIdentifier.identifier().toString());
        }
        { // page number
            if (metaIdentifier.pageNumber() != null) {
                map.put(PAGE_NUMBER_KEY, metaIdentifier.pageNumber());
            }
        }
        { // scale constraint
            ScaleConstraint sc = metaIdentifier.scaleConstraint();
            if (sc == null) {
                sc = new ScaleConstraint(1, 1);
            }
            if (sc.hasEffect()) {
                map.put(SCALE_CONSTRAINT_KEY, List.of(
                        sc.rational().numerator(),
                        sc.rational().denominator()));
            }
        }
        try {
            return delegate.serializeMetaIdentifier(map);
        } catch (DelegateException e) {
            LOGGER.error("serialize(): {}", e.getMessage());
            return null;
        }
    }

    void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

}
