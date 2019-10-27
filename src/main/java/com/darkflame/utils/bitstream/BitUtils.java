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

/**
 * A simple collection of utility functions that are used internally
 * by the bitstream classes, but are also available to third parties.
 *
 * @author  Jonathan Hart
 * @since   1.0.0
 */
public class BitUtils {

    // -- Bit/Byte Conversions

    /**
     * Converts the specified number of bits into how many
     * bytes it would take to hold those bits.
     * <br>
     * If <code>numBits</code> is a multiple of 8, then the
     * number of bytes is simply <code>numBits / 8</code>.
     * <br>
     * If <code>numBits</code> is not a multiple of 8, this means
     * that there are some bits at the end that will not take up
     * a full byte, so this method will return the number of
     * full bytes used plus one additional extra to cover the
     * remaining bits.
     *
     * @since   1.0.0
     * @param   numBits The number of bits to convert
     * @return  The number of bytes that can hold the bits
     */
    public static int bitsToBytes(int numBits) {
        return (numBits + 7) >> 3;
    }

    /**
     * Converts the specified number of bytes into the
     * corresponding number of bits.
     * <br>
     * The calculations here are much simpler than the other
     * way around - all that needs to be done is to multiply
     * the number of bytes by the number of bits in a byte (8 bits).
     *
     * @since   1.0.0
     * @param   numBytes The number of bytes to convert
     * @return  The number of bits in those bytes
     */
    public static int bytesToBits(int numBytes) {
        return numBytes << 3;
    }
}
