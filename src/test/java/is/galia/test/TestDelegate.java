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

package is.galia.test;

import is.galia.delegate.Delegate;
import is.galia.delegate.DelegateException;
import is.galia.plugin.Plugin;
import is.galia.resource.RequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mock implementation. There is a file for this class in {@literal
 * src/test/resources/META-INF/services}.
 */
public class TestDelegate implements Delegate, Plugin {

    public static boolean isApplicationStarted, isApplicationStopped;

    public boolean isPluginInitialized;
    private RequestContext requestContext;

    //region Plugin methods

    @Override
    public Set<String> getPluginConfigKeys() {
        return Set.of();
    }

    @Override
    public String getPluginName() {
        return TestDelegate.class.getSimpleName();
    }

    @Override
    public void onApplicationStart() {
        isApplicationStarted = true;
    }

    @Override
    public void onApplicationStop() {
        isApplicationStopped = true;
    }

    @Override
    public void initializePlugin() {
        isPluginInitialized = true;
    }

    //endregion
    //region Delegate methods


    @Override
    public void setRequestContext(RequestContext context) {
        this.requestContext = context;
    }

    @Override
    public RequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * Used by {@link is.galia.image.DelegateMetaIdentifierTransformerTest}
     */
    @Override
    public Map<String, Object> deserializeMetaIdentifier(String metaIdentifier)
            throws DelegateException {
        return Delegate.super.deserializeMetaIdentifier(metaIdentifier);
    }

    /**
     * Used by {@link is.galia.image.DelegateMetaIdentifierTransformerTest}
     */
    @Override
    public String serializeMetaIdentifier(Map<String, Object> metaIdentifier)
            throws DelegateException {
        return Delegate.super.serializeMetaIdentifier(metaIdentifier);
    }

    @Override
    public Object authorizeBeforeAccess() {
        String identifier = getRequestContext().getIdentifier().toString();
        return switch (identifier) {
            case "allowed.jpg"      -> true;
            case "forbidden.jpg"    -> false;
            case "unauthorized.jpg" -> Map.of(
                    "status_code", 401,
                    "challenge", "Basic");
            case "redirect.jpg"     -> Map.of(
                    "status_code", 303,
                    "location", "http://example.org/");
            case "reduce.jpg"       -> Map.of(
                    "status_code", 302,
                    "scale_numerator", 1,
                    "scale_denominator", 2);
            default                 -> true;
        };
    }

    @Override
    public Object authorize() {
        String identifier = getRequestContext().getIdentifier().toString();
        return switch (identifier) {
            case "allowed.jpg"      -> true;
            case "forbidden.jpg"    -> false;
            case "unauthorized.jpg" -> Map.of(
                    "status_code", 401,
                    "challenge", "Basic");
            case "redirect.jpg"     -> Map.of(
                    "status_code", 303,
                    "location", "http://example.org/");
            case "reduce.jpg"       -> Map.of(
                    "status_code", 302,
                    "scale_numerator", 1,
                    "scale_denominator", 2);
            default                 -> true;
        };
    }

    @Override
    public void customizeIIIF1InformationResponse(Map<String,Object> info) {
        info.put("new_key", "new value");
    }

    @Override
    public void customizeIIIF2InformationResponse(Map<String,Object> info) {
        info.put("new_key", "new value");
    }

    @Override
    public void customizeIIIF3InformationResponse(Map<String,Object> info) {
        info.put("new_key", "new value");
    }

    @Override
    public String getFilesystemSourcePathname() {
        String identifier = getRequestContext().getIdentifier().toString();
        return switch (identifier) {
            case "missing" -> null;
            default        -> identifier;
        };
    }

