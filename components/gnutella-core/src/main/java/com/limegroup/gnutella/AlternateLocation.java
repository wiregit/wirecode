package com.limegroup.gnutella;

import java.net.*;
import java.util.*;
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
public final class AlternateLocation implements Comparable {

	/**
	 * A <tt>URL</tt> instance for the URL specified in the header.
	 */
	private final URL URL;

	/**
	 * <tt>Date</tt> instance for conveniently making time comparisons
	 * betwween <tt>AlternateLocation</tt> instances.
	 */
	private final Date DATE;

	/**
	 * The <tt>String</tt> instance as it is written in the header.
	 */
	private final String OUTPUT_DATE_TIME;
	

	/**
	 * Constructs a new <tt>AlternateLocation</tt> instance based on the
	 * specified string argument.  
	 *
	 * @param LOCATION a string containing a single alternate location,
	 *  including a full URL for a file and an optional date
	 * @throws <tt>IOException</tt> if there is any problem constructing
	 *  the new instance from the specified string
	 */
	public AlternateLocation(final String LOCATION) throws IOException {
		try {
			URL = AlternateLocation.createUrl(LOCATION);
		} catch(MalformedURLException e) {
			throw new IOException("MALFORMED URL");
		}
		
		if(!AlternateLocation.isTimestamped(LOCATION)) {
			OUTPUT_DATE_TIME = null;		
			DATE = new Date(0);
		}
		else {
			// this can be null
			OUTPUT_DATE_TIME = AlternateLocation.extractDateTimeString(LOCATION);			
			DATE = AlternateLocation.createDateInstance(OUTPUT_DATE_TIME);
		}		
	}

	/**
	 * Returns the <tt>URL</tt> instance for this alternate location.
	 *
	 * @return the <tt>URL</tt> instance for this alternate location, which
	 *  is guaranteed to be non-null
	 */
	public URL getUrl() {
		// this is fine because the URL class is immutable
		return URL;
	}

	/**
	 * Returns the <tt>Date</tt> instance for this alternate location.
	 *
	 * @return the <tt>Date</tt> instance for this alternate location, or
	 *  <tt>null</tt> if this alternate location does not have a timestamp
	 */
	public Date getTimestamp() {
		// this is fine because the Date class is immutable
		return DATE;
	}

	/**
	 * Parses out the time-date string from the alternate location
	 * header string, throwing an exception if there is any error.
	 *
	 * @param LOCATION the full alternate-location HTTP header string,
	 *  as specified in HUGE v0.93
	 * @return the date/time string from the the alternate location
	 *  header, or <tt>null</tt> if the date/time string could not
	 *  be extracted or does not exist
	 */
	private static String extractDateTimeString(final String LOCATION) {
		if(!AlternateLocation.isTimestamped(LOCATION)) {
			return null;
		}
		int dateIndex = LOCATION.lastIndexOf(" ");
		if((dateIndex == -1) ||
		   ((dateIndex+1) >= LOCATION.length())) {
			return null;
		}
		return LOCATION.substring(dateIndex+1).trim(); 
	}

	/**
	 * Creates a new <tt>Date</tt> instance from the date specified in
	 * the alternate location header.
	 *
	 * @param DATE_TIME_STRING the extracted date-time string from the
	 *  alternate location header
	 * @return a new <tt>Date</tt> instance for the date specified in
	 *  the header, or a new <tt>Date</tt> instance of the oldest
	 *  possible date (according to the <tt>Date</tt> class) in the case 
	 *  where no timestamp data could be successfully created
	 */
	private static Date createDateInstance(final String DATE_TIME_STRING) {
		int dateTimeSepIndex = DATE_TIME_STRING.indexOf("T");

		// if there's no "T", or it f
		if((dateTimeSepIndex == -1) || 
		   ((dateTimeSepIndex+1) >= DATE_TIME_STRING.length())) {
			return new Date(0);
			//throw new IOException("INVALID DATE/TIME STRING");
		}
		String YYYYMMDD = DATE_TIME_STRING.substring(0, dateTimeSepIndex);
		String hhmmss = 
		    DATE_TIME_STRING.substring(dateTimeSepIndex+1, 
									   DATE_TIME_STRING.length()-1);
		StringTokenizer stdate = new StringTokenizer(YYYYMMDD, "-");
		if(stdate.countTokens() != 3) {
			return null;
			//throw new IOException("INVALID DATE FORMAT");
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
			//throw new IOException("INVALID TIME FORMAT");
		}
		String hhStr = sttime.nextToken();
		String mmStr = sttime.nextToken();
		String ssStr = sttime.nextToken();
		int hh = Integer.parseInt(hhStr);
		int mm = Integer.parseInt(mmStr);
		int ss = Integer.parseInt(ssStr);		

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.clear();
		cal.set(YYYY, MM, DD, hh, mm, ss);
		return cal.getTime();
	}

