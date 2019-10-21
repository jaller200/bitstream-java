package com.darkflame.utils.bitstream;

import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import java.util.Arrays;

/**
 * An output stream that allows bit write operations.
 */
public class BitOutputStream extends OutputStream {

    // -- Protected Constants

    /** The number of bits in a byte. */
    protected static final int BITS_PER_BYTE = 8;

    /** The default buffer size. */
    protected static final int DEFAULT_BYTE_BUFFER_SIZE = 32;

    /** The UTF-8 encoder. */
    private static final CharsetEncoder UTF8_ENCODER = Charset.forName("UTF-8").newEncoder();

    /** The UTF-16 encoder (big endian). */
    private static final CharsetEncoder UTF16_ENCODER_BE = Charset.forName("UTF-16BE").newEncoder();

    /** The UTF-16 encoder (little endian). */
    private static final CharsetEncoder UTF16_ENCODER_LE = Charset.forName("UTF-16LE").newEncoder();



    // -- Protected Variables

    /* The number of bits that have been allocated. */
    protected int bitsAllocated;

    /** The number of bits that have been used. */
    protected int bitsUsed;

    /** The data buffer. Will expand as necessary */
    protected byte[] buffer;




    // -- Constructor

    /**
     * Creates a bitstream and sets the default buffer widths
     */
    public BitOutputStream() {

        // Create the default buffer
        this.buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

        // Set our sizes
        this.bitsAllocated = BitUtils.bytesToBits(DEFAULT_BYTE_BUFFER_SIZE);
        this.bitsUsed = 0;
    }

    /**
     * Creates a bitstream from a set of byte data. Aligns new information to the byte
     * boundary of the new data.
     * @param data The data
     */
    public BitOutputStream(byte[] data) {
        this(data, data.length);
    }

    /**
     * Creates a bitstream from a set of byte data with the specified length. New information
     * is aligned to byte boundary.
     * @param data The data to copy
     * @param length The length of the data to copy
     */
    public BitOutputStream(byte[] data, int length) {

        // Make sure we have byte data
        if (data == null || data.length == 0)
            throw new IllegalArgumentException("Cannot create a BitStream from a nonexistent or zero-sized byte array.");

        // Make sure that we want to copy data
        if (length <= 0 || length > data.length)
            throw new IllegalArgumentException("Can only copy data within the bounds of the byte array size and non-zero.");

        // Create our buffer
        int allocationSize = DEFAULT_BYTE_BUFFER_SIZE > length ? DEFAULT_BYTE_BUFFER_SIZE : length;
        this.buffer = new byte[allocationSize];

        // Copy our data buffer
        System.arraycopy(data, 0, this.buffer, 0, length);

        // Set our sizes
        this.bitsAllocated = BitUtils.bytesToBits(length);
        this.bitsUsed = this.bitsAllocated;
    }

    /**
     * Copies a bitstream into this bitstream.
     * @param bitOutputStream The old bitstream
     */
    public BitOutputStream(BitOutputStream bitOutputStream) {

        if (bitOutputStream == null)
            throw new IllegalArgumentException("Cannot copy a non-existent bitstream.");

        // Get the number of bytes of the old stream (will be DEFAULT_BYTE_BUFFER_SIZE or greater)
        byte[] oldBuffer = bitOutputStream.buffer;
        this.buffer = new byte[oldBuffer.length];

        // Copy the data
        System.arraycopy(oldBuffer, 0, this.buffer, 0, oldBuffer.length);

        // Set our data
        this.bitsAllocated = bitOutputStream.bitsAllocated;
        this.bitsUsed = bitOutputStream.bitsUsed;
    }


    // -- Buffer Methods

    /**
     * Checks if we have enough space to write data.
     * @param bitsToWrite The number of bits that we need to write.
     */
    private void checkCapacity(int bitsToWrite) {

        if (bitsToWrite < 0)
            throw new IllegalArgumentException("You can only check capacity with a non-negative amount of bits.");

        int bitsNeeded = this.bitsUsed + bitsToWrite;
        int bitsAllocated = this.bitsAllocated;

        if (bitsNeeded > bitsAllocated) {
            growCapacity(bitsNeeded);
        }
    }

    /**
     * Grows the capacity of the buffer by doubling it. Less memory efficient, but
     * has a faster time (less calls to copy the buffer).
     * @param bitsNeeded The number of bits that we need to write
     */
    private void growCapacity(int bitsNeeded) {

        // Convert to bytes
        int bytesNeeded = BitUtils.bitsToBytes(bitsNeeded);

        // Try to double our buffer size
        int oldCapacity = this.buffer.length;
        int newCapacity = oldCapacity << 1;

        // If we need something higher, go up until we need what we need
        if (newCapacity - bytesNeeded < 0)
            newCapacity = bytesNeeded;

        if (newCapacity < 0) {

            // If the number of bytes needed overflows, throw an error
            if (bytesNeeded < 0)
                throw new OutOfMemoryError();

            // Cap out data at the max value
            newCapacity = Integer.MAX_VALUE;
        }

        // Copy the array
        this.buffer = Arrays.copyOf(this.buffer, newCapacity);
    }



