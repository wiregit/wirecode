package com.limegroup.gnutella.uploader;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationCollector;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.http.*;


import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.LinkedList;
import com.sun.java.util.collections.List;
import com.sun.java.util.collections.Map;
import com.sun.java.util.collections.Set;

/**
 * tests the handling of Firewalled alternate locations on the uploader side.
 */
public class PushLocTest extends BaseTestCase {

	private static final int PORT = 6668;

	/** The file name, plain and encoded. */
	private static String testDirName = "com/limegroup/gnutella/uploader/data";

	private static String incName = "partial alphabet.txt";

	private static String fileName = "alphabet test file#2.txt";

	private static String encodedFile = "alphabet%20test+file%232.txt";

	/** The file contents. */
	private static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

	/** The hash of the file contents. */
	private static final String baseHash = "GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";

	private static final String hash = "urn:sha1:" + baseHash;

	private static final String badHash = "urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2SAM";

	private static final String incompleteHash = "urn:sha1:INCOMCPLETEXBSQEZY37FIM5QQSA2OUJ";

	private static final int index = 0;

	/** Our listening port for pushes. */
	private static final int callbackPort = 6671;

	private UploadManager upMan;

	/** The verifying file for the shared incomplete file */
	private static final VerifyingFile vf = new VerifyingFile(false, 252450);

	/** The filedesc of the shared file. */
	private FileDesc FD;

	/** The root32 of the shared file. */
	private String ROOT32;

	private static final RouterService ROUTER_SERVICE = new RouterService(
			new FManCallback());

	private static final Object loaded = new Object();
	
	private static String FAWTFeatures, FWAWTFeatures;

