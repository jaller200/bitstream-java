/*
 * Copyright 2019 Jonathan Hart. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.darkflame.utils.bitstream;

import java.io.EOFException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A {@link BitInputStream} contains an internal byte buffer
 * that will be read bit-by-bit. An internal counter keeps
 * track of the current bit that is being read, and is updated
 * everytime {@link BitInputStream#readBits(int, boolean)} is
 * called.
 * <br>
 * This type of {@link InputStream} is quite useful for things
 * such as networking as it allows data to be packed and
 * unpacked much more tightly and reduces redundant or unnecessary
 * data.
 * <br>
 * For example, in a typical {@link InputStream}, boolean data
 * is read as a full byte. This means that 7 bits are wasted
 * from whatever wrote the data. In this {@link BitInputStream},
 * boolean data is stored as a single bit rather than a full byte.
 * <br>
 * In addition, it is up to the programmer to understand the original
 * format of the data, as it can be quite unsafe reading unknown
 * binary information.
 *
 * @author  Jonathan Hart
 * @since   1.0.0
 */
public class BitInputStream extends InputStream {

    // -- Protected Variables

    /**
     * The number of bits that have been used by the
     * input stream. To obtain the number of bytes used
     * by the input stream, simply add 7 and divide by 8
     * as partial bits will form a new byte.
     * <br>
     * This will always be a non-negative number as
     * it indicates 0 or more bits are part of the
     * stream.
     *
     * @since   1.0.0
     */
    protected int bitsUsed;

    /**
     * The current offset that we are reading from. This
     * offset is also measured in bits and signifies the
     * current bit that we are currently reading.
     * <br>
     * It will always start from 0 and continue to increase as
     * data is read until reaching {@link BitInputStream#bitsUsed}.
     * <br>
     * Once the bit offset reaches {@link BitInputStream#bitsUsed},
     * attempting to read any more data will cause an
     * {@link EOFException} to be thrown.
     *
     * @since   1.0.0
     */
    protected int bitOffset;

    /**
     * An array of bytes that was provided in the
     * constructor to read from.
     * <br>
     * Read operations will always start from
     * <code>buffer[0]</code> and will continue to the
     * index <code>buffer[(bitsUsed + 7) >> 3 - 1].</code>
     *
     * @since   1.0.0
     */
    protected byte[] buffer;



    // -- Constructor

    /**
     * Creates a bit input stream from an array of binary
     * data stored in a byte array.
     * <br>
     * This also sets the number of bits used to be equal
     * to the number of bytes multiplied by 8, since there
     * is no external information to tell us otherwise, and
     * resets the current bit offset to zero.
     *
     * @since   1.0.0
     * @param   data The input data
     */
    public BitInputStream(byte[] data) {

        // Make sure the data exists
        if (data == null)
            throw new IllegalArgumentException("Cannot create a bit input stream from null data");

        // Set the buffer data
        this.buffer = data;

        // Set our variables
        this.bitsUsed = BitUtils.bytesToBits(this.buffer.length);
        this.bitOffset = 0;
    }



    // -- Input Stream Methods

    /**
     * Reads the next byte of data from the input stream. The
     * value is returned as a <code>int</code> in the range of
     * <code>0</code> to <code>255</code>.
     * <br>
     * If we have reached the end of the input stream before
     * we could read a full byte, we throw an {@link EOFException}
     * to let the developer know that they have reached the end
     * of the data stream unexpectedly.
     * <br>
     * It is up to the the developer to make sure they are reading
     * within the bounds of the data that they have.
     *
     * @since   1.0.0
     * @return  A byte in the range of <code>0</code> to
     *          <code>255</code>
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    @Override
    public synchronized int read() throws EOFException {
        return this.readByte();
    }



    // -- Read Methods

    /**
     * Reads a specified number of bits from the input stream. The
     * values that are read are returned as a byte array that include
     * enough bytes to hold all bit values. Any partial bits left over
     * that are not enough to form a new byte will be padded and
     * right-aligned to make up one final byte.
     * <br>
     * For example, reading 1 bit will return a byte array of a single
     * value. Reading 8 bits will do the same. Reading 9 bits will return
     * a byte array with 2 values, and so on.
     * <br>
     * If more bits are attempted to be read than exist in the input
     * stream relative to the current read offset, this will throw
     * an {@link EOFException}.
     *
     * @since   1.0.0
     * @param   numBits The number of bits to read
     * @return  A byte array containing all bits that have been
     *          read
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     * @see     BitInputStream#readBits(int, boolean)
     */
    public synchronized byte[] readBits(int numBits) throws EOFException {
        return this.readBits(numBits, true);
    }

