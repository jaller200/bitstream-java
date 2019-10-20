package com.darkflame.utils.bitstream;

import java.util.Arrays;

/**
 * An output stream that allows bit write operations.
 */
public class BitOutputStream {

    // -- Protected Constants

    /** The number of bits in a byte. */
    protected static final int BITS_PER_BYTE = 8;

    /** The default buffer size. */
    protected static final int DEFAULT_BYTE_BUFFER_SIZE = 32;



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



    // -- Data Methods


}
