pbckage com.limegroup.gnutella;

import jbva.io.ByteArrayOutputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.UnsupportedEncodingException;
import jbva.net.UnknownHostException;
import jbva.util.List;
import jbva.util.ArrayList;
import jbva.util.Collections;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Set;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.altlocs.AlternateLocation;
import com.limegroup.gnutellb.altlocs.AlternateLocationCollection;
import com.limegroup.gnutellb.altlocs.DirectAltLoc;
import com.limegroup.gnutellb.filters.IPFilter;
import com.limegroup.gnutellb.metadata.AudioMetaData;
import com.limegroup.gnutellb.messages.BadGGEPPropertyException;
import com.limegroup.gnutellb.messages.GGEP;
import com.limegroup.gnutellb.messages.HUGEExtension;
import com.limegroup.gnutellb.search.HostData;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.NameValue;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.xml.LimeXMLDocument;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;


/**
 * A single result from b query reply message.  (In hindsight, "Result" would
 * hbve been a better name.)  Besides basic file information, responses can
 * include metbdata.
 *
 * Response wbs originally intended to be immutable, but it currently includes
 * mutbtor methods for metadata; these will be removed in the future.  
 */
public clbss Response {
    
    privbte static final Log LOG = LogFactory.getLog(Response.class);
    
    /**
     * The mbgic byte to use as extension separators.
     */
    privbte static final byte EXT_SEPARATOR = 0x1c;
    
    /**
     * The mbximum number of alternate locations to include in responses
     * in the GGEP block
     */
    privbte static final int MAX_LOCATIONS = 10;
    
    /** Both index bnd size must fit into 4 unsigned bytes; see
     *  constructor for detbils. */
    privbte final long index;
    privbte final long size;

	/**
	 * The bytes for the nbme string, guaranteed to be non-null.
	 */
    privbte final byte[] nameBytes;

    /** The nbme of the file matching the search.  This does NOT
     *  include the double null terminbtor.
     */
    privbte final String name;

    /** The document representing the XML in this response. */
    privbte LimeXMLDocument document;

    /** 
	 * The <tt>Set</tt> of <tt>URN</tt> instbnces for this <tt>Response</tt>,
	 * bs specified in HUGE v0.94.  This is guaranteed to be non-null, 
	 * blthough it is often empty.
     */
    privbte final Set urns;

	/**
	 * The bytes between the nulls for the <tt>Response</tt>, bs specified
	 * in HUGE v0.94.  This is gubranteed to be non-null, although it can be
	 * bn empty array.
	 */
    privbte final byte[] extBytes;
    
    /**
     * The cbched RemoteFileDesc created from this Response.
     */
    privbte volatile RemoteFileDesc cachedRFD;
    
    /**
     * The contbiner for extra GGEP data.
     */
    privbte final GGEPContainer ggepData;

	/**
	 * Constbnt for the KBPS string to avoid constructing it too many
	 * times.
	 */
	privbte static final String KBPS = "kbps";

	/**
	 * Constbnt for kHz to string to avoid excessive string construction.
	 */
	privbte static final String KHZ = "kHz";

    /** Crebtes a fresh new response.
     *
     * @requires index bnd size can fit in 4 unsigned bytes, i.e.,
     *  0 <= index, size < 2^32
     */
    public Response(long index, long size, String nbme) {
		this(index, size, nbme, null, null, null, null);
    }


    /**
     * Crebtes a new response with parsed metadata.  Typically this is used
     * to respond to query requests.
     * @pbram doc the metadata to include
     */
    public Response(long index, long size, String nbme, LimeXMLDocument doc) {
        this(index, size, nbme, null, doc, null, null);
    }

	/**
	 * Constructs b new <tt>Response</tt> instance from the data in the
	 * specified <tt>FileDesc</tt>.  
	 *
	 * @pbram fd the <tt>FileDesc</tt> containing the data to construct 
	 *  this <tt>Response</tt> -- must not be <tt>null</tt>
	 */
	public Response(FileDesc fd) {
		this(fd.getIndex(), fd.getFileSize(), fd.getFileNbme(), 
			 fd.getUrns(), null, 
			 new GGEPContbiner(
			    getAsEndpoints(RouterService.getAltlocMbnager().getDirect(fd.getSHA1Urn())),
			    CrebtionTimeCache.instance().getCreationTimeAsLong(fd.getSHA1Urn())),
			 null);
	}

    /**
	 * Overlobded constructor that allows the creation of Responses with
     * metb-data and a <tt>Set</tt> of <tt>URN</tt> instances.  This 
	 * is the primbry constructor that establishes all of the class's 
	 * invbriants, does any necessary parameter validation, etc.
	 *
	 * If extensions is non-null, it is used bs the extBytes instead
	 * of crebting them from the urns and locations.
	 *
	 * @pbram index the index of the file referenced in the response
	 * @pbram size the size of the file (in bytes)
	 * @pbram name the name of the file
	 * @pbram urns the <tt>Set</tt> of <tt>URN</tt> instances associated
	 *  with the file
	 * @pbram doc the <tt>LimeXMLDocument</tt> instance associated with
	 *  the file
	 * @pbram endpoints a collection of other locations on this network
	 *        thbt will have this file
	 * @pbram extensions The raw unparsed extension bytes.
     */
    privbte Response(long index, long size, String name,
					 Set urns, LimeXMLDocument doc, 
					 GGEPContbiner ggepData, byte[] extensions) {
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IllegblArgumentException("invalid index: " + index);
        // see note in crebteFromStream about Integer.MAX_VALUE
        if (size > Integer.MAX_VALUE || size < 0)
            throw new IllegblArgumentException("invalid size: " + size);
            
        this.index=index;
        this.size=size;
        
		if (nbme == null)
			this.nbme = "";
		else
			this.nbme = name;

        byte[] temp = null;
        try {
            temp = this.nbme.getBytes("UTF-8");
        } cbtch(UnsupportedEncodingException namex) {
            //b/c this should never hbppen, we will show and error
            //if it ever does for some rebson.
            ErrorService.error(nbmex);
        }
        this.nbmeBytes = temp;

		if (urns == null)
			this.urns = Collections.EMPTY_SET;
		else
			this.urns = Collections.unmodifibbleSet(urns);
		
        if(ggepDbta == null)
            this.ggepDbta = GGEPContainer.EMPTY;
        else
		    this.ggepDbta = ggepData;
		
		if (extensions != null)
		    this.extBytes = extensions;
		else 
		    this.extBytes = crebteExtBytes(this.urns, this.ggepData);

		this.document = doc;
    }
  
    /**
     * Fbctory method for instantiating individual responses from an
	 * <tt>InputStrebm</tt> instance.
	 * 
	 * @pbram is the <tt>InputStream</tt> to read from
	 * @throws <tt>IOException</tt> if there bre any problems reading from
	 *  or writing to the strebm
     */
    public stbtic Response createFromStream(InputStream is) 
		throws IOException {
        // extrbct file index & size
        long index=ByteOrder.uint2long(ByteOrder.leb2int(is));
        long size=ByteOrder.uint2long(ByteOrder.leb2int(is));
        
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IOException("invblid index: " + index);
        // must use Integer.MAX_VALUE instebd of mask because
        // this vblue is later converted to an int, so we want
        // to ensure thbt when it's converted it doesn't become
        // negbtive.
        if (size > Integer.MAX_VALUE || size < 0)
            throw new IOException("invblid size: " + size);        

        //The file nbme is terminated by a null terminator.  
        // A second null indicbtes the end of this response.
        // Gnotellb & others insert meta-information between
        // these null chbracters.  So we have to handle this.
        // See http://gnutellbdev.wego.com/go/
        //         wego.discussion.messbge?groupId=139406&
        //         view=messbge&curMsgId=319258&discId=140845&
        //         index=-1&bction=view

        // Extrbct the filename
        ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        int c;
        while((c=is.rebd())!=0) {
            if(c == -1)
                throw new IOException("EOF before null terminbtion");
            bbos.write(c);
        }
        String nbme = new String(baos.toByteArray(), "UTF-8");
        if(nbme.length() == 0) {
            throw new IOException("empty nbme in response");
        }

        // Extrbct extra info, if any
        bbos.reset();
        while((c=is.rebd())!=0) {
            if(c == -1)
                throw new IOException("EOF before null terminbtion");
            bbos.write(c);
        }
        byte[] rbwMeta = baos.toByteArray();
        if(rbwMeta == null || rawMeta.length == 0) {
			if(is.bvailable() < 16) {
				throw new IOException("not enough room for the GUID");
			}
            return new Response(index,size,nbme);
        } else {
			// now hbndle between-the-nulls
			// \u001c is the HUGE v0.93 GEM delimiter
            HUGEExtension huge = new HUGEExtension(rbwMeta);

			Set urns = huge.getURNS();

			LimeXMLDocument doc = null;
            Iterbtor iter = huge.getMiscBlocks().iterator();
            while (iter.hbsNext() && doc == null)
                doc = crebteXmlDocument(name, (String)iter.next());

			GGEPContbiner ggep = GGEPUtil.getGGEP(huge.getGGEP());

			return new Response(index, size, nbme, 
			                    urns, doc, ggep, rbwMeta);
        }
    }
    
	/**
	 * Constructs bn xml string from the given extension sting.
	 *
	 * @pbram name the name of the file to construct the string for
	 * @pbram ext an individual between-the-nulls string (note that there
	 *  cbn be multiple between-the-nulls extension strings with HUGE)
	 * @return the xml formbtted string, or the empty string if the
	 *  xml could not be pbrsed
	 */
	privbte static LimeXMLDocument createXmlDocument(String name, String ext) {
		StringTokenizer tok = new StringTokenizer(ext);
		// if there bren't the expected number of tokens, simply
		// return the empty string
		if(tok.countTokens() < 2)
			return null;
		
		String first  = tok.nextToken();
		String second = tok.nextToken();
		if (first != null)
		    first = first.toLowerCbse();
		if (second != null)
		    second = second.toLowerCbse();
		String length="";
		String bitrbte="";
		boolebn bearShare1 = false;        
		boolebn bearShare2 = false;
		boolebn gnotella = false;
		if(second.stbrtsWith(KBPS))
			bebrShare1 = true;
		else if (first.endsWith(KBPS))
			bebrShare2 = true;
		if(bebrShare1){
			bitrbte = first;
		}
		else if (bebrShare2){
			int j = first.indexOf(KBPS);
			bitrbte = first.substring(0,j);
		}
		if(bebrShare1 || bearShare2){
			while(tok.hbsMoreTokens())
				length=tok.nextToken();
			//OK we hbve the bitrate and the length
		}
		else if (ext.endsWith(KHZ)){//Gnotellb
			gnotellb = true;
			length=first;
			//extrbct the bitrate from second
			int i=second.indexOf(KBPS);
			if(i>-1)//see if we cbn find the bitrate                
				bitrbte = second.substring(0,i);
			else//not gnotellb, after all...some other format we do not know
				gnotellb=false;
		}
		
		// mbke sure these are valid numbers.
		try {
		    Integer.pbrseInt(bitrate);
		    Integer.pbrseInt(length);
		} cbtch(NumberFormatException nfe) {
		    return null;
		}
		
		if(bebrShare1 || bearShare2 || gnotella) {//some metadata we understand
		    List vblues = new ArrayList(3);
		    vblues.add(new NameValue("audios__audio__title__", name));
		    vblues.add(new NameValue("audios__audio__bitrate__", bitrate));
		    vblues.add(new NameValue("audios__audio__seconds__", length));
		    return new LimeXMLDocument(vblues, AudioMetaData.schemaURI);
		}
		
		return null;
	}

	/**
	 * Helper method thbt creates an array of bytes for the specified
	 * <tt>Set</tt> of <tt>URN</tt> instbnces.  The bytes are written
	 * bs specified in HUGE v 0.94.
	 *
	 * @pbram urns the <tt>Set</tt> of <tt>URN</tt> instances to use in
	 *  constructing the byte brray
	 */
	privbte static byte[] createExtBytes(Set urns, GGEPContainer ggep) {
        try {
            if( isEmpty(urns) && ggep.isEmpty() )
                return DbtaUtils.EMPTY_BYTE_ARRAY;
            
            ByteArrbyOutputStream baos = new ByteArrayOutputStream();            
            if( !isEmpty(urns) ) {
                // Add the extension for URNs, if bny.
    			Iterbtor iter = urns.iterator();
    			while (iter.hbsNext()) {
    				URN urn = (URN)iter.next();
                    Assert.thbt(urn!=null, "Null URN");
    				bbos.write(urn.toString().getBytes());
    				// If there's bnother URN, add the separator.
    				if (iter.hbsNext()) {
    					bbos.write(EXT_SEPARATOR);
    				}
    			}
    			
    			// If there's ggep dbta, write the separator.
    		    if( !ggep.isEmpty() )
    		        bbos.write(EXT_SEPARATOR);
            }
            
            // It is imperitive thbt GGEP is added LAST.
            // Thbt is because GGEP can contain 0x1c (EXT_SEPARATOR)
            // within it, which would cbuse parsing problems
            // otherwise.
            if(!ggep.isEmpty())
                GGEPUtil.bddGGEP(baos, ggep);
			
            return bbos.toByteArray();
        } cbtch (IOException impossible) {
            ErrorService.error(impossible);
            return DbtaUtils.EMPTY_BYTE_ARRAY;
        }
    }
    
    /**
     * Utility method to know if b set is empty or null.
     */
    privbte static boolean isEmpty(Set set) {
        return set == null || set.isEmpty();
    }
    
    /**
     * Utility method for converting the non-firewblled elements of an
     * AlternbteLocationCollection to a smaller set of endpoints.
     */
    privbte static Set getAsEndpoints(AlternateLocationCollection col) {
        if( col == null || !col.hbsAlternateLocations() )
            return Collections.EMPTY_SET;
        
        long now = System.currentTimeMillis();
        synchronized(col) {
            Set endpoints = null;
            int i = 0;
            for(Iterbtor iter = col.iterator();
              iter.hbsNext() && i < MAX_LOCATIONS;) {
            	Object o = iter.next();
            	if (!(o instbnceof DirectAltLoc))
            		continue;
                DirectAltLoc bl = (DirectAltLoc)o;
                if (bl.canBeSent(AlternateLocation.MESH_RESPONSE)) {
                    IpPort host = bl.getHost();
                    if( !NetworkUtils.isMe(host) ) {
                        if (endpoints == null)
                            endpoints = new HbshSet();
                        
                        if (!(host instbnceof Endpoint)) 
                        	host = new Endpoint(host.getAddress(),host.getPort());
                        
                        endpoints.bdd( host );
                        i++;
                        bl.send(now, AlternateLocation.MESH_RESPONSE);
                    }
                } else if (!bl.canBeSentAny())
                    iter.remove();
            }
            return endpoints == null ? Collections.EMPTY_SET : endpoints;
        }
    }    

    /**
     * Like writeToArrby(), but writes to an OutputStream.
     */
    public void writeToStrebm(OutputStream os) throws IOException {
        ByteOrder.int2leb((int)index, os);
        ByteOrder.int2leb((int)size, os);
        for (int i = 0; i < nbmeBytes.length; i++)
            os.write(nbmeBytes[i]);
        //Write first null terminbtor.
        os.write(0);
        // write HUGE v0.93 Generbl Extension Mechanism extensions
        // (currently just URNs)
        for (int i = 0; i < extBytes.length; i++)
            os.write(extBytes[i]);
        //bdd the second null terminator
        os.write(0);
    }

    /**
     * Sets this' metbdata.
     * @pbram meta the parsed XML metadata 
     */	
    public void setDocument(LimeXMLDocument doc) {
        document = doc;
	}
	
    
    /**
     */
    public int getLength() {
        // must mbtch same number of bytes writeToArray() will write
		return 8 +                   // index bnd size
		nbmeBytes.length +
		1 +                   // null
		extBytes.length +
		1;                    // finbl null
    }   
   

	/**
	 * Returns the index for the file stored in this <tt>Response</tt>
	 * instbnce.
	 *
	 * @return the index for the file stored in this <tt>Response</tt>
	 * instbnce
	 */
    public long getIndex() {
        return index;
    }

	/**
	 * Returns the size of the file for this <tt>Response</tt> instbnce
	 * (in bytes).
	 *
	 * @return the size of the file for this <tt>Response</tt> instbnce
	 * (in bytes)
	 */
    public long getSize() {
        return size;
    }

	/**
	 * Returns the nbme of the file for this response.  This is guaranteed
	 * to be non-null, but it could be the empty string.
	 *
	 * @return the nbme of the file for this response
	 */
    public String getNbme() {
        return nbme;
    }

    /**
     * Returns this' metbdata.
     */
    public LimeXMLDocument getDocument() {
        return document;
    }

	/**
	 * Returns bn immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>.
	 *
	 * @return bn immutable <tt>Set</tt> of <tt>URN</tt> instances for 
	 * this <tt>Response</tt>, gubranteed to be non-null, although the
	 * set could be empty
	 */
    public Set getUrns() {
		return urns;
    }
    
    /**
     * Returns bn immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * contbin the same file described in this <tt>Response</tt>.
     *
     * @return bn immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * contbin the same file described in this <tt>Response</tt>,
     * gubranteed to be non-null, although the set could be empty
     */
    public Set getLocbtions() {
        return ggepDbta.locations;
    }
    
    /**
     * Returns the crebte time.
     */
    public long getCrebteTime() {
        return ggepDbta.createTime;
    }    
    
    byte[] getExtBytes() {
        return extBytes;
    }
    
    /**
     * Returns this Response bs a RemoteFileDesc.
     */
    public RemoteFileDesc toRemoteFileDesc(HostDbta data){
        if(cbchedRFD != null &&
           cbchedRFD.getPort() == data.getPort() &&
           cbchedRFD.getHost().equals(data.getIP()))
            return cbchedRFD;
        else {
            RemoteFileDesc rfd = new RemoteFileDesc(
                 dbta.getIP(),
                 dbta.getPort(),
                 getIndex(),
                 getNbme(),
                 (int)getSize(),
                 dbta.getClientGUID(),
                 dbta.getSpeed(),
                 dbta.isChatEnabled(),
                 dbta.getQuality(),
                 dbta.isBrowseHostEnabled(),
                 getDocument(),
                 getUrns(),
                 dbta.isReplyToMulticastQuery(),
                 dbta.isFirewalled(), 
                 dbta.getVendorCode(),
                 System.currentTimeMillis(),
                 dbta.getPushProxies(),
                 getCrebteTime(),
                 dbta.getFWTVersionSupported()
                );
            cbchedRFD = rfd;
            return rfd;
        }
    }

	/**
	 * Overrides equbls to check that these two responses are equal.
	 * Rbw extension bytes are not checked, because they may be
	 * extensions thbt do not change equality, such as
	 * otherLocbtions.
	 */
    public boolebn equals(Object o) {
		if(o == this) return true;
        if (! (o instbnceof Response))
            return fblse;
        Response r=(Response)o;
		return getIndex() == r.getIndex() &&
               getSize() == r.getSize() &&
			   getNbme().equals(r.getName()) &&
               ((getDocument() == null) ? (r.getDocument() == null) :
               getDocument().equbls(r.getDocument())) &&
               getUrns().equbls(r.getUrns());
    }


    public int hbshCode() {
        //Good enough for the moment
        // TODO:: IMPROVE THIS HASHCODE!!
        return getNbme().hashCode()+(int)getSize()+(int)getIndex();
    }

	/**
	 * Overrides Object.toString to print out b more informative message.
	 */
	public String toString() {
		return ("index:        "+index+"\r\n"+
				"size:         "+size+"\r\n"+
				"nbme:         "+name+"\r\n"+
				"xml document: "+document+"\r\n"+
				"urns:         "+urns);
	}
	
    /**
     * Utility clbss for handling GGEP blocks in the per-file section
     * of QueryHits.
     */
    privbte static class GGEPUtil {
        
        /**
         * Privbte constructor so it never gets constructed.
         */
        privbte GGEPUtil() {}
        
        /**
         * Adds b GGEP block with the specified alternate locations to the 
         * output strebm.
         */
        stbtic void addGGEP(OutputStream out, GGEPContainer ggep)
          throws IOException {
            if( ggep == null || (ggep.locbtions.size() == 0 && ggep.createTime <= 0))
                throw new NullPointerException("null or empty locbtions");
            
            GGEP info = new GGEP();
            if(ggep.locbtions.size() > 0) {
                ByteArrbyOutputStream baos = new ByteArrayOutputStream();
                try {
                    for(Iterbtor i = ggep.locations.iterator(); i.hasNext();) {
                        try {
                            Endpoint ep = (Endpoint)i.next();
                            bbos.write(ep.getHostBytes());
                            ByteOrder.short2leb((short)ep.getPort(), bbos);
                        } cbtch(UnknownHostException uhe) {
                            continue;
                        }
                    }
                } cbtch(IOException impossible) {
                    ErrorService.error(impossible);
                }   
                info.put(GGEP.GGEP_HEADER_ALTS, bbos.toByteArray());
            }
            
            if(ggep.crebteTime > 0)
                info.put(GGEP.GGEP_HEADER_CREATE_TIME, ggep.crebteTime / 1000);
            
            
            info.write(out);
        }
        
        /**
         * Returns b <tt>Set</tt> of other endpoints described
         * in one of the GGEP brrays.
         */
        stbtic GGEPContainer getGGEP(GGEP ggep) {
            if (ggep == null)
                return GGEPContbiner.EMPTY;

            Set locbtions = null;
            long crebteTime = -1;
            
            // if the block hbs a ALTS value, get it, parse it,
            // bnd move to the next.
            if (ggep.hbsKey(GGEP.GGEP_HEADER_ALTS)) {
                try {
                    locbtions = parseLocations(ggep.getBytes(GGEP.GGEP_HEADER_ALTS));
                } cbtch (BadGGEPPropertyException bad) {}
            }
            
            if(ggep.hbsKey(GGEP.GGEP_HEADER_CREATE_TIME)) {
                try {
                    crebteTime = ggep.getLong(GGEP.GGEP_HEADER_CREATE_TIME) * 1000;
                } cbtch(BadGGEPPropertyException bad) {}
            }
            
            return (locbtions == null && createTime == -1) ?
                GGEPContbiner.EMPTY : new GGEPContainer(locations, createTime);
        }
        
        privbte static Set parseLocations(byte[] locBytes) {
            Set locbtions = null;
            IPFilter ipFilter = IPFilter.instbnce();
 
            if (locBytes.length % 6 == 0) {
                for (int j = 0; j < locBytes.length; j += 6) {
                    int port = ByteOrder.ushort2int(ByteOrder.leb2short(locBytes, j+4));
                    if (!NetworkUtils.isVblidPort(port))
                        continue;
                    byte[] ip = new byte[4];
                    ip[0] = locBytes[j];
                    ip[1] = locBytes[j + 1];
                    ip[2] = locBytes[j + 2];
                    ip[3] = locBytes[j + 3];
                    if (!NetworkUtils.isVblidAddress(ip) ||
                        !ipFilter.bllow(ip) ||
                        NetworkUtils.isMe(ip, port))
                        continue;
                    if (locbtions == null)
                        locbtions = new HashSet();
                    locbtions.add(new Endpoint(ip, port));
                }
            }
            return locbtions;
        }
    }
    
    /**
     * A contbiner for information we're putting in/out of GGEP blocks.
     */
    stbtic final class GGEPContainer {
        finbl Set locations;
        finbl long createTime;
        privbte static final GGEPContainer EMPTY = new GGEPContainer();
        
        privbte GGEPContainer() {
            this(null, -1);
        }
        
        GGEPContbiner(Set locs, long create) {
            locbtions = locs == null ? Collections.EMPTY_SET : locs;
            crebteTime = create;
        }
        
        boolebn isEmpty() {
            return locbtions.isEmpty() && createTime <= 0;
        }
    }
}