	public PushLocTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(PushLocTest.class);
	}

	protected void setUp() throws Exception {
		FilterSettings.BLACK_LISTED_IP_ADDRESSES
				.setValue(new String[] { "*.*.*.*" });
		FilterSettings.WHITE_LISTED_IP_ADDRESSES
				.setValue(new String[] { "127.*.*.*" });
		ConnectionSettings.PORT.setValue(PORT);

		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt");
		UploadSettings.HARD_MAX_UPLOADS.setValue(10);
		UploadSettings.UPLOADS_PER_PERSON.setValue(10);

		FilterSettings.FILTER_DUPLICATES.setValue(false);

		ConnectionSettings.NUM_CONNECTIONS.setValue(8);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);

		File testDir = CommonUtils.getResourceFile(testDirName);
		assertTrue("test directory could not be found", testDir.isDirectory());
		File testFile = new File(testDir, fileName);
		assertTrue("test file should exist", testFile.exists());
		File sharedFile = new File(_sharedDir, fileName);
		// we must use a seperate copy method
		// because the filename has a # in it which can't be a resource.
		copyFile(testFile, sharedFile);
		assertTrue("should exist", new File(_sharedDir, fileName).exists());

		if (!RouterService.isStarted()) {
			startAndWaitForLoad();
			Thread.sleep(2000);
		}

		assertEquals("ports should be equal", PORT, ConnectionSettings.PORT
				.getValue());

		upMan = RouterService.getUploadManager();

		FileManager fm = RouterService.getFileManager();
		File incFile = new File(_incompleteDir, incName);
		CommonUtils.copyResourceFile(testDirName + "/" + incName, incFile);
		URN urn = URN.createSHA1Urn(incompleteHash);
		Set urns = new HashSet();
		urns.add(urn);
		fm.addIncompleteFile(incFile, urns, incName, 1981, vf);
		assertEquals(1, fm.getNumIncompleteFiles());
		assertEquals(1, fm.getNumFiles());
		FD = fm.getFileDescForFile(new File(_sharedDir, fileName));
		while (FD.getHashTree() == null)
			Thread.sleep(300);
		ROOT32 = FD.getHashTree().getRootHash();
		// remove all alts for clarity.
		List alts = new LinkedList();
		AlternateLocationCollection alc = FD.getAlternateLocationCollection();
		for (Iterator i = alc.iterator(); i.hasNext();)
			alts.add((AlternateLocation) i.next());
		for (Iterator i = alts.iterator(); i.hasNext();) {
			AlternateLocation al = (AlternateLocation) i.next();
			alc.remove(al); // demote
			alc.remove(al); // remove
		}

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
		}

		assertEquals("unexpected uploads in progress", 0, upMan
				.uploadsInProgress());
		assertEquals("unexpected queued uploads", 0, upMan
				.getNumQueuedUploads());

		// clear the cache history so no banning occurs.
		Map requests = (Map) PrivilegedAccessor.getValue(upMan, "REQUESTS");
		requests.clear();
		
		FAWTFeatures= HTTPHeaderName.FEATURES+
			": "+ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE.httpStringValue();
		FWAWTFeatures= HTTPHeaderName.FEATURES+
			": "+ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE.httpStringValue();
	}

	private static void copyFile(File one, File two) throws Exception {
		FileInputStream fis = new FileInputStream(one);
		FileOutputStream fos = new FileOutputStream(two);
		int read = fis.read();
		while (read != -1) {
			fos.write(read);
			read = fis.read();
		}
	}

	/**
	 * Reads a new line WITHOUT end of line characters. A line is defined as a
	 * minimal sequence of character ending with "\n", with all "\r"'s thrown
	 * away. Hence calling readLine on a stream containing "abc\r\n" or
	 * "a\rbc\n" will return "abc".
	 * 
	 * Throws IOException if there is an IO error. Returns null if there are no
	 * more lines to read, i.e., EOF has been reached. Note that calling
	 * readLine on "ab <EOF>" returns null.
	 */
	public static String readLine(Reader _istream) throws IOException {
		if (_istream == null)
			return "";

		StringBuffer sBuffer = new StringBuffer();
		int c = -1; //the character just read
		boolean keepReading = true;

		do {
			try {
				c = _istream.read();
			} catch (ArrayIndexOutOfBoundsException aiooe) {
				// this is apparently thrown under strange circumstances.
				// interpret as an IOException.
				throw new IOException("aiooe.");
			}
			switch (c) {
			// if this was a \n character, break out of the reading loop
			case '\n':
				keepReading = false;
				break;
			// if this was a \r character, ignore it.
			case '\r':
				continue;
			// if we reached an EOF ...
			case -1:
				return null;
			// if it was any other character, append it to the buffer.
			default:
				sBuffer.append((char) c);
			}
		} while (keepReading);

		// return the string we have read.
		return sBuffer.toString();
	}

	private static class FManCallback extends ActivityCallbackStub {
		public void fileManagerLoaded() {
			synchronized (loaded) {
				loaded.notify();
			}
		}

		private static class Header {
			final String title;

			final List contents;

			public Header(String data) {
				contents = new LinkedList();
				int colon = data.indexOf(":");
				if (colon == -1) {
					title = data;
				} else {
					title = data.substring(0, colon);
					StringTokenizer st = new StringTokenizer(data
							.substring(colon + 1), ",");
					while (st.hasMoreTokens()) {
						String info = st.nextToken().trim();
						contents.add(info);
					}
				}
			}

			public boolean equals(Object o) {
				if (o == this)
					return true;
				if (!(o instanceof Header))
					return false;
				Header other = (Header) o;
				if (!title.toLowerCase().equals(other.title.toLowerCase()))
					return false;
				return listEquals(contents, other.contents);
			}

			public boolean listEquals(List one, List two) {
				if (one.size() != two.size())
					return false;
				boolean found;
				for (Iterator i = one.iterator(); i.hasNext();) {
					found = false;
					String a = (String) i.next();
					for (Iterator j = two.iterator(); j.hasNext();) {
						String b = (String) j.next();
						if (a.equalsIgnoreCase(b))
							found = true;
					}
					if (!found)
						return false;
				}
				for (Iterator i = two.iterator(); i.hasNext();) {
					found = false;
					String a = (String) i.next();
					for (Iterator j = two.iterator(); j.hasNext();) {
						String b = (String) j.next();
						if (a.equalsIgnoreCase(b))
							found = true;
					}
					if (!found)
						return false;
				}
				return true;
			}

			public String toString() {
				return title + " : " + contents;
			}
		}
	}

	private static void startAndWaitForLoad() {
		synchronized (loaded) {
			try {
				ROUTER_SERVICE.start();
				loaded.wait();
			} catch (InterruptedException e) {
				//good.
			}
		}
	}

	/**
	 * tests whether the status flag is set properly
	 */
	public void testFlagSetProperly() throws Exception {
		
		Socket s = new Socket("localhost",PORT);
		BufferedReader in = new BufferedReader(
				new InputStreamReader(s.getInputStream()));
		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(s.getOutputStream()));
		
		//first don't send the feature header that indicates interest.
		
		sendHeader(fileName,HTTPHeaderName.ALT_LOCATION+":", out);
		
		Thread.sleep(500);
		UploadManager umanager = RouterService.getUploadManager();
		
		assertEquals(1,umanager.uploadsInProgress());
		
		List l = (List) PrivilegedAccessor.getValue(umanager,"_activeUploadList");
		
		HTTPUploader u = (HTTPUploader)l.get(0);
		
		assertFalse(u.wantsFAlts());
		assertFalse(u.wantsFWTAlts());
		
		try {
			in.close();
		}catch(IOException ignored){}
		try {
			out.close();
		}catch(IOException ignored){}
		
		Thread.sleep(700);
		
		//repeat with the "fawt" feature sent.
		
		assertEquals(0,umanager.uploadsInProgress());
		s = new Socket("localhost",PORT);
		in = new BufferedReader(
				new InputStreamReader(s.getInputStream()));
		out = new BufferedWriter(
				new OutputStreamWriter(s.getOutputStream()));
		
		
		sendHeader(fileName,FAWTFeatures, out);
		
		Thread.sleep(700);
		
		assertEquals(1,umanager.uploadsInProgress());
		
		u = (HTTPUploader)l.get(0);
		
		assertTrue(u.wantsFAlts());
		assertFalse(u.wantsFWTAlts());
		
		try {
			in.close();
		}catch(IOException ignored){}
		try {
			out.close();
		}catch(IOException ignored){}
		Thread.sleep(500);
		
		//repeat with the "fwawt" feature sent.
		
		assertEquals(0,umanager.uploadsInProgress());
		s = new Socket("localhost",PORT);
		in = new BufferedReader(
				new InputStreamReader(s.getInputStream()));
		out = new BufferedWriter(
				new OutputStreamWriter(s.getOutputStream()));
		
		sendHeader(fileName,FWAWTFeatures, out);
		
		Thread.sleep(700);
		
		assertEquals(1,umanager.uploadsInProgress());
		
		u = (HTTPUploader)l.get(0);
		
		assertTrue(u.wantsFAlts());
		assertTrue(u.wantsFWTAlts());
		
		try {
			in.close();
		}catch(IOException ignored){}
		try {
			out.close();
		}catch(IOException ignored){}
	}

	/**
	 * tests whether the right alternate locations are sent in responses.
	 */
	public void testProperHeadersSent () throws Exception {
		
		//make sure the FD has got some push altlocs.
		FileDesc fd = RouterService.getFileManager().get(0);
		
		assertNotNull(fd);
		assertEquals(0,fd.getAltLocsSize());
		
		URN sha1 = URN.createSHA1Urn(hash);
		GUID clientGUID = new GUID(GUID.makeGuid());
		
		AlternateLocation direct = AlternateLocation.create("1.2.3.4:5",sha1);
		AlternateLocation push = AlternateLocation.create(
				clientGUID.toHexString()+";1.2.3.4:5",sha1);
		
		fd.add(direct);
		fd.add(push);
		
		assertEquals(2,fd.getAltLocsSize());
		
		//send a set of headers without the FALT header.  The response should
		//not contain any firewalled altlocs.
		
		Socket s = new Socket("localhost",PORT);
		BufferedReader in = new BufferedReader(
				new InputStreamReader(s.getInputStream()));
		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(s.getOutputStream()));
		
		sendHeader(fileName,HTTPHeaderName.ALT_LOCATION+":", out);
		
		Thread.sleep(500);
		UploadManager umanager = RouterService.getUploadManager();
		
		assertEquals(1,umanager.uploadsInProgress());
		
		List l = (List) PrivilegedAccessor.getValue(umanager,"_activeUploadList");
		
		HTTPUploader u = (HTTPUploader)l.get(0);
		
		Boolean b = (Boolean)PrivilegedAccessor.getValue(u,"_wantsFalts");
		
		assertFalse(b.booleanValue());
		
		
		
		try {
				while(true){
					String header = readLine(in);
					if (header == null)
						break;
					if(header.startsWith(HTTPHeaderName.FALT_LOCATION.toString()))
						fail("responded with Falt locations without being prompted");
				}
		}catch(IOException expected){}
			
		try {
			in.close();
		}catch(IOException ignored){}
		try {
			out.close();
		}catch(IOException ignored){}
		
		Thread.sleep(700);
		assertEquals(0,l.size());
		
		//now repeat with the fawt header sent.  We should get back one firewalled altloc.
		
		s = new Socket("localhost",PORT);
		in = new BufferedReader(
				new InputStreamReader(s.getInputStream()));
		out = new BufferedWriter(
				new OutputStreamWriter(s.getOutputStream()));
		
		sendHeader(fileName,FAWTFeatures+":", out);
		
		Thread.sleep(700);
		
		assertEquals(1,umanager.uploadsInProgress());
		
		u = (HTTPUploader)l.get(0);
		
		b = (Boolean)PrivilegedAccessor.getValue(u,"_wantsFalts");
		
		assertTrue(b.booleanValue());
		
		boolean present = false;
		AlternateLocationCollection returned = 
			AlternateLocationCollection.create(sha1);
		String header=null;
		try {
			while(true) {
				header = readLine(in);
				if (header == null)
					break;
				if (header.startsWith(HTTPHeaderName.FALT_LOCATION.toString())) {
					present=true;
					break;
				}
			}
		
		}catch(IOException expected ){}
		
		assertTrue(present);
		assertNotNull(header);
		parseHeader(header,returned);
		assertEquals(1,returned.getAltLocsSize());
		
		
		//now send with the FWAWT header sent.  We should not get any
		//in the response.
		
		try {
			in.close();
		}catch(IOException ignored){}
		try {
			out.close();
		}catch(IOException ignored){}
		
		Thread.sleep(700);
		assertEquals(0,l.size());
		
		s = new Socket("localhost",PORT);
		in = new BufferedReader(
				new InputStreamReader(s.getInputStream()));
		out = new BufferedWriter(
				new OutputStreamWriter(s.getOutputStream()));
		
		sendHeader(fileName,FWAWTFeatures+":", out);
		
		Thread.sleep(700);
		
		assertEquals(1,umanager.uploadsInProgress());
		
		u = (HTTPUploader)l.get(0);
		
		b = (Boolean)PrivilegedAccessor.getValue(u,"_wantsFalts");
		
		assertTrue(b.booleanValue());
		
		present = false;
		header=null;
		try {
			while(true) {
				header = readLine(in);
				if (header == null)
					break;
				if (header.startsWith(HTTPHeaderName.FALT_LOCATION.toString())) {
					present=true;
					break;
				}
			}
		
		}catch(IOException expected ){}
		
		assertFalse(present);
		assertNull(header);
		
		//TODO: update test after FWT is done
		
		//clean up for next test
		fd.remove(direct);fd.remove(direct);
		fd.remove(push);fd.remove(push);
		
		try {
			in.close();
		}catch(IOException ignored){}
		try {
			out.close();
		}catch(IOException ignored){}
		
		Thread.sleep(700);
		assertEquals(0,l.size());
	}
	
	
	
	
	/**
	 * tests that the uploader properly stores falts given by downloaders.
	 */
	public void testUploaderStoresFAlts () throws Exception {
		//make sure the FD has got no push altlocs.
		FileDesc fd = RouterService.getFileManager().get(0);
		
		assertNotNull(fd);
		assertEquals(0,fd.getPushAlternateLocationCollection().getAltLocsSize());
		
		Socket s = new Socket("localhost",PORT);
		BufferedReader in = new BufferedReader(
				new InputStreamReader(s.getInputStream()));
		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(s.getOutputStream()));
		
		
		//generate a header that contains one push loc and one direct loc.
		GUID guid = new GUID(GUID.makeGuid());
		AlternateLocation push = AlternateLocation.create(
				guid.toHexString()+";1.2.3.4:5",fd.getSHA1Urn());
		
		AlternateLocation direct = 
			AlternateLocation.create("1.2.3.4:5",fd.getSHA1Urn());
		
		String header = HTTPHeaderName.ALT_LOCATION+":"+direct.httpStringValue()+
			"\n\r"+HTTPHeaderName.FALT_LOCATION+":"+push.httpStringValue();
		
		
		sendHeader(fileName,header,out);
		
		Thread.sleep(500);
		
		UploadManager umanager = RouterService.getUploadManager();
		
		assertEquals(1,umanager.uploadsInProgress());
		
		List l = (List) PrivilegedAccessor.getValue(umanager,"_activeUploadList");
		
		HTTPUploader u = (HTTPUploader)l.get(0);
		
		Boolean b = (Boolean)PrivilegedAccessor.getValue(u,"_wantsFalts");
		
		assertTrue(b.booleanValue());
		
		assertEquals(2,fd.getAltLocsSize());
		assertEquals(1,fd.getPushAlternateLocationCollection().getAltLocsSize());
		assertEquals(1,fd.getAlternateLocationCollection().getAltLocsSize());
		
		assertTrue(fd.getPushAlternateLocationCollection().contains(push));
		assertTrue(fd.getAlternateLocationCollection().contains(direct));
		
		
		fd.remove(push);fd.remove(push);
		fd.remove(direct);fd.remove(direct);
		assertEquals(0,fd.getAltLocsSize());
		
		try {
			in.close();
		}catch(IOException ignored){}
		try {
			out.close();
		}catch(IOException ignored){}
		
		Thread.sleep(700);
		assertEquals(0,l.size());
	}
	private static String makeRequest(String req) {
		if (req.startsWith("/uri-res"))
			return req;
		else
			return "/get/" + index + "/" + req;
	}

	private static void sendHeader(String file,
			String header, Writer out) throws IOException {
		// send request
		out.write("GET" + " " + makeRequest(file) + " "
				+ "HTTP/1.1" + "\r\n");
		if (header != null)
			out.write(header + "\r\n");
		
			out.write("Connection: Keep-Alive\r\n");
		out.write("\r\n");
		out.flush();
	}
	
	private static void sendFeaturesHeader(String file,
			Writer out) throws IOException{
		// send request
		out.write("GET" + " " + makeRequest(file) + " "
				+ "HTTP/1.1" + "\r\n");
		HTTPUtils.writeFeatures(out);
		
			out.write("Connection: Keep-Alive\r\n");
		out.write("\r\n");
		out.flush();
	
	}
	
	private static void parseHeader(String altHeader,AlternateLocationCollector alc) {
		
		final String alternateLocations=HTTPUtils.extractHeaderValue(altHeader);

		// return if the alternate locations could not be properly extracted
		if(alternateLocations == null) return;
		StringTokenizer st = new StringTokenizer(alternateLocations, ",");
        while(st.hasMoreTokens()) {
            try {
                // note that the trim method removes any CRLF character
                // sequences that may be used if the sender is using
                // continuations.
                AlternateLocation al = 
                AlternateLocation.create(st.nextToken().trim(),
                                         alc.getSHA1Urn());
                
                URN sha1 = al.getSHA1Urn();
                if(sha1.equals(alc.getSHA1Urn())) {
                        alc.add(al);
                }
            } catch(IOException e) {
                // just return without adding it.
                continue;
            }
        }
	}


