package com.darkflame.utils.bitstream;

/**
 * A collection of utilities to operate on bits.
 */
public class BitUtils {

    // -- Bit/Byte Conversions

    /**
     * Determines how many bytes are needed to hold the specified bits.
     * @param numBits The number of bits that we need
     * @return The number of bytes that we need
     */
    public static int bitsToBytes(int numBits) {
        return (numBits + 7) >> 3;
    }

    /**
     * Determines how many bits are in the specified bytes.
     * @param numBytes The number of bytes that we need
     * @return The number of bits that we need
     */
    public static int bytesToBits(int numBytes) {
        return numBytes << 3;
    }
}
