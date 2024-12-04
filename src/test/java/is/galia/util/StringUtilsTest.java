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

package is.galia.util;

import is.galia.config.Configuration;
import is.galia.config.Key;
import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest extends BaseTest {

    @Test
    void decodeSlashes() {
        Configuration.forApplication().setProperty(Key.SLASH_SUBSTITUTE, "$$");
        assertEquals("cats", StringUtils.decodeSlashes("cats"));
        assertEquals("ca/ts", StringUtils.decodeSlashes("ca$$ts"));
    }

    @Test
    void encodeSlashes() {
        Configuration.forApplication().setProperty(Key.SLASH_SUBSTITUTE, "$$");
        assertEquals("cats", StringUtils.encodeSlashes("cats"));
        assertEquals("ca$$ts", StringUtils.encodeSlashes("ca/ts"));
    }

    @Test
    void escapeHTML() {
        String html = "the quick brown <script type=\"text/javascript\">alert('hi');</script> fox";
        String expected = "the quick brown &#60;script type=&#34;text/javascript&#34;&#62;alert('hi');&#60;/script&#62; fox";
        assertEquals(expected, StringUtils.escapeHTML(html));
    }

    @Test
    void fromByteSizeWithBytes() {
        assertEquals("0 bytes", StringUtils.fromByteSize(0));
        assertEquals("1 bytes", StringUtils.fromByteSize(1));
        assertEquals("229 bytes", StringUtils.fromByteSize(229));
        assertEquals("865 bytes", StringUtils.fromByteSize(865));
    }

    @Test
    void fromByteSizeWithKB() {
        assertEquals("1 KB", StringUtils.fromByteSize(1465));
        assertEquals("143 KB", StringUtils.fromByteSize(146268));
        assertEquals("827 KB", StringUtils.fromByteSize(846528));
    }

    @Test
    void fromByteSizeWithMB() {
        assertEquals("1 MB", StringUtils.fromByteSize(1465854));
        assertEquals("140 MB", StringUtils.fromByteSize(146493025));
        assertEquals("807 MB", StringUtils.fromByteSize(846493025));
    }

    @Test
    void fromByteSizeWithGB() {
        assertEquals("1.4 GB", StringUtils.fromByteSize(1462534105L));
        assertEquals("788.1 GB", StringUtils.fromByteSize(846253441085L));
    }

    @Test
    void fromByteSizeWithTB() {
        assertEquals("1.33 TB", StringUtils.fromByteSize(1462534105523L));
        assertEquals("769.66 TB", StringUtils.fromByteSize(846253441081234L));
    }

    @Test
    void fromByteSizeWithPB() {
        assertEquals("1.299 PB", StringUtils.fromByteSize(1462534105523419L));
        assertEquals("751.624 PB", StringUtils.fromByteSize(846253441081234435L));
    }

    @Test
    void md5() {
        assertEquals("0832c1202da8d382318e329a7c133ea0",
                StringUtils.md5("cats"));
    }

    @Test
    void randomAlphanumeric() {
        String string = StringUtils.randomAlphanumeric(52);
        assertEquals(52, string.length());
        assertTrue(string.matches("[A-Za-z0-9]+"));
    }

    @Test
    void removeTrailingZeroes() {
        // with floats
        assertEquals("0", StringUtils.removeTrailingZeroes(0.0f));
        assertEquals("0.5", StringUtils.removeTrailingZeroes(0.5f));
        assertEquals("50", StringUtils.removeTrailingZeroes(50.0f));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.5f));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.50f));
        assertTrue(StringUtils.removeTrailingZeroes(50.5555555555555f).length() <= 13);

        // with doubles
        assertEquals("0", StringUtils.removeTrailingZeroes(0.0));
        assertEquals("0.5", StringUtils.removeTrailingZeroes(0.5));
        assertEquals("50", StringUtils.removeTrailingZeroes(50.0));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.5));
        assertEquals("50.5", StringUtils.removeTrailingZeroes(50.50));
        assertTrue(StringUtils.removeTrailingZeroes(50.5555555555555).length() <= 13);
    }

    @Test
    void reverse() {
        assertEquals("321stac", StringUtils.reverse("cats123"));
    }

    @Test
    void sanitize1() {
        assertEquals("", StringUtils.sanitize("dirt", "dirt", "dirt"));
        assertEquals("y", StringUtils.sanitize("dirty", "dirt", "dirt"));
        assertEquals("dirty", StringUtils.sanitize("dir1ty", "1", "1"));

        // test injection
        assertEquals("", StringUtils.sanitize("cacacatststs", "cats", "cats"));
        assertEquals("", StringUtils.sanitize("cadocadogstsgsts", "cats", "dogs", "foxes"));
    }

    @Test
    void sanitize2() {
        assertEquals("", StringUtils.sanitize("dirt", Pattern.compile("dirt")));
        assertEquals("y", StringUtils.sanitize("dirty", Pattern.compile("dirt")));
        assertEquals("dirty", StringUtils.sanitize("dir1ty", Pattern.compile("1")));

        // test injection
        assertEquals("", StringUtils.sanitize("cacacatststs",
                Pattern.compile("cats")));
        assertEquals("", StringUtils.sanitize("cadocadogstsgsts",
                Pattern.compile("cats"), Pattern.compile("dogs")));
    }

    @Test
    void stripEndWithMatch() {
        String str = "ababab";
        String toStrip = "ab";
        assertEquals("abab", StringUtils.stripEnd(str, toStrip));
    }

    @Test
    void stripEndWithoutMatch() {
        String str = "ababab";
        String toStrip = "c";
        assertSame(str, StringUtils.stripEnd(str, toStrip));

        toStrip = "longer than str";
        assertSame(str, StringUtils.stripEnd(str, toStrip));
    }

    @Test
    void stripStartWithMatch() {
        String str = "abcdefg";
        String toStrip = "ab";
        assertEquals("cdefg", StringUtils.stripStart(str, toStrip));
    }

    @Test
    void stripStartWithoutMatch() {
        String str = "ababab";
        String toStrip = "c";
        assertSame(str, StringUtils.stripStart(str, toStrip));

        toStrip = "longer than str";
        assertSame(str, StringUtils.stripStart(str, toStrip));
    }

    @Test
    void toBooleanWithNullValue() {
        assertThrows(NumberFormatException.class,
                () -> StringUtils.toBoolean(null));
    }

    @Test
    void toBooleanWithUnrecognizedValue() {
        Exception e = assertThrows(NumberFormatException.class,
                () -> StringUtils.toBoolean("cats"));
        assertEquals("Not a boolean: cats", e.getMessage());
    }

    @Test
    void toBooleanWithRecognizedValue() {
        assertFalse(StringUtils.toBoolean("0"));
        assertFalse(StringUtils.toBoolean("false"));
        assertTrue(StringUtils.toBoolean("1"));
        assertTrue(StringUtils.toBoolean("true"));
    }

    @Test
    void toByteSizeWithIllegalArgument() {
        assertThrows(NumberFormatException.class,
                () -> StringUtils.toByteSize("cats"));
    }

    @Test
    void toByteSizeWithNumber() {
        assertEquals(254254254, StringUtils.toByteSize("254254254"));
        assertEquals(255, StringUtils.toByteSize("254.9"));
        assertEquals(-255, StringUtils.toByteSize("-254.9"));
    }

    @Test
    void toByteSizeWithKB() {
        long expected = 25 * 1024;
        assertEquals(expected, StringUtils.toByteSize("25K"));
        assertEquals(expected, StringUtils.toByteSize("25KB"));
        assertEquals(expected, StringUtils.toByteSize("25k"));
        assertEquals(expected, StringUtils.toByteSize("25kb"));
        assertEquals(expected, StringUtils.toByteSize("25 K"));
        assertEquals(expected, StringUtils.toByteSize("25 KB"));
        assertEquals(expected, StringUtils.toByteSize("25 k"));
        assertEquals(expected, StringUtils.toByteSize("25 kb"));
    }

    @Test
    void toByteSizeWithMB() {
        long expected = 25 * (long) Math.pow(1024, 2);
        assertEquals(expected, StringUtils.toByteSize("25M"));
        assertEquals(expected, StringUtils.toByteSize("25MB"));
        assertEquals(expected, StringUtils.toByteSize("25m"));
        assertEquals(expected, StringUtils.toByteSize("25mb"));
        assertEquals(expected, StringUtils.toByteSize("25 M"));
        assertEquals(expected, StringUtils.toByteSize("25 MB"));
        assertEquals(expected, StringUtils.toByteSize("25 m"));
        assertEquals(expected, StringUtils.toByteSize("25 mb"));
    }

    @Test
    void toByteSizeWithGB() {
        long expected = 25 * (long) Math.pow(1024, 3);
        assertEquals(expected, StringUtils.toByteSize("25G"));
        assertEquals(expected, StringUtils.toByteSize("25GB"));
        assertEquals(expected, StringUtils.toByteSize("25g"));
        assertEquals(expected, StringUtils.toByteSize("25gb"));
        assertEquals(expected, StringUtils.toByteSize("25 G"));
        assertEquals(expected, StringUtils.toByteSize("25 GB"));
        assertEquals(expected, StringUtils.toByteSize("25 g"));
        assertEquals(expected, StringUtils.toByteSize("25 gb"));
    }

    @Test
    void toByteSizeWithTB() {
        long expected = 25 * (long) Math.pow(1024, 4);
        assertEquals(expected, StringUtils.toByteSize("25T"));
        assertEquals(expected, StringUtils.toByteSize("25TB"));
        assertEquals(expected, StringUtils.toByteSize("25t"));
        assertEquals(expected, StringUtils.toByteSize("25tb"));
        assertEquals(expected, StringUtils.toByteSize("25 T"));
        assertEquals(expected, StringUtils.toByteSize("25 TB"));
        assertEquals(expected, StringUtils.toByteSize("25 t"));
        assertEquals(expected, StringUtils.toByteSize("25 tb"));
    }

    @Test
    void toByteSizeWithPB() {
        long expected = 25 * (long) Math.pow(1024, 5);
        assertEquals(expected, StringUtils.toByteSize("25P"));
        assertEquals(expected, StringUtils.toByteSize("25PB"));
        assertEquals(expected, StringUtils.toByteSize("25p"));
        assertEquals(expected, StringUtils.toByteSize("25pb"));
        assertEquals(expected, StringUtils.toByteSize("25 P"));
        assertEquals(expected, StringUtils.toByteSize("25 PB"));
        assertEquals(expected, StringUtils.toByteSize("25 p"));
        assertEquals(expected, StringUtils.toByteSize("25 pb"));
    }

    @Test
    void toHexWithEmptyArgument() {
        byte[] bytes = {};
        String actual = StringUtils.toHex(bytes);
        assertEquals("", actual);
    }

    @Test
    void toHex() {
        byte[] bytes = { 0x00, 0x12, (byte) 0xa5, (byte) 0xff };
        String actual = StringUtils.toHex(bytes);
        assertEquals("0012a5ff", actual);
    }

    @Test
    void wrap() {
        String str = "This is a very very very very very very very very long line.";
        final int maxWidth        = 200;
        final BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d      = image.createGraphics();

        final Map<TextAttribute, Object> attributes = Map.of(
                TextAttribute.FAMILY, "Helvetica",
                TextAttribute.SIZE, 18,
                TextAttribute.WEIGHT, 1,
                TextAttribute.TRACKING, 0);
        final Font font = Font.getFont(attributes);
        g2d.setFont(font);
        final FontMetrics fm = g2d.getFontMetrics();

        List<String> lines = StringUtils.wrap(str, fm, maxWidth);
        assertTrue(lines.size() > 2 && lines.size() < 6);
        assertTrue(lines.get(0).length() > 10 && lines.get(0).length() < 30);
        assertTrue(lines.get(1).length() > 10 && lines.get(1).length() < 30);
        assertTrue(lines.get(2).length() > 5 && lines.get(2).length() < 30);
    }

}
