package org.limewire.collection;

import junit.framework.Assert;

import org.limewire.util.BaseTestCase;

public class NorViewTest extends BaseTestCase {

    public NorViewTest(String name) {
        super(name);
    }

    /**
     * 
     * <pre>
     * Output: bf1: 1100 
     *         av:  0011
     * </pre>
     */
    public void testBasic() {
        BitSet bitSet1 = new BitSet(4);
        bitSet1.flip(0);
        bitSet1.flip(1);
  
        BitField bitField1 = new BitFieldSet(bitSet1, 4);
 
        NotView notView = new NotView(bitField1);
        Assert.assertEquals(false, notView.get(0));
        Assert.assertEquals(false, notView.get(1));
        Assert.assertEquals(true, notView.get(2));
        Assert.assertEquals(true, notView.get(3));
        
        Assert.assertEquals(2, notView.nextSetBit(0));
        Assert.assertEquals(2, notView.nextSetBit(1));
        Assert.assertEquals(2, notView.nextSetBit(2));
        Assert.assertEquals(3, notView.nextSetBit(3));
        
        Assert.assertEquals(0, notView.nextClearBit(0));
        Assert.assertEquals(1, notView.nextClearBit(1));
        Assert.assertEquals(-1, notView.nextClearBit(2));
        Assert.assertEquals(-1, notView.nextClearBit(3));
    }
  
}
