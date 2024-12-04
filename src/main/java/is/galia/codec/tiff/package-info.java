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
 * <p>Contains two main components:</p>
 *
 * <ol>
 *     <li>{@link is.galia.codec.tiff.TIFFDecoder}, a wrapper around a TIFF
 *     {@link javax.imageio.ImageReader}</li>
 *     <li>A general-purpose {@link is.galia.codec.tiff.DirectoryReader TIFF
 *     directory reader} that can also be used to parse embedded EXIF metadata
 *     in non-TIFF images.</li>
 * </ol>
 */
package is.galia.codec.tiff;
