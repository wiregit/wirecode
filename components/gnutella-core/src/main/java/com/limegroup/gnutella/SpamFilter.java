package com.limegroup.gnutella;

import com.limegroup.gnutella.filters.*;
import java.util.*;

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
        //Keyword-based techniques.
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
            return kf;
        } else {
            //This is really just a minor optimization; you could also
            //just use an empty KeywordFilter.
            return new AllowFilter();
        }
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

        //1. IP-based techniques.
        String[] badIPs=settings.getBannedIps();
        if (badIPs.length!=0) {
            //BlackListFilter uses the singleton pattern, so it is really
            //not necessary to call this on every connection.  But we are
            //not at all concerned with efficiency here, and it actually
            //works.
            BlackListFilter bf=BlackListFilter.instance();
            synchronized (bf) { //just to be safe
            bf.clear();
            for (int i=0; i<badIPs.length; i++)
                bf.add(badIPs[i]);
            }
            buf.add(bf);
        }

        //2. Duplicate-based techniques.
        if (settings.getFilterDuplicates())
            buf.add(new DuplicateFilter());

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (settings.getFilterGreedyQueries())
            buf.add(new GreedyQueryFilter());

        //4. BearShare high-bit queries.
        if (settings.getFilterBearShareQueries())
            buf.add(new BearShareFilter());

        //As a minor optimization, we avoid a few method calls in
        //special cases.
        if (buf.size()==0)
            return new AllowFilter();
        else if (buf.size()==1)
            return (SpamFilter)buf.get(0);
        else {
            SpamFilter[] delegates=new SpamFilter[buf.size()];
            buf.copyInto(delegates);
            return new CompositeFilter(delegates);
        }
    }

    /**
     * Returns true iff this is considered spam and should not be processed.
     */
    public abstract boolean allow(Message m);
}
