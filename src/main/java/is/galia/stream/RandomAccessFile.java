/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package is.galia.stream;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Drop-in replacement for {@link java.io.RandomAccessFile}, with some
 * notable extensions:</p>
 *
 * <ul>
 *     <li>Buffered I/O: instances of this class realise substantial speed
 *     increases over {@link java.io.RandomAccessFile} through the use of
 *     buffering.</li>
 *     <li>Read String methods support user-specified Charsets (default
 *     UTF-8).</li>
 *     <li>Support for both big and little endiannness on reads and write:
 *     users may specify the byte order for I/O operations.</li>
 * </ul>
 *
 * <p>This is a subclass of Object, as it was not possible to subclass
 * {@link java.io.RandomAccessFile} because many of the methods are final.
 * However, if it is necessary to use this and that interchangeably, both
 * classes implement the {@link DataInput} and {@link DataOutput}
 * interfaces.</p>
 *
 * <h2>History</h2>
 *
 * <ol>
 *     <li>{@link java.io.RandomAccessFile} by Sun Microsystems</li>
 *     <li>Alex McManus: derived <a href="http://www.aber.ac.uk/~agm/Java.html">
 *     BufferedRandomAccessFile</a> (dead link) containing the bulk of the
 *     enhancements</li>
 *     <li>Russ Rew, Unidata: misc. changes</li>
 *     <li>Baird Creek Software: misc. refactoring, migrated unit tests to
 *     JUnit, cleaned up Javadoc</li>
 * </ol>
 *
 * <p>Not thread-safe.</p>
 *
 * @author Alex McManus
 * @author Russ Rew
 * @author john caron
 * @see java.io.DataInput
 * @see java.io.DataOutput
 * @see java.io.RandomAccessFile
 */
class RandomAccessFile implements DataInput, DataOutput, Closeable {

    public static final int BIG_ENDIAN = 0;
    public static final int LITTLE_ENDIAN = 1;

    protected static final int defaultBufferSize = 8092; // The default buffer size, in bytes.

    ///////////////////////////////////////////////////////////////////////
    // debug leaks - keep track of open files
    protected static boolean debugLeaks;
    protected static boolean debugAccess;
    protected static Set<String> allFiles;
    protected static List<String> openFiles =  Collections.synchronizedList(new ArrayList<>()); // could keep map on file
    // hashcode
    private static AtomicLong count_openFiles = new AtomicLong();
    private static AtomicInteger maxOpenFiles = new AtomicInteger();
    private static AtomicInteger debug_nseeks = new AtomicInteger();
    private static AtomicLong debug_nbytes = new AtomicLong();

    protected static boolean showOpen;
    protected static boolean showRead;

    /** @deprecated do not use. */
    @Deprecated
    public static void setDebugLeaks(boolean b) {
        if (b) {
            count_openFiles.set(0);
            maxOpenFiles.set(0);
            allFiles = new HashSet<>(1000);
        }
        debugLeaks = b;
    }

    /** @deprecated do not use. */
    @Deprecated
    public static List<String> getOpenFiles() {
        return Collections.unmodifiableList(openFiles);
    }

    /** @deprecated do not use. */
    @Deprecated
    public static long getOpenFileCount() {
        return count_openFiles.get();
    }

    /** @deprecated do not use. */
    @Deprecated
    public static int getMaxOpenFileCount() {
        return maxOpenFiles.get();
    }

    /** @deprecated do not use. */
    @Deprecated
    public static List<String> getAllFiles() {
        if (null == allFiles)
            return null;
        return allFiles.stream().sorted().collect(Collectors.toList());
    }

    /** @deprecated do not use. */
    @Deprecated
    public static void setDebugAccess(boolean b) {
        debugAccess = b;
        if (b) {
            debug_nseeks = new AtomicInteger();
            debug_nbytes = new AtomicLong();
        }
    }

    /** @deprecated do not use. */
    @Deprecated
    public static int getDebugNseeks() {
        return (debug_nseeks == null) ? 0 : debug_nseeks.intValue();
    }

    /** @deprecated do not use. */
    @Deprecated
    public static long getDebugNbytes() {
        return (debug_nbytes == null) ? 0 : debug_nbytes.longValue();
    }


    /**
     * File location
     */
    protected String location;

    /**
     * The underlying java.io.RandomAccessFile.
     */
    protected java.io.RandomAccessFile file;
    protected java.nio.channels.FileChannel fileChannel;

    /**
     * The offset in bytes from the file start, of the next read or
     * write operation.
     */
    protected long filePosition;

    /**
     * The buffer used for reading the data.
     */
    protected byte[] buffer;

    /**
     * The offset in bytes of the start of the buffer, from the start of the file.
     */
    protected long bufferStart;

    /**
     * The offset in bytes of the end of the data in the buffer, from
     * the start of the file. This can be calculated from
     * <code>bufferStart + dataSize</code>, but it is cached to speed
     * up the read( ) method.
     */
    protected long dataEnd;

    /**
     * The size of the data stored in the buffer, in bytes. This may be
     * less than the size of the buffer.
     */
    protected int dataSize;

    /**
     * True if we are at the end of the file.
     */
    protected boolean endOfFile;

    /**
     * The access mode of the file.
     */
    protected boolean readonly;

    /**
     * The current endian (big or little) mode of the file.
     */
    protected boolean bigEndian;

    /**
     * True if the data in the buffer has been modified.
     */
    protected boolean bufferModified;

    /**
     * make sure file is at least this long when closed
     */
    private long minLength;

    /**
     * STUPID extendMode for truncated, yet valid files. old netcdf C library code allowed NOFILL to do this
     */
    private boolean extendMode;

    /**
     * Constructor, for subclasses
     *
     * @param bufferSize size of read buffer
     */
    protected RandomAccessFile(int bufferSize) {
        file = null;
        readonly = true;
        init(bufferSize);
    }

    /**
     * Constructor, default buffer size.
     *
     * @param location location of the file
     * @param mode same as for java.io.RandomAccessFile, usually "r" or "rw"
     * @throws IOException on open error
     */
    public RandomAccessFile(String location, String mode) throws IOException {
        this(location, mode, defaultBufferSize);
        this.location = location;
    }

    /**
     * Constructor.
     *
     * @param location location of the file
     * @param mode same as for java.io.RandomAccessFile
     * @param bufferSize size of buffer to use.
     * @throws IOException on open error
     */
    public RandomAccessFile(String location, String mode, int bufferSize) throws IOException {
        if (bufferSize < 0)
            bufferSize = defaultBufferSize;
        this.location = location;
        if (debugLeaks) {
            allFiles.add(location);
        }

        try {
            this.file = new java.io.RandomAccessFile(location, mode);
        } catch (IOException ioe) {
            if (ioe.getMessage().equals("Too many open files")) {
                System.out.printf("RandomAccessFile %s%n", ioe);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.file = new java.io.RandomAccessFile(location, mode); // Windows having trouble keeping up ??
            } else {
                throw ioe;
            }
        }

        this.readonly = mode.equals("r");
        init(bufferSize);

        if (debugLeaks) {
            openFiles.add(location);
            int max = Math.max(openFiles.size(), maxOpenFiles.get());
            maxOpenFiles.set(max);
            count_openFiles.getAndIncrement();
            if (showOpen)
                System.out.println(" DebugRAF open " + location);
        }
    }

