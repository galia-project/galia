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

package is.galia.codec.iptc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Arrays;

public class DataSetDeserializer extends JsonDeserializer<DataSet> {

    @Override
    public DataSet deserialize(final JsonParser parser,
                               final DeserializationContext deserializationContext) throws IOException {
        final JsonNode rootNode = parser.getCodec().readTree(parser);
        final int record        = rootNode.get("record").intValue();
        final int tagNum        = rootNode.get("tag").intValue();
        final byte[] dataField  = rootNode.get("dataField").binaryValue();

        final Tag tag = Arrays.stream(Tag.values())
                .filter(t -> t.getRecord().getRecordNum() == record &&
                        t.getDataSetNum() == tagNum)
                .findFirst()
                .orElse(null);
        return new DataSet(tag, dataField);
    }

}