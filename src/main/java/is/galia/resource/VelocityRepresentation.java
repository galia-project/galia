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
import is.galia.util.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation for Velocity templates.
 */
public class VelocityRepresentation implements Representation {

    private final String templateName;
    private final Map<String, Object> escapableTemplateVars    = new HashMap<>();
    private final Map<String, Object> nonEscapableTemplateVars = new HashMap<>();

    static {
        if (Application.isDeveloping()) {
            // FileResourceLoader can detect changes so that we don't have to
            // restart the app all the time.
            Velocity.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
            Velocity.setProperty("resource.loader.file.class",
                    FileResourceLoader.class.getName());
            Velocity.setProperty("resource.loader.file.path",
                    Paths.get(".", "src", "main", "resources").toAbsolutePath().toString());
            Velocity.setProperty("resource.loader.file.cache", false);
            Velocity.setProperty("resource.loader.file.modification_check_interval", 2);
        } else {
            Velocity.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
            Velocity.setProperty("resource.loader.classpath.class",
                    ClasspathResourceLoader.class.getName());
            Velocity.setProperty("resource.loader.class.cache", true);
        }
        // http://velocity.apache.org/engine/2.0/developer-guide.html#space-gobbling
        Velocity.setProperty(RuntimeConstants.SPACE_GOBBLING, "lines");
        Velocity.init();

    }

    /**
     * @param templateName Template pathname, with leading slash.
     */
    public VelocityRepresentation(String templateName) {
        this.templateName = templateName;
    }

    /**
     * @param templateName Template pathname, with leading slash.
     * @param templateVars Template variables, which will all be escaped.
     */
    public VelocityRepresentation(String templateName,
                                  Map<String,Object> templateVars) {
        this(templateName);
        this.escapableTemplateVars.putAll(templateVars);
    }

    /**
     * @param templateName             Template pathname, with leading slash.
     * @param escapableTemplateVars    Template variables, which will be
     *                                 escaped.
     * @param nonEscapableTemplateVars Template variables, which will not be
     *                                 escaped.
     */
    public VelocityRepresentation(String templateName,
                                  Map<String,Object> escapableTemplateVars,
                                  Map<String,Object> nonEscapableTemplateVars) {
        this(templateName);
        this.escapableTemplateVars.putAll(escapableTemplateVars);
        this.nonEscapableTemplateVars.putAll(nonEscapableTemplateVars);
    }

    private static Object escape(Object var) {
        if (var instanceof String stringVar) {
            return StringUtils.escapeHTML(stringVar);
        }
        return var;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        // Copy template variables into the VelocityContext
        VelocityContext context = new VelocityContext();
        for (Map.Entry<String,Object> entry : escapableTemplateVars.entrySet()) {
            context.put(entry.getKey(), escape(entry.getValue()));
        }
        for (Map.Entry<String,Object> entry : nonEscapableTemplateVars.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
        Template template = Velocity.getTemplate(templateName);
        try (OutputStreamWriter writer =
                     new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            template.merge(context, writer);
        }
    }

}