    /**
     * Reads a specified number of bits from the input stream. The
     * values that are read are returned as a byte array that include
     * enough bytes to hold all bit values.
     * <br>
     * For example, reading 1 bit will return a byte array of a single
     * value. Reading 8 bits will do the same. Reading 9 bits will return
     * a byte array with 2 values, and so on.
     * <br>
     * If more bits are attempted to be read than exist in the input
     * stream relative to the current read offset, this will throw
     * an {@link EOFException} and the read position will not be
     * updated.
     *
     * @since   1.0.0
     * @param   numBits The number of bits to read
     * @param   rightAlign Whether or not to right align extraneous bits
     * @return  A byte array containing all bits that have been
     *          read
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     * @see     BitInputStream#readBits(int)
     */
    public synchronized byte[] readBits(int numBits, boolean rightAlign) throws EOFException {

        // Make sure we are reading bits
        if (numBits <= 0)
            throw new IllegalArgumentException("We can only read a positive number of bits");

        // Make sure we are within the bounds.
        if (this.bitOffset + numBits > bitsUsed)
            throw new EOFException("Cannot read more bits than exist in the input stream relative to the current read offset");

        // Create our array to write to
        int size = BitUtils.bitsToBytes(numBits);
        byte[] output = new byte[size];

        // Now get the bit that we are currently on
        int readOffsetMod8 = this.bitOffset & 0xF;

        // If we are reading from a byte boundary and we are reading a multiple of 8, just copy the data
        if (readOffsetMod8 == 0 && (numBits % 8) == 0) {

            int startByte = this.bitOffset >>> 3;
            System.arraycopy(this.buffer, startByte, output, 0, numBits >>> 3);

            return output;
        }

        // Otherwise, we need to read each bit by bit (no pun intended).
        // Stat by getting our current offset
        int offset = 0;

        // Now start reading bits
        while (numBits > 0) {

            // Copy the first part of the data
            output[offset] |= this.buffer[this.bitOffset >>> 3] << readOffsetMod8;

            // If we need to copy more to a new byte, do so here
            int remaining = BitConstants.BITS_PER_BYTE - readOffsetMod8;
            if ((readOffsetMod8 > 0) && (numBits > remaining))
                output[offset] |= (this.buffer[(this.bitOffset >>> 3) + 1] & 0xFF) >>> remaining;

            // Determine how much is left to read
            if (numBits >= BitConstants.BITS_PER_BYTE) {

                // Subtract the bits and update our current offset
                numBits -= BitConstants.BITS_PER_BYTE;
                offset++;

                // Update our read offset
                this.bitOffset += BitConstants.BITS_PER_BYTE;
            } else {

                // Otherwise, determine how many less than 8 we need
                byte neg = (byte) (numBits - BitConstants.BITS_PER_BYTE);

                // If we have less bits than needed for a byte, read the partial byte
                if (neg < 0) {

                    // If we are aligning to the right, do so here
                    byte shift = (byte) (-neg);
                    if (rightAlign)
                        output[offset] = (byte) ((output[offset] & 0xFF) >> shift);

                    // Update our read offset
                    this.bitOffset += BitConstants.BITS_PER_BYTE + neg;
                } else {
                    this.bitOffset += BitConstants.BITS_PER_BYTE;
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

    /**
     * Reads and returns a single byte value.
     * <br>
     * The byte returned is treated as a signed value
     * in the range of <code>-128</code> to <code>127</code>
     * inclusive.
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeByte(byte)}.
     *
     * @since   1.0.0
     * @return  The signed byte value read
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized byte readByte() throws EOFException {

        // Read the next byte
        byte[] data = this.readBits(Byte.SIZE, true);
        return data[0];
    }

    /**
     * Reads and returns two byte values as a <code>char</code>
     * value.
     * <br>
     * The character read in this method is done as if it were
     * written in big-endian format (MSB stored first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeChar(char)}.
     *
     * @since   1.0.0
     * @return  The character value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized char readChar() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Character.SIZE)).order(ByteOrder.BIG_ENDIAN).getChar();
    }

    /**
     * Reads and returns two byte values as a <code>char</code>.
     * <br>
     * The character read in this method is done as if it were
     * written in little-endian format (LSB stored first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeCharLE(char)}.
     *
     * @since   1.0.0
     * @return  The character value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized char readCharLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Character.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getChar();
    }

    /**
     * Reads and returns an 8 byte <code>double</code> value.
     * <br>
     * The double is read by first reading in 64 bits and then
     * converting those bits to a double value.
     * <br>
     * In addition, the double is read as if it were in
     * big-endian format here (MSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeDouble(double)}.
     *
     * @since   1.0.0
     * @return  The double value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized double readDouble() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Double.SIZE)).order(ByteOrder.BIG_ENDIAN).getDouble();
    }

    /**
     * Reads and returns an 8 byte <code>double</code> value.
     * <br>
     * The double is read by first reading in 64 bits and then
     * converting those bits to a double value.
     * <br>
     * In addition, the double is read as if it were in
     * little-endian format here (LSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeDoubleLE(double)}.
     *
     * @since   1.0.0
     * @return  The double value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized double readDoubleLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Double.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    /**
     * Reads and returns a 4 byte <code>float</code> value.
     * <br>
     * The float is read by first reading in 32 bits and then
     * converting those bits to a float value.
     * <br>
     * In addition, the float is read as if it were in
     * big-endian format here (MSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeFloat(float)}.
     *
     * @since   1.0.0
     * @return  The float value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized float readFloat() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Float.SIZE)).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    /**
     * Reads and returns a 4 byte <code>float</code> value.
     * <br>
     * The float is read by first reading in 32 bits and then
     * converting those bits to a float value.
     * <br>
     * In addition, the float is read as if it were in
     * little-endian format here (LSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeFloatLE(float)}.
     *
     * @since   1.0.0
     * @return  The float value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized float readFloatLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Float.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Reads and returns a 4 byte <code>int</code> value.
     * <br>
     * The integer returned is treated as a signed
     * integer value in the range of <code>-2,147,483,648</code>
     * to <code>2,147,483,647</code>.
     * <br>
     * In addition, the integer is read as if it were in
     * big-endian format here (MSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeInteger(int)}.
     *
     * @since   1.0.0
     * @return  The signed integer value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized int readInteger() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Integer.SIZE)).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    /**
     * Reads and returns a 4 byte <code>int</code> value.
     * <br>
     * The integer returned is treated as a signed
     * integer value in the range of <code>-2,147,483,648</code>
     * to <code>2,147,483,647</code>.
     * <br>
     * In addition, the integer is read as if it were in
     * little-endian format here (LSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeIntegerLE(int)}.
     *
     * @since   1.0.0
     * @return  The signed integer value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized int readIntegerLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Integer.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Reads and returns an 8 byte <code>long</code> value.
     * <br>
     * The long returned is treated as a signed value in the
     * range of <code>-9,223,372,036,854,775,808</code> to
     * <code>9,223,372,036,854,775,807</code>.
     * <br>
     * In addition, the long is read as if it were in big-endian
     * format here (MSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeLong(long)}.
     *
     * @since   1.0.0
     * @return  The signed long value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized long readLong() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Long.SIZE)).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    /**
     * Reads and returns an 8 byte <code>long</code> value.
     * <br>
     * The long returned is treated as a signed value in the
     * range of <code>-9,223,372,036,854,775,808</code> to
     * <code>9,223,372,036,854,775,807</code>.
     * <br>
     * In addition, the long is read as if it were in little-endian
     * format here (LSB first).
     * <br>
     * This method is suitable for reading the data written
     * to a stream by {@link BitOutputStream#writeLongLE(long)}.
     *
     * @since   1.0.0
     * @return  The signed long value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized long readLongLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Long.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /**
     * Reads and returns a 2 byte <code>short</code> value.
     * <br>
     * The short returned is treated as a signed value in
     * the range of <code>-32,768</code> to <code>32,767</code>.
     * <br>
     * In addition, the short is read as if it were in
     * big-endian format here (MSB first).
     * <br>
     * This method is suitable for reading the data written to
     * a stream by {@link BitOutputStream#writeShort(short)}.
     *
     * @since   1.0.0
     * @return  The signed short value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized short readShort() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Short.SIZE)).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    /**
     * Reads and returns a 2 byte <code>short</code> value.
     * <br>
     * The short returned is treated as a signed value in the range
     * of <code>-32,768</code> to <code>32,767</code>.
     * <br>
     * In addition, the short is read as if it were in
     * little-endian format here (LSB first).
     * <br>
     * This method is suitable for reading the data written to
     * a stream by {@link BitOutputStream#writeShortLE(short)}.
     *
     * @since   1.0.0
     * @return  The signed short value
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public synchronized short readShortLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Short.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }




    // -- Read Methods

    /**
     * Tells the input stream to skip over a specified number
     * of bits so that they are not read. This is done by simply
     * making sure that we first have enough data to skip, and
     * then updating the bit offset by adding the number of bits
     * that we are going to skip.
     * <br>
     * If one tries to skip more bits than we have, we throw an
     * {@link EOFException}. In addition, if one tries to skip a
     * negative amount of bits, simply throw an
     * {@link IllegalArgumentException}.
     *
     * @since   1.0.0
     * @param   numBits the number of bits to skip over.
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly.
     */
    public synchronized void skipBits(int numBits) throws EOFException {

        // Make sure we are not skipping negative bits
        if (numBits < 0)
            throw new IllegalArgumentException("Cannot skip a negative amount of bits");

        // Make sure we have enough bits to skip
        if (this.bitOffset + numBits > this.bitsUsed)
            throw new EOFException("Cannot skip more bits than are left to be read");

        // Update our offset
        this.bitOffset += numBits;
    }

    /**
     * Tells the input stream to skip over a specified number of
     * bytes (8 bits each) so that they are not read.
     * <br>
     * This is just a fancy call to {@link BitInputStream#skipBits(int)}
     * by converting the number of bytes to a number of bits and passing
     * those into the method call.
     * <br>
     * The same errors are thrown in this method as well.
     *
     * @since   1.0.0
     * @param   numBytes The number of bytes to skip over.
     * @throws  EOFException If the end of the stream is reached
     *          unexpectedly
     */
    public void skipBytes(int numBytes) throws EOFException {

        // Make sure we are not trying to skip a negative number of bytes
        if (numBytes < 0)
            throw new IllegalArgumentException("Cannot skip a negative amount of bytes");

        // Just skip the number of bytes that we need
        int numBits = BitUtils.bytesToBits(numBytes);
        this.skipBits(numBits);
    }



    // -- Getter Methods

    /**
     * Returns the number of bits that remain to be read from
     * the input stream.
     * <br>
     * This is done by subtracting the current bit offset the
     * total number of bits that have been used by the buffer.
     * <br>
     * This should always return a value between 0 and the total
     * number of bits {@link BitInputStream#bitsUsed} in the input
     * buffer.
     *
     * @since   1.0.0
     * @return  The number of bits left to be read
     */
    public synchronized int getNumBitsLeft() {
        return (this.bitsUsed - this.bitOffset);
    }

    /**
     * Returns the number of bytes that remain to be read
     * from the input stream.
     * <br>
     * This is done by essentially doing the same calculations
     * as {@link BitInputStream#getNumBitsLeft()} to get the
     * number of bits that are left to be read, and then converting
     * those bits to bytes using {@link BitUtils#bitsToBytes(int)}
     * to get the total number of bytes required to hold the number
     * of bits specified.
     * <br>
     * This should always return a value between 0 and the total
     * number of bytes in the input buffer.
     *
     * @since   1.0.0
     * @return  The number of bytes left to be read
     */
    public synchronized int getNumBytesLeft() {
        return BitUtils.bitsToBytes(this.bitsUsed - this.bitOffset);
    }

    /**
     * Returns the number of bits that are currently in use by the
     * input stream.
     * <br>
     * This will always return the length of the {@link BitInputStream#buffer}
     * multiplied by 8 (the number of bits in a byte).
     *
     * @since   1.0.0
     * @return  The number of bits that are in use
     */
    public synchronized int getNumBitsUsed() {
        return this.bitsUsed;
    }

    /**
     * Returns the number of bytes that are currently in use by the
     * input stream.
     * <br>
     * This will always return the length of the {@link BitInputStream#buffer}.
     *
     * @since   1.0.0
     * @return  The number of bytes that are in use
     */
    public synchronized int getNumBytesUsed() {
        return BitUtils.bitsToBytes(this.bitsUsed);
    }

    /**
     * Returns the current bit offset as the current read offset.
     * <br>
     * This will always be a value that begins at 0 and will increase as more
     * data is read from the input stream.
     *
     * @since   1.0.0
     * @return  The current read bit offset
     */
    public synchronized int getReadOffset() {
        return this.bitOffset;
    }
}
