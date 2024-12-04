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

import is.galia.Application;
import is.galia.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Handles requests for static files.</p>
 *
 * <p>When {@link Application#ENVIRONMENT_VM_ARGUMENT} is set to {@link
 * Application#DEVELOPMENT_ENVIRONMENT}, the files are read from the
 * filesystem. Otherwise they are loaded by the class loader.</p>
 */
public class FileResource extends AbstractResource implements Resource {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FileResource.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<Route> getRoutes() {
        return Set.of(new Route(
                Set.of(Method.GET),
                Set.of(Pattern.compile("^/static/"))));
    }

    @Override
    public void doGET() throws Exception {
        // If we are in development mode, stream the file from disk. This
        // enables the browser to see changes upon reload.
        if (Application.isDeveloping()) {
            streamFile();
        } else {
            streamResource();
        }
    }

    private void streamFile() throws IOException {
        String pathStr = getLocalBasePath()
                .replaceAll("^/static", "src/main/resources/webapp");
        Path path = Paths.get(pathStr);
        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path)) {
                getResponse().setStatus(200);
                getResponse().setHeader("Cache-Control", "no-store, no-cache");
                getResponse().setHeader("Content-Type", getContentType(pathStr));
                try (OutputStream os = getResponse().openBodyStream()) {
                    is.transferTo(os);
                }
            }
        } else {
            getResponse().setStatus(404);
        }
    }

    private void streamResource() throws IOException {
        String pathStr = getLocalBasePath().replaceAll("^/static", "/webapp");
        final URL resURL = getClass().getResource(pathStr);
        if (resURL != null) {
            try (InputStream is = new BufferedInputStream(resURL.openStream())) {
                getResponse().setStatus(200);
                getResponse().setHeader("Cache-Control", "public, max-age=2592000");
                getResponse().setHeader("Content-Type", getContentType(pathStr));
                try (OutputStream os = getResponse().openBodyStream()) {
                    is.transferTo(os);
                }
            }
        } else {
            getResponse().setStatus(404);
        }
    }

    private static String getContentType(String path) {
        int extIdx = path.lastIndexOf(".");
        if (extIdx > 0) {
            String ext = path.substring(extIdx);
            // This statement must include a case for every static file type
            // served by the app.
            switch (ext) {
                case ".css":
                    return "text/css";
                case ".js":
                    return "application/javascript";
                case ".png":
                    return "image/png";
                case ".svg":
                    return "image/svg+xml";
            }
        }
        return "application/octet-stream";
    }

}
