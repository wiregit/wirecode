package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.limewire.mojito.KUID;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;

/**
 * 
 */
public class LimeDHTUtils {

    private LimeDHTUtils() {}
    
    /**
     * Converts the given SHA-1 URN into a KUID
     */
    public static KUID toKUID(URN urn) {
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException("Expected a SHA-1 URN: " + urn);
        }
        return KUID.createWithBytes(urn.getBytes());
    }
    
    /**
     * Converts the given KUID into a SHA-1 URN
     */
    public static URN toURN(KUID kuid) {
        try {
            return URN.createSHA1UrnFromBytes(kuid.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns a KUID for the GUID (KUID = SHA-1(GUID))
     */
    public static KUID toKUID(GUID guid) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(guid.bytes());
            byte[] digest = md.digest();
            return KUID.createWithBytes(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns true if the FileDesc is considered rare
     * 
     * TODO: Define rare
     */
    public static boolean isRare(FileDesc fd) {
        long time = fd.getLastAttemptedUploadTime();
        return (System.currentTimeMillis() - time >= 0L);
    }
}
