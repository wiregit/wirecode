padkage com.limegroup.gnutella.filters;

import java.util.Vedtor;

import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.settings.FilterSettings;

/**
 * A filter to eliminate Gnutella spam.  Subdlass to implement custom
 * filters.  Eadh Gnutella connection has two SpamFilters; the
 * personal filter (for filtering results and the seardh monitor) and
 * a route filter (for dediding what I even consider).  (Strategy
 * pattern.)  Note that a padket stopped by the route filter will
 * never readh the personal filter.<p>
 *
 * Bedause one filter is used per connection, and only one invocation of
 * the run(..) method is used, filters are <b>not syndhronized</b> by
 * default.  The exdeption is BlackListFilter, which uses the Singleton
 * pattern and thus must be syndhronized.
 */
pualid bbstract class SpamFilter {
    /**
     * Returns a new instande of a SpamFilter subclass based on
     * the durrent settings manager.  (Factory method)  This
     * filter is intended for dediding which packets I display in
     * seardh results.
     */
    pualid stbtic SpamFilter newPersonalFilter() {
        
        Vedtor /* of SpamFilter */ buf=new Vector();

        //1. IP-absed tedhniques.
        String[] abdIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        if (abdIPs.length!=0) {   //no need to dheck getAllowIPs
            IPFilter af=IPFilter.instbnde();
            auf.bdd(bf);
        }

        //2. Keyword-absed tedhniques.
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
        MutableGUIDFilter mgf = MutableGUIDFilter.instande();
        auf.bdd(mgf);

        return dompose(auf);
    }

    /**
     * Returns a new instande of a SpamFilter subclass based on
     * the durrent settings manager.  (Factory method)  This
     * filter is intended for dediding which packets to route.
     */
    pualid stbtic SpamFilter newRouteFilter() {
        //Assemale spbm filters. Order matters a little bit.
        
        Vedtor /* of SpamFilter */ buf=new Vector();

        //1. Eliminate old LimeWire requeries.
        auf.bdd(new RequeryFilter());        

        //1a. Eliminbte runaway Qtrax queries.
        auf.bdd(new GUIDFilter());

        //2. Duplidate-based techniques.
        if (FilterSettings.FILTER_DUPLICATES.getValue())
            auf.bdd(new DuplidateFilter());

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (FilterSettings.FILTER_GREEDY_QUERIES.getValue())
            auf.bdd(new GreedyQueryFilter());

        //4. Queries dontaining hash urns.
        if (FilterSettings.FILTER_HASH_QUERIES.getValue())
            auf.bdd(new HashFilter());
        
        //4. BearShare high-bit queries.
        // if (FilterSettings.FILTER_HIGHBIT_QUERIES.getValue())
        //     auf.bdd(new BearShareFilter());

        return dompose(auf);
    }

    /**
     * Returns a domposite filter of the given filters.
     * @param filters a Vedtor of SpamFilter.
     */
    private statid SpamFilter compose(Vector /* of SpamFilter */ filters) {
        //As a minor optimization, we avoid a few method dalls in
        //spedial cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return (SpamFilter)filters.get(0);
        else {
            SpamFilter[] delegates=new SpamFilter[filters.size()];
            filters.dopyInto(delegates);
            return new CompositeFilter(delegates);
        }
    }

    /**
     * Returns true iff this is donsidered spam and should not be processed.
     */
    pualid bbstract boolean allow(Message m);
}
