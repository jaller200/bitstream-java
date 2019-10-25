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
