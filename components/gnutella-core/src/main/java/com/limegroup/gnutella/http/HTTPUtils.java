package com.limegroup.gnutella.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;

/**
 * This class supplies general facilities for handling HTTP, such as
 * writing headers, extracting header values, etc..
 */
public final class HTTPUtils {
	
	/**
	 * Constant for the carriage-return linefeed sequence that marks
	 * the end of an HTTP header
	 */
	private static final String CRLF = "\r\n";

	/**
	 * Cached colon followed by a space to avoid excessive allocations.
	 */
	private static final String COLON_SPACE = ": ";

	/**
	 * Cached colon to avoid excessive allocations.
	 */
	private static final String COLON = ":";
	
	/**
	 * Cached slash to avoid excessive allocations.
	 */
	private static final String SLASH = "/";

	/**
	 * Private constructor to ensure that this class cannot be constructed
	 */
	private HTTPUtils() {}
	
	/**
	 * Writes an single http header to the specified 
	 * <tt>OutputStream</tt> instance, with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name to write to the stream
	 * @param value the <tt>String</tt> instance containing the
	 *  header value to write to the stream
	 * @param os the <tt>OutputStream</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, String value, 
								   OutputStream os) 
		throws IOException {
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(os == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value);
		os.write(header.getBytes());
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes an single http header to the specified 
	 * <tt>OutputStream</tt> instance, with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name to write to the stream
	 * @param value the <tt>HTTPHeaderValue</tt> instance containing the
	 *  header value to write to the stream
	 * @param out the <tt>Writer</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, String value, Writer out) 
	  throws IOException {
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(out == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value);
		out.write(header);
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}
	

	/**
	 * Writes an single http header to the specified 
	 * <tt>OutputStream</tt> instance, with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name to write to the stream
	 * @param name the <tt>HTTPHeaderValue</tt> instance containing the
	 *  header value to write to the stream
	 * @param os the <tt>OutputStream</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, OutputStream os) 
      throws IOException {
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(os == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value.httpStringValue());
		os.write(header.getBytes());
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}

	/**
	 * Writes an single http header to the specified 
	 * <tt>OutputStream</tt> instance, with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name to write to the stream
	 * @param name the <tt>HTTPHeaderValue</tt> instance containing the
	 *  header value to write to the stream
	 * @param out the <tt>Writer</tt> instance to write to
	 */
	public static void writeHeader(HTTPHeaderName name, HTTPHeaderValue value, 
								   Writer out) 
		throws IOException {
		if(name == null) {
			throw new NullPointerException("null name in writing http header");
		} else if(value == null) {
			throw new NullPointerException("null value in writing http header: "+
										   name);
		} else if(out == null) {
			throw new NullPointerException("null os in writing http header: "+
										   name);
		}
		String header = createHeader(name, value.httpStringValue());
		out.write(header);
		BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH.addData(header.length());
	}
    
    public static String createHeader(HTTPHeaderName name, HTTPHeaderValue value) {
        return createHeader(name.httpStringValue(), value.httpStringValue());
    }

	/**
	 * Create a single http header String with the specified header name 
	 * and the specified header value.
	 *
	 * @param name the <tt>HTTPHeaderName</tt> instance containing the
	 *  header name 
	 * @param valueStr the value of the header, generally the httpStringValue
	 *  or a HttpHeaderValue, or just a String.
	 */
	public static String createHeader(HTTPHeaderName name, String valueStr) {
        return createHeader(name.httpStringValue(), valueStr);
    }
    
    public static String createHeader(String name, HTTPHeaderValue value) {
        return createHeader(name, value.httpStringValue());
    }
    
    public static String createHeader(String name, String value) {
        StringBuilder sb = new StringBuilder(name.length() + value.length() + 4);
		return sb.append(name).append(COLON_SPACE).append(value).append(CRLF).toString();
	}

	/**
	 * Parses out the header value from the HTTP header string.
	 *
	 * @return the header value for the specified full header string, or
	 *  <tt>null</tt> if the value could not be extracted
	 */
	public static String extractHeaderValue(final String header) {
		int index = header.indexOf(COLON);
		if(index <= 0) return null;
		return header.substring(index+1).trim();
	}

