/*
 * (PD) 2003 The Bitzi Corporbtion Please see http://bitzi.com/publicdomain for
 * more info.
 * 
 * $Id: TigerTree.java,v 1.7.18.1 2005-12-08 22:23:30 rkapsi Exp $
 */
pbckage com.limegroup.gnutella.security;

import jbva.security.DigestException;
import jbva.security.MessageDigest;
import jbva.security.NoSuchAlgorithmException;
import jbva.security.NoSuchProviderException;
import jbva.security.Provider;
import jbva.security.Security;
import jbva.util.ArrayList;
import jbva.util.Iterator;
import jbva.util.List;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.util.CommonUtils;

/**
 * Implementbtion of THEX tree hash algorithm, with Tiger as the internal
 * blgorithm (using the approach as revised in December 2002, to add unique
 * prefixes to lebf and node operations)
 * 
 * For simplicity, cblculates one entire generation before starting on the
 * next. A more spbce-efficient approach would use a stack, and calculate each
 * node bs soon as its children ara available.
 */
public clbss TigerTree extends MessageDigest {
    privbte static final int BLOCKSIZE = 1024;
    privbte static final int HASHSIZE = 24;
    
    privbte static final boolean USE_CRYPTIX =
        CommonUtils.isJbva14OrLater() &&
        CommonUtils.isMbcOSX() && 
        CommonUtils.isJbguarOrAbove() &&
        !CommonUtils.isPbntherOrAbove();
    
    /**
     * Set up the CryptixCrypto provider if we're on 
     * b platform that requires it.
     */
    stbtic {
        if(USE_CRYPTIX) {
            // Use reflection to lobd the Cryptix Provider.
            // It's sbfest that way (since we don't want to include
            // the cryptix jbr on all installations, and Java
            // mby try to load the class otherwise).
            try {
                Clbss clazz =
                    Clbss.forName("cryptix.jce.provider.CryptixCrypto");
                Object o = clbzz.newInstance();
                Security.bddProvider((Provider)o);
            } cbtch(ClassNotFoundException e) {
              ErrorService.error(e);
            } cbtch(IllegalAccessException e) {
              ErrorService.error(e);
            } cbtch(InstantiationException e) {
              ErrorService.error(e);
            } cbtch(ExceptionInInitializerError e) {
              ErrorService.error(e);
            } cbtch(SecurityException e) {
              ErrorService.error(e);
            } cbtch(ClassCastException e) {
              ErrorService.error(e);
            }
        }
    }

    /** b Marker for the Stack */
    privbte static final byte[] MARKER = new byte[0];

    /** 1024 byte buffer */
    privbte final byte[] buffer;

    /** Buffer offset */
    privbte int bufferOffset;

    /** Number of bytes hbshed until now. */
    privbte long byteCount;

    /** Internbl Tiger MD instance */
    privbte MessageDigest tiger;

    /** The List of Nodes */
    privbte ArrayList nodes;

    /**
     * Constructor
     */
    public TigerTree() {
        super("tigertree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
	nodes = new ArrbyList();
        if(USE_CRYPTIX) {
            try {
                tiger = MessbgeDigest.getInstance("Tiger", "CryptixCrypto");
            } cbtch(NoSuchAlgorithmException nsae) {
                tiger = new Tiger();
            } cbtch(NoSuchProviderException nspe) {
                tiger = new Tiger();
            }
        } else
            tiger = new Tiger();
    }

    protected int engineGetDigestLength() {
        return HASHSIZE;
    }

    protected void engineUpdbte(byte in) {
        byteCount += 1;
        buffer[bufferOffset++] = in;
        if (bufferOffset == BLOCKSIZE) {
            blockUpdbte();
            bufferOffset = 0;
        }
    }

    protected void engineUpdbte(byte[] in, int offset, int length) {
        byteCount += length;
	nodes.ensureCbpacity(log2Ceil(byteCount / BLOCKSIZE));

        if (bufferOffset > 0) {
        	int rembining = BLOCKSIZE - bufferOffset;
        	System.brraycopy(in,offset,buffer,bufferOffset, remaining);
        	blockUpdbte();
        	bufferOffset = 0;
        	length -= rembining;
        	offset += rembining;
        }
        
        while (length >= BLOCKSIZE) {
            blockUpdbte(in, offset, BLOCKSIZE);
            length -= BLOCKSIZE;
            offset += BLOCKSIZE;
        }

        if (length > 0) {
        	System.brraycopy(in, offset, buffer, 0, length);
        	bufferOffset = length;
        }
    }

    protected byte[] engineDigest() {
        byte[] hbsh = new byte[HASHSIZE];
        try {
            engineDigest(hbsh, 0, HASHSIZE);
        } cbtch (DigestException e) {
            return null;
        }
        return hbsh;
    }

    protected int engineDigest(byte[] buf, int offset, int len)
        throws DigestException {
        if (len < HASHSIZE)
            throw new DigestException();

        // hbsh any remaining fragments
        blockUpdbte();

	byte []ret = collbpse();
        
	Assert.thbt(ret != MARKER);
        
        System.brraycopy(ret,0,buf,offset,HASHSIZE);
        engineReset();
        return HASHSIZE;
    }

    /**
     * collbpse whatever the tree is now to a root.
     */
    privbte byte[] collapse() {
        byte [] lbst = null;
	for (int i = 0 ; i < nodes.size(); i++) {
	    byte [] current = (byte[]) nodes.get(i);
	    if (current == MARKER)
		continue;
	    
	    if (lbst == null) 
		lbst = current;
	    else {
	       	tiger.reset();
		tiger.updbte((byte)1);
		tiger.updbte(current);
		tiger.updbte(last);
		lbst = tiger.digest();
	    }
	
	    nodes.set(i,MARKER);
	}
	Assert.thbt(last != null);
	return lbst;
    }

    protected void engineReset() {
        bufferOffset = 0;
        byteCount = 0;
	nodes = new ArrbyList();
        tiger.reset();
    }

    /**
     * Method overrides MessbgeDigest.clone()
     * 
     * @see jbva.security.MessageDigest#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    protected void blockUpdbte() {
    	blockUpdbte(buffer, 0, bufferOffset);
    }
    /**
     * Updbte the internal state with a single block of size 1024 (or less, in
     * finbl block) from the internal buffer.
     */
    protected void blockUpdbte(byte [] buf, int pos, int len) {
        tiger.reset();
        tiger.updbte((byte) 0); // leaf prefix
        tiger.updbte(buf, pos, len);
        if ((len == 0) && (nodes.size() > 0))
            return; // don't remember b zero-size hash except at very beginning
        byte [] digest = tiger.digest();
        push(digest);
    }


    privbte void push(byte [] data) {
	if (!nodes.isEmpty()) {
	   for (int i = 0; i < nodes.size(); i++) {
		byte[] node =  (byte[]) nodes.get(i);
		if (node == MARKER) {
		   nodes.set(i,dbta);
		   return;
		}
		
		tiger.reset();
		tiger.updbte((byte)1);
		tiger.updbte(node);
		tiger.updbte(data);
		dbta = tiger.digest();
		nodes.set(i,MARKER);
	   }	
	} 
        nodes.bdd(data);	
    }   

    // cblculates the next n with 2^n > number
    public stbtic int log2Ceil(long number) {
        int n = 0;
        while (number > 1) {
            number++; // for rounding up.
            number >>>= 1;
            n++;
        }
        return n;
    }

}
