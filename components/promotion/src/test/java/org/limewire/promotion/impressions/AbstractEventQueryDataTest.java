package org.limewire.promotion.impressions;

import java.util.Date;

import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtil;

abstract class AbstractEventQueryDataTest extends BaseTestCase {

    public AbstractEventQueryDataTest(String name) {
        super(name);
    }

    private final String Q = "the query";

    private final long L = 123123123123L;

    private final Date D = new Date(L);

    protected final UserQueryEventData getImpressions(int n) {

        UserQueryEvent e = getUserQueryEvent(n);
        UserQueryEventData data = new UserQueryEventData(e);

        final byte[] expected = new byte[4 + 8 + 1 + n * 24];
        int cur = 0;
        cur = transfer(data.getMillisSinceToday(), expected, cur);
        cur = transfer(D, expected, cur);
        expected[cur++] = (byte) e.getImpressions().size();
        for (Impression imp : e.getImpressions()) {
            cur = transfer(imp, expected, cur);
        }

        assertEquals(data.getQuery(), Q);
        assertEquals(expected, data.getData());

        return data;
    }

    protected final UserQueryEvent getUserQueryEvent(int n) {

        Impression[] imps = new Impression[n];
        for (int i = 0; i < n; i++) {
            imps[i] = new Impression(new PromotionMessageContainer(),
                    (long) (Math.random() * Long.MAX_VALUE), new Date(L + i));
        }

        UserQueryEvent e = new UserQueryEvent(Q, D);
        for (Impression imp : imps) {
            e.addImpression(imp);
        }

        return e;
    }

    private int transfer(Date d, byte[] in, int start) {
        System.arraycopy(date2bytes(d), 0, in, start, 8);
        return start + 8;
    }

    private int transfer(long i, byte[] in, int start) {
        System.arraycopy(ByteUtil.convertToBytes(i, 4), 0, in, start, 4);
        return start + 4;
    }

    private int transfer(Impression i, byte[] in, int start) {
        arraycopy(ByteUtil.convertToBytes(i.getBinderUniqueID(), 8), 0, in, start, 8);
        arraycopy(ByteUtil.convertToBytes(i.getPromo().getUniqueID(), 8), 0, in, start + 8, 8);
        arraycopy(date2bytes(i.getTimeShown()), 0, in, start + 16, 8);
        return start + 24;
    }

    private void arraycopy(byte[] src, int i, byte[] in, int start, int j) {
        System.arraycopy(src, i, in, start, j);
    }

    private byte[] date2bytes(Date d) {
        return ByteUtil.convertToBytes(d.getTime(), 8);
    }
}