	/**
	 * Creates a new <tt>URL</tt> instance based on the URL specified in
	 * the alternate location header.
	 * 
	 * @param LOCATION_HEADER the alternate location header from an HTTP
	 *  header
	 * @return a new <tt>URL</tt> instance for the URL in the alternate
	 *  location header
	 * @throws <tt>IOException</tt> if the url could not be extracted from
	 *  the header in the expected format
	 * @throws <tt>MalformedURLException</tt> if the enclosed URL is not
	 *  formatted correctly
	 */
	private static URL createUrl(final String LOCATION_HEADER) 
		throws IOException {
		String test = LOCATION_HEADER.toLowerCase();
		String urlString;		
		if(!test.startsWith("http")) {
			int colonIndex  = LOCATION_HEADER.indexOf(":");
			if(colonIndex == -1) {
				throw new IOException("ERROR EXTRACTING URL STRING");
			}
			if(!AlternateLocation.isTimestamped(LOCATION_HEADER)) {				
				urlString = LOCATION_HEADER.substring(colonIndex+1);
			}
			else {
				int dateIndex = LOCATION_HEADER.lastIndexOf(" ");
				if(dateIndex == -1) {
					throw new IOException("ERROR EXTRACTING URL DATE");
				}
				urlString = LOCATION_HEADER.substring(colonIndex+1, dateIndex);
			}
		} else {
			if(AlternateLocation.isTimestamped(LOCATION_HEADER)) {
				int dateIndex = LOCATION_HEADER.lastIndexOf(" ");
				urlString = LOCATION_HEADER.substring(0, dateIndex);
			}
			else {
				urlString = LOCATION_HEADER;
			}
		}

		// get rid of any surrounding whitespace
		return new URL(urlString.trim());
	}

