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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Representation for serializing objects to JSON strings.
 */
public class JacksonRepresentation implements Representation {

    private static final ObjectWriter DEFAULT_WRITER;

    private final Object toWrite;

    static {
        ObjectMapper mapper = new ObjectMapper();
        // Make ObjectMapper aware of JDK8 date/time objects
        // See: https://github.com/FasterXML/jackson-modules-java8
        mapper.registerModule(new JavaTimeModule());
        // Serialize dates as ISO-8601 strings rather than timestamps.
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Add a config override to omit keys with empty or null values.
        //
        // The IIIF Image API 2.1 spec (sec. 5.3) says,
        // "If any of formats, qualities, or supports have no additional values
        // beyond those specified in the referenced compliance level, then
        // the property should be omitted from the response rather than being
        // present with an empty list."
        mapper.configOverride(Object.class).setInclude(
                JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
        DEFAULT_WRITER = mapper.writer();
    }

    public JacksonRepresentation(Object toWrite) {
        this.toWrite = toWrite;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        DEFAULT_WRITER.writeValue(outputStream, toWrite);
    }

    public void write(OutputStream outputStream,
                      Map<SerializationFeature,Boolean> serializationFeatures) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        serializationFeatures.forEach(mapper::configure);
        mapper.configOverride(Object.class).setInclude(
                JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, null));
        mapper.writer().writeValue(outputStream, toWrite);
    }

}