    private void init(int bufferSize) {
        // Initialise the buffer
        bufferStart = 0;
        dataEnd = 0;
        dataSize = 0;
        filePosition = 0;
        buffer = new byte[bufferSize];
        endOfFile = false;
        bigEndian = true;
    }

    /**
     * Set the buffer size.
     * If writing, call flush() first.
     *
     * @param bufferSize length in bytes
     */
    public void setBufferSize(int bufferSize) {
        init(bufferSize);
    }

    /**
     * Get the buffer size
     *
     * @return bufferSize length in bytes
     */
    public int getBufferSize() {
        return buffer.length;
    }

    /**
     * Close the file, and release any associated system resources.
     *
     * @throws IOException if an I/O error occurrs.
     */
    public void close() throws IOException {
        if (debugLeaks) {
            openFiles.remove(location);
            if (showOpen)
                System.out.println("  close " + location);
        }

        if (file == null)
            return;

        // If we are writing and the buffer has been modified, flush the contents of the buffer.
        flush();

        // may need to extend file, in case no fill is being used
        // may need to truncate file in case overwriting a longer file
        // use only if minLength is set (by N3iosp)
        long fileSize = file.length();
        if (!readonly && (minLength != 0) && (minLength != fileSize)) {
            file.setLength(minLength);
        }

        // Close the underlying file object.
        file.close();
        file = null; // help the gc
    }

    public FileTime getLastModified() throws IOException {
        return Files.getLastModifiedTime(Path.of(getLocation()));
    }

    /** Returns true if file pointer is at end of file. */
    public boolean isAtEndOfFile() {
        return endOfFile;
    }

    /** Returns true if RandomAccessFile represents a directory structure */
    public boolean isDirectory() {
        return false;
    }

    /**
     * Set the position in the file for the next read or write.
     *
     * @param pos the offset (in bytes) from the start of the file.
     * @throws IOException if an I/O error occurrs.
     */
    public void seek(long pos) throws IOException {
        if (pos < 0)
            throw new java.io.IOException("Negative seek offset");

        // If the seek is into the buffer, just update the file pointer.
        if ((pos >= bufferStart) && (pos < dataEnd)) {
            filePosition = pos;
            endOfFile = false;
            return;
        }

        // need new buffer, starting at pos
        readBuffer(pos);
    }

    protected void readBuffer(long pos) throws IOException {
        // If the current buffer is modified, write it to disk.
        if (bufferModified) {
            flush();
        }

        bufferStart = pos;
        filePosition = pos;

        dataSize = read_(pos, buffer, 0, buffer.length);

        if (dataSize <= 0) {
            dataSize = 0;
            endOfFile = true;
        } else {
            endOfFile = false;
        }

        // Cache the position of the buffer end.
        dataEnd = bufferStart + dataSize;
    }

    /**
     * Returns the current position in the file, where the next read or
     * write will occur.
     *
     * @return the offset from the start of the file in bytes.
     */
    public long getFilePointer() {
        return filePosition;
    }

    /**
     * Get the file location, or name.
     *
     * @return file location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Get the length of the file. The data in the buffer (which may not
     * have been written the disk yet) is taken into account.
     *
     * @return the length of the file in bytes.
     * @throws IOException if an I/O error occurrs.
     */
    public long length() throws IOException {
        long fileLength = (file == null) ? -1L : file.length(); // GRIB has closed the data raf
        if (fileLength < dataEnd) {
            return dataEnd;
        } else {
            return fileLength;
        }
    }

    /**
     * Change the current endian mode. Subsequent reads of short, int, float, double, long, char will
     * use this. Does not currently affect writes - ByteOrder must be explicitly specified on writes.
     * Default values is BIG_ENDIAN.
     *
     * This method is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param endian RandomAccessFile.BIG_ENDIAN or RandomAccessFile.LITTLE_ENDIAN
     */
    public void order(int endian) {
        if (endian < 0)
            return;
        this.bigEndian = (endian == BIG_ENDIAN);
    }

    public void order(ByteOrder bo) {
        if (bo == null)
            return;
        this.bigEndian = bo.equals(ByteOrder.BIG_ENDIAN);
    }

    /**
     * Copy the contents of the buffer to the disk.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void flush() throws IOException {
        if (bufferModified) {
            file.seek(bufferStart);
            file.write(buffer, 0, dataSize);
            bufferModified = false;
        }
    }

    /**
     * Make sure file is at least this long when its closed.
     * needed when not using fill mode, and not all data is written.
     *
     * @param minLength minimum length of the file.
     */
    public void setMinLength(long minLength) {
        this.minLength = minLength;
    }