	/**
	 * Convenience method for checking whether or not the specified 
	 * alternate location header is timestamped.
	 *
	 * @param LOCATION_HEADER the header to check for a timestamp
	 * @return <tt>true</tt> if the header has a timestamp, <tt>false</tt>
	 *  otherwise
	 */
	private static boolean isTimestamped(final String LOCATION_HEADER) {
		int dateIndex = LOCATION_HEADER.lastIndexOf(" ");
		if(dateIndex == -1) {
			return false;
		}
		else if((LOCATION_HEADER.length()-dateIndex) > 20) {
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
	 * Returns whether this <tt>AlternateLocation</tt> instance is
	 * older than the <tt>AlternateLocation</tt> argument.
	 * 
	 * @return <tt>true</tt> if the date for this <tt>AlternateLocation</tt>
	 *  is older than the date for the <tt>AlternateLocation</tt>
	 *  argument, and otherwise returns <tt>false</tt> 
	 */
	public boolean isOlderThan(AlternateLocation loc) {
		return this.getTimestamp().before(loc.getTimestamp());
	}

	/**
	 * Returns whether this <tt>AlternateLocation</tt> instance is
	 * newer than the <tt>AlternateLocation</tt> argument.
	 * 
	 * @return <tt>true</tt> if the date for this <tt>AlternateLocation</tt>
	 *  is newer than the date for the <tt>AlternateLocation</tt>
	 *  argument, and otherwise returns <tt>false</tt> 
	 */
	public boolean isNewerThan(AlternateLocation loc) {
		return this.getTimestamp().after(loc.getTimestamp());
	}

	/**
	 * Compares <tt>AlternateLocation</tt> instances by date.  
	 *
	 * @param obj the <tt>Object</tt> instance to be compared
     * @return  the value <tt>0</tt> if the argument is an 
	 *  <tt>AlternateLocation</tt> with a timestamp equal to this 
	 *  <tt>AlternateLocation</tt>'s timestamp; a value less than <tt>0</tt> 
	 *  if the argument is an <tt>AlternateLocation</tt> with a timestamp 
	 *  after this <tt>AlternateLocation</tt>s timestamp; and a value greater 
	 *  than <tt>0</tt> if the argument is an <tt>AlternateLocation</tt> 
	 *  with a timestamp before the timestamp of this 
	 *  <tt>AlternateLocation</tt>
     * @exception <tt>ClassCastException</tt> if the argument is not an
     *  <tt>AlternateLocation</tt> 
	 * @see java.lang.Comparable
	 */
	public int compareTo(Object obj) {
		if(this.equals(obj)) return 0;
		AlternateLocation al = (AlternateLocation)obj;		
		long thisTime    = DATE.getTime();
		long anotherTime = al.getTimestamp().getTime();
		return (thisTime<anotherTime ? 1 : (thisTime==anotherTime ? 0 : -1));
	}

	/**
	 * Overrides the equals method to accurately compare 
	 * <tt>AlternateLocation</tt> instances.  <tt>AlternateLocation</tt>s 
	 * are equal if their <tt>URL</tt>s and <tt>Date</tt>s are equal.
	 *
	 * @param obj the <tt>Object</tt> instance to compare to
	 * @return <tt>true</tt> if the <tt>URL</tt> and <tt>Date</tt> of this
	 *  <tt>AlternateLocation</tt> are equal to the <tt>URL</tt> and 
	 *  <tt>Date</tt> of the <tt>AlternateLocation</tt> location argument,
	 *  and otherwise returns <tt>false</tt>
	 */
	public boolean equals(Object obj) {
		if(obj == this) return true;
		if(!(obj instanceof AlternateLocation)) return false;
		AlternateLocation al = (AlternateLocation)obj;
		Date date = al.getTimestamp();
		URL url = al.getUrl();
		if(al.isTimestamped() && this.isTimestamped()) {
			if(date.compareTo(DATE) != 0) {
				return false;
			}
		}
		else if(!(!al.isTimestamped() && !this.isTimestamped())) {
			return false;
		}
		
		return this.URL.equals(url);
	}

	/**
	 * Overrides toString to return a string representation of this 
	 * <tt>AlternateLocation</tt>, namely the url and the date.
	 *
	 * @return the string representation of this alternate location
	 */
	public String toString() {
		if(this.isTimestamped()) {
			return (this.URL.toString()+" "+OUTPUT_DATE_TIME);
		} else {
			return this.URL.toString();
		}
	}

	/*
	public static void main(String[] args) {
		System.out.println("TESTING VALID ALTERNATE LOCATION CONSTRUCTION..."); 
		String[] validlocs = {
			"Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg "+
			"2002-04-09T20:32:33Z",
			"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg "+
			"2002-04-09T20:32:33Z",
			"Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg",
			"X-Gnutella-Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg",
			"http://Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg",
			"http: //Y.Y.Y.Y:6352/get/2/"+
			"lime%20capital%20management%2001.mpg"
		};
		try {
			for(int i=0; i<validlocs.length; i++) {
				AlternateLocation al = new AlternateLocation(validlocs[i]);
				System.out.println(al); 
			}
			System.out.println("TEST PASSED"); 
		} catch(IOException e) {
			System.out.println("TEST FAILED WITH EXCEPTION: "); 
			e.printStackTrace();
		}		
	}
	*/
	
}









