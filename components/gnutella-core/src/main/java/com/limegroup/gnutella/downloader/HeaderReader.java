package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.SocketOpener;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.util.CommonUtils;
import java.util.StringTokenizer;

/**
 * Responsible for parsing the HTTP header information for
 * the downloader.  Maintains the parsed information, so there
 * should be one instance of this class per header.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class HeaderReader {

	/** reads the headers off the socket */
	private ByteReader _byteReader; 
	/** the total size of the file */ 
	private int _fileSize;             
	/** the first byte to be read */
	private int _initialReadingPoint;
	/** the length of the section being downloaded */
	private int _contentLength;        

	/**
	 * The constructor initializes the byteReader, and throws
	 * an exception if byteReader is null.  The bytereader
	 * is reading off of the socket in HTTPDownloader.
	 */
	public HeaderReader(ByteReader byteReader) throws IOException {
		_byteReader = byteReader;
		if (_byteReader == null) 
			throw new ReaderIsNullException();
		readHeader();
	}

	/**
	 * returns the content length, ie the number passed in the 
	 * Content-length header.  
	 *
	 * Example: 
	 * Content-range: bytes 9-99/100
	 * Content-length: 90
	 * this method returns 90
	 *
	 * @return if the Content-length header was not sent, this
	 *         value is undefined.
	 */
	public int getContentLength() { 
		return _contentLength;
	}

	/**
	 * returns the size of the file.  This number is passed in 
	 * either the Content-range header.  
	 *
	 * Example: 
	 * Content-range: bytes 9-99/100
	 * Content-length: 90
	 * this method returns 100
	 *
	 * @return if the Content-range header was not sent, this
	 *         value is undefined.
	 */
	public int getFileSize() { 
		return _fileSize;
	}

	/**
	 * returns the index of the first byte to be read.  this
	 * is the first number passed in the Content-range header.
	 * 
	 * Example: 
	 *Content-range: bytes 0-99/100
	 * this method returns 0;
	 *
	 * @return if the Content-range header was not sent, this
	 *         value is undefined.
	 */
	public int getStart() {
		return _initialReadingPoint;
	}
	
	/** 
	 * Reads the HTTP Header information.  this method must be
	 * called before getting any of the results from the headers.
	 * If there are any problems reading the headers, an IOException
	 * is thrown.
	 */
	private void readHeader() throws IOException {
		
		String str; // = " ";
		// Read the first line and then check for any possible errors
		str = _byteReader.readLine();  
		if (str==null || str.equals(""))
			return;
		
		parseConnect(str);

		// if we've gotten this far, then we can assume that we should
		// be alright to prodeed.

		while (true) {

			// convert to all uppercase
			str = str.toUpperCase();

			if (str.indexOf("CONTENT-LENGTH:") != -1)  
				parseContentLength(str);

			if (str.indexOf("CONTENT-RANGE:") != -1)  
				parseContentRange(str);
			
			str = _byteReader.readLine();
			
			//EOF?
			if (str==null || str.equals(""))
				break;
		}
		
		
	}

	///////////////// PRIVATE PARSING METHODS //////////////////

	/**
	 * Parses the connect header. 
	 * str should be some sort of HTTP connect string.
	 * The string should look like:	
	 *  str = "HTTP 200 OK \r\n";
	 *  We will accept any 2xx's, but reject other codes.
	 */
	private void parseConnect(String str) throws IOException {
		// create a new String tokenizer with the space as the 
		// delimeter.		
		StringTokenizer tokenizer = new StringTokenizer(str, " ");
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new NoHTTPOKException();

		token = tokenizer.nextToken();
		
		// the first token should contain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new NoHTTPOKException();
		
		// the next token should be a number
		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new NoHTTPOKException();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		int code;
		try {
			code = java.lang.Integer.parseInt(num);
		} catch (NumberFormatException e) {
			throw new ProblemReadingHeaderException();
		}
		
		// accept anything that is 2xx
		if ( (code < 200) || (code > 300) ) {
			if (code == 404)
				throw new 
				    com.limegroup.gnutella.downloader.FileNotFoundException();
			else if (code == 410)
				throw new 
                    com.limegroup.gnutella.downloader.NotSharingException();
			else if (code == 503)
				throw new TryAgainLaterException();
			// a general catch for 4xx and 5xx's
			// should maybe be a different exception?
			// else if ( (code >= 400) && (code < 600) ) 
			else 
				throw new IOException();
			
		}
	}

	/** 
	 * Parses the Content-range header.  This method
	 * assumes that the string representation of the
	 * header has already been converted to all uppercase.
	 * 
	 * Many clients send an equal sign after 'bytes'.  This
	 * is technically not valid HTTP, but we accept either.
	 *
	 * Also, full HTTP would support sending a '*' in place of
	 * some of the numbers.  We don't support this.
	 *
	 * The rfc defines the Content-Range header as follows:
	 * 
	 * Content-Range = "Content-Range" ":" content-range-spec
	 *   content-range-spec      = byte-content-range-spec
	 *   byte-content-range-spec = bytes-unit SP
	 *   byte-range-resp-spec "/"
	 *   ( instance-length | "*" )
	 *   byte-range-resp-spec = (first-byte-pos "-" last-byte-pos)
	 *   | "*"
	 *   instance-length           = 1*DIGIT
	 *
	 * Examples of byte-content-range-spec values, 
	 * assuming that the entity contains a total of 1234 bytes: 
	 *        . The first 500 bytes:
	 *         bytes 0-499/1234
	 *
	 *        . The second 500 bytes:
	 *         bytes 500-999/1234
	 *
	 *        . All except for the first 500 bytes:
	 *         bytes 500-1233/1234
	 *			
	 *        . The last 500 bytes:
	 *         bytes 734-1233/1234
	 */
	private void parseContentRange(String str) throws IOException {
		int dash;
		int slash;
		
		String beforeDash;
		int numBeforeDash;
			
		String afterSlash;
		int numAfterSlash;
		
		String beforeSlash;
		int numBeforeSlash;
		
		int after_bytes; 
		
		try {
			after_bytes = str.indexOf("BYTES") + 5;
			str = str.substring(after_bytes);
			
			dash=str.indexOf('-');
			slash = str.indexOf('/');
			
			afterSlash = str.substring(slash+1);
			afterSlash = afterSlash.trim();
			
			beforeDash = str.substring(0, dash);
			beforeDash = beforeDash.replace('=', ' ');
			beforeDash = beforeDash.trim();
			
			beforeSlash = str.substring(dash+1, slash);
			beforeSlash = beforeSlash.trim();
		} catch (IndexOutOfBoundsException e) {
			throw new ProblemReadingHeaderException();
		}
		try {
			numAfterSlash = java.lang.Integer.parseInt(afterSlash);
			numBeforeDash = java.lang.Integer.parseInt(beforeDash);
			numBeforeSlash = java.lang.Integer.parseInt(beforeSlash);
		}
		catch (NumberFormatException e) {
			throw new ProblemReadingHeaderException();
		}
		
		// In order to be backwards compatible with
		// LimeWire 0.5, which sent broken headers like:
		// Content-range: bytes=1-67818707/67818707
		//
		// If the number preceding the '/' is equal 
		// to the number after the '/', then we want
		// to decrement the first number and the number
		// before the '/'.
		if (numBeforeSlash == numAfterSlash) {
			numBeforeDash--;
			numBeforeSlash--;
		}
		
		_initialReadingPoint = numBeforeDash;
		_fileSize = numAfterSlash;
		
	} 

	/** 
	 * Parses the Content-Length header.  This method
	 * assumes that the string representation of the
	 * header has already been converted to all uppercase.
	 * 
	 * The rfc defines the Content-Length header as follows:
	 *
	 * Content-Length    = "Content-Length" ":" 1*DIGIT
	 * An example is 
	 * Content-Length: 3495
	 */
	private void parseContentLength(String str) throws IOException {
		int colon = str.indexOf(":");
		String digit = str.substring(colon+1);			 
		digit = digit.trim();
		int tempSize;
		try {
			tempSize = java.lang.Integer.parseInt(digit);
		}
		catch (NumberFormatException e) {
			throw new ProblemReadingHeaderException();
		}
		_contentLength = tempSize;
	}
	
	
	/////////////////////////// UNIT TEST  /////////////////////////////////

	//  private HeaderReader(String str) {
