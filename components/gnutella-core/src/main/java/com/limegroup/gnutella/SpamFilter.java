package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.filters.*;
import com.sun.java.util.collections.Vector;

/**
 * A filter to eliminate Gnutella spam.  Subclass to implement custom
 * filters.  Each Gnutella connection has two SpamFilters; the
 * personal filter (for filtering results and the search monitor) and
 * a route filter (for deciding what I even consider).  (Strategy
 * pattern.)  Note that a packet stopped by the route filter will
 * never reach the personal filter.<p>
 *
 * Because one filter is used per connection, and only one invocation of
 * the run(..) method is used, filters are <b>not synchronized</b> by
 * default.  The exception is BlackListFilter, which uses the Singleton
 * pattern and thus must be synchronized.
 */
public abstract class SpamFilter {
    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets I display in
     * search results.
     */
    public static SpamFilter newPersonalFilter() {
        SettingsManager settings=SettingsManager.instance();
        Vector /* of SpamFilter */ buf=new Vector();

        //1. IP-based techniques.
        String[] badIPs=settings.getBannedIps();
        if (badIPs.length!=0) {   //no need to check getAllowIPs
            IPFilter bf=new IPFilter();
            buf.add(bf);
        }

        //2. Keyword-based techniques.
        String[] badWords=settings.getBannedWords();
        boolean filterAdult=settings.getFilterAdult();
        boolean filterVbs=settings.getFilterVbs();
        boolean filterHtml=settings.getFilterHtml();
        if (badWords.length!=0 || filterAdult || filterVbs || filterHtml) {
            KeywordFilter kf=new KeywordFilter();
            for (int i=0; i<badWords.length; i++)
                kf.disallow(badWords[i]);
            if (filterAdult)
                kf.disallowAdult();
            if (filterVbs)
                kf.disallowVbs();
            if (filterHtml)
                kf.disallowHtml();
            buf.add(kf);
        }

        return compose(buf);
    }

    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets to route.
     */
    public static SpamFilter newRouteFilter() {
        //Assemble spam filters. Order matters a little bit.
        SettingsManager settings=SettingsManager.instance();
        Vector /* of SpamFilter */ buf=new Vector();

        //1. Eliminate old LimeWire requeries.
        buf.add(new RequeryFilter());        

        //1b. Eliminate runaway Qtrax queries.
        buf.add(new GUIDFilter());

        //2. Duplicate-based techniques.
        if (settings.getFilterDuplicates())
            buf.add(new DuplicateFilter());

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (settings.getFilterGreedyQueries())
            buf.add(new GreedyQueryFilter());

        //4. BearShare high-bit queries.
        if (settings.getFilterBearShareQueries())
            buf.add(new BearShareFilter());

        return compose(buf);
    }

    /**
     * Returns a composite filter of the given filters.
     * @param filters a Vector of SpamFilter.
     */
    private static SpamFilter compose(Vector /* of SpamFilter */ filters) {
        //As a minor optimization, we avoid a few method calls in
        //special cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return (SpamFilter)filters.get(0);
        else {
            SpamFilter[] delegates=new SpamFilter[filters.size()];
            filters.copyInto(delegates);
            return new CompositeFilter(delegates);
        }
    }

    /**
     * Returns true iff this is considered spam and should not be processed.
     */
    public abstract boolean allow(Message m);
}
