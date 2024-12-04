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

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Method;
import is.galia.http.Reference;
import is.galia.http.Status;
import is.galia.util.Stopwatch;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Handles all requests.
 */
public class RequestHandler extends DefaultHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RequestHandler.class);

    @Override
    public boolean handle(Request jettyRequest,
                          Response jettyResponse,
                          Callback callback) throws Exception {
        jettyRequest.getComponents().getExecutor().execute(() -> {
            final Stopwatch requestClock = new Stopwatch();

            final HttpURI publicURI = jettyRequest.getHttpURI();
            final String localPath  = getLocalPath(jettyRequest);

            // Find a resource that can handle the URI path and request method.
            Resource resource = null;
            try {
                Method requestMethod = Method.valueOf(jettyRequest.getMethod());
                List<String> pathArguments = new ArrayList<>();
                resource = ResourceFactory.newResource(
                        requestMethod, localPath, pathArguments);

                if (resource == null) {
                    throw new ResourceException(Status.NOT_FOUND,
                            "No resource at path: " + publicURI.getPath());
                }

                is.galia.resource.Request request = new JettyRequestWrapper(jettyRequest);
                request.setPathArguments(pathArguments);
                resource.setRequest(request);
                is.galia.resource.Response response = new JettyResponseWrapper(jettyResponse);
                resource.setResponse(response);
                resource.doInit();

                final Set<Method> supportedMethods = EnumSet.noneOf(Method.class);
                for (Route route : resource.getRoutes()) {
                    supportedMethods.addAll(route.requestMethods());
                }
                // If the request method is HEAD and GET is supported
                if ((Method.HEAD.equals(requestMethod) && supportedMethods.contains(Method.GET)) ||
                        // or if the request method is OPTIONS
                        Method.OPTIONS.equals(requestMethod) ||
                        // or if the request method is supported
                        supportedMethods.contains(requestMethod)) {
                    switch (jettyRequest.getMethod()) {
                        case "DELETE"  -> resource.doDELETE();
                        case "GET"     -> resource.doGET();
                        case "HEAD"    -> resource.doHEAD();
                        case "OPTIONS" -> resource.doOPTIONS();
                        case "PATCH"   -> resource.doPATCH();
                        case "POST"    -> resource.doPOST();
                        case "PUT"     -> resource.doPUT();
                        default ->     throw new ResourceException(Status.METHOD_NOT_ALLOWED);
                    }
                } else {
                    throw new ResourceException(Status.METHOD_NOT_ALLOWED);
                }
                callback.succeeded();
            } catch (Throwable t) {
                handleError(jettyRequest, jettyResponse, t, callback);
            } finally {
                if (resource != null) {
                    resource.destroy();
                }
                LOGGER.debug("Responded to {} {} with HTTP {} in {}",
                        jettyRequest.getMethod(), publicURI.getPath(),
                        jettyResponse.getStatus(), requestClock);
            }
        });
        return true;
    }

    /**
     * @return Request path with the path from the {@link Key#BASE_URI}
     *         configuration key, or, if that is not set, the value of the
     *         {@code X-Forwarded-BasePath} header, stripped from its start.
     */
    private static String getLocalPath(Request jettyRequest) {
        String publicBasePath = "";
        // If base_uri is set in the configuration, use that.
        String baseURI = Configuration.forApplication().getString(Key.BASE_URI, "");
        if (!baseURI.isEmpty()) {
            Reference baseRef = new Reference(baseURI);
            publicBasePath    = baseRef.getPath();
        } else {
            // If X-Forwarded-BasePath is set, use that.
            String forwardedBasePath =
                    jettyRequest.getHeaders().get("X-Forwarded-BasePath");
            if (forwardedBasePath != null) {
                publicBasePath = forwardedBasePath;
            }
        }
        return jettyRequest.getHttpURI().getPath()
                .replaceAll("^" + publicBasePath, "");
    }

    private void handleError(Request jettyRequest,
                             Response jettyResponse,
                             Throwable t,
                             Callback callback) {
        // Try to use an ErrorResource, which will set an appropriate status
        // depending on the type of Throwable, and also try to negotiate a
        // client-friendly representation such as an HTML template. This might
        // not work, in which case we will have to fall back.
        ErrorResource resource = new ErrorResource(t);
        try {
            // The response status and headers will be set by ErrorResource.
            resource.setRequest(new JettyRequestWrapper(jettyRequest));
            resource.setResponse(new JettyResponseWrapper(jettyResponse));
            resource.doInit();
            resource.doGET();
        } catch (IllegalClientArgumentException e) {
            handleError(jettyResponse, e, 400, callback);
        } catch (Throwable t2) {
            handleError(jettyResponse, t2, 500, callback);
        } finally {
            callback.failed(t);
            resource.destroy();
        }
    }

    private void handleError(Response jettyResponse,
                             Throwable t,
                             int status,
                             Callback callback) {
        jettyResponse.setStatus(status);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(os)) {
            writer.print("Unrecoverable error in " +
                    RequestHandler.class.getSimpleName());
            if (isPrintingStackTraces()) {
                writer.println(":");
                writer.println("");
                t.printStackTrace(writer);
            }
            writer.flush();
            ByteBuffer buffer = ByteBuffer.wrap(os.toByteArray());
            jettyResponse.write(true, buffer, callback);
        } catch (IOException e) {
            LOGGER.error("handleError(): {}", e.getMessage(), e);
        }
    }

    private boolean isPrintingStackTraces() {
        Configuration config = Configuration.forApplication();
        return config.getBoolean(Key.PRINT_STACK_TRACE_ON_ERROR_PAGES, false);
    }

}
