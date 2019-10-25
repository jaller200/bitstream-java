package com.darkflame.utils.bitstream;

import java.io.EOFException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An input stream that allows bit read operations.
 */
public class BitInputStream extends InputStream {

    // -- Protected Variables

    /** The number of bits that have been used. */
    protected int bitsUsed;

    /** The data buffer. This is fixed by the input data. */
    protected byte[] buffer;

    /** The read offset in bits. */
    protected int readOffset;



    // -- Constructor

    /**
     * Creates a bit input stream with the given data.
     * @param data The input data
     */
    public BitInputStream(byte[] data) {

        // Make sure the data exists
        if (data == null)
            throw new IllegalArgumentException("Cannot create an input stream from null data!");

        // Set the buffer data
        this.buffer = data;

        // Set our variables
        this.bitsUsed = BitUtils.bytesToBits(this.buffer.length);
        this.readOffset = 0;
    }



    // -- Bit Methods

    /**
     * Reads the specified number of bits from the input, right-aligned.
     * @param numBits The number of bits to read
     * @return The bits read
     * @throws EOFException Thrown if we reach the end of stream prematurely
     */
    public synchronized byte[] readBits(int numBits) throws EOFException {
        return this.readBits(numBits, true);
    }

    /**
     * Reads the specified number of bits from the input stream.
     * @param numBits The number of bits to read.
     * @param rightAligned Whether or not to align bits to the right when less than we need for a full byte.
     * @return The bits read
     * @throws EOFException Thrown if we reach the end of stream prematurely
     */
    public synchronized byte[] readBits(int numBits, boolean rightAligned) throws EOFException {

        // Make sure we are reading bits
        if (numBits <= 0)
            throw new IllegalArgumentException("We can only read positive numbers of bits!");

        // Make sure we are within the bounds.
        if (this.readOffset + numBits > bitsUsed)
            throw new EOFException("Cannot read more bits than exist in the stream relative to the read offset");

        // Create our array to write to
        byte[] output = new byte[BitUtils.bitsToBytes(numBits)];

        // Now get the bit that we are currently on
        int readOffsetMod8 = this.readOffset & 0xF;

        // If we are reading from a byte boundary and we are reading a multiple of 8, just copy the data
        if (readOffsetMod8 == 0 && (numBits % 8) == 0) {

            int startByte = this.readOffset >>> 3;
            System.arraycopy(this.buffer, startByte, output, 0, numBits >>> 3);

            return output;
        }

        // Otherwise, we need to read each bit by bit (no pun indended).
        // Stat by getting our current offset
        int offset = 0;

        // Now start reading bits
        while (numBits > 0) {

            // Copy the first part of the data
            output[offset] |= this.buffer[this.readOffset >>> 3] << readOffsetMod8;

            // If we need to copy more to a new byte, do so here
            int remaining = BitConstants.BITS_PER_BYTE - readOffsetMod8;
            if ((readOffsetMod8 > 0) && (numBits > remaining))
                output[offset] |= (this.buffer[(this.readOffset >>> 3) + 1] & 0xFF) >>> remaining;

            // Determine how much is left to read
            if (numBits >= BitConstants.BITS_PER_BYTE) {

                // Subtract the bits and update our current offset
                numBits -= BitConstants.BITS_PER_BYTE;
                offset++;

                // Update our read offset
                this.readOffset += BitConstants.BITS_PER_BYTE;
            } else {

                // Otherwise, determine how many less than 8 we need
                byte neg = (byte) (numBits - BitConstants.BITS_PER_BYTE);

                // If we have less bits than needed for a byte, read the partial byte
                if (neg < 0) {

                    // If we are aligning to the right, do so here
                    byte shift = (byte) (-neg);
                    if (rightAligned)
                        output[offset] = (byte) ((output[offset] & 0xFF) >> shift);

                    // Update our read offset
                    this.readOffset += BitConstants.BITS_PER_BYTE + neg;
                } else {
                    this.readOffset += BitConstants.BITS_PER_BYTE;
                }

                // Update our current offset
                offset++;

                // Set our number of bits to read at 0
                numBits = 0;
            }
        }

        // Return our output
        return output;
    }



