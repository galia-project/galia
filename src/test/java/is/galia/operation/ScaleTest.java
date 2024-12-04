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

package is.galia.operation;

import is.galia.image.Size;
import is.galia.image.ScaleConstraint;
import is.galia.processor.resample.ResampleFilters;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

abstract class ScaleTest extends BaseTest {

    @Nested
    class FilterTest extends BaseTest {

        @Test
        void testToResampleFilter() {
            assertSame(ResampleFilters.getBellFilter(),
                    Scale.Filter.BELL.toResampleFilter());
            assertSame(ResampleFilters.getBiCubicFilter(),
                    Scale.Filter.BICUBIC.toResampleFilter());
            assertSame(ResampleFilters.getBoxFilter(),
                    Scale.Filter.BOX.toResampleFilter());
            assertSame(ResampleFilters.getBSplineFilter(),
                    Scale.Filter.BSPLINE.toResampleFilter());
            assertSame(ResampleFilters.getHermiteFilter(),
                    Scale.Filter.HERMITE.toResampleFilter());
            assertSame(ResampleFilters.getLanczos3Filter(),
                    Scale.Filter.LANCZOS3.toResampleFilter());
            assertSame(ResampleFilters.getMagicKernelSharp2013Filter(),
                    Scale.Filter.MKS2013.toResampleFilter());
            assertSame(ResampleFilters.getMitchellFilter(),
                    Scale.Filter.MITCHELL.toResampleFilter());
            assertSame(ResampleFilters.getTriangleFilter(),
                    Scale.Filter.TRIANGLE.toResampleFilter());
        }

    }

    static final double DELTA = 0.00000001;

    abstract Scale newInstance();

    @Test
    void testSetFilterWhenFrozenThrowsException() {
        Scale instance = newInstance();
        instance.freeze();
        assertThrows(IllegalStateException.class,
                () -> instance.setFilter(Scale.Filter.LANCZOS3));
    }

    @Test
    void testToMapReturnsUnmodifiableMap() {
        Size fullSize                   = new Size(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);

        Map<String,Object> map = newInstance().toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

}
