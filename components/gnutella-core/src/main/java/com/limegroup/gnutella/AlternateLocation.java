package com.limegroup.gnutella;

import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.sun.java.util.collections.*;
import java.net.*;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.*;
import java.io.*;

/**
 * This class encapsulates the data for an alternate resource location, as 
 * specified in HUGE v0.93.  This also provides utility methods for such 
 * operations as comparing alternate locations based on the date they were 
 * stored.<p>
 *
 * This class is immutable.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class AlternateLocation 
	implements com.sun.java.util.collections.Comparable, HTTPHeaderValue  {

	/**
	 * A <tt>URL</tt> instance for the URL specified in the header.
	 */
	private final URL URL;

	/**
	 * The <tt>String</tt> instance as it is written in the header.
	 */
	private final String OUTPUT_DATE_TIME;

	/**
	 * Contant long representation of the time in milliseconds since 
	 * January 1, 1970, 00:00:00 GMT.
	 */
	private final long TIME;

	/**
	 * Constant for the sha1 urn for this <tt>AlternateLocation</tt> --
	 * can be <tt>null</tt>.
	 */
	private final URN SHA1_URN;

	/**
	 * Cached hash code that is lazily initialized.
	 */
	private volatile int hashCode = 0;
	
	/**
	 * Constant empty clientGUID for RFDs made from locations.
	 */
	private static final byte[] EMPTY_GUID = new byte[16];

	/**
	 * Constructs a new <tt>AlternateLocation</tt> instance based on the
	 * specified string argument.  
	 *
	 * @param location a string containing a single alternate location,
	 *  including a full URL for a file and an optional date
	 * @throws <tt>IOException</tt> if there is any problem constructing
	 *  the new instance from the specified string, or if the <tt<location</tt>
	 *  argument is either null or the empty string -- we could (should?) 
	 *  throw NullPointerException here, but since we're already forcing the
	 *  caller to catch IOException, we might as well throw in in both cases
	 */
	public static AlternateLocation 
		createAlternateLocation(final String location) 
		throws IOException {
		if(location == null || location.equals("")) {
			throw new IOException("null or empty in alt loc: "+location);
		}

		URL url = AlternateLocation.createUrl(location);        
		if(url == null) {
			throw new IOException("could not parse url for alt loc: "+
								  location);
		}
		String outputDateTime = AlternateLocation.extractTimestamp(location);
		Date date;
		if(outputDateTime == null) {
			// just set the time to be as old as possible since there's
			// no date information -- this makes comparisons easier
			date = new Date(0);
		} else {
			date = AlternateLocation.createDateInstance(outputDateTime);
		}

		URN sha1 = null;
		try {
			sha1 = URN.createSHA1UrnFromURL(url);
		} catch(IOException e) {
			// don't accept if there's no SHA1, as SHA1 is all we currently
			// understand
			throw new IOException("no SHA1 in url: "+url);
		}
		return new AlternateLocation(url, date, sha1);
	}


	/**
	 * Creates a new <tt>AlternateLocation</tt> instance for the given 
	 * <tt>URL</tt> instance.  This constructor creates an alternate
	 * location with the current date and time as its timestamp.
	 * This can be used, for example, for newly uploaded files.
	 *
	 * @param url the <tt>URL</tt> instance for the resource
	 * @throws <tt>NullPointerException</tt> if the <tt>url</tt> argument is 
	 *  <tt>null</tt>
	 * @throws <tt>MalformedURLException</tt> if a copy of the supplied 
	 *  <tt>URL</tt> instance cannot be successfully created
	 * @throws <tt>IOException</tt> if the url argument is not a
	 *  valid location for any reason
	 */
	public static AlternateLocation createAlternateLocation(final URL url) 
		throws MalformedURLException, IOException {
		if(url == null) {
			throw new NullPointerException("cannot accept null URL");
		}

		if((url.getPort() & 0xFFFF0000) != 0) {
			throw new IllegalArgumentException("invalid port: "+url.getPort());
		}
		// create a new URL instance from the data for the given url
		// and the urn
		URL tempUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(),
							  url.getFile());
		// make the date the current time
		Date date = new Date();
		URN sha1 = null;
		try {
			sha1 = URN.createSHA1UrnFromURL(tempUrl);
		} catch(IOException e) {
			// don't accept if there's no SHA1, as SHA1 is all we currently
			// understand
			throw new IOException("no SHA1 in url: "+url);
		}
		return new AlternateLocation(tempUrl, date, sha1);
	}

	/**
	 * Creates a new <tt>AlternateLocation</tt> for the data stored in
	 * a <tt>RemoteFileDesc</tt>.
	 *
	 * @param rfd the <tt>RemoteFileDesc</tt> to use in creating the 
	 *  <tt>AlternateLocation</tt>
	 * @return a new <tt>AlternateLocation</tt>
	 * @throws <tt>IOException</tt> if the <tt>rfd</tt> does not contain
	 *  a valid urn or if it's a private address
	 * @throws <tt>NullPointerException</tt> if the <tt>rfd</tt> is 
	 *  <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the port is invalid
	 */
	public static AlternateLocation 
		createAlternateLocation(final RemoteFileDesc rfd) 
		throws IOException {
		if(rfd == null) {
			throw new NullPointerException("cannot accept null RFD");
		}
		URN urn = rfd.getSHA1Urn();
		if(urn == null) {
			throw new IOException("no SHA1 in RFD");
		}
		int port = rfd.getPort();
		if(!NetworkUtils.isValidPort(port)) {
			throw new IllegalArgumentException("invalid port: "+port);
		}	

        InetAddress address = InetAddress.getByName(rfd.getHost());
        if(NetworkUtils.isPrivateAddress(address.getAddress())) {
            throw new IOException("cannot accept private addresses: "+
                                  address.getAddress());
        }

		URL url = new URL("http", rfd.getHost(), port,						  
						  HTTPConstants.URI_RES_N2R + urn.httpStringValue());
		return new AlternateLocation(url, new Date(), urn);
	}

	/**
	 * Creates a new <tt>AlternateLocation</tt> for a file stored locally 
	 * with the specified <tt>URN</tt>.
	 *
	 * @param urn the <tt>URN</tt> of the locally stored file
	 */
	public static AlternateLocation createAlternateLocation(URN urn) 
		throws IOException {
		if(urn == null) throw new NullPointerException("null sha1");
		URL url = 
			new URL("http", 
					NetworkUtils.ip2string(RouterService.getAddress()), 
					RouterService.getPort(), 
					HTTPConstants.URI_RES_N2R + urn.httpStringValue());
		return new AlternateLocation(url, new Date(), urn);
	}

	/**
	 * Creates a new <tt>AlternateLocation</tt> with the specified <tt>URL</tt>
	 * and <tt>Date</tt> timestamp.
	 *
	 * @param url the <tt>URL</tt> for the <tt>AlternateLocation</tt>
	 * @param date the <tt>Date</tt> timestamp for the 
	 *  <tt>AlternateLocation</tt>
	 */
	private AlternateLocation(final URL url, final Date date, final URN sha1) {
		this.URL       = url;
		this.TIME      = date.getTime();
		this.SHA1_URN  = sha1;
		if(TIME == 0) {
			this.OUTPUT_DATE_TIME = null;
		} else {
			this.OUTPUT_DATE_TIME = 
				AlternateLocation.convertDateToString(date);
		}
	}

	/**
	 * Returns an instance of the <tt>URL</tt> instance for this 
	 * alternate location.
	 *
	 * @return a <tt>URL</tt> instance corresponding to the URL for this 
	 *  alternate location, or <tt>null</tt> if an instance could not
	 *  be created
	 */
	public URL getUrl() {
		try {
			return new URL(this.URL.getProtocol(), 
						   this.URL.getHost(), 
						   this.URL.getPort(),
						   this.URL.getFile());
		} catch(MalformedURLException e) {
			// this should never happen in practice, but retun null 
			// nevertheless
			return null;
		}
	}

	/**
	 * Accessor for the SHA1 urn for this <tt>AlternateLocation</tt>.
	 * 
	 * @return the SHA1 urn for the this <tt>AlternateLocation</tt>
	 */
	public URN getSHA1Urn() {
		return SHA1_URN;
	}
	

	/**
	 * Accessor for the datetime of this location
	 *
	 * @return the time (in millisecond since epoch) that this was stamped
	 */
	public long getTime() {
	    return TIME;
	}
	

	/**
	 * Converts the specified <tt>Date</tt> instance to a <tt>String</tt> 
	 * that fits the syntax specified in the ISO 8601 subset we're using,
	 * discussed at: http://www.w3.org/TR/NOTE-datetime.
	 *
	 * @param date the <tt>Date</tt> instance to convert
	 * @return a new <tt>String</tt> instance that matches the standard
	 *  syntax
	 */
	private static String convertDateToString(Date date) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(date);
		int[]    dateInts = new int[6];
		String[] dateStrs = new String[6];


		dateInts[0] = cal.get(Calendar.YEAR);		
		// add a month because the Calendar class stores January as '0'
		dateInts[1] = cal.get(Calendar.MONTH)+1;
		dateInts[2] = cal.get(Calendar.DAY_OF_MONTH);
		dateInts[3] = cal.get(Calendar.HOUR);
		dateInts[4] = cal.get(Calendar.MINUTE);
		dateInts[5] = cal.get(Calendar.SECOND);

		// loop through the ints to convert them to strings with leading 
		// zeros if they're less than 10
		for(int i=0; i<dateInts.length; i++) {
			if(dateInts[i] < 10) {
				dateStrs[i] = "0"+String.valueOf(dateInts[i]);
			}
			else {
				dateStrs[i] = String.valueOf(dateInts[i]);
			}
		}
		final String DASH  = "-";
		final String COLON = ":";
		StringBuffer sb = new StringBuffer();
		sb.append(dateStrs[0]);
		sb.append(DASH);
		sb.append(dateStrs[1]);
		sb.append(DASH);
		sb.append(dateStrs[2]);
		sb.append("T");
		sb.append(dateStrs[3]);
		sb.append(COLON);
		sb.append(dateStrs[4]);
		sb.append(COLON);
		sb.append(dateStrs[5]);
		sb.append("Z");
		return sb.toString();
	}

	/**
	 * Checks to see if the specified date string is valid, according to
	 * our interpretation.  First, we are requiring date formats of the 
	 * form specified at: http://www.w3.org/TR/NOTE-datetime, a subset of 
	 * ISO 8601.  In addition to this, we require that the date at least
	 * specify the day of the month.  If it does not do this, it is so 
	 * general that it is useless for our purposes.
	 *
	 * @param date the date string to validate
	 * @return <tt>true</tt> if the string represents a valid date 
	 *  according to our critetia, <tt>false</tt> otherwise
	 */
	private static boolean isValidTimestamp(final String timestamp) {
		StringTokenizer st = new StringTokenizer(timestamp, "T");
		int numToks = st.countTokens();
		if(numToks == 1) {
			return isValidDate(timestamp);
		} else if(numToks == 2) {
			String date = st.nextToken();
			String time = st.nextToken();
			return (isValidDate(date) && isValidTime(time));		
		} else {
			return false;
		}
	}

	
	/**
	 * Checks to see if the time specified in the given string is a valid time
	 * according to the date-time formate specified at:<p>
	 * 
	 * http://www.w3.org/TR/NOTE-datetime
	 *
	 * @param time the time to check
	 * @return <tt>true</tt> if the time fits the standard, <tt>false</tt> 
	 *  otherwise
	 */
	private static boolean isValidTime(final String time) {
		if(!time.endsWith("Z")) return false;
		String timeStr = time.substring(0, time.length()-1);
		StringTokenizer st = new StringTokenizer(timeStr, ":");
		int tokens = st.countTokens();

		// at least the hours and minutes must be specified if the time is
		// specified at all, according to the specification
		if(tokens < 2 || tokens > 3) {
			return false;
		}
		try {
			int hh = Integer.parseInt(st.nextToken());
			if(hh < 0 || hh > 23) {
				return false;
			}
			if(st.hasMoreTokens()) {
				int mm = Integer.parseInt(st.nextToken());
				if(mm < 0 || mm > 59) {
					return false;
				}
			}
			if(st.hasMoreTokens()) {
				// get the first two characters of the seconds field, 
				// ignoring fractions of a second
				String ssStr = st.nextToken().substring(0, 2);				
				int ss = Integer.parseInt(ssStr);
				if(ss < 0 || ss > 59) {
					return false;
				}
			}
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}

	/**
	 * Checks to see if the date specified in the given string is a valid date
	 * according to the date-time formate specified at:<p>
	 * 
	 * http://www.w3.org/TR/NOTE-datetime
	 *
	 * @param date the date to check
	 * @return <tt>true</tt> if the date fits the standard, <tt>false</tt> 
	 *  otherwise
	 */
	private static boolean isValidDate(final String date) {
		StringTokenizer dateTokenizer = new StringTokenizer(date, "-");		

		
		// we require that the date have the year, month, and day
		if(dateTokenizer.countTokens() != 3) {
			return false;
		}
		String YYYYStr = dateTokenizer.nextToken();
		String MMStr   = dateTokenizer.nextToken();
		String DDStr   = dateTokenizer.nextToken();		
		try {
			int YYYY = Integer.parseInt(YYYYStr);
			int MM   = Integer.parseInt(MMStr);
			int DD   = Integer.parseInt(DDStr);
			
			// no one implemented HUGE before 2001 and no one likely will 
			// past 4000
			if(YYYY < 2001 || YYYY > 4000) return false;
			if(MM < 1 || MM > 12) return false;
			if(DD < 1 || DD > 31) return false;
		} catch(NumberFormatException e) {
			return false;
		}
		return true;
	}

	


	/**
	 * Creates a new <tt>Date</tt> instance from the date specified in
	 * the alternate location header.
	 *
	 * @param dateTimeString the extracted date-time string from the
	 *  alternate location header
	 * @return a new <tt>Date</tt> instance for the date specified in
	 *  the header, or a new <tt>Date</tt> instance of the oldest
	 *  possible date (according to the <tt>Date</tt> class) in the case 
	 *  where no timestamp data could be successfully created
	 */
	private static Date createDateInstance(final String dateTimeString) {
		StringTokenizer st = new StringTokenizer(dateTimeString, "T");
		int tokens = st.countTokens();
		if(tokens < 1 || tokens > 2) {
			return new Date(0);
		}
		String YYYYMMDD = st.nextToken();
		StringTokenizer stdate = new StringTokenizer(YYYYMMDD, "-");
		if(stdate.countTokens() != 3) {
			return new Date(0);
		}
		String YYYYStr = stdate.nextToken();
		String MMStr   = stdate.nextToken();
		String DDStr   = stdate.nextToken();
		int YYYY = Integer.parseInt(YYYYStr);

		try {
			// we subtract 1 because the Calendar class uses 0 for January,
			// whereas the ISO 8601 subset we're using uses 1 for January
			int MM = Integer.parseInt(MMStr)-1; 
			int DD = Integer.parseInt(DDStr);		
			
			if(!st.hasMoreTokens()) {
				Calendar cal = 
					Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				cal.set(YYYY, MM, DD);
				return cal.getTime();
			}
			String hhmmss = st.nextToken();
			if(!hhmmss.endsWith("Z")) {
				return new Date(0);
			}
			hhmmss = hhmmss.substring(0, hhmmss.length()-1);
			StringTokenizer sttime = new StringTokenizer(hhmmss, ":");
			int numToks = sttime.countTokens();
			if(numToks < 2 || numToks > 3) {
				return new Date(0);
			}
			String hhStr = sttime.nextToken();
			String mmStr = sttime.nextToken();
			int hh = Integer.parseInt(hhStr);
			int mm = Integer.parseInt(mmStr);
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			if(sttime.hasMoreTokens()) {
				String ssStr = sttime.nextToken().substring(0, 2);
				int ss = Integer.parseInt(ssStr);		
				cal.set(YYYY, MM, DD, hh, mm, ss);
			} else {
				cal.set(YYYY, MM, DD, hh, mm);
			}
			return cal.getTime();
		} catch(NumberFormatException e) {
			// a number could not be parsed, so consider it unstamped
			return new Date(0);
		}
	}

	/**
	 * Creates a new <tt>URL</tt> instance based on the URL specified in
	 * the alternate location header.
	 * 
	 * @param locationHeader the alternate location header from an HTTP
	 *  header
	 * @return a new <tt>URL</tt> instance for the URL in the alternate
	 *  location header
	 * @throws <tt>IOException</tt> if the url could not be extracted from
	 *  the header in the expected format
	 * @throws <tt>MalformedURLException</tt> if the enclosed URL is not
	 *  formatted correctly
	 */
	private static URL createUrl(final String locationHeader) 
		throws IOException {
		String test = locationHeader.toLowerCase();

		if(test.startsWith("http")) {
            String urlStr = AlternateLocation.removeTimestamp(locationHeader);
            URL url = new URL(urlStr);
            String host = url.getHost();
            int    port = url.getPort();
            if(host == null || host.equals("")) {
                throw new IOException("invalid host in alternate location: "+
                                      "host: "+host+"header: "+locationHeader);
            }
			// Handle bad merged alternate locations minus the spaces
			if(test.lastIndexOf("http://") > 4) {
                throw new IOException("messy alternate location: "+
                                      locationHeader);
			}
			// Handle bad ports in alternate locations 
			if((port & 0xFFFF0000) != 0) {
				throw new IOException("invalid port in alternate location: "+
									  "port: "+port+"header: "+locationHeader);
			}

            // check for private addresses if it appears to be in dotted quad 
            // format..
            if(Character.isDigit(host.charAt(0))) {
                InetAddress address = InetAddress.getByName(host);
                if(NetworkUtils.isPrivateAddress(address.getAddress())) {
                    throw new IOException("cannot include private address in "+
                                          "alt loc: "+host);
                } 
            }
            if(url.getPort()==-1)
                url = new URL("http",url.getHost(),80,url.getFile());
			return url;
		} else {
			// we could not understand the beginning of the alternate location
			// line
			throw new IOException("invalid start for alternate location: "+
								  locationHeader);
		}
	}

	/**
	 * Parses out the timestamp string from the alternate location
	 * header string, throwing an exception if there is any error.
	 *
	 * @param location the full alternate-location HTTP header string,
	 *  as specified in HUGE v0.93
	 * @return the date/time string from the the alternate location
	 *  header, or <tt>null</tt> if the date/time string could not
	 *  be extracted, is invalid, or does not exist
	 */
	private static String extractTimestamp(final String location) {
		StringTokenizer st = new StringTokenizer(location);
		int numToks = st.countTokens();
		String curTok = null;
		for(int i=0; i<numToks; i++) {
			curTok = st.nextToken();
		}
		if(AlternateLocation.isValidTimestamp(curTok)) {
			return curTok;
		} else {
			return null;
		}
	}

	/**
	 * Removes the timestamp from an alternate location header.  This will
	 * remove the timestamp from an alternate location header string that 
	 * includes the header name, or from an alternate location string that
	 * only contains the alternate location header value.
	 *
	 * @param locationHeader the string containing the full header, or only
	 *  the header value
	 * @return the same string as supplied in the <tt>locationHeader</tt> 
	 *  argument, but with the timestamp removed
	 */
	private static String removeTimestamp(final String locationHeader) {
		StringTokenizer st = new StringTokenizer(locationHeader);
		int numToks = st.countTokens();
		if(numToks == 1) {
			return locationHeader;
		}
		String curTok = null;
		for(int i=0; i<numToks; i++) {
			curTok = st.nextToken();
		}
		
		int tsIndex = locationHeader.indexOf(curTok);
		if(tsIndex == -1) return null;
		return locationHeader.substring(0, tsIndex);
	}


	/**
	 * Returns whether or not this <tt>AlternateLocation</tt> instance 
	 * includes a timestamp for when it was last known to be valid.
	 *
	 * @return <tt>true</tt> if this <tt>AlternateLocation</tt> includes
	 *  a timestamp, <tt>false</tt> otherwise
	 */
	public boolean isTimestamped() {
		return (OUTPUT_DATE_TIME != null);
	}

	/**
	 * Compares <tt>AlternateLocation</tt> instances by date.  This compares
	 * alternate locations by how "good" we think they are, based on
	 * their freshness.  So, an natural ordering will provide alternate
	 * locations from latest to earliest, as the more recent locations
	 * are the preferred locations.
	 *
	 * @param obj the <tt>Object</tt> instance to be compared
     * @return  the value <tt>0</tt> if the argument is an 
	 *  <tt>AlternateLocation</tt> with a timestamp equal to this 
	 *  <tt>AlternateLocation</tt>'s timestamp; a value less than <tt>0</tt> 
	 *  if the argument is an <tt>AlternateLocation</tt> with a timestamp 
	 *  before this <tt>AlternateLocation</tt>s timestamp; and a value greater 
	 *  than <tt>0</tt> if the argument is an <tt>AlternateLocation</tt> 
	 *  with a timestamp after the timestamp of this 
	 *  <tt>AlternateLocation</tt>
     * @exception <tt>ClassCastException</tt> if the argument is not an
     *  <tt>AlternateLocation</tt> 
	 * @see java.lang.Comparable
	 */
	public int compareTo(Object obj) {
		if(obj == this) return 0;
		//if(equals(obj)) return 0;		
		if(!(obj instanceof AlternateLocation)) return -1;
		
		AlternateLocation al = (AlternateLocation)obj;		
		if(URL.equals(al.URL)) {
		    return this.TIME < al.TIME ? 1 : this.TIME == al.TIME ? 0 : -1;
		} 
		if(isTimestamped() && al.isTimestamped()) {
			if(this.TIME == al.TIME) {
				return URL.toString().compareTo(al.URL.toString());
			}
			return (this.TIME<al.TIME ? 1 : -1);
		}
		if(isTimestamped()) {
			return -1;
		}
		if(al.isTimestamped()) {
			return 1;
		}

		// otherwise, niether location is timestamped and their URLs are 
		// not equal, so just return -1
		return -1;
	}

	/**
	 * Overrides the equals method to accurately compare 
	 * <tt>AlternateLocation</tt> instances.  <tt>AlternateLocation</tt>s 
	 * are equal if their <tt>URL</tt>s are equal.
	 *
	 * @param obj the <tt>Object</tt> instance to compare to
	 * @return <tt>true</tt> if the <tt>URL</tt> of this
	 *  <tt>AlternateLocation</tt> is equal to the <tt>URL</tt>
	 *  of the <tt>AlternateLocation</tt> location argument,
	 *  and otherwise returns <tt>false</tt>
	 */
	public boolean equals(Object obj) {
		if(obj == this) return true;
		if(!(obj instanceof AlternateLocation)) return false;
		AlternateLocation al = (AlternateLocation)obj;
		return URL == null ? al.URL == null : URL.equals(al.URL);
//		return ((OUTPUT_DATE_TIME == null ? al.OUTPUT_DATE_TIME == null :
//				 OUTPUT_DATE_TIME.equals(al.OUTPUT_DATE_TIME)) && 
//				(URL == null ? al.URL == null :
//				 URL.equals(al.URL)));
	}

	/**
	 * Creates a new <tt>RemoteFileDesc</tt> from this AlternateLocation
     *
	 * @param size the size of the file for the new <tt>RemoteFileDesc</tt> 
	 *  -- this is necessary to make sure the download bucketing works 
	 *  correctly
	 * @return new <tt>RemoteFileDesc</tt> based off of this, or 
	 *  <tt>null</tt> if the <tt>RemoteFileDesc</tt> could not be created
	 */
	public RemoteFileDesc createRemoteFileDesc(int size) {
		Set urnSet = new HashSet();
		urnSet.add(getSHA1Urn());
		return new RemoteFileDesc(URL.getHost(), URL.getPort(),
								  0, URL.getFile(), size,  
								  EMPTY_GUID, 1000,
								  true, 3, false, null, urnSet, false,
                                  false, //assume altLoc is not firewalled
                                  "ALT",//Never displayed, and we don't know
                                  getTime(),//not accurate, best we can do
                                  null);
	}

	/**
	 * Overrides the hashCode method of Object to meet the contract of 
	 * hashCode.  Since we override equals, it is necessary to also 
	 * override hashcode to ensure that two "equal" alternate locations
	 * return the same hashCode, less we unleash unknown havoc on the
	 * hash-based collections.
	 *
	 * @return a hash code value for this object
	 */
	public int hashCode() {
		if(hashCode == 0) {
		    hashCode = 37 * this.URL.hashCode();
//			int result = 17;
//          result = (37*result) + (int)(TIME^(TIME >>> 32));
//			result = (37*result) + this.URL.hashCode();
//			hashCode = result;
		}
		return hashCode;
	}

	/**
	 * Overrides toString to return a string representation of this 
	 * <tt>AlternateLocation</tt>, namely the url and the date.
	 *
	 * @return the string representation of this alternate location
	 */
	public String toString() {
		if(this.isTimestamped()) {
			return (this.URL.toExternalForm()+" "+OUTPUT_DATE_TIME);
		} else {
			return this.URL.toExternalForm();
		}
	}

	public String httpStringValue() {
		if(this.isTimestamped()) {
			return (this.URL.toExternalForm()+" "+OUTPUT_DATE_TIME);
		} else {
			return this.URL.toExternalForm();
		}		
	}
}









