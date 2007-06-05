package org.limewire.collection;

/**
 * Provides a <code>BitField</code> implementation for a {@link BitSet} object.
 * 
 <pre>
    void printBitField(BitField bf, String bfName){
        System.out.print(bfName + ": ");
        for(int i = 0; i < bf.maxSize(); i++){
            int j = 0;
            if(bf.get(i)){
                j = 1;          
            }
            System.out.print(j);
        }
        System.out.println(""); 
    }

    void sampleCodeBitFieldSet(){
        BitSet bs1 = new BitSet();
        bs1.set(1);
        bs1.set(3);
        
        BitSet bs2 = new BitSet();
        bs2.set(2);
        
        BitSet bs3 = new BitSet();
        bs3.set(3);
        
        BitField bf1 = new BitFieldSet(bs1, 16);
        BitField bf2 = new BitFieldSet(bs2, 16);
        BitField bf3 = new BitFieldSet(bs3, 16);

        printBitField(bf1, "bf1");
        printBitField(bf2, "bf2");
        printBitField(bf3, "bf3");

        System.out.println("\nbf1) cardinality: " + bf1.cardinality());
        System.out.println("bf1) Next clear bit is: " + bf1.nextClearBit(2));
        System.out.println("bf1) Next set bit is: " + bf1.nextSetBit(0));

        System.out.println("\nbf2) cardinality: " + bf2.cardinality());
        System.out.println("bf2) Next clear bit is: " + bf2.nextClearBit(2));
        System.out.println("bf2) Next set bit is: " + bf2.nextSetBit(0));

        System.out.println("\nbf3) cardinality: " + bf3.cardinality());
        System.out.println("bf3) Next clear bit is: " + bf3.nextClearBit(2));
        System.out.println("bf3) Next set bit is: " + bf3.nextSetBit(0));        
    }
    Output:
        bf1: 0101000000000000
        bf2: 0010000000000000
        bf3: 0001000000000000
        
        bf1) cardinality: 2
        bf1) Next clear bit is: 2
        bf1) Next set bit is: 1
        
        bf2) cardinality: 1
        bf2) Next clear bit is: 3
        bf2) Next set bit is: 2
        
        bf3) cardinality: 1
        bf3) Next clear bit is: 2
        bf3) Next set bit is: 3
 </pre>
 * 
 */
public class BitFieldSet implements BitField {

	private final int maxSize;
	private final BitSet bs;
	
	/**
	 * Constructs a BitField view over the passed bitset with the
	 * specified size. 
	 */
	public BitFieldSet(BitSet bs, int maxSize) {
		this.bs = bs;
		this.maxSize = maxSize;
	}
	
	public int maxSize() {
		return maxSize;
	}

	public int cardinality() {
		if (bs.length() <= maxSize)
			return bs.cardinality();
		else
			return bs.get(0, maxSize).cardinality(); // expensive, avoid.
	}

	public boolean get(int i) {
		if (i > maxSize)
			throw new IndexOutOfBoundsException();
		return bs.get(i);
	}

	public int nextClearBit(int i) {
		int ret = bs.nextClearBit(i); 
		return ret >= maxSize ? -1 : ret;
	}

	public int nextSetBit(int i) {
		int ret = bs.nextSetBit(i);
		return ret >= maxSize ? -1 : ret;
	}

}
