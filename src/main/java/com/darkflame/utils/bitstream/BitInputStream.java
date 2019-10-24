package com.darkflame.utils.bitstream;

/**
 * An input stream that allows bit read operations.
 */
public class BitInputStream {

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



    // -- Data Methods

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


    // -- Bit Methods

    /**
     * Reads the specified number of bits from the input stream.
     * @param numBits The number of bits to read.
     * @param rightAligned Whether or not to align bits to the right when less than we need for a full byte.
     * @return The bits read
     */
    public synchronized byte[] readBits(int numBits, boolean rightAligned) {

        // Make sure we are reading bits
        if (numBits <= 0)
            throw new IllegalArgumentException("We can only read positive numbers of bits!");

        // Make sure we are within the bounds.
        if (this.readOffset + numBits > bitsUsed)
            throw new IllegalArgumentException("Cannot read more bits than exist in the stream relative to the read offset");

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
}
