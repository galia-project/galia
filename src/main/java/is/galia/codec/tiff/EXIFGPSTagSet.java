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
 * Contains all GPS tags defined in the EXIF standard.
 */
public class EXIFGPSTagSet extends TagSet {

    public static final int IFD_POINTER = 34853;

    public static final Tag GPS_VERSION_ID          = new Tag(0, "GPSVersionID", false);
    public static final Tag GPS_LATITUDE_REF        = new Tag(1, "GPSLatitudeRef", false);
    public static final Tag GPS_LATITUDE            = new Tag(2, "GPSLatitude", false);
    public static final Tag GPS_LONGITUDE_REF       = new Tag(3, "GPSLongitudeRef", false);
    public static final Tag GPS_LONGITUDE           = new Tag(4, "GPSLongitude", false);
    public static final Tag GPS_ALTITUDE_REF        = new Tag(5, "GPSAltitudeRef", false);
    public static final Tag GPS_ALTITUDE            = new Tag(6, "GPSAltitude", false);
    public static final Tag GPS_TIME_STAMP          = new Tag(7, "GPSTimeStamp", false);
    public static final Tag GPS_SATELLITES          = new Tag(8, "GPSSatellites", false);
    public static final Tag GPS_STATUS              = new Tag(9, "GPSStatus", false);
    public static final Tag GPS_MEASURE_MODE        = new Tag(10, "GPSMeasureMode", false);
    public static final Tag GPS_DOP                 = new Tag(11, "GPSDOP", false);
    public static final Tag GPS_SPEED_REF           = new Tag(12, "GPSSpeedRef", false);
    public static final Tag GPS_SPEED               = new Tag(13, "GPSSpeed", false);
    public static final Tag GPS_TRACK_REF           = new Tag(14, "GPSTrackRef", false);
    public static final Tag GPS_TRACK               = new Tag(15, "GPSTrack", false);
    public static final Tag GPS_IMG_DIRECTION_REF   = new Tag(16, "GPSImgDirectionRef", false);
    public static final Tag GPS_IMG_DIRECTION       = new Tag(17, "GPSImgDirection", false);
    public static final Tag GPS_MAP_DATUM           = new Tag(18, "GPSMapDatum", false);
    public static final Tag GPS_DEST_LATITUDE_REF   = new Tag(19, "GPSDestLatitudeRef", false);
    public static final Tag GPS_DEST_LATITUDE       = new Tag(20, "GPSDestLatitude", false);
    public static final Tag GPS_DEST_LONGITUDE_REF  = new Tag(21, "GPSDestLongitudeRef", false);
    public static final Tag GPS_DEST_LONGITUDE      = new Tag(22, "GPSDestLongitude", false);
    public static final Tag GPS_DEST_BEARING_REF    = new Tag(23, "GPSDestBearingRef", false);
    public static final Tag GPS_DEST_BEARING        = new Tag(24, "GPSDestBearing", false);
    public static final Tag GPS_DEST_DISTANCE_REF   = new Tag(25, "GPSDestDistanceRef", false);
    public static final Tag GPS_DEST_DISTANCE       = new Tag(26, "GPSDestDistance", false);
    public static final Tag GPS_PROCESSING_METHOD   = new Tag(27, "GPSProcessingMethod", false);
    public static final Tag GPS_AREA_INFORMATION    = new Tag(28, "GPSAreaInformation", false);
    public static final Tag GPS_DATE_STAMP          = new Tag(29, "GPSDateStamp", false);
    public static final Tag GPS_DIFFERENTIAL        = new Tag(30, "GPSDifferential", false);
    public static final Tag GPS_H_POSITIONING_ERROR = new Tag(31, "GPSHPositioningError", false);

    public EXIFGPSTagSet() {
        super(IFD_POINTER, "GPS");
        addTag(GPS_VERSION_ID);
        addTag(GPS_LATITUDE_REF);
        addTag(GPS_LATITUDE);
        addTag(GPS_LONGITUDE_REF);
        addTag(GPS_LONGITUDE);
        addTag(GPS_ALTITUDE_REF);
        addTag(GPS_ALTITUDE);
        addTag(GPS_TIME_STAMP);
        addTag(GPS_SATELLITES);
        addTag(GPS_STATUS);
        addTag(GPS_MEASURE_MODE);
        addTag(GPS_DOP);
        addTag(GPS_SPEED_REF);
        addTag(GPS_SPEED);
        addTag(GPS_TRACK_REF);
        addTag(GPS_TRACK);
        addTag(GPS_IMG_DIRECTION_REF);
        addTag(GPS_IMG_DIRECTION);
        addTag(GPS_MAP_DATUM);
        addTag(GPS_DEST_LATITUDE_REF);
        addTag(GPS_DEST_LATITUDE);
        addTag(GPS_DEST_LONGITUDE_REF);
        addTag(GPS_DEST_LONGITUDE);
        addTag(GPS_DEST_BEARING_REF);
        addTag(GPS_DEST_BEARING);
        addTag(GPS_DEST_DISTANCE_REF);
        addTag(GPS_DEST_DISTANCE);
        addTag(GPS_PROCESSING_METHOD);
        addTag(GPS_AREA_INFORMATION);
        addTag(GPS_DATE_STAMP);
        addTag(GPS_DIFFERENTIAL);
        addTag(GPS_H_POSITIONING_ERROR);
    }

}
