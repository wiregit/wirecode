package com.limegroup.gnutella;

import com.limegroup.gnutella.filters.*;
import java.util.Vector;

/** 
 * A filter to eliminate Gnutella spam.  Each Gnutella connection
 * has its own SpamFilter.  (Strategy pattern.) Subclass to implement
 * custom spam filters.
 */
public abstract class SpamFilter {
    /**
     * Returns a new instance of a SpamFilter subclass based on 
     * the current settings manager.  (Factory method)
     */
    public static SpamFilter newInstance() {
	//Assemble spam filters. Order matters a little bit.
	SettingsManager settings=SettingsManager.instance();
	Vector /* of SpamFilter */ buf=new Vector();

	//IP-based techniques.
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

	//Keyword-based techniques.
	String[] badWords=settings.getBannedWords();
	boolean filterAdult=settings.getFilterAdult();
	if (badWords.length!=0 || filterAdult) {
	    KeywordFilter kf=new KeywordFilter();
	    for (int i=0; i<badWords.length; i++)
		kf.disallow(badWords[i]);
	    if (filterAdult)       
		kf.disallowAdult();
	    buf.add(kf);
	}
	
	//Duplicate-based techniques.
	if (settings.getFilterDuplicates())
	    buf.add(new DuplicateFilter());

	SpamFilter[] delegates=new SpamFilter[buf.size()];	
	buf.copyInto(delegates);
	return new CompositeFilter(delegates);
    }

    /** 
     * Returns true iff this is considered spam and should not be processed. 
     */
    public abstract boolean allow(Message m);
}
