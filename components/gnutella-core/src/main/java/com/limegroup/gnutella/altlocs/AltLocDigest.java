
package com.limegroup.gnutella.altlocs;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.BitSet;
import com.limegroup.gnutella.util.BloomFilter;
import com.limegroup.gnutella.util.HashFunction;

/**
 * A bloom filter factory that can create filters for direct locs and push locs
 * 
 * It assumes that each altloc hashes to a 12-bit digit, which allows us to have up to 
 * 4096 altlocs per mesh - which is plenty.
 * 
 * In memory, those 4096 bits are stored as a BitSet, but on the network they are 
 * represented as list of values - i.e. each 3 bytes carry the hashes of two altlocs.
 * 
 * You can store either pushlocs or direct altlocs, or mix them (at your own risk).
 * 
 * 
 * On the wire, the digest is represented as a 3-byte header, and body.
 * The first byte gives the size of each element, and the next two bytes the number of elements.
 * The rest of the body is a list of elements, each one packed in size bytes.
 * Note: Whenever the default size of 12 bits/element is used, the class uses an optimized 
 * serialization, but it supports reading/writing to elements any size up to 24 bits. 
 */
public class AltLocDigest implements BloomFilter {

    /**
     * constants for which hasing function this digest uses.
     */
    private final HashFunction DIRECT = new DirectLocHasher();
    private final HashFunction PUSH = new PushLocHasher();
    
    /**
     * default size for each element
     */
    private static final int DEFAULT_ELEMENT_SIZE = 12;
    
    /**
     * Max number of elements a digest can contain
     * TODO: simpp this
     */
    private static final int MAX_ELEMENTS = 1024;
    
    /**
     * 24 bits = 16MB ram!
     */
    private static final int MAX_ELEMENT_SIZE = 24;
    
    /**
     * constructor with default values.
     */
    public AltLocDigest(boolean push) {
        this();
        _hash = push ? PUSH : DIRECT;
    }
    
    private AltLocDigest() {
        _values = new BitSet();
        setElementSize(DEFAULT_ELEMENT_SIZE);
    }
    
    /**
     * A BitSet storage for the values of the filter.
     * When we have many entries, or need to do boolean algebra we use
     * this representation.
     */
    private BitSet _values;
    
    /**
     * How many bits are necessary to represent each element.  This determines the
     * range of the hash function.  Use setElementSize() instead of modifying directly.
     */
    private int _elementSize;
    
    /**
     * the mask to use by the hash functions
     */
    private int _mask;
    
    /**
     * the actual hashing function.  Acts differently on altlocs than pushlocs.
     * range is [0, 2^_elementSize)
     */
    private HashFunction _hash;
    
    /**
     * tells this digest to use the hash function for direct altlocs.
     * takes effect on the next add/check.
     */
    public void setDirect() {
        _hash = DIRECT;
    }

    /**
     * tells this digest to use the hash function for push altlocs.
     * takes effect on the next add/check.
     */
    public void setPush() {
        _hash = PUSH;
    }
    