//  		ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
//    		_byteReader = new ByteReader(stream);
//  	}

//  	public static void main(String[] argc) {
		
//  		String str = " ";

//  		HeaderReader hreader = new HeaderReader(str);
//  		try {
//  			str = "CONTENT-LENGTH: 500\r\n";
//  			try {hreader.readHeader();}
//  			catch (IOException e) {}
//  			hreader.parseContentLength(str);
//  			Assert.that(hreader.getContentLength() == 500);
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		try {
//  			str = "CONTENT-RANGE: BYTES=0-299/300\r\n";
//  			try {hreader.readHeader();}
//  			catch (IOException ee) {}
//  			hreader.parseContentRange(str);
//  			Assert.that(hreader.getFileSize() == 300);
//  			Assert.that(hreader.getStart() == 0);
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		try {
//  			str = "CONTENT-RANGE: BYTES 0-599/600\r\n";
//  			try {hreader.readHeader();}
//  			catch (IOException eee) {}
//  			hreader.parseContentRange(str);
//  			Assert.that(hreader.getFileSize() == 600);
//  			Assert.that(hreader.getStart() == 0);
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		try {
//  			str =  "HTTP 200 OK\r\nUser-Agent: LimeWire\r\nContent-range: bytes=9-99/100\r\nContent-Length: 90\r\n\r\n";
//  			hreader = new HeaderReader(str);
//  			hreader.readHeader();
//  			Assert.that(hreader.getFileSize() == 100);
//  			Assert.that(hreader.getContentLength() == 90);
//  			Assert.that(hreader.getStart() == 9);
//  		} catch (IOException e) {
//  			Assert.that(false);

//  		}
	

//  	}

	

}
