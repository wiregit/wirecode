package com.limegroup.gnutella;

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
	 * Cached hash code that is lazily initialized.
	 */
	private volatile int hashCode = 0;
	

	/**
	 * Constructs a new <tt>AlternateLocation</tt> instance based on the
	 * specified string argument.  
	 *
	 * @param location a string containing a single alternate location,
	 *  including a full URL for a file and an optional date
	 * @throws <tt>IOException</tt> if there is any problem constructing
	 *  the new instance from the specified string
	 */
	public AlternateLocation(final String location) throws IOException {
		try {
			URL = AlternateLocation.createUrl(location);
		} catch(MalformedURLException e) {
			throw new IOException("MALFORMED URL");
		}
		
		if(!AlternateLocation.isTimestamped(location)) {
			OUTPUT_DATE_TIME = null;		
			
			// just set the time to be as old as possible since there's
			// no date information -- this makes comparisons easier
			TIME = new Date(0).getTime();
		}
		else {
			// this can be null
			OUTPUT_DATE_TIME = AlternateLocation.extractDateTimeString(location);			
			Date date = AlternateLocation.createDateInstance(OUTPUT_DATE_TIME);
			TIME = date.getTime();
		}		
	}

	/**
	 * Creates a new <tt>AlternateLocation</tt> instance for the givel 
	 * <tt>URL</tt> instance.  This constructor creates an alternate
	 * location with the current date and time as its timestamp.
	 * This can be used, for example, for newly uploaded files.
	 *
	 * @param url the <tt>URL</tt> instance for the resource
	 * @throws <tt>MalformedURLException</tt> if a <tt>URL</tt> instance
	 *  could not be succussfully constructed from the supplied arguments
	 */
	public AlternateLocation(final URL url) 
		throws MalformedURLException {
		// create a new URL instance from the data for the given url
		// and the urn
		this.URL = new URL(url.getProtocol(), url.getHost(), url.getPort(),
						   url.getFile());
		// make the date the current time
		Date date = new Date();
		TIME = date.getTime();
		this.OUTPUT_DATE_TIME = AlternateLocation.convertDateToString(date);
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
	 * Converts the specified <tt>Date</tt> instance to a <tt>String</tt> 
	 * that fits the syntax specified in the ISO 8601 subset we're using,
	 * discussed at: http://www.w3.org/TR/NOTE-datetime.
	 *
	 * @param date the <tt>Date</tt> instance to convert
	 * @return a new <tt>String</tt> instance that matches the standard
	 *  syntax
	 */
	public static String convertDateToString(Date date) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(date);
		int[]    dateInts = new int[6];
		String[] dateStrs = new String[6];
		dateInts[0] = cal.get(Calendar.YEAR);
		dateInts[1] = cal.get(Calendar.MONTH);
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
		return (dateStrs[0]+DASH+
				dateStrs[1]+DASH+
				dateStrs[2]+"T"+
				dateStrs[3]+COLON+
				dateStrs[4]+COLON+
				dateStrs[5]+"Z");
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
	public static boolean isValidDate(final String date) {
		int length = date.length();
		final String DASH = "-";
		final String COLON = ":";
		// if the date length is less than ten, then the date does not contain
		// information for the day, as defined in the subset of ISO 8601, as 
	    // defined at: http://www.w3.org/TR/NOTE-datetime, and we consider it
		// itvalid
		if(length < 10) return false;

		// if the date is not one of our valid lengths, return false
		// this requires that the date use UTC time, as designated by the
		// trailing "Z"
		if((length != 10) && (length != 17) && (length != 20) && (length != 22)) {
			return false;
		}
		// the date must be in this millenium
		if(!date.startsWith("2")) {
			return false;
		}
		int firstDashIndex  = date.indexOf(DASH);
		int secondDashIndex = date.indexOf(DASH, firstDashIndex+1);
		if((firstDashIndex == -1) || (secondDashIndex == -1)) {
			return false;
		}

		if(length == 10) return true;
 
		int firstColonIndex  = date.indexOf(COLON);
		int secondColonIndex = date.indexOf(COLON, firstColonIndex+1);
		if((firstColonIndex != 13) || (secondColonIndex != 16)) {
			return false;
		}
		return true;
	}

	/**
	 * Parses out the time-date string from the alternate location
	 * header string, throwing an exception if there is any error.
	 *
	 * @param location the full alternate-location HTTP header string,
	 *  as specified in HUGE v0.93
	 * @return the date/time string from the the alternate location
	 *  header, or <tt>null</tt> if the date/time string could not
	 *  be extracted, is invalid, or does not exist
	 */
	private static String extractDateTimeString(final String location) {
		if(!AlternateLocation.isTimestamped(location)) {
			return null;
		}
		int dateIndex = location.lastIndexOf(" ");
		if((dateIndex == -1) ||
		   ((dateIndex+1) >= location.length())) {
			return null;
		}
		String dateTimeString = location.substring(dateIndex+1).trim(); 
		if(AlternateLocation.isValidDate(dateTimeString)) {
			return dateTimeString; 
		} else {
			return null;
		}
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
		int dateTimeSepIndex = dateTimeString.indexOf("T");

		// if there's no "T", or it f
		if((dateTimeSepIndex == -1) || 
		   ((dateTimeSepIndex+1) >= dateTimeString.length())) {
			return new Date(0);
		}
		String YYYYMMDD = dateTimeString.substring(0, dateTimeSepIndex);
		String hhmmss = 
		    dateTimeString.substring(dateTimeSepIndex+1, 
									 dateTimeString.length()-1);
		StringTokenizer stdate = new StringTokenizer(YYYYMMDD, "-");
		if(stdate.countTokens() != 3) {
			return null;
		}
		String YYYYStr = stdate.nextToken();
		String MMStr   = stdate.nextToken();
		String DDStr   = stdate.nextToken();
		int YYYY = Integer.parseInt(YYYYStr);

		// we subtract 1 because the Calendar class uses 0 for January,
		// whereas the ISO 8601 subset we're using uses 1 for January
		int MM   = Integer.parseInt(MMStr)-1; 
		int DD   = Integer.parseInt(DDStr);		
 
		StringTokenizer sttime = new StringTokenizer(hhmmss, ":");
		if(sttime.countTokens() != 3) {
			return new Date(0);
		}
		String hhStr = sttime.nextToken();
		String mmStr = sttime.nextToken();
		String ssStr = sttime.nextToken();
		int hh = Integer.parseInt(hhStr);
		int mm = Integer.parseInt(mmStr);
		int ss = Integer.parseInt(ssStr);		

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.set(YYYY, MM, DD, hh, mm, ss);
		return cal.getTime();
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
		String urlString;		
		if(!test.startsWith("http")) {
			int colonIndex  = locationHeader.indexOf(":");
			if(colonIndex == -1) {
				throw new IOException("ERROR EXTRACTING URL STRING");
			}
			if(!AlternateLocation.isTimestamped(locationHeader)) {				
				urlString = locationHeader.substring(colonIndex+1);
			}
			else {
				int dateIndex = locationHeader.lastIndexOf(" ");
				if(dateIndex == -1) {
					throw new IOException("ERROR EXTRACTING URL DATE");
				}
				urlString = locationHeader.substring(colonIndex+1, dateIndex);
			}
		} else {
			if(AlternateLocation.isTimestamped(locationHeader)) {
				int dateIndex = locationHeader.lastIndexOf(" ");
				urlString = locationHeader.substring(0, dateIndex);
			}
			else {
				urlString = locationHeader;
			}
		}

		// get rid of any surrounding whitespace
		return new URL(urlString.trim());
	}

	/**
	 * Convenience method for checking whether or not the specified 
	 * alternate location header is timestamped.
	 *
	 * @param locationHeader the header to check for a timestamp
	 * @return <tt>true</tt> if the header has a timestamp, <tt>false</tt>
	 *  otherwise
	 */
	public static boolean isTimestamped(final String locationHeader) {
		int dateIndex = locationHeader.lastIndexOf(" ");
		if(dateIndex == -1) {
			return false;
		}
		else if((locationHeader.length()-dateIndex) > 21) {
			return false;
		}
		return true;
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
		if(equals(obj)) return 0;
		if(obj == this) return 0;
		if(!(obj instanceof AlternateLocation)) return -1;				
		AlternateLocation al = (AlternateLocation)obj;		
		if(URL.equals(al.URL)) {
			return (this.TIME<al.TIME ? 1 : (this.TIME==al.TIME ? 0 : -1));
		}
		if(isTimestamped() && al.isTimestamped()) {
			return (this.TIME<al.TIME ? 1 : (this.TIME==al.TIME ? 0 : -1));
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
	 * are equal if their <tt>URL</tt>s and timestamps are equal.
	 *
	 * @param obj the <tt>Object</tt> instance to compare to
	 * @return <tt>true</tt> if the <tt>URL</tt> and timestamp of this
	 *  <tt>AlternateLocation</tt> are equal to the <tt>URL</tt> and 
	 *  timestamp of the <tt>AlternateLocation</tt> location argument,
	 *  and otherwise returns <tt>false</tt>
	 */
	public boolean equals(Object obj) {
		if(obj == this) return true;
		if(!(obj instanceof AlternateLocation)) return false;
		AlternateLocation al = (AlternateLocation)obj;		
		return ((OUTPUT_DATE_TIME == null ? al.OUTPUT_DATE_TIME == null :
				 OUTPUT_DATE_TIME.equals(al.OUTPUT_DATE_TIME)) && 
				(URL == null ? al.URL == null :
				 URL.equals(al.URL)));
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
			int result = 17;
			result = (37*result) + (int)(TIME^(TIME >>> 32));
			result = (37*result) + this.URL.hashCode();
			hashCode = result;
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

	/**
	 * A factory method for creating new <tt>Comparator</tt> instances 
	 * for comparing alternate locations.
	 *
	 * @return a new <tt>Comparator</tt> instance for comparing
	 * alternate locations
	 */
	/*
	public static com.sun.java.util.collections.Comparator createComparator() {
		return new AlternateLocationsComparator();
	}
	*/
	

	/**
	 * Private class for comparing <tt>AlternateLocation</tt> instances.
	 */
	/*
	private static class AlternateLocationsComparator 
		implements com.sun.java.util.collections.Comparator {
		public int compare(Object obj1, Object obj2) {
			return ((AlternateLocation)obj1).compareTo(obj2);
		}
		
		public boolean equals(Object obj) {
			return this.equals(obj);
		}
	} 
	*/
}









