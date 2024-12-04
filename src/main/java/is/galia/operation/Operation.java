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

package is.galia.operation;

import is.galia.image.Size;
import is.galia.image.ScaleConstraint;

import java.util.Map;

/**
 * Interface to be implemented by all image-processing operations. Clients
 * should check instances' type and recast.
 */
public interface Operation {

    /**
     * Makes the instance unmodifiable. When frozen, mutation methods should
     * throw an {@link IllegalStateException} and getters should return
     * immutable values, if possible. (But they should do that anyway.)
     */
    void freeze();

    /**
     * This default implementation returns the {@code fullSize} argument. It
     * will need to be overridden by operations that could change the image's
     * resulting size.
     *
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint applied to the given full size.
     * @return                Resulting dimensions when the operation is
     *                        applied.
     */
    default Size getResultingSize(Size fullSize,
                                  ScaleConstraint scaleConstraint) {
        return fullSize;
    }

    /**
     * Simpler but less-accurate counterpart of {@link
     * #hasEffect(Size, OperationList)}.
     *
     * @return Whether applying the operation on its own would result in a
     *         changed image.
     */
    boolean hasEffect();

    /**
     * Context-aware counterpart to {@link #hasEffect()}. For example, a scale
     * operation specifying a scale to 300&times;200, when the given operation
     * list contains a crop of 300&times;200, would return {@code false}.
     *
     * @param fullSize Full size of the source image.
     * @param opList   Operation list of which the operation may or may not be
     *                 a member.
     * @return         Whether applying the operation in the context of the
     *                 given full size and operation list would result in a
     *                 changed image.
     */
    boolean hasEffect(Size fullSize, OperationList opList);

    /**
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint applied to the given full size.
     * @return                Unmodifiable representation of the operation for
     *                        human consumption. The map includes a {@code
     *                        class} key pointing to the operation's class
     *                        name.
     */
    Map<String,Object> toMap(Size fullSize,
                             ScaleConstraint scaleConstraint);

    /**
     * <p>Validates the instance, throwing an exception if invalid.</p>
     *
     * <p>Implementations can also throw exceptions from property setters as an
     * alternative to using this method.</p>
     *
     * <p>This default implementation does nothing.</p>
     *
     * @param fullSize        Full size of the source image on which the
     *                        operation is being applied.
     * @param scaleConstraint Scale constraint applied to the given full size.
     * @throws OperationException if the instance is invalid.
     */
    default void validate(Size fullSize,
                          ScaleConstraint scaleConstraint) throws OperationException {}

}
