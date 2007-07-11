/*
 * (PD) 2003 The Bitzi Corporation Please see http://bitzi.com/publicdomain for
 * more info.
 * 
 * $Id: TigerTree.java,v 1.11 2007-07-11 20:53:51 sberlin Exp $
 */
package com.limegroup.gnutella.security;

import java.security.DigestException;
import java.security.MessageDigest;
import java.util.ArrayList;

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
    public static final int HASHSIZE = 24;

    /** a Marker for the Stack */
    private static final byte[] MARKER = new byte[0];

    /** 1024 byte buffer */
    private final byte[] buffer;

    /** Buffer offset */
    private int bufferOffset;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private final MessageDigest tiger;

    /** The List of Nodes */
    private final ArrayList<byte[]> nodes;

    /**
     * Constructor
     */
    public TigerTree() {
        super("tigertree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        nodes = new ArrayList<byte[]>();
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
            System.arraycopy(in, offset, buffer, bufferOffset, remaining);
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

    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        if (len < HASHSIZE)
            throw new DigestException();

        // hash any remaining fragments
        blockUpdate();

        byte[] ret = collapse();

        assert(ret != MARKER);

        System.arraycopy(ret, 0, buf, offset, HASHSIZE);
        engineReset();
        return HASHSIZE;
    }

    /**
     * collapse whatever the tree is now to a root.
     */
    private byte[] collapse() {
        byte[] last = null;
        for (int i = 0; i < nodes.size(); i++) {
            byte[] current = nodes.get(i);
            if (current == MARKER)
                continue;

            if (last == null)
                last = current;
            else {
                tiger.reset();
                tiger.update((byte) 1);
                tiger.update(current);
                tiger.update(last);
                last = tiger.digest();
            }

            nodes.set(i, MARKER);
        }
        assert(last != null);
        return last;
    }

    protected void engineReset() {
        bufferOffset = 0;
        byteCount = 0;
        nodes.clear();
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


    private void push(byte[] data) {
        if (!nodes.isEmpty()) {
            for (int i = 0; i < nodes.size(); i++) {
                byte[] node = nodes.get(i);
                if (node == MARKER) {
                    nodes.set(i, data);
                    return;
                }

                tiger.reset();
                tiger.update((byte) 1);
                tiger.update(node);
                tiger.update(data);
                data = tiger.digest();
                nodes.set(i, MARKER);
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
