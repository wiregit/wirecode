/*
 * (PD) 2003 The Bitzi Corporation Please see http://bitzi.com/publicdomain for
 * more info.
 * 
 * $Id: TigerTree.java,v 1.6 2005-07-08 21:14:04 zlatinb Exp $
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
import java.util.Stack;

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

    /** 1024 byte buffer */
    private final byte[] buffer;

    /** Buffer offset */
    private int bufferOffset;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private MessageDigest tiger;

    private Stack stack;

    /**
     * Constructor
     */
    public TigerTree() {
        super("tigertree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        stack = new Stack();
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

        byte [] ret;
        if (stack.size() > 1) 
        	collapse();
        
        Assert.that(stack.size() == 1);
        Node top = (Node)stack.pop();
        ret = top.data;
        
        System.arraycopy(ret,0,buf,offset,HASHSIZE);
        engineReset();
        return HASHSIZE;
    }

    /**
     * collapse whatever the tree is now to a root.
     */
    private void collapse() {
    	while(stack.size() > 1) {
    		Node right = (Node)stack.pop();
    		Node left = (Node)stack.pop();
    		tiger.reset();
    		tiger.update((byte)1);
    		tiger.update(left.data);
    		tiger.update(right.data);
    		stack.push(new Node(tiger.digest(),-1));
    	}
    	
    	Assert.that(stack.size() == 1);
    }

    protected void engineReset() {
        bufferOffset = 0;
        byteCount = 0;
        stack = new Stack();
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
        if ((len == 0) && (stack.size() > 0))
            return; // don't remember a zero-size hash except at very beginning
        byte [] digest = tiger.digest();
        push(digest,0);
    }

    private void push(byte [] data, int level) {
    	if (!stack.isEmpty()) {
    		Node n = (Node) stack.peek();
    		if (n.level == level) {
    			stack.pop();
    			tiger.reset();
    			tiger.update((byte) 1); // node prefix
    			tiger.update(n.data);
    			tiger.update(data);
    			push(tiger.digest(),++level);
    			return;
    		}
    		Assert.that(n.level > level);
    	} 
    	stack.push(new Node(data,level));
    }
    
    private class Node {
    	public final byte [] data;
    	public final int level;
    	Node(byte [] data, int level) {
    		this.data = data;
    		this.level = level;
    	}
    	public String toString() {
    		return "level "+level;
    	}
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
