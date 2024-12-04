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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DirectoryDeserializer extends JsonDeserializer<Directory> {

    private static final String DATA_TYPE_KEY = "dataType";
    private static final String TAG_ID_KEY    = "id";

    @Override
    public Directory deserialize(final JsonParser parser,
                                 final DeserializationContext deserializationContext) throws IOException {
        final JsonNode rootNode = parser.getCodec().readTree(parser);
        return deserialize(rootNode, parser);
    }

    /**
     * Recursively deserializes a {@link Directory} and all of its sub-{@link
     * Directory}s.
     *
     * @param dirNode Directory node (maybe but not necessarily the root
     *                directory).
     * @param parser  Parser.
     */
    private Directory deserialize(final JsonNode dirNode,
                                  final JsonParser parser) throws IOException {
        Directory dir;

        // Find the parent tag.
        int parentTag = 0;
        Iterator<Map.Entry<String,JsonNode>> dirEntries = dirNode.fields();
        while (dirEntries.hasNext()) {
            Map.Entry<String, JsonNode> dirEntry = dirEntries.next();
            JsonNode rootValue                   = dirEntry.getValue();
            if ("parentTag".equals(dirEntry.getKey())) {
                parentTag = rootValue.intValue();
                break;
            }
        }

        final TagSet tagSet = TagSet.forIFDPointerTag(parentTag);
        if (tagSet == null) {
            throw new JsonParseException(parser,
                    "Unrecognized tag set: " + parentTag);
        }

        dir = new Directory(tagSet);
        dirEntries = dirNode.fields();
        while (dirEntries.hasNext()) {
            Map.Entry<String, JsonNode> rootEntry = dirEntries.next();
            if (!"fields".equals(rootEntry.getKey())) {
                continue;
            }

            JsonNode rootValue = rootEntry.getValue();
            Iterator<JsonNode> fieldsIter = rootValue.elements();
            while (fieldsIter.hasNext()) {
                final JsonNode fieldNode = fieldsIter.next();
                int tagID = -1, dataTypeID = -1;
                Iterator<Map.Entry<String, JsonNode>> keysIter = fieldNode.fields();
                while (keysIter.hasNext()) {
                    Map.Entry<String, JsonNode> keyEntry = keysIter.next();
                    switch (keyEntry.getKey()) {
                        case TAG_ID_KEY:
                            tagID = keyEntry.getValue().intValue();
                            break;
                        case DATA_TYPE_KEY:
                            dataTypeID = keyEntry.getValue().intValue();
                            break;
                    }
                }

                if (tagID == -1) {
                    throw new JsonParseException(parser,
                            "Field is missing a " + TAG_ID_KEY + " key.");
                } else if (dataTypeID == -1) {
                    throw new JsonParseException(parser,
                            "Field is missing a " + DATA_TYPE_KEY + " key.");
                }

                Tag tag           = tagSet.getTag(tagID);
                DataType dataType = DataType.forValue(dataTypeID);
                if (tag == null) {
                    throw new JsonParseException(parser,
                            "Unsupported tag: " + tagID);
                } else if (dataType == null) {
                    throw new JsonParseException(parser,
                            "Unsupported data type: " + dataTypeID);
                }

                Field field = Field.forTag(tag, dataType);
                JsonNode jsonFirstValue = null;

                keysIter = fieldNode.fields();
                while (keysIter.hasNext()) {
                    Map.Entry<String, JsonNode> keyEntry = keysIter.next();
                    if ("values".equals(keyEntry.getKey())) {
                        JsonNode jsonArray = keyEntry.getValue();
                        jsonFirstValue     = jsonArray.get(0);
                        switch (tag.id()) {
                            // Components Configuration and Scene Type are numbers.
                            case 37121, 41729 -> {
                                byte[] bytes = new byte[]{(byte) jsonFirstValue.intValue()};
                                ((ByteArrayField) field).setValue(bytes);
                            }
                            // EXIF Version and Flashpix Version are strings.
                            case 36864, 40960 -> {
                                byte[] bytes = jsonFirstValue.textValue().getBytes(StandardCharsets.US_ASCII);
                                ((ByteArrayField) field).setValue(bytes);
                            }
                            default -> {
                                switch (dataType) {
                                    case BYTE, SBYTE, UNDEFINED ->
                                            ((ByteArrayField) field).setValue(jsonFirstValue.binaryValue());
                                    case ASCII, UTF8 ->
                                            ((StringField) field).setValue(jsonFirstValue.textValue());
                                    default ->
                                            toJavaValues(dataType, jsonArray)
                                                    .forEach(v -> ((MultiValueField) field).addValue(v));
                                }
                            }
                        }
                    }
                }

                if (field.getValues().isEmpty()) {
                    throw new JsonParseException(parser,
                            "Field is missing a value.");
                }

                if (tag.isIFDPointer()) {
                    Directory subDir = deserialize(jsonFirstValue, parser);
                    dir.add(tag, subDir);
                } else {
                    dir.add(field);
                }
            }
        }
        return dir;
    }

    private List<Object> toJavaValues(DataType dataType,
                                      JsonNode valuesNode) throws IOException {
        final List<Object> values     = new ArrayList<>();
        final JsonNode firstValueNode = valuesNode.get(0);
        switch (dataType) {
            case BYTE, SBYTE, UNDEFINED -> {
                // IntNode has been seen in the wild; the other conditions may
                // or may not be needed here, but they can't hurt.
                if (firstValueNode instanceof IntNode) {
                    values.add(firstValueNode.longValue());
                } else if (firstValueNode instanceof LongNode) {
                    values.add(firstValueNode.longValue());
                } else if (firstValueNode instanceof FloatNode) {
                    values.add(firstValueNode.floatValue());
                } else if (firstValueNode instanceof DoubleNode) {
                    values.add(firstValueNode.doubleValue());
                } else if (firstValueNode instanceof BooleanNode) {
                    values.add(firstValueNode.booleanValue());
                } else if (firstValueNode instanceof TextNode) {
                    values.add(firstValueNode.textValue());
                } else {
                    values.add(firstValueNode.binaryValue());
                }
            }
            case ASCII, UTF8 -> values.add(firstValueNode.asText());
            case SHORT       -> values.add(firstValueNode.intValue());
            case LONG, SLONG -> values.add(firstValueNode.longValue());
            case RATIONAL    -> {
                Iterator<JsonNode> it = valuesNode.elements();
                while (it.hasNext()) {
                    JsonNode value = it.next();
                    values.add(List.of(
                            value.get(0).longValue(),
                            value.get(1).longValue()));
                }
            }
            case SSHORT    -> values.add(firstValueNode.shortValue());
            case SRATIONAL -> {
                Iterator<JsonNode> it = valuesNode.elements();
                while (it.hasNext()) {
                    JsonNode value = it.next();
                    values.add(List.of(
                            value.get(0).intValue(),
                            value.get(1).intValue()));
                }
            }
            case FLOAT     -> values.add(firstValueNode.floatValue());
            case DOUBLE    -> values.add(firstValueNode.doubleValue());
            default        -> throw new IllegalArgumentException(
                    "Unsupported data type: " + dataType);
        }
        return values;
    }

}