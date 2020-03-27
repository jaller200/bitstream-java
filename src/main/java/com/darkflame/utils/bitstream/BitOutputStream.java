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

import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.Arrays;

/**
 * A {@link BitOutputStream} contains an internal byte buffer
 * that will keep track of all data being written. Data in this
 * stream will be written bit-by-bit. An internal counter
 * {@link BitOutputStream#bitsUsed} determines how many bits we
 * have currently written to the output stream, while another
 * counter {@link BitOutputStream#bitsAllocated} contains
 * the number of bits that are currently allocated in the buffer
 * and can be used to determine if the internal byte buffer needs
 * to grow in it's capacity.
 * <br>
 * The buffer itself is allocated either to hold a starting amount
 * of 32 bytes (256 bits) or more if initialized with an external
 * byte buffer that exceeds 32 bits.
 * <br>
 * This type of {@link OutputStream} is quite useful for things
 * such as networking as it allows data to be written in a much more
 * efficient manner.
 * <br>
 * For example, in a typical {@link OutputStream}, most implementations
 * like to write boolean data as single bytes, which ultimately wastes
 * a good amount of space - 7 bits typically - that could be used to
 * store other types of information, especially if we want to conserve
 * the amount of data being sent over a network.
 * <br>
 * In this {@link BitOutputStream}, boolean values are stored as single
 * bits - 1 or 0 - rather than a full byte, leading to smaller sizes and
 * greater efficiency.
 *
 * @author  Jonathan Hart
 * @since   1.0.0
 * @see     BitInputStream
 */
public class BitOutputStream extends OutputStream {

    // -- Private Constants

    /**
     * The default buffer size in bytes for creating a new
     * {@link BitOutputStream}. This is used when the data
     * is too small initially so that we don't have to call
     * the {@link BitOutputStream#growCapacity(int)} method
     * constantly during the first few write calls.
     *
     * @since   1.0.0
     */
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 32;



    // -- Protected Variables

    /**
     * The number of bits that have been currently
     * allocated in the internal buffer used by the
     * output stream.
     * <br>
     * To obtain the number of bytes allocated, simply
     * add 7 and divide by 8 as partial bits will form
     * a new byte.
     * <br>
     * The value of this variable will always be a
     * minimum of 256 (32 bytes) but will increase over
     * time as the buffer size is expanded to make way
     * for more information.
     * <br>
     * This value will also never be less than the value
     * of {@link BitOutputStream#bitsUsed}.
     *
     * @since   1.0.0
     */
    protected int bitsAllocated;

    /**
     * The number of bits that have been used by the
     * input stream. To obtain the number of bytes used by
     * the input stream, simply add 7 and divide by 8 as
     * partial bits will form a new byte.
     * <br>
     * This will always be a non-negative number as it
     * indicates that 0 or more bits are part of the stream.
     *
     * @since   1.0.0
     */
    protected int bitsUsed;

    /**
     * An array of bytes that will hold the data that we are
     * writing to the stream. This will increase in size over
     * time to allow for holding more and more data.
     * <br>
     * It will always start out being able to hold at least
     * 32 bytes (256 bits) or more if specified in the constructor.
     * <br>
     * Write operations will always start from <code>buffer[0]</code>
     * and will constantly move forward as more data is written
     * and the buffer expands.
     *
     * @since   1.0.0
     */
    protected byte[] buffer;




    // -- Constructor

    /**
     * Creates a bit output stream that will hold binary data
     * written to it and stored in a byte array.
     * <br>
     * This will first allocated 32 bytes (256 bits) to the internal
     * byte array buffer and will set the number of bits allocated
     * to 256, as well as reset the number of bits used to 0.
     *
     * @since   1.0.0
     */
    public BitOutputStream() {

        // Create the default buffer
        this.buffer = new byte[DEFAULT_BYTE_BUFFER_SIZE];

        // Set our sizes
        this.bitsAllocated = BitUtils.bytesToBits(DEFAULT_BYTE_BUFFER_SIZE);
        this.bitsUsed = 0;
    }