    public void add(Object o) {
        if (! (o instanceof AlternateLocation))
            throw new IllegalArgumentException ("trying to add a non-altloc to an altloc digest");
        AlternateLocation loc = (AlternateLocation)o;
        _values.set(_hash.hash(loc));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#addAll(java.util.Collection)
     */
    public void addAll(Collection c) {
        for (Iterator iter = c.iterator();iter.hasNext();)
            add(iter.next());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#contains(java.lang.Object)
     */
    public boolean contains(Object o) {
        if (! (o instanceof AlternateLocation))
            return false;
        AlternateLocation loc = (AlternateLocation)o;
        
        return _values.get(_hash.hash(loc));
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c) {
        for (Iterator iter = c.iterator();iter.hasNext();) {
            if (!contains(iter.next()))
                return false;
        }
        return true;
    }
    
    /**
     * @return a packed representation of hashes, where every 3 bytes represent
     * two altlocs.
     */
    public byte [] toBytes() {
        // create the output array
        int bitSize = _values.cardinality() * _elementSize;
        int size = bitSize  / 8;
        if (bitSize % 8 != 0)
            size++;
        byte []ret = new byte[size+3];
        // write the size of each element and the number of elements 
        ret[0] = (byte)_elementSize;
        ByteOrder.short2leb((short)_values.cardinality(),ret,1);
        
        // if the elements are the default size of 12 bits, serialize them the fast way
        if (_elementSize == DEFAULT_ELEMENT_SIZE ) {
            int index = 3;
            boolean first = true;
            for(int i=_values.nextSetBit(0); i>=0;i=_values.nextSetBit(i+1)){
                if (first) {
                    ret[index++] = (byte)((i & 0xFF0 ) >> 4);
                    ret[index] = (byte)((i & 0xF) << 4);
                    first = false;
                } else {
                    ret [index++] |= (byte)((i & 0xF00) >> 8);
                    ret [index++] = (byte)(i & 0xFF);
                    first = true;
                }
            }
        } else {
            // otherwise, the slow way 
            BitSet tmp = new BitSet((ret.length-3)*8);
            int offset = 0;
            int mask = 1 << _elementSize-1;
            for(int element=_values.nextSetBit(0); element >=0; 
            	element=_values.nextSetBit(element+1)) {
                for (int i = 0; i < _elementSize; i++) {
                    if (((element << i) & mask) == mask) 
                        tmp.set(offset);
                    offset++;
                }
            }
            
            // a LongBuffer -> ByteBuffer conversion would do this much faster...
            for (int i = 0;i < tmp.length();i++) {
                if (tmp.get(i)) 
                    ret[3+i/8] |= (0x80 >>> (i % 8));
            }
        }
        
        return ret;
        
    }
    
    public void write(OutputStream out) throws IOException {
        out.write(toBytes());
    }
    
    /**
     * parses a digest contained in the given byte array.  The resulting
     * bloom filter does not have associated hash function with it, so use
     * setPush or setDirect before using it.
     * 
     * @throws IOException if input was invalid.
     */
    public static AltLocDigest parseDigest(byte []data, int offset, int length) 
    throws IOException {
        // do some sanity checks
        if (data.length < offset+length || length < 3)
            throw new IOException();
        
        int elementSize = data[0];
        if (elementSize > MAX_ELEMENT_SIZE)
            throw new IOException();
        
        int numElements = ByteOrder.ubytes2int(ByteOrder.leb2short(data,1));
        if (numElements > MAX_ELEMENTS)
            throw new IOException();
        if ((length-3) * 8 < elementSize * numElements)
            throw new IOException();
        
        // create the new altloc digest
        AltLocDigest digest = new AltLocDigest();
        digest.setElementSize(elementSize);
        
        // and populate it
        offset+=24;
        for (int i = 0; i < numElements; i++) {
                int element=0;
                for (int j = 0; j < elementSize; j++) {
                        int current = data[offset / 8];
                        int extractedBit = current & (0x80 >>> (offset % 8));
                        extractedBit >>= (7 - (offset % 8));
                        element <<= 1;
                        element |= extractedBit;
                        offset++;
                }
                if (element < 0) element+=256;
                digest._values.set(element);
        }

        
        return digest;
    }
    
    /**
     * parses a digest contained in the given InputStream.  The resulting
     * bloom filter does not have an associated hash function with it, so use
     * setPush or setDirect before using it.
     * 
     * @throws IOException if input was invalid.
     */
    public static AltLocDigest parseDigest(InputStream in) throws IOException {
        
        // read the header, do some sanity checks
        int elementSize = in.read();
        if (elementSize > 32)
            throw new IOException();
        
        short snum = ByteOrder.leb2short(in);
        int num = ByteOrder.ubytes2int(snum);
        if (num > MAX_ELEMENTS)
            throw new IOException();
        
        int size = (elementSize * num) / 8;
        if (elementSize % 8 != 0)
            size++;
        
        // read the entire digest in memory before trying to parse
        byte [] digest = new byte[size+3];
        digest[0] = (byte)elementSize;
        ByteOrder.short2leb(snum,digest,1);
        
        DataInputStream dais = new DataInputStream(in);
        dais.readFully(digest,3,size);
        
        return parseDigest(digest,0,digest.length);
    }
    
    private final class DirectLocHasher implements HashFunction {
        /**
         * hashes a direct altloc. It takes as many bits as possible from the tail
         * of the ip address.
         */
       public int hash(Object o) {
           DirectAltLoc loc = (DirectAltLoc)o;
           int full =0;
           try {
               byte [] addr = loc.getHost().getHostBytes();
               full = (addr[3] << 24) | (addr[2] << 16) | (addr[1] << 8) | addr[0];
           }catch (UnknownHostException hmm) {
               ErrorService.error(hmm);
           }
           return full & _mask;
       }
    }
    
    private final class PushLocHasher implements HashFunction {
        /**
         * hashes a pushloc.  Takes as many bits as possible from the guid
         */
        public int hash(Object o) {
            PushAltLoc push = (PushAltLoc)o;
            byte [] guid = push.getPushAddress().getClientGUID();
            
            int full = (guid[3] << 24) | (guid[2] << 16) | (guid[1]) << 8 | guid[0];
            
            return full & _mask;
        }
     }

    
    public void and(BloomFilter other) {
        AltLocDigest digest = (AltLocDigest) other;
        _values.and(digest._values);
    }
    
    /**
     * slow - use andNot whenver possible
     */
    public void invert() {
        for (int i = 0;i < _values.size();i++)
            _values.flip(i);
    }
    
    public void or(BloomFilter other) {
        AltLocDigest digest = (AltLocDigest) other;
        _values.or(digest._values);
    }
    public void xor(BloomFilter other) {
        AltLocDigest digest = (AltLocDigest) other;
        _values.xor(digest._values);
    }
    
    /**
     * efficient AndNot with another AltLocDigest.
     */
    public void andNot(AltLocDigest other) {
        _values.andNot(other._values);
    }
    
    private void setElementSize(int size) {
        _elementSize = size;
        for (int i=0;i < size;i++)
            _mask |= (1 << i); 
    }
}
