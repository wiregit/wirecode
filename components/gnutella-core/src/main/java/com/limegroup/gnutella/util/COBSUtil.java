package com.limegroup.gnutella.util;

import java.io.*;
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
    public static byte[] cobsEncode(byte[] src) throws IOException {
        final int srcLen = src.length;
        int code = 1;
        int currIndex = 0;
        // COBS encoding adds no more than one byte of overhead for every 254
        // bytes of packet data
        final int maxEncodingLen = src.length + ((src.length+1)/254) + 1;
        ByteArrayOutputStream sink = new ByteArrayOutputStream(maxEncodingLen);
        // 254 is the max size of a temporary block
        ByteArrayOutputStream temp = new ByteArrayOutputStream(254);

        while (currIndex < srcLen) {
            if (src[currIndex] == 0)
                code = finishBlock(code, sink, temp);
            else {
                temp.write((int)src[currIndex]);
                code++;
                if (code == 0xFF) 
                    code = finishBlock(code, sink, temp);
            }
            currIndex++;
        }

        finishBlock(code, sink, temp);
        return sink.toByteArray();
    }

    private static int finishBlock(int code, ByteArrayOutputStream sink, 
                            ByteArrayOutputStream temp) throws IOException {
        sink.write(code);
        sink.write(temp.toByteArray());
        temp.reset();
        return (byte) 0x01;
    }

    /** Decode a COBS-encoded byte array.  The non-allowable byte value is 0.
     *  PRE: src is not null.
     *  POST: the return array will be a cobs decoded version of src.  namely,
     *  cobsDecode(cobsEncode(src)) ==  src.  
     *  @return the original COBS decoded string with a extra trailing 0 at the
     *  end - feel free to discard it.
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
                sink.write((int)src[currIndex++]);
            }
            if (code < 0xFF) sink.write(0);
        }

        return sink.toByteArray();
    }

}
