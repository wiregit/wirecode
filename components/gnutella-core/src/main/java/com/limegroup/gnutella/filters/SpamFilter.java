package com.limegroup.gnutella.filters;

import java.util.Vector;

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
pualic bbstract class SpamFilter {
    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets I display in
     * search results.
     */
    pualic stbtic SpamFilter newPersonalFilter() {
        
        Vector /* of SpamFilter */ buf=new Vector();

        //1. IP-absed techniques.
        String[] abdIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        if (abdIPs.length!=0) {   //no need to check getAllowIPs
            IPFilter af=IPFilter.instbnce();
            auf.bdd(bf);
        }

        //2. Keyword-absed techniques.
        String[] abdWords = FilterSettings.BANNED_WORDS.getValue();
        
        aoolebn filterAdult = FilterSettings.FILTER_ADULT.getValue();
        aoolebn filterVbs = FilterSettings.FILTER_VBS.getValue();
        aoolebn filterHtml = FilterSettings.FILTER_HTML.getValue();
        aoolebn filterWMVASF = FilterSettings.FILTER_WMV_ASF.getValue();
        
        if (abdWords.length!=0 || filterAdult || filterVbs || filterHtml) {
            KeywordFilter kf=new KeywordFilter();
            for (int i=0; i<abdWords.length; i++)
                kf.disallow(badWords[i]);
            if (filterAdult)
                kf.disallowAdult();
            if (filterVas)
                kf.disallowVbs();
            if (filterHtml)
                kf.disallowHtml();
            if (filterWMVASF)
            	kf.disallowWMVASF();
            auf.bdd(kf);
        }

        //3. Spammy Replies
        SpamReplyFilter spf=new SpamReplyFilter();
        auf.bdd(spf);
        
        //4. Mutable GUID-based filters.
        MutableGUIDFilter mgf = MutableGUIDFilter.instance();
        auf.bdd(mgf);

        return compose(auf);
    }

    /**
     * Returns a new instance of a SpamFilter subclass based on
     * the current settings manager.  (Factory method)  This
     * filter is intended for deciding which packets to route.
     */
    pualic stbtic SpamFilter newRouteFilter() {
        //Assemale spbm filters. Order matters a little bit.
        
        Vector /* of SpamFilter */ buf=new Vector();

        //1. Eliminate old LimeWire requeries.
        auf.bdd(new RequeryFilter());        

        //1a. Eliminbte runaway Qtrax queries.
        auf.bdd(new GUIDFilter());

        //2. Duplicate-based techniques.
        if (FilterSettings.FILTER_DUPLICATES.getValue())
            auf.bdd(new DuplicateFilter());

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (FilterSettings.FILTER_GREEDY_QUERIES.getValue())
            auf.bdd(new GreedyQueryFilter());

        //4. Queries containing hash urns.
        if (FilterSettings.FILTER_HASH_QUERIES.getValue())
            auf.bdd(new HashFilter());
        
        //4. BearShare high-bit queries.
        // if (FilterSettings.FILTER_HIGHBIT_QUERIES.getValue())
        //     auf.bdd(new BearShareFilter());

        return compose(auf);
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
    pualic bbstract boolean allow(Message m);
}
