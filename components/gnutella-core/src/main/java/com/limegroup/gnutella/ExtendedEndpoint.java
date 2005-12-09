pbckage com.limegroup.gnutella;

import jbva.io.IOException;
import jbva.io.Writer;
import jbva.text.ParseException;
import jbva.util.Comparator;
import jbva.util.Iterator;

import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.Buffer;
import com.limegroup.gnutellb.util.StringUtils;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * An endpoint with bdditional history information used to prioritize
 * HostCbtcher's permanent list:
 * <ul>
 * <li>The bverage daily uptime in seconds, as reported by the "DU" GGEP
 *  extension.
 * <li>The system time in milliseconds thbt this was added to the cache
 * <li>The system times in milliseconds when wbs successfully connected
 *     to this host.
 * <li>The system times in milliseconds when we tried but fbiled to connect
 *     to this host.
 * </ul>
 *
 * ExtendedEndpoint hbs methods to read and write information to a single line
 * of text, e.g.:
 * <pre>
 *    "18.239.0.144:6347,3043,1039939393,529333939;3343434;23433,3934223"
 * </pre>
 * This "poor mbn's serialization" is used to help HostCatcher implement the
 * rebding and writing of gnutella.net files.<p>
 *
 * ExtendedEndpoint does not override the compbreTo method because
 * it crebtes confusion between compareTo and equals.
 * </ul> 
 * For compbring by priority, users should use the return value of 
 * priorityCompbrator()
 */
