package is.galia.stream;

import is.galia.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathImageInputStreamTest extends BaseTest {

    @Test
    void pathConstructorWithNonexistentFile() {
        assertThrows(NoSuchFileException.class,
                () -> new PathImageInputStream(Path.of("bogus")));
    }

}
