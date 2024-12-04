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

package is.galia.codec.xmp;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XMPUtilsTest extends BaseTest {

    /* trimXMP() */

    @Test
    void trimXMPWithTrimmableXMP() {
        String xmp = "<?xpacket id=\"cats\"?>" +
                "<x:xmpmeta bla=\"dogs\">" +
                "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>" +
                "</x:xmpmeta>";
        String result = XMPUtils.trimXMP(xmp);
        assertTrue(result.startsWith("<rdf:RDF"));
        assertTrue(result.endsWith("</rdf:RDF>"));
    }

    @Test
    void trimXMPWithNonTrimmableXMP() {
        String xmp = "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>";
        String result = XMPUtils.trimXMP(xmp);
        assertSame(xmp, result);
    }

    @Test
    void trimXMPWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> XMPUtils.trimXMP(null));
    }

    /* wrapInXMPMetaTag() */

    @Test
    void wrapInXMPMetaTag() {
        final String xmp = "<rdf:RDF></rdf:RDF>";
        String actual    = XMPUtils.wrapInXMPMetaTag(xmp);
        assertTrue(actual.startsWith("<x:xmpmeta"));
        assertTrue(actual.endsWith("</x:xmpmeta>"));
    }

    /* wrapInXPacket() */

    @Test
    void wrapInXPacket() {
        final String xmp = "<rdf:RDF></rdf:RDF>";
        String actual    = XMPUtils.wrapInXPacket(xmp);
        assertTrue(actual.startsWith("<?xpacket"));
        assertTrue(actual.endsWith("<?xpacket end=\"r\"?>"));
    }

}