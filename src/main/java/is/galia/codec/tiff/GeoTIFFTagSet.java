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

/**
 * GeoTIFF adds several tags to the main IFD and does not define its own
 * private IFD.
 */
public class GeoTIFFTagSet extends BaselineTIFFTagSet {

    public static final Tag MODEL_PIXEL_SCALE_TAG    = new Tag(33550, "ModelPixelScaleTag", false);
    public static final Tag MODEL_TIEPOINT_TAG       = new Tag(33922, "ModelTiepointTag", false);
    public static final Tag MODEL_TRANSFORMATION_TAG = new Tag(34264, "ModelTransformationTag", false);
    public static final Tag GEO_KEY_DIRECTORY_TAG    = new Tag(34735, "GeoKeyDirectoryTag", false);
    public static final Tag GEO_DOUBLE_PARAMS_TAG    = new Tag(34736, "GeoDoubleParamsTag", false);
    public static final Tag GEO_ASCII_PARAMS_TAG     = new Tag(34737, "GeoAsciiParamsTag", false);

    public GeoTIFFTagSet() {
        super();
        addTag(MODEL_PIXEL_SCALE_TAG);
        addTag(MODEL_TIEPOINT_TAG);
        addTag(MODEL_TRANSFORMATION_TAG);
        addTag(GEO_KEY_DIRECTORY_TAG);
        addTag(GEO_DOUBLE_PARAMS_TAG);
        addTag(GEO_ASCII_PARAMS_TAG);
    }

}
