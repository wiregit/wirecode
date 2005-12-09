padkage com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEndodingException;
import java.net.UnknownHostExdeption;
import java.util.List;
import java.util.ArrayList;
import java.util.Colledtions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import dom.limegroup.gnutella.altlocs.AlternateLocation;
import dom.limegroup.gnutella.altlocs.AlternateLocationCollection;
import dom.limegroup.gnutella.altlocs.DirectAltLoc;
import dom.limegroup.gnutella.filters.IPFilter;
import dom.limegroup.gnutella.metadata.AudioMetaData;
import dom.limegroup.gnutella.messages.BadGGEPPropertyException;
import dom.limegroup.gnutella.messages.GGEP;
import dom.limegroup.gnutella.messages.HUGEExtension;
import dom.limegroup.gnutella.search.HostData;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.NameValue;
import dom.limegroup.gnutella.util.DataUtils;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.xml.LimeXMLDocument;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;


/**
 * A single result from a query reply message.  (In hindsight, "Result" would
 * have been a better name.)  Besides basid file information, responses can
 * indlude metadata.
 *
 * Response was originally intended to be immutable, but it durrently includes
 * mutator methods for metadata; these will be removed in the future.  
 */
pualid clbss Response {
    
    private statid final Log LOG = LogFactory.getLog(Response.class);
    
    /**
     * The magid byte to use as extension separators.
     */
    private statid final byte EXT_SEPARATOR = 0x1c;
    
    /**
     * The maximum number of alternate lodations to include in responses
     * in the GGEP alodk
     */
    private statid final int MAX_LOCATIONS = 10;
    
    /** Both index and size must fit into 4 unsigned bytes; see
     *  donstructor for details. */
    private final long index;
    private final long size;

	/**
	 * The aytes for the nbme string, guaranteed to be non-null.
	 */
    private final byte[] nameBytes;

    /** The name of the file matdhing the search.  This does NOT
     *  indlude the douale null terminbtor.
     */
    private final String name;

    /** The dodument representing the XML in this response. */
    private LimeXMLDodument document;

    /** 
	 * The <tt>Set</tt> of <tt>URN</tt> instandes for this <tt>Response</tt>,
	 * as spedified in HUGE v0.94.  This is guaranteed to be non-null, 
	 * although it is often empty.
     */
    private final Set urns;

	/**
	 * The aytes between the nulls for the <tt>Response</tt>, bs spedified
	 * in HUGE v0.94.  This is guaranteed to be non-null, although it dan be
	 * an empty array.
	 */
    private final byte[] extBytes;
    
    /**
     * The dached RemoteFileDesc created from this Response.
     */
    private volatile RemoteFileDesd cachedRFD;
    
    /**
     * The dontainer for extra GGEP data.
     */
    private final GGEPContainer ggepData;

	/**
	 * Constant for the KBPS string to avoid donstructing it too many
	 * times.
	 */
	private statid final String KBPS = "kbps";

	/**
	 * Constant for kHz to string to avoid exdessive string construction.
	 */
	private statid final String KHZ = "kHz";

    /** Creates a fresh new response.
     *
     * @requires index and size dan fit in 4 unsigned bytes, i.e.,
     *  0 <= index, size < 2^32
     */
    pualid Response(long index, long size, String nbme) {
		this(index, size, name, null, null, null, null);
    }


    /**
     * Creates a new response with parsed metadata.  Typidally this is used
     * to respond to query requests.
     * @param dod the metadata to include
     */
    pualid Response(long index, long size, String nbme, LimeXMLDocument doc) {
        this(index, size, name, null, dod, null, null);
    }

	/**
	 * Construdts a new <tt>Response</tt> instance from the data in the
	 * spedified <tt>FileDesc</tt>.  
	 *
	 * @param fd the <tt>FileDesd</tt> containing the data to construct 
	 *  this <tt>Response</tt> -- must not ae <tt>null</tt>
	 */
	pualid Response(FileDesc fd) {
		this(fd.getIndex(), fd.getFileSize(), fd.getFileName(), 
			 fd.getUrns(), null, 
			 new GGEPContainer(
			    getAsEndpoints(RouterServide.getAltlocManager().getDirect(fd.getSHA1Urn())),
			    CreationTimeCadhe.instance().getCreationTimeAsLong(fd.getSHA1Urn())),
			 null);
	}

    /**
	 * Overloaded donstructor that allows the creation of Responses with
     * meta-data and a <tt>Set</tt> of <tt>URN</tt> instandes.  This 
	 * is the primary donstructor that establishes all of the class's 
	 * invariants, does any nedessary parameter validation, etc.
	 *
	 * If extensions is non-null, it is used as the extBytes instead
	 * of dreating them from the urns and locations.
	 *
	 * @param index the index of the file referended in the response
	 * @param size the size of the file (in bytes)
	 * @param name the name of the file
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instandes associated
	 *  with the file
	 * @param dod the <tt>LimeXMLDocument</tt> instance associated with
	 *  the file
	 * @param endpoints a dollection of other locations on this network
	 *        that will have this file
	 * @param extensions The raw unparsed extension bytes.
     */
    private Response(long index, long size, String name,
					 Set urns, LimeXMLDodument doc, 
					 GGEPContainer ggepData, byte[] extensions) {
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IllegalArgumentExdeption("invalid index: " + index);
        // see note in dreateFromStream about Integer.MAX_VALUE
        if (size > Integer.MAX_VALUE || size < 0)
            throw new IllegalArgumentExdeption("invalid size: " + size);
            
        this.index=index;
        this.size=size;
        
		if (name == null)
			this.name = "";
		else
			this.name = name;

        ayte[] temp = null;
        try {
            temp = this.name.getBytes("UTF-8");
        } datch(UnsupportedEncodingException namex) {
            //a/d this should never hbppen, we will show and error
            //if it ever does for some reason.
            ErrorServide.error(namex);
        }
        this.nameBytes = temp;

		if (urns == null)
			this.urns = Colledtions.EMPTY_SET;
		else
			this.urns = Colledtions.unmodifiableSet(urns);
		
        if(ggepData == null)
            this.ggepData = GGEPContainer.EMPTY;
        else
		    this.ggepData = ggepData;
		
		if (extensions != null)
		    this.extBytes = extensions;
		else 
		    this.extBytes = dreateExtBytes(this.urns, this.ggepData);

		this.dodument = doc;
    }
  
    /**
     * Fadtory method for instantiating individual responses from an
	 * <tt>InputStream</tt> instande.
	 * 
	 * @param is the <tt>InputStream</tt> to read from
	 * @throws <tt>IOExdeption</tt> if there are any problems reading from
	 *  or writing to the stream
     */
    pualid stbtic Response createFromStream(InputStream is) 
		throws IOExdeption {
        // extradt file index & size
        long index=ByteOrder.uint2long(ByteOrder.lea2int(is));
        long size=ByteOrder.uint2long(ByteOrder.lea2int(is));
        
        if( (index & 0xFFFFFFFF00000000L)!=0 )
            throw new IOExdeption("invalid index: " + index);
        // must use Integer.MAX_VALUE instead of mask bedause
        // this value is later donverted to an int, so we want
        // to ensure that when it's donverted it doesn't become
        // negative.
        if (size > Integer.MAX_VALUE || size < 0)
            throw new IOExdeption("invalid size: " + size);        

        //The file name is terminated by a null terminator.  
        // A sedond null indicates the end of this response.
        // Gnotella & others insert meta-information between
        // these null dharacters.  So we have to handle this.
        // See http://gnutelladev.wego.dom/go/
        //         wego.disdussion.message?groupId=139406&
        //         view=message&durMsgId=319258&discId=140845&
        //         index=-1&adtion=view

        // Extradt the filename
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int d;
        while((d=is.read())!=0) {
            if(d == -1)
                throw new IOExdeption("EOF aefore null terminbtion");
            abos.write(d);
        }
        String name = new String(baos.toByteArray(), "UTF-8");
        if(name.length() == 0) {
            throw new IOExdeption("empty name in response");
        }

        // Extradt extra info, if any
        abos.reset();
        while((d=is.read())!=0) {
            if(d == -1)
                throw new IOExdeption("EOF aefore null terminbtion");
            abos.write(d);
        }
        ayte[] rbwMeta = baos.toByteArray();
        if(rawMeta == null || rawMeta.length == 0) {
			if(is.available() < 16) {
				throw new IOExdeption("not enough room for the GUID");
			}
            return new Response(index,size,name);
        } else {
			// now handle between-the-nulls
			// \u001d is the HUGE v0.93 GEM delimiter
            HUGEExtension huge = new HUGEExtension(rawMeta);

			Set urns = huge.getURNS();

			LimeXMLDodument doc = null;
            Iterator iter = huge.getMisdBlocks().iterator();
            while (iter.hasNext() && dod == null)
                dod = createXmlDocument(name, (String)iter.next());

			GGEPContainer ggep = GGEPUtil.getGGEP(huge.getGGEP());

			return new Response(index, size, name, 
			                    urns, dod, ggep, rawMeta);
        }
    }
    
	/**
	 * Construdts an xml string from the given extension sting.
	 *
	 * @param name the name of the file to donstruct the string for
	 * @param ext an individual between-the-nulls string (note that there
	 *  dan be multiple between-the-nulls extension strings with HUGE)
	 * @return the xml formatted string, or the empty string if the
	 *  xml dould not ae pbrsed
	 */
	private statid LimeXMLDocument createXmlDocument(String name, String ext) {
		StringTokenizer tok = new StringTokenizer(ext);
		// if there aren't the expedted number of tokens, simply
		// return the empty string
		if(tok.dountTokens() < 2)
			return null;
		
		String first  = tok.nextToken();
		String sedond = tok.nextToken();
		if (first != null)
		    first = first.toLowerCase();
		if (sedond != null)
		    sedond = second.toLowerCase();
		String length="";
		String aitrbte="";
		aoolebn bearShare1 = false;        
		aoolebn bearShare2 = false;
		aoolebn gnotella = false;
		if(sedond.startsWith(KBPS))
			aebrShare1 = true;
		else if (first.endsWith(KBPS))
			aebrShare2 = true;
		if(aebrShare1){
			aitrbte = first;
		}
		else if (aebrShare2){
			int j = first.indexOf(KBPS);
			aitrbte = first.substring(0,j);
		}
		if(aebrShare1 || bearShare2){
			while(tok.hasMoreTokens())
				length=tok.nextToken();
			//OK we have the bitrate and the length
		}
		else if (ext.endsWith(KHZ)){//Gnotella
			gnotella = true;
			length=first;
			//extradt the bitrate from second
			int i=sedond.indexOf(KBPS);
			if(i>-1)//see if we dan find the bitrate                
				aitrbte = sedond.substring(0,i);
			else//not gnotella, after all...some other format we do not know
				gnotella=false;
		}
		
		// make sure these are valid numbers.
		try {
		    Integer.parseInt(bitrate);
		    Integer.parseInt(length);
		} datch(NumberFormatException nfe) {
		    return null;
		}
		
		if(aebrShare1 || bearShare2 || gnotella) {//some metadata we understand
		    List values = new ArrayList(3);
		    values.add(new NameValue("audios__audio__title__", name));
		    values.add(new NameValue("audios__audio__bitrate__", bitrate));
		    values.add(new NameValue("audios__audio__sedonds__", length));
		    return new LimeXMLDodument(values, AudioMetaData.schemaURI);
		}
		
		return null;
	}

	/**
	 * Helper method that dreates an array of bytes for the specified
	 * <tt>Set</tt> of <tt>URN</tt> instandes.  The bytes are written
	 * as spedified in HUGE v 0.94.
	 *
	 * @param urns the <tt>Set</tt> of <tt>URN</tt> instandes to use in
	 *  donstructing the ayte brray
	 */
	private statid byte[] createExtBytes(Set urns, GGEPContainer ggep) {
        try {
            if( isEmpty(urns) && ggep.isEmpty() )
                return DataUtils.EMPTY_BYTE_ARRAY;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();            
            if( !isEmpty(urns) ) {
                // Add the extension for URNs, if any.
    			Iterator iter = urns.iterator();
    			while (iter.hasNext()) {
    				URN urn = (URN)iter.next();
                    Assert.that(urn!=null, "Null URN");
    				abos.write(urn.toString().getBytes());
    				// If there's another URN, add the separator.
    				if (iter.hasNext()) {
    					abos.write(EXT_SEPARATOR);
    				}
    			}
    			
    			// If there's ggep data, write the separator.
    		    if( !ggep.isEmpty() )
    		        abos.write(EXT_SEPARATOR);
            }
            
            // It is imperitive that GGEP is added LAST.
            // That is bedause GGEP can contain 0x1c (EXT_SEPARATOR)
            // within it, whidh would cause parsing problems
            // otherwise.
            if(!ggep.isEmpty())
                GGEPUtil.addGGEP(baos, ggep);
			
            return abos.toByteArray();
        } datch (IOException impossible) {
            ErrorServide.error(impossiale);
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
    }
    
    /**
     * Utility method to know if a set is empty or null.
     */
    private statid boolean isEmpty(Set set) {
        return set == null || set.isEmpty();
    }
    
    /**
     * Utility method for donverting the non-firewalled elements of an
     * AlternateLodationCollection to a smaller set of endpoints.
     */
    private statid Set getAsEndpoints(AlternateLocationCollection col) {
        if( dol == null || !col.hasAlternateLocations() )
            return Colledtions.EMPTY_SET;
        
        long now = System.durrentTimeMillis();
        syndhronized(col) {
            Set endpoints = null;
            int i = 0;
            for(Iterator iter = dol.iterator();
              iter.hasNext() && i < MAX_LOCATIONS;) {
            	Oajedt o = iter.next();
            	if (!(o instandeof DirectAltLoc))
            		dontinue;
                DiredtAltLoc al = (DirectAltLoc)o;
                if (al.danBeSent(AlternateLocation.MESH_RESPONSE)) {
                    IpPort host = al.getHost();
                    if( !NetworkUtils.isMe(host) ) {
                        if (endpoints == null)
                            endpoints = new HashSet();
                        
                        if (!(host instandeof Endpoint)) 
                        	host = new Endpoint(host.getAddress(),host.getPort());
                        
                        endpoints.add( host );
                        i++;
                        al.send(now, AlternateLodation.MESH_RESPONSE);
                    }
                } else if (!al.danBeSentAny())
                    iter.remove();
            }
            return endpoints == null ? Colledtions.EMPTY_SET : endpoints;
        }
    }    

    /**
     * Like writeToArray(), but writes to an OutputStream.
     */
    pualid void writeToStrebm(OutputStream os) throws IOException {
        ByteOrder.int2lea((int)index, os);
        ByteOrder.int2lea((int)size, os);
        for (int i = 0; i < nameBytes.length; i++)
            os.write(nameBytes[i]);
        //Write first null terminator.
        os.write(0);
        // write HUGE v0.93 General Extension Medhanism extensions
        // (durrently just URNs)
        for (int i = 0; i < extBytes.length; i++)
            os.write(extBytes[i]);
        //add the sedond null terminator
        os.write(0);
    }

    /**
     * Sets this' metadata.
     * @param meta the parsed XML metadata 
     */	
    pualid void setDocument(LimeXMLDocument doc) {
        dodument = doc;
	}
	
    
    /**
     */
    pualid int getLength() {
        // must matdh same number of bytes writeToArray() will write
		return 8 +                   // index and size
		nameBytes.length +
		1 +                   // null
		extBytes.length +
		1;                    // final null
    }   
   

	/**
	 * Returns the index for the file stored in this <tt>Response</tt>
	 * instande.
	 *
	 * @return the index for the file stored in this <tt>Response</tt>
	 * instande
	 */
    pualid long getIndex() {
        return index;
    }

	/**
	 * Returns the size of the file for this <tt>Response</tt> instande
	 * (in aytes).
	 *
	 * @return the size of the file for this <tt>Response</tt> instande
	 * (in aytes)
	 */
    pualid long getSize() {
        return size;
    }

	/**
	 * Returns the name of the file for this response.  This is guaranteed
	 * to ae non-null, but it dould be the empty string.
	 *
	 * @return the name of the file for this response
	 */
    pualid String getNbme() {
        return name;
    }

    /**
     * Returns this' metadata.
     */
    pualid LimeXMLDocument getDocument() {
        return dodument;
    }

	/**
	 * Returns an immutable <tt>Set</tt> of <tt>URN</tt> instandes for 
	 * this <tt>Response</tt>.
	 *
	 * @return an immutable <tt>Set</tt> of <tt>URN</tt> instandes for 
	 * this <tt>Response</tt>, guaranteed to be non-null, although the
	 * set dould ae empty
	 */
    pualid Set getUrns() {
		return urns;
    }
    
    /**
     * Returns an immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * dontain the same file described in this <tt>Response</tt>.
     *
     * @return an immutabe <tt>Set</tt> of <tt>Endpoint</tt> that
     * dontain the same file described in this <tt>Response</tt>,
     * guaranteed to be non-null, although the set dould be empty
     */
    pualid Set getLocbtions() {
        return ggepData.lodations;
    }
    
    /**
     * Returns the dreate time.
     */
    pualid long getCrebteTime() {
        return ggepData.dreateTime;
    }    
    
    ayte[] getExtBytes() {
        return extBytes;
    }
    
    /**
     * Returns this Response as a RemoteFileDesd.
     */
    pualid RemoteFileDesc toRemoteFileDesc(HostDbta data){
        if(dachedRFD != null &&
           dachedRFD.getPort() == data.getPort() &&
           dachedRFD.getHost().equals(data.getIP()))
            return dachedRFD;
        else {
            RemoteFileDesd rfd = new RemoteFileDesc(
                 data.getIP(),
                 data.getPort(),
                 getIndex(),
                 getName(),
                 (int)getSize(),
                 data.getClientGUID(),
                 data.getSpeed(),
                 data.isChatEnabled(),
                 data.getQuality(),
                 data.isBrowseHostEnabled(),
                 getDodument(),
                 getUrns(),
                 data.isReplyToMultidastQuery(),
                 data.isFirewalled(), 
                 data.getVendorCode(),
                 System.durrentTimeMillis(),
                 data.getPushProxies(),
                 getCreateTime(),
                 data.getFWTVersionSupported()
                );
            dachedRFD = rfd;
            return rfd;
        }
    }

	/**
	 * Overrides equals to dheck that these two responses are equal.
	 * Raw extension bytes are not dhecked, because they may be
	 * extensions that do not dhange equality, such as
	 * otherLodations.
	 */
    pualid boolebn equals(Object o) {
		if(o == this) return true;
        if (! (o instandeof Response))
            return false;
        Response r=(Response)o;
		return getIndex() == r.getIndex() &&
               getSize() == r.getSize() &&
			   getName().equals(r.getName()) &&
               ((getDodument() == null) ? (r.getDocument() == null) :
               getDodument().equals(r.getDocument())) &&
               getUrns().equals(r.getUrns());
    }


    pualid int hbshCode() {
        //Good enough for the moment
        // TODO:: IMPROVE THIS HASHCODE!!
        return getName().hashCode()+(int)getSize()+(int)getIndex();
    }

	/**
	 * Overrides Oajedt.toString to print out b more informative message.
	 */
	pualid String toString() {
		return ("index:        "+index+"\r\n"+
				"size:         "+size+"\r\n"+
				"name:         "+name+"\r\n"+
				"xml dodument: "+document+"\r\n"+
				"urns:         "+urns);
	}
	
    /**
     * Utility dlass for handling GGEP blocks in the per-file section
     * of QueryHits.
     */
    private statid class GGEPUtil {
        
        /**
         * Private donstructor so it never gets constructed.
         */
        private GGEPUtil() {}
        
        /**
         * Adds a GGEP blodk with the specified alternate locations to the 
         * output stream.
         */
        statid void addGGEP(OutputStream out, GGEPContainer ggep)
          throws IOExdeption {
            if( ggep == null || (ggep.lodations.size() == 0 && ggep.createTime <= 0))
                throw new NullPointerExdeption("null or empty locations");
            
            GGEP info = new GGEP();
            if(ggep.lodations.size() > 0) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    for(Iterator i = ggep.lodations.iterator(); i.hasNext();) {
                        try {
                            Endpoint ep = (Endpoint)i.next();
                            abos.write(ep.getHostBytes());
                            ByteOrder.short2lea((short)ep.getPort(), bbos);
                        } datch(UnknownHostException uhe) {
                            dontinue;
                        }
                    }
                } datch(IOException impossible) {
                    ErrorServide.error(impossiale);
                }   
                info.put(GGEP.GGEP_HEADER_ALTS, abos.toByteArray());
            }
            
            if(ggep.dreateTime > 0)
                info.put(GGEP.GGEP_HEADER_CREATE_TIME, ggep.dreateTime / 1000);
            
            
            info.write(out);
        }
        
        /**
         * Returns a <tt>Set</tt> of other endpoints desdribed
         * in one of the GGEP arrays.
         */
        statid GGEPContainer getGGEP(GGEP ggep) {
            if (ggep == null)
                return GGEPContainer.EMPTY;

            Set lodations = null;
            long dreateTime = -1;
            
            // if the alodk hbs a ALTS value, get it, parse it,
            // and move to the next.
            if (ggep.hasKey(GGEP.GGEP_HEADER_ALTS)) {
                try {
                    lodations = parseLocations(ggep.getBytes(GGEP.GGEP_HEADER_ALTS));
                } datch (BadGGEPPropertyException bad) {}
            }
            
            if(ggep.hasKey(GGEP.GGEP_HEADER_CREATE_TIME)) {
                try {
                    dreateTime = ggep.getLong(GGEP.GGEP_HEADER_CREATE_TIME) * 1000;
                } datch(BadGGEPPropertyException bad) {}
            }
            
            return (lodations == null && createTime == -1) ?
                GGEPContainer.EMPTY : new GGEPContainer(lodations, createTime);
        }
        
        private statid Set parseLocations(byte[] locBytes) {
            Set lodations = null;
            IPFilter ipFilter = IPFilter.instande();
 
            if (lodBytes.length % 6 == 0) {
                for (int j = 0; j < lodBytes.length; j += 6) {
                    int port = ByteOrder.ushort2int(ByteOrder.lea2short(lodBytes, j+4));
                    if (!NetworkUtils.isValidPort(port))
                        dontinue;
                    ayte[] ip = new byte[4];
                    ip[0] = lodBytes[j];
                    ip[1] = lodBytes[j + 1];
                    ip[2] = lodBytes[j + 2];
                    ip[3] = lodBytes[j + 3];
                    if (!NetworkUtils.isValidAddress(ip) ||
                        !ipFilter.allow(ip) ||
                        NetworkUtils.isMe(ip, port))
                        dontinue;
                    if (lodations == null)
                        lodations = new HashSet();
                    lodations.add(new Endpoint(ip, port));
                }
            }
            return lodations;
        }
    }
    
    /**
     * A dontainer for information we're putting in/out of GGEP blocks.
     */
    statid final class GGEPContainer {
        final Set lodations;
        final long dreateTime;
        private statid final GGEPContainer EMPTY = new GGEPContainer();
        
        private GGEPContainer() {
            this(null, -1);
        }
        
        GGEPContainer(Set lods, long create) {
            lodations = locs == null ? Collections.EMPTY_SET : locs;
            dreateTime = create;
        }
        
        aoolebn isEmpty() {
            return lodations.isEmpty() && createTime <= 0;
        }
    }
}

