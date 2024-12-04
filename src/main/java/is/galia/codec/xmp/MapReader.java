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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Extracts label-value pairs from an XMP model, making them available in a
 * {@link Map}.
 */
public final class MapReader {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MapReader.class);

    private static final Map<String,String> NAMESPACES = Map.ofEntries(
            Map.entry("http://ns.adobe.com/camera-raw-settings/1.0/", "crs"),
            Map.entry("http://purl.org/dc/elements/1.1/", "dc"),
            Map.entry("http://purl.org/dc/terms/", "dcterms"),
            Map.entry("http://ns.adobe.com/exif/1.0/", "exif"),
            Map.entry("http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/", "Iptc4xmpCore"),
            Map.entry("http://ns.adobe.com/iX/1.0/", "iX"),
            Map.entry("http://ns.adobe.com/pdf/1.3/", "pdf"),
            Map.entry("http://ns.adobe.com/photoshop/1.0/", "photoshop"),
            Map.entry("http://ns.adobe.com/tiff/1.0/", "tiff"),
            Map.entry("http://ns.adobe.com/xap/1.0/", "xmp"),
            Map.entry("http://ns.adobe.com/xap/1.0/bj/", "xmpBJ"),
            Map.entry("http://ns.adobe.com/xmp/1.0/DynamicMedia/", "xmpDM"),
            Map.entry("http://ns.adobe.com/xmp/identifier/qual/1.0/", "xmpidq"),
            Map.entry("http://ns.adobe.com/xap/1.0/mm/", "xmpMM"),
            Map.entry("http://ns.adobe.com/xap/1.0/rights/", "xmpRights"),
            Map.entry("http://ns.adobe.com/xap/1.0/t/pg/", "xmpTPg"));

    private final Model model;
    private final Map<String,Object> elements = new TreeMap<>();
    private boolean hasReadElements;

    /**
     * @param xmp XMP string. {@code <rdf:RDF>} must be the root element.
     * @see XMPUtils#trimXMP
     */
    public MapReader(String xmp) throws IOException {
        Objects.requireNonNull(xmp);
        try {
            this.model = XMPUtils.readModel(xmp);
        } catch (RiotException e) {
            // The XMP string may be invalid. Nothing we can do.
            throw new IOException(e);
        }
    }

    /**
     * @param model XMP model, already initialized.
     */
    public MapReader(Model model) {
        Objects.requireNonNull(model);
        this.model = model;
    }

    public Map<String,Object> readElements() throws IOException {
        if (!hasReadElements) {
            StmtIterator it = model.listStatements();
            while (it.hasNext()) {
                Statement stmt = it.next();
                //System.out.println(stmt.getSubject() + " " + stmt.getSubject().isAnon());
                //System.out.println("  " + stmt.getPredicate());
                //System.out.println("    " + stmt.getObject() + " " + stmt.getObject().isLiteral());
                //System.out.println("---------------------------");
                if (!stmt.getSubject().isAnon()) {
                    recurse(stmt);
                }
            }
            LOGGER.trace("readElements(): read {} elements", elements.size());
            hasReadElements = true;
        }
        return Collections.unmodifiableMap(elements);
    }

    private void recurse(Statement stmt) {
        recurse(stmt, null);
    }

    private void recurse(Statement stmt, String predicateOverride) {
        String predicate = stmt.getPredicate().toString();
        if (stmt.getObject().isLiteral()) {
            addElement(label(predicateOverride != null ? predicateOverride : predicate),
                    stmt.getObject().asLiteral().getValue());
        } else {
            StmtIterator it = model.listStatements(
                    stmt.getObject().asResource(), null, (RDFNode) null);
            while (it.hasNext()) {
                Statement substmt = it.next();
                predicateOverride = null;
                if (substmt.getPredicate().toString().matches("(.*)#_\\d+\\b")) {
                    predicateOverride = predicate;
                }
                recurse(substmt, predicateOverride);
            }
        }
    }

    private void addElement(String label, Object value) {
        if (elements.containsKey(label)) {
            if (elements.get(label) instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> valueList = (List<Object>) elements.get(label);
                valueList.add(value);
            } else {
                List<Object> valueList = new ArrayList<>();
                valueList.add(elements.get(label));
                valueList.add(value);
                elements.put(label, valueList);
            }
        } else {
            elements.put(label, value);
        }
    }

    private String label(String uri) {
        for (Map.Entry<String,String> entry : NAMESPACES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                String prefix  = entry.getValue();
                String[] parts = uri.split("/");
                return prefix + ":" + parts[parts.length - 1];
            }
        }
        return uri;
    }

}