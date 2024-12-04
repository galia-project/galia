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
 * Contains all Baseline TIFF tags specified in the EXIF standard.
 */
public class EXIFBaselineTIFFTagSet extends TagSet {

    public static final int IFD_POINTER = 0;

    public static final Tag IMAGE_WIDTH                    = new Tag(256, "ImageWidth", false);
    public static final Tag IMAGE_LENGTH                   = new Tag(257, "ImageLength", false);
    public static final Tag BITS_PER_SAMPLE                = new Tag(258, "BitsPerSample", false);
    public static final Tag COMPRESSION                    = new Tag(259, "Compression", false);
    public static final Tag PHOTOMETRIC_INTERPRETATION     = new Tag(262, "PhotometricInterpretation", false);
    public static final Tag IMAGE_DESCRIPTION              = new Tag(270, "ImageDescription", false);
    public static final Tag MAKE                           = new Tag(271, "Make", false);
    public static final Tag MODEL                          = new Tag(272, "Model", false);
    public static final Tag STRIP_OFFSETS                  = new Tag(273, "StripOffsets", false);
    public static final Tag ORIENTATION                    = new Tag(274, "Orientation", false);
    public static final Tag SAMPLES_PER_PIXEL              = new Tag(277, "SamplesPerPixel", false);
    public static final Tag ROWS_PER_STRIP                 = new Tag(278, "RowsPerStrip", false);
    public static final Tag STRIP_BYTE_COUNTS              = new Tag(279, "StripByteCounts", false);
    public static final Tag X_RESOLUTION                   = new Tag(282, "XResolution", false);
    public static final Tag Y_RESOLUTION                   = new Tag(283, "YResolution", false);
    public static final Tag PLANAR_CONFIGURATION           = new Tag(284, "PlanarConfiguration", false);
    public static final Tag RESOLUTION_UNIT                = new Tag(296, "ResolutionUnit", false);
    public static final Tag TRANSFER_FUNCTION              = new Tag(301, "TransferFunction", false);
    public static final Tag SOFTWARE                       = new Tag(305, "Software", false);
    public static final Tag DATE_TIME                      = new Tag(306, "DateTime", false);
    public static final Tag ARTIST                         = new Tag(315, "Artist", false);
    public static final Tag WHITE_POINT                    = new Tag(318, "WhitePoint", false);
    public static final Tag PRIMARY_CHROMATICITIES         = new Tag(319, "PrimaryChromaticities", false);
    public static final Tag JPEG_INTERCHANGE_FORMAT        = new Tag(513, "JPEGInterchangeFormat", false);
    public static final Tag JPEG_INTERCHANGE_FORMAT_LENGTH = new Tag(514, "JPEGInterchangeFormatLength", false);
    public static final Tag Y_CB_CR_COEFFICIENTS           = new Tag(529, "YCbCrCoefficients", false);
    public static final Tag Y_CB_CR_SUB_SAMPLING           = new Tag(530, "YCbCrSubSampling", false);
    public static final Tag Y_CB_CR_POSITIONING            = new Tag(531, "YCbCrPositioning", false);
    public static final Tag REFERENCE_BLACK_WHITE          = new Tag(532, "ReferenceBlackWhite", false);
    public static final Tag COPYRIGHT                      = new Tag(33432, "Copyright", false);
    public static final Tag EXIF_IFD_POINTER               = new Tag(34665, "EXIFIFD", true);
    public static final Tag GPS_IFD_POINTER                = new Tag(34853, "GPSIFD", true);
    public static final Tag INTEROPERABILITY_IFD_POINTER   = new Tag(40965, "InteroperabilityIFD", true);

    public EXIFBaselineTIFFTagSet() {
        super(IFD_POINTER, "Baseline TIFF");

        addTag(EXIF_IFD_POINTER);
        addTag(GPS_IFD_POINTER);
        addTag(INTEROPERABILITY_IFD_POINTER);

        addTag(IMAGE_WIDTH);
        addTag(IMAGE_LENGTH);
        addTag(BITS_PER_SAMPLE);
        addTag(COMPRESSION);
        addTag(PHOTOMETRIC_INTERPRETATION);
        addTag(IMAGE_DESCRIPTION);
        addTag(MAKE);
        addTag(MODEL);
        addTag(STRIP_OFFSETS);
        addTag(ORIENTATION);
        addTag(SAMPLES_PER_PIXEL);
        addTag(ROWS_PER_STRIP);
        addTag(STRIP_BYTE_COUNTS);
        addTag(X_RESOLUTION);
        addTag(Y_RESOLUTION);
        addTag(PLANAR_CONFIGURATION);
        addTag(RESOLUTION_UNIT);
        addTag(TRANSFER_FUNCTION);
        addTag(SOFTWARE);
        addTag(DATE_TIME);
        addTag(ARTIST);
        addTag(WHITE_POINT);
        addTag(PRIMARY_CHROMATICITIES);
        addTag(JPEG_INTERCHANGE_FORMAT);
        addTag(JPEG_INTERCHANGE_FORMAT_LENGTH);
        addTag(Y_CB_CR_COEFFICIENTS);
        addTag(Y_CB_CR_SUB_SAMPLING);
        addTag(Y_CB_CR_POSITIONING);
        addTag(REFERENCE_BLACK_WHITE);
        addTag(COPYRIGHT);
    }

}
