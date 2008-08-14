package org.limewire.collection;

import junit.framework.Assert;

import org.limewire.util.BaseTestCase;

public class AndViewTest extends BaseTestCase {

    public AndViewTest(String name) {
        super(name);
    }

    /**
     * 
     * <pre>
     * Output: bf1: 1100 
     *         bf2: 1010 
     *         av:  1000
     * </pre>
     */
    public void testBasic() {
        BitSet bitSet1 = new BitSet(4);
        bitSet1.flip(0);
        bitSet1.flip(1);
        BitSet bitSet2 = new BitSet(4);
        bitSet2.flip(0);
        bitSet2.flip(2);

        BitField bitField1 = new BitFieldSet(bitSet1, 4);
        BitField bitField2 = new BitFieldSet(bitSet2, 4);

        AndView andView = new AndView(bitField1, bitField2);
        Assert.assertEquals(true, andView.get(0));
        Assert.assertEquals(false, andView.get(1));
        Assert.assertEquals(false, andView.get(2));
        Assert.assertEquals(false, andView.get(3));
        
        Assert.assertEquals(0, andView.nextSetBit(0));
        Assert.assertEquals(-1, andView.nextSetBit(1));
        Assert.assertEquals(-1, andView.nextSetBit(2));
        Assert.assertEquals(-1, andView.nextSetBit(3));
        
        Assert.assertEquals(1, andView.nextClearBit(0));
        Assert.assertEquals(1, andView.nextClearBit(1));
        Assert.assertEquals(2, andView.nextClearBit(2));
        Assert.assertEquals(3, andView.nextClearBit(3));
    }
  
}
