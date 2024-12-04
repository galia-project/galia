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

package is.galia.operation.redaction;

import is.galia.image.Identifier;
import is.galia.image.Region;
import is.galia.delegate.Delegate;
import is.galia.operation.Color;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RedactionServiceTest extends BaseTest {

    private RedactionService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new RedactionService();
    }

    @Test
    void redactionsForWithRedactions() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("redacted"));

        List<Redaction> redactions = instance.redactionsFor(delegate);
        assertEquals(1, redactions.size());
        Redaction redaction = redactions.getFirst();
        assertEquals(new Region(0, 10, 50, 70), redaction.getRegion());
        assertEquals(Color.BLACK, redaction.getColor());
    }

    @Test
    void redactionsForWithNoRedactions() throws Exception {
        Delegate delegate = TestUtils.newDelegate();
        delegate.getRequestContext().setIdentifier(new Identifier("bogus"));

        List<Redaction> redactions = instance.redactionsFor(delegate);
        assertTrue(redactions.isEmpty());
    }

}