    /**
     * Creates a bit output stream from existing binary data by
     * simply copying the binary data from the passed byte array
     * and setting our write offset to be at the end of the data
     * as well as allocating enough space to hold the data.
     *
     * @since   1.0.0
     * @param   data The binary data byte array
     */
    public BitOutputStream(byte[] data) {
        this(data, data.length);
    }

    /**
     * Creates a bit output stream from existing binary data with a
     * specified length.
     *
     * The data
     *
     * @param data
     * @param length
     */
    public BitOutputStream(byte[] data, int length) {

        // Make sure we have byte data
        if (data == null || data.length == 0)
            throw new IllegalArgumentException("Cannot create a BitStream from a nonexistent or zero-sized byte array.");

        // Make sure that we want to copy data
        if (length <= 0 || length > data.length)
            throw new IllegalArgumentException("Can only copy data within the bounds of the byte array size and non-zero.");

        // Create our buffer
        int allocationSize = Math.max(DEFAULT_BYTE_BUFFER_SIZE, length);
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
    private synchronized void checkCapacity(int bitsToWrite) {

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
    private synchronized void growCapacity(int bitsNeeded) {

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




    // -- Bit Methods

    /**
     * Writes the specified number of bits to the bitstream (right aligned).
     * @param data The data to write
     * @param numBits The number of bits to write
     */
    public synchronized void writeBits(byte[] data, int numBits) {
        this.writeBits(data, numBits, true);
    }

    /**
     * Writes the specified number of bits to the bitstream.
     * @param data The data to write
     * @param numBits The number of bits of the data to write
     * @param rightAligned Whether or not to align bits to the right or not when less than we need for a full byte
     */
    public synchronized void writeBits(byte[] data, int numBits, boolean rightAligned) {

        if (numBits < 0 || (data.length * BitConstants.BITS_PER_BYTE) < numBits)
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
                dataByte <<= (byte) (BitConstants.BITS_PER_BYTE - numBits);
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
                this.buffer[this.bitsUsed >>> 3] |= (byte) ((dataByte & 0xFF) >>> bitsUsedMod8);

                // If we have remaining data, write it here
                int remaining = BitConstants.BITS_PER_BYTE - bitsUsedMod8;
                if (remaining < 8 && remaining < numBits) {
                    this.buffer[(this.bitsUsed >>> 3) + 1] = (byte) (dataByte << remaining);
                }
            }

            // Determine how much we wrote
            if (numBits >= BitConstants.BITS_PER_BYTE) {

                // Increase our bits used
                this.bitsUsed += BitConstants.BITS_PER_BYTE;

                // Subtract what we used
                numBits -= BitConstants.BITS_PER_BYTE;
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
    public synchronized void write(int b) {
        this.writeByte((byte) b);
    }

    /**
     * Writes a 0 bit to the output stream.
     */
    public synchronized void write0() {

        byte[] data = {0x00};
        this.writeBits(data, 1);
    }

    /**
     * Writes a 1 bit to the output stream.
     */
    public synchronized void write1() {

        byte[] data = {0x01};
        this.writeBits(data, 1);
    }

    /**
     * Writes a boolean to the output stream. To conserve space,
     * boolean values are written as a single bit instead of a single byte.
     * @param data The boolean value
     */
    public synchronized void writeBoolean(boolean data) {
        if (data)
            write1();
        else
            write0();
    }

    /**
     * Writes a full array of bytes to the output stream.
     * @param data The data to write
     */
    public synchronized void writeBytes(byte[] data) {
        if (data == null)
            throw new IllegalArgumentException("Cannot write null data!");

        this.writeBytes(data, data.length);
    }

    /**
     * Writes an array of bytes with a length to the output stream.
     *
     * This is just a convenience call to {@link BitOutputStream#writeBits(byte[], int, boolean)}
     * to keep users from converting byte length to bit length
     *
     * @param data The data to write
     * @param length The length of the data to write
     */
    public synchronized void writeBytes(byte[] data, int length) {
        if (data == null)
            throw new IllegalArgumentException("Cannot write null data!");
        if (length < 0 || length > data.length)
            throw new IllegalArgumentException("Write length must be between 0 and the data length!");

        this.writeBits(data, length * BitConstants.BITS_PER_BYTE, true);
    }

    /**
     * Writes a byte to the output stream.
     * @param data The byte to write
     */
    public synchronized void writeByte(byte data) {

        byte[] arr = {data};
        this.writeBits(arr, Byte.SIZE);
    }

    /**
     * Writes a character to the output stream.
     * @param data The character value
     */
    public synchronized void writeChar(char data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putChar(data).array(), Character.SIZE);
    }

    /**
     * Writes a character to the output stream in little-endian.
     * @param data The character value
     */
    public synchronized void writeCharLE(char data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar(data).array(), Character.SIZE);
    }

    /**
     * Writes a double to the output stream.
     * @param data The double value
     */
    public synchronized void writeDouble(double data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(data).array(), Double.SIZE);
    }

    /**
     * Writes a double to the output stream in little-endian.
     * @param data The double value
     */
    public synchronized void writeDoubleLE(double data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(data).array(), Double.SIZE);
    }

