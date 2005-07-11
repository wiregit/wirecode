/*
 * (PD) 2003 The Bitzi Corporation Please see http://bitzi.com/publicdomain for
 * more info.
 * 
 * $Id: TigerTree.java,v 1.7 2005-07-11 00:05:13 zlatinb Exp $
 */
package com.limegroup.gnutella.security;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.CommonUtils;

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

    /** a Marker for the Stack */
    private static final byte[] MARKER = new byte[0];

    /** 1024 byte buffer */
    private final byte[] buffer;

    /** Buffer offset */
    private int bufferOffset;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private MessageDigest tiger;

    /** The List of Nodes */
    private ArrayList nodes;

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
	nodes.ensureCapacity(log2Ceil(byteCount / BLOCKSIZE));

        if (bufferOffset > 0) {
        	int remaining = BLOCKSIZE - bufferOffset;
        	System.arraycopy(in,offset,buffer,bufferOffset, remaining);
        	blockUpdate();
        	bufferOffset = 0;
        	length -= remaining;
        	offset += remaining;
        }
        
        while (length >= BLOCKSIZE) {
            blockUpdate(in, offset, BLOCKSIZE);
            length -= BLOCKSIZE;
            offset += BLOCKSIZE;
        }

        if (length > 0) {
        	System.arraycopy(in, offset, buffer, 0, length);
        	bufferOffset = length;
        }
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

	byte []ret = collapse();
        
	Assert.that(ret != MARKER);
        
        System.arraycopy(ret,0,buf,offset,HASHSIZE);
        engineReset();
        return HASHSIZE;
    }

    /**
     * collapse whatever the tree is now to a root.
     */
    private byte[] collapse() {
        byte [] last = null;
	for (int i = 0 ; i < nodes.size(); i++) {
	    byte [] current = (byte[]) nodes.get(i);
	    if (current == MARKER)
		continue;
	    
	    if (last == null) 
		last = current;
	    else {
	       	tiger.reset();
		tiger.update((byte)1);
		tiger.update(current);
		tiger.update(last);
		last = tiger.digest();
	    }
	
	    nodes.set(i,MARKER);
	}
	Assert.that(last != null);
	return last;
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

    protected void blockUpdate() {
    	blockUpdate(buffer, 0, bufferOffset);
    }
    /**
     * Update the internal state with a single block of size 1024 (or less, in
     * final block) from the internal buffer.
     */
    protected void blockUpdate(byte [] buf, int pos, int len) {
        tiger.reset();
        tiger.update((byte) 0); // leaf prefix
        tiger.update(buf, pos, len);
        if ((len == 0) && (nodes.size() > 0))
            return; // don't remember a zero-size hash except at very beginning
        byte [] digest = tiger.digest();
        push(digest);
    }


    private void push(byte [] data) {
	if (!nodes.isEmpty()) {
	   for (int i = 0; i < nodes.size(); i++) {
		byte[] node =  (byte[]) nodes.get(i);
		if (node == MARKER) {
		   nodes.set(i,data);
		   return;
		}
		
		tiger.reset();
		tiger.update((byte)1);
		tiger.update(node);
		tiger.update(data);
		data = tiger.digest();
		nodes.set(i,MARKER);
	   }	
	} 
        nodes.add(data);	
    }   

    // calculates the next n with 2^n > number
    public static int log2Ceil(long number) {
        int n = 0;
        while (number > 1) {
            number++; // for rounding up.
            number >>>= 1;
            n++;
        }
        return n;
    }

}
