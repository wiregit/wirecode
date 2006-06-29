package com.limegroup.gnutella.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.limegroup.gnutella.ByteOrder;

/** For implementation details, please see:
 *  http://www.acm.org/sigcomm/sigcomm97/papers/p062.pdf 
 */
public class COBSUtil {
    
    /** Encode a byte array with COBS.  The non-allowable byte value is 0.
     *  PRE: src is not null.
     *  POST: the return array will be a cobs encoded version of src.  namely,
     *  cobsDecode(cobsEncode(src)) ==  src.
     *  @return a COBS encoded version of src.
     */
    public static byte[] cobsEncode(byte[] src) {
        final int srcLen = src.length;
        int code = 1;
        int currIndex = 0;
        // COBS encoding adds no more than one byte of overhead for every 254
        // bytes of packet data
        final int maxEncodingLen = src.length + ((src.length+1)/254) + 1;
        ByteArrayOutputStream sink = new ByteArrayOutputStream(maxEncodingLen);
        int writeStartIndex = -1;

        while (currIndex < srcLen) {
            if (src[currIndex] == 0) {
                // currIndex was incremented so take 1 less
                code = finishBlock(code, sink, src, writeStartIndex,
                                   (currIndex-1));
                writeStartIndex = -1;
            }
            else {
                if (writeStartIndex < 0) writeStartIndex = currIndex;
                code++;
                if (code == 0xFF) {
                    code = finishBlock(code, sink, src, writeStartIndex,
                                       currIndex);
                    writeStartIndex = -1;
                }
            }
            currIndex++;
        }

        // currIndex was incremented so take 1 less
        finishBlock(code, sink, src, writeStartIndex, (currIndex-1));
        return sink.toByteArray();
    }

    private static int finishBlock(int code, ByteArrayOutputStream sink, 
                                   byte[] src, int begin, int end) {
        sink.write(code);
        if (begin > -1)
            sink.write(src, begin, (end-begin)+1);
        return (byte) 0x01;
    }

    /** Decode a COBS-encoded byte array.  The non-allowable byte value is 0.
     *  PRE: src is not null.
     *  POST: the return array will be a cobs decoded version of src.  namely,
     *  cobsDecode(cobsEncode(src)) ==  src.  
     *  @return the original COBS decoded string
     */
    public static byte[] cobsDecode(byte[] src) throws IOException {
        final int srcLen = src.length;
        int currIndex = 0;
        int code = 0;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();        

        while (currIndex < srcLen) {
            code = ByteOrder.ubyte2int(src[currIndex++]);
            if ((currIndex+(code-2)) >= srcLen)
                throw new IOException();
            for (int i = 1; i < code; i++) {
                sink.write(src[currIndex++]);
            }
            if (currIndex < srcLen) // don't write this last one, it isn't used
                if (code < 0xFF) sink.write(0);
        }

        return sink.toByteArray();
    }

}
