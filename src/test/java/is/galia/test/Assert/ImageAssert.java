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

package is.galia.test.Assert;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public final class ImageAssert {

    /**
     * Asserts that all of the pixels in the given image are either black or
     * white.
     *
     * @param image Image to test.
     */
    public static void assertBitonal(BufferedImage image) {
        final int width    = image.getWidth();
        final int height   = image.getHeight();
        final int maxValue =
                (int) Math.pow(2, image.getColorModel().getComponentSize(0)) - 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = image.getRGB(x, y);
                final int red   = (pixel >> 16) & 0xff;
                final int green = (pixel >> 8) & 0xff;
                final int blue  = pixel & 0xff;
                assertTrue(
                        (red == maxValue && green == maxValue && blue == maxValue) ||
                        (red == 0 && green == 0 && blue == 0));
            }
        }
    }

    /**
     * Asserts that all of the pixels in the given image are some shade of gray.
     *
     * @param image Image to test.
     */
    public static void assertGray(BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                assertGray(image.getRGB(x, y));
            }
        }
    }

    /**
     * Asserts that the given pixel is some shade of gray.
     *
     * @param pixel Pixel to test.
     */
    public static void assertGray(int pixel) {
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        if (red != green || green != blue) {
            fail("Red: " + red + " green: " + green + " blue: " + blue);
        }
    }

    /**
     * @param pixel Pixel to test.
     * @param expectedRed Expected red value of the pixel.
     * @param expectedGreen Expected green value of the pixel.
     * @param expectedBlue Expected blue value of the pixel.
     * @param expectedAlpha Expected alpha value of the pixel.
     */
    public static void assertNotRGBA(int pixel,
                                     int expectedRed,
                                     int expectedGreen,
                                     int expectedBlue,
                                     int expectedAlpha) {
        int alpha = (pixel >> 24) & 0xff;
        int red   = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue  = (pixel) & 0xff;
        assertFalse(expectedRed == red && expectedBlue == blue &&
                expectedGreen == green && expectedAlpha == alpha);
    }

    /**
     * @param pixel Pixel to test.
     * @param expectedRed Expected red value of the pixel.
     * @param expectedGreen Expected green value of the pixel.
     * @param expectedBlue Expected blue value of the pixel.
     * @param expectedAlpha Expected alpha value of the pixel.
     */
    public static void assertRGBA(int pixel,
                                  int expectedRed,
                                  int expectedGreen,
                                  int expectedBlue,
                                  int expectedAlpha) {
        int alpha = (pixel >> 24) & 0xff;
        int red   = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue  = (pixel) & 0xff;
        assertEquals(expectedAlpha, alpha, "alpha");
        assertEquals(expectedRed, red, "red");
        assertEquals(expectedGreen, green, "green");
        assertEquals(expectedBlue, blue, "blue");
    }

    private ImageAssert() {}

}
