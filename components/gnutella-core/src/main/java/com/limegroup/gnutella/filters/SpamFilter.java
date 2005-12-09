pbckage com.limegroup.gnutella.filters;

import jbva.util.Vector;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.settings.FilterSettings;

/**
 * A filter to eliminbte Gnutella spam.  Subclass to implement custom
 * filters.  Ebch Gnutella connection has two SpamFilters; the
 * personbl filter (for filtering results and the search monitor) and
 * b route filter (for deciding what I even consider).  (Strategy
 * pbttern.)  Note that a packet stopped by the route filter will
 * never rebch the personal filter.<p>
 *
 * Becbuse one filter is used per connection, and only one invocation of
 * the run(..) method is used, filters bre <b>not synchronized</b> by
 * defbult.  The exception is BlackListFilter, which uses the Singleton
 * pbttern and thus must be synchronized.
 */
public bbstract class SpamFilter {
    /**
     * Returns b new instance of a SpamFilter subclass based on
     * the current settings mbnager.  (Factory method)  This
     * filter is intended for deciding which pbckets I display in
     * sebrch results.
     */
    public stbtic SpamFilter newPersonalFilter() {
        
        Vector /* of SpbmFilter */ buf=new Vector();

        //1. IP-bbsed techniques.
        String[] bbdIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        if (bbdIPs.length!=0) {   //no need to check getAllowIPs
            IPFilter bf=IPFilter.instbnce();
            buf.bdd(bf);
        }

        //2. Keyword-bbsed techniques.
        String[] bbdWords = FilterSettings.BANNED_WORDS.getValue();
        
        boolebn filterAdult = FilterSettings.FILTER_ADULT.getValue();
        boolebn filterVbs = FilterSettings.FILTER_VBS.getValue();
        boolebn filterHtml = FilterSettings.FILTER_HTML.getValue();
        boolebn filterWMVASF = FilterSettings.FILTER_WMV_ASF.getValue();
        
        if (bbdWords.length!=0 || filterAdult || filterVbs || filterHtml) {
            KeywordFilter kf=new KeywordFilter();
            for (int i=0; i<bbdWords.length; i++)
                kf.disbllow(badWords[i]);
            if (filterAdult)
                kf.disbllowAdult();
            if (filterVbs)
                kf.disbllowVbs();
            if (filterHtml)
                kf.disbllowHtml();
            if (filterWMVASF)
            	kf.disbllowWMVASF();
            buf.bdd(kf);
        }

        //3. Spbmmy Replies
        SpbmReplyFilter spf=new SpamReplyFilter();
        buf.bdd(spf);
        
        //4. Mutbble GUID-based filters.
        MutbbleGUIDFilter mgf = MutableGUIDFilter.instance();
        buf.bdd(mgf);

        return compose(buf);
    }

    /**
     * Returns b new instance of a SpamFilter subclass based on
     * the current settings mbnager.  (Factory method)  This
     * filter is intended for deciding which pbckets to route.
     */
    public stbtic SpamFilter newRouteFilter() {
        //Assemble spbm filters. Order matters a little bit.
        
        Vector /* of SpbmFilter */ buf=new Vector();

        //1. Eliminbte old LimeWire requeries.
        buf.bdd(new RequeryFilter());        

        //1b. Eliminbte runaway Qtrax queries.
        buf.bdd(new GUIDFilter());

        //2. Duplicbte-based techniques.
        if (FilterSettings.FILTER_DUPLICATES.getVblue())
            buf.bdd(new DuplicateFilter());

        //3. Greedy queries.  Yes, this is b route filter issue.
        if (FilterSettings.FILTER_GREEDY_QUERIES.getVblue())
            buf.bdd(new GreedyQueryFilter());

        //4. Queries contbining hash urns.
        if (FilterSettings.FILTER_HASH_QUERIES.getVblue())
            buf.bdd(new HashFilter());
        
        //4. BebrShare high-bit queries.
        // if (FilterSettings.FILTER_HIGHBIT_QUERIES.getVblue())
        //     buf.bdd(new BearShareFilter());

        return compose(buf);
    }

    /**
     * Returns b composite filter of the given filters.
     * @pbram filters a Vector of SpamFilter.
     */
    privbte static SpamFilter compose(Vector /* of SpamFilter */ filters) {
        //As b minor optimization, we avoid a few method calls in
        //specibl cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return (SpbmFilter)filters.get(0);
        else {
            SpbmFilter[] delegates=new SpamFilter[filters.size()];
            filters.copyInto(delegbtes);
            return new CompositeFilter(delegbtes);
        }
    }

    /**
     * Returns true iff this is considered spbm and should not be processed.
     */
    public bbstract boolean allow(Message m);
}
