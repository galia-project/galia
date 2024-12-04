package is.galia.util;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoftwareVersionTest extends BaseTest {

    /* parse() */

    @Test
    void parseWithInvalidString() {
        assertThrows(IllegalArgumentException.class,
                () -> SoftwareVersion.parse("a"));
    }

    @Test
    void parseWithMajorVersion() {
        SoftwareVersion version;
        version = SoftwareVersion.parse("5");
        assertEquals(new SoftwareVersion(5), version);
        assertEquals("5.0", version.toString());
    }

    @Test
    void parseWithMinorVersion() {
        SoftwareVersion version;
        version = SoftwareVersion.parse("5.0");
        assertEquals(new SoftwareVersion(5, 0), version);
        assertEquals("5.0", version.toString());
    }

    @Test
    void parseWithPatchVersion() {
        SoftwareVersion version = SoftwareVersion.parse("5.6.2");
        assertEquals(new SoftwareVersion(5, 6, 2), version);
        assertEquals("5.6.2", version.toString());
    }

    @Test
    void parseWithMajorQualifier() {
        SoftwareVersion version = SoftwareVersion.parse("5-SNAPSHOT");
        assertEquals(new SoftwareVersion(5, 0, 0, "-SNAPSHOT"), version);
        assertEquals("5.0-SNAPSHOT", version.toString());
    }

    @Test
    void parseWithMinorQualifier() {
        SoftwareVersion version = SoftwareVersion.parse("5.6-SNAPSHOT");
        assertEquals(new SoftwareVersion(5, 6, 0, "-SNAPSHOT"), version);
        assertEquals("5.6-SNAPSHOT", version.toString());
    }

    @Test
    void parseWithPatchQualifier() {
        SoftwareVersion version = SoftwareVersion.parse("5.6.2-SNAPSHOT");
        assertEquals(new SoftwareVersion(5, 6, 2, "-SNAPSHOT"), version);
        assertEquals("5.6.2-SNAPSHOT", version.toString());
    }

    /* compareTo() */

    @Test
    void compareTo() {
        assertEquals(-1, new SoftwareVersion(4).compareTo(new SoftwareVersion(5)));
        assertEquals(-1, new SoftwareVersion(5).compareTo(new SoftwareVersion(5, 1)));
        assertEquals(-1, new SoftwareVersion(5, 1).compareTo(new SoftwareVersion(5, 1, 3)));
        assertEquals(-1, new SoftwareVersion(5, 1, 3, "-beta5").compareTo(new SoftwareVersion(5, 1, 3)));
        assertEquals(-1, new SoftwareVersion(5, 1, 3, "-beta5").compareTo(new SoftwareVersion(5, 1, 3, "-beta6")));
        assertEquals(0, new SoftwareVersion(5).compareTo(new SoftwareVersion(5)));
        assertEquals(1, new SoftwareVersion(5).compareTo(new SoftwareVersion(4)));
        assertEquals(1, new SoftwareVersion(5, 1).compareTo(new SoftwareVersion(5)));
        assertEquals(1, new SoftwareVersion(5, 1, 3).compareTo(new SoftwareVersion(5, 1)));
        assertEquals(1, new SoftwareVersion(5, 1, 3).compareTo(new SoftwareVersion(5, 1, 3, "-beta5")));
        assertEquals(1, new SoftwareVersion(5, 1, 3, "-beta6").compareTo(new SoftwareVersion(5, 1, 3, "-beta5")));
    }

    /* isGreaterThan() */

    @Test
    void isGreaterThan() {
        assertTrue(new SoftwareVersion(5).isGreaterThan(new SoftwareVersion(4)));
        assertTrue(new SoftwareVersion(5, 1).isGreaterThan(new SoftwareVersion(5)));
        assertTrue(new SoftwareVersion(5, 1, 3).isGreaterThan(new SoftwareVersion(5, 1)));
        assertTrue(new SoftwareVersion(5, 1, 3).isGreaterThan(new SoftwareVersion(5, 1, 3, "-beta5")));
        assertTrue(new SoftwareVersion(5, 1, 3, "-beta6").isGreaterThan(new SoftwareVersion(5, 1, 3, "-beta5")));
    }

    /* toString() */

    @Test
    void testToString() {
        assertEquals("0.5", new SoftwareVersion(0, 5).toString());
        assertEquals("5.0", new SoftwareVersion(5).toString());
        assertEquals("5.2", new SoftwareVersion(5, 2).toString());
        assertEquals("5.2.7", new SoftwareVersion(5, 2, 7).toString());
        assertEquals("5.2.7rc3", new SoftwareVersion(5, 2, 7, "rc3").toString());
    }

}
