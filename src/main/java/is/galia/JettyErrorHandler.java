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

package is.galia;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Status;
import is.galia.resource.VelocityRepresentation;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles unrecoverable errors from Jetty. These are errors that happen before
 * a higher-level {@link is.galia.resource.Resource} can be instantiated, such
 * as URI parsing errors.
 */
class JettyErrorHandler extends org.eclipse.jetty.server.handler.ErrorHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JettyErrorHandler.class);

    @Override
    protected void generateResponse(Request request,
                                    Response response,
                                    int code,
                                    String message,
                                    Throwable cause,
                                    Callback callback) throws IOException {
        final Map<String,Object> escapableTemplateVars = new HashMap<>();
        escapableTemplateVars.put("pageTitle", message);
        escapableTemplateVars.put("statusCode", code);
        escapableTemplateVars.put("statusReason", new Status(code).description());
        escapableTemplateVars.put("message", message);

        if (Configuration.forApplication().getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false)) {
            try (StringWriter stringWriter = new StringWriter();
                 PrintWriter printWriter = new PrintWriter(stringWriter)) {
                cause.printStackTrace(printWriter);
                escapableTemplateVars.put("stackTrace", stringWriter.toString());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        final Map<String,Object> nonEscapableTemplateVars = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/webapp/styles/public.css"),
                StandardCharsets.UTF_8))) {
            String css = reader.lines().collect(Collectors.joining("\n"));
            nonEscapableTemplateVars.put("css", css);
        }

        VelocityRepresentation rep = new VelocityRepresentation(
                "/error_inline_css.html.vm",
                escapableTemplateVars, nonEscapableTemplateVars);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            rep.write(os);
            ByteBuffer buffer = ByteBuffer.wrap(os.toByteArray());
            response.write(true, buffer, callback);
        }
    }

}
