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

package is.galia.codec.tiff;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <p>Serializes a {@link Directory} as JSON.</p>
 */
public class DirectorySerializer extends JsonSerializer<Directory> {

    @Override
    public void serialize(Directory directory,
                          JsonGenerator generator,
                          SerializerProvider serializerProvider) throws IOException {
        generator.writeStartObject();

        final int parentTag = directory.getTagSet().getIFDPointerTag();
        if (parentTag > 0) {
            generator.writeFieldName("parentTag");
            generator.writeNumber(parentTag);
        }
        generator.writeFieldName("fields");
        generator.writeStartArray();

        for (Field field : directory.getFields()) {
            generator.writeStartObject();
            generator.writeNumberField("id", field.getTag().id());
            generator.writeNumberField("dataType", field.getDataType().getValue());
            generator.writeFieldName("values");
            generator.writeStartArray();
            for (Object value : field.getValues()) {
                writeValue(generator, field.getTag(), value, serializerProvider);
            }
            generator.writeEndArray();
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    private void writeValue(JsonGenerator generator,
                            Tag tag,
                            Object value,
                            SerializerProvider serializerProvider) throws IOException {
        switch (tag.id()) {
            // Components Configuration and Scene Type are serialized as
            // numbers.
            case 37121, 41729 -> {
                int number = ((byte[]) value)[0];
                generator.writeNumber(number);
                return;
            }
            case 36864, 40960 -> {
                // EXIF Version and Flashpix Version are serialized as strings.
                String str = new String((byte[]) value, StandardCharsets.US_ASCII);
                generator.writeString(str);
                return;
            }
        }
        switch (value) {
            case Directory dir -> serialize(dir, generator, serializerProvider);
            case byte[] bytes  -> generator.writeBinary(bytes);
            case null, default -> generator.writeObject(value);
        }
    }

}