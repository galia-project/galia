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
 * Contains all Baseline TIFF tags defined in the TIFF 6.0 specification.
 */
public class BaselineTIFFTagSet extends TagSet {

    public static final int IFD_POINTER = 0;

    public static final Tag NEW_SUBFILE_TYPE               = new Tag(254, "NewSubfileType", false);
    public static final Tag SUBFILE_TYPE                   = new Tag(255, "SubfileType", false);
    public static final Tag IMAGE_WIDTH                    = new Tag(256, "ImageWidth", false);
    public static final Tag IMAGE_LENGTH                   = new Tag(257, "ImageLength", false);
    public static final Tag BITS_PER_SAMPLE                = new Tag(258, "BitsPerSample", false);
    public static final Tag COMPRESSION                    = new Tag(259, "Compression", false);
    public static final Tag PHOTOMETRIC_INTERPRETATION     = new Tag(262, "PhotometricInterpretation", false);
    public static final Tag THRESHHOLDING                  = new Tag(263, "Threshholding", false);
    public static final Tag CELL_WIDTH                     = new Tag(264, "CellWidth", false);
    public static final Tag CELL_LENGTH                    = new Tag(265, "CellLength", false);
    public static final Tag FILL_ORDER                     = new Tag(266, "FillOrder", false);
    public static final Tag DOCUMENT_ORDER                 = new Tag(269, "DocumentOrder", false);
    public static final Tag IMAGE_DESCRIPTION              = new Tag(270, "ImageDescription", false);
    public static final Tag MAKE                           = new Tag(271, "Make", false);
    public static final Tag MODEL                          = new Tag(272, "Model", false);
    public static final Tag STRIP_OFFSETS                  = new Tag(273, "StripOffsets", false);
    public static final Tag ORIENTATION                    = new Tag(274, "Orientation", false);
    public static final Tag SAMPLES_PER_PIXEL              = new Tag(277, "SamplesPerPixel", false);
    public static final Tag ROWS_PER_STRIP                 = new Tag(278, "RowsPerStrip", false);
    public static final Tag STRIP_BYTE_COUNTS              = new Tag(279, "StripByteCounts", false);
    public static final Tag MIN_SAMPLE_VALUE               = new Tag(280, "MinSampleValue", false);
    public static final Tag MAX_SAMPLE_VALUE               = new Tag(281, "MaxSampleValue", false);
    public static final Tag X_RESOLUTION                   = new Tag(282, "XResolution", false);
    public static final Tag Y_RESOLUTION                   = new Tag(283, "YResolution", false);
    public static final Tag PLANAR_CONFIGURATION           = new Tag(284, "PlanarConfiguration", false);
    public static final Tag PAGE_NAME                      = new Tag(285, "PageName", false);
    public static final Tag X_POSITION                     = new Tag(286, "XPosition", false);
    public static final Tag Y_POSITION                     = new Tag(287, "YPosition", false);
    public static final Tag FREE_OFFSETS                   = new Tag(288, "FreeOffsets", false);
    public static final Tag FREE_BYTE_COUNTS               = new Tag(289, "FreeByteCounts", false);
    public static final Tag GRAY_RESPONSE_UNIT             = new Tag(290, "GrayResponseUnit", false);
    public static final Tag GRAY_RESPONSE_CURVE            = new Tag(291, "GrayResponseCurve", false);
    public static final Tag T4_OPTIONS                     = new Tag(292, "T4Options", false);
    public static final Tag T6_OPTIONS                     = new Tag(293, "T6Options", false);
    public static final Tag RESOLUTION_UNIT                = new Tag(296, "ResolutionUnit", false);
    public static final Tag PAGE_NUMBER                    = new Tag(297, "PageNumber", false);
    public static final Tag TRANSFER_FUNCTION              = new Tag(301, "TransferFunction", false);
    public static final Tag SOFTWARE                       = new Tag(305, "Software", false);
    public static final Tag DATE_TIME                      = new Tag(306, "DateTime", false);
    public static final Tag ARTIST                         = new Tag(315, "Artist", false);
    public static final Tag HOST_COMPUTER                  = new Tag(316, "HostComputer", false);
    public static final Tag PREDICTOR                      = new Tag(317, "Predictor", false);
    public static final Tag WHITE_POINT                    = new Tag(318, "WhitePoint", false);
    public static final Tag PRIMARY_CHROMATICITIES         = new Tag(319, "PrimaryChromaticities", false);
    public static final Tag COLOR_MAP                      = new Tag(320, "ColorMap", false);
    public static final Tag HALFTONE_HINTS                 = new Tag(321, "HalftoneHints", false);
    public static final Tag TILE_WIDTH                     = new Tag(322, "TileWidth", false);
    public static final Tag TILE_LENGTH                    = new Tag(323, "TileLength", false);
    public static final Tag TILE_OFFSETS                   = new Tag(324, "TileOffsets", false);
    public static final Tag TILE_BYTE_COUNTS               = new Tag(325, "TileByteCounts", false);
    public static final Tag INK_SET                        = new Tag(332, "InkSet", false);
    public static final Tag INK_NAMES                      = new Tag(333, "InkNames", false);
    public static final Tag NUMBER_OF_INKS                 = new Tag(334, "NumberOfInks", false);
    public static final Tag DOT_RANGE                      = new Tag(336, "DotRange", false);
    public static final Tag TARGET_PRINTER                 = new Tag(337, "TargetPrinter", false);
    public static final Tag EXTRA_SAMPLES                  = new Tag(338, "ExtraSamples", false);
    public static final Tag SAMPLE_FORMAT                  = new Tag(339, "SampleFormat", false);
    public static final Tag S_MIN_SAMPLE_VALUE             = new Tag(340, "SMinSampleValue", false);
    public static final Tag S_MAX_SAMPLE_VALUE             = new Tag(341, "SMaxSampleValue", false);
    public static final Tag TRANSFER_RANGE                 = new Tag(342, "TransferRange", false);
    public static final Tag JPEG_PROC                      = new Tag(512, "JPEGProc", false);
    public static final Tag JPEG_INTERCHANGE_FORMAT        = new Tag(513, "JPEGInterchangeFormat", false);
    public static final Tag JPEG_INTERCHANGE_FORMAT_LENGTH = new Tag(514, "JPEGInterchangeFormatLength", false);
    public static final Tag JPEG_RESTART_INTERVAL          = new Tag(515, "JPEGRestartInterval", false);
    public static final Tag JPEG_LOSSLESS_PREDICTORS       = new Tag(517, "JPEGLosslessPredictors", false);
    public static final Tag JPEG_POINT_TRANSFORMS          = new Tag(518, "JPEGPointTransforms", false);
    public static final Tag JPEG_Q_TABLES                  = new Tag(519, "JPEGQTables", false);
    public static final Tag JPEG_DC_TABLES                 = new Tag(520, "JPEGDCTables", false);
    public static final Tag JPEG_AC_TABLES                 = new Tag(521, "JPEGACTables", false);
    public static final Tag Y_CB_CR_COEFFICIENTS           = new Tag(529, "YCbCrCoefficients", false);
    public static final Tag Y_CB_CR_SUB_SAMPLING           = new Tag(530, "YCbCrSubSampling", false);
    public static final Tag Y_CB_CR_POSITIONING            = new Tag(531, "YCbCrPositioning", false);
    public static final Tag REFERENCE_BLACK_WHITE          = new Tag(532, "ReferenceBlackWhite", false);
    public static final Tag COPYRIGHT                      = new Tag(33432, "Copyright", false);