    @Override
    public Map<String, ?> getHTTPSourceResourceInfo() {
        String identifier = getRequestContext().getIdentifier().toString();

        // Supply a localhost URL to return the same URL.
        if (identifier.startsWith("http://localhost") ||
                identifier.startsWith("https://localhost")) {
            return Map.of(
                    "uri", identifier,
                    "headers", Map.of(
                            "x-custom", "yes"));
        }
        // Supply a valid URL prefixed with "valid-auth-" to return a valid URL
        // with valid auth info.
        else if (identifier.startsWith("valid-auth-")) {
            return Map.of(
                    "uri", identifier.replaceAll("valid-auth-", ""),
                    "username", "user",
                    "secret", "secret");
        }
        // Supply a valid URL prefixed with "invalid-auth-" to return a valid URL
        // with invalid auth info.
        else if (identifier.startsWith("invalid-auth-")) {
            return Map.of(
                    "uri", identifier.replaceAll("invalid-auth-", ""),
                    "username", "user",
                    "secret", "bogus");
        }
        else if ("1.2.3.4".equals(getRequestContext().getClientIP())) {
            if ("https".equals(getRequestContext().getRequestHeaders().get("x-forwarded-proto"))) {
                return Map.of(
                        "uri", "https://other-example.org/bleh/" + identifier);
            } else {
                return Map.of(
                        "uri", "http://other-example.org/bleh/" + identifier);
            }
        }

        return switch (identifier) {
            case "http-sample-images/jpg/rgb-64x56x8-baseline.jpg" -> Map.of(
                    "uri", "http://example.org/bla/" + identifier,
                    "headers", Map.of("x-custom", "yes"),
                    "send_head_request", true);
            case "https-sample-images/jpg/rgb-64x56x8-baseline.jpg" -> Map.of(
                    "uri", "https://example.org/bla/" + identifier,
                    "headers", Map.of("x-custom", "yes"),
                    "send_head_request", true);
            case "http-sample-images/jpg/rgb-64x56x8-plane.jpg" -> Map.of(
                    "uri", "http://example.org/bla/" + identifier,
                    "username", "username",
                    "secret", "secret",
                    "headers", Map.of("x-custom", "yes"),
                    "send_head_request", true);
            case "https-sample-images/jpg/rgb-64x56x8-plane.jpg" -> Map.of(
                    "uri", "https://example.org/bla/" + identifier,
                    "username", "username",
                    "secret", "secret",
                    "headers", Map.of("x-custom", "yes"),
                    "send_head_request", true);
            default -> Map.of();
        };
    }

    @Override
    public String getMetadata() {
        String identifier = getRequestContext().getIdentifier().toString();
        return switch (identifier) {
            case "metadata" -> "<rdf:RDF>variant metadata</rdf:RDF>";
            default         -> null;
        };
    }

    /**
     * Used by {@link is.galia.operation.overlay.DelegateOverlayServiceTest}.
     */
    @Override
    public Map<String, Object> getOverlayProperties() {
        String identifier = getRequestContext().getIdentifier().toString();
        switch (identifier) {
            case "image" -> {
                return Map.of("image", "/dev/cats",
                        "inset", 5,
                        "position", "bottom left");
            }
            case "string" -> {
                return Map.ofEntries(
                        Map.entry("background_color", "rgba(12, 23, 34, 45)"),
                        Map.entry("string", "dogs\ndogs"),
                        Map.entry("inset", 5),
                        Map.entry("position", "bottom left"),
                        Map.entry( "color", "red"),
                        Map.entry("font", "SansSerif"),
                        Map.entry("font_size", 20),
                        Map.entry("font_min_size", 11),
                        Map.entry("font_weight", 1.5),
                        Map.entry("glyph_spacing", 0.1),
                        Map.entry("stroke_color", "blue"),
                        Map.entry("stroke_width", 3),
                        Map.entry("word_wrap", false));
            }
        }
        return Map.of();
    }

    /**
     * Used by {@link is.galia.operation.redaction.RedactionServiceTest}
     */
    @Override
    public List<Map<String, Object>> getRedactions() {
        String identifier = getRequestContext().getIdentifier().toString();
        List<Map<String,Object>> redactions = new ArrayList<>();
        switch (identifier) {
            case "redacted":
                redactions.add(
                        Map.of("x", 0, "y", 10, "width", 50, "height", 70,
                                "color", "black"));
        }
        return redactions;
    }

    @Override
    public String getSource() {
        String identifier = getRequestContext().getIdentifier().toString();
        return switch (identifier) {
            case "http"  -> "HTTPSource";
            case "bogus" -> null;
            default      -> "FilesystemSource";
        };
    }

}
