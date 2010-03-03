package com.limegroup.gnutella.filters;

import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.limegroup.gnutella.messages.Message;

public class CompositeFilter implements SpamFilter {

    private static Log LOG = LogFactory.getLog(CompositeFilter.class);

    @InspectionPoint("filter hits")
    private static final InspectionHistogram<String> hitCounts =
        new InspectionHistogram<String>();

    SpamFilter[] delegates;

    /**
     * @requires filters not modified while this is in use (rep is exposed!),
     *           filters contains no null elements
     * @effects creates a new spam filter from a number of other filters.
     */
    public CompositeFilter(SpamFilter[] filters) {
        this.delegates = filters;
    }

    public boolean allow(Message m) {
        for(int i = 0; i < delegates.length; i++) {
            if(!delegates[i].allow(m)) {
                String name = delegates[i].getClass().getSimpleName();
                hitCounts.count(name);
                LOG.debugf("{0} blocked {1}", name, m);
                return false;
            }
        }
        return true;
    }
}
