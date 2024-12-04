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

/**
 * <p>Package in which processors reside.</p>
 *
 * <p>A processor is an image processing library. Given a reference to a
 * source image, it does the following:</p>
 *
 * <ol>
 *     <li>Acquires the image from a {@link is.galia.codec.Decoder}</li>
 *     <li>Applies the {@link is.galia.operation.Operation operations} from an
 *     {@link is.galia.operation.OperationList} in order</li>
 *     <li>Writes the result to an {@link java.io.OutputStream} provided by an
 *     {@link is.galia.codec.Encoder}. In this way it is source- and output-
 *     agnostic&mdash;it doesn't care where the source image is coming from,
 *     where the processed variant image is going to, or even what format
 *     either of them are in, necessarily.</li>
 * </ol>
 */
package is.galia.processor;