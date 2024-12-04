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

package is.galia.resource;

import is.galia.async.VirtualThreadPool;
import is.galia.codec.SourceFormatException;
import is.galia.codec.VariantFormatException;
import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Status;
import is.galia.operation.IllegalScaleException;
import is.galia.operation.IllegalSizeException;
import is.galia.operation.OperationException;
import is.galia.resource.iiif.FormatException;
import is.galia.status.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>A &quot;special&quot; resource that doesn't have its own URI, doesn't
 * implement {@link Resource}, and is not returned by {@link
 * ResourceFactory}. It is used by {@link RequestHandler}.</p>
 *
 * <p>Its main responsibility is to translate a {@link Throwable} to an HTTP
 * 4xx or 5xx-level response in a variety of content types. It also optionally
 * logs and/or reports the error.</p>
 */
class ErrorResource extends AbstractResource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ErrorResource.class);

    private static final List<String> SUPPORTED_MEDIA_TYPES =
            List.of("text/plain", "text/html", "application/xhtml+xml",
                    "application/json", "application/ld+json");

    private static final Reporter REPORTER = new Reporter();

    private final Throwable throwable;
    private final Status status;

    /**
     * @return Empty set.
     */
    @Override
    public Set<Route> getRoutes() {
        return Set.of();
    }

    private static Status toStatus(Throwable t) {
        Status status;
        if (t instanceof ResourceException) {
            status = ((ResourceException) t).getStatus();
        } else if (t instanceof IllegalSizeException ||
                t instanceof IllegalScaleException ||
                t instanceof AccessDeniedException) {
            status = Status.FORBIDDEN;
        } else if (t instanceof OperationException ||
                t instanceof IllegalClientArgumentException ||
                t instanceof UnsupportedEncodingException) {
            status = Status.BAD_REQUEST;
        } else if (t instanceof FormatException ||
                t instanceof VariantFormatException) {
            status = Status.UNSUPPORTED_MEDIA_TYPE;
        } else if (t instanceof FileNotFoundException ||
                t instanceof NoSuchFileException) {
            status = Status.NOT_FOUND;
        } else if (t instanceof SourceFormatException) {
            status = Status.NOT_IMPLEMENTED;
        } else {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        return status;
    }

    ErrorResource(Throwable throwable) {
        this.throwable = throwable;
        this.status = toStatus(throwable);
    }

    //region AbstractResource overrides

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void doGET() throws Exception {
        final Status status = toStatus(throwable);
        if (status.isServerError()) {
            Configuration config = Configuration.forApplication();
            if (config.getBoolean(Key.LOG_ERROR_RESPONSES, false)) {
                logError(status);
            }
            if (config.getBoolean(Key.REPORT_ERRORS, false)) {
                reportError();
            }
        }

        // Negotiate a response representation type.
        String negotiatedType = negotiateContentType(SUPPORTED_MEDIA_TYPES);
        if (negotiatedType == null) {
            negotiatedType = "text/plain";
        }

        String contentType;
        Representation representation;
        if (List.of("text/html", "application/xhtml+xml").contains(negotiatedType)) {
            contentType    = "text/html";
            representation = new VelocityRepresentation(
                    "/error.html.vm", getHTMLTemplateVars());
        } else if (List.of("application/json", "application/ld+json").contains(negotiatedType)) {
            contentType    = "application/json";
            representation = new JacksonRepresentation(getJSONObject());
        } else {
            contentType    = "text/plain";
            representation = new VelocityRepresentation(
                    "/error.txt.vm", getHTMLTemplateVars());
        }

        getResponse().setStatus(status);
        getResponse().setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        getResponse().setHeader("Content-Type", contentType + ";charset=UTF-8");

        try (OutputStream os = getResponse().openBodyStream()) {
            representation.write(os);
        }
    }

    //endregion
    //region Private methods

    private Map<String,Object> getHTMLTemplateVars() {
        final Map<String,Object> templateVars = getCommonTemplateVars();
        templateVars.put("pageTitle", status.toString());
        templateVars.put("statusCode", status.code());
        templateVars.put("statusReason", status.description());
        templateVars.put("message", throwable.getMessage());

        Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false)) {
            templateVars.put("stackTrace", getStackTrace());
        }
        return templateVars;
    }

    private Map<String,Object> getJSONObject() {
        Map<String,Object> jsonObj = new LinkedHashMap<>();
        jsonObj.put("status", status.code());
        jsonObj.put("reason", status.description());
        jsonObj.put("message", throwable.getMessage());

        Configuration config = Configuration.forApplication();
        if (config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false)) {
            jsonObj.put("stackTrace", throwable.getStackTrace());
        }
        return jsonObj;
    }

    private String getStackTrace() {
        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (IOException e) {
            LOGGER.error("getStackTrace(): {}", e.getMessage(), e);
            return "Stack trace unavailable";
        }
    }

    private void logError(Status status) {
        String message = "Responding with HTTP {} to {} {}: {}";
        Object[] args = {
                status.code(),
                getRequest().getMethod(),
                getRequest().getReference(),
                throwable.getMessage(),
                throwable
        };
        if (status.isServerError()) {
            LOGGER.error(message, args);
        } else if (status.code() != 404) {
            LOGGER.warn(message, args);
        }
    }

    private void reportError() {
        VirtualThreadPool.getInstance().submit(() -> {
            try {
                REPORTER.reportErrorToHQ(throwable);
            } catch (IOException e) {
                LOGGER.warn("Failed to report error: {}", e.getMessage(), e);
            }
        });
    }

}
