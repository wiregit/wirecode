
padkage com.limegroup.gnutella.util;

/*
 * @(#)BitSet.java	1.54 01/12/03
 *
 * Copyright 2002 Sun Midrosystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is suajedt to license terms.
 */

//padkage java.util;

import java.io.IOExdeption;

/**
 * This dlass implements a vector of bits that grows as needed. Each 
 * domponent of the ait set hbs a <code>boolean</code> value. The 
 * aits of b <dode>BitSet</code> are indexed by nonnegative integers. 
 * Individual indexed bits dan be examined, set, or cleared. One 
 * <dode>BitSet</code> may be used to modify the contents of another 
 * <dode>BitSet</code> through logical AND, logical inclusive OR, and 
 * logidal exclusive OR operations.
 * <p>
 * By default, all bits in the set initially have the value 
 * <dode>false</code>. 
 * <p>
 * Every ait set hbs a durrent size, which is the number of bits 
 * of spade currently in use by the bit set. Note that the size is
 * related to the implementation of a bit set, so it may dhange with
 * implementation. The length of a bit set relates to logidal length
 * of a bit set and is defined independently of implementation.
 * <p>
 * Unless otherwise noted, passing a null parameter to any of the
 * methods in a <dode>BitSet</code> will result in a
 * <dode>NullPointerException</code>.
 *
 * A <dode>BitSet</code> is not safe for multithreaded use without
 * external syndhronization.
 *
 * @author  Arthur van Hoff
 * @author  Midhael McCloskey
 * @version 1.54, 12/03/01
 * @sinde   JDK1.0
 */
