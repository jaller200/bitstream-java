package com.darkflame.utils.bitstream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the BitInputStream class.
 */
public class BitInputStreamTests {

    // -- Construction Tests

    /**
     * Test that we can actually create the BitInputStream with data.
     */
    @Test
    public void testDefaultConstructionValidData() {

        // Create the input stream
        byte[] data = {0x00, 0x01, 0x02};
        BitInputStream bitInputStream = new BitInputStream(data);
        Assert.assertNotNull(bitInputStream);

        // Check the size of the data and the current read offset
        Assert.assertEquals(24, bitInputStream.getNumBitsUsed());
        Assert.assertEquals(3, bitInputStream.getNumBytesUsed());
        Assert.assertEquals(0, bitInputStream.getReadOffset());
    }

    /**
     * Test that we can still create the input stream from data with zero length.
     */
    @Test
    public void testDefaultConstructionValidZeroData() {

        // Create the input stream
        byte[] data = {};
        BitInputStream bitInputStream = new BitInputStream(data);
        Assert.assertNotNull(bitInputStream);

        // Check the size of the data and the current read offset
        Assert.assertEquals(0, bitInputStream.getNumBitsUsed());
        Assert.assertEquals(0, bitInputStream.getNumBytesUsed());
        Assert.assertEquals(0, bitInputStream.getReadOffset());
    }

    /**
     * Test that an exception is thrown when trying to create the input stream from null data.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDefaultConstructionNullData() {

        // Create the input stream
        BitInputStream bitInputStream = new BitInputStream(null);
    }



    // -- BitInputStream.readBits(...) Tests

    /**
     * Tests that we can read a 0 bit from the input stream.
     */
    @Test
    public void testReadBitsSingleZeroBit() {

        // Create the stream
        byte[] data = {0x00};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(1, true);

        // Verify that our data is valid
        Assert.assertEquals(1, read.length);
        Assert.assertEquals((byte) 0x00, read[0]);
    }

    /**
     * Tests that we can read a 1 bit from the input stream.
     */
    @Test
    public void testReadBitsSingleOneBit() {

        // Create the stream
        byte[] data = {(byte) 0x80};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(1, true);

        // Verify that our data is valid
        Assert.assertEquals(1, read.length);
        Assert.assertEquals((byte) 0x01, read[0]);
    }

    /**
     * Tests that we can read a 1 byte from the input stream, but left-align it
     */
    @Test
    public void testReadBitsSingleOneBitLeftAligned() {

        // Create the stream
        byte[] data = {(byte) 0x80};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(1, false);

        // Verify that our data is valid
        Assert.assertEquals(1, read.length);
        Assert.assertEquals((byte) 0x80, read[0]);
    }

    /**
     * Tests that we can read 8 bits from the input stream.
     */
    @Test
    public void testReadBitsEightBits() {

        // Create the stream
        byte[] data = {0x01};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(8, true);

        // Verify that our data is valid
        Assert.assertEquals(1, read.length);
        Assert.assertEquals((byte) 0x01, read[0]);
    }

    /**
     * Tests that we can read 8 bits from the input stream, left-aligning them
     * NOTE: Pro tip - values should equal right alignment
     */
    @Test
    public void testReadBitsEightBitsLeftAligned() {

        // Create the stream
        byte[] data = {0x01};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(8, false);

        // Verify that our data is valid
        Assert.assertEquals(1, read.length);
        Assert.assertEquals((byte) 0x01, read[0]);
    }

    /**
     * Tests that we can read 9 bits from the input stream.
     */
    @Test
    public void testReadBitsNineBits() {

        // Create the stream
        byte[] data = {(byte) 0x01, (byte) 0x80};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(9, true);

        // Verify that our data is valid
        Assert.assertEquals(2, read.length);
        Assert.assertEquals((byte) 0x01, read[0]);
        Assert.assertEquals((byte) 0x01, read[1]);
    }

    /**
     * Tests that we can read 9 bits from the input stream, left-aligned.
     */
    @Test
    public void testReadBitsNineBitsLeftAligned() {

        // Create the stream
        byte[] data = {(byte) 0x01, (byte) 0x80};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(9, false);

        // Verify that our data is valid
        Assert.assertEquals(2, read.length);
        Assert.assertEquals((byte) 0x01, read[0]);
        Assert.assertEquals((byte) 0x80, read[1]);
    }

    /**
     * Tests that reading more bits than we have throws an error.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testReadBitsTooManyBits() {

        // Create the stream
        byte[] data = {0x01};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(9, true);
    }

    /**
     * Tests that reading 0 bits throws an error.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testReadBitsZeroBits() {

        // Create the stream
        byte[] data = {0x01};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(0, true);
    }

    /**
     * Tests that reading negative bits throws an error.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testReadBitsNegativeBits() {

        // Create the stream
        byte[] data = {0x01};
        BitInputStream bitInputStream = new BitInputStream(data);

        // Read the data
        byte[] read = bitInputStream.readBits(-1, true);
    }
}
