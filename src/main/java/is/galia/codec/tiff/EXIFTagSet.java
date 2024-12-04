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
 * Contains all EXIF tags defined in the EXIF standard.
 */
public class EXIFTagSet extends TagSet {

    public static final int IFD_POINTER = 34665;

    public static final Tag EXPOSURE_TIME                            = new Tag(33434, "ExposureTime", false);
    public static final Tag F_NUMBER                                 = new Tag(33437, "FNumber", false);
    public static final Tag EXPOSURE_PROGRAM                         = new Tag(34850, "ExposureProgram", false);
    public static final Tag SPECTRAL_SENSITIVITY                     = new Tag(34852, "SpectralSensitivity", false);
    public static final Tag PHOTOGRAPHIC_SENSITIVITY                 = new Tag(34855, "PhotographicSensitivity", false);
    public static final Tag OECF                                     = new Tag(34856, "OECF", false);
    public static final Tag SENSITIVITY_TYPE                         = new Tag(34864, "SensitivityType", false);
    public static final Tag STANDARD_OUTPUT_SENSITIVITY              = new Tag(34865, "StandardOutputSensitivity", false);
    public static final Tag RECOMMENDED_EXPOSURE_INDEX               = new Tag(34866, "RecommendedExposureIndex", false);
    public static final Tag ISO_SPEED                                = new Tag(34867, "ISOSpeed", false);
    public static final Tag ISO_SPEED_LATITUDE_YYY                   = new Tag(34868, "ISOSpeedLatitudeyyy", false);
    public static final Tag ISO_SPEED_LATITUDE_ZZZ                   = new Tag(34869, "ISOSpeedLatitudezzz", false);
    public static final Tag EXIF_VERSION                             = new Tag(36864, "ExifVersion", false);
    public static final Tag DATE_TIME_ORIGINAL                       = new Tag(36867, "DateTimeOriginal", false);
    public static final Tag DATE_TIME_DIGITIZED                      = new Tag(36868, "DateTimeDigitized", false);
    public static final Tag OFFSET_TIME                              = new Tag(36880, "OffsetTime", false);
    public static final Tag OFFSET_TIME_ORIGINAL                     = new Tag(36881, "OffsetTimeOriginal", false);
    public static final Tag OFFSET_TIME_DIGITIZED                    = new Tag(36882, "OffsetTimeDigitized", false);
    public static final Tag COMPONENTS_CONFIGURATION                 = new Tag(37121, "ComponentsConfiguration", false);
    public static final Tag COMPRESSED_BITS_PER_PIXEL                = new Tag(37122, "CompressedBitsPerPixel", false);
    public static final Tag SHUTTER_SPEED                            = new Tag(37377, "ShutterSpeedValue", false);
    public static final Tag APERTURE                                 = new Tag(37378, "Aperture", false);
    public static final Tag BRIGHTNESS                               = new Tag(37379, "Brightness", false);
    public static final Tag EXPOSURE_BIAS                            = new Tag(37380, "ExposureBias", false);
    public static final Tag MAX_APERTURE_VALUE                       = new Tag(37381, "MaxApertureValue", false);
    public static final Tag SUBJECT_DISTANCE                         = new Tag(37382, "SubjectDistance", false);
    public static final Tag METERING_MODE                            = new Tag(37383, "MeteringMode", false);
    public static final Tag LIGHT_SOURCE                             = new Tag(37384, "LightSource", false);
    public static final Tag FLASH                                    = new Tag(37385, "Flash", false);
    public static final Tag FOCAL_LENGTH                             = new Tag(37386, "FocalLength", false);
    public static final Tag SUBJECT_AREA                             = new Tag(37396, "SubjectArea", false);
    public static final Tag MAKER_NOTE                               = new Tag(37500, "MakerNote", false);
    public static final Tag USER_COMMENT                             = new Tag(37510, "UserComment", false);
    public static final Tag SUB_SEC_TIME                             = new Tag(37520, "SubSecTime", false);
    public static final Tag SUB_SEC_TIME_ORIGINAL                    = new Tag(37521, "SubSecTimeOriginal", false);
    public static final Tag SUB_SEC_TIME_DIGITIZED                   = new Tag(37522, "SubSecTimeDigitized", false);
    /** @since EXIF 2.31 */
    public static final Tag TEMPERATURE                              = new Tag(37888, "Temperature", false);
    /** @since EXIF 2.31 */
    public static final Tag HUMIDITY                                 = new Tag(37889, "Humidity", false);
    /** @since EXIF 2.31 */
    public static final Tag PRESSURE                                 = new Tag(37890, "Pressure", false);
    /** @since EXIF 2.31 */
    public static final Tag WATER_DEPTH                              = new Tag(37891, "WaterDepth", false);
    /** @since EXIF 2.31 */
    public static final Tag ACCELERATION                             = new Tag(37892, "Acceleration", false);
    /** @since EXIF 2.31 */
    public static final Tag CAMERA_ELEVATION_ANGLE                   = new Tag(37893, "CameraElevationAngle", false);
    public static final Tag FLASHPIX_VERSION                         = new Tag(40960, "FlashpixVersion", false);
    public static final Tag COLOR_SPACE                              = new Tag(40961, "ColorSpace", false);
    public static final Tag PIXEL_X_DIMENSION                        = new Tag(40962, "PixelXDimension", false);
    public static final Tag PIXEL_Y_DIMENSION                        = new Tag(40963, "PixelYDimension", false);
    public static final Tag RELATED_SOUND_FILE                       = new Tag(40964, "RelatedSoundFile", false);
    public static final Tag FLASH_ENERGY                             = new Tag(41483, "FlashEnergy", false);
    public static final Tag SPATIAL_FREQUENCY_RESPONSE               = new Tag(41484, "SpatialFrequencyResponse", false);
    public static final Tag FOCAL_PLANE_X_RESOLUTION                 = new Tag(41486, "FocalPlaneXResolution", false);
    public static final Tag FOCAL_PLANE_Y_RESOLUTION                 = new Tag(41487, "FocalPlaneYResolution", false);
    public static final Tag FOCAL_PLANE_RESOLUTION_UNIT              = new Tag(41488, "FocalPlaneResolutionUnit", false);
    public static final Tag SUBJECT_LOCATION                         = new Tag(41492, "SubjectLocation", false);
    public static final Tag EXPOSURE_INDEX                           = new Tag(41493, "ExposureIndex", false);
    public static final Tag SENSING_METHOD                           = new Tag(41495, "SensingMethod", false);
    public static final Tag FILE_SOURCE                              = new Tag(41728, "FileSource", false);
    public static final Tag SCENE_TYPE                               = new Tag(41729, "SceneType", false);
    public static final Tag CFA_PATTERN                              = new Tag(41730, "CFAPattern", false);
    public static final Tag CUSTOM_RENDERED                          = new Tag(41985, "CustomRendered", false);
    public static final Tag EXPOSURE_MODE                            = new Tag(41986, "ExposureMode", false);
    public static final Tag WHITE_BALANCE                            = new Tag(41987, "WhiteBalance", false);
    public static final Tag DIGITAL_ZOOM_RATIO                       = new Tag(41988, "DigitalZoomRatio", false);
    public static final Tag FOCAL_LENGTH_IN_35MM_FILM                = new Tag(41989, "FocalLengthIn35mmFilm", false);
    public static final Tag SCENE_CAPTURE_TYPE                       = new Tag(41990, "SceneCaptureType", false);
    public static final Tag GAIN_CONTROL                             = new Tag(41991, "GainControl", false);
    public static final Tag CONTRAST                                 = new Tag(41992, "Contrast", false);
    public static final Tag SATURATION                               = new Tag(41993, "Saturation", false);
    public static final Tag SHARPNESS                                = new Tag(41994, "Sharpness", false);
    public static final Tag DEVICE_SETTING_DESCRIPTION               = new Tag(41995, "DeviceSettingDescription", false);
    public static final Tag SUBJECT_DISTANCE_RANGE                   = new Tag(41996, "SubjectDistanceRange", false);
    public static final Tag IMAGE_UNIQUE_ID                          = new Tag(42016, "ImageUniqueID", false);
    public static final Tag CAMERA_OWNER_NAME                        = new Tag(42032, "CameraOwnerName", false);
    public static final Tag BODY_SERIAL_NUMBER                       = new Tag(42033, "BodySerialNumber", false);
    public static final Tag LENS_SPECIFICATION                       = new Tag(42034, "LensSpecification", false);
    public static final Tag LENS_MAKE                                = new Tag(42035, "LensMake", false);
    public static final Tag LENS_MODEL                               = new Tag(42036, "LensModel", false);
    /** @since EXIF 3.0 */
    public static final Tag LENS_SERIAL_NUMBER                       = new Tag(42037, "LensSerialNumber", false);
    /** @since EXIF 3.0 */
    public static final Tag IMAGE_TITLE                              = new Tag(42038, "Image Title", false);
    /** @since EXIF 3.0 */
    public static final Tag PHOTOGRAPHER                             = new Tag(42039, "Photographer", false);
    /** @since EXIF 3.0 */
    public static final Tag IMAGE_EDITOR                             = new Tag(42040, "Image Editor", false);
    /** @since EXIF 3.0 */
    public static final Tag CAMERA_FIRMWARE                          = new Tag(42041, "Camera Firmware", false);
    /** @since EXIF 3.0 */
    public static final Tag RAW_DEVELOPING_SOFTWARE                  = new Tag(42042, "RAWDevelopingSoftware", false);
    /** @since EXIF 3.0 */
    public static final Tag IMAGE_EDITING_SOFTWARE                   = new Tag(42043, "Image Editing Software", false);
    /** @since EXIF 3.0 */
    public static final Tag METADATA_EDITING_SOFTWARE                = new Tag(42044, "MetadataEditingSoftware", false);
    /** @since EXIF 2.32 */
    public static final Tag COMPOSITE_IMAGE                          = new Tag(42080, "CompositeImage", false);
    /** @since EXIF 2.32 */
    public static final Tag SOURCE_IMAGE_NUMBER_OF_COMPOSITE_IMAGE   = new Tag(42081, "SourceImageNumberOfCompositeImage", false);
    /** @since EXIF 2.32 */
    public static final Tag SOURCE_EXPOSURE_TIMES_OF_COMPOSITE_IMAGE = new Tag(42082, "SourceExposureTimesOfCompositeImage", false);
    public static final Tag GAMMA                                    = new Tag(42240, "Gamma", false);