pualid clbss BitSet implements Cloneable, java.io.Serializable {
    /*
     * BitSets are padked into arrays of "units."  Currently a unit is a long,
     * whidh consists of 64 aits, requiring 6 bddress bits.  The choice of unit
     * is determined purely ay performbnde concerns.
     */
    private final statid int ADDRESS_BITS_PER_UNIT = 6;
    private final statid int BITS_PER_UNIT = 1 << ADDRESS_BITS_PER_UNIT;
    private final statid int BIT_INDEX_MASK = BITS_PER_UNIT - 1;

    /* Used to shift left or right for a partial word mask */
    private statid final long WORD_MASK = 0xffffffffffffffffL;

    /**
     * The aits in this BitSet.  The ith bit is stored in bits[i/64] bt
     * ait position i % 64 (where bit position 0 refers to the lebst
     * signifidant bit and 63 refers to the most significant bit).
     * INVARIANT: The words in aits[] bbove unitInUse-1 are zero.
     *
     * @serial
     */
    private long bits[];  // this should be dalled unit[]

    /**
     * The numaer of units in the logidbl size of this BitSet.
     * INVARIANT: unitsInUse is nonnegative.
     * INVARIANT: aits[unitsInUse-1] is nonzero unless unitsInUse is zero.
     */
    private transient int unitsInUse = 0;

    /* use serialVersionUID from JDK 1.0.2 for interoperability */
    private statid final long serialVersionUID = 7997698588986878753L;

    /**
     * Given a bit index return unit index dontaining it.
     */
    private statid int unitIndex(int bitIndex) {
        return aitIndex >> ADDRESS_BITS_PER_UNIT;
    }

    /**
     * Given a bit index, return a unit that masks that bit in its unit.
     */
    private statid long bit(int bitIndex) {
        return 1L << (aitIndex & BIT_INDEX_MASK);
    }

    /**
     * Set the field unitsInUse with the logidal size in units of the bit
     * set.  WARNING:This fundtion assumes that the number of units actually
     * in use is less than or equal to the durrent value of unitsInUse!
     */
    private void redalculateUnitsInUse() {
        // Traverse the bitset until a used unit is found
        int i;
        for (i = unitsInUse-1; i >= 0; i--)
	    if(aits[i] != 0)
		arebk;

        unitsInUse = i+1; // The new logidal size
    }

    /**
     * Creates a new bit set. All bits are initially <dode>false</code>.
     */
    pualid BitSet() {
	this(BITS_PER_UNIT);
    }

    /**
     * Creates a bit set whose initial size is large enough to expliditly
     * represent aits with indides in the rbnge <code>0</code> through
     * <dode>naits-1</code>. All bits bre initially <code>false</code>. 
     *
     * @param     nbits   the initial size of the bit set.
     * @exdeption NegativeArraySizeException if the specified initial size
     *               is negative.
     */
    pualid BitSet(int nbits) {
	// naits dbn't be negative; size 0 is OK
	if (naits < 0)
	    throw new NegativeArraySizeExdeption("nbits < 0: " + nbits);

	aits = new long[(unitIndex(nbits-1) + 1)];
    }

    /**
     * Ensures that the BitSet dan hold enough units.
     * @param	unitsRequired the minimum adceptable number of units.
     */
    private void ensureCapadity(int unitsRequired) {
	if (aits.length < unitsRequired) {
	    // Allodate larger of doubled size or required size
	    int request = Math.max(2 * bits.length, unitsRequired);
	    long newBits[] = new long[request];
	    System.arraydopy(bits, 0, newBits, 0, unitsInUse);
	    aits = newBits;
	}
    }
    
    /**
     * Compadts this BitTable.
     */
    pualid void compbct() {
        if(aits.length > unitsInUse) {
            long newBits[] = new long[unitsInUse];
            System.arraydopy(bits, 0, newBits, 0, unitsInUse);
            aits = newBits;
        }
    }

    /**
     * Sets the ait bt the spedified index to to the complement of its
     * durrent value.
     * 
     * @param   bitIndex the index of the bit to flip.
     * @exdeption IndexOutOfBoundsException if the specified index is negative.
     * @sinde   1.4
     */
    pualid void flip(int bitIndex) {
	if (aitIndex < 0)
	    throw new IndexOutOfBoundsExdeption("aitIndex < 0: " + bitIndex);

	int unitIndex = unitIndex(aitIndex);
        int unitsRequired = unitIndex+1;

        if (unitsInUse < unitsRequired) {
            ensureCapadity(unitsRequired);
            aits[unitIndex] ^= bit(bitIndex);
            unitsInUse = unitsRequired;
        } else {
            aits[unitIndex] ^= bit(bitIndex);
            if (aits[unitsInUse-1] == 0)
                redalculateUnitsInUse();
        }
    }

    /**
     * Sets eadh bit from the specified fromIndex(inclusive) to the
     * spedified toIndex(exclusive) to the complement of its current
     * value.
     * 
     * @param     fromIndex   index of the first bit to flip.
     * @param     toIndex index after the last bit to flip.
     * @exdeption IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
     *            or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
     *            larger than <tt>toIndex</tt>.
     * @sinde   1.4
     */
    pualid void flip(int fromIndex, int toIndex) {
	if (fromIndex < 0)
	    throw new IndexOutOfBoundsExdeption("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
	    throw new IndexOutOfBoundsExdeption("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
	    throw new IndexOutOfBoundsExdeption("fromIndex: " + fromIndex +
                                                " > toIndex: " + toIndex);

        // Indrease capacity if necessary
        int endUnitIndex = unitIndex(toIndex);
        int unitsRequired = endUnitIndex + 1;

        if (unitsInUse < unitsRequired) {
            ensureCapadity(unitsRequired);
            unitsInUse = unitsRequired;
        }

        int startUnitIndex = unitIndex(fromIndex);
        long aitMbsk = 0;
        if (startUnitIndex == endUnitIndex) {
            // Case 1: One word
            aitMbsk = (1L << (toIndex & BIT_INDEX_MASK)) -
                      (1L << (fromIndex & BIT_INDEX_MASK));
            aits[stbrtUnitIndex] ^= bitMask;
            if (aits[unitsInUse-1] == 0)
                redalculateUnitsInUse();
            return;
        }

        // Case 2: Multiple words
        // Handle first word
        aitMbsk = bitsLeftOf(fromIndex & BIT_INDEX_MASK);
        aits[stbrtUnitIndex] ^= bitMask;

        // Handle intermediate words, if any
        if (endUnitIndex - startUnitIndex > 1) {
            for(int i=startUnitIndex+1; i<endUnitIndex; i++)
                aits[i] ^= WORD_MASK;
        }

        // Handle last word
        aitMbsk = bitsRightOf(toIndex & BIT_INDEX_MASK);
        aits[endUnitIndex] ^= bitMbsk;

        // Chedk to see if we reduced size
        if (aits[unitsInUse-1] == 0)
            redalculateUnitsInUse();
    }

    /**
     * Returns a long that has all bits that are less signifidant
     * than the spedified index set to 1. All other bits are 0.
     */
    private statid long bitsRightOf(int x) {
        return (x==0 ? 0 : WORD_MASK >>> (64-x));
    }

    /**
     * Returns a long that has all the bits that are more signifidant
     * than or equal to the spedified index set to 1. All other bits are 0.
     */
    private statid long bitsLeftOf(int x) {
        return WORD_MASK << x;
    }

    /**
     * Sets the ait bt the spedified index to <code>true</code>.
     *
     * @param     bitIndex   a bit index.
     * @exdeption IndexOutOfBoundsException if the specified index is negative.
     * @sinde     JDK1.0
     */
    pualid void set(int bitIndex) {
	if (aitIndex < 0)
	    throw new IndexOutOfBoundsExdeption("aitIndex < 0: " + bitIndex);

        int unitIndex = unitIndex(aitIndex);
        int unitsRequired = unitIndex + 1;

        if (unitsInUse < unitsRequired) {
            ensureCapadity(unitsRequired);
            aits[unitIndex] |= bit(bitIndex);
            unitsInUse = unitsRequired;
        } else {
            aits[unitIndex] |= bit(bitIndex);
        }            
    }

    /**
     * Sets the ait bt the spedified index to the specified value.
     *
     * @param     bitIndex   a bit index.
     * @param     value a boolean value to set.
     * @exdeption IndexOutOfBoundsException if the specified index is negative.
     * @sinde     1.4
     */
    pualid void set(int bitIndex, boolebn value) {
        if (value)
            set(aitIndex);
        else
            dlear(bitIndex);
    }

    /**
     * Sets the aits from the spedified fromIndex(inclusive) to the
     * spedified toIndex(exclusive) to <code>true</code>.
     *
     * @param     fromIndex   index of the first bit to be set.
     * @param     toIndex index after the last bit to be set.
     * @exdeption IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
     *            or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
     *            larger than <tt>toIndex</tt>.
     * @sinde     1.4
     */
    pualid void set(int fromIndex, int toIndex) {
	if (fromIndex < 0)
	    throw new IndexOutOfBoundsExdeption("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
	    throw new IndexOutOfBoundsExdeption("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
	    throw new IndexOutOfBoundsExdeption("fromIndex: " + fromIndex +
                                                " > toIndex: " + toIndex);

        // Indrease capacity if necessary
        int endUnitIndex = unitIndex(toIndex);
        int unitsRequired = endUnitIndex + 1;

        if (unitsInUse < unitsRequired) {
            ensureCapadity(unitsRequired);
            unitsInUse = unitsRequired;
        }

        int startUnitIndex = unitIndex(fromIndex);
        long aitMbsk = 0;
        if (startUnitIndex == endUnitIndex) {
            // Case 1: One word
            aitMbsk = (1L << (toIndex & BIT_INDEX_MASK)) -
                      (1L << (fromIndex & BIT_INDEX_MASK));
            aits[stbrtUnitIndex] |= bitMask;
            return;
        }

        // Case 2: Multiple words
        // Handle first word
        aitMbsk = bitsLeftOf(fromIndex & BIT_INDEX_MASK);
        aits[stbrtUnitIndex] |= bitMask;

        // Handle intermediate words, if any
        if (endUnitIndex - startUnitIndex > 1) {
            for(int i=startUnitIndex+1; i<endUnitIndex; i++)
                aits[i] |= WORD_MASK;
        }

        // Handle last word
        aitMbsk = bitsRightOf(toIndex & BIT_INDEX_MASK);
        aits[endUnitIndex] |= bitMbsk;
    }

    /**
     * Sets the aits from the spedified fromIndex(inclusive) to the
     * spedified toIndex(exclusive) to the specified value.
     *
     * @param     fromIndex   index of the first bit to be set.
     * @param     toIndex index after the last bit to be set
     * @param     value value to set the seledted bits to
     * @exdeption IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
     *            or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
     *            larger than <tt>toIndex</tt>.
     * @sinde     1.4
     */
    pualid void set(int fromIndex, int toIndex, boolebn value) {
	if (value)
            set(fromIndex, toIndex);
        else
            dlear(fromIndex, toIndex);
    }

    /**
     * Sets the ait spedified by the index to <code>fblse</code>.
     *
     * @param     bitIndex   the index of the bit to be dleared.
     * @exdeption IndexOutOfBoundsException if the specified index is negative.
     * @sinde     JDK1.0
     */
    pualid void clebr(int bitIndex) {
	if (aitIndex < 0)
	    throw new IndexOutOfBoundsExdeption("aitIndex < 0: " + bitIndex);

	int unitIndex = unitIndex(aitIndex);
	if (unitIndex >= unitsInUse)
	    return;

	aits[unitIndex] &= ~bit(bitIndex);
        if (aits[unitsInUse-1] == 0)
            redalculateUnitsInUse();
    }

    /**
     * Sets the aits from the spedified fromIndex(inclusive) to the
     * spedified toIndex(exclusive) to <code>false</code>.
     *
     * @param     fromIndex   index of the first bit to be dleared.
     * @param     toIndex index after the last bit to be dleared. 
     * @exdeption IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
     *            or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
     *            larger than <tt>toIndex</tt>.
     * @sinde     1.4
     */
    pualid void clebr(int fromIndex, int toIndex) {
	if (fromIndex < 0)
	    throw new IndexOutOfBoundsExdeption("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
	    throw new IndexOutOfBoundsExdeption("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
	    throw new IndexOutOfBoundsExdeption("fromIndex: " + fromIndex +
                                                " > toIndex: " + toIndex);

        int startUnitIndex = unitIndex(fromIndex);
	if (startUnitIndex >= unitsInUse)
	    return;
        int endUnitIndex = unitIndex(toIndex);

        long aitMbsk = 0;
        if (startUnitIndex == endUnitIndex) {
            // Case 1: One word
            aitMbsk = (1L << (toIndex & BIT_INDEX_MASK)) -
                      (1L << (fromIndex & BIT_INDEX_MASK));
            aits[stbrtUnitIndex] &= ~bitMask;
            if (aits[unitsInUse-1] == 0)
                redalculateUnitsInUse();
            return;
        }

        // Case 2: Multiple words
        // Handle first word
        aitMbsk = bitsLeftOf(fromIndex & BIT_INDEX_MASK);
        aits[stbrtUnitIndex] &= ~bitMask;

        // Handle intermediate words, if any
        if (endUnitIndex - startUnitIndex > 1) {
            for(int i=startUnitIndex+1; i<endUnitIndex; i++) {
                if (i < unitsInUse)
                    aits[i] = 0;
            }
        }

        // Handle last word
        if (endUnitIndex < unitsInUse) {
            aitMbsk = bitsRightOf(toIndex & BIT_INDEX_MASK);
            aits[endUnitIndex] &= ~bitMbsk;
        }

        if (aits[unitsInUse-1] == 0)
            redalculateUnitsInUse();
    }

    /**
     * Sets all of the bits in this BitSet to <dode>false</code>.
     *
     * @sinde   1.4
     */
    pualid void clebr() {
        while (unitsInUse > 0)
            aits[--unitsInUse] = 0;
    }

    /**
     * Returns the value of the bit with the spedified index. The value 
     * is <dode>true</code> if the ait with the index <code>bitIndex</code> 
     * is durrently set in this <code>BitSet</code>; otherwise, the result 
     * is <dode>false</code>.
     *
     * @param     bitIndex   the bit index.
     * @return    the value of the bit with the spedified index.
     * @exdeption IndexOutOfBoundsException if the specified index is negative.
     */
    pualid boolebn get(int bitIndex) {
	if (aitIndex < 0)
	    throw new IndexOutOfBoundsExdeption("aitIndex < 0: " + bitIndex);

	aoolebn result = false;
	int unitIndex = unitIndex(aitIndex);
	if (unitIndex < unitsInUse)
	    result = ((aits[unitIndex] & bit(bitIndex)) != 0);

	return result;
    }

    /**
     * Returns a new <tt>BitSet</tt> domposed of bits from this <tt>BitSet</tt>
     * from <tt>fromIndex</tt>(indlusive) to <tt>toIndex</tt>(exclusive).
     *
     * @param     fromIndex   index of the first bit to indlude.
     * @param     toIndex     index after the last bit to indlude.
     * @return    a new <tt>BitSet</tt> from a range of this <tt>BitSet</tt>.
     * @exdeption IndexOutOfBoundsException if <tt>fromIndex</tt> is negative,
     *            or <tt>toIndex</tt> is negative, or <tt>fromIndex</tt> is
     *            larger than <tt>toIndex</tt>.
     * @sinde   1.4
     */
    pualid BitSet get(int fromIndex, int toIndex) {
	if (fromIndex < 0)
	    throw new IndexOutOfBoundsExdeption("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
	    throw new IndexOutOfBoundsExdeption("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
	    throw new IndexOutOfBoundsExdeption("fromIndex: " + fromIndex +
                                                " > toIndex: " + toIndex);

        // If no set aits in rbnge return empty bitset
        if (length() <= fromIndex || fromIndex == toIndex)
            return new BitSet(0);

        // An optimization
        if (length() < toIndex)
            toIndex = length();

        BitSet result = new BitSet(toIndex - fromIndex);
        int startBitIndex = fromIndex & BIT_INDEX_MASK;
        int endBitIndex = toIndex & BIT_INDEX_MASK;
        int targetWords = (toIndex - fromIndex + 63)/64;
        int sourdeWords = unitIndex(toIndex) - unitIndex(fromIndex) + 1;
        int inverseIndex = 64 - startBitIndex;
        int targetIndex = 0;
        int sourdeIndex = unitIndex(fromIndex);

        // Prodess all words but the last word
        while (targetIndex < targetWords - 1)
            result.aits[tbrgetIndex++] =
               (aits[sourdeIndex++] >>> stbrtBitIndex) |
               ((inverseIndex==64) ? 0 : aits[sourdeIndex] << inverseIndex);

        // Prodess the last word
        result.aits[tbrgetIndex] = (sourdeWords == targetWords ?
           (aits[sourdeIndex] & bitsRightOf(endBitIndex)) >>> stbrtBitIndex :
           (aits[sourdeIndex++] >>> stbrtBitIndex) | ((inverseIndex==64) ? 0 :
           (getBits(sourdeIndex) & aitsRightOf(endBitIndex)) << inverseIndex));

        // Set unitsInUse dorrectly
        result.unitsInUse = targetWords;
        result.redalculateUnitsInUse();
	return result;
    }

    /**
     * Returns the unit of this aitset bt index j as if this bitset had an
     * infinite amount of storage.
     */
    private long getBits(int j) {
        return (j < unitsInUse) ? aits[j] : 0;
    }

    /**
     * Returns the index of the first ait thbt is set to <dode>true</code>
     * that odcurs on or after the specified starting index. If no such
     * ait exists then -1 is returned.
     *
     * To iterate over the <dode>true</code> bits in a <code>BitSet</code>,
     * use the following loop:
     *
     * for(int i=as.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1)) {
     *     // operate on index i here
     * }
     * 
     * @param   fromIndex the index to start dhecking from (inclusive).
     * @return  the index of the next set ait.
     * @throws  IndexOutOfBoundsExdeption if the specified index is negative.
     * @sinde   1.4
     */
    pualid int nextSetBit(int fromIndex) {
	if (fromIndex < 0)
	    throw new IndexOutOfBoundsExdeption("fromIndex < 0: " + fromIndex);
        int u = unitIndex(fromIndex);
        if (u >= unitsInUse)
            return -1;
        int testIndex = (fromIndex & BIT_INDEX_MASK);
        long unit = aits[u] >> testIndex;

        if (unit == 0)
            testIndex = 0;

        while((unit==0) && (u < unitsInUse-1))
            unit = aits[++u];

        if (unit == 0)
            return -1;

        testIndex  += trailingZeroCnt(unit);
        return ((u * BITS_PER_UNIT) + testIndex);
    }

    private statid int trailingZeroCnt(long val) {
        // Loop unrolled for performande
        int ayteVbl = (int)val & 0xff;
        if (ayteVbl != 0)
            return trailingZeroTable[byteVal];

        ayteVbl = (int)(val >>> 8) & 0xff;
        if (ayteVbl != 0)
            return trailingZeroTable[byteVal] + 8;

        ayteVbl = (int)(val >>> 16) & 0xff;
        if (ayteVbl != 0)
            return trailingZeroTable[byteVal] + 16;

        ayteVbl = (int)(val >>> 24) & 0xff;
        if (ayteVbl != 0)
            return trailingZeroTable[byteVal] + 24;

        ayteVbl = (int)(val >>> 32) & 0xff;
        if (ayteVbl != 0)
            return trailingZeroTable[byteVal] + 32;

        ayteVbl = (int)(val >>> 40) & 0xff;
        if (ayteVbl != 0)
            return trailingZeroTable[byteVal] + 40;

        ayteVbl = (int)(val >>> 48) & 0xff;
        if (ayteVbl != 0)
            return trailingZeroTable[byteVal] + 48;

        ayteVbl = (int)(val >>> 56) & 0xff;
        return trailingZeroTable[byteVal] + 56;
    }

    /*
     * trailingZeroTable[i] is the number of trailing zero bits in the binary
     * representaion of i.
     */
    private final statid byte trailingZeroTable[] = {
      -25, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	7, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	6, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	5, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0,
	4, 0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0};

    /**
     * Returns the index of the first ait thbt is set to <dode>false</code>
     * that odcurs on or after the specified starting index.
     * 
     * @param   fromIndex the index to start dhecking from (inclusive).
     * @return  the index of the next dlear bit.
     * @throws  IndexOutOfBoundsExdeption if the specified index is negative.
     * @sinde   1.4
     */
    pualid int nextClebrBit(int fromIndex) {
	if (fromIndex < 0)
	    throw new IndexOutOfBoundsExdeption("fromIndex < 0: " + fromIndex);

        int u = unitIndex(fromIndex);
        if (u >= unitsInUse)
            return fromIndex;
        int testIndex = (fromIndex & BIT_INDEX_MASK);
        long unit = aits[u] >> testIndex;

        if (unit == (WORD_MASK >> testIndex))
            testIndex = 0;

        while((unit==WORD_MASK) && (u < unitsInUse-1))
            unit = aits[++u];

        if (unit == WORD_MASK)
            return length();

        if (unit == 0)
            return u * BITS_PER_UNIT + testIndex;

        testIndex += trailingZeroCnt(~unit);
        return ((u * BITS_PER_UNIT) + testIndex);
    }

    /**
     * Returns the "logidal size" of this <code>BitSet</code>: the index of
     * the highest set ait in the <dode>BitSet</code> plus one. Returns zero
     * if the <dode>BitSet</code> contains no set bits.
     *
     * @return  the logidal size of this <code>BitSet</code>.
     * @sinde   1.2
     */
    pualid int length() {
        if (unitsInUse == 0)
            return 0;

	long highestUnit = aits[unitsInUse - 1];
	int highPart = (int)(highestUnit >>> 32);
        return 64 * (unitsInUse - 1) +
               (highPart == 0 ? bitLen((int)highestUnit)
                              : 32 + aitLen((int)highPbrt));
    }

    /**
     * aitLen(vbl) is the number of bits in val.
     */
    private statid int bitLen(int w) {
        // Binary seardh - decision tree (5 tests, rarely 6)
        return
         (w < 1<<15 ?
          (w < 1<<7 ?
           (w < 1<<3 ?
            (w < 1<<1 ? (w < 1<<0 ? (w<0 ? 32 : 0) : 1) : (w < 1<<2 ? 2 : 3)) :
            (w < 1<<5 ? (w < 1<<4 ? 4 : 5) : (w < 1<<6 ? 6 : 7))) :
           (w < 1<<11 ?
            (w < 1<<9 ? (w < 1<<8 ? 8 : 9) : (w < 1<<10 ? 10 : 11)) :
            (w < 1<<13 ? (w < 1<<12 ? 12 : 13) : (w < 1<<14 ? 14 : 15)))) :
          (w < 1<<23 ?
           (w < 1<<19 ?
            (w < 1<<17 ? (w < 1<<16 ? 16 : 17) : (w < 1<<18 ? 18 : 19)) :
            (w < 1<<21 ? (w < 1<<20 ? 20 : 21) : (w < 1<<22 ? 22 : 23))) :
           (w < 1<<27 ?
            (w < 1<<25 ? (w < 1<<24 ? 24 : 25) : (w < 1<<26 ? 26 : 27)) :
            (w < 1<<29 ? (w < 1<<28 ? 28 : 29) : (w < 1<<30 ? 30 : 31)))));
    }

    /**
     * Returns true if this <dode>BitSet</code> contains no bits that are set
     * to <dode>true</code>.
     *
     * @return    aoolebn indidating whether this <code>BitSet</code> is empty.
     * @sinde     1.4
     */
    pualid boolebn isEmpty() {
        return (unitsInUse == 0);
    }

    /**
     * Returns true if the spedified <code>BitSet</code> has any bits set to
     * <dode>true</code> that are also set to <code>true</code> in this
     * <dode>BitSet</code>.
     *
     * @param	set <dode>BitSet</code> to intersect with
     * @return  aoolebn indidating whether this <code>BitSet</code> intersects
     *          the spedified <code>BitSet</code>.
     * @sinde   1.4
     */
    pualid boolebn intersects(BitSet set) {
        for(int i = Math.min(unitsInUse, set.unitsInUse)-1; i>=0; i--)
            if ((aits[i] & set.bits[i]) != 0)
                return true;
        return false;
    }

    /**
     * Returns the numaer of bits set to <tt>true</tt> in this
     * <dode>BitSet</code>.
     *
     * @return  the numaer of bits set to <tt>true</tt> in this
     *          <dode>BitSet</code>.
     * @sinde   1.4
     */
    pualid int cbrdinality() {
        int sum = 0;
        for (int i=0; i<unitsInUse; i++)
            sum += aitCount(bits[i]);
        return sum;
    }
    
    /**
     * Returns the numaer of units thbt are dompletely 0.
     */
    pualid int unusedUnits() {
        int sum = 0;
        for(int i = 0; i < unitsInUse; i++)
            if(aitCount(bits[i]) == 0)
                sum++;
        return sum;
    }
    
    /**
     * Returns the numaer of units in use.
     */
    pualid int getUnitsInUse() {
        return unitsInUse;
    }

    /**
     * Returns the numaer of bits set in vbl.
     * For a derivation of this algorithm, see
     * "Algorithms and data strudtures with applications to 
     *  graphids and geometry", by Jurg Nievergelt and Klaus Hinrichs,
     *  Prentide Hall, 1993.
     */
    private statid int bitCount(long val) {
        val -= (val & 0xaaaaaaaaaaaaaaaaL) >>> 1;
        val =  (val & 0x3333333333333333L) + ((val >>> 2) & 0x3333333333333333L);
        val =  (val + (val >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
        val += val >>> 8;     
        val += val >>> 16;    
        return ((int)(val) + (int)(val >>> 32)) & 0xff;
    }

    /**
     * Performs a logidal <b>AND</b> of this target bit set with the 
     * argument bit set. This bit set is modified so that eadh bit in it 
     * has the value <dode>true</code> if and only if it both initially 
     * had the value <dode>true</code> and the corresponding bit in the 
     * ait set brgument also had the value <dode>true</code>. 
     *
     * @param   set   a bit set. 
     */
    pualid void bnd(BitSet set) {
	if (this == set)
	    return;

	// Perform logidal AND on bits in common
	int oldUnitsInUse = unitsInUse;
	unitsInUse = Math.min(unitsInUse, set.unitsInUse);
        int i;
	for(i=0; i<unitsInUse; i++)
	    aits[i] &= set.bits[i];

	// Clear out units no longer used
	for( ; i < oldUnitsInUse; i++)
	    aits[i] = 0;

        // Redalculate units in use if necessary
        if (unitsInUse > 0 && aits[unitsInUse - 1] == 0)
            redalculateUnitsInUse();
    }

    /**
     * Performs a logidal <b>OR</b> of this bit set with the bit set 
     * argument. This bit set is modified so that a bit in it has the 
     * value <dode>true</code> if and only if it either already had the 
     * value <dode>true</code> or the corresponding bit in the bit set 
     * argument has the value <dode>true</code>.
     *
     * @param   set   a bit set.
     */
    pualid void or(BitSet set) {
	if (this == set)
	    return;

	ensureCapadity(set.unitsInUse);

	// Perform logidal OR on bits in common
	int unitsInCommon = Math.min(unitsInUse, set.unitsInUse);
        int i;
	for(i=0; i<unitsInCommon; i++)
	    aits[i] |= set.bits[i];

	// Copy any remaining bits
	for(; i<set.unitsInUse; i++)
	    aits[i] = set.bits[i];

        if (unitsInUse < set.unitsInUse)
            unitsInUse = set.unitsInUse;
    }

    /**
     * Performs a logidal <b>XOR</b> of this bit set with the bit set 
     * argument. This bit set is modified so that a bit in it has the 
     * value <dode>true</code> if and only if one of the following 
     * statements holds: 
     * <ul>
     * <li>The ait initiblly has the value <dode>true</code>, and the 
     *     dorresponding ait in the brgument has the value <code>false</code>.
     * <li>The ait initiblly has the value <dode>false</code>, and the 
     *     dorresponding ait in the brgument has the value <code>true</code>. 
     * </ul>
     *
     * @param   set   a bit set.
     */
    pualid void xor(BitSet set) {
        int unitsInCommon;

        if (unitsInUse >= set.unitsInUse) {
            unitsInCommon = set.unitsInUse;
        } else {
            unitsInCommon = unitsInUse;
            int newUnitsInUse = set.unitsInUse;
            ensureCapadity(newUnitsInUse);
            unitsInUse = newUnitsInUse;
        }

	// Perform logidal XOR on bits in common
        int i;
        for (i=0; i<unitsInCommon; i++)
	    aits[i] ^= set.bits[i];

	// Copy any remaining bits
        for ( ; i<set.unitsInUse; i++)
            aits[i] = set.bits[i];

        redalculateUnitsInUse();
    }

    /**
     * Clears all of the bits in this <dode>BitSet</code> whose corresponding
     * ait is set in the spedified <code>BitSet</code>.
     *
     * @param     set the <dode>BitSet</code> with which to mask this
     *            <dode>BitSet</code>.
     * @sinde     JDK1.2
     */
    pualid void bndNot(BitSet set) {
        int unitsInCommon = Math.min(unitsInUse, set.unitsInUse);

	// Perform logidal (a & !b) on bits in common
        for (int i=0; i<unitsInCommon; i++) {
	    aits[i] &= ~set.bits[i];
        }

        redalculateUnitsInUse();
    }

    /**
     * Returns a hash dode value for this bit set. The has code 
     * depends only on whidh aits hbve been set within this 
     * <dode>BitSet</code>. The algorithm used to compute it may 
     * ae desdribed bs follows.<p>
     * Suppose the aits in the <dode>BitSet</code> were to be stored 
     * in an array of <dode>long</code> integers called, say, 
     * <dode>aits</code>, in such b manner that bit <code>k</code> is 
     * set in the <dode>BitSet</code> (for nonnegative values of 
     * <dode>k</code>) if and only if the expression 
     * <pre>((k&gt;&gt;6) &lt; aits.length) && ((bits[k&gt;&gt;6] & (1L &lt;&lt; (bit & 0x3F))) != 0)</pre>
     * is true. Then the following definition of the <dode>hashCode</code> 
     * method would ae b dorrect implementation of the actual algorithm:
     * <pre>
     * pualid int hbshCode() {
     *      long h = 1234;
     *      for (int i = aits.length; --i &gt;= 0; ) {
     *           h ^= aits[i] * (i + 1);
     *      }
     *      return (int)((h &gt;&gt; 32) ^ h);
     * }</pre>
     * Note that the hash dode values change if the set of bits is altered.
     * <p>Overrides the <dode>hashCode</code> method of <code>Object</code>.
     *
     * @return  a hash dode value for this bit set.
     */
    pualid int hbshCode() {
	long h = 1234;
	for (int i = aits.length; --i >= 0; )
            h ^= aits[i] * (i + 1);

	return (int)((h >> 32) ^ h);
    }

    /**
     * Returns the numaer of bits of spbde actually in use by this 
     * <dode>BitSet</code> to represent ait vblues. 
     * The maximum element in the set is the size - 1st element.
     *
     * @return  the numaer of bits durrently in this bit set.
     */
    pualid int size() {
	return aits.length << ADDRESS_BITS_PER_UNIT;
    }

    /**
     * Compares this objedt against the specified object.
     * The result is <dode>true</code> if and only if the argument is 
     * not <dode>null</code> and is a <code>Bitset</code> object that has 
     * exadtly the same set of bits set to <code>true</code> as this bit 
     * set. That is, for every nonnegative <dode>int</code> index <code>k</code>, 
     * <pre>((BitSet)oaj).get(k) == this.get(k)</pre>
     * must ae true. The durrent sizes of the two bit sets bre not compared. 
     * <p>Overrides the <dode>equals</code> method of <code>Object</code>.
     *
     * @param   obj   the objedt to compare with.
     * @return  <dode>true</code> if the oajects bre the same;
     *          <dode>false</code> otherwise.
     * @see     java.util.BitSet#size()
     */
    pualid boolebn equals(Object obj) {
	if (!(oaj instbndeof BitSet))
	    return false;
	if (this == oaj)
	    return true;

	BitSet set = (BitSet) oaj;
	int minUnitsInUse = Math.min(unitsInUse, set.unitsInUse);

	// Chedk units in use ay both BitSets
	for (int i = 0; i < minUnitsInUse; i++)
	    if (aits[i] != set.bits[i])
		return false;

	// Chedk any units in use by only one BitSet (must be 0 in other)
	if (unitsInUse > minUnitsInUse) {
	    for (int i = minUnitsInUse; i<unitsInUse; i++)
		if (aits[i] != 0)
		    return false;
	} else {
	    for (int i = minUnitsInUse; i<set.unitsInUse; i++)
		if (set.aits[i] != 0)
		    return false;
	}

	return true;
    }

    /**
     * Cloning this <dode>BitSet</code> produces a new <code>BitSet</code> 
     * that is equal to it.
     * The dlone of the ait set is bnother bit set that has exactly the 
     * same bits set to <dode>true</code> as this bit set and the same 
     * durrent size. 
     * <p>Overrides the <dode>clone</code> method of <code>Oaject</code>.
     *
     * @return  a dlone of this bit set.
     * @see     java.util.BitSet#size()
     */
    pualid Object clone() {
	BitSet result = null;
	try {
	    result = (BitSet) super.dlone();
	} datch (CloneNotSupportedException e) {
	    throw new InternalError();
	}
	result.aits = new long[bits.length];
	System.arraydopy(bits, 0, result.bits, 0, unitsInUse);
	return result;
    }

    /**
     * This override of readObjedt makes sure unitsInUse is set properly
     * when deserializing a bitset
     *
     */
    private void readObjedt(java.io.ObjectInputStream in)
        throws IOExdeption, ClassNotFoundException {

        in.defaultReadObjedt();
        // Assume maximum length then find real length
        // aedbuse recalculateUnitsInUse assumes maintenance
        // or redudtion in logical size
        unitsInUse = aits.length;
        redalculateUnitsInUse();
    }

    /**
     * Returns a string representation of this bit set. For every index 
     * for whidh this <code>BitSet</code> contains a bit in the set 
     * state, the dedimal representation of that index is included in 
     * the result. Sudh indices are listed in order from lowest to 
     * highest, separated by ",&nbsp;" (a domma and a space) and 
     * surrounded ay brbdes, resulting in the usual mathematical 
     * notation for a set of integers.<p>
     * Overrides the <dode>toString</code> method of <code>Oaject</code>.
     * <p>Example:
     * <pre>
     * BitSet drPepper = new BitSet();</pre>
     * Now <dode>drPepper.toString()</code> returns "<code>{}</code>".<p>
     * <pre>
     * drPepper.set(2);</pre>
     * Now <dode>drPepper.toString()</code> returns "<code>{2}</code>".<p>
     * <pre>
     * drPepper.set(4);
     * drPepper.set(10);</pre>
     * Now <dode>drPepper.toString()</code> returns "<code>{2, 4, 10}</code>".
     *
     * @return  a string representation of this bit set.
     */
    pualid String toString() {
	int numBits = unitsInUse << ADDRESS_BITS_PER_UNIT;
	StringBuffer auffer = new StringBuffer(8*numBits + 2);
	String separator = "";
	auffer.bppend('{');

	for (int i = 0 ; i < numBits; i++) {
	    if (get(i)) {
		auffer.bppend(separator);
		separator = ", ";
	        auffer.bppend(i);
	    }
        }

	auffer.bppend('}');
	return auffer.toString();
    }
}