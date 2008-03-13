package com.limegroup.gnutella.routing;

import org.limewire.collection.BitField;
import org.limewire.collection.BitSet;

/**
 * Implementation of QRTTableStorage which uses a BitSet.
 */
class BitSetQRTTableStorage implements QRTTableStorage {

    private final BitSet bitSet;
    private final int bitTableLength;
    
    private BitSet cachedResizedSet;
    private int cachedResizedSetLength;
    
    BitSetQRTTableStorage(int bitTableLength) {
        this.bitTableLength = bitTableLength;
        this.bitSet = new BitSet();
    }
    
    /**
     * copy constructor.
     */
    private BitSetQRTTableStorage(BitSet bitSet, int bitTableLength) {
        this.bitSet = bitSet;
        this.bitTableLength = bitTableLength;
    }
    
    public double getPercentFull() {
        return bitSet.cardinality() * 100.0 / bitTableLength;
    }
    
    public QRTTableStorage clone() {
        return new BitSetQRTTableStorage((BitSet)bitSet.clone(), bitTableLength);
    }
    
    public void clear(int hash) {
        cachedResizedSet = null;
        bitSet.clear(hash);

    }

    public void compact() {
        bitSet.compact();
    }

    public int getUnitsInUse() {
        return bitSet.getUnitsInUse();
    }

    public int getUnusedUnits() {
        return bitSet.unusedUnits();
    }

    public int numUnitsWithLoad(int load) {
        return bitSet.numUnitsWithLoad(load);
    }

    public void or(BitField other) {
        if (other instanceof BitSetQRTTableStorage) {
            BitSetQRTTableStorage optimized = (BitSetQRTTableStorage) other;
            bitSet.or(optimized.bitSet);
        } else {
            for (int i=other.nextSetBit(0); i >= 0; i=other.nextSetBit(i+1)) 
                bitSet.set(i);
        }
    }

    public QRTTableStorage resize(int newSize) {
        // if this bitTable is already the correct size,
        // return it
        if (bitTableLength == newSize)
            return this;
        
        // if we already have a cached resizedQRT and
        // it is the correct size, then use it.
        if (cachedResizedSet != null && cachedResizedSetLength == newSize)
            return new BitSetQRTTableStorage(cachedResizedSet, cachedResizedSetLength);
        
        cachedResizedSet = new BitSet();
        cachedResizedSetLength = newSize;
        
        //This algorithm scales between tables of different lengths.
        //Refer to the query routing paper for a full explanation.
        //(The below algorithm, contributed by Philippe Verdy,
        // uses integer values instead of decimal values
        // as both double & float can cause precision problems on machines
        // with odd setups, causing the wrong values to be set in tables)
        final int m = this.bitTableLength;
        final int m2 = cachedResizedSetLength;
        for (int i = this.bitSet.nextSetBit(0); i >= 0;
          i = this.bitSet.nextSetBit(i + 1)) {
             // floor(i*m2/m)
             final int firstSet = (int)(((long)i * m2) / m);
             i = this.bitSet.nextClearBit(i + 1);
             // ceil(i*m2/m)
             final int lastNotSet = (int)(((long)i * m2 - 1) / m + 1);
             cachedResizedSet.set(firstSet, lastNotSet);
        }
        
        return new BitSetQRTTableStorage(cachedResizedSet, cachedResizedSetLength);
    }

    public void set(int hash) {
        cachedResizedSet = null;
        bitSet.set(hash);
    }

    public void xor(BitField other) {
        if (other instanceof BitSetQRTTableStorage) {
            BitSetQRTTableStorage optimized = (BitSetQRTTableStorage) other;
            bitSet.xor(optimized.bitSet);
        } else {
            for (int i=other.nextSetBit(0); i >= 0; i=other.nextSetBit(i+1)) 
                bitSet.set(i,bitSet.get(i) != other.get(i));
        }

    }

    public int cardinality() {
        return bitSet.cardinality();
    }

    public boolean get(int i) {
        return bitSet.get(i);
    }

    public int maxSize() {
        return bitTableLength;
    }

    public int nextClearBit(int i) {
        return bitSet.nextClearBit(i);
    }

    public int nextSetBit(int i) {
        return bitSet.nextSetBit(i);
    }
    
    public boolean equals(Object o) {
        if (! (o instanceof BitSetQRTTableStorage)) 
            return false;
        
            // this will be updated when other types are implemented
        BitSetQRTTableStorage other = (BitSetQRTTableStorage)o;
        return bitSet.equals(other.bitSet);
    }

}
