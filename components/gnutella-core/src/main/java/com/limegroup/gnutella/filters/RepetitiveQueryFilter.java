package com.limegroup.gnutella.filters;

import org.limewire.core.settings.FilterSettings;
import org.limewire.inspection.InspectablePrimitive;

import com.google.inject.Singleton;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;

@Singleton
public class RepetitiveQueryFilter implements SpamFilter {

    /** Recent incoming queries. */
    private final String[] recentQueries;

    /** The index of the recent query that should be replaced next. */
    private int roundRobin = 0;

    /** The number of repetitive queries dropped this session. */
    @InspectablePrimitive("repetitive queries")
    private long dropped = 0;

    RepetitiveQueryFilter() {
        int size = FilterSettings.REPETITIVE_QUERY_FILTER_SIZE.getValue();
        recentQueries = new String[size];
    }

    @Override
    public boolean allow(Message m) {
        if(!(m instanceof QueryRequest))
            return true;
        if(recentQueries.length == 0)
            return true;
        // Drop repetitive queries
        String query = ((QueryRequest)m).getQuery();
        assert query != null;
        for(String recentQuery : recentQueries) {
            if(query.equals(recentQuery)) {
                dropped++;
                return false;
            }
        }
        recentQueries[roundRobin] = query;
        roundRobin = (roundRobin + 1) % recentQueries.length;
        return true;
    }
}
