
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
import com.limegroup.gnutella.util.DataUtils;
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
     * default size for each element.
     */
    public static final int DEFAULT_ELEMENT_SIZE = 12;
    
    /**
     * Max number of elements a digest can contain
     * TODO: simpp this
     */
    private static final int MAX_ELEMENTS = 1024;
    
    /**
     * 24 bits = 16MB ram!
     */
    private static final int MAX_ELEMENT_SIZE = 24;
    
    public AltLocDigest() {
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
     * the mask to use by the hash functions - ensures the range stays within reasonable bounds
     */
    private int _mask;
    
    /**
     * if we want to use custom hash function, put it here.
     */
    private HashFunction _hasher;
    
    public void add(Object o) {
        if (! (o instanceof AlternateLocation))
            throw new IllegalArgumentException ("trying to add a non-altloc to an altloc digest");
        AlternateLocation loc = (AlternateLocation)o;
        
        if (_hasher != null)
            _values.set(_hasher.hash(loc) & _mask);
        else
            _values.set(loc.hash() & _mask);
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
        if (_hasher != null)
            return _values.get(_hasher.hash(loc) & _mask);
        else
            return _values.get(loc.hash() & _mask);
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
        int maxElement = _values.length();
        
        // make sure we can represent all the values in the bitset
        while (1 << _elementSize <= maxElement)
            _elementSize++; 
        
        // create the output array
        int bitSize = _values.cardinality() * _elementSize;
        int size = bitSize  / 8;
        if (bitSize % 8 != 0)
            size++;
        byte []ret = new byte[size+3];
        // write the size of each element and the number of elements 
        ret[0] = (byte)_elementSize;
        ByteOrder.short2leb((short)_values.cardinality(),ret,1);
        
        // the two most common sizes will be 8 and 12 bits, so optimize those.
        if (_elementSize == 8) {
            int index =3;
            for(int i=_values.nextSetBit(0); i>=0;i=_values.nextSetBit(i+1))
                ret[index++]=(byte)i;
            
        } else if (_elementSize == 12) {
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
            // if its not 8 or 12 bits, serialize the slow way. 
            BitSet tmp = new BitSet((ret.length-3)*8);
            
            int [] values = new int[_values.cardinality()];
            int i=0;
            for(int element=_values.nextSetBit(0); element >=0; 
            		element=_values.nextSetBit(element+1)) 
                values[i++]=element;
            byte [] packed = DataUtils.bitPack(values,_elementSize);
            System.arraycopy(packed,0,ret,3,packed.length);
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
        
        // unpack the values
        int [] values = DataUtils.bitUnpack(data,3,numElements,elementSize);
        
        // and populate the bitset
        for (int i = 0;i < values.length;i++) 
            digest._values.set(values[i]);
        
        return digest;
    }
    
    /**
     * parses a digest contained in the given InputStream.  The resulting
     * bloom filter does not have an associated hash function with it, so 
     * make sure you check against the proper type of altlocs.
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
    
    
    public int getElementSize() {
        return _elementSize;
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
    
    public void setElementSize(int size) {
        _elementSize = size;
        for (int i=0;i < size;i++)
            _mask |= (1 << i); 
    }
}
