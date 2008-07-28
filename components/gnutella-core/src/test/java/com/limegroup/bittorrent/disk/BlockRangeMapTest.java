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

    private void assertBitField(BitField bitField, boolean... fields) {
        for (int i = 0; i < fields.length; i++) {
            Assert.assertEquals(fields[i], bitField.get(i));
        }
    }
}
