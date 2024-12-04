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

package is.galia.delegate;

import is.galia.resource.RequestContext;
import is.galia.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Delegates a variety of methods to user-supplied code.</p>
 *
 * <p>New instances are generally acquired via {@link
 * DelegateFactory#newDelegate(RequestContext)}.</p>
 */
public interface Delegate {

    /**
     * Provides information about the request to the instance. This is the
     * first method in this interface to be invoked.
     *
     * @param context Context to set.
     * @throws DelegateException if the delegate object does not contain a
     *         setter method for the context.
     */
    void setRequestContext(RequestContext context) throws DelegateException;

    /**
     * @return The instance passed to {@link
     *         #setRequestContext(RequestContext)}.
     */
    RequestContext getRequestContext();

    /**
     * <p>Deserializes the given meta-identifier string into its component
     * parts.</p>
     *
     * <p>The identifier portion has already been URL-decoded.</p>
     *
     * <p>This method is used only when the {@link
     * is.galia.config.Key#META_IDENTIFIER_TRANSFORMER} configuration key is
     * set to the simple class name of {@link
     * is.galia.image.DelegateMetaIdentifierTransformer}.</p>
     *
     * <p>The returned instance contains the following keys:</p>
     *
     * <dl>
     *     <dt>{@code identifier}</dt>
     *     <dd>String. Required.</dd>
     *     <dt>{@code page_number}</dt>
     *     <dd>Integer. Optional.</dd>
     *     <dt>{@code scale_constraint}</dt>
     *     <dd>Two-element array of integers with scale constraint numerator at
     *     position 0 and denominator at position 1. Optional.</dd>
     * </dl>
     *
     * <p>This default implementation supports the {@code
     * identifier;page_number;sc_numerator:sc_denominator} scheme.</p>
     *
     * @param metaIdentifier Meta-identifier to deserialize.
     * @return See above. The return value must be compatible with the argument
     *         to {@link #serializeMetaIdentifier(Map)}.
     * @throws DelegateException if there is any problem generating a result.
     */
    default Map<String,Object> deserializeMetaIdentifier(String metaIdentifier)
            throws DelegateException {
        Map<String,Object> map = new HashMap<>();
        String reversedMetaID  = StringUtils.reverse(metaIdentifier);
        Pattern pattern        = Pattern.compile("^((?<sc>\\d+:\\d+);)?((?<pg>\\d+);)?(?<id>.+)");
        Matcher matcher        = pattern.matcher(reversedMetaID);
        if (matcher.find()) {
            String sc = matcher.group("sc");
            if (sc != null) {
                List<Integer> intArray = Arrays.stream(StringUtils.reverse(sc).split(":"))
                        .map(Integer::parseInt)
                        .toList();
                map.put("scale_constraint", intArray);
            }
            String pg = matcher.group("pg");
            if (pg != null) {
                map.put("page_number", Integer.parseInt(StringUtils.reverse(pg)));
            }
            String id = matcher.group("id");
            map.put("identifier", StringUtils.reverse(id));
        }
        return map;
    }

    /**
     * <p>Serializes the given meta-identifier hash, but does not URL-encode
     * it.</p>
     *
     * <p>This method is used only when the {@link
     * is.galia.config.Key#META_IDENTIFIER_TRANSFORMER} configuration key is
     * set to the simple class name of {@link
     * is.galia.image.DelegateMetaIdentifierTransformer}.</p>
     *
     * <p>This default implementation supports the {@code
     * identifier;page_number;sc_numerator:sc_denominator} scheme.</p>
     *
     * @param metaIdentifier See {@link #deserializeMetaIdentifier} for a
     *        description of the map structure.
     * @return Serialized meta-identifier compatible with the argument to
     *         {@link #deserializeMetaIdentifier(String)}.
     * @throws DelegateException if there is any problem generating a result.
     */
    default String serializeMetaIdentifier(Map<String,Object> metaIdentifier)
            throws DelegateException {
        // identifier
        String identifier = (String) metaIdentifier.get("identifier");
        // page_number
        Number pageNumber = (Number) metaIdentifier.get("page_number");
        String pageStr    = "";
        if (pageNumber != null) {
            pageStr = "" + pageNumber;
        }
        // scale_constraint
        @SuppressWarnings("unchecked")
        List<Long> scParts = (List<Long>) metaIdentifier.get("scale_constraint");
        String sc;
        if (scParts != null) {
            sc = scParts.stream()
                    .map(Objects::toString)
                    .collect(Collectors.joining(":"));
        } else {
            sc = "";
        }
        return Stream.of(identifier, pageStr, sc)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(";"));
    }

    /**
     * <p>Returns authorization status for the current request. This method is
     * called upon all requests to all public endpoints early in the request
     * lifecycle, before the image has been accessed. This means that some
     * {@link #getRequestContext() request context} keys will not be available
     * yet.</p>
     *
     * <p>This method should implement all possible authorization logic except
     * that which requires any of the context keys that aren't yet available.
     * This will ensure efficient authorization failures.
     *
     * <p>Implementations should assume that the underlying resource is
     * available, and not try to check for it.</p>
     *
     * <p>Possible return values:</p>
     *
     * <ol>
     *     <li>Boolean {@code true}/{@code false}, indicating whether the
     *     request is fully authorized or not. If false, the client will
     *     receive a 403 Forbidden response.</li>
     *     <li>Hash with a {@code status_code} key.
     *         <ol>
     *             <li>If it corresponds to an integer from 200-299, the
     *             request is authorized.</li>
     *             <li>If it corresponds to an integer from 300-399:
     *                 <ol>
     *                     <li>If the hash also contains a {@code location} key
     *                     corresponding to a URI string, the request will be
     *                     redirected to that URI using that code.</li>
     *                     <li>If the hash also contains {@code
     *                     scale_numerator} and {@code scale_denominator} keys,
     *                     the request will be redirected using that code to a
     *                     virtual reduced-scale version of the source
     *                     image.</li>
     *                 </ol>
     *             </li>
     *             <li>If it corresponds to 401, the hash must include a {@code
     *             challenge} key corresponding to a WWW-Authenticate header
     *             value.</li>
     *         </ol>
     *     </li>
     * </ol>
     *
     * @return See above.
     * @throws DelegateException if there is any problem generating a result.
     */
    default Object authorizeBeforeAccess() throws DelegateException {
        return true;
    }

    /**
     * <p>Returns authorization status for the current request. Will be called
     * upon all requests to all public image endpoints.</p>
     *
     * <p>This is a counterpart of {@link #authorizeBeforeAccess()} that is invoked
     * later in the request cycle, once information about the source image has
     * become available. It should only contain logic that depends on request
     * context keys that aren't available yet from {@link #authorizeBeforeAccess()}.</p>
     *
     * <p>Implementations should assume that the underlying resource is
     * available, and not try to check for it.</p>
     *
     * @return See the documentation of {@link #authorizeBeforeAccess()}.
     * @throws DelegateException if there is any problem generating a result.
     */
    default Object authorize() throws DelegateException {
        return true;
    }

    /**
     * <p>Can be used to modify the JSON content of an IIIF Image API 1.x
     * information response. See the <a
     * href="https://iiif.io/api/image/1.1/#image-info-request">IIIF Image API
     * 1.1</a> specification and &quot;endpoints&quot; section of the user
     * manual.</p>
     *
     * @param info Modifiable information content.
     * @throws DelegateException if there is any problem generating a result.
     */
    default void customizeIIIF1InformationResponse(Map<String,Object> info)
            throws DelegateException {
    }

    /**
     * <p>Can be used to modify the JSON content of an IIIF Image API 2.x
     * information response. See the <a
     * href="http://iiif.io/api/image/2.1/#image-information">IIIF Image API
     * 2.1</a> specification and &quot;endpoints&quot; section of the user
     * manual.</p>
     *
     * @param info Modifiable information content.
     * @throws DelegateException if there is any problem generating a result.
     */
    default void customizeIIIF2InformationResponse(Map<String,Object> info)
            throws DelegateException {
    }

    /**
     * <p>Can be used to modify the JSON content of an IIIF Image API 3.x
     * information response. See the <a
     * href="http://iiif.io/api/image/3.0/#image-information">IIIF Image API
     * 3.0</a> specification and &quot;endpoints&quot; section of the user
     * manual.</p>
     *
     * @param info Modifiable information content.
     * @throws DelegateException if there is any problem generating a result.
     */
    default void customizeIIIF3InformationResponse(Map<String,Object> info)
            throws DelegateException {
    }

    /**
     * Tells the server which {@link is.galia.source.Source} implementation to
     * use for the given identifier.
     *
     * @return Source class name.
     * @throws DelegateException if there is any problem generating a result.
     */
    default String getSource() throws DelegateException {
        return null;
    }

    /**
     * N.B.: this method should not try to perform authorization.
     *
     * @return Absolute pathname of the image corresponding to the identifier
     *         in the request context, or {@code null} if not found.
     * @throws DelegateException if there is any problem generating a result.
     */
    default String getFilesystemSourcePathname() throws DelegateException {
        return null;
    }

    /**
     * Returns one of the following:
     *
     * <ol>
     *     <li>String URI</li>
     *     <li>Map with the following keys:
     *         <dl>
     *             <dt>{@code uri}</dt>
     *             <dd>String. Required.</dd>
     *             <dt>{@code username}</dt>
     *             <dd>For HTTP Basic authentication. Optional.</dd>
     *             <dt>{@code secret}</dt>
     *             <dd>For HTTP Basic authentication. Optional.</dd>
     *             <dt>{@code headers}</dt>
     *             <dd>Hash of request headers. Optional.</dd>
     *             <dt>{@code send_head_request}</dt>
     *             <dd>Optional. Defaults to {@code true}. See the
     *             documentation of the {@link
     *             is.galia.config.Key#HTTPSOURCE_SEND_HEAD_REQUESTS}
     *             configuration key in the sample config file.</dd>
     *         </dl>
     *     </li>
     *     <li>{@code null} if not found.</li>
     * </ol>
     *
     * <p>N.B.: This method should not try to perform authorization. {@link
     * #authorize()} should be used instead.</p>
     *
     * @return See above.
     * @throws DelegateException if there is any problem generating a result.
     */
    default Map<String,?> getHTTPSourceResourceInfo() throws DelegateException {
        return Map.of();
    }

    /**
     * <p>Returns XMP metadata to embed in the variant image.</p>
     *
     * <p>Source image metadata is available in the {@code metadata} request
     * context key, and has the following structure:</p>
     *
     * <pre>{@code
     * {
     *     "exif": {
     *         "tagSet": "Baseline TIFF",
     *         "fields": {
     *             "Field1Name": [
     *                 value
     *             ],
     *             "Field2Name": [
     *                 value
     *             ],
     *             "EXIFIFD": {
     *                 "tagSet": "EXIF",
     *                 "fields": {
     *                     "Field1Name": [
     *                         value
     *                     ],
     *                     "Field2Name": [
     *                         value
     *                     ]
     *                 }
     *             }
     *         }
     *     },
     *     "iptc": [
     *         "Field1Name": value,
     *         "Field2Name": value
     *     ],
     *     "xmp_string": "&lt;rdf:RDF&gt;...&lt;/rdf:RDF&gt;",
     *     "xmp_model": See https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Model.html,
     *     "xmp_elements": {
     *         "Field1Name": "value",
     *         "Field2Name": [
     *             "value1",
     *             "value2"
     *         ]
     *     },
     *     "native": {
     *         # structure varies
     *     }
     * }}</pre>
     *
     * <ul>
     *     <li>The {@code exif} key refers to embedded EXIF data. This also
     *     includes IFD0 metadata from source TIFFs, whether or not an EXIF IFD
     *     is present.</li>
     *     <li>The {@code iptc} key refers to embedded IPTC IIM data.</li>
     *     <li>The {@code xmp_string} key refers to raw embedded XMP data.</li>
     *     <li>The {@code xmp_model} key contains a Jena Model object
     *     pre-loaded with the contents of {@code xmp_string}.</li>
     *     <li>The {@code xmp_elements} key contains a view of the embedded XMP
     *     data as key-value pairs. This is convenient to use, but won't work
     *     correctly with XMP fields that cannot be expressed as key-value
     *     pairs.</li>
     *     <li>The {@code native} key refers to format-specific metadata.</li>
     * </ul>
     *
     * <p>Any combination of the above keys may be present or missing depending
     * on what is available in a particular source image.</p>
     *
     * <p>Only XMP can be embedded in variant images. See the Guide for
     * examples of working with the XMP model programmatically.</p>
     *
     * @return String or Jena model containing XMP data to embed in the variant
     *         image, or {@code null} to not embed anything.
     * @throws DelegateException if there is any problem generating a result.
     */
    default String getMetadata() throws DelegateException {
        return null;
    }

    /**
     * <p>Tells the server what overlay, if any, to apply to an image. Called
     * upon all image requests to any endpoint if {@link
     * is.galia.config.Key#OVERLAY_ENABLED overlays are enabled} and the {@link
     * is.galia.config.Key#OVERLAY_STRATEGY overlay strategy} is set to {@code
     * DelegateStrategy} in the application configuration.</p>
     *
     * <p>Possible return values:</p>
     *
     * <ol>
     *     <li>For string overlays, a map with the following keys:
     *         <dl>
     *             <dt>{@code background_color}</dt>
     *             <dd>CSS-compliant RGA(A) color.</dd>
     *             <dt>{@code color}</dt>
     *             <dd>CSS-compliant RGA(A) color.</dd>
     *             <dt>{@code font}</dt>
     *             <dd>Font name.</dd>
     *             <dt>{@code font_min_size}</dt>
     *             <dd>Minimum font size in points. Ignored when {@code
     *             word_wrap} is {@code true}.</dd>
     *             <dt>{@code font_size}</dt>
     *             <dd>Font size in points.</dd>
     *             <dt>{@code font_weight}</dt>
     *             <dd>Font weight based on 1.</dd>
     *             <dt>{@code glyph_spacing}</dt>
     *             <dd>Glyph spacing based on 0.</dd>
     *             <dt>{@code inset}</dt>
     *             <dd>Pixels of inset.</dd>
     *             <dt>{@code position}</dt>
     *             <dd>Position like {@code top left}, {@code center}, {@code
     *             center right}, etc.</dd>
     *             <dt>{@code string}</dt>
     *             <dd>String to draw.</dd>
     *             <dt>{@code stroke_color}</dt>
     *             <dd>CSS-compliant RGB(A) text outline color.</dd>
     *             <dt>{@code stroke_width}</dt>
     *             <dd>Text outline width in pixels.</dd>
     *             <dt>{@code word_wrap}</dt>
     *             <dd>Whether to wrap long lines within {@code string} ({@code
     *             true} or {@code false}).</dd>
     *         </dl>
     *     </li>
     *     <li>For image overlays, a hash with the following keys:
     *         <dl>
     *             <dt>{@code image}</dt>
     *             <dd>Image pathname or URL.</dd>
     *             <dt>{@code position}</dt>
     *             <dd>See above.</dd>
     *             <dt>{@code inset}</dt>
     *             <dd>See above.</dd>
     *         </dl>
     *     </li>
     *     <li>{@code null} for no overlay.</li>
     * </ol>
     *
     * @return See above.
     * @throws DelegateException if there is any problem generating a result.
     */
    default Map<String,Object> getOverlayProperties() throws DelegateException {
        return Map.of();
    }

    /**
     * <p>Draws one or more rectangles over an image in response to a request.
     * Will be called upon all image requests to any endpoint.</p>
     *
     * @return List of maps, each with {@code x}, {@code y}, {@code width},
     *         {@code height}, and {@code color} keys. The first four have
     *         integer values, and the last is a valid CSS color. If no
     *         redactions are to be applied, an empty list is returned.
     * @throws DelegateException if there is any problem generating a result.
     */
    default List<Map<String,Object>> getRedactions() throws DelegateException {
        return List.of();
    }

    /**
     * <p>Invokes an arbitrarily-named method.</p>
     *
     * <p>This default implementation simply invokes the method with the given
     * name defined in the same class.</p>
     *
     * @param methodName Name of the method to invoke.
     * @param args       Arguments to pass to the invocation.
     * @return           Method return value.
     * @throws DelegateException if there is any problem generating a result.
     */
    default Object invoke(String methodName,
                          Object... args) throws DelegateException {
        try {
            Method method = getClass().getDeclaredMethod(methodName);
            return method.invoke(this, args);
        } catch (NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new DelegateException(e);
        }
    }

}
