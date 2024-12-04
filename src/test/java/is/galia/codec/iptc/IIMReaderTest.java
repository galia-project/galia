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

import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IIMReaderTest extends BaseTest {

    private IIMReader instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new IIMReader();
    }

    @Test
    void readWithValidBytes() throws Exception {
        byte[] iptc = Files.readAllBytes(TestUtils.getSampleImage("iptc/iptc.bin"));
        instance.setSource(iptc);

        List<DataSet> expected = List.of(
                new DataSet(Tag.CATEGORY, "Supl. Category2".getBytes()),
                new DataSet(Tag.CATEGORY, "Supl. Category1".getBytes()),
                new DataSet(Tag.CATEGORY, "Cat".getBytes()),
                new DataSet(Tag.COPYRIGHT_NOTICE, "Copyright".getBytes()),
                new DataSet(Tag.SPECIAL_INSTRUCTIONS, "Special Instr.".getBytes()),
                new DataSet(Tag.HEADLINE, "Headline".getBytes()),
                new DataSet(Tag.WRITER_EDITOR, "CaptionWriter".getBytes()),
                new DataSet(Tag.CAPTION_ABSTRACT, "Caption".getBytes()),
                new DataSet(Tag.ORIGINAL_TRANSMISSION_REFERENCE, "Transmission".getBytes()),
                new DataSet(Tag.COUNTRY_PRIMARY_LOCATION_NAME, "Country".getBytes()),
                new DataSet(Tag.PROVINCE_STATE, "State".getBytes()),
                new DataSet(Tag.CITY, "City".getBytes()),
                new DataSet(Tag.DATE_CREATED, new byte[] {
                        0x32, 0x30, 0x30, 0x30, 0x30, 0x31, 0x30, 0x31 }),
                new DataSet(Tag.OBJECT_NAME, "ObjectName".getBytes()),
                new DataSet(Tag.SOURCE, "Source".getBytes()),
                new DataSet(Tag.CREDIT, "Credits".getBytes()),
                new DataSet(Tag.BYLINE_TITLE, "BylineTitle".getBytes()),
                new DataSet(Tag.BYLINE, "Byline".getBytes())
        );
        List<DataSet> actual = instance.read();

        assertEquals(expected, actual);
    }

    @Test
    void readWithInvalidBytes() {
        byte[] iptc = new byte[] { 0x02, 0x05, 0x09 }; // random bytes
        instance.setSource(iptc);
        assertEquals(0, instance.read().size());
    }

}
