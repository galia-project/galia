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

package is.galia.resource.iiif.v2;

import is.galia.image.Info;
import is.galia.image.MutableMetadata;
import is.galia.image.Orientation;
import is.galia.image.ScaleConstraint;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InformationFactoryTest extends BaseTest {

    private InformationFactory instance;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        instance = new InformationFactory();
    }

    private Map<String,Object> invokeNewImageInfo() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder().withSize(1500, 1200).build();
        return instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));
    }

    @Test
    void newImageInfoContext() {
        Map<String,Object> info = invokeNewImageInfo();
        assertEquals("http://iiif.io/api/image/2/context.json",
                info.get("@context"));
    }

    @Test
    void newImageInfoID() {
        Map<String,Object> info = invokeNewImageInfo();
        assertEquals("http://example.org/bla", info.get("@id"));
    }

    @Test
    void newImageInfoProtocol() {
        Map<String,Object> info = invokeNewImageInfo();
        assertEquals("http://iiif.io/api/image", info.get("protocol"));
    }

    @Test
    void newImageInfoWidth() {
        Map<String,Object> info = invokeNewImageInfo();
        assertEquals(1500L, info.get("width"));
    }

    @Test
    void newImageInfoWidthWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .withMetadata(new MutableMetadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        assertEquals(1200L, iiifInfo.get("width"));
    }

    @Test
    void newImageInfoWidthWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1499, 1199) // test rounding
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 2));

        assertEquals(750L, iiifInfo.get("width"));
    }

    @Test
    void newImageInfoHeight() {
        Map<String,Object> info = invokeNewImageInfo();
        assertEquals(1200L, info.get("height"));
    }

    @Test
    void newImageInfoHeightWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .withMetadata(new MutableMetadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        assertEquals(1500L, iiifInfo.get("height"));
    }

    @Test
    void newImageInfoHeightWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1499, 1199) // test rounding
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 2));

        assertEquals(600L, iiifInfo.get("height"));
    }

    @Test
    void newImageInfoSizes() {
        Map<String, Object> iiifInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<Map<String,Long>> sizes =
                (List<Map<String,Long>>) iiifInfo.get("sizes");
        assertEquals(5, sizes.size());
        assertEquals(94, sizes.get(0).get("width"));
        assertEquals(75, sizes.get(0).get("height"));
        assertEquals(188, sizes.get(1).get("width"));
        assertEquals(150, sizes.get(1).get("height"));
        assertEquals(375, sizes.get(2).get("width"));
        assertEquals(300, sizes.get(2).get("height"));
        assertEquals(750, sizes.get(3).get("width"));
        assertEquals(600, sizes.get(3).get("height"));
        assertEquals(1500, sizes.get(4).get("width"));
        assertEquals(1200, sizes.get(4).get("height"));
    }

    @Test
    void newImageInfoSizesMinSize() {
        instance.setMinSize(500);
        Map<String, Object> iiifInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<Map<String,Long>> sizes =
                (List<Map<String,Long>>) iiifInfo.get("sizes");
        assertEquals(2, sizes.size());
        assertEquals(750, sizes.get(0).get("width"));
        assertEquals(600, sizes.get(0).get("height"));
        assertEquals(1500, sizes.get(1).get("width"));
        assertEquals(1200, sizes.get(1).get("height"));
    }

    @Test
    void newImageInfoSizesMaxSize() {
        instance.setMaxPixels(10000);
        Map<String, Object> iiifInfo = invokeNewImageInfo();

        @SuppressWarnings("unchecked")
        List<Map<String,Long>> sizes =
                (List<Map<String,Long>>) iiifInfo.get("sizes");
        assertEquals(1, sizes.size());
        assertEquals(94, sizes.getFirst().get("width"));
        assertEquals(75, sizes.getFirst().get("height"));
    }

    @Test
    void newImageInfoSizesWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .withMetadata(new MutableMetadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Map<String,Long>> sizes =
                (List<Map<String,Long>>) iiifInfo.get("sizes");
        assertEquals(5, sizes.size());
        assertEquals(75, sizes.get(0).get("width"));
        assertEquals(94, sizes.get(0).get("height"));
        assertEquals(150, sizes.get(1).get("width"));
        assertEquals(188, sizes.get(1).get("height"));
        assertEquals(300, sizes.get(2).get("width"));
        assertEquals(375, sizes.get(2).get("height"));
        assertEquals(600, sizes.get(3).get("width"));
        assertEquals(750, sizes.get(3).get("height"));
        assertEquals(1200, sizes.get(4).get("width"));
        assertEquals(1500, sizes.get(4).get("height"));
    }

    @Test
    void newImageInfoSizesWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(1500, 1200)
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 2));

        @SuppressWarnings("unchecked")
        List<Map<String,Long>> sizes =
                (List<Map<String,Long>>) iiifInfo.get("sizes");
        assertEquals(4, sizes.size());
        assertEquals(94, sizes.get(0).get("width"));
        assertEquals(75, sizes.get(0).get("height"));
        assertEquals(188, sizes.get(1).get("width"));
        assertEquals(150, sizes.get(1).get("height"));
        assertEquals(375, sizes.get(2).get("width"));
        assertEquals(300, sizes.get(2).get("height"));
        assertEquals(750, sizes.get(3).get("width"));
        assertEquals(600, sizes.get(3).get("height"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void newImageInfoTilesWithUntiledMonoResolutionImage() {
        Map<String, Object> iiifInfo = invokeNewImageInfo();

        List<Map<String,Object>> tiles =
                (List<Map<String,Object>>) iiifInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(512L, tiles.getFirst().get("width"));
        assertEquals(512L, tiles.getFirst().get("height"));

        List<Integer> scaleFactors =
                (List<Integer>) tiles.getFirst().get("scaleFactors");
        assertEquals(5, scaleFactors.size());
        assertEquals(1, scaleFactors.get(0));
        assertEquals(2, scaleFactors.get(1));
        assertEquals(4, scaleFactors.get(2));
        assertEquals(8, scaleFactors.get(3));
        assertEquals(16, scaleFactors.get(4));
    }

    @SuppressWarnings("unchecked")
    @Test
    void newImageInfoTilesWithUntiledMultiResolutionImage() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder()
                .withSize(3000, 2000)
                .withNumResolutions(3)
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        List<Map<String,Object>> tiles =
                (List<Map<String,Object>>) iiifInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(512L, tiles.getFirst().get("width"));
        assertEquals(512L, tiles.getFirst().get("height"));

        List<Integer> scaleFactors =
                (List<Integer>) tiles.getFirst().get("scaleFactors");
        assertEquals(5, scaleFactors.size());
        assertEquals(1, scaleFactors.get(0));
        assertEquals(2, scaleFactors.get(1));
        assertEquals(4, scaleFactors.get(2));
        assertEquals(8, scaleFactors.get(3));
        assertEquals(16, scaleFactors.get(4));
    }


    @Test
    void newImageInfoMinTileSize() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(2000, 2000)
                .withTileSize(1000, 1000)
                .build();
        instance.setMinTileSize(1000);
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> tiles =
                (List<Map<String,Object>>) iiifInfo.get("tiles");
        assertEquals(1000L, tiles.getFirst().get("width"));
        assertEquals(1000L, tiles.getFirst().get("height"));
    }

    @Test
    void newImageInfoTilesWithRotatedImage() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(64, 56)
                .withMetadata(new MutableMetadata() {
                    @Override
                    public Orientation getOrientation() {
                        return Orientation.ROTATE_90;
                    }
                })
                .withTileSize(64, 56)
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> tiles =
                (List<Map<String,Object>>) iiifInfo.get("tiles");
        assertEquals(56L, tiles.getFirst().get("width"));
        assertEquals(64L, tiles.getFirst().get("height"));
    }

    @Test
    void newImageInfoTilesWithScaleConstrainedImage() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 2));

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> tiles =
                (List<Map<String,Object>>) iiifInfo.get("tiles");
        assertEquals(32L, tiles.getFirst().get("width"));
        assertEquals(28L, tiles.getFirst().get("height"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void newImageInfoTilesWithTiledImage() {
        final String imageURI = "http://example.org/bla";
        Info info = Info.builder()
                .withSize(64, 56)
                .withTileSize(64, 56)
                .build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 1));

        List<Map<String,Object>> tiles =
                (List<Map<String,Object>>) iiifInfo.get("tiles");
        assertEquals(1, tiles.size());
        assertEquals(64L, tiles.getFirst().get("width"));
        assertEquals(56L, tiles.getFirst().get("height"));

        assertEquals(1, ((List<Integer>) tiles.getFirst().get("scaleFactors")).size());
        assertEquals(1, (int) ((List<Integer>) tiles.getFirst().get("scaleFactors")).getFirst());
    }

    @Test
    void newImageInfoPageCount() {
        Map<String, Object> iiifInfo = invokeNewImageInfo();
        int pageCount = (int) iiifInfo.get("pageCount");
        assertEquals(1, pageCount);
    }

    @Test
    void newImageInfoProfile() {
        Map<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertEquals("http://iiif.io/api/image/2/level2.json", profile.getFirst());
    }

    @Test
    void newImageInfoFormats() {
        Map<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent.)
        assertTrue(((Set<?>) ((Map<?, ?>) profile.get(1)).get("formats")).contains("gif"));
    }

    @Test
    void newImageInfoQualities() {
        Map<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        // If some are present, we will assume the rest are. (The exact
        // contents of the sets are processor-dependent.)
        assertTrue(((Set<?>) ((Map<?, ?>) profile.get(1)).get("qualities")).contains("color"));
    }

    @Test
    void newImageInfoMaxAreaWithPositiveMaxPixels() {
        final int maxPixels = 100;
        instance.setMaxPixels(maxPixels);

        Map<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertEquals(maxPixels, ((Map<?, ?>) profile.get(1)).get("maxArea"));
    }

    @Test
    void newImageInfoMaxAreaWithZeroMaxPixels() {
        final int maxPixels = 0;
        instance.setMaxPixels(maxPixels);

        Map<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertFalse(((Map<?, ?>) profile.get(1)).containsKey("maxArea"));
    }

    @Test
    void newImageInfoMaxAreaWithAllowUpscalingDisabled() {
        final int maxPixels = 2000000;
        instance.setMaxPixels(maxPixels);
        instance.setMaxScale(1.0);

        Map<String, Object> iiifInfo = invokeNewImageInfo();
        List<?> profile = (List<?>) iiifInfo.get("profile");
        assertEquals(1500 * 1200, ((Map<?, ?>) profile.get(1)).get("maxArea"));
    }

    @Test
    void newImageInfoSupports() {
        Map<String, Object> iiifInfo = invokeNewImageInfo();

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertTrue(supportsSet.contains("baseUriRedirect"));
        assertTrue(supportsSet.contains("canonicalLinkHeader"));
        assertTrue(supportsSet.contains("cors"));
        assertTrue(supportsSet.contains("jsonldMediaType"));
        assertTrue(supportsSet.contains("profileLinkHeader"));
        assertTrue(supportsSet.contains("sizeByConfinedWh"));
        assertTrue(supportsSet.contains("sizeByWhListed"));
    }

    @Test
    void newImageInfoSupportsWhenUpscalingIsAllowed() {
        instance.setMaxScale(9.0);
        Map<String, Object> iiifInfo = invokeNewImageInfo();

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertTrue(supportsSet.contains("sizeAboveFull"));
    }

    @Test
    void newImageInfoSupportsWhenUpscalingIsDisallowed() {
        instance.setMaxScale(1.0);
        Map<String, Object> iiifInfo = invokeNewImageInfo();

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertFalse(supportsSet.contains("sizeAboveFull"));
    }

    @Test
    void newImageInfoSupportsWithScaleConstraint() {
        final String imageURI = "http://example.org/bla";
        final Info info = Info.builder().withSize(1500, 1200).build();
        Map<String, Object> iiifInfo = instance.newImageInfo(
                imageURI, info, 0, new ScaleConstraint(1, 4));

        List<?> profile = (List<?>) iiifInfo.get("profile");
        final Set<?> supportsSet = (Set<?>) ((Map<?, ?>) profile.get(1)).get("supports");
        assertFalse(supportsSet.contains("sizeAboveFull"));
    }

}