    // -- Data Read Methods (Primitives)

    /**
     * Reads the next byte of data from the input stream.
     * @return The next byte of data from the input stream
     * @throws EOFException Thrown if we reach the end of stream prematurely
     */
    @Override
    public synchronized int read() throws EOFException {
        return (int) this.readByte();
    }

    /**
     * Reads a byte from the input stream.
     * @return The byte read
     * @throws EOFException Thrown if we reach the end of stream prematurely
     */
    public synchronized byte readByte() throws EOFException {

        // Read the next byte
        byte[] data = this.readBits(Byte.SIZE, true);
        return data[0];
    }

    /**
     * Reads a character from the input stream.
     * @return The character read
     * @throws EOFException Thrown if we reach the end of the stream prematurely
     */
    public synchronized char readChar() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Character.SIZE)).order(ByteOrder.BIG_ENDIAN).getChar();
    }

    /**
     * Reads a character from the input stream in little endian
     * @return The character read
     * @throws EOFException Thrown if we reach the end of the stream prematurely
     */
    public synchronized char readCharLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Character.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getChar();
    }

    /**
     * Reads a double from the input stream.
     * @return The double read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized double readDouble() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Double.SIZE)).order(ByteOrder.BIG_ENDIAN).getDouble();
    }

    /**
     * Reads a double from the input stream in little endian.
     * @return The double read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized double readDoubleLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Double.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    /**
     * Reads a float from the input stream.
     * @return The float read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized float readFloat() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Float.SIZE)).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    /**
     * Reads a float from the input stream in little endian.
     * @return The float read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized float readFloatLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Float.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Reads an integer from the input stream.
     * @return The integer read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized int readInteger() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Integer.SIZE)).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    /**
     * Reads an integer from the input stream in little endian.
     * @return The integer read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized int readIntegerLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Integer.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Reads a long from the input stream.
     * @return The long read
     * @throws EOFException Thrown if we reach the end of the stream prematurely
     */
    public synchronized long readLong() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Long.SIZE)).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    /**
     * Reads a long from the input stream in little endian.
     * @return The long read
     * @throws EOFException Thrown if we reach the end of the stream prematurely
     */
    public synchronized long readLongLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Long.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /**
     * Reads a short from the input stream.
     * @return The short read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized short readShort() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Short.SIZE)).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    /**
     * Reads a short from the input stream in little endian
     * @return The short read
     * @throws EOFException Thrown if we reach the end of the stream prematurely.
     */
    public synchronized short readShortLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Short.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }




    // -- Read Methods

    /**
     * Skips the specified number of bits.
     * @param numBits The number of bits to skip
     * @throws EOFException Thrown if we reach the end of the stream prematurely
     */
    public void skipBits(int numBits) throws EOFException {
        if (this.readOffset + numBits > this.bitsUsed)
            throw new EOFException("Cannot skip more bits than are left!");

        // Update our offset
        this.readOffset += numBits;
    }

    /**
     * Skips the specified number of bytes.
     * @param numBytes The number of bytes to skip
     * @throws EOFException Thrown if we reach the end of the stream prematurely
     */
    public void skipBytes(int numBytes) throws EOFException {

        // Just skip the number of bytes that we need
        int numBits = BitUtils.bytesToBits(numBytes);
        this.skipBits(numBits);
    }



    // -- Getter Methods

    /**
     * Returns the number of bits that we have used.
     * @return The number of bits used
     */
    public synchronized int getNumBitsUsed() {
        return this.bitsUsed;
    }

    /**
     * Return the number of bytes that we have used.
     * @return The number of bytes that we have used
     */
    public synchronized int getNumBytesUsed() {
        return BitUtils.bitsToBytes(this.bitsUsed);
    }

    /**
     * Returns the current read offset.
     * @return The current read offset
     */
    public synchronized int getReadOffset() {
        return this.readOffset;
    }
}
