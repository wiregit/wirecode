package com.limegroup.gnutella.filters;

import java.util.Vector;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.FilterSettings;

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
        
        Vector<SpamFilter> buf=new Vector<SpamFilter>();

        //1. IP-based techniques.
        IPFilter ipFilter = ProviderHacks.getIpFilter();
        if(ipFilter.hasBlacklistedHosts())
            buf.add(ipFilter);

        //2. Keyword-based techniques.
        String[] badWords = FilterSettings.BANNED_WORDS.getValue();
        
        boolean filterAdult = FilterSettings.FILTER_ADULT.getValue();
        boolean filterVbs = FilterSettings.FILTER_VBS.getValue();
        boolean filterHtml = FilterSettings.FILTER_HTML.getValue();
        boolean filterWMVASF = FilterSettings.FILTER_WMV_ASF.getValue();
        
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
            if (filterWMVASF)
            	kf.disallowWMVASF();
            buf.add(kf);
        }

        //3. Spammy Replies
        SpamReplyFilter spf=new SpamReplyFilter();
        buf.add(spf);
        
        //4. Mutable GUID-based filters.
        MutableGUIDFilter mgf = MutableGUIDFilter.instance();
        buf.add(mgf);

        return compose(buf);
    }

    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets to route.
     */
    public static SpamFilter newRouteFilter() {
        //Assemble spam filters. Order matters a little bit.
        
        Vector<SpamFilter>  buf=new Vector<SpamFilter> ();

        //1. Eliminate old LimeWire requeries.
        buf.add(new RequeryFilter());        

        //1b. Eliminate runaway Qtrax queries.
        buf.add(new GUIDFilter());

        //2. Duplicate-based techniques.
        if (FilterSettings.FILTER_DUPLICATES.getValue())
            buf.add(new DuplicateFilter());

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (FilterSettings.FILTER_GREEDY_QUERIES.getValue())
            buf.add(new GreedyQueryFilter());

        //4. Queries containing hash urns.
        if (FilterSettings.FILTER_HASH_QUERIES.getValue())
            buf.add(new HashFilter());
        
        //4. BearShare high-bit queries.
        // if (FilterSettings.FILTER_HIGHBIT_QUERIES.getValue())
        //     buf.add(new BearShareFilter());
        
        // always filter hostiles
        buf.add(ProviderHacks.getHostileFilter());

        return compose(buf);
    }

    /**
     * Returns a composite filter of the given filters.
     * @param filters a Vector of SpamFilter.
     */
    private static SpamFilter compose(Vector<? extends SpamFilter>  filters) {
        //As a minor optimization, we avoid a few method calls in
        //special cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return filters.get(0);
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
