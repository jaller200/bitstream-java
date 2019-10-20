package com.darkflame.utils.bitstream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the BitOutputStream class.
 */
public class BitOutputStreamTests {

    /**
     * Test that we can actually create the BitOutputStream and that the data
     * is correct (i.e., data used is 0)
     */
    @Test
    public void testConstruction() {

        // Create the stream
        BitOutputStream bitOutputStream = new BitOutputStream();
        Assert.assertNotNull(bitOutputStream);

        // Check the size (NOTE: this is not the buffer size, this is how much data we have used)
        Assert.assertEquals(0, bitOutputStream.getNumBitsUsed());
        Assert.assertEquals(0, bitOutputStream.getNumBytesUsed());

        // Check the size of the buffer
        Assert.assertEquals(32, bitOutputStream.getData().length);
    }

    /**
     * Tests that if we write a zero byte to the BitOutputStream that we will have used exactly
     * 1 byte (8 bits).
     */
    @Test
    public void testWriteBitsWriteZeroByte() {

        // Create the stream
        BitOutputStream bitOutputStream = new BitOutputStream();

        // Write our data
        byte[] data = {0x0};
        bitOutputStream.writeBits(data, 8, false);

        // Check our data
        Assert.assertEquals(8, bitOutputStream.getNumBitsUsed());
        Assert.assertEquals(1, bitOutputStream.getNumBytesUsed());

        // Check that our data is valid
        Assert.assertEquals(32, bitOutputStream.getData().length);
        Assert.assertEquals(0x00, bitOutputStream.getData()[0]);
    }

    /**
     * Tests that if we write a one byte to the BitOutputStream that we will have used exactly
     * 1 byte (8 bits).
     */
    @Test
    public void testWriteBitsWriteOneByte() {

        // Create the stream
        BitOutputStream bitOutputStream = new BitOutputStream();

        // Write our data
        byte[] data = {0x1};
        bitOutputStream.writeBits(data, 8, false);

        // Check our data
        Assert.assertEquals(8, bitOutputStream.getNumBitsUsed());
        Assert.assertEquals(1, bitOutputStream.getNumBytesUsed());

        // Check that our data is valid
        Assert.assertEquals(32, bitOutputStream.getData().length);
        Assert.assertEquals(0x01, bitOutputStream.getData()[0]);
    }

    /**
     * Tests that if we write a single bit (1) to the BitOutputStream that we will have used exactly
     * 1 byte (1 bit).
     */
    @Test
    public void testWriteBitsWriteOneBitLeftAligned() {

        // Create the stream
        BitOutputStream bitOutputStream = new BitOutputStream();

        // Write our data
        byte[] data = {0x01};
        bitOutputStream.writeBits(data, 1, false);

        // Check our data
        Assert.assertEquals(1, bitOutputStream.getNumBitsUsed());
        Assert.assertEquals(1, bitOutputStream.getNumBytesUsed());

        // Check that our data is valid
        Assert.assertEquals(32, bitOutputStream.getData().length);
        Assert.assertEquals(0x01, bitOutputStream.getData()[0]);
    }

    /**
     * Tests that if we write a single bit (1) to the BitOutputStream that we will have used exactly
     * 1 byte (1 bit).
     */
    @Test
    public void testWriteBitsWriteOneBitRightAligned() {

        // Create the stream
        BitOutputStream bitOutputStream = new BitOutputStream();

        // Write our data
        byte[] data = {0x01};
        bitOutputStream.writeBits(data, 1, true);

        // Check our data
        Assert.assertEquals(1, bitOutputStream.getNumBitsUsed());
        Assert.assertEquals(1, bitOutputStream.getNumBytesUsed());

        // Check that our data is valid
        Assert.assertEquals(32, bitOutputStream.getData().length);
        Assert.assertEquals((byte) 0x80, bitOutputStream.getData()[0]);
    }

    /**
     * Tests that if we write nine bits to the BitOutputStream that we will have used exactly
     * 2 bytes (9 bits).
     */
    /*@Test
    public void testWriteBitsWriteNineBits() {

        // Create the stream
        BitOutputStream bitOutputStream = new BitOutputStream();

        // Write our data
        byte[] data = {0x01, 0x03};
        bitOutputStream.writeBits(data, 9, false);

        // Check our data
        Assert.assertEquals(1, bitOutputStream.getNumBitsUsed());
        Assert.assertEquals(1, bitOutputStream.getNumBytesUsed());
    }*/
}
