package com.darkflame.utils.bitstream;

/**
 * An output stream that allows bit write operations.
 */
public class BitOutputStream {

    // -- Protected Constants

    /** The number of bits in a byte. */
    protected static final int BITS_PER_BYTE = 8;

    /** The default buffer size. */
    protected static final int DEFAULT_BUFFER_SIZE = 32;



    // -- Protected Variables

    /** The number of bits that have been used. */
    protected int bitsUsed;

    /** The data buffer. Will expand as necessary */
    protected byte buffer[];



    // -- Constructor

    /**
     * Creates a bitstream and sets the default buffer widths
     */
    public BitOutputStream() {

        // Create the default buffer
        this.buffer = new byte[DEFAULT_BUFFER_SIZE];

        // Set our sizes
        this.bitsUsed = DEFAULT_BUFFER_SIZE * BITS_PER_BYTE;
    }
}
