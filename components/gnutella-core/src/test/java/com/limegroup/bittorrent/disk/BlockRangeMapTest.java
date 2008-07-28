package com.limegroup.bittorrent.disk;

import junit.framework.Assert;

import org.limewire.collection.BitField;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTInterval;

public class BlockRangeMapTest extends BaseTestCase {

    public BlockRangeMapTest(String name) {
        super(name);
    }

    public void testBasic() {
        BlockRangeMap blockRangeMap = new BlockRangeMap(10);

        BitField bitField = blockRangeMap.getBitField();
        assertBitField(bitField, false, false, false, false, false);

        BTInterval btInterval1 = new BTInterval(0, 3, 1);
        BTInterval btInterval2 = new BTInterval(5, 6, 4);
        blockRangeMap.addInterval(btInterval1);
        blockRangeMap.addInterval(btInterval2);

        assertBitField(bitField, false, true, false, false, true);

        blockRangeMap.remove(1);

        assertBitField(bitField, false, false, false, false, true);

        blockRangeMap.removeInterval(btInterval2);

        assertBitField(bitField, false, false, false, false, false);
    }

    private void assertBitField(BitField bitField, boolean field0, boolean field1, boolean field2,
            boolean field3, boolean field4) {
        Assert.assertEquals(field0, bitField.get(0));
        Assert.assertEquals(field1, bitField.get(1));
        Assert.assertEquals(field2, bitField.get(2));
        Assert.assertEquals(field3, bitField.get(3));
        Assert.assertEquals(field4, bitField.get(4));
    }
}