    // -- Data Methods

    /**
     * Returns a copy of our buffer data
     * @return The buffer data
     */
    public byte[] getData() {
        return this.buffer.clone();
    }

    /**
     * Returns the number of bits that we have used.
     * @return The number of bits used
     */
    public int getNumBitsUsed() {
        return this.bitsUsed;
    }

    /**
     * Return the number of bytes that we have used.
     * @return The number of bytes that we have used
     */
    public int getNumBytesUsed() {
        return BitUtils.bitsToBytes(this.bitsUsed);
    }




    // -- Bit Methods

    /**
     * Writes the specified number of bits to the bitstream (right aligned).
     * @param data The data to write
     * @param numBits The number of bits to write
     */
    public void writeBits(byte[] data, int numBits) {
        this.writeBits(data, numBits, true);
    }

    /**
     * Writes the specified number of bits to the bitstream.
     * @param data The data to write
     * @param numBits The number of bits of the data to write
     * @param rightAligned Whether or not to align bits to the right or not when less than we need for a full byte
     */
    public void writeBits(byte[] data, int numBits, boolean rightAligned) {

        if (numBits < 0 || (data.length * BITS_PER_BYTE) < numBits)
            throw new IllegalArgumentException("The number of bits to write must be between 0" +
                    "and the bit length of the data.");

        // Make sure we have enough space
        this.checkCapacity(numBits);

        // Get the start offset
        byte dataByte;
        int offset = 0;

        // Get the bit that we are starting to write on
        int bitsUsedMod8 = this.bitsUsed & 0xF;

        // Now start parsing the data
        while (numBits > 0) {

            // Get the next piece of data to write
            dataByte = data[offset];

            // If we have less bits to write than make up a byte, create a byte and
            // align it as specified (left is default).
            if (numBits < 8 && rightAligned) {
                dataByte <<= (byte) (BITS_PER_BYTE - numBits);
            }

            // If we are already on a byte boundary, we can just set the next byte to our data
            //
            // NOTE: We don't mask anything here, although we could. This is due to the fact that
            // extraneous bits will be overwritten in the future.
            //
            // TODO: Determine if it would be better to mask (i.e., should we only write the bits
            // TODO: that we want, or should we write everything and let it be overwritten?
            if (bitsUsedMod8 == 0) {
                this.buffer[this.bitsUsed >>> 3] = dataByte;
            } else {

                // If we are here, our starting bit is not the last bit of a byte, so we need to
                // write partial data to the current byte and then start a new byte.
                this.buffer[this.bitsUsed >>> 3] |= (byte) (dataByte >>> bitsUsedMod8);

                // If we have remaining data, write it here
                int remaining = BITS_PER_BYTE - bitsUsedMod8;
                if (remaining < 8 && remaining < numBits) {
                    this.buffer[(this.bitsUsed >>> 3) + 1] = (byte) (dataByte << remaining);
                }
            }

            // Determine how much we wrote
            if (numBits >= BITS_PER_BYTE) {

                // Increase our bits used
                this.bitsUsed += BITS_PER_BYTE;

                // Subtract what we used
                numBits -= BITS_PER_BYTE;
            } else {

                // Add the remaining bits
                this.bitsUsed += numBits;

                // We've used everything
                numBits = 0;
            }

            // Increase our offset
            offset++;
        }
    }



    // -- Data Write Methods (Primitives)

    /**
     * Writes the lower 8 bits of the input integer to the stream.
     *
     * This is just a simple call to {@link BitOutputStream#writeByte(byte)} meant
     * to satisfy the OutputStream contract.
     *
     * @param b The data to write
     */
    @Override
    public void write(int b) {
        this.writeByte((byte) b);
    }

    /**
     * Writes a 0 bit to the output stream.
     */
    public void write0() {

        byte[] data = {0x00};
        this.writeBits(data, 1);
    }

    /**
     * Writes a 1 bit to the output stream.
     */
    public void write1() {

        byte[] data = {0x01};
        this.writeBits(data, 1);
    }

    /**
     * Writes a boolean to the output stream. To conserve space,
     * boolean values are written as a single bit instead of a single byte.
     * @param data The boolean value
     */
    public void writeBoolean(boolean data) {
        if (data)
            write1();
        else
            write0();
    }

    /**
     * Writes a byte to the output stream.
     * @param data The byte to write
     */
    public void writeByte(byte data) {

        byte[] arr = {data};
        this.writeBits(arr, Byte.SIZE);
    }