    /**
     * Set extendMode for truncated, yet valid files - old NetCDF code allowed this
     * when NOFILL on, and user doesnt write all variables.
     */
    public void setExtendMode() {
        this.extendMode = true;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Read primitives.
    //

    /**
     * Read a byte of data from the file, blocking until data is
     * available.
     *
     * @return the next byte of data, or -1 if the end of the file is
     *         reached.
     * @throws IOException if an I/O error occurrs.
     */
    public int read() throws IOException {

        // If the file position is within the data, return the byte...
        if (filePosition < dataEnd) {
            int pos = (int) (filePosition - bufferStart);
            filePosition++;
            return (buffer[pos] & 0xff);

            // ...or should we indicate EOF...
        } else if (endOfFile) {
            return -1;

            // ...or seek to fill the buffer, and try again.
        } else {
            seek(filePosition);
            return read();
        }
    }

    /**
     * Read up to <code>len</code> bytes into an array, at a specified
     * offset. This will block until at least one byte has been read.
     *
     * @param b the byte array to receive the bytes.
     * @param off the offset in the array where copying will start.
     * @param len the number of bytes to copy.
     * @return the actual number of bytes read, or -1 if there is not
     *         more data due to the end of the file being reached.
     * @throws IOException if an I/O error occurrs.
     */
    public int readBytes(byte[] b, int off, int len) throws IOException {

        // Check for end of file.
        if (endOfFile) {
            return -1;
        }

        // See how many bytes are available in the buffer - if none,
        // seek to the file position to update the buffer and try again.
        int bytesAvailable = (int) (dataEnd - filePosition);
        if (bytesAvailable < 1) {
            seek(filePosition);
            return readBytes(b, off, len);
        }

        // Copy as much as we can.
        int copyLength = (bytesAvailable >= len) ? len : bytesAvailable;
        System.arraycopy(buffer, (int) (filePosition - bufferStart), b, off, copyLength);
        filePosition += copyLength;

        // If there is more to copy...
        if (copyLength < len) {
            int extraCopy = len - copyLength;

            // If the amount remaining is more than a buffer's length, read it
            // directly from the file.
            if (extraCopy > buffer.length) {
                extraCopy = read_(filePosition, b, off + copyLength, len - copyLength);

                // ...or read a new buffer full, and copy as much as possible...
            } else {
                seek(filePosition);
                if (!endOfFile) {
                    extraCopy = (extraCopy > dataSize) ? dataSize : extraCopy;
                    System.arraycopy(buffer, 0, b, off + copyLength, extraCopy);
                } else {
                    extraCopy = -1;
                }
            }

            // If we did manage to copy any more, update the file position and
            // return the amount copied.
            if (extraCopy > 0) {
                filePosition += extraCopy;
                return copyLength + extraCopy;
            }
        }

        // Return the amount copied.
        return copyLength;
    }

    /**
     * Read <code>nbytes</code> bytes, at the specified file offset, send to a WritableByteChannel.
     * This will block until all bytes are read.
     * This uses the underlying file channel directly, bypassing all user buffers.
     *
     * @param dest write to this WritableByteChannel.
     * @param offset the offset in the file where copying will start.
     * @param nbytes the number of bytes to read.
     * @return the actual number of bytes read and transfered
     * @throws IOException if an I/O error occurs.
     */
    public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {

        if (fileChannel == null)
            fileChannel = file.getChannel();

        long need = nbytes;
        while (need > 0) {
            long count = fileChannel.transferTo(offset, need, dest);
            if (count == 0)
                break; // EOF condition
            need -= count;
            offset += count;
        }
        return nbytes - need;
    }


    /**
     * Read directly from file, without going through the buffer.
     * All reading goes through here or readToByteChannel;
     *
     * @param pos start here in the file
     * @param b put data into this buffer
     * @param offset buffer offset
     * @param len this number of bytes
     * @return actual number of bytes read, -1 if underlying random access file was closed
     * @throws IOException on io error
     */
    protected int read_(long pos, byte[] b, int offset, int len) throws IOException {
        if (file == null) {
            return -1;
        }

        file.seek(pos);
        int n = file.read(b, offset, len);
        if (debugAccess) {
            if (showRead)
                System.out.printf(" **read_ %s = %d bytes at %d; block = %d%n", location, len, pos, (pos / buffer.length));
            debug_nseeks.incrementAndGet();
            debug_nbytes.addAndGet(len);
        }

        if (extendMode && (n < len)) {
            n = len;
        }
        return n;
    }

    /**
     * Read up to <code>len</code> bytes into an array, at a specified
     * offset. This will block until at least one byte has been read.
     *
     * @param b the byte array to receive the bytes.
     * @param off the offset in the array where copying will start.
     * @param len the number of bytes to copy.
     * @return the actual number of bytes read, or -1 if there is not
     *         more data due to the end of the file being reached.
     * @throws IOException if an I/O error occurrs.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        return readBytes(b, off, len);
    }

    /**
     * Read up to <code>b.length( )</code> bytes into an array. This
     * will block until at least one byte has been read.
     *
     * @param b the byte array to receive the bytes.
     * @return the actual number of bytes read, or -1 if there is not
     *         more data due to the end of the file being reached.
     * @throws IOException if an I/O error occurrs.
     */
    public int read(byte[] b) throws IOException {
        return readBytes(b, 0, b.length);
    }

    /**
     * Read fully count number of bytes
     *
     * @param count how many bytes tp read
     * @return a byte array of length count, fully read in
     * @throws IOException if an I/O error occurrs.
     */
    public byte[] readBytes(int count) throws IOException {
        byte[] b = new byte[count];
        readFully(b);
        return b;
    }

    /**
     * Reads <code>b.length</code> bytes from this file into the byte
     * array. This method reads repeatedly from the file until all the
     * bytes are read. This method blocks until all the bytes are read,
     * the end of the stream is detected, or an exception is thrown.
     *
     * @param b the buffer into which the data is read.
     * @throws EOFException if this file reaches the end before reading
     *         all the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
     * Reads exactly <code>len</code> bytes from this file into the byte
     * array. This method reads repeatedly from the file until all the
     * bytes are read. This method blocks until all the bytes are read,
     * the end of the stream is detected, or an exception is thrown.
     *
     * @param b the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the number of bytes to read.
     * @throws EOFException if this file reaches the end before reading
     *         all the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public final void readFully(byte[] b, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int count = this.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException("Reading " + location + " at " + filePosition + " file length = " + length());
            }
            n += count;
        }
    }

    /**
     * Skips exactly <code>n</code> bytes of input.
     * This method blocks until all the bytes are skipped, the end of
     * the stream is detected, or an exception is thrown.
     *
     * @param n the number of bytes to be skipped.
     * @return the number of bytes skipped, which is always <code>n</code>.
     * @throws EOFException if this file reaches the end before skipping
     *         all the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public int skipBytes(int n) throws IOException {
        seek(getFilePointer() + n);
        return n;
    }

    public long skipBytes(long n) throws IOException {
        seek(getFilePointer() + n);
        return n;
    }

    /*
     * public void skipToMultiple( int multipleOfBytes) throws IOException {
     * long pos = getFilePointer();
     * int pad = (int) (pos % multipleOfBytes);
     * if (pad != 0) pad = multipleOfBytes - pad;
     * if (pad > 0) skipBytes(pad);
     * }
     */

    /**
     * Unread the last byte read.
     * This method should not be used more than once
     * between reading operations, or strange things might happen.
     */
    public void unread() {
        filePosition--;
    }

    //
    // Write primitives.
    //

    /**
     * <p>Write a byte to the file. If the file has not been opened for
     * writing, an IOException will be raised only when an attempt is
     * made to write the buffer to the file.</p>
     *
     * <p>Caveat: the effects of seek( )ing beyond the end of the file are
     * undefined.</p>
     *
     * @param b write this byte
     * @throws IOException if an I/O error occurrs.
     */
    public void write(int b) throws IOException {

        // If the file position is within the block of data...
        if (filePosition < dataEnd) {
            int pos = (int) (filePosition - bufferStart);
            buffer[pos] = (byte) b;
            bufferModified = true;
            filePosition++;

            // ...or (assuming that seek will not allow the file pointer
            // to move beyond the end of the file) get the correct block of
            // data...
        } else {

            // If there is room in the buffer, expand it...
            if (dataSize != buffer.length) {
                int pos = (int) (filePosition - bufferStart);
                buffer[pos] = (byte) b;
                bufferModified = true;
                filePosition++;
                dataSize++;
                dataEnd++;

                // ...or do another seek to get a new buffer, and start again...
            } else {
                seek(filePosition);
                write(b);
            }
        }
    }

    /**
     * Write <code>len</code> bytes from an array to the file.
     *
     * @param b the array containing the data.
     * @param off the offset in the array to the data.
     * @param len the length of the data.
     * @throws IOException if an I/O error occurrs.
     */
    public void writeBytes(byte[] b, int off, int len) throws IOException {
        // If the amount of data is small (less than a full buffer)...
        if (len < buffer.length) {

            // If any of the data fits within the buffer...
            int spaceInBuffer = 0;
            int copyLength = 0;
            if (filePosition >= bufferStart) {
                spaceInBuffer = (int) ((bufferStart + buffer.length) - filePosition);
            }

            if (spaceInBuffer > 0) {
                // Copy as much as possible to the buffer.
                copyLength = (spaceInBuffer > len) ? len : spaceInBuffer;
                System.arraycopy(b, off, buffer, (int) (filePosition - bufferStart), copyLength);
                bufferModified = true;
                long myDataEnd = filePosition + copyLength;
                dataEnd = (myDataEnd > dataEnd) ? myDataEnd : dataEnd;
                dataSize = (int) (dataEnd - bufferStart);
                filePosition += copyLength;
            }

            // If there is any data remaining, move to the new position and copy to
            // the new buffer.
            if (copyLength < len) {
                seek(filePosition); // triggers a flush
                System.arraycopy(b, off + copyLength, buffer, (int) (filePosition - bufferStart), len - copyLength);
                bufferModified = true;
                long myDataEnd = filePosition + (len - copyLength);
                dataEnd = (myDataEnd > dataEnd) ? myDataEnd : dataEnd;
                dataSize = (int) (dataEnd - bufferStart);
                filePosition += (len - copyLength);
            }

            // ...or write a lot of data...
        } else {

            // Flush the current buffer, and write this data to the file.
            if (bufferModified) {
                flush();
            }
            file.seek(filePosition); // moved per Steve Cerruti; Jan 14, 2005
            file.write(b, off, len);

            filePosition += len;
            bufferStart = filePosition; // an empty buffer
            dataSize = 0;
            dataEnd = bufferStart + dataSize;
        }
    }

    /**
     * Writes <code>b.length</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this file.
     *
     * @param b the data.
     * @throws IOException if an I/O error occurs.
     */
    public void write(byte[] b) throws IOException {
        writeBytes(b, 0, b.length);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this file.
     *
     * @param b the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    public void write(byte[] b, int off, int len) throws IOException {
        writeBytes(b, off, len);
    }

    //
    // DataInput methods.
    //

    /**
     * Reads a <code>boolean</code> from this file. This method reads a
     * single byte from the file. A value of <code>0</code> represents
     * <code>false</code>. Any other value represents <code>true</code>.
     * This method blocks until the byte is read, the end of the stream
     * is detected, or an exception is thrown.
     *
     * @return the <code>boolean</code> value read.
     * @throws EOFException if this file has reached the end.
     * @throws IOException if an I/O error occurs.
     */
    public final boolean readBoolean() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (ch != 0);
    }

    /**
     * <p>Reads a signed 8-bit value from this file. This method reads a byte
     * from the file. If the byte read is <code>b</code>, where
     * <code>0&nbsp;&le;&nbsp;b&nbsp;&le;&nbsp;255</code>,
     * then the result is:</p>
     *
     * <code>(byte) (b)</code>
     *
     * <p>This method blocks until the byte is read, the end of the stream is
     * detected, or an exception is thrown.</p>
     *
     * @return the next byte of this file as a signed 8-bit
     *         <code>byte</code>.
     * @throws EOFException if this file has reached the end.
     * @throws IOException if an I/O error occurs.
     */
    public final byte readByte() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) (ch);
    }

    /**
     * <p>Reads an unsigned 8-bit number from this file. This method reads
     * a byte from this file and returns that byte.</p>
     *
     * <p>This method blocks until the byte is read, the end of the stream
     * is detected, or an exception is thrown.</p>
     *
     * @return the next byte of this file, interpreted as an unsigned 8-bit
     *         number.
     * @throws EOFException if this file has reached the end.
     * @throws IOException if an I/O error occurs.
     */
    public final int readUnsignedByte() throws IOException {
        int ch = this.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return ch;
    }

    /**
     * <p>Reads a signed 16-bit number from this file. The method reads 2
     * bytes from this file. If the two bytes read, in order, are
     * <code>b1</code> and <code>b2</code>, where each of the two values is
     * between <code>0</code> and <code>255</code>, inclusive, then the
     * result is equal to:</p>
     *
     * <code>
     * (short)((b1 &lt;&lt; 8) | b2)
     * </code>
     *
     * <p>This method blocks until the two bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return the next two bytes of this file, interpreted as a signed
     *         16-bit number.
     * @throws EOFException if this file reaches the end before reading
     *         two bytes.
     * @throws IOException if an I/O error occurs.
     */
    public final short readShort() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        if (bigEndian) {
            return (short) ((ch1 << 8) + (ch2));
        } else {
            return (short) ((ch2 << 8) + (ch1));
        }
    }

    /**
     * Read an array of shorts
     *
     * @param pa read into this array
     * @param start starting at pa[start]
     * @param n read this many elements
     * @throws IOException on read error
     */
    public final void readShort(short[] pa, int start, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            pa[start + i] = readShort();
        }
    }

    /**
     * <p>Reads an unsigned 16-bit number from this file. This method reads two
     * bytes from the file. If the bytes read, in order, are {@code b1} and
     * {@code b2}, where {@code 0 &lte; b1}, {@code b2 &lte; 255}, then the
     * result is equal to:</p>
     *
     * <code>
     * (b1 &lt;&lt; 8) | b2
     * </code>
     *
     * <p>This method blocks until the two bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return the next two bytes of this file, interpreted as an unsigned
     *         16-bit integer.
     * @throws EOFException if this file reaches the end before reading
     *         two bytes.
     * @throws IOException if an I/O error occurs.
     */
    public final int readUnsignedShort() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        if (bigEndian) {
            return ((ch1 << 8) + (ch2));
        } else {
            return ((ch2 << 8) + (ch1));
        }
    }


    /*
     * Reads a signed 24-bit integer from this file. This method reads 3
     * bytes from the file. If the bytes read, in order, are <code>b1</code>,
     * <code>b2</code>, and <code>b3</code>, where
     * <code>0&nbsp;&le;&nbsp;b1, b2, b3&nbsp;&le;&nbsp;255</code>,
     * then the result is equal to:
     * <ul><code>
     * (b1 &lt;&lt; 16) | (b2 &lt;&lt; 8) + (b3 &lt;&lt; 0)
     * </code></ul>
     * <p/>
     * This method blocks until the three bytes are read, the end of the
     * stream is detected, or an exception is thrown.
     */

    /**
     * <p>Reads a Unicode character from this file. This method reads two
     * bytes from the file. If the bytes read, in order, are {@code b1} and
     * {@code b2}, where {@code 0 &lte; b1, b2 &lte; 255}, then the result is
     * equal to:</p>
     *
     * <code>
     * (char)((b1 &lt;&lt; 8) | b2)
     * </code>
     *
     * <p>This method blocks until the two bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return the next two bytes of this file as a Unicode character.
     * @throws EOFException if this file reaches the end before reading
     *         two bytes.
     * @throws IOException if an I/O error occurs.
     */
    public final char readChar() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        if (bigEndian) {
            return (char) ((ch1 << 8) + (ch2));
        } else {
            return (char) ((ch2 << 8) + (ch1));
        }
    }

    /**
     * <p>Reads a signed 32-bit integer from this file. This method reads 4
     * bytes from the file. If the bytes read, in order, are {@code b1}, {@code
     * b2}, {@code b3}, and {@code b4}, where
     * {@code 0 &lte; b1, b2, b3, b4 &lte; 255}, then the result is equal
     * to:</p>
     *
     * <code>
     * (b1 &lt;&lt; 24) | (b2 &lt;&lt; 16) + (b3 &lt;&lt; 8) + b4
     * </code>
     *
     * <p>This method blocks until the four bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return the next four bytes of this file, interpreted as an
     *         <code>int</code>.
     * @throws EOFException if this file reaches the end before reading
     *         four bytes.
     * @throws IOException if an I/O error occurs.
     */
    public final int readInt() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        int ch3 = this.read();
        int ch4 = this.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }

        if (bigEndian) {
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
        } else {
            return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
        }
    }

    /**
     * Read an integer at the given position, bypassing all buffering.
     *
     * @param pos read a byte at this position
     * @return The int that was read
     * @throws IOException if an I/O error occurs.
     */
    public final int readIntUnbuffered(long pos) throws IOException {
        byte[] bb = new byte[4];
        read_(pos, bb, 0, 4);
        int ch1 = bb[0] & 0xff;
        int ch2 = bb[1] & 0xff;
        int ch3 = bb[2] & 0xff;
        int ch4 = bb[3] & 0xff;
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }

        if (bigEndian) {
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
        } else {
            return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1));
        }
    }


    /**
     * Read an array of ints
     *
     * @param pa read into this array
     * @param start starting at pa[start]
     * @param n read this many elements
     * @throws IOException on read error
     */
    public final void readInt(int[] pa, int start, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            pa[start + i] = readInt();
        }
    }

    /**
     * <p>Reads a signed 64-bit integer from this file. This method reads eight
     * bytes from the file. If the bytes read, in order, are
     * {@code b1}, {@code b2}, {@code b3}, {@code b4}, {@code b5}, {@code b6},
     * {@code b7}, and {@code b8}, where:</p>
     *
     * <code>
     * 0 &le; b1, b2, b3, b4, b5, b6, b7, b8 &le;255,
     * </code>
     *
     * <p>then the result is equal to:</p>
     *
     * <blockquote>
     * <pre>
     * ((long) b1 &lt;&lt; 56) + ((long) b2 &lt;&lt; 48) + ((long) b3 &lt;&lt; 40) + ((long) b4 &lt;&lt; 32) + ((long) b5 &lt;&lt; 24)
     *     + ((long) b6 &lt;&lt; 16) + ((long) b7 &lt;&lt; 8) + b8
     * </pre>
     * </blockquote>
     *
     * <p>This method blocks until the eight bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return the next eight bytes of this file, interpreted as a
     *         {@code long}.
     * @throws EOFException if this file reaches the end before reading
     *         eight bytes.
     * @throws IOException if an I/O error occurs.
     */
    public final long readLong() throws IOException {
        if (bigEndian) {
            return ((long) (readInt()) << 32) + (readInt() & 0xFFFFFFFFL); // tested ok
        } else {
            return ((readInt() & 0xFFFFFFFFL) + ((long) readInt() << 32)); // not tested yet ??
        }

        /*
         * int ch1 = this.read();
         * int ch2 = this.read();
         * int ch3 = this.read();
         * int ch4 = this.read();
         * int ch5 = this.read();
         * int ch6 = this.read();
         * int ch7 = this.read();
         * int ch8 = this.read();
         * if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0)
         * throw new EOFException();
         *
         * if (bigEndian)
         * return ((long)(ch1 << 56)) + (ch2 << 48) + (ch3 << 40) + (ch4 << 32) + (ch5 << 24) + (ch6 << 16) + (ch7 << 8) +
         * (ch8 << 0));
         * else
         * return ((long)(ch8 << 56) + (ch7 << 48) + (ch6 << 40) + (ch5 << 32) + (ch4 << 24) + (ch3 << 16) + (ch2 << 8) +
         * (ch1 << 0));
         */
    }

    /**
     * Read an array of longs
     *
     * @param pa read into this array
     * @param start starting at pa[start]
     * @param n read this many elements
     * @throws IOException on read error
     */
    public final void readLong(long[] pa, int start, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            pa[start + i] = readLong();
        }
    }


    /**
     * <p>Reads a {@code float} from this file. This method reads an
     * {@code int} value as if by the {@code readInt} method and then converts
     * it to a {@code float} using {@link Float#intBitsToFloat}.</p>
     *
     * <p>This method blocks until the four bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return the next four bytes of this file, interpreted as a
     *         {@code float}.
     * @throws EOFException if this file reaches the end before reading four
     *         bytes.
     * @throws IOException if an I/O error occurs.
     * @see java.io.RandomAccessFile#readInt()
     * @see java.lang.Float#intBitsToFloat(int)
     */
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Read an array of floats
     *
     * @param pa read into this array
     * @param start starting at pa[start]
     * @param n read this many elements
     * @throws IOException on read error
     */
    public final void readFloat(float[] pa, int start, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            pa[start + i] = Float.intBitsToFloat(readInt());
        }
    }


    /**
     * <p>Reads a {@code double} from this file. This method reads a {@code
     * long} value as if by the {@link #readLong} method and then converts that
     * {@code long} to a {@code double} using {@link
     * Double#longBitsToDouble}.</p>
     *
     * <p>This method blocks until the eight bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return the next eight bytes of this file, interpreted as a {@code
     *         double}.
     * @throws EOFException if this file reaches the end before reading
     *         eight bytes.
     * @throws IOException if an I/O error occurs.
     * @see java.io.RandomAccessFile#readLong()
     * @see java.lang.Double#longBitsToDouble(long)
     */
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Read an array of doubles
     *
     * @param pa read into this array
     * @param start starting at pa[start]
     * @param n read this many elements
     * @throws IOException on read error
     */
    public final void readDouble(double[] pa, int start, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            pa[start + i] = Double.longBitsToDouble(readLong());
        }
    }

    /**
     * Reads the next line of text from this file. This method successively
     * reads bytes from the file, starting at the current file pointer,
     * until it reaches a line terminator or the end
     * of the file. Each byte is converted into a character by taking the
     * byte's value for the lower eight bits of the character and setting the
     * high eight bits of the character to zero. This method does not,
     * therefore, support the full Unicode character set.
     *
     * <p>
     * A line of text is terminated by a carriage-return character
     * (<code>'&#92;r'</code>), a newline character (<code>'&#92;n'</code>), a
     * carriage-return character immediately followed by a newline character,
     * or the end of the file. Line-terminating characters are discarded and
     * are not included as part of the string returned.
     *
     * <p>
     * This method blocks until a newline character is read, a carriage
     * return and the byte following it are read (to see if it is a newline),
     * the end of the file is reached, or an exception is thrown.
     *
     * @return the next line of text from this file, or null if end
     *         of file is encountered before even one byte is read.
     * @exception IOException if an I/O error occurs.
     */
    public final String readLine() throws IOException {
        return readLine(StandardCharsets.UTF_8);
    }

    /**
     * Read the next line of text as the specified charset
     * The charset parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param charset - character encoding to use
     * @return the next line of text
     * @throws IOException
     */
    public String readLine(Charset charset) throws IOException {
        StringBuilder input = new StringBuilder();
        int bytes_per_char = (charset.equals(StandardCharsets.UTF_16) || charset.equals(StandardCharsets.UTF_16BE)
                || charset.equals(StandardCharsets.UTF_16LE)) ? 2 : 1;

        char c = (char) -1;
        boolean eol = false;

        while (!eol) {
            try {
                // read 2 bytes at a time for UTF-16, else 1 byte at a time
                // NOTE: endianness of the file mst match endianness of the charset
                switch (c = bytes_per_char > 1 ? readChar() : (char) read()) {
                    case (char) -1:
                    case '\n':
                        eol = true;
                        break;
                    case '\r':
                        eol = true;
                        long cur = getFilePointer();
                        char next = bytes_per_char > 1 ? readChar() : (char) read();
                        if (next != '\n') {
                            seek(cur);
                        }
                        break;
                    default:
                        input.append(c);
                        break;
                }
            } catch (EOFException eof) {
                eol = true;
            }
        }

        if ((c == (char) -1) && (input.length() == 0)) {
            return null;
        }
        return input.toString();
    }

    /**
     * <p>Reads in a string from this file. The string has been encoded using a
     * modified UTF-8 format.</p>
     *
     * <p>The first two bytes are read as if by {@code readUnsignedShort}. This
     * value gives the number of following bytes that are in the encoded
     * string, not the length of the resulting string. The following bytes are
     * then interpreted as bytes encoding characters in the UTF-8 format and
     * are converted into characters.</p>
     *
     * <p>This method blocks until all the bytes are read, the end of the
     * stream is detected, or an exception is thrown.</p>
     *
     * @return a Unicode string.
     * @throws EOFException if this file reaches the end before reading all the
     *         bytes.
     * @throws IOException if an I/O error occurs.
     * @throws UTFDataFormatException if the bytes do not represent
     *         valid UTF-8 encoding of a Unicode string.
     * @see java.io.RandomAccessFile#readUnsignedShort()
     */
    public final String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    /**
     * Read a String of known length.
     *
     * @param nbytes number of bytes to read
     * @return String wrapping the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public String readString(int nbytes) throws IOException {
        return readString(nbytes, StandardCharsets.UTF_8);
    }

    /**
     * Read a String of known length as the specified charset.
     * The charset parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param nbytes number of bytes to reSad
     * @param charset the {@link Charset charset} to be used to decode the bytes
     * @return String wrapping the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public String readString(int nbytes, Charset charset) throws IOException {
        byte[] data = new byte[nbytes];
        readFully(data);
        return new String(data, charset);
    }

    /**
     * Read a String of max length, zero terminate.
     *
     * @param nbytes number of bytes to read
     * @return String wrapping the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public String readStringMax(int nbytes) throws IOException {
        return readStringMax(nbytes, StandardCharsets.UTF_8);
    }

    /**
     * Read a String of max length as the specified charset, zero terminate.
     * The charset parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param nbytes number of bytes to read
     * @param charset the {@link Charset charset} to be used to decode the bytes
     * @return String wrapping the bytes.
     * @throws IOException if an I/O error occurs.
     */
    public String readStringMax(int nbytes, Charset charset) throws IOException {
        int bytes_per_char = (charset.equals(StandardCharsets.UTF_16) || charset.equals(StandardCharsets.UTF_16BE)
                || charset.equals(StandardCharsets.UTF_16LE)) ? 2 : 1;
        byte[] b = new byte[nbytes];
        readFully(b);
        int count;
        int nzeros = 0;
        for (count = 0; count < nbytes; count++) {
            if (b[count] == 0) {
                nzeros++;
                // break if all bytes in char are zero
                if (nzeros == bytes_per_char)
                    break;
            } else // reset nzeros for non-zero byte
                nzeros = 0;
        }
        return new String(b, 0, count, charset);
    }

    //
    // DataOutput methods.
    //

    /**
     * Writes a <code>boolean</code> to the file as a 1-byte value. The
     * value <code>true</code> is written out as the value
     * <code>(byte)1</code>; the value <code>false</code> is written out
     * as the value <code>(byte)0</code>.
     *
     * @param v a <code>boolean</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    /**
     * Write an array of booleans
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @throws IOException on read error
     */
    public final void writeBoolean(boolean[] pa, int start, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            writeBoolean(pa[start + i]);
        }
    }

    /**
     * Writes a <code>byte</code> to the file as a 1-byte value.
     *
     * @param v a <code>byte</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeByte(int v) throws IOException {
        write(v);
    }

    /**
     * Writes a <code>short</code> to the file as two bytes, high byte first.
     *
     * @param v a <code>short</code> to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeShort(int v) throws IOException {
        writeShort(v, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Writes a <code>short</code> to the file as two bytes with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>short</code> to be written.
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException if an I/O error occurs.
     */
    public final void writeShort(int v, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeShort(v, bo);
    }

    /**
     * Writes a <code>short</code> to the file as two bytes with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>short</code> to be written.
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException if an I/O error occurs.
     */
    public void writeShort(int v, ByteOrder bo) throws IOException {
        if (bo.equals(ByteOrder.LITTLE_ENDIAN)) {
            write((v) & 0xFF);
            write((v >>> 8) & 0xFF);
        } else {
            write((v >>> 8) & 0xFF);
            write((v) & 0xFF);
        }
    }

    /**
     * Write an array of shorts
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n this number of elements
     * @throws IOException on read error
     */
    public final void writeShort(short[] pa, int start, int n) throws IOException {
        writeShort(pa, start, n, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Write an array of shorts with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n this number of elements
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException on read error
     */
    public final void writeShort(short[] pa, int start, int n, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeShort(pa, start, n, bo);
    }

    /**
     * Write an array of shorts with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n this number of elements
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException on read error
     */
    public final void writeShort(short[] pa, int start, int n, ByteOrder bo) throws IOException {
        for (int i = 0; i < n; i++) {
            writeShort(pa[start + i], bo);
        }
    }

    /**
     * Writes a <code>char</code> to the file as a 2-byte value, high byte first.
     *
     * @param v a <code>char</code> value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeChar(int v) throws IOException {
        writeChar(v, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Writes a <code>char</code> to the file as a 2-byte value with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>char</code> value to be written.
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException if an I/O error occurs.
     */
    public void writeChar(int v, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeChar(v, bo);
    }

    /**
     * Writes a <code>char</code> to the file as a 2-byte value with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>char</code> value to be written.
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException if an I/O error occurs.
     */
    public void writeChar(int v, ByteOrder bo) throws IOException {
        if (bo.equals(ByteOrder.LITTLE_ENDIAN)) {
            write((v) & 0xFF);
            write((v >>> 8) & 0xFF);
        } else {
            write((v >>> 8) & 0xFF);
            write((v) & 0xFF);
        }
    }

    /**
     * Write an array of chars
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n this number of elements
     * @throws IOException on read error
     */
    public final void writeChar(char[] pa, int start, int n) throws IOException {
        writeChar(pa, start, n, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Write an array of chars with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n this number of elements
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException on read error
     */
    public final void writeChar(char[] pa, int start, int n, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeChar(pa, start, n, bo);
    }

    /**
     * Write an array of chars with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n this number of elements
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException on read error
     */
    public final void writeChar(char[] pa, int start, int n, ByteOrder bo) throws IOException {
        for (int i = 0; i < n; i++) {
            writeChar(pa[start + i], bo);
        }
    }

    /**
     * Writes an <code>int</code> to the file as four bytes, high byte first.
     *
     * @param v an <code>int</code> to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeInt(int v) throws IOException {
        writeInt(v, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Writes an <code>int</code> to the file as four bytes with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v an <code>int</code> to be written.
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException if an I/O error occurs.
     */
    public final void writeInt(int v, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeInt(v, bo);
    }

    /**
     * Writes an <code>int</code> to the file as four bytes with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v an <code>int</code> to be written.
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException if an I/O error occurs.
     */
    public final void writeInt(int v, ByteOrder bo) throws IOException {
        if (bo.equals(ByteOrder.LITTLE_ENDIAN)) {
            write((v) & 0xFF);
            write((v >>> 8) & 0xFF);
            write((v >>> 16) & 0xFF);
            write((v >>> 24) & 0xFF);
        } else {
            write((v >>> 24) & 0xFF);
            write((v >>> 16) & 0xFF);
            write((v >>> 8) & 0xFF);
            write((v) & 0xFF);
        }
    }

    /**
     * Write an array of ints
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @throws IOException on read error
     */
    public final void writeInt(int[] pa, int start, int n) throws IOException {
        writeInt(pa, start, n, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Write an array of ints with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException on read error
     */
    public final void writeInt(int[] pa, int start, int n, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeInt(pa, start, n, bo);
    }

    /**
     * Write an array of ints with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException on read error
     */
    public final void writeInt(int[] pa, int start, int n, ByteOrder bo) throws IOException {
        for (int i = 0; i < n; i++) {
            writeInt(pa[start + i], bo);
        }
    }

    /**
     * Writes a <code>long</code> to the file as eight bytes, high byte first.
     *
     * @param v a <code>long</code> to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeLong(long v) throws IOException {
        writeLong(v, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Writes a <code>long</code> to the file as eight bytes with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>long</code> to be written.
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException if an I/O error occurs.
     */
    public final void writeLong(long v, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeLong(v, bo);
    }

    /**
     * Writes a <code>long</code> to the file as eight bytes with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>long</code> to be written.
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException if an I/O error occurs.
     */
    public final void writeLong(long v, ByteOrder bo) throws IOException {
        if (bo.equals(ByteOrder.LITTLE_ENDIAN)) {
            write((int) (v) & 0xFF);
            write((int) (v >>> 8) & 0xFF);
            write((int) (v >>> 16) & 0xFF);
            write((int) (v >>> 24) & 0xFF);
            write((int) (v >>> 32) & 0xFF);
            write((int) (v >>> 40) & 0xFF);
            write((int) (v >>> 48) & 0xFF);
            write((int) (v >>> 56) & 0xFF);
        } else {
            write((int) (v >>> 56) & 0xFF);
            write((int) (v >>> 48) & 0xFF);
            write((int) (v >>> 40) & 0xFF);
            write((int) (v >>> 32) & 0xFF);
            write((int) (v >>> 24) & 0xFF);
            write((int) (v >>> 16) & 0xFF);
            write((int) (v >>> 8) & 0xFF);
            write((int) (v) & 0xFF);
        }
    }

    /**
     * Write an array of longs
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @throws IOException on read error
     */
    public final void writeLong(long[] pa, int start, int n) throws IOException {
        writeLong(pa, start, n, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Write an array of longs with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException on read error
     */
    public final void writeLong(long[] pa, int start, int n, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeLong(pa, start, n, bo);
    }

    /**
     * Write an array of longs with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException on read error
     */
    public final void writeLong(long[] pa, int start, int n, ByteOrder bo) throws IOException {
        for (int i = 0; i < n; i++) {
            writeLong(pa[start + i], bo);
        }
    }

    /**
     * Converts the float argument to an <code>int</code> using the
     * <code>floatToIntBits</code> method in class <code>Float</code>,
     * and then writes that <code>int</code> value to the file as a
     * 4-byte quantity, high byte first.
     *
     * @param v a <code>float</code> value to be written.
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Float#floatToIntBits(float)
     */
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v), ByteOrder.BIG_ENDIAN);
    }

    /**
     * Converts the float argument to an <code>int</code> using the
     * <code>floatToIntBits</code> method in class <code>Float</code>,
     * and then writes that <code>int</code> value to the file as a
     * 4-byte quantity, with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>float</code> value to be written.
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Float#floatToIntBits(float)
     */
    public final void writeFloat(float v, int endian) throws IOException {
        writeInt(Float.floatToIntBits(v), endian);
    }

    /**
     * Converts the float argument to an <code>int</code> using the
     * <code>floatToIntBits</code> method in class <code>Float</code>,
     * and then writes that <code>int</code> value to the file as a
     * 4-byte quantity, with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>float</code> value to be written.
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Float#floatToIntBits(float)
     */
    public final void writeFloat(float v, ByteOrder bo) throws IOException {
        writeInt(Float.floatToIntBits(v), bo);
    }

    /**
     * Write an array of floats
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @throws IOException on read error
     */
    public final void writeFloat(float[] pa, int start, int n) throws IOException {
        writeFloat(pa, start, n, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Write an array of floats with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException on read error
     */
    public final void writeFloat(float[] pa, int start, int n, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeFloat(pa, start, n, bo);
    }

    /**
     * Write an array of floats with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException on read error
     */
    public final void writeFloat(float[] pa, int start, int n, ByteOrder bo) throws IOException {
        for (int i = 0; i < n; i++) {
            writeFloat(pa[start + i], bo);
        }
    }

    /**
     * Converts the double argument to a <code>long</code> using the
     * <code>doubleToLongBits</code> method in class <code>Double</code>,
     * and then writes that <code>long</code> value to the file as an
     * 8-byte quantity, high byte first.
     *
     * @param v a <code>double</code> value to be written.
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Double#doubleToLongBits(double)
     */
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v), ByteOrder.BIG_ENDIAN);
    }

    /**
     * Converts the double argument to a <code>long</code> using the
     * <code>doubleToLongBits</code> method in class <code>Double</code>,
     * and then writes that <code>long</code> value to the file as an
     * 8-byte quantity, with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>double</code> value to be written.
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Double#doubleToLongBits(double)
     */
    public final void writeDouble(double v, int endian) throws IOException {
        writeLong(Double.doubleToLongBits(v), endian);
    }

    /**
     * Converts the double argument to a <code>long</code> using the
     * <code>doubleToLongBits</code> method in class <code>Double</code>,
     * and then writes that <code>long</code> value to the file as an
     * 8-byte quantity, with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param v a <code>double</code> value to be written.
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException if an I/O error occurs.
     * @see java.lang.Double#doubleToLongBits(double)
     */
    public final void writeDouble(double v, ByteOrder bo) throws IOException {
        writeLong(Double.doubleToLongBits(v), bo);
    }

    /**
     * Write an array of doubles
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @throws IOException on read error
     */
    public final void writeDouble(double[] pa, int start, int n) throws IOException {
        writeDouble(pa, start, n, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Write an array of doubles with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException on read error
     */
    public final void writeDouble(double[] pa, int start, int n, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeDouble(pa, start, n, bo);
    }

    /**
     * Write an array of doubles with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param pa write from this array
     * @param start starting with this element in the array
     * @param n write this number of elements
     * @param bo Endianness of the file as a ByteOrder
     * @throws IOException on read error
     */
    public final void writeDouble(double[] pa, int start, int n, ByteOrder bo) throws IOException {
        for (int i = 0; i < n; i++) {
            writeDouble(pa[start + i], bo);
        }
    }

    /**
     * Writes the string to the file as a sequence of bytes. Each
     * character in the string is written out, in sequence, by discarding
     * its high eight bits.
     *
     * @param s a string of bytes to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            write((byte) s.charAt(i));
        }
    }

    /**
     * Writes the character array to the file as a sequence of bytes. Each
     * character in the string is written out, in sequence, by discarding
     * its high eight bits.
     *
     * @param b a character array of bytes to be written.
     * @param off the index of the first character to write.
     * @param len the number of characters to write.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeBytes(char[] b, int off, int len) throws IOException {
        for (int i = off; i < len; i++) {
            write((byte) b[i]);
        }
    }

    /**
     * Writes a string to the file as a sequence of characters. Each
     * character is written to the data output stream as if by the
     * <code>writeChar</code> method.
     *
     * @param s a <code>String</code> value to be written.
     * @throws IOException if an I/O error occurs.
     * @see java.io.RandomAccessFile#writeChar(int)
     */
    public final void writeChars(String s) throws IOException {
        writeChars(s, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Writes a string to the file as a sequence of characters. Each
     * character is written to the data output stream as if by the
     * <code>writeChar</code> method, with the provided endianness.
     * The endian parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param s a <code>String</code> value to be written.
     * @param endian Endianness of the file as an int (0 = big endian, 1 = little endian)
     * @throws IOException if an I/O error occurs.
     * @see java.io.RandomAccessFile#writeChar(int)
     */
    public void writeChars(String s, int endian) throws IOException {
        ByteOrder bo = endian == LITTLE_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        writeChars(s, bo);
    }

    /**
     * Writes a string to the file as a sequence of characters. Each
     * character is written to the data output stream as if by the
     * <code>writeChar</code> method, with the provided byte order.
     * The byte order parameter is an extension not implemented in java.io.RandomAccessFile.
     *
     * @param s a <code>String</code> value to be written.
     * @throws IOException if an I/O error occurs.
     * @param bo Endianness of the file as a ByteOrder
     * @see java.io.RandomAccessFile#writeChar(int)
     */
    public void writeChars(String s, ByteOrder bo) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            int v = s.charAt(i);
            writeChar(v, bo);
        }
    }

    /**
     * <p>Writes a string to the file using UTF-8 encoding in a
     * machine-independent manner.</p>
     *
     * <p>First, two bytes are written to the file as if by the {@link
     * #writeShort} method giving the number of bytes to follow. This value is
     * the number of bytes actually written out, not the length of the string.
     * Following the length, each character of the string is output, in
     * sequence, using the UTF-8 encoding for each character.</p>
     *
     * @param str a string to be written.
     * @throws IOException if an I/O error occurs.
     */
    public final void writeUTF(String str) throws IOException {
        int strlen = str.length();
        int utflen = 0;

        for (int i = 0; i < strlen; i++) {
            int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }
        if (utflen > 65535) {
            throw new UTFDataFormatException();
        }

        write((utflen >>> 8) & 0xFF);
        write((utflen) & 0xFF);
        for (int i = 0; i < strlen; i++) {
            int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                write(c);
            } else if (c > 0x07FF) {
                write(0xE0 | ((c >> 12) & 0x0F));
                write(0x80 | ((c >> 6) & 0x3F));
                write(0x80 | ((c) & 0x3F));
            } else {
                write(0xC0 | ((c >> 6) & 0x1F));
                write(0x80 | ((c) & 0x3F));
            }
        }
    }

    /**
     * Create a string representation of this object.
     *
     * @return a string representation of the state of the object.
     */
    public String toString() {
        return location;
        /*
         * return "fp=" + filePosition + ", bs=" + bufferStart + ", de="
         * + dataEnd + ", ds=" + dataSize + ", bl=" + buffer.length
         * + ", readonly=" + readonly + ", bm=" + bufferModified;
         */
    }

    /////////////////////////////////////////////////

    /**
     * Search forward from the current pos, looking for a match.
     *
     * @param match the match to look for.
     * @param maxBytes maximum number of bytes to search. use -1 for all
     * @return true if found, file position will be at the start of the match.
     * @throws IOException on read error
     */
    public boolean searchForward(KMPMatch match, int maxBytes) throws IOException {
        long start = getFilePointer();
        long last = (maxBytes < 0) ? length() : Math.min(length(), start + maxBytes);
        long needToScan = last - start;

        // check what ever is now in the buffer
        int bytesAvailable = (int) (dataEnd - filePosition);
        if (bytesAvailable < 1) {
            seek(filePosition); // read a new buffer
            bytesAvailable = (int) (dataEnd - filePosition);
        }
        int bufStart = (int) (filePosition - bufferStart);
        int scanBytes = (int) Math.min(bytesAvailable, needToScan);
        int pos = match.indexOf(buffer, bufStart, scanBytes);
        if (pos >= 0) {
            seek(bufferStart + pos);
            return true;
        }

        int matchLen = match.getMatchLength();
        needToScan -= scanBytes - matchLen;

        while (needToScan > matchLen) {
            readBuffer(dataEnd - matchLen); // force new buffer

            scanBytes = (int) Math.min(buffer.length, needToScan);
            pos = match.indexOf(buffer, 0, scanBytes);
            if (pos > 0) {
                seek(bufferStart + pos);
                return true;
            }

            needToScan -= scanBytes - matchLen;
        }

        // failure
        seek(last);
        return false;
    }

}