	/**
     * Utility method for writing a header with an integer value.  This removes
     * the burden to the caller of converting integer HTTP values to strings.
     * 
	 * @param name the <tt>HTTPHeaderName</tt> of the header to write
	 * @param value the int value of the header
	 * @param writer the <tt>Writer</tt> instance to write the header to
	 * @throws IOException if an IO error occurs during the write
	 */
    public static void writeHeader(HTTPHeaderName name, int value, Writer writer) throws IOException {
        writeHeader(name, String.valueOf(value), writer);
    }
    
    /**
     * Utility method for writing a header with an integer value.  This removes
     * the burden to the caller of converting integer HTTP values to strings.
     * 
     * @param name the <tt>HTTPHeaderName</tt> of the header to write
     * @param value the int value of the header
     * @param stream the <tt>OutputStream</tt> instance to write the header to
     * @throws IOException if an IO error occurs during the write
     */
    public static void writeHeader(HTTPHeaderName name, int value, OutputStream stream) throws IOException {
        writeHeader(name, String.valueOf(value), stream);
    }
    
    /**
     * Writes the Content-Disposition header, assuming an 'attachment'.
     */
    public static void writeContentDisposition(String name, Writer writer) throws IOException {
        writeHeader(HTTPHeaderName.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encode(name, "US-ASCII") + "\"",
                    writer);
    }
    
    /**
     * Utility method for writing the "Date" header, as specified in RFC 2616
     * section 14.18, to a <tt>Writer</tt>.
     * 
     * @param writer the <tt>Writer</tt> to write the header to
     * @throws IOException if a write error occurs
     */
    public static void writeDate(Writer writer) throws IOException {
        writeHeader(HTTPHeaderName.DATE, getDateValue(), writer);
    }
  
    /**
     * Utility method for writing the "Date" header, as specified in RFC 2616
     * section 14.18, to a <tt>OutputStream</tt>.
     * 
     * @param stream the <tt>OutputStream</tt> to write the header to
     * @throws IOException if a write error occurs
     */
    public static void writeDate(OutputStream stream) throws IOException {
        writeHeader(HTTPHeaderName.DATE, getDateValue(), stream);       
    }
    
    /**
     * Utility method for writing the currently supported features
     * to the <tt>Writer</tt>.
     */
    public static void writeFeatures(Writer writer) throws IOException {
        Set<HTTPHeaderValue> features = getFeaturesValue();
        // Write X-Features header.
        if (features.size() > 0) {
            writeHeader(HTTPHeaderName.FEATURES,
                    new HTTPHeaderValueCollection(features), writer);
        }
    }
    
    /**
     * Utility method for writing the currently supported features
     * to the <tt>OutputStream</tt>.
     */
    public static void writeFeatures(OutputStream stream) throws IOException {
        Set<HTTPHeaderValue> features = getFeaturesValue();
        // Write X-Features header.
        if (features.size() > 0) {
            writeHeader(HTTPHeaderName.FEATURES,
                    new HTTPHeaderValueCollection(features), stream);
        }
    }        
    
    /**
     * Utlity method for getting the currently supported features.
     */
    private static Set<HTTPHeaderValue> getFeaturesValue() {
        Set<HTTPHeaderValue> features = new HashSet<HTTPHeaderValue>(4);
        features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
        if (ChatSettings.CHAT_ENABLED.getValue())
            features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
        
       	features.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
       	
       	if (!RouterService.acceptedIncomingConnection() && UDPService.instance().canDoFWT())
       	    features.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        
        return features;
    }
    
    /**
     * Utility method for extracting the version from a feature token.
     */
    public static float parseFeatureToken(String token) throws
    	ProblemReadingHeaderException{
        int slashIndex = token.indexOf(SLASH);
        
        if (slashIndex == -1 || slashIndex >= token.length()-1)
            throw new ProblemReadingHeaderException("invalid feature token");
        
        String versionS = token.substring(slashIndex+1);
        
        try {
            return Float.parseFloat(versionS);
        }catch (NumberFormatException bad) {
            throw new ProblemReadingHeaderException(bad);
        }
    }
    
    /**
     * Utility method for getting the date value for the "Date" header in
     * standard format.
     * 
     * @return the current date as a standardized date string -- see 
     *  RFC 2616 section 14.18
     */
    private static String getDateValue() {
        DateFormat df = 
            new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date());
    }
    
    /**
     * Encodes a name using URLEncoder, using %20 instead of + for spaces.
     */
    private static String encode(String name, String encoding) throws IOException {
        return StringUtils.replace(URLEncoder.encode(name, encoding), "+", "%20");
    }
}