    /**
     * Writes a character to the output stream.
     * @param data The character value
     */
    public void writeChar(char data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putChar(data).array(), Character.SIZE);
    }

    /**
     * Writes a character to the output stream in little-endian.
     * @param data The character value
     */
    public void writeCharLE(char data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar(data).array(), Character.SIZE);
    }

    /**
     * Writes a double to the output stream.
     * @param data The double value
     */
    public void writeDouble(double data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(data).array(), Double.SIZE);
    }

    /**
     * Writes a double to the output stream in little-endian.
     * @param data The double value
     */
    public void writeDoubleLE(double data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(data).array(), Double.SIZE);
    }

    /**
     * Writes a float to the output stream.
     * @param data The float value
     */
    public void writeFloat(float data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(data).array(), Float.SIZE);
    }

    /**
     * Writes a float to the output stream in little-endian.
     * @param data The float value
     */
    public void writeFloatLE(float data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(data).array(), Float.SIZE);
    }

    /**
     * Writes an integer to the output stream.
     * @param data The integer value
     */
    public void writeInteger(int data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(data).array(), Integer.SIZE);
    }

    /**
     * Writes an integer to the output stream in little-endian.
     * @param data The integer value
     */
    public void writeIntegerLE(int data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array(), Integer.SIZE);
    }

    /**
     * Writes a long to the output stream.
     * @param data The long value
     */
    public void writeLong(long data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(data).array(), Long.SIZE);
    }

    /**
     * Writes a long to the output stream in little-endian.
     * @param data The long data
     */
    public void writeLongLE(long data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(data).array(), Long.SIZE);
    }

    /**
     * Writes a short to the output stream.
     * @param data The short value
     */
    public void writeShort(short data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(data).array(), Short.SIZE);
    }

    /**
     * Writes a short to the output stream in little-endian.
     * @param data The short value
     */
    public void writeShortLE(short data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array(), Short.SIZE);
    }



    // -- Data Write Methods (Objects)

    /**
     * Writes a BitOutputStream to this output stream.
     * @param bitOutputStream The old stream
     */
    public void writeBitStream(BitOutputStream bitOutputStream) {

        // First, make sure we have data
        if (bitOutputStream == null)
            throw new IllegalArgumentException("Cannot write a null output stream");

        // If we have no bits in this stream, just return.
        if (bitOutputStream.bitsUsed == 0) return;

        // Otherwise, write the data
        this.writeBits(bitOutputStream.buffer, bitOutputStream.bitsUsed);
    }



    // -- Data Write Methods (Strings)

    /**
     * A private helper function to write UTF strings.
     * @param str The string to write
     * @param encoder The encoder to use
     * @param addNullTerminator Whether or not to add the null terminator
     */
    private void writeString(String str, CharsetEncoder encoder, boolean addNullTerminator) {

        if (str == null)
            throw new IllegalArgumentException("Cannot write a null string to the output stream.");

        if (encoder == null)
            throw new IllegalArgumentException("Cannot write string with an invalid charset encoder");

        // Create our byte array
        byte[] strArray = new byte[addNullTerminator ? str.length() + 1 : str.length()];
        ByteBuffer strBuffer = ByteBuffer.wrap(strArray);

        // Encode the data and terminate it
        encoder.encode(CharBuffer.wrap(str), strBuffer, true);
        if (addNullTerminator)
            strArray[str.length()] = 0x00;

        // Write the data
        this.writeBits(strArray, strArray.length * BITS_PER_BYTE);
    }

    /**
     * Writes a null-terminated UTF-8 string to the output stream.
     * @param str The string to write
     */
    public void writeUTF8String(String str) {
        this.writeUTF8String(str, true);
    }

    /**
     * Writes a UTF-8 string to the output stream.
     * @param str The string to write
     * @param addNullTerminator Whether or not to add a null terminator
     */
    public void writeUTF8String(String str, boolean addNullTerminator) {
        this.writeString(str, UTF8_ENCODER, addNullTerminator);
    }

    /**
     * Writes a null-terminated UTF-16 string to the output stream in big-endian.
     * @param str The string to write
     */
    public void writeUTF16String(String str) {
        this.writeUTF16String(str, true);
    }

    /**
     * Writes a UTF-16 string to the output stream in big-endian.
     * @param str The string to write
     * @param addNullTerminator Whether or not to add a null terminator
     */
    public void writeUTF16String(String str, boolean addNullTerminator) {
        this.writeString(str, UTF16_ENCODER_BE, true);
    }

    /**
     * Writes a null-terminated UTF-16 string to the output stream in little-endian.
     * @param str The string to write
     */
    public void writeUTF16StringLE(String str) {
        this.writeUTF16StringLE(str, true);
    }

    /**
     * Writes a UTF-16 string to the output stream in little-endian
     * @param str The string to write
     * @param addNullTerminator Whether or not to add a null terminator
     */
    public void writeUTF16StringLE(String str, boolean addNullTerminator) {
        this.writeString(str, UTF16_ENCODER_LE, true);
    }



    // -- Object Methods

    /**
     * Returns a string representation of the class.
     * @return The string representation of the class
     */
    @Override
    public String toString() {
        return "BitOutputStream[bitsAllocated=" + this.bitsAllocated + ", bitsUsed=" + this.bitsUsed + "]";
    }
}
