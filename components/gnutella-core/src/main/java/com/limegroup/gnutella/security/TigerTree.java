/*
 * (PD) 2003 The Bitzi Corporation Please see http://bitzi.com/publicdomain for
 * more info.
 * 
 * $Id: TigerTree.java,v 1.2.6.1 2004-10-05 19:45:23 zlatinb Exp $
 */
package com.limegroup.gnutella.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Provider;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.ErrorService;

/**
 * Implementation of THEX tree hash algorithm, with Tiger as the internal
 * algorithm (using the approach as revised in December 2002, to add unique
 * prefixes to leaf and node operations)
 * 
 * For simplicity, calculates one entire generation before starting on the
 * next. A more space-efficient approach would use a stack, and calculate each
 * node as soon as its children ara available.
 */
public class TigerTree extends MessageDigest {
    private static final int BLOCKSIZE = 1024;
    private static final int HASHSIZE = 24;
    
    private static final boolean USE_CRYPTIX =
        CommonUtils.isJava14OrLater() &&
        CommonUtils.isMacOSX() && 
        CommonUtils.isJaguarOrAbove() &&
        !CommonUtils.isPantherOrAbove();
    
    /**
     * Set up the CryptixCrypto provider if we're on 
     * a platform that requires it.
     */
    static {
        if(USE_CRYPTIX) {
            // Use reflection to load the Cryptix Provider.
            // It's safest that way (since we don't want to include
            // the cryptix jar on all installations, and Java
            // may try to load the class otherwise).
            try {
                Class clazz =
                    Class.forName("cryptix.jce.provider.CryptixCrypto");
                Object o = clazz.newInstance();
                Security.addProvider((Provider)o);
            } catch(ClassNotFoundException e) {
              ErrorService.error(e);
            } catch(IllegalAccessException e) {
              ErrorService.error(e);
            } catch(InstantiationException e) {
              ErrorService.error(e);
            } catch(ExceptionInInitializerError e) {
              ErrorService.error(e);
            } catch(SecurityException e) {
              ErrorService.error(e);
            } catch(ClassCastException e) {
              ErrorService.error(e);
            }
        }
    }

    /** 1024 byte buffer */
    private final byte[] buffer;

    /** Buffer offset */
    private int bufferOffset;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private MessageDigest tiger;

    /** Interim tree node hash values */
    private List nodes;

    /**
     * Constructor
     */
    public TigerTree() {
        super("tigertree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        nodes = new ArrayList();
        if(USE_CRYPTIX) {
            try {
                tiger = MessageDigest.getInstance("Tiger", "CryptixCrypto");
            } catch(NoSuchAlgorithmException nsae) {
                tiger = new Tiger();
            } catch(NoSuchProviderException nspe) {
                tiger = new Tiger();
            }
        } else
            tiger = new Tiger();
    }

    protected int engineGetDigestLength() {
        return HASHSIZE;
    }

    protected void engineUpdate(byte in) {
        byteCount += 1;
        buffer[bufferOffset++] = in;
        if (bufferOffset == BLOCKSIZE) {
            blockUpdate();
            bufferOffset = 0;
        }
    }

    protected void engineUpdate(byte[] in, int offset, int length) {
        byteCount += length;

        int remaining;
        while (length >= (remaining = BLOCKSIZE - bufferOffset)) {
            System.arraycopy(in, offset, buffer, bufferOffset, remaining);
            bufferOffset += remaining;
            blockUpdate();
            length -= remaining;
            offset += remaining;
            bufferOffset = 0;
        }

        System.arraycopy(in, offset, buffer, bufferOffset, length);
        bufferOffset += length;
    }

    protected byte[] engineDigest() {
        byte[] hash = new byte[HASHSIZE];
        try {
            engineDigest(hash, 0, HASHSIZE);
        } catch (DigestException e) {
            return null;
        }
        return hash;
    }

    protected int engineDigest(byte[] buf, int offset, int len)
        throws DigestException {
        if (len < HASHSIZE)
            throw new DigestException();

        // hash any remaining fragments
        blockUpdate();

        // composite neighboring nodes together up to top value
        while (nodes.size() > 1) {
            List newNodes = new ArrayList();
            Iterator iter = nodes.iterator();
            while (iter.hasNext()) {
                byte[] left = (byte[]) iter.next();
                if (iter.hasNext()) {
                    byte[] right = (byte[]) iter.next();
                    tiger.reset();
                    tiger.update((byte) 1); // node prefix
                    tiger.update(left, 0, left.length);
                    tiger.update(right, 0, right.length);
                    newNodes.add(tiger.digest());
                } else {
                    newNodes.add(left);
                }
            }
            nodes = newNodes;
        }
        System.arraycopy(nodes.get(0), 0, buf, offset, HASHSIZE);
        engineReset();
        return HASHSIZE;
    }

    protected void engineReset() {
        bufferOffset = 0;
        byteCount = 0;
        nodes = new ArrayList();
        tiger.reset();
    }

    /**
     * Method overrides MessageDigest.clone()
     * 
     * @see java.security.MessageDigest#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Update the internal state with a single block of size 1024 (or less, in
     * final block) from the internal buffer.
     */
    protected void blockUpdate() {
        tiger.reset();
        tiger.update((byte) 0); // leaf prefix
        tiger.update(buffer, 0, bufferOffset);
        if ((bufferOffset == 0) & (nodes.size() > 0))
            return; // don't remember a zero-size hash except at very beginning
        nodes.add(tiger.digest());
    }

    /**
     * Public 
     * @param args
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
     /*
    public static void main(String[] args)
        throws IOException, NoSuchAlgorithmException {
        if (args.length < 1) {
            System.out.println("You must supply a filename.");
            return;
        }
        MessageDigest tt = new TigerTree();
        FileInputStream fis;

        for (int i = 0; i < args.length; i++) {
            fis = new FileInputStream(args[i]);
            int read;
            byte[] in = new byte[1024];
            while ((read = fis.read(in)) > -1) {
                tt.update(in, 0, read);
            }
            fis.close();
            byte[] digest = tt.digest();
            String hash = new BigInteger(1, digest).toString(16);
            while (hash.length() < 48) {
                hash = "0" + hash;
            }
            System.out.println("hex:" + hash);
            System.out.println("b32:" + Base32.encode(digest));
            tt.reset();
        }
    }*/
}
