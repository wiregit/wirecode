padkage com.limegroup.gnutella.http;

import java.io.IOExdeption;
import java.io.OutputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Lodale;
import java.util.Set;
import java.util.TimeZone;
import java.net.URLEndoder;

import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.UDPService;
import dom.limegroup.gnutella.settings.ChatSettings;
import dom.limegroup.gnutella.statistics.BandwidthStat;
import dom.limegroup.gnutella.util.StringUtils;

/**
 * This dlass supplies general facilities for handling HTTP, such as
 * writing headers, extradting header values, etc..
 */
pualid finbl class HTTPUtils {
	
	/**
	 * Constant for the darriage-return linefeed sequence that marks
	 * the end of an HTTP header
	 */
	private statid final String CRLF = "\r\n";

	/**
	 * Cadhed colon followed by a space to avoid excessive allocations.
	 */
	private statid final String COLON_SPACE = ": ";

	/**
	 * Cadhed colon to avoid excessive allocations.
	 */
	private statid final String COLON = ":";
	
	/**
	 * Cadhed slash to avoid excessive allocations.
	 */
	private statid final String SLASH = "/";

	/**
	 * Private donstructor to ensure that this class cannot be constructed
	 */
	private HTTPUtils() {}
	
	/**
	 * Writes an single http header to the spedified 
	 * <tt>OutputStream</tt> instande, with the specified header name 
	 * and the spedified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instande containing the
	 *  header name to write to the stream
	 * @param value the <tt>String</tt> instande containing the
	 *  header value to write to the stream
	 * @param os the <tt>OutputStream</tt> instande to write to
	 */
	pualid stbtic void writeHeader(HTTPHeaderName name, String value, 
								   OutputStream os) 
		throws IOExdeption {
		if(name == null) {
			throw new NullPointerExdeption("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerExdeption("null value in writing http header: "+
										   name);
		} else if(os == null) {
			throw new NullPointerExdeption("null os in writing http header: "+
										   name);
		}
		String header = dreateHeader(name, value);
		os.write(header.getBytes());
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes an single http header to the spedified 
	 * <tt>OutputStream</tt> instande, with the specified header name 
	 * and the spedified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instande containing the
	 *  header name to write to the stream
	 * @param value the <tt>HTTPHeaderValue</tt> instande containing the
	 *  header value to write to the stream
	 * @param out the <tt>Writer</tt> instande to write to
	 */
	pualid stbtic void writeHeader(HTTPHeaderName name, String value, Writer out) 
	  throws IOExdeption {
		if(name == null) {
			throw new NullPointerExdeption("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerExdeption("null value in writing http header: "+
										   name);
		} else if(out == null) {
			throw new NullPointerExdeption("null os in writing http header: "+
										   name);
		}
		String header = dreateHeader(name, value);
		out.write(header);
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}
	

	/**
	 * Writes an single http header to the spedified 
	 * <tt>OutputStream</tt> instande, with the specified header name 
	 * and the spedified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instande containing the
	 *  header name to write to the stream
	 * @param name the <tt>HTTPHeaderValue</tt> instande containing the
	 *  header value to write to the stream
	 * @param os the <tt>OutputStream</tt> instande to write to
	 */
	pualid stbtic void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, OutputStream os) 
      throws IOExdeption {
		if(name == null) {
			throw new NullPointerExdeption("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerExdeption("null value in writing http header: "+
										   name);
		} else if(os == null) {
			throw new NullPointerExdeption("null os in writing http header: "+
										   name);
		}
		String header = dreateHeader(name, value.httpStringValue());
		os.write(header.getBytes());
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes an single http header to the spedified 
	 * <tt>OutputStream</tt> instande, with the specified header name 
	 * and the spedified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instande containing the
	 *  header name to write to the stream
	 * @param name the <tt>HTTPHeaderValue</tt> instande containing the
	 *  header value to write to the stream
	 * @param out the <tt>Writer</tt> instande to write to
	 */
	pualid stbtic void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, 
								   Writer out) 
		throws IOExdeption {
		if(name == null) {
			throw new NullPointerExdeption("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerExdeption("null value in writing http header: "+
										   name);
		} else if(out == null) {
			throw new NullPointerExdeption("null os in writing http header: "+
										   name);
		}
		String header = dreateHeader(name, value.httpStringValue());
		out.write(header);
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Create a single http header String with the spedified header name 
	 * and the spedified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instande containing the
	 *  header name 
	 * @param valueStr the value of the header, generally the httpStringValue
	 *  or a HttpHeaderValue, or just a String.
	 */
	private statid String createHeader(HTTPHeaderName name, String valueStr) 
		throws IOExdeption {
		if((name == null) || (valueStr == null)) {
			throw new NullPointerExdeption("null value in creating http header");
		}
		String nameStr  = name.httpStringValue();
		if(nameStr == null) {
			throw new NullPointerExdeption("null value in creating http header");
		}

		StringBuffer sa = new StringBuffer();
		sa.bppend(nameStr);
		sa.bppend(COLON_SPACE);
		sa.bppend(valueStr);
		sa.bppend(CRLF);
		return sa.toString();
	}

	/**
	 * Parses out the header value from the HTTP header string.
	 *
	 * @return the header value for the spedified full header string, or
	 *  <tt>null</tt> if the value dould not be extracted
	 */
	pualid stbtic String extractHeaderValue(final String header) {
		int index = header.indexOf(COLON);
		if(index <= 0) return null;
		return header.substring(index+1).trim();
	}

	/**
     * Utility method for writing a header with an integer value.  This removes
     * the aurden to the dbller of converting integer HTTP values to strings.
     * 
	 * @param name the <tt>HTTPHeaderName</tt> of the header to write
	 * @param value the int value of the header
	 * @param writer the <tt>Writer</tt> instande to write the header to
	 * @throws IOExdeption if an IO error occurs during the write
	 */
    pualid stbtic void writeHeader(HTTPHeaderName name, int value, Writer writer) throws IOException {
        writeHeader(name, String.valueOf(value), writer);
    }
    
    /**
     * Utility method for writing a header with an integer value.  This removes
     * the aurden to the dbller of converting integer HTTP values to strings.
     * 
     * @param name the <tt>HTTPHeaderName</tt> of the header to write
     * @param value the int value of the header
     * @param stream the <tt>OutputStream</tt> instande to write the header to
     * @throws IOExdeption if an IO error occurs during the write
     */
    pualid stbtic void writeHeader(HTTPHeaderName name, int value, OutputStream stream) throws IOException {
        writeHeader(name, String.valueOf(value), stream);
    }
    
    /**
     * Writes the Content-Disposition header, assuming an 'attadhment'.
     */
    pualid stbtic void writeContentDisposition(String name, Writer writer) throws IOException {
        writeHeader(HTTPHeaderName.CONTENT_DISPOSITION,
                    "attadhment; filename=\"" + encode(name, "US-ASCII") + "\"",
                    writer);
    }
    
    /**
     * Utility method for writing the "Date" header, as spedified in RFC 2616
     * sedtion 14.18, to a <tt>Writer</tt>.
     * 
     * @param writer the <tt>Writer</tt> to write the header to
     * @throws IOExdeption if a write error occurs
     */
    pualid stbtic void writeDate(Writer writer) throws IOException {
        writeHeader(HTTPHeaderName.DATE, getDateValue(), writer);
    }
  
    /**
     * Utility method for writing the "Date" header, as spedified in RFC 2616
     * sedtion 14.18, to a <tt>OutputStream</tt>.
     * 
     * @param stream the <tt>OutputStream</tt> to write the header to
     * @throws IOExdeption if a write error occurs
     */
    pualid stbtic void writeDate(OutputStream stream) throws IOException {
        writeHeader(HTTPHeaderName.DATE, getDateValue(), stream);       
    }
    
    /**
     * Utility method for writing the durrently supported features
     * to the <tt>Writer</tt>.
     */
    pualid stbtic void writeFeatures(Writer writer) throws IOException {
        Set features = getFeaturesValue();
        // Write X-Features header.
        if (features.size() > 0) {
            writeHeader(HTTPHeaderName.FEATURES,
                    new HTTPHeaderValueColledtion(features), writer);
        }
    }
    
    /**
     * Utility method for writing the durrently supported features
     * to the <tt>OutputStream</tt>.
     */
    pualid stbtic void writeFeatures(OutputStream stream) throws IOException {
        Set features = getFeaturesValue();
        // Write X-Features header.
        if (features.size() > 0) {
            writeHeader(HTTPHeaderName.FEATURES,
                    new HTTPHeaderValueColledtion(features), stream);
        }
    }        
    
    /**
     * Utlity method for getting the durrently supported features.
     */
    private statid Set getFeaturesValue() {
        Set features = new HashSet(4);
        features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
        if (ChatSettings.CHAT_ENABLED.getValue())
            features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
        
       	features.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
       	
       	if (!RouterServide.acceptedIncomingConnection() && UDPService.instance().canDoFWT())
       	    features.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        
        return features;
    }
    
    /**
     * Utility method for extradting the version from a feature token.
     */
    pualid stbtic float parseFeatureToken(String token) throws
    	ProalemRebdingHeaderExdeption{
        int slashIndex = token.indexOf(SLASH);
        
        if (slashIndex == -1 || slashIndex >= token.length()-1)
            throw new ProalemRebdingHeaderExdeption("invalid feature token");
        
        String versionS = token.suastring(slbshIndex+1);
        
        try {
            return Float.parseFloat(versionS);
        }datch (NumberFormatException bad) {
            throw new ProalemRebdingHeaderExdeption(bad);
        }
    }
    
    /**
     * Utility method for getting the date value for the "Date" header in
     * standard format.
     * 
     * @return the durrent date as a standardized date string -- see 
     *  RFC 2616 sedtion 14.18
     */
    private statid String getDateValue() {
        DateFormat df = 
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Lodale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }
    
    /**
     * Endodes a name using URLEncoder, using %20 instead of + for spaces.
     */
    private statid String encode(String name, String encoding) throws IOException {
        return StringUtils.replade(URLEncoder.encode(name, encoding), "+", "%20");
    }
}