    public EXIFTagSet() {
        super(IFD_POINTER, "EXIF");
        addTag(EXPOSURE_TIME);
        addTag(F_NUMBER);
        addTag(EXPOSURE_PROGRAM);
        addTag(SPECTRAL_SENSITIVITY);
        addTag(PHOTOGRAPHIC_SENSITIVITY);
        addTag(OECF);
        addTag(SENSITIVITY_TYPE);
        addTag(STANDARD_OUTPUT_SENSITIVITY);
        addTag(RECOMMENDED_EXPOSURE_INDEX);
        addTag(ISO_SPEED);
        addTag(ISO_SPEED_LATITUDE_YYY);
        addTag(ISO_SPEED_LATITUDE_ZZZ);
        addTag(EXIF_VERSION);
        addTag(DATE_TIME_ORIGINAL);
        addTag(DATE_TIME_DIGITIZED);
        addTag(OFFSET_TIME);
        addTag(OFFSET_TIME_ORIGINAL);
        addTag(OFFSET_TIME_DIGITIZED);
        addTag(COMPONENTS_CONFIGURATION);
        addTag(COMPRESSED_BITS_PER_PIXEL);
        addTag(SHUTTER_SPEED);
        addTag(APERTURE);
        addTag(BRIGHTNESS);
        addTag(EXPOSURE_BIAS);
        addTag(MAX_APERTURE_VALUE);
        addTag(SUBJECT_DISTANCE);
        addTag(METERING_MODE);
        addTag(LIGHT_SOURCE);
        addTag(FLASH);
        addTag(FOCAL_LENGTH);
        addTag(SUBJECT_AREA);
        addTag(MAKER_NOTE);
        addTag(USER_COMMENT);
        addTag(SUB_SEC_TIME);
        addTag(SUB_SEC_TIME_ORIGINAL);
        addTag(SUB_SEC_TIME_DIGITIZED);
        addTag(TEMPERATURE);
        addTag(HUMIDITY);
        addTag(PRESSURE);
        addTag(WATER_DEPTH);
        addTag(ACCELERATION);
        addTag(CAMERA_ELEVATION_ANGLE);
        addTag(FLASHPIX_VERSION);
        addTag(COLOR_SPACE);
        addTag(PIXEL_X_DIMENSION);
        addTag(PIXEL_Y_DIMENSION);
        addTag(RELATED_SOUND_FILE);
        addTag(FLASH_ENERGY);
        addTag(SPATIAL_FREQUENCY_RESPONSE);
        addTag(FOCAL_PLANE_X_RESOLUTION);
        addTag(FOCAL_PLANE_Y_RESOLUTION);
        addTag(FOCAL_PLANE_RESOLUTION_UNIT);
        addTag(SUBJECT_LOCATION);
        addTag(EXPOSURE_INDEX);
        addTag(SENSING_METHOD);
        addTag(FILE_SOURCE);
        addTag(SCENE_TYPE);
        addTag(CFA_PATTERN);
        addTag(CUSTOM_RENDERED);
        addTag(EXPOSURE_MODE);
        addTag(WHITE_BALANCE);
        addTag(DIGITAL_ZOOM_RATIO);
        addTag(FOCAL_LENGTH_IN_35MM_FILM);
        addTag(SCENE_CAPTURE_TYPE);
        addTag(GAIN_CONTROL);
        addTag(CONTRAST);
        addTag(SATURATION);
        addTag(SHARPNESS);
        addTag(DEVICE_SETTING_DESCRIPTION);
        addTag(SUBJECT_DISTANCE_RANGE);
        addTag(IMAGE_UNIQUE_ID);
        addTag(CAMERA_OWNER_NAME);
        addTag(BODY_SERIAL_NUMBER);
        addTag(LENS_SPECIFICATION);
        addTag(LENS_MAKE);
        addTag(LENS_MODEL);
        addTag(LENS_SERIAL_NUMBER);
        addTag(IMAGE_TITLE);
        addTag(PHOTOGRAPHER);
        addTag(IMAGE_EDITOR);
        addTag(CAMERA_FIRMWARE);
        addTag(RAW_DEVELOPING_SOFTWARE);
        addTag(IMAGE_EDITING_SOFTWARE);
        addTag(METADATA_EDITING_SOFTWARE);
        addTag(COMPOSITE_IMAGE);
        addTag(SOURCE_IMAGE_NUMBER_OF_COMPOSITE_IMAGE);
        addTag(SOURCE_EXPOSURE_TIMES_OF_COMPOSITE_IMAGE);
        addTag(GAMMA);
    }
    
}
