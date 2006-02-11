
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.limegroup.gnutella.ByteOrder;

/**
 * COBS encoding is a way to hide all the 0 bytes in some data.
 * If you have some data that might contain 0s, and want to put it somewhere where 0s are not allowed, COBS encode it.
 * LimeWire uses COBS encoding for the value of a tag in a GGEP block in a Gnutella packet.
 * 
 * COBS stands for Consistant Overhead Byte Stuffing.
 * It adds no more than one byte of overhead for every 254 bytes of data.
 * http://www.acm.org/sigcomm/sigcomm97/papers/p062.pdf
 */
public class COBSUtil {

    /**
     * Encode a byte array with COBS.
     * The array might contain bytes that are 0s, which are not allowed.
     * This will hide them.
     * 
     * COBS encoding takes a group of bytes that doesn't contain any 0s, and writes the length followed by the data.
     * {'h' 'e' 'l' 'l' 'o'} becomes {6 'h' 'e' 'l' 'l' 'o'}
     * The COBS encoded data on the right contains a single block that starts with its length, 6.
     * {'a' 'a'} becomes {2 'a' 'a'}
     * 
     * We choose groups separated by 0s.
     * {'a' 'a' 0 'a' 'a'} becomes {3 'a' 'a' 3 'a' 'a'}
     * 
     * If there are multiple 0s between groups, an extra 0 can be represented by {1}, a block with length 1 and no data
     * {'a' 'a' 0 0 'a' 'a'} becomes {3 'a' 'a' 1 3 'a' 'a'}
     * 
     * 0s on the ends become {1} blocks like this.
     * {0 'a'} becomes {1 2 'a'}
     * {'a' 0} becomes {2 'a' 1}
     * {0} becomes {1 1}
     * {0 0} becomes {1 1 1}
     * 
     * @param src A byte array of data with 0s to COBS encode
     * @return    The COBS encoded data with all the 0s hidden
     */
    public static byte[] cobsEncode(byte[] src) throws IOException {

        // Variables for the loop
        final int srcLen = src.length; // Get the length of the data we'll encode
        int code = 1;                  // The length byte, which can be 0x01 through 0xFF
        int currIndex = 0;             // An index that moves over the data we've encoded

        // Make a ByteArrayOutputStream that will grow to hold the data we write to it
        final int maxEncodingLen = src.length + ((src.length + 1) / 254) + 1;   // Calculate the maximum possible size of the encoded data
        ByteArrayOutputStream sink = new ByteArrayOutputStream(maxEncodingLen); // Specify that size for the buffer

        // Set writeStartIndex to -1 to indicate we don't have block data right now
        int writeStartIndex = -1;

        // Loop until we've encoded all the given data
        while (currIndex < srcLen) {

            // We're on a 0 byte
            if (src[currIndex] == 0) {

                // Write the block, which starts with the code and has the data from writeStartIndex to currIndex
                code = finishBlock(code, sink, src, writeStartIndex, (currIndex - 1));
                writeStartIndex = -1; // No data yet for the next block

            // We're not on a 0 byte
            } else {

                // If we don't have any block data, start it here
                if (writeStartIndex < 0) writeStartIndex = currIndex;

                // Record this block will be one byte longer
                code++;

                // If this block has grown to 255 bytes, we have to write it
                if (code == 0xFF) {

                    // Write the block, which starts with the code byte 0xFF, and has the data from writeStartIndex to currIndex, including it
                    code = finishBlock(code, sink, src, writeStartIndex, currIndex);
                    writeStartIndex = -1; // No data yet for the next block
                }
            }

            // Move to the next byte in the source data
            currIndex++;
        }

        // Write the last block, which starts with the code byte and has the data from writeStartIndex to currIndex
        finishBlock(code, sink, src, writeStartIndex, (currIndex - 1));

        // Return the byte array inside the ByteArrayOutputStream that has the data we've written to it
        return sink.toByteArray();
    }

    /**
     * Write a single block of COBS encoded data.
     * A COBS block starts with a byte that tells how long the whole block is.
     * After that are bytes of data, none of which are 0.
     * 
     * @param code  A length code byte that will begin the next block we'll write
     * @param sink  The ByteArrayOutputStream where we're writing the encoded data
     * @param src   The source data we're COBS encoding
     * @param begin The index in src of the first byte of data to take, or -1 if there is no data
     * @param end   The index in src of the last byte of data to take
     * @return      The byte 0x01
     */
    private static int finishBlock(int code, ByteArrayOutputStream sink, byte[] src, int begin, int end) throws IOException {

        // Write the length byte that begins the block
        sink.write(code);

        // If this block contains some data
        if (begin > -1) {

            // Copy data from src into sink
            sink.write(             // The destination is sink
                src,                // The source is src
                begin,              // Start the distance begin into src
                (end - begin) + 1); // Copy to the byte at end, including it
        }

        // Always return the byte 0x01
        return (byte)0x01;
    }

    /**
     * Remove the COBS encoding from a byte array.
     * The array was COBS encoding to hide bytes that are 0s.
     * This will restore them.
     * 
     * COBS encoded data consists of blocks that look like this:
     * 
     * 0x01
     * 0x02 'h'
     * 0x03 'h' 'e'
     * 0x06 'h' 'e' 'l' 'l' 'o'
     * 
     * The first byte tells the size of the whole block.
     * The 3 block above would decode into this:
     * 
     * 0h0he0hello
     * 
     * We put 0s between the blocks, but not at the end.
     * The empty block at the start became a 0.
     * The only special length byte is 0xff.
     * It describes a block 255 bytes long, after which there isn't a 0.
     * 
     * @param src A byte array of COBS encoded data with all the 0 bytes hidden
     * @return    The data with the COBS encoding removed and the 0s restored
     */
    public static byte[] cobsDecode(byte[] src) throws IOException {

        // srcLen is the length of the source data, currIndex is our distance into it, and code is a byte we've read
        final int srcLen = src.length; // The length of the source data
        int currIndex = 0;             // An index to where we are in it
        int code = 0;                  // The length byte at the start of a block

        // Make a new ByteArrayOutputStream that will grow to hold the data we write to it
        ByteArrayOutputStream sink = new ByteArrayOutputStream();

        // Loop until we've moved through all the given data
        while (currIndex < srcLen) {

            // Read the length byte, which tells the size of the next block
            code = ByteOrder.ubyte2int(src[currIndex++]); // Read it as an unsigned byte

            // Make sure there's enough data left for this block
            if ((currIndex + (code - 2)) >= srcLen) throw new IOException();

            // Copy the rest of the block across
            for (int i = 1; i < code; i++) sink.write((int)src[currIndex++]);

            // If that wasn't the last block, and the length isn't the special 0xff that means don't write a 0, write a 0
            if (currIndex < srcLen && code < 0xFF) sink.write(0);
        }

        // Return the byte array inside the ByteArrayOutputStream that has the data we wrote to it
        return sink.toByteArray();
    }
}