    /**
     * Writes a float to the output stream.
     * @param data The float value
     */
    public synchronized void writeFloat(float data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(data).array(), Float.SIZE);
    }

    /**
     * Writes a float to the output stream in little-endian.
     * @param data The float value
     */
    public synchronized void writeFloatLE(float data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(data).array(), Float.SIZE);
    }

    /**
     * Writes an integer to the output stream.
     * @param data The integer value
     */
    public synchronized void writeInteger(int data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(data).array(), Integer.SIZE);
    }

    /**
     * Writes an integer to the output stream in little-endian.
     * @param data The integer value
     */
    public synchronized void writeIntegerLE(int data) {
        this.writeBits(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data).array(), Integer.SIZE);
    }

    /**
     * Writes a long to the output stream.
     * @param data The long value
     */
    public synchronized void writeLong(long data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(data).array(), Long.SIZE);
    }

    /**
     * Writes a long to the output stream in little-endian.
     * @param data The long data
     */
    public synchronized void writeLongLE(long data) {
        this.writeBits(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(data).array(), Long.SIZE);
    }

    /**
     * Writes a short to the output stream.
     * @param data The short value
     */
    public synchronized void writeShort(short data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(data).array(), Short.SIZE);
    }

    /**
     * Writes a short to the output stream in little-endian.
     * @param data The short value
     */
    public synchronized void writeShortLE(short data) {
        this.writeBits(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data).array(), Short.SIZE);
    }



    // -- Data Write Methods (Objects)

    /**
     * Writes a BitOutputStream to this output stream.
     * @param bitOutputStream The old stream
     */
    public synchronized void writeBitStream(BitOutputStream bitOutputStream) {

        // First, make sure we have data
        if (bitOutputStream == null)
            throw new IllegalArgumentException("Cannot write a null output stream");

        // If we have no bits in this stream, just return.
        if (bitOutputStream.bitsUsed == 0) return;

        // Otherwise, write the data
        this.writeBits(bitOutputStream.buffer, bitOutputStream.bitsUsed);
    }



    // -- Getter Methods

    /**
     * Returns the length of the internal buffer.
     * @return The internal buffer length.
     */
    public synchronized int getBufferLength() {
        return this.buffer.length;
    }

    /**
     * Returns a copy of our buffer data.
     * @return The buffer data
     */
    public synchronized byte[] getData() {
        return this.buffer.clone();
    }

    /**
     * Returns a copy of our used buffer data.
     * @return The buffer data
     */
    public synchronized byte[] getDataUsed() { return Arrays.copyOfRange(this.buffer, 0, getNumBytesUsed()); }

    /**
     * Returns the byte data at a specific index.
     * @param index The index
     * @return The data at the index
     */
    public synchronized byte getDataAtIndex(int index) {

        int bytesUsed = BitUtils.bitsToBytes(this.bitsUsed);
        if (index > bytesUsed - 1)
            throw new IndexOutOfBoundsException("Data index is out of bounds");

        return this.buffer[index];
    }

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
