package is.galia.stream;

import is.galia.Application;
import is.galia.test.BaseTest;
import is.galia.test.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RandomAccessFileTest extends BaseTest {

    // use small buffer size for test cases
    private static final int TEST_BUFFER_SIZE = 10;

    // test file
    private static RandomAccessFile testFile;
    private static final String TEST_FILE_PATH = TestUtils.getFixture("utf8.txt").toString();

    // contents of test file
    private static final String TEST_FILE_STRING = "Hello world, this is a test.\r\nThis is a second line of text.";
    private static final byte[] UTF8_BYTES = TEST_FILE_STRING.getBytes(StandardCharsets.UTF_8);
    private static final long TEST_FILE_LENGTH = UTF8_BYTES.length;

    // first three values in test file when reading as short, int, long, float, and double
    private static final short[] DATA_AS_LE_SHORTS = new short[] {25928, 27756, 8303};
    private static final short[] DATA_AS_BE_SHORTS = new short[] {18533, 27756, 28448};
    private static final int[] DATA_AS_LE_INTS = new int[] {1819043144, 1870078063, 744778866};
    private static final int[] DATA_AS_BE_INTS = new int[] {1214606444, 1864398703, 1919706156};
    private static final long[] DATA_AS_LE_LONGS =
            new long[] {Long.parseLong("8031924123371070792"), Long.parseLong("7595448453092895858"), Long.parseLong("8367794899657498739")};
    private static final long[] DATA_AS_BE_LONGS =
            new long[] {Long.parseLong("5216694956355254127"), Long.parseLong("8245075158494373993"), Long.parseLong("8295746456801845364")};
    private static final float[] DATA_AS_LE_FLOATS =
            new float[] {Float.parseFloat("1.1431391e+27"), Float.parseFloat("7.6482007e+28"), Float.parseFloat("3.2460948e-12")};
    private static final float[] DATA_AS_BE_FLOATS =
            new float[] {Float.parseFloat("234929.69"), Float.parseFloat("4.9661988e+28"), Float.parseFloat("4.682212e+30")};
    private static final double[] DATA_AS_LE_DOUBLES = new double[] {Double.parseDouble("8.765776478827854e+228"),
            Double.parseDouble("5.849385300349674e+199"), Double.parseDouble("2.345440516152973e+251")};
    private static final double[] DATA_AS_BE_DOUBLES = new double[] {Double.parseDouble("5.832039480691944e+40"),
            Double.parseDouble("1.51450869579011e+243"), Double.parseDouble("3.585961897485533e+246")};


    @BeforeAll
    public static void beforeClass() throws IOException {
        testFile = new RandomAccessFile(TEST_FILE_PATH, "r", TEST_BUFFER_SIZE);
    }

    @AfterAll
    public static void cleanUpTest() throws IOException {
        testFile.close();
    }

    private static Path newTempFile() {
        try {
            return Files.createTempFile(Application.getTempDir(),
                    RandomAccessFileTest.class.getSimpleName(),
                    "tmp");
        } catch (IOException e) {
            fail(e.getMessage(), e);
            return null;
        }
    }

    ////////////////////
    // test reads for persistent test files
    // no changes made to file

    @Test
    void testSetBufferSize() {
        int expected = 100;
        testFile.setBufferSize(expected);
        assertEquals(expected, testFile.getBufferSize());
        testFile.setBufferSize(TEST_BUFFER_SIZE);
        assertEquals(TEST_BUFFER_SIZE, testFile.getBufferSize());
    }

    @Test
    void testIsAtEndOfFile() throws IOException {
        testFile.seek(TEST_FILE_LENGTH);
        assertTrue(testFile.isAtEndOfFile());
        testFile.seek(0);
        assertFalse(testFile.isAtEndOfFile());
    }

    @Test
    void testSeek() throws IOException {
        long pos = 5;
        testFile.seek(pos);
        assertEquals(pos, testFile.getFilePointer());
        pos = 15;
        testFile.seek(pos);
        assertEquals(pos, testFile.getFilePointer());
    }

    @Test
    void testOrder() throws IOException {
        testFile.order(ByteOrder.LITTLE_ENDIAN);
        testFile.seek(0);
        assertNotEquals(DATA_AS_BE_INTS[0], testFile.readInt());
        testFile.order(ByteOrder.BIG_ENDIAN);
        assertEquals(DATA_AS_BE_INTS[1], testFile.readInt());
    }

    @Test
    void testGetLocation() {
        assertEquals(TEST_FILE_PATH, testFile.getLocation());
    }

    @Test
    void testLength() throws IOException {
        assertEquals(TEST_FILE_LENGTH, testFile.length());
    }

    @Test
    void testRead() throws IOException {
        int pos = 0;
        testFile.seek(pos);
        assertEquals((int) UTF8_BYTES[pos], testFile.read());
        pos = 15;
        testFile.seek(pos);
        assertEquals((int) UTF8_BYTES[pos], testFile.read());
        testFile.seek(TEST_FILE_LENGTH);
        assertEquals(-1, testFile.read());
    }

    @Test
    void testReadBytes() throws IOException {
        int offset = 0;
        byte[] buff;
        int len;
        int n;

        // read byte
        testFile.seek(0);
        assertEquals(UTF8_BYTES[0], testFile.readByte());
        // read unsigned byte
        assertEquals(UTF8_BYTES[1], testFile.readUnsignedByte());

        // read across buffer
        testFile.seek(0);
        len = TEST_BUFFER_SIZE + 1;
        buff = new byte[len];
        n = testFile.readBytes(buff, offset, len);
        assertEquals(len, n);
        assertTrue(arraysMatch(buff, UTF8_BYTES, 0, 0, len));

        // read with offset
        testFile.seek(0);
        offset = 2;
        len = len - offset;
        n = testFile.readBytes(buff, offset, len);
        assertEquals(len, n);
        assertEquals(UTF8_BYTES[0], buff[0]);
        assertTrue(arraysMatch(buff, UTF8_BYTES, offset, 0, len));

        // read directly from file (more than an extra buffer length)
        testFile.seek(0);
        offset = 0;
        len = (TEST_BUFFER_SIZE * 2) + 1;
        buff = new byte[len];
        n = testFile.readBytes(buff, offset, len);
        assertEquals(len, n);
        assertTrue(arraysMatch(buff, UTF8_BYTES, 0, 0, len));

        // read over end of file
        len = 2;
        testFile.seek(TEST_FILE_LENGTH - 1);
        buff = new byte[len];
        n = testFile.readBytes(buff, offset, len);
        assertTrue(n < len);

        // read at end of file
        testFile.seek(TEST_FILE_LENGTH);
        n = testFile.readBytes(buff, offset, len);
        assertEquals(-1, n);
    }

    @Test
    void testReadToByteChannel() throws IOException {
        TestWritableByteChannel dest;
        byte[] out;
        long n;
        int nbytes = 10;
        int offset = 0;

        // test read
        dest = new TestWritableByteChannel();
        n = testFile.readToByteChannel(dest, offset, nbytes);
        assertEquals(nbytes, n);
        out = dest.getBytes();
        // spot check first and last byte
        assertTrue(arraysMatch(out, UTF8_BYTES, 0, 0, (int) n));
        dest.reset();

        // test read with offset
        offset = 10;
        n = testFile.readToByteChannel(dest, offset, nbytes);
        assertEquals(nbytes, n);
        out = dest.getBytes();
        assertTrue(arraysMatch(out, UTF8_BYTES, 0, offset, (int) n));
        dest.reset();

        // test read past EOF
        offset = (int) TEST_FILE_LENGTH - nbytes + 1;
        n = testFile.readToByteChannel(dest, offset, nbytes);
        assertTrue(n < nbytes);
    }

    @Test
    void testReadClosedRaf() throws IOException {
        RandomAccessFile closedTempFile = new RandomAccessFile(TEST_FILE_PATH, "r", TEST_BUFFER_SIZE);
        int n = 1;
        byte[] expected = new byte[TEST_BUFFER_SIZE + n];
        System.arraycopy(UTF8_BYTES, 0, expected, 0, TEST_BUFFER_SIZE);
        System.arraycopy(new byte[n], 0, expected, TEST_BUFFER_SIZE, n);

        closedTempFile.seek(0);
        closedTempFile.close();

        byte[] actual = new byte[TEST_BUFFER_SIZE + n];
        closedTempFile.read(actual, 0, TEST_BUFFER_SIZE + n);
        assertTrue(arraysMatch(expected, actual, 0, 0, TEST_BUFFER_SIZE + n));
    }

    @Test
    void testReadFully() throws IOException {
        // read fully, buff < file length
        testFile.seek(0);
        int len = 11;
        byte[] buff = new byte[len];
        testFile.readFully(buff);
        assertTrue(arraysMatch(buff, UTF8_BYTES, 0, 0, len));

        // read fully, buff > file length
        testFile.seek(0);
        len = (int) TEST_FILE_LENGTH + 1;
        byte[] finalBuff = new byte[len];
        assertThrows(EOFException.class, () -> testFile.readFully(finalBuff));

        // read fully with offset
        testFile.seek(0);
        int offset = 5;
        len = 11 - offset;
        testFile.readFully(buff, offset, len);
        assertTrue(arraysMatch(buff, UTF8_BYTES, 0, 0, offset));
        assertTrue(arraysMatch(buff, UTF8_BYTES, offset, 0, len));
    }

    @Test
    void testSkipBytes() throws IOException {
        testFile.seek(0);
        int skip = 5;
        testFile.skipBytes(skip);
        assertEquals(skip, testFile.getFilePointer());
        int val = testFile.read();
        assertEquals(UTF8_BYTES[skip], val);
    }

    @Test
    void testUnread() throws IOException {
        testFile.seek(0);
        int a = testFile.read();
        assertEquals(1, testFile.getFilePointer());
        testFile.unread();
        assertEquals(0, testFile.getFilePointer());
        int b = testFile.read();
        assertEquals(b, a);
    }

    @Test
    void testReadLittleEndian() throws IOException {
        // set byte order
        testFile.order(ByteOrder.LITTLE_ENDIAN);

        // read boolean
        assertTrue(testFile.readBoolean());

        // read short
        testFile.seek(0);
        assertEquals(DATA_AS_LE_SHORTS[0], testFile.readShort());
        // read short array
        short[] outShort = new short[2];
        testFile.readShort(outShort, 0, 2);
        assertEquals(DATA_AS_LE_SHORTS[1], outShort[0]);
        assertEquals(DATA_AS_LE_SHORTS[2], outShort[1]);
        // read unsigned short
        testFile.seek(0);
        assertEquals(DATA_AS_LE_SHORTS[0], testFile.readUnsignedShort());

        // read char
        assertEquals((char) DATA_AS_LE_SHORTS[1], testFile.readChar());

        // read int
        testFile.seek(0);
        assertEquals(DATA_AS_LE_INTS[0], testFile.readInt());
        // read int array
        int[] outInt = new int[2];
        testFile.readInt(outInt, 0, 2);
        assertEquals(DATA_AS_LE_INTS[1], outInt[0]);
        assertEquals(DATA_AS_LE_INTS[2], outInt[1]);
        // read int unbuffered
        assertEquals(DATA_AS_LE_INTS[1], testFile.readIntUnbuffered(4));

        // read long
        testFile.seek(0);
        assertEquals(DATA_AS_LE_LONGS[0], testFile.readLong());
        // read long array
        long[] outLong = new long[2];
        testFile.readLong(outLong, 0, 2);
        assertEquals(DATA_AS_LE_LONGS[1], outLong[0]);
        assertEquals(DATA_AS_LE_LONGS[2], outLong[1]);

        // read float
        testFile.seek(0);
        assertTrue(compareFloats(testFile.readFloat(), DATA_AS_LE_FLOATS[0]));
        // read float array
        float[] outFloat = new float[2];
        testFile.readFloat(outFloat, 0, 2);
        assertTrue(compareFloats(outFloat[0], DATA_AS_LE_FLOATS[1]));
        assertTrue(compareFloats(outFloat[1], DATA_AS_LE_FLOATS[2]));

        // read double
        testFile.seek(0);
        assertTrue(compareDoubles(testFile.readDouble(), DATA_AS_LE_DOUBLES[0]));
        // read double array
        double[] outDouble = new double[2];
        testFile.readDouble(outDouble, 0, 2);
        assertTrue(compareDoubles(outDouble[0], DATA_AS_LE_DOUBLES[1]));
        assertTrue(compareDoubles(outDouble[1], DATA_AS_LE_DOUBLES[2]));
    }

    @Test
    void testReadBigEndian() throws IOException {
        // set byte order
        testFile.order(ByteOrder.BIG_ENDIAN);

        // read boolean
        assertTrue(testFile.readBoolean());

        // read short
        testFile.seek(0);
        assertEquals(DATA_AS_BE_SHORTS[0], testFile.readShort());
        // read short array
        short[] outShort = new short[2];
        testFile.readShort(outShort, 0, 2);
        assertEquals(DATA_AS_BE_SHORTS[1], outShort[0]);
        assertEquals(DATA_AS_BE_SHORTS[2], outShort[1]);
        // read unsigned short
        testFile.seek(0);
        assertEquals(DATA_AS_BE_SHORTS[0], testFile.readUnsignedShort());

        // read char
        assertEquals((char) DATA_AS_BE_SHORTS[1], testFile.readChar());

        // read int
        testFile.seek(0);
        assertEquals(DATA_AS_BE_INTS[0], testFile.readInt());
        // read int array
        int[] outInt = new int[2];
        testFile.readInt(outInt, 0, 2);
        assertEquals(DATA_AS_BE_INTS[1], outInt[0]);
        assertEquals(DATA_AS_BE_INTS[2], outInt[1]);
        // read int unbuffered
        assertEquals(DATA_AS_BE_INTS[1], testFile.readIntUnbuffered(4));

        // read long
        testFile.seek(0);
        assertEquals(DATA_AS_BE_LONGS[0], testFile.readLong());
        // read long array
        long[] outLong = new long[2];
        testFile.readLong(outLong, 0, 2);
        assertEquals(DATA_AS_BE_LONGS[1], outLong[0]);
        assertEquals(DATA_AS_BE_LONGS[2], outLong[1]);

        // read float
        testFile.seek(0);
        assertTrue(compareFloats(testFile.readFloat(), DATA_AS_BE_FLOATS[0]));
        // read float array
        float[] outFloat = new float[2];
        testFile.readFloat(outFloat, 0, 2);
        assertTrue(compareFloats(outFloat[0], DATA_AS_BE_FLOATS[1]));
        assertTrue(compareFloats(outFloat[1], DATA_AS_BE_FLOATS[2]));

        // read double
        testFile.seek(0);
        assertTrue(compareDoubles(testFile.readDouble(), DATA_AS_BE_DOUBLES[0]));
        // read double array
        double[] outDouble = new double[2];
        testFile.readDouble(outDouble, 0, 2);
        assertTrue(compareDoubles(outDouble[0], DATA_AS_BE_DOUBLES[1]));
        assertTrue(compareDoubles(outDouble[1], DATA_AS_BE_DOUBLES[2]));
    }

    @Test
    void testReadStringUTF8() throws IOException {
        // read line
        testFile.seek(0);
        int linebreak = TEST_FILE_STRING.indexOf("\r\n");
        assertEquals(TEST_FILE_STRING.substring(0, linebreak),
                testFile.readLine());
        assertEquals(TEST_FILE_STRING.substring(linebreak + 2),
                testFile.readLine());

        // read string
        int nbytes = 11;
        testFile.seek(0);
        assertEquals(TEST_FILE_STRING.substring(0, nbytes),
                testFile.readString(nbytes));

        // read string max
        testFile.seek(0);
        assertEquals(TEST_FILE_STRING,
                testFile.readStringMax((int) TEST_FILE_LENGTH));
    }

    @Test
    void testToString() {
        assertEquals(TEST_FILE_PATH, testFile.toString());
    }

    @Test
    void testSearchForward() throws IOException {
        testFile.seek(0);
        // test match found
        KMPMatch match = new KMPMatch("world".getBytes(StandardCharsets.UTF_8));
        assertTrue(testFile.searchForward(match, -1));

        // test match not reached
        testFile.seek(0);
        assertFalse(testFile.searchForward(match, 5));

        // test match not found
        KMPMatch notMatch = new KMPMatch("not match".getBytes(StandardCharsets.UTF_8));
        assertFalse(testFile.searchForward(notMatch, -1));
    }

    ////////////////////////////
    // test writes on temp file

    @Test
    void testWrite() throws IOException {
        RandomAccessFile writeFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        writeFile.seek(0);
        writeFile.write(0);
        writeFile.seek(0);
        assertEquals(0, writeFile.read());
        // test write byte array
        writeFile.seek(0);
        writeFile.write(UTF8_BYTES);
        writeFile.seek(0);
        int nbytes = UTF8_BYTES.length;
        assertTrue(arraysMatch(writeFile.readBytes(nbytes), UTF8_BYTES, 0, 0, nbytes));
        // test write with offset
        int offset = 5;
        writeFile.write(UTF8_BYTES, offset, nbytes - offset);
        writeFile.seek(nbytes);
        assertTrue(arraysMatch(writeFile.readBytes(nbytes - offset), UTF8_BYTES, 0, offset, nbytes - offset));
        writeFile.close();
    }

    @Test
    void testWriteBytes() throws IOException {
        RandomAccessFile writeFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        writeFile.seek(0);
        // write single byte
        writeFile.writeByte(0);
        writeFile.seek(0);
        assertEquals(0, writeFile.read());
        // test write byte array
        writeFile.seek(0);
        int offset = 0;
        int nbytes = UTF8_BYTES.length;
        writeFile.writeBytes(UTF8_BYTES, offset, nbytes);
        writeFile.seek(0);
        assertTrue(arraysMatch(writeFile.readBytes(nbytes), UTF8_BYTES, 0, 0, nbytes));
        // test write with offset
        offset = 5;
        writeFile.write(UTF8_BYTES, offset, nbytes - offset);
        writeFile.seek(nbytes);
        assertTrue(arraysMatch(writeFile.readBytes(nbytes - offset), UTF8_BYTES, 0, offset, nbytes - offset));
        // test write as string
        writeFile.seek(0);
        writeFile.writeBytes(TEST_FILE_STRING);
        writeFile.seek(0);
        assertTrue(arraysMatch(writeFile.readBytes(nbytes), UTF8_BYTES, 0, 0, nbytes));
        writeFile.close();
    }


    @Test
    void testWriteLittleEndian() throws IOException {
        RandomAccessFile writeFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        writeFile.order(ByteOrder.LITTLE_ENDIAN); // writes are always big endian
        // pos = 0
        writeFile.seek(0);

        // boolean
        writeFile.writeBoolean(true);
        writeFile.writeBoolean(new boolean[] {true, false, false}, 1, 2);
        byte[] expected = new byte[] {(byte) 1, (byte) 0, (byte) 0};
        writeFile.seek(0);
        assertTrue(arraysMatch(writeFile.readBytes(3), expected, 0, 0, 3));
        // 3 bytes written
        // pos = 3

        // short
        writeFile.writeShort(0, ByteOrder.LITTLE_ENDIAN);
        writeFile.writeShort(new short[] {1, 2, 3}, 1, 2, 1);
        short[] expectedShorts = new short[] {0, 2, 3};
        short[] outShorts = new short[3];
        writeFile.seek(3);
        writeFile.readShort(outShorts, 0, outShorts.length);
        assertArrayEquals(expectedShorts, outShorts);
        // 6 bytes written
        // pos = 9

        // int
        writeFile.writeInt(0, 1);
        writeFile.writeInt(new int[] {1, 2, 3}, 1, 2, ByteOrder.LITTLE_ENDIAN);
        int[] expectedInts = new int[] {0, 2, 3};
        int[] outInts = new int[3];
        writeFile.seek(9);
        writeFile.readInt(outInts, 0, outInts.length);
        assertArrayEquals(expectedInts, outInts);
        // 12 bytes written
        // pos = 21

        // long
        writeFile.writeLong(0, ByteOrder.LITTLE_ENDIAN);
        writeFile.writeLong(new long[] {1, 2, 3}, 1, 2, 1);
        long[] expectedLongs = new long[] {0, 2, 3};
        long[] outLongs = new long[3];
        writeFile.seek(21);
        writeFile.readLong(outLongs, 0, outLongs.length);
        assertArrayEquals(expectedLongs, outLongs);
        // 24 bytes written
        // pos = 45

        // float
        writeFile.writeFloat(0, 1);
        writeFile.writeFloat(new float[] {1, 2, 3}, 1, 2, ByteOrder.LITTLE_ENDIAN);
        float[] expectedFloats = new float[] {0, 2, 3};
        float[] outFloats = new float[3];
        writeFile.seek(45);
        writeFile.readFloat(outFloats, 0, outFloats.length);
        assertArrayEquals(expectedFloats, outFloats);
        // 12 bytes written
        // pos = 57

        // double
        writeFile.writeDouble(0, ByteOrder.LITTLE_ENDIAN);
        writeFile.writeDouble(new double[] {1, 2, 3}, 1, 2, 1);
        double[] expectedDoubles = new double[] {0, 2, 3};
        double[] outDoubles = new double[3];
        writeFile.seek(57);
        writeFile.readDouble(outDoubles, 0, outDoubles.length);
        assertArrayEquals(expectedDoubles, outDoubles);
        // 24 bytes written
        // pos = 81

        // char
        writeFile.writeChar(new char[] {1, 2, 3}, 0, 3, ByteOrder.LITTLE_ENDIAN);
        char[] expectedChars = new char[] {1, 2, 3};
        writeFile.seek(81);
        assertEquals(expectedChars[0], writeFile.readChar());
        assertEquals(expectedChars[1], writeFile.readChar());
        assertEquals(expectedChars[2], writeFile.readChar());
        // 6 bytes written
        // pos = 87
        writeFile.writeChars(TEST_FILE_STRING, ByteOrder.LITTLE_ENDIAN);
        writeFile.seek(87);
        assertEquals(TEST_FILE_STRING,
                writeFile.readString((int) TEST_FILE_LENGTH * 2, StandardCharsets.UTF_16LE));

        writeFile.close();
    }

    @Test
    void testWriteBigEndian() throws IOException {
        RandomAccessFile writeFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        writeFile.order(ByteOrder.BIG_ENDIAN); // writes are always big endian
        // pos = 0
        writeFile.seek(0);

        // boolean
        writeFile.writeBoolean(true);
        writeFile.writeBoolean(new boolean[] {true, false, false}, 1, 2);
        byte[] expected = new byte[] {(byte) 1, (byte) 0, (byte) 0};
        writeFile.seek(0);
        assertTrue(arraysMatch(writeFile.readBytes(3), expected, 0, 0, 3));
        // 3 bytes written
        // pos = 3

        // short
        writeFile.writeShort(0, ByteOrder.BIG_ENDIAN);
        writeFile.writeShort(new short[] {1, 2, 3}, 1, 2, 0);
        short[] expectedShorts = new short[] {0, 2, 3};
        short[] outShorts = new short[3];
        writeFile.seek(3);
        writeFile.readShort(outShorts, 0, outShorts.length);
        assertArrayEquals(expectedShorts, outShorts);
        // 6 bytes written
        // pos = 9

        // int
        writeFile.writeInt(0, 0);
        writeFile.writeInt(new int[] {1, 2, 3}, 1, 2, ByteOrder.BIG_ENDIAN);
        int[] expectedInts = new int[] {0, 2, 3};
        int[] outInts = new int[3];
        writeFile.seek(9);
        writeFile.readInt(outInts, 0, outInts.length);
        assertArrayEquals(expectedInts, outInts);
        // 12 bytes written
        // pos = 21

        // long
        writeFile.writeLong(0, ByteOrder.BIG_ENDIAN);
        writeFile.writeLong(new long[] {1, 2, 3}, 1, 2, 0);
        long[] expectedLongs = new long[] {0, 2, 3};
        long[] outLongs = new long[3];
        writeFile.seek(21);
        writeFile.readLong(outLongs, 0, outLongs.length);
        assertArrayEquals(expectedLongs, outLongs);
        // 24 bytes written
        // pos = 45

        // float
        writeFile.writeFloat(0, 0);
        writeFile.writeFloat(new float[] {1, 2, 3}, 1, 2, ByteOrder.BIG_ENDIAN);
        float[] expectedFloats = new float[] {0, 2, 3};
        float[] outFloats = new float[3];
        writeFile.seek(45);
        writeFile.readFloat(outFloats, 0, outFloats.length);
        assertArrayEquals(expectedFloats, outFloats);
        // 12 bytes written
        // pos = 57

        // double
        writeFile.writeDouble(0, ByteOrder.BIG_ENDIAN);
        writeFile.writeDouble(new double[] {1, 2, 3}, 1, 2, 0);
        double[] expectedDoubles = new double[] {0, 2, 3};
        double[] outDoubles = new double[3];
        writeFile.seek(57);
        writeFile.readDouble(outDoubles, 0, outDoubles.length);
        assertArrayEquals(expectedDoubles, outDoubles);
        // 24 bytes written
        // pos = 81

        // char
        writeFile.writeChar(new char[] {1, 2, 3}, 0, 3, ByteOrder.BIG_ENDIAN);
        char[] expectedChars = new char[] {1, 2, 3};
        writeFile.seek(81);
        assertEquals(expectedChars[0], writeFile.readChar());
        assertEquals(expectedChars[1], writeFile.readChar());
        assertEquals(expectedChars[2], writeFile.readChar());

        // 6 bytes written
        // pos = 87
        writeFile.writeChars(TEST_FILE_STRING, ByteOrder.BIG_ENDIAN);
        writeFile.seek(87);
        assertEquals(TEST_FILE_STRING,
                writeFile.readString((int) TEST_FILE_LENGTH * 2, StandardCharsets.UTF_16BE));

        writeFile.close();
    }

    @Test
    void testWriteDefaultEndian() throws IOException {
        RandomAccessFile writeFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        // pos = 0
        writeFile.seek(0);

        // boolean
        writeFile.writeBoolean(true);
        writeFile.writeBoolean(new boolean[] {true, false, false}, 1, 2);
        byte[] expected = new byte[] {(byte) 1, (byte) 0, (byte) 0};
        writeFile.seek(0);
        assertTrue(arraysMatch(writeFile.readBytes(3), expected, 0, 0, 3));
        // 3 bytes written
        // pos = 3

        // short
        writeFile.writeShort(0);
        writeFile.writeShort(new short[] {1, 2, 3}, 1, 2);
        short[] expectedShorts = new short[] {0, 2, 3};
        short[] outShorts = new short[3];
        writeFile.seek(3);
        writeFile.readShort(outShorts, 0, outShorts.length);
        assertArrayEquals(expectedShorts, outShorts);
        // 6 bytes written
        // pos = 9

        // int
        writeFile.writeInt(0);
        writeFile.writeInt(new int[] {1, 2, 3}, 1, 2);
        int[] expectedInts = new int[] {0, 2, 3};
        int[] outInts = new int[3];
        writeFile.seek(9);
        writeFile.readInt(outInts, 0, outInts.length);
        assertArrayEquals(expectedInts, outInts);
        // 12 bytes written
        // pos = 21

        // long
        writeFile.writeLong(0);
        writeFile.writeLong(new long[] {1, 2, 3}, 1, 2);
        long[] expectedLongs = new long[] {0, 2, 3};
        long[] outLongs = new long[3];
        writeFile.seek(21);
        writeFile.readLong(outLongs, 0, outLongs.length);
        assertArrayEquals(expectedLongs, outLongs);
        // 24 bytes written
        // pos = 45

        // float
        writeFile.writeFloat(0);
        writeFile.writeFloat(new float[] {1, 2, 3}, 1, 2);
        float[] expectedFloats = new float[] {0, 2, 3};
        float[] outFloats = new float[3];
        writeFile.seek(45);
        writeFile.readFloat(outFloats, 0, outFloats.length);
        assertArrayEquals(expectedFloats, outFloats);
        // 12 bytes written
        // pos = 57

        // double
        writeFile.writeDouble(0);
        writeFile.writeDouble(new double[] {1, 2, 3}, 1, 2);
        double[] expectedDoubles = new double[] {0, 2, 3};
        double[] outDoubles = new double[3];
        writeFile.seek(57);
        writeFile.readDouble(outDoubles, 0, outDoubles.length);
        assertArrayEquals(expectedDoubles, outDoubles);
        // 24 bytes written
        // pos = 81

        // char
        writeFile.writeChar(new char[] {1, 2, 3}, 0, 3);
        char[] expectedChars = new char[] {1, 2, 3};
        writeFile.seek(81);
        assertEquals(expectedChars[0], writeFile.readChar());
        assertEquals(expectedChars[1], writeFile.readChar());
        assertEquals(expectedChars[2], writeFile.readChar());

        // 6 bytes written
        // pos = 87
        writeFile.writeChars(TEST_FILE_STRING);
        writeFile.seek(87);
        assertEquals(TEST_FILE_STRING,
                writeFile.readString((int) TEST_FILE_LENGTH * 2, StandardCharsets.UTF_16));

        writeFile.close();
    }

    @Test
    void testFlush() throws IOException {
        RandomAccessFile tempFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        tempFile.seek(0);
        tempFile.write(0);
        assertTrue(tempFile.bufferModified);
        tempFile.flush();
        assertFalse(tempFile.bufferModified);
        tempFile.close();
    }

    @Test
    void testClose() throws IOException {
        RandomAccessFile tempFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);

        // write a byte
        tempFile.seek(0);
        tempFile.writeByte(0);
        assertTrue(tempFile.bufferModified);
        // close
        tempFile.close();
        // check buffer is flushed
        assertFalse(tempFile.bufferModified);
    }

    @Test
    void testWriteUTF() throws IOException {
        RandomAccessFile tempFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        tempFile.writeUTF(TEST_FILE_STRING);
        tempFile.seek(0);
        assertEquals((short) TEST_FILE_LENGTH, tempFile.readShort());
        byte[] out = new byte[(int) TEST_FILE_LENGTH];
        tempFile.read(out, 0, (int) TEST_FILE_LENGTH);
        assertTrue(arraysMatch(out, UTF8_BYTES, 0, 0, (int) TEST_FILE_LENGTH));
        tempFile.close();
    }

    @Test
    void testReadUTF() throws IOException {
        RandomAccessFile tempFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        tempFile.writeShort((int) TEST_FILE_LENGTH);
        tempFile.write(UTF8_BYTES);
        tempFile.seek(0);
        assertEquals(TEST_FILE_STRING, tempFile.readUTF());
        tempFile.close();
    }

    ///////////////////////////
    // Test read and write UTF-16
    @Test
    void testReadAndWriteUTF16() throws IOException {
        readAndWriteUTF16(ByteOrder.LITTLE_ENDIAN, StandardCharsets.UTF_16LE);
        readAndWriteUTF16(ByteOrder.BIG_ENDIAN, StandardCharsets.UTF_16BE);
    }

    private void readAndWriteUTF16(ByteOrder bo, Charset charset) throws IOException {
        RandomAccessFile tempFile = new RandomAccessFile(newTempFile().toString(), "rw", TEST_BUFFER_SIZE);
        tempFile.order(bo);

        // write bytes
        tempFile.write(TEST_FILE_STRING.getBytes(charset));

        // read line
        tempFile.seek(0);
        int linebreak = TEST_FILE_STRING.indexOf("\r\n");
        assertEquals(TEST_FILE_STRING.substring(0, linebreak),
                tempFile.readLine(charset));
        assertEquals(TEST_FILE_STRING.substring(linebreak + 2),
                tempFile.readLine(charset));

        // read string
        int nbytes = 11;
        tempFile.seek(0);
        assertEquals(TEST_FILE_STRING.substring(0, nbytes),
                tempFile.readString(nbytes * 2, charset));

        // read string max
        tempFile.seek(0);
        assertEquals(TEST_FILE_STRING,
                tempFile.readStringMax((int) TEST_FILE_LENGTH * 2, charset));

        tempFile.close();
    }

    /**
     * Elementwise comparison of subsections of two byte arrays
     *
     * @param arr1
     * @param arr2
     * @param start1- position in arr1 to start comparison
     * @param start2 - position in arr2 to start comparison
     * @param n - number of elements to compare
     * @return true if arr1 and arr2 have n matching elements starting at positions start1 and start2
     */
    private boolean arraysMatch(byte[] arr1, byte[] arr2, int start1, int start2, int n) {
        if ((start1 + n) > arr1.length || (start2 + n) > arr2.length) {
            return false;
        }

        for (int i = 0; i < n; i++) {
            if (arr1[start1 + i] != arr2[start2 + i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare two floats, ignoring precision differences
     *
     * @param f1 float one
     * @param f2 float two
     * @return true if floats are equal within threshold
     */
    private boolean compareFloats(float f1, float f2) {
        return Math.abs(f1 - f2) < (f1 / Math.pow(10, 7));
    }

    /**
     * Compare two dubles, ignoring precision differences
     *
     * @param d1 double 1
     * @param d2 double 2
     * @return true if doubles are equal within threshold
     */
    private boolean compareDoubles(double d1, double d2) {
        double dif = Math.abs(d1 - d2);
        double threshold = (d1 / Math.pow(10, 16));
        return Math.abs(d1 - d2) < (d1 / Math.pow(10, 15));
    }

    /**
     * simple WritableByteChannel implementation
     * Writes to outputstream
     */
    private static class TestWritableByteChannel implements WritableByteChannel {

        private boolean open;
        private ByteArrayOutputStream dest;

        public TestWritableByteChannel() {
            open = true;
            dest = new ByteArrayOutputStream();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            byte[] out = src.array();
            dest.write(out);
            return out.length;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            open = false;
        }

        public byte[] getBytes() {
            return dest.toByteArray();
        }

        void reset() throws IOException {
            dest.reset();
        }
    }
}