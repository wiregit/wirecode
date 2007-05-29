package org.limewire.collection;

/**
 * Provides an XOR view.
<pre>
    void PrintBitField(BitField bfs, String bfsName){
        System.out.print(bfsName + ": ");
        for(int i = 0; i < bfs.maxSize(); i++){
            int j = 0;
            if(bfs.get(i)){
                j = 1;          
            }
            System.out.print(j);
        }
        System.out.println(""); 
        
    }
    void SampleCodeXorView(){
        BitSet bs1 = new BitSet();
        bs1.set(0);
        bs1.set(1);
        
        BitSet bs2 = new BitSet();
        bs2.set(0);
        bs2.set(2);
        
        BitSet bs3 = new BitSet();
        bs3.set(0);
        bs3.set(3);
        
        BitField bf1 = new BitFieldSet(bs1, 5);
        BitField bf2 = new BitFieldSet(bs2, 5);
               
        PrintBitField(bf1, "bf1");
        PrintBitField(bf2, "bf2");
                       
        XorView xov = new XorView(bf1, bf2);

        System.out.print("xov: ");
 
        for(int i = 0 ; i < xov.maxSize(); i++){
            int j = 0;
            if(xov.get(i))
               j = 1;
            System.out.print(j);
        }
    }
        
    Output:
        bf1: 11000
        bf2: 10100
        xov: 01100

</pre>
 * <p>
 * See <a href="http://en.wikipedia.org/wiki/Exclusive_or">exclusive or</a> 
 * for more information.
 */
public class XorView extends BooleanFunction {

	public XorView(BitField first, BitField... more) {
		super(first, more);
	}

	public boolean get(int i) {
		boolean ret = fields[0].get(i);
		for (int j = 1;j < fields.length;j++) {
			ret ^= fields[j].get(i);
        }
		return ret;
	}

	public int nextClearBit(int startIndex) {
		// not very efficient
		for (int i = startIndex; i < maxSize(); i++) {
			if (!get(i))
				return i;
		}
		return -1;
	}

	public int nextSetBit(int startIndex) {
		// not very efficient
		for (int i = startIndex; i < maxSize(); i++) {
			if (get(i))
				return i;
		}
		return -1;
	}

}