    public BaselineTIFFTagSet() {
        super(IFD_POINTER, "Baseline TIFF");

        addTag(NEW_SUBFILE_TYPE);
        addTag(SUBFILE_TYPE);
        addTag(IMAGE_WIDTH);
        addTag(IMAGE_LENGTH);
        addTag(BITS_PER_SAMPLE);
        addTag(COMPRESSION);
        addTag(PHOTOMETRIC_INTERPRETATION);
        addTag(THRESHHOLDING);
        addTag(CELL_WIDTH);
        addTag(CELL_LENGTH);
        addTag(FILL_ORDER);
        addTag(DOCUMENT_ORDER);
        addTag(IMAGE_DESCRIPTION);
        addTag(MAKE);
        addTag(MODEL);
        addTag(STRIP_OFFSETS);
        addTag(ORIENTATION);
        addTag(SAMPLES_PER_PIXEL);
        addTag(ROWS_PER_STRIP);
        addTag(STRIP_BYTE_COUNTS);
        addTag(MIN_SAMPLE_VALUE);
        addTag(MAX_SAMPLE_VALUE);
        addTag(X_RESOLUTION);
        addTag(Y_RESOLUTION);
        addTag(PLANAR_CONFIGURATION);
        addTag(PAGE_NAME);
        addTag(X_POSITION);
        addTag(Y_POSITION);
        addTag(FREE_OFFSETS);
        addTag(FREE_BYTE_COUNTS);
        addTag(GRAY_RESPONSE_UNIT);
        addTag(GRAY_RESPONSE_CURVE);
        addTag(T4_OPTIONS);
        addTag(T6_OPTIONS);
        addTag(RESOLUTION_UNIT);
        addTag(PAGE_NUMBER);
        addTag(TRANSFER_FUNCTION);
        addTag(SOFTWARE);
        addTag(DATE_TIME);
        addTag(ARTIST);
        addTag(HOST_COMPUTER);
        addTag(PREDICTOR);
        addTag(WHITE_POINT);
        addTag(PRIMARY_CHROMATICITIES);
        addTag(COLOR_MAP);
        addTag(HALFTONE_HINTS);
        addTag(TILE_WIDTH);
        addTag(TILE_LENGTH);
        addTag(TILE_OFFSETS);
        addTag(TILE_BYTE_COUNTS);
        addTag(INK_SET);
        addTag(INK_NAMES);
        addTag(NUMBER_OF_INKS);
        addTag(DOT_RANGE);
        addTag(TARGET_PRINTER);
        addTag(EXTRA_SAMPLES);
        addTag(SAMPLE_FORMAT);
        addTag(S_MIN_SAMPLE_VALUE);
        addTag(S_MAX_SAMPLE_VALUE);
        addTag(TRANSFER_RANGE);
        addTag(JPEG_PROC);
        addTag(JPEG_INTERCHANGE_FORMAT);
        addTag(JPEG_INTERCHANGE_FORMAT_LENGTH);
        addTag(JPEG_RESTART_INTERVAL);
        addTag(JPEG_LOSSLESS_PREDICTORS);
        addTag(JPEG_POINT_TRANSFORMS);
        addTag(JPEG_Q_TABLES);
        addTag(JPEG_DC_TABLES);
        addTag(JPEG_AC_TABLES);
        addTag(Y_CB_CR_COEFFICIENTS);
        addTag(Y_CB_CR_SUB_SAMPLING);
        addTag(Y_CB_CR_POSITIONING);
        addTag(REFERENCE_BLACK_WHITE);
        addTag(COPYRIGHT);
    }

}
