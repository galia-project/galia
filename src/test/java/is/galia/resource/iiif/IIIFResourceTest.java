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

package is.galia.resource.iiif;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.http.Status;
import is.galia.image.Size;
import is.galia.image.Identifier;
import is.galia.operation.OperationList;
import is.galia.operation.Scale;
import is.galia.operation.ScaleByPercent;
import is.galia.resource.JettyRequestWrapper;
import is.galia.resource.JettyResponseWrapper;
import is.galia.resource.MockJettyRequest;
import is.galia.resource.MockJettyResponse;
import is.galia.resource.Route;
import is.galia.resource.ScaleRestrictedException;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IIIFResourceTest extends BaseTest {

    private IIIFResource instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new IIIFResource() {
            @Override
            protected Logger getLogger() {
                return LoggerFactory.getLogger(IIIFResourceTest.class);
            }
            @Override
            public Set<Route> getRoutes() {
                return Set.of();
            }
        };
        instance.setRequest(new JettyRequestWrapper(
                new MockJettyRequest()));
        instance.setResponse(new JettyResponseWrapper(
                new MockJettyResponse()));
    }

    /* getIdentifier() */

    @Test
    void getIdentifierWithNoPathArguments() {
        assertNull(instance.getIdentifier());
    }

    @Test
    void getIdentifier() {
        instance.getRequest().setPathArguments(List.of("cats"));
        assertEquals(new Identifier("cats"), instance.getIdentifier());
    }

    /* getIdentifierPathComponent() */

    @Test
    void getIdentifierPathComponentWithNoPathArguments() {
        assertNull(instance.getIdentifierPathComponent());
    }

    @Test
    void getIdentifierPathComponent() {
        instance.getRequest().setPathArguments(List.of("cats"));
        assertEquals("cats", instance.getIdentifierPathComponent());
    }

    /* getPublicIdentifier() */

    @Test
    void getPublicIdentifierWithNoPathArguments() {
        assertNull(instance.getPublicIdentifier());
    }

    @Test
    void getPublicIdentifier() {
        instance.getRequest().setPathArguments(List.of("cats"));
        assertEquals("cats", instance.getPublicIdentifier());
    }

    /* getPublicReference(MetaIdentifier) */

    @Test
    void getPublicReference() {
        // TODO: write this
    }

    /* authorize() */

    @Test
    void authorize() {
        // TODO: write this
    }

    /* authorizeBeforeAccess() */

    @Test
    void authorizeBeforeAccess() {
        // TODO: write this
    }

    /* setLastModifiedHeader() */

    @Test
    void setLastModifiedHeader() {
        instance.setLastModifiedHeader(Instant.EPOCH);
        assertEquals("Thu, 1 Jan 1970 00:00:00 GMT",
                instance.getResponse().getHeaders().getFirstValue("Last-Modified"));
    }

    /* constrainSizeToMaxPixels() */

    @Test
    void constrainSizeToMaxPixelsWithMaxPixelsNotSet() {
        OperationList opList = new OperationList();
        instance.constrainSizeToMaxPixels(new Size(500, 500), opList);
        assertFalse(opList.iterator().hasNext());
    }

    @Test
    void constrainSizeToMaxPixelsWithNoConstraintNecessary() {
        Configuration.forApplication().setProperty(Key.MAX_PIXELS, 10000);
        OperationList opList = new OperationList();
        instance.constrainSizeToMaxPixels(new Size(50, 10), opList);
        assertFalse(opList.iterator().hasNext());
    }

    @Test
    void constrainSizeToMaxPixels() {
        Configuration.forApplication().setProperty(Key.MAX_PIXELS, 10000);
        OperationList opList = new OperationList();
        instance.constrainSizeToMaxPixels(new Size(500, 500), opList);
        assertNotNull(opList.getFirst(Scale.class));
    }

    /* validateScale() */

    @Test
    void validateScaleWithInvalidScale() {
        instance.getRequest().setPathArguments(List.of("identifier;1;1:2"));
        ScaleRestrictedException e = assertThrows(
                ScaleRestrictedException.class,
                () -> instance.validateScale(new Size(1000, 1000),
                        new ScaleByPercent(2.5),
                        Status.BAD_REQUEST));
        assertEquals(Status.BAD_REQUEST, e.getStatus());
    }

    @Test
    void validateScaleWithScaleConstraint() throws Exception {
        instance.getRequest().setPathArguments(List.of("identifier;1;1:2"));
        instance.validateScale(new Size(1000, 1000),
                null,
                Status.BAD_REQUEST);
    }

    @Test
    void validateScaleWithScaleOperation() throws Exception {
        instance.getRequest().setPathArguments(List.of("identifier"));
        instance.validateScale(new Size(1000, 1000),
                new ScaleByPercent(0.5),
                Status.BAD_REQUEST);
    }

    @Test
    void validateScaleWithScaleConstraintAndScaleOperation() throws Exception {
        instance.getRequest().setPathArguments(List.of("identifier;1;1:2"));
        instance.validateScale(new Size(1000, 1000),
                new ScaleByPercent(),
                Status.BAD_REQUEST);
    }

}