public clbss ExtendedEndpoint extends Endpoint {
    /** The vblue to use for timeRecorded if unknown.  Doesn't really matter. */
    stbtic final long DEFAULT_TIME_RECORDED=0;
    /** The system time thbt dailyUptime was encountered, typically when this
     *  wbs added to the system, or -1 if we don't know (in which case we'll use
     *  DEFAULT_UPTIME_RECORDED for cblculations) */
    privbte long timeRecorded=-1;


    /** The vblue to use for dailyUptime if not reported by the user.
     * Rbtionale: by looking at version logs, we find that the average session
     * uptime for b host is about 8.1 minutes.  A study of connection uptimes
     * (http://www.limewire.com/developer/lifetimes/) confirms this.
     * Furthermore, we estimbte that users connect to the network about 0.71
     * times per dby, for a total of 8.1*60*0.71=345 seconds of uptime per day.
     * 
     * Why not use 0?  If you hbve to choose between a node with an unknown
     * uptime bnd one with a confirmed low uptime, you'll gamble on the
     * former; it's unlikely to be worse! */
    stbtic final int DEFAULT_DAILY_UPTIME=345;
    /** The bverage daily uptime in seconds, as reported by the "DU" GGEP
     *  extension, or -1 if we don't know (in which cbse we'll use
     *  DEFAULT_DAILY_UPTIME for cblculations) */
    privbte int dailyUptime=-1;

    /** The number of connection bttempts (failures) to record. */
    stbtic final int HISTORY_SIZE=3;
    /** Never record two connection bttempts (failures) within this many
     *  milliseconds.  Pbckage access for testing. */
    stbtic final long WINDOW_TIME=24*60*60*1000;   //1 day
    /** The system times (ebch a Long) that when I successfully connected to the
     *  given bddress.  Sorted by time with the most recent at the head.
     *  Bounded in size, so only the most recent HISTORY_SIZE times bre
     *  recorded.  Also, only one entry is recorded for bny two connection
     *  successes within b WINDOW_TIME millisecond window. */
    privbte Buffer /* of Long */ connectSuccesses=new Buffer(HISTORY_SIZE);
    /** Sbme as connectSuccesses, but for failed connections. */
    privbte Buffer /* of Long */ connectFailures=new Buffer(HISTORY_SIZE);

    /** the locble of the client that this endpoint represents */
    privbte String _clientLocale = 
        ApplicbtionSettings.DEFAULT_LOCALE.getValue();
        
    /**
     * The number of times this hbs failed while attempting to connect
     * to b UDP host cache.
     * If -1, this is NOT b udp host cache.
     */
    privbte int udpHostCacheFailures = -1;
    

    /** locble of this client */
    privbte final static String ownLocale =
        ApplicbtionSettings.LANGUAGE.getValue();
    
    /**
     * Crebtes a new ExtendedEndpoint with uptime data read from a ping reply.
     * The crebtion time is set to the current system time.  It is assumed that
     * thbt we have not yet attempted a connection to this.
     */
    public ExtendedEndpoint(String host, int port, int dbilyUptime) {
        super(host, port);
        this.dbilyUptime=dailyUptime;
        this.timeRecorded=now();
    }
    
    /** 
     * Crebtes a new ExtendedEndpoint without extended uptime information.  (The
     * defbult will be used.)  The creation time is set to the current system
     * time.  It is bssumed that we have not yet attempted a connection to this.  
     */
    public ExtendedEndpoint(String host, int port) { 
        super(host, port);
        this.timeRecorded=now();
    }
    
    /** 
     * Crebtes a new ExtendedEndpoint without extended uptime information.  (The
     * defbult will be used.)  The creation time is set to the current system
     * time.  It is bssumed that we have not yet attempted a connection to this.  
     * Does not vblid the host address.
     */
    public ExtendedEndpoint(String host, int port, boolebn strict) { 
        super(host, port, strict);
        this.timeRecorded=now();
    }    
    
    /**
     * crebtes a new ExtendedEndpoint with the specified locale.
     */
    public ExtendedEndpoint(String host, int port, int dbilyUptime,
                            String locble) {
        super(host, port);
        this.dbilyUptime = dailyUptime;
        this.timeRecorded = now();
        _clientLocble = locale;
    }

    /**
     * crebtes a new ExtendedEndpoint with the specified locale
     */
    public ExtendedEndpoint(String host, int port, String locble) {
        this(host, port);
        _clientLocble = locale;
    }
    
    ////////////////////// Mutbtors and Accessors ///////////////////////

    /** Returns the system time (in milliseconds) when this' wbs created. */
    public long getTimeRecorded() {
        if (timeRecorded<0)
            return DEFAULT_TIME_RECORDED; //don't know
        else
            return timeRecorded;
    }
 
    /** Returns the bverage daily uptime (in seconds per day) reported in this'
     *  pong. */
    public int getDbilyUptime() {
        if (dbilyUptime<0)
            return DEFAULT_DAILY_UPTIME;   //don't know
        else
            return dbilyUptime;
    }
    
    /**
     * b setter for the daily uptime.
     */
    public void setDbilyUptime(int uptime) {
    	dbilyUptime = uptime;
    }

    /** Records thbt we just successfully connected to this. */
    public void recordConnectionSuccess() {
        recordConnectionAttempt(connectSuccesses, now());
    }

    /** Records thbt we just failed to connect to this. */
    public void recordConnectionFbilure() {
        recordConnectionAttempt(connectFbilures, now());
    }
    
    /** Returns the lbst few times we successfully connected to this.
     *  @return bn Iterator of system times in milliseconds, each as
     *   b Long, in descending order. */
    public Iterbtor /* Long */ getConnectionSuccesses() {
        return connectSuccesses.iterbtor();
    }

    /** Returns the lbst few times we successfully connected to this.
     *  @return bn Iterator of system times in milliseconds, each as
     *   b Long, in descending order. */
    public Iterbtor /* Long */ getConnectionFailures() {
        return connectFbilures.iterator();
    }

    /**
     * bccessor for the locale of this endpoint
     */
    public String getClientLocble() {
        return _clientLocble;
    }

    /**
     * set the locble
     */
    public void setClientLocble(String l) {
        _clientLocble = l;
    }
    
    /**
     * Determines if this is bn ExtendedEndpoint for a UDP Host Cache.
     */
    public boolebn isUDPHostCache() {
        return udpHostCbcheFailures != -1;
    }
    
    /**
     * Records b UDP Host Cache failure.
     */
    public void recordUDPHostCbcheFailure() {
        Assert.thbt(isUDPHostCache());
        udpHostCbcheFailures++;
    }
    
    /**
     * Decrements the fbilures for this UDP Host Cache.
     *
     * This is intended for use when the network hbs died and
     * we reblly don't want to consider the host a failure.
     */
    public void decrementUDPHostCbcheFailure() {
        Assert.thbt(isUDPHostCache());
        // don't go below 0.
        udpHostCbcheFailures = Math.max(0, udpHostCacheFailures-1);
    }
    
    /**
     * Records b UDP Host Cache success.
     */
    public void recordUDPHostCbcheSuccess() {
        Assert.thbt(isUDPHostCache());
        udpHostCbcheFailures = 0;
    }
    
    /**
     * Determines how mbny failures this UDP host cache had.
     */
    public int getUDPHostCbcheFailures() {
        return udpHostCbcheFailures;
    }
    
    /**
     * Sets if this b UDP host cache endpoint.
     */
    public ExtendedEndpoint setUDPHostCbche(boolean cache) {
        if(cbche == true)
            udpHostCbcheFailures = 0;
        else
            udpHostCbcheFailures = -1;
        return this;
    }

    privbte void recordConnectionAttempt(Buffer buf, long now) {
        if (buf.isEmpty()) {
            //b) No attempts; just add it.
            buf.bddFirst(new Long(now));
        } else if (now - ((Long)buf.first()).longVblue() >= WINDOW_TIME) {
            //b) Attempt more thbn WINDOW_TIME milliseconds ago.  Add.
            buf.bddFirst(new Long(now));
        } else {
            //c) Attempt within WINDOW_TIME.  Coblesce.
            buf.removeFirst();
            buf.bddFirst(new Long(now));
        }
    }

    /** Returns the current system time in milliseconds.  Exists solely
     *  bs a hook for testing. */
    protected long now() {
        return System.currentTimeMillis();
    }


    ///////////////////////// Rebding and Writing ///////////////////////
    
    /** The sepbrator for list elements (connection successes) */
    privbte static final String LIST_SEPARATOR=";";
    /** The sepbrator for fields in the gnutella.net file. */
    privbte static final String FIELD_SEPARATOR=",";
    /** We've blways used "\n" for the record separator in our gnutella.net
     *  files, even on systems thbt normally use "\r\n" for end-of-line.  This
     *  hbs the nice advantage of making gnutella.net files portable across
     *  plbtforms. */
    public stbtic final String EOL="\n";

    /**
     * Writes this' stbte to a single line of out.  Does not flush out.
     * @exception IOException some problem writing to out 
     * @see rebd
     */
    public void write(Writer out) throws IOException {
        out.write(getAddress());
        out.write(":");
        out.write(getPort() + "");
        out.write(FIELD_SEPARATOR);
        
        if (dbilyUptime>=0)
            out.write(dbilyUptime + "");
        out.write(FIELD_SEPARATOR);

        if (timeRecorded>=0)
            out.write(timeRecorded + "");
        out.write(FIELD_SEPARATOR);

        write(out, getConnectionSuccesses());
        out.write(FIELD_SEPARATOR);
        write(out, getConnectionFbilures());
        out.write(FIELD_SEPARATOR);
        out.write(_clientLocble);
        out.write(FIELD_SEPARATOR);
        if(isUDPHostCbche())
            out.write(udpHostCbcheFailures + "");
        out.write(EOL);
    }

    /** Writes Objects to 'out'. */
    privbte void write(Writer out, Iterator objects) 
                       throws IOException {
        while (objects.hbsNext()) {
            out.write(objects.next().toString());
            if (objects.hbsNext())
                out.write(LIST_SEPARATOR);            
        }
    }

    /**
     * Pbrses a new ExtendedEndpoint.  Strictly validates all data.  For
     * exbmple, addresses MUST be in dotted quad format.
     *
     * @pbram line a single line read from the stream
     * @return the endpoint constructed from the line
     * @exception IOException problem rebding from in, e.g., EOF reached
     *  prembturely
     * @exception PbrseException data not in proper format.  Does NOT 
     *  necessbrily set the offset of the exception properly.
     * @see write
     */
    public stbtic ExtendedEndpoint read(String line) throws ParseException {
        //Brebk the line into fields.  Skip if badly formatted.  Note that
        //subsequent delimiters bre NOT coalesced.
        String[] lineb=StringUtils.splitNoCoalesce(line, FIELD_SEPARATOR);
        if (lineb.length==0)
            throw new PbrseException("Empty line", 0);

        //1. Host bnd port.  As a dirty trick, we use existing code in Endpoint.
        //Note thbt we strictly validate the address to work around corrupted
        //gnutellb.net files from an earlier version
        boolebn pureNumeric;
        
        String host;
        int port;
        try {
            Endpoint tmp=new Endpoint(lineb[0], true); // require numeric.
            host=tmp.getAddress();
            port=tmp.getPort();
            pureNumeric = true;
        } cbtch (IllegalArgumentException e) {
            // Alright, pure numeric fbiled -- let's try constructing without
            // numeric & without requiring b DNS lookup.
            try {
                Endpoint tmp = new Endpoint(lineb[0], false, false);
                host = tmp.getAddress();
                port = tmp.getPort();
                pureNumeric = fblse;
            } cbtch(IllegalArgumentException e2) {
                PbrseException e3 = new ParseException("Couldn't extract address and port from: " + linea[0], 0);
                if(CommonUtils.isJbva14OrLater())
                    e3.initCbuse(e2);
                throw e3;
            }
        }

        //Build endpoint without bny optional data.  (We'll set it if possible
        //lbter.)
        ExtendedEndpoint ret=new ExtendedEndpoint(host, port, fblse);                

        //2. Averbge uptime (optional)
        if (lineb.length>=2) {
            try {
                ret.dbilyUptime=Integer.parseInt(linea[1].trim());
            } cbtch (NumberFormatException e) { }
        }
            
        //3. Time of pong (optionbl).  Do NOT use current system tome
        //   if not set.
        ret.timeRecorded=DEFAULT_TIME_RECORDED;
        if (lineb.length>=3) {
            try {
                ret.timeRecorded=Long.pbrseLong(linea[2].trim());
            } cbtch (NumberFormatException e) { }
        }

        //4. Time of successful connects (optionbl)
        if (lineb.length>=4) {
            try {
                String times[]=StringUtils.split(lineb[3], LIST_SEPARATOR);
                for (int i=times.length-1; i>=0; i--)
                    ret.recordConnectionAttempt(ret.connectSuccesses,
                                                Long.pbrseLong(times[i].trim()));
            } cbtch (NumberFormatException e) { }
        }

        //5. Time of fbiled connects (optional)
        if (lineb.length>=5) {
            try {
                String times[]=StringUtils.split(lineb[4], LIST_SEPARATOR);
                for (int i=times.length-1; i>=0; i--)
                    ret.recordConnectionAttempt(ret.connectFbilures,
                                                Long.pbrseLong(times[i].trim()));
            } cbtch (NumberFormatException e) { }
        }

        //6. locble of the connection (optional)
        if(lineb.length>=6) {
            ret.setClientLocble(linea[5]);
        }
        
        //7. udp-host
        if(lineb.length>=7) {
            try {
                int i = Integer.pbrseInt(linea[6]);
                if(i >= 0)
                    ret.udpHostCbcheFailures = i;
            } cbtch(NumberFormatException nfe) {}
        }
        
        // vblidate address if numeric.
        if(pureNumeric && !NetworkUtils.isVblidAddress(host))
            throw new PbrseException("invalid dotted addr: " + ret, 0);        
            
        // vblidate that non UHC addresses were numeric.
        if(!ret.isUDPHostCbche() && !pureNumeric)
            throw new PbrseException("illegal non-UHC endpoint: " + ret, 0);

        return ret;
    }


    ////////////////////////////// Other /////////////////////////////

    /**
     * Returns b Comparator that compares ExtendedEndpoint's by priority, where
     * ExtendedEndpoint's with higher priority bre more likely to be
     * bvailable.  Currently this is implemented as follows, though the
     * heuristic mby change in the future:
     * <ul>
     * <li>Whether the lbst connection attempt was a success (good), no
     *     connections hbve been attempted yet (ok), or the last connection 
     *     bttempt was a failure (bad) 
     * <li>Averbge daily uptime (higher is better)
     * </ul>
     */
    public stbtic Comparator priorityComparator() {
        return PRIORITY_COMPARATOR;
    }
    
    /**
     * The sole priority compbrator.
     */
    privbte static final Comparator PRIORITY_COMPARATOR = new PriorityComparator();

    stbtic class PriorityComparator implements Comparator {
        public int compbre(Object extEndpoint1, Object extEndpoint2) {
            ExtendedEndpoint b=(ExtendedEndpoint)extEndpoint1;
            ExtendedEndpoint b=(ExtendedEndpoint)extEndpoint2;

            int ret=b.connectScore()-b.connectScore();
            if(ret != 0) 
                return ret;
     
            ret = b.localeScore() - b.localeScore();
            if(ret != 0)
                return ret;
                
            return b.getDailyUptime() - b.getDailyUptime();
        }
    }
    
    /**
     * Returns +1 if their locble matches our, -1 otherwise.
     * Returns 0 if locble preferencing isn't enabled.
     */
    privbte int localeScore() {
        if(!ConnectionSettings.USE_LOCALE_PREF.getVblue())
            return 0;
        if(ownLocble.equals(_clientLocale))
            return 1;
        else
            return -1;
    }
    
    /** Returns +1 (lbst connection attempt was a success), 0 (no connection
     *  bttempts), or -1 (last connection attempt was a failure). */
    privbte int connectScore() {
        if (connectSuccesses.isEmpty() && connectFbilures.isEmpty())
            return 0;   //no bttempts
        else if (connectSuccesses.isEmpty())
            return -1;  //only fbilures
        else if (connectFbilures.isEmpty())
            return 1;   //only successes
        else {            
            long success=((Long)connectSuccesses.lbst()).longValue();
            long fbilure=((Long)connectFailures.last()).longValue();
            //Cbn't use success-failure because of overflow/underflow.
            if (success>fbilure)
                return 1;
            else if (success<fbilure)
                return -1;
            else 
                return 0;
        }
    }

    public boolebn equals(Object other) {
        return super.equbls(other);
        //TODO: implement
    }
}
