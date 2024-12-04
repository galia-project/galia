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

import is.galia.Application;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RIOT;

import java.io.StringReader;

public final class XMPUtils {

    private static final String XMP_TOOLKIT = Application.getName() + " " +
            Application.getVersion();

    static {
        RIOT.init();
    }

    public static Model readModel(String rdfXML) {
        String base = "";
        if (rdfXML.contains("rdf:about=''") || rdfXML.contains("rdf:about=\"\"")) {
            base = "http://example.org";
        }
        Model model = ModelFactory.createDefaultModel();
        try (StringReader reader = new StringReader(rdfXML)) {
            model.read(reader, base, "RDF/XML");
        }
        return model;
    }

    /**
     * Strips any enclosing tags or other content around the {@code rdf:RDF}
     * element within an RDF/XML XMP string.
     */
    public static String trimXMP(String xmp) {
        final int start = xmp.indexOf("<rdf:RDF");
        final int end   = xmp.indexOf("</rdf:RDF");
        if (start > -1 && end > -1) {
            xmp = xmp.substring(start, end + 10);
        }
        return xmp;
    }

    /**
     * Wraps an XMP string in an {@code xpacket} PI with magic trailer.
     *
     * @param xmp XMP string with an {@code rdf:RDF} root element.
     * @return    Encapsulated XMP data packet.
     */
    public static String wrapInXPacket(String xmp) {
        final StringBuilder b = new StringBuilder();
        b.append("<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n");
        b.append(wrapInXMPMetaTag(xmp));
        // Append the magic trailer
        b.append(" ".repeat(2048));
        b.append("<?xpacket end=\"r\"?>");
        return b.toString();
    }

    /**
     * Returns an XMP string encapsulated in an {@code x:xmpmeta} element,
     * which is itself encapsulated in an {@code xpacket} PI.
     *
     * @param xmp XMP string with an {@code rdf:RDF} root element.
     * @return    Encapsulated XMP data packet.
     */
    public static String wrapInXMPMetaTag(String xmp) {
        return "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"" + XMP_TOOLKIT + "\">" +
                xmp +
                "</x:xmpmeta>";
    }

}
