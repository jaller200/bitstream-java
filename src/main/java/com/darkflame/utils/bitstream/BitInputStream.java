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
 * An input stream that contains an internal byte buffer to be read bit-by-bit.
 *
 * <p>An internal counter keeps track of the current bit that is being read,
 * and is updated every time {@link #readBits(int, boolean)} is called.</p>
 *
 * <p>This type of {@link InputStream} is quite useful for things such as
 * networking as it allows data to be packed and unpacked much more tightly
 * and reduces redundant or unnecessary data.</p>
 *
 * <p>For example, in a typical input stream, boolean data is read as a full byte.
 * This means that 7 bits are wasted from whatever wrote the data. In this
 * input stream boolean data is stored as a single bit rather than a full byte.</p>
 *
 * @author  Jonathan Hart
 * @since   1.0.0
 * @see     BitOutputStream
 */
public class BitInputStream extends InputStream {

    // -- Protected Variables

    /**
     * The total number of bits that have been used by the input stream.
     *
     * <p>This will always be a non-negative number as it indicates 0 or more bits
     * are part of the stream.</p>
     *
     * <p>To obtain the number of bytes used by the input stream, simply add 7
     * and divide by 8 as partial bits will form a new byte. You could also use
     * {@link #getNumBytesUsed()} to get the same answer.</p>
     *
     * @since   1.0.0
     * @see     BitInputStream#getNumBytesUsed()
     */
    protected int bitsUsed;

    /**
     * The current bit offset that we are reading from.
     *
     * <p>The bit offset will always start from 0 and continue to increase as data is read
     * until reaching {@link #bitsUsed}.</p>
     *
     * <p>Once the bit offset reaches {@link #bitsUsed}, attempting to read any more data will
     * cause an {@link EOFException} to be thrown.</p>
     *
     * @since   1.0.0
     * @see     BitInputStream#getReadOffset()
     */
    protected int bitOffset;

    /**
     * The internal binary byte buffer that is being read from.
     *
     * <p>Read operations will always start from {@code buffer[0]} and will continue to the
     * index {@code buffer[((bitsUsed+7) >> 3) - 1}.</p>
     *
     * @since   1.0.0
     */
    protected byte[] buffer;



    // -- Constructor

    /**
     * Creates a bit input stream from an array of binary data stored in a byte array.
     *
     * <p>This also sets the number of bits used to be equal to the number of bytes
     * multiplied by 8, since there is no external information to tell us otherwise, and
     * resets the current bit offset to zero.</p>
     *
     * @since   1.0.0
     * @param   data The input data
     * @see     BitInputStream#BitInputStream(byte[], int)
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

    /**
     * Creates a bit input stream from an array of binary data stored in a byte array.
     *
     * <p>This allows the developer to specify the number of bits that were used in the original
     * bitstream (or less) which would lead to an {@link EOFException} being thrown when trying
     * to reach past this specified number of bits while reading data.</p>
     *
     * @since   1.0.0
     * @param   data The binary data
     * @param   bitsUsed The number of bits that were used
     * @see     BitInputStream#BitInputStream(byte[])
     */
    public BitInputStream(byte[] data, int bitsUsed) {

        if (data == null || bitsUsed > BitUtils.bytesToBits(data.length))
            throw new IllegalArgumentException("Cannot create a bit input stream with null data or more bits than available");

        // Set the buffer data (no need to copy - we keep an internal reference in the class)
        this.buffer = data;

        // Set our variables
        this.bitsUsed = bitsUsed;
        this.bitOffset = 0;
    }



    // -- Input Stream Methods

    /**
     * Reads the next byte of data from the input stream.
     *
     * <p>The value is returned as an integer in the range of {@code 0} to {@code 255}.</p>
     *
     * <p>If the end of the input stream is reached before a full byte could be read, a
     * {@link EOFException} is thrown to let the developer know that they have reached the end
     * of the data stream unexpectedly.</p>
     *
     * @since   1.0.0
     * @return  A byte in the range of {@code 0} to {@code 255}
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readByte()
     */
    @Override
    public synchronized int read() throws EOFException {
        return (int) (this.readByte()) & 0xFF;
    }



    // -- Read Methods

    /**
     * Reads the specified number of bits from the input stream.
     *
     * <p>The values that are read are returned as a byte array that include enough bytes to
     * hold all bit values. Any partial bits left over that are not enough to form a new byte
     * will be padded and right-aligned to make up one final byte.</p>
     *
     * <p>For example, reading 1 bit will return a byte array of a single value. Reading 8 bits
     * will do the same. Reading 9 bits will return a byte array with 2 values, and so on.</p>
     *
     * <p>If more bits are attempted to be read than exist in the input stream relative to the
     * current read offset, this will throw an {@link EOFException} and the read offset will not
     * be updated.</p>
     *
     * @since   1.0.0
     * @param   numBits The number of bits to read
     * @return  A byte array containing all bits that have been read
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readBits(int, boolean)
     */
    public synchronized byte[] readBits(int numBits) throws EOFException {
        return this.readBits(numBits, true);
    }

    /**
     * Reads a specified number of bits from the input stream, with any extra bits either right-
     * or left-aligned.
     *
     * <p>The values that are read are returned as a byte array that include enough bytes to hold
     * all bit values. Any partial bits remaining will be either right- or left- padded with 0 bits
     * in order to make another full final byte.</p>
     *
     * <p>For example, reading 1 bit will return a byte array of a single value. Reading 8 bits will
     * do the same. Reading 9 bits will return a byte array with 2 values, and so on.</p>
     *
     * <p>If more bits are attempted to be read than exist in the input stream relative to the current
     * read offset, this will throw an {@link EOFException} and the read position will not be updated.</p>
     *
     * @since   1.0.0
     * @param   numBits The number of bits to read
     * @param   rightAlign Whether or not to right align extraneous bits
     * @return  A byte array containing all bits that have been read
     * @throws  EOFException If the end of the stream is reached unexpectedly
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
     * Reads and returns a single byte value from the underlying stream.
     *
     * <p>The byte returned is treated as a signed value in the range of <code>-128</code> to
     * <code>127</code> inclusive.</p>
     *
     * <p>If the end of the input stream is reached before a full byte could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading data written with
     * {@link BitOutputStream#writeByte(byte)}</p>
     *
     * @since   1.0.0
     * @return  The signed byte value read
     * @throws  EOFException If the end of the stream is reached unexpectedly
     */
    public synchronized byte readByte() throws EOFException {

        // Read the next byte
        byte[] data = this.readBits(Byte.SIZE, true);
        return data[0];
    }

    /**
     * Reads and returns a single character from the underlying stream. Characters in Java are
     * two bytes long (16 bits).
     *
     * <p>This method will read the character in little-endian notation, for example,
     * {@code 1 = 0x00 0x01}</p>
     *
     * <p>If the end of the stream is reached before the full two bytes could be read, a
     * {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeChar(char)}.</p>
     *
     * @since   1.0.0
     * @return  The character value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readCharLE()
     */
    public synchronized char readChar() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Character.SIZE)).order(ByteOrder.BIG_ENDIAN).getChar();
    }

    /**
     * Reads and returns a single character from the underlying stream. Characters in Java are
     * two bytes long (16 bits).
     *
     * <p>This method will read the character in little-endian notation, for example,
     * {@code 1 = 0x01 0x00}</p>
     *
     * <p>If the end of the input stream is reached before the full 2 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeCharLE(char)}.</p>
     *
     * @since   1.0.0
     * @return  The character value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readChar()
     */
    public synchronized char readCharLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Character.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getChar();
    }

    /**
     * Reads and returns a double value from the underlying stream.
     *
     * <p>This method will read the double value as 8 bytes in big endian order</p>
     *
     * <p>If the end of the input stream is reached before the full 8 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeDouble(double)}</p>
     *
     * @since   1.0.0
     * @return  The double value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readDoubleLE()
     */
    public synchronized double readDouble() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Double.SIZE)).order(ByteOrder.BIG_ENDIAN).getDouble();
    }

    /**
     * Reads and returns a double value from the underlying stream.
     *
     * <p>This method will read the double value as 8 bytes in little endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 8 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeDoubleLE(double)}</p>
     *
     * @since   1.0.0
     * @return  The double value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readDouble()
     */
    public synchronized double readDoubleLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Double.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    /**
     * Reads and returns a float value from the underlying stream.
     *
     * <p>This method will read the float value as 4 bytes in big endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 4 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeFloat(float)}</p>
     *
     * @since   1.0.0
     * @return  The float value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readFloatLE()
     */
    public synchronized float readFloat() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Float.SIZE)).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    /**
     * Reads and returns a float value from the underlying stream.
     *
     * <p>This method will read the float value as 4 bytes in little endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 4 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeFloatLE(float)}</p>
     *
     * @since   1.0.0
     * @return  The float value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readFloat()
     */
    public synchronized float readFloatLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Float.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Reads and returns an integer value from the underlying stream.
     *
     * <p>This method will read the integer value as 4 bytes in big endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 4 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeInteger(int)}</p>
     *
     * @since   1.0.0
     * @return  The integer value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readIntegerLE()
     */
    public synchronized int readInteger() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Integer.SIZE)).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    /**
     * Reads and returns an integer value from the underlying stream.
     *
     * <p>This method will read the integer value as 4 bytes in little endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 4 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeIntegerLE(int)}</p>
     *
     * @since   1.0.0
     * @return  The integer value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readInteger()
     */
    public synchronized int readIntegerLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Integer.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Reads and returns a long value from the underlying stream.
     *
     * <p>This method will read the long value as 8 bytes in big endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 8 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeLong(long)}</p>
     *
     * @since   1.0.0
     * @return  The long value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readLongLE()
     */
    public synchronized long readLong() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Long.SIZE)).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    /**
     * Reads and returns a long value from the underlying stream.
     *
     * <p>This method will read the long value as 8 bytes in little endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 8 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeLongLE(long)}</p>
     *
     * @since   1.0.0
     * @return  The long value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readLong()
     */
    public synchronized long readLongLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Long.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /**
     * Reads and returns a short value from the underlying stream.
     *
     * <p>This method will read the short value as 2 bytes in big endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full two bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeShort(short)}</p>
     *
     * @since   1.0.0
     * @return  The short value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readShortLE()
     */
    public synchronized short readShort() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Short.SIZE)).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    /**
     * Reads and returns a short value from the underlying stream.
     *
     * <p>This method will read the short value as 2 bytes in big endian order.</p>
     *
     * <p>If the end of the input stream is reached before the full 2 bytes could be read,
     * a {@link EOFException} will be thrown and the current read position will not be updated.</p>
     *
     * <p>This method is suitable for reading the data written by
     * {@link BitOutputStream#writeShortLE(short)}</p>
     *
     * @since   1.0.0
     * @return  The short value
     * @throws  EOFException If the end of the stream is reached unexpectedly
     * @see     BitInputStream#readShort()
     */
    public synchronized short readShortLE() throws EOFException {
        return ByteBuffer.wrap(this.readBits(Short.SIZE)).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }




    // -- Read Methods

    /**
     * Skips over a specified number of bits in the input stream.
     *
     * <p>Essentially, this method moves the bit read offset forward by the specified number
     * of bits.</p>
     *
     * <p>If we try to read more bits than are available to us, an {@link EOFException} is thrown to
     * let the developer know.</p>
     *
     * <p>One can never skip backwards in bits - {@code numBits} must always be a positive or zero
     * integer value.</p>
     *
     * @since   1.0.0
     * @param   numBits the number of bits to skip over.
     * @throws  EOFException If the end of the stream is reached unexpectedly.
     * @see     BitInputStream#skipBytes(int)
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
     * Skips over a specified number of bytes in the input stream.
     *
     * <p>This is basically just a fancy call to {@link #skipBits(int)} by converting the number
     * of bytes to skip into bits and passing that to the method call.</p>
     *
     * <p>If there are less bits in the input stream than are being read, an {@link EOFException} is
     * thrown.</p>
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
     * Returns the number of bits that remain to be read in the input stream.
     *
     * <p>This method will allow programmers to check themselves as they read a bitstream so that
     * boundary conditions are never exceeded.</p>
     *
     * <p>The way we calculated this is by taking the total number of bits used and subtracting
     * the current bit read offset.</p>
     *
     * <p>Note that compared to the data that was sent, this method may display up to 7 more bits as
     * the number of bits used is not stored internally in the stream, so creating an input stream
     * will always include an extra byte to compensate for partial bytes (less than 8 bits). This means
     * that ultimately it is up to the developer to make sure that the number of bits they are reading
     * does not exceed the number of bits that were written to the data in the first place.</p>
     *
     * <p>The developer also has the ability to specify the number of bits used in the input stream
     * themselves, which would in turn give the correct answer.</p>
     *
     * @since   1.0.0
     * @return  The number of bits left to be read
     * @see     BitInputStream#getNumBytesLeft()
     */
    public synchronized int getNumBitsLeft() {
        return (this.bitsUsed - this.bitOffset);
    }

    /**
     * Returns the number of bytes that remain to be read in the input stream.
     *
     * <p>This simply does the same computation as {@link #getNumBitsLeft()}, but also converts that answer
     * into the number of bytes remaining.</p>
     *
     * @since   1.0.0
     * @return  The number of bytes left to be read
     * @see     BitInputStream#getNumBitsLeft()
     */
    public synchronized int getNumBytesLeft() {
        return BitUtils.bitsToBytes(this.bitsUsed - this.bitOffset);
    }

    /**
     * Returns the total number of bits that are in use by the input stream.
     *
     * @since   1.0.0
     * @return  The number of bits that are in use
     * @see     BitInputStream#getNumBytesUsed()
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
     * @see     BitInputStream#getNumBitsUsed()
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
     * @see     BitInputStream#resetReadOffset()
     * @see     BitInputStream#setReadOffset(int)
     */
    public synchronized int getReadOffset() {
        return this.bitOffset;
    }



    // -- Setter Methods

    /**
     * Resets the read offset back to 0.
     *
     * @since   1.0.0
     * @see     BitInputStream#getReadOffset()
     * @see     BitInputStream#setReadOffset(int)
     */
    public synchronized void resetReadOffset() {
        this.bitOffset = 0;
    }

    /**
     * Sets the read offset for the input stream.
     *
     * <p>Note that the new read offset must be between 0 and the total number
     * of bits used (which can be read via {@link #getNumBitsUsed()}</p>
     *
     * @since   1.0.0
     * @param   readOffset The new read offset
     * @see     BitInputStream#getReadOffset()
     * @see     BitInputStream#getNumBitsUsed()
     * @see     BitInputStream#getNumBytesUsed()
     */
    public synchronized void setReadOffset(int readOffset) {

        // Make sure we are in bounds
        if (readOffset > this.bitsUsed)
            throw new IllegalArgumentException("Cannot move the read offset past the bounds of the input stream");

        this.bitOffset = readOffset;
    }
}
