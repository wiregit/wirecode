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
public final class AlternateLocation {

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
		
		// this can be null
		OUTPUT_DATE_TIME = AlternateLocation.extractDateTimeString(LOCATION);

		// this can be null
		DATE = AlternateLocation.createDateInstance(OUTPUT_DATE_TIME);
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
	 *  the header, or <tt>null</tt> if the date could not be extracted
	 */
	private static Date createDateInstance(final String DATE_TIME_STRING) {
		int dateTimeSepIndex = DATE_TIME_STRING.indexOf("T");

		// if there's no "T", or it f
		if((dateTimeSepIndex == -1) || 
		   ((dateTimeSepIndex+1) >= DATE_TIME_STRING.length()) ||
		   (DATE_TIME_STRING.length() != 20)) {
			return null;
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
			return null;
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
			int urlIndex  = LOCATION_HEADER.indexOf(":");
			int dateIndex = LOCATION_HEADER.lastIndexOf(" ");
			if((urlIndex == -1) ||
			   (urlIndex == dateIndex) ||
			   ((dateIndex+1) >= LOCATION_HEADER.length())) {
				   throw new IOException("ERROR EXTRACTING URL STRING");
			   }
			urlString = LOCATION_HEADER.substring(urlIndex+1, dateIndex);
		} else {
			urlString = LOCATION_HEADER;
		}

		// get rid of any surrounding whitespace
		return new URL(urlString.trim());
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
		if(((date == null) && (DATE != null)) ||
		   ((date != null) && (DATE == null))) {
			return false;
		}
		else if(((date == null) && (DATE == null)) &&
				(this.URL.equals(url))) {
			return true;
		}
		return (this.DATE.equals(date) ||
				this.URL.equals(url));
	}

	/**
	 * Overrides toString to return a string representation of this 
	 * <tt>AlternateLocation</tt>, namely the url and the date.
	 *
	 * @return the string representation of this alternate location
	 */
	public String toString() {
		return this.URL.toString()+" "+OUTPUT_DATE_TIME;
	}

	
	/*
	public static void main(String[] args) {
		String alt = "Alt-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z";
		try {
			AlternateLocation al = new AlternateLocation(alt);
			//System.out.println(al.getTimestamp()); 
			System.out.println(al); 
		} catch(IOException e) {
			e.printStackTrace();
		}		
	}
	*/
	
}









