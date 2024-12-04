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

package is.galia;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineArgumentParserTest extends BaseTest {

    /* parse() */

    @Test
    void parseWithMultipleArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""),
                new CommandLineArgument(
                        "arg2", CommandLineArgument.Type.BOOLEAN, true, false, ""),
                new CommandLineArgument(
                        "arg3", CommandLineArgument.Type.STRING, true, false, ""),
                new CommandLineArgument(
                        "arg4", CommandLineArgument.Type.INTEGER, true, false, ""));
        Map<String,Object> result = parser.parse(
                "-arg1", "-arg2", "true", "-arg3", "cats", "-arg4", "534");
        assertNull(result.get("arg1"));
        assertTrue((boolean) result.get("arg2"));
        assertEquals("cats", result.get("arg3"));
        assertEquals(534, result.get("arg4"));
    }

    @Test
    void parseWithMultipleArgumentsOutOfOrder() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""),
                new CommandLineArgument(
                        "arg2", CommandLineArgument.Type.BOOLEAN, true, false, ""),
                new CommandLineArgument(
                        "arg3", CommandLineArgument.Type.STRING, true, false, ""),
                new CommandLineArgument(
                        "arg4", CommandLineArgument.Type.INTEGER, true, false, ""));
        Map<String,Object> result = parser.parse(
                "-arg4", "534", "-arg2", "true", "-arg3", "cats", "-arg1");
        assertNull(result.get("arg1"));
        assertTrue((boolean) result.get("arg2"));
        assertEquals("cats", result.get("arg3"));
        assertEquals(534, result.get("arg4"));
    }

    @Test
    void parseWithMultiDashArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""),
                new CommandLineArgument(
                        "arg2", CommandLineArgument.Type.BOOLEAN, true, false, ""));
        Map<String,Object> result = parser.parse("--arg1", "---arg2", "true");
        assertNull(result.get("arg1"));
        assertTrue((boolean) result.get("arg2"));
    }

    @Test
    void parseWithVaryingCaseArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "lowercase", CommandLineArgument.Type.STRING, true, false, ""),
                new CommandLineArgument(
                        "CamelCase", CommandLineArgument.Type.STRING, true, false, ""));
        Map<String,Object> result = parser.parse(
                "-LoWeRcAsE", "cats", "-CamelCASE", "dogs");
        assertEquals("cats", result.get("lowercase"));
        assertEquals("dogs", result.get("CamelCase"));
    }

    @Test
    void parseWithMultipleSameArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.STRING, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class,
                () -> parser.parse("-arg1", "cats", "-arg1", "dogs"));
        assertEquals("Multiple arguments supplied for arg1",
                e.getMessage());
    }

    @Test
    void parseWithMultipleValuesForSameArgument() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.STRING, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class,
                () -> parser.parse("-arg1", "cats", "dogs"));
        assertEquals("Multiple values supplied for argument: arg1",
                e.getMessage());
    }

    @Test
    void parseWithExtraWhitespace() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""),
                new CommandLineArgument(
                        "arg2", CommandLineArgument.Type.BOOLEAN, true, false, ""),
                new CommandLineArgument(
                        "arg3", CommandLineArgument.Type.STRING, true, false, ""),
                new CommandLineArgument(
                        "arg4", CommandLineArgument.Type.INTEGER, true, false, ""));
        Map<String,Object> result = parser.parse(
                " -arg1 ", "\t-arg2\t", "\ttrue ", " -arg3 ", "  cats  ", " -arg4 ", "  534  ");
        assertNull(result.get("arg1"));
        assertTrue((boolean) result.get("arg2"));
        assertEquals("cats", result.get("arg3"));
        assertEquals(534, result.get("arg4"));
    }

    @Test
    void parseWithMissingRequiredArgument() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class, () ->
            parser.parse("-arg2", "cats"));
        assertEquals("Unrecognized argument: -arg2", e.getMessage());
    }

    @Test
    void parseWithNoValueSuppliedForRequiredArgument() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.STRING, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class, () ->
                parser.parse("-arg1"));
        assertEquals("No value supplied for argument -arg1", e.getMessage());
    }

    @Test
    void parseWithIndependentArgumentWhenOthersAreRequired() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "required", CommandLineArgument.Type.STRING, true, false, ""),
                new CommandLineArgument(
                        "independent", CommandLineArgument.Type.FLAG_ONLY, false, true, ""));
        Map<String,Object> result = parser.parse("-independent");
        assertNull(result.get("independent"));
    }

    @Test
    void parseWithUnrecognizedArgument() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class, () ->
                parser.parse("-bogus", "cats"));
        assertEquals("Unrecognized argument: -bogus", e.getMessage());
    }

    @Test
    void parseParsesFlagOnlyArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""));
        Map<String,Object> result = parser.parse("-arg1");
        assertNull(result.get("arg1"));
    }

    @Test
    void parseThrowsWhenAValueIsGivenForAFlagOnlyArgument() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.FLAG_ONLY, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class,
                () -> parser.parse("-arg1", "cats"));
        assertEquals("A value was provided for a flag-only argument: arg1",
                e.getMessage());
    }

    @Test
    void parseParsesBooleanArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.BOOLEAN, true, false, ""));
        Map<String,Object> result = parser.parse("-arg1", "true");
        assertTrue((boolean) result.get("arg1"));
        result = parser.parse("-arg1", "false");
        assertFalse((boolean) result.get("arg1"));
    }

    @Test
    void parseWithNonBooleanGivenForBooleanArgument() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.BOOLEAN, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class,
                () -> parser.parse("-arg1", "notabool"));
        assertEquals("Not a boolean: notabool", e.getMessage());
    }

    @Test
    void parseParsesStringArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.STRING, true, false, ""));
        Map<String,Object> result = parser.parse("-arg1", "cats");
        assertEquals("cats", result.get("arg1"));
    }

    @Test
    void parseParsesIntegerArguments() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.INTEGER, true, false, ""));
        Map<String,Object> result = parser.parse("-arg1", "342");
        assertEquals(342, result.get("arg1"));
    }

    @Test
    void parseWithNonIntegerGivenForIntegerArgument() {
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1", CommandLineArgument.Type.INTEGER, true, false, ""));
        Exception e = assertThrows(CommandLineArgumentException.class,
                () -> parser.parse("-arg1", "notanint"));
        assertEquals("Not an integer: notanint", e.getMessage());
    }

    /* usage() */

    @Test
    void usage() {
        final String n = System.lineSeparator();
        CommandLineArgumentParser parser = new CommandLineArgumentParser(
                new CommandLineArgument(
                        "arg1",
                        CommandLineArgument.Type.FLAG_ONLY,
                        false,
                        false,
                        "This is arg1"),
                new CommandLineArgument(
                        "superDuperLongArg2",
                        CommandLineArgument.Type.BOOLEAN,
                        true,
                        false,
                        "This is arg2"),
                new CommandLineArgument(
                        "arg3",
                        CommandLineArgument.Type.STRING,
                        false,
                        false,
                        "This is arg3"),
                new CommandLineArgument(
                        "arg4",
                        CommandLineArgument.Type.INTEGER,
                        true,
                        false,
                        "This is arg4"));
        String expected =
                "Usage: java <VM args> -jar " +
                        Application.getJARFile().getFileName().toString() +
                        " <command args>" + n +
                        n +
                        "Command arguments:" + n +
                        "-arg1                This is arg1" + n +
                        "-arg3                This is arg3" + n +
                        "-arg4                This is arg4 (REQUIRED)" + n +
                        "-superDuperLongArg2  This is arg2 (REQUIRED)" + n;
        assertEquals(expected, parser.usage());
    }

}