/*	private static String download(String file, String header, String expResp,
			String expHeader) throws IOException {
		//Unfortunately we can't use URLConnection because we need to test
		//malformed and slightly malformed headers

		//1. Write request
		Socket s = new Socket("localhost", PORT);
		BufferedReader in = new BufferedReader(new InputStreamReader(s
				.getInputStream()));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s
				.getOutputStream()));

		String ret = downloadInternal("GET", makeRequest(file), header, out,
				in, expHeader);
		in.close();
		out.close();
		s.close();
		return ret;
	}*/

	


	private static class Header {
		final String title;

		final List contents;

		public Header(String data) {
			contents = new LinkedList();
			int colon = data.indexOf(":");
			if (colon == -1) {
				title = data;
			} else {
				title = data.substring(0, colon);
				StringTokenizer st = new StringTokenizer(data
						.substring(colon + 1), ",");
				while (st.hasMoreTokens()) {
					String info = st.nextToken().trim();
					contents.add(info);
				}
			}
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Header))
				return false;
			Header other = (Header) o;
			if (!title.toLowerCase().equals(other.title.toLowerCase()))
				return false;
			return listEquals(contents, other.contents);
		}

		public boolean listEquals(List one, List two) {
			if (one.size() != two.size())
				return false;
			boolean found;
			for (Iterator i = one.iterator(); i.hasNext();) {
				found = false;
				String a = (String) i.next();
				for (Iterator j = two.iterator(); j.hasNext();) {
					String b = (String) j.next();
					if (a.equalsIgnoreCase(b))
						found = true;
				}
				if (!found)
					return false;
			}
			for (Iterator i = two.iterator(); i.hasNext();) {
				found = false;
				String a = (String) i.next();
				for (Iterator j = two.iterator(); j.hasNext();) {
					String b = (String) j.next();
					if (a.equalsIgnoreCase(b))
						found = true;
				}
				if (!found)
					return false;
			}
			return true;
		}

		public String toString() {
			return title + " : " + contents;
		}
	}
}