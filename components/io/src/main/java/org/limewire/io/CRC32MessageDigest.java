package org.limewire.io;

import java.security.MessageDigest;
import java.util.zip.CRC32;

/**
 * Technical Note: A CRC is a type of hash function but not 
 * a message digest. As Java provides only MD5 and SHA-1 with
 * 128bit and 160bit respectively we do as if CRC is a message
 * digest to keep the overhead low.
 */
public class CRC32MessageDigest extends MessageDigest {
    
    private final CRC32 crc = new CRC32();
    
    public CRC32MessageDigest() {
        super("CRC32");
    }

    @Override
    protected int engineGetDigestLength() {
        return 4;
    }
    
    @Override
    protected byte[] engineDigest() {
        long value = crc.getValue();
        byte[] digest = {
            (byte)((value >> 24) & 0xFF),
            (byte)((value >> 16) & 0xFF),
            (byte)((value >>  8) & 0xFF),
            (byte)((value      ) & 0xFF)
        };
        return digest;
    }

    @Override
    protected void engineReset() {
        crc.reset();
    }

    @Override
    protected void engineUpdate(byte input) {
        crc.update(input & 0xFF);
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        crc.update(input, offset, len);
    }
}