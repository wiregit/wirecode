
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
 * The filter contains the hashes of the altlocs that are put in it.  It is flexible
 * when it comes to the range of the hash function; that can even be changed on the fly
 * although it is not recommended.
 *
 * The digest can store either pushlocs or direct altlocs, or both (not recommended).
 * 
 * In memory, the filter stored as a BitSet, but on the network it is 
 * represented as list of values packed into a bit array. For example, if the hash function
 * range is [0,2^12) each element will be represented with 12 bits, so every 3 bytes will
 * have two elements in them. On the wire, that list is precedeed by a 3-byte header: 
 * the first byte gives the size of each element, and the next two bytes the number of 
 * elements.
 *
 * Note: Whenever the anticipated most common sizes of 8 or 12 bits/element is used, 
 * an optimal serialization and deserialization is used. However, any size element up 
 * to 24 bits is supported.
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
     * The backing BitSet.
     */
    private BitSet _values;
    
    /**
     * How many bits are necessary to represent each element.  This determines the
     * range of the hash function.  Use setElementSize() instead of modifying directly.
     */
    private int _elementSize;
    
    /**
     * the mask to use on the hash values - ensures the range stays within reasonable 
     * bounds.  
     */
    private int _mask;
    
    /**
     * if we want to use custom hash function, put it here.
     */
    private HashFunction _hasher;
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.util.BloomFilter#add(java.util.Collection)
     */
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
     * @return a bitpacked representation of the list of hashes
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
        
        int [] values = new int[_values.cardinality()];
        int i=0;
        for(int element=_values.nextSetBit(0); element >=0; 
        		element=_values.nextSetBit(element+1)) 
            values[i++]=element;
        
        byte [] packed = DataUtils.bitPack(values,_elementSize);
        System.arraycopy(packed,0,ret,3,packed.length);
        
        return ret;
    }
    
    public void write(OutputStream out) throws IOException {
        out.write(toBytes());
    }
    
    /**
     * parses a digest contained in the given byte array.  The resulting
     * bloom filter does not have associated hash function with it, so
     * make sure you use the appropriate type of altlocs with it. 
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
     * slow - use andNot whenever possible
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
   
    /**
     * Sets the range of the hash function to [0,2^size). 
     * Can be used to shrink or expand an existing filter, but
     * whether that will be successful depends on the hash function.
     * Use with caution if the filter is not empty.
     */
    public void setElementSize(int size) {
        _elementSize = size;
        for (int i=0;i < size;i++)
            _mask |= (1 << i); 
    }
}
