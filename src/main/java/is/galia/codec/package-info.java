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
 * <p>Contains classes related to reading/decoding and writing/coding of images
 * and image metadata.</p>
 *
 * <h2>Image I/O readers &amp; writers</h2>
 *
 * <p>{@link is.galia.codec.Decoder} and {@link is.galia.codec.Encoder}
 * implementations wrap {@link javax.imageio.ImageReader} and {@link
 * javax.imageio.ImageWriter} instances to augment them with improved
 * functionality, including simplified reading and writing methods, improved
 * metadata access, and more efficient handling of multi-resolution source
 * images.</p>
 *
 * <p>Instances can be obtained from {@link is.galia.codec.DecoderFactory} and
 * {@link is.galia.codec.EncoderFactory}, respectively.</p>
 *
 * <h2>Custom readers &amp; writers</h2>
 *
 * <p>The Image I/O readers &amp; writers work well within the bounds of their
 * capabilities, but there are some things they can't do, or for which the API
 * is awkward. Where going through Image I/O would be too difficult, there are
 * also some custom classes in subpackages that access images directly.</p>
 *
 * <h2>Metadata</h2>
 *
 * <p>Metadata may be encoded in different ways in different image formats.
 * {@link is.galia.image.Metadata} is an
 * interface for normalized metadata that can either be returned from a reader,
 * or assembled pretty easily, and then passed to a writer which codes it
 * differently depending on the format being written.</p>
 */
package is.galia.codec;
