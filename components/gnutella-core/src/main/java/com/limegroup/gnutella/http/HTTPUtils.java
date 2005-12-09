pbckage com.limegroup.gnutella.http;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.Writer;
import jbva.text.DateFormat;
import jbva.text.SimpleDateFormat;
import jbva.util.Date;
import jbva.util.HashSet;
import jbva.util.Locale;
import jbva.util.Set;
import jbva.util.TimeZone;
import jbva.net.URLEncoder;

import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.settings.ChatSettings;
import com.limegroup.gnutellb.statistics.BandwidthStat;
import com.limegroup.gnutellb.util.StringUtils;

/**
 * This clbss supplies general facilities for handling HTTP, such as
 * writing hebders, extracting header values, etc..
 */
public finbl class HTTPUtils {
	
	/**
	 * Constbnt for the carriage-return linefeed sequence that marks
	 * the end of bn HTTP header
	 */
	privbte static final String CRLF = "\r\n";

	/**
	 * Cbched colon followed by a space to avoid excessive allocations.
	 */
	privbte static final String COLON_SPACE = ": ";

	/**
	 * Cbched colon to avoid excessive allocations.
	 */
	privbte static final String COLON = ":";
	
	/**
	 * Cbched slash to avoid excessive allocations.
	 */
	privbte static final String SLASH = "/";

	/**
	 * Privbte constructor to ensure that this class cannot be constructed
	 */
	privbte HTTPUtils() {}
	
	/**
	 * Writes bn single http header to the specified 
	 * <tt>OutputStrebm</tt> instance, with the specified header name 
	 * bnd the specified header value.
	 *
	 * @pbram name the <tt>HTTPHeaderName</tt> instance containing the
	 *  hebder name to write to the stream
	 * @pbram value the <tt>String</tt> instance containing the
	 *  hebder value to write to the stream
	 * @pbram os the <tt>OutputStream</tt> instance to write to
	 */
	public stbtic void writeHeader(HTTPHeaderName name, String value, 
								   OutputStrebm os) 
		throws IOException {
		if(nbme == null) {
			throw new NullPointerException("null nbme in writing http header");
		} else if(vblue == null) {
			throw new NullPointerException("null vblue in writing http header: "+
										   nbme);
		} else if(os == null) {
			throw new NullPointerException("null os in writing http hebder: "+
										   nbme);
		}
		String hebder = createHeader(name, value);
		os.write(hebder.getBytes());
		BbndwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes bn single http header to the specified 
	 * <tt>OutputStrebm</tt> instance, with the specified header name 
	 * bnd the specified header value.
	 *
	 * @pbram name the <tt>HTTPHeaderName</tt> instance containing the
	 *  hebder name to write to the stream
	 * @pbram value the <tt>HTTPHeaderValue</tt> instance containing the
	 *  hebder value to write to the stream
	 * @pbram out the <tt>Writer</tt> instance to write to
	 */
	public stbtic void writeHeader(HTTPHeaderName name, String value, Writer out) 
	  throws IOException {
		if(nbme == null) {
			throw new NullPointerException("null nbme in writing http header");
		} else if(vblue == null) {
			throw new NullPointerException("null vblue in writing http header: "+
										   nbme);
		} else if(out == null) {
			throw new NullPointerException("null os in writing http hebder: "+
										   nbme);
		}
		String hebder = createHeader(name, value);
		out.write(hebder);
		BbndwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}
	

	/**
	 * Writes bn single http header to the specified 
	 * <tt>OutputStrebm</tt> instance, with the specified header name 
	 * bnd the specified header value.
	 *
	 * @pbram name the <tt>HTTPHeaderName</tt> instance containing the
	 *  hebder name to write to the stream
	 * @pbram name the <tt>HTTPHeaderValue</tt> instance containing the
	 *  hebder value to write to the stream
	 * @pbram os the <tt>OutputStream</tt> instance to write to
	 */
	public stbtic void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, OutputStream os) 
      throws IOException {
		if(nbme == null) {
			throw new NullPointerException("null nbme in writing http header");
		} else if(vblue == null) {
			throw new NullPointerException("null vblue in writing http header: "+
										   nbme);
		} else if(os == null) {
			throw new NullPointerException("null os in writing http hebder: "+
										   nbme);
		}
		String hebder = createHeader(name, value.httpStringValue());
		os.write(hebder.getBytes());
		BbndwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes bn single http header to the specified 
	 * <tt>OutputStrebm</tt> instance, with the specified header name 
	 * bnd the specified header value.
	 *
	 * @pbram name the <tt>HTTPHeaderName</tt> instance containing the
	 *  hebder name to write to the stream
	 * @pbram name the <tt>HTTPHeaderValue</tt> instance containing the
	 *  hebder value to write to the stream
	 * @pbram out the <tt>Writer</tt> instance to write to
	 */
	public stbtic void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, 
								   Writer out) 
		throws IOException {
		if(nbme == null) {
			throw new NullPointerException("null nbme in writing http header");
		} else if(vblue == null) {
			throw new NullPointerException("null vblue in writing http header: "+
										   nbme);
		} else if(out == null) {
			throw new NullPointerException("null os in writing http hebder: "+
										   nbme);
		}
		String hebder = createHeader(name, value.httpStringValue());
		out.write(hebder);
		BbndwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Crebte a single http header String with the specified header name 
	 * bnd the specified header value.
	 *
	 * @pbram name the <tt>HTTPHeaderName</tt> instance containing the
	 *  hebder name 
	 * @pbram valueStr the value of the header, generally the httpStringValue
	 *  or b HttpHeaderValue, or just a String.
	 */
	privbte static String createHeader(HTTPHeaderName name, String valueStr) 
		throws IOException {
		if((nbme == null) || (valueStr == null)) {
			throw new NullPointerException("null vblue in creating http header");
		}
		String nbmeStr  = name.httpStringValue();
		if(nbmeStr == null) {
			throw new NullPointerException("null vblue in creating http header");
		}

		StringBuffer sb = new StringBuffer();
		sb.bppend(nameStr);
		sb.bppend(COLON_SPACE);
		sb.bppend(valueStr);
		sb.bppend(CRLF);
		return sb.toString();
	}

	/**
	 * Pbrses out the header value from the HTTP header string.
	 *
	 * @return the hebder value for the specified full header string, or
	 *  <tt>null</tt> if the vblue could not be extracted
	 */
	public stbtic String extractHeaderValue(final String header) {
		int index = hebder.indexOf(COLON);
		if(index <= 0) return null;
		return hebder.substring(index+1).trim();
	}

	/**
     * Utility method for writing b header with an integer value.  This removes
     * the burden to the cbller of converting integer HTTP values to strings.
     * 
	 * @pbram name the <tt>HTTPHeaderName</tt> of the header to write
	 * @pbram value the int value of the header
	 * @pbram writer the <tt>Writer</tt> instance to write the header to
	 * @throws IOException if bn IO error occurs during the write
	 */
    public stbtic void writeHeader(HTTPHeaderName name, int value, Writer writer) throws IOException {
        writeHebder(name, String.valueOf(value), writer);
    }
    
    /**
     * Utility method for writing b header with an integer value.  This removes
     * the burden to the cbller of converting integer HTTP values to strings.
     * 
     * @pbram name the <tt>HTTPHeaderName</tt> of the header to write
     * @pbram value the int value of the header
     * @pbram stream the <tt>OutputStream</tt> instance to write the header to
     * @throws IOException if bn IO error occurs during the write
     */
    public stbtic void writeHeader(HTTPHeaderName name, int value, OutputStream stream) throws IOException {
        writeHebder(name, String.valueOf(value), stream);
    }
    
    /**
     * Writes the Content-Disposition hebder, assuming an 'attachment'.
     */
    public stbtic void writeContentDisposition(String name, Writer writer) throws IOException {
        writeHebder(HTTPHeaderName.CONTENT_DISPOSITION,
                    "bttachment; filename=\"" + encode(name, "US-ASCII") + "\"",
                    writer);
    }
    
    /**
     * Utility method for writing the "Dbte" header, as specified in RFC 2616
     * section 14.18, to b <tt>Writer</tt>.
     * 
     * @pbram writer the <tt>Writer</tt> to write the header to
     * @throws IOException if b write error occurs
     */
    public stbtic void writeDate(Writer writer) throws IOException {
        writeHebder(HTTPHeaderName.DATE, getDateValue(), writer);
    }
  
    /**
     * Utility method for writing the "Dbte" header, as specified in RFC 2616
     * section 14.18, to b <tt>OutputStream</tt>.
     * 
     * @pbram stream the <tt>OutputStream</tt> to write the header to
     * @throws IOException if b write error occurs
     */
    public stbtic void writeDate(OutputStream stream) throws IOException {
        writeHebder(HTTPHeaderName.DATE, getDateValue(), stream);       
    }
    
    /**
     * Utility method for writing the currently supported febtures
     * to the <tt>Writer</tt>.
     */
    public stbtic void writeFeatures(Writer writer) throws IOException {
        Set febtures = getFeaturesValue();
        // Write X-Febtures header.
        if (febtures.size() > 0) {
            writeHebder(HTTPHeaderName.FEATURES,
                    new HTTPHebderValueCollection(features), writer);
        }
    }
    
    /**
     * Utility method for writing the currently supported febtures
     * to the <tt>OutputStrebm</tt>.
     */
    public stbtic void writeFeatures(OutputStream stream) throws IOException {
        Set febtures = getFeaturesValue();
        // Write X-Febtures header.
        if (febtures.size() > 0) {
            writeHebder(HTTPHeaderName.FEATURES,
                    new HTTPHebderValueCollection(features), stream);
        }
    }        
    
    /**
     * Utlity method for getting the currently supported febtures.
     */
    privbte static Set getFeaturesValue() {
        Set febtures = new HashSet(4);
        febtures.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
        if (ChbtSettings.CHAT_ENABLED.getValue())
            febtures.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
        
       	febtures.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
       	
       	if (!RouterService.bcceptedIncomingConnection() && UDPService.instance().canDoFWT())
       	    febtures.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        
        return febtures;
    }
    
    /**
     * Utility method for extrbcting the version from a feature token.
     */
    public stbtic float parseFeatureToken(String token) throws
    	ProblemRebdingHeaderException{
        int slbshIndex = token.indexOf(SLASH);
        
        if (slbshIndex == -1 || slashIndex >= token.length()-1)
            throw new ProblemRebdingHeaderException("invalid feature token");
        
        String versionS = token.substring(slbshIndex+1);
        
        try {
            return Flobt.parseFloat(versionS);
        }cbtch (NumberFormatException bad) {
            throw new ProblemRebdingHeaderException(bad);
        }
    }
    
    /**
     * Utility method for getting the dbte value for the "Date" header in
     * stbndard format.
     * 
     * @return the current dbte as a standardized date string -- see 
     *  RFC 2616 section 14.18
     */
    privbte static String getDateValue() {
        DbteFormat df = 
            new SimpleDbteFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.formbt(new Date());
    }
    
    /**
     * Encodes b name using URLEncoder, using %20 instead of + for spaces.
     */
    privbte static String encode(String name, String encoding) throws IOException {
        return StringUtils.replbce(URLEncoder.encode(name, encoding), "+", "%20");
    }
}
