package com.limegroup.gnutella.archive;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.xerces.dom3.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;



public class DefaultContribution extends AbstractContribution {
	
	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/DefaultContribution.java,v 1.1.2.11 2005-11-02 19:09:12 tolsen Exp $";

	private String _identifier;
	private String _ftpServer;
	private String _ftpPath;
	private String _verificationUrl;
	
	// no default constructor
	private DefaultContribution() {
	}
	
	
	/**
	 * @param media
	 *        One of ArchiveConstants.MEDIA_*
	 *        
	 * @throws IllegalArgumentException
	 *         If type is not valid
	 */
	public DefaultContribution( String username, String password, 
			String title, int media ) {
		this( username, password, title, media, 
				Archives.defaultCollectionForMedia( media ));
	}
	
	/**
	 * 
	 * @param username
	 * @param password
	 * @param title
	 * @param media
	 * @param collection
	 * 
	 * @throws IllegalArgumentException
	 *         If type or collection is not valid
	 *         
	 */
	public DefaultContribution( String username, String password, 
			String title, int media, int collection ) {
		setUsername( username );
		setPassword( password );
		setTitle( title );
		setMedia( media );
		setCollection( collection );	
	}
	
	public String getVerificationUrl() {
		return _verificationUrl;
	}

	
	/** 
	 * 	normalizes identifier and checks with Internet Archive
	 *  if identifier is available.
	 *  throws a IdentifierUnavailableException if the identifier
	 *  is not available
	 *  otherwise, returns normalized identifier 
	 * 
	 * @throws 	IdentifierUnavailableException
	 * 			If the identifier is not available
	 * 
	 * @throws	BadResponseException
	 * 			If we get a bad response from Internet Archive
	 * 
	 * @throws	HttpException
	 * 			If something bad happens in the http layer
	 * 
	 * @throws  IOException
	 * 			If something bad happens during I/O
	 * 
	 * @throws	IllegalStateException
	 * 			If java's xml parser configuration is bad
	 * 
	 */
	
	public String reserveIdentifier( String identifier ) 
	throws IdentifierUnavailableException, BadResponseException,
	HttpException, IOException {
		
		final String _createIdUrl = "http://www.archive.org:80/create.php";
		
		_identifier = null;
		// normalize the identifier
		
		String nId = Archives.normalizeName( identifier );
		
        final HttpClient client = new HttpClient();

        final PostMethod post = postMethod( _createIdUrl, getUsername(), nId );

        client.executeMethod( post );
        
        final String responseString = post.getResponseBodyAsString();
        final InputStream responseStream = post.getResponseBodyAsStream();
        post.releaseConnection();
        
        final Element resultElement;
        
        try {
        	resultElement = getResultElement( responseStream );
        } finally {
        	responseStream.close();
        }

		/*
		 * On success, XML will be returned like:
		 * 
		 * <result type="success"><url>[FTP url]</url></result>
		 * 
		 * On failure, XML will be returned like:
		 * 
		 * <result type="error"><message>[human readable error]</message></result>
		 */

		if ( resultElement == null ) {
			throw new BadResponseException( "No top level element <result>\n"
					+ responseString );
		}
		
		final Node typeNode = resultElement.getAttributes().getNamedItem( "type" );
		
		if ( typeNode == null ) {
			throw new BadResponseException( "<result> node does not have a \"type\" attribute\n"
					+ responseString );
		}
		
		final String type = typeNode.getNodeValue();
		
		if ( type.equals( "success" ) ) {
			
			final Node urlNode = _findChildElement( resultElement, "url" ); 
			if ( urlNode == null ) {
				throw new BadResponseException( "<result type=\"success\"> does not have <url> child\n"
						+ responseString );
			} 
			
			final String urlString = _findText( urlNode );
			
			// the url returned does not have ftp:// in front of it
			// i.e. :  server/path
			
			final String[] urlSplit = urlString.split( "/", 2 );
			
			if ( urlSplit.length < 2 ) {
				throw new BadResponseException( "No slash (/) present to separate server from path: "
				+ urlString + "\n" + responseString );
			}

			// we're all good now
			
			_ftpServer = urlSplit[0];
			_ftpPath = "/" + urlSplit[1];
			
			_identifier = nId;
			
			
			// set verification URL
			
			_verificationUrl =  "http://www.archive.org/" +
				Archives.getMediaString( getMedia() ) +
				"_details_db.php?collection=" +
				Archives.getCollectionString( getCollection() ) +
				"&collectionid=" + _identifier;
			
			return _identifier;
			
			
		} else if ( type.equals( "error" ) ) {
			// let's fetch the message
			
			final Node msgNode = _findChildElement( resultElement, "message" );
			String msg = "[NO MESSAGE GIVEN]";
			
			if ( msgNode != null ) {
				msg = _findText( msgNode );
			}
			
			throw new IdentifierUnavailableException( msg, nId );
		} else {
			// unidentified type
			throw new BadResponseException ( "unidentified value for attribute \"type\" in <result> element \n"
					+ "(should be either \"success\" or \"error\"): " + type + "\n" + responseString ); 
		}
			


		
		
	}
	
	/** helper function for reserveIdentifier() */
	private static Element _findChildElement( Node parent, String name ) {
		Node n = parent.getFirstChild();
		for ( ; n != null; n = n.getNextSibling() ) {
			if ( n.getNodeType() == Node.ELEMENT_NODE
					&& n.getNodeName().equals( name )) {
				return (Element) n;
			}
		}		
		return null;
	}
	
	/** helper function for reserveIdentifier() */
	private static String _findText( Node parent ) {

		for ( Node n = parent.getFirstChild(); 
			n != null; n = n.getNextSibling()) {
			
			if (n.getNodeType() == Node.TEXT_NODE ) {
				return n.getNodeValue();
			}
		}
		return "";
	}

	
	private static final int  NUM_XML_FILES = 2;
	private static final String META_XML_SUFFIX = "_meta.xml";
	private static final String FILES_XML_SUFFIX = "_files.xml";
	

	/**
	 * 
	 * @throws UnknownHostException
	 *         If the hostname cannot be resolved.
	 *         
	 * @throws SocketException
	 *         If the socket timeout could not be set.
	 *         
	 * @throws FTPConnectionClosedException
	 *         If the connection is closed by the server.
	 *         
	 * @throws LoginFailedException
	 *         If the login fails.
	 *         
	 * @throws DirectoryChangeFailedException
	 *         If changing to the directory provided by the internet
	 *         archive fails.
	 *         
	 * @throws CopyStreamException
	 *         If an I/O error occurs while in the middle of
	 *         transferring a file.
	 *         
	 * @throws IOException
	 *         If an I/O error occurs while sending a command or
	 *         receiving a reply from the server
	 *         
	 * @throws IllegalStateException
	 * 		   If the contribution object is not ready to upload
	 * 		   (no username, password, server, etc. set)
	 * 		   or if java's xml parser is configured badly
	 */
	
	public void upload() throws UnknownHostException, SocketException, 
	FTPConnectionClosedException, LoginFailedException,
	DirectoryChangeFailedException, CopyStreamException, 
	RefusedConnectionException, IOException {
		
		final String username = getUsername();
		final String password = getPassword();
		
		if ( _ftpServer == null ) {
			throw new IllegalStateException( "ftp server not set" );
		}
		if ( _ftpPath == null ) {
			throw new IllegalStateException( "ftp path not set" );
		}
		if ( username == null ) {
			throw new IllegalStateException( "username not set" );			
		}
		if ( password == null ) {
			throw new IllegalStateException( "password not set" );
		}
		
		// calculate total number of files and bytes
		
		
		
		final String metaXmlString = serializeDocument( getMetaDocument() );
		final String filesXmlString = serializeDocument( getFilesDocument() );
		
		final byte[] metaXmlBytes = metaXmlString.getBytes();
		final byte[] filesXmlBytes = filesXmlString.getBytes();
		
		final int metaXmlLength = metaXmlBytes.length;
		final int filesXmlLength = filesXmlBytes.length;
		
		final Collection files = getFiles();
		
		final int totalFiles = NUM_XML_FILES + files.size();
		
		final String[] fileNames = new String[totalFiles];
		final long[] fileSizes = new long[totalFiles];
		
		final String metaXmlName = _identifier + META_XML_SUFFIX; 
		fileNames[0] = metaXmlName;
		fileSizes[0] = metaXmlLength;
		
		final String filesXmlName = _identifier + FILES_XML_SUFFIX;
		fileNames[1] = filesXmlName;
		fileSizes[1] = filesXmlLength;
		
		int j = 2;
		for (Iterator i = files.iterator(); i.hasNext();) {
			final File f = (File) i.next();
			fileNames[j] = f.getLocalFileName();
			fileSizes[j] = f.getFileSize();
			j++;
		}
		
		final UploadEvent uploadEvent = new UploadEvent( this, fileNames, fileSizes );
		
		FTPClient ftp = new FTPClient();
		
		try {
			// first connect
			
			ftp.enterLocalPassiveMode();
			ftp.connect( _ftpServer );
			
			final int reply = ftp.getReplyCode();
			if ( !FTPReply.isPositiveCompletion(reply) ) {
				throw new RefusedConnectionException( _ftpServer + "refused FTP connection" );
			}
			// now login
			if (!ftp.login( username, password )) {
				throw new LoginFailedException();
			}
			
			try {
				
				// now change directory
				if (!ftp.changeWorkingDirectory( _ftpPath )) {
					throw new DirectoryChangeFailedException();
				}
				
				uploadEvent.connected();
				processUploadEvent( uploadEvent );
				
				
				// upload xml files
				uploadFile( metaXmlName, metaXmlName,
						new ByteArrayInputStream( metaXmlBytes ),
						ftp, uploadEvent );
				
				
				uploadFile( filesXmlName, filesXmlName,
						new ByteArrayInputStream( filesXmlBytes ),
						ftp, uploadEvent );
				
				// now switch to binary mode
				ftp.setFileType( FTP.BINARY_FILE_TYPE );
				
				// upload contributed files
				for (final Iterator i = files.iterator(); i.hasNext();) {
					final File f = (File) i.next();
					
					uploadFile( f.getLocalFileName(), f.getRemoteFileName(), 
							new FileInputStream( f.getIOFile() ),
							ftp,uploadEvent );
				}
			} finally {
				ftp.logout(); // we don't care if logging out fails
			}
		} finally {
			ftp.disconnect();	
		}
		
		// now tell the Internet Archive that we're done
		checkin();
		
	}
	
	private PostMethod postMethod( String url, String username, String identifier  ) {
        
		final PostMethod post = new PostMethod( url );
        post.addRequestHeader("Content-type","application/x-www-form-urlencoded");
        post.addRequestHeader("Accept","text/plain");

        final NameValuePair[] nameVal = new NameValuePair[] {
                        new NameValuePair("xml","1"),
                        new NameValuePair("user", username ),
                        new NameValuePair("identifier", identifier )};

        post.addParameters(nameVal);
        
        return post;
	}
	
	/**
	 * 
	 * @param fileName
	 * @param input
	 * 		  The input stream (not necessarily buffered).
	 *        This stream will be closed by this method
	 */
	private void uploadFile( String localFileName, String remoteFileName, 
			InputStream input, FTPClient ftp, UploadEvent uploadEvent )
	throws IOException {
		uploadEvent.fileStarted( localFileName );
		processUploadEvent( uploadEvent );
		final InputStream fileStream = new BufferedInputStream(
				new UploadMonitorInputStream( input, this, uploadEvent ));
		
		try {
			ftp.storeFile( remoteFileName, fileStream );
		} finally {
			fileStream.close();
		}
		uploadEvent.fileCompleted();
		processUploadEvent( uploadEvent );
	}
	
	/**
	 * 
	 * @throws	HttpException
	 * 			If something bad happens in the http layer
	 * 
	 * @throws  IOException
	 * 			If something bad happens during I/O
	 * 
	 * @throws IllegalStateException
	 *         If username or identifier is not set.
	 *         
	 * @throws BadResponseException
	 *         If the checkin fails
	 *
	 */
	private void checkin() throws HttpException, 
	BadResponseException, IOException {
		
		final String checkinUrl = "http://www.archive.org/checkin.php";
		final String username = getUsername();
		
		if ( username == null ) {
			throw new IllegalStateException( "username not set" );			
		}
		if ( _identifier == null ) {
			throw new IllegalStateException( "identifier not set" );
		}
		
		final PostMethod post = postMethod( checkinUrl, username, _identifier );
		
		final HttpClient client = new HttpClient();
		client.executeMethod( post );
		
		final String responseString = post.getResponseBodyAsString();
		final InputStream responseStream = post.getResponseBodyAsStream();
		post.releaseConnection();
		
		final Element resultElement;
		
		try {
			resultElement = getResultElement( responseStream );
		} finally {
			responseStream.close();
		}
		
		/*
		 * On success, XML will be returned like:
		 *
		 * <result type="success"><message>item has been checked in</message></result>
		 *
		 * On failure, XML will be returned like:
		 *
		 * <result type="error"><message>[human readable error]</message></result>
		 *
		 */
		
		if ( resultElement == null ) {
			throw new BadResponseException( "No top level element <result>\n"
					+ responseString );
		}
		
		final Node typeNode = resultElement.getAttributes().getNamedItem( "type" );
		
		if ( typeNode == null ) {
			throw new BadResponseException( "<result> node does not have a \"type\" attribute\n"
					+ responseString );
		}
		
		final String type = typeNode.getNodeValue();
		
		if ( type.equals( "success" ) ) {
			return;
		} else if ( type.equals( "error" ) ) {
			// let's fetch the message
			
			final Node msgNode = _findChildElement( resultElement, "message" );
			String msg = "[NO MESSAGE GIVEN]";
			
			if ( msgNode != null ) {
				msg = _findText( msgNode );
			}
			
			throw new BadResponseException( "checkin failed: " + msg );
		} else {
			// unidentified type
			throw new BadResponseException( "unidentified value for attribute \"type\" in <result> element \n"
					+ "(should be either \"success\" or \"error\"): " + type + "\n" + responseString ); 
		}
	}

	/**
	 * 
	 * @param responseStream
	 * @param responseString
	 * @return
	 * 
	 * @throws BadResponseException
	 *         If we get a bad response from Internet Archive
	 *         
	 * @throws IOException
	 *         If something bad happens during I/O
	 *         
	 * @throws IllegalStateException
	 *         If java's xml parser configuration is bad
	 */

	private Element getResultElement( InputStream responseStream )
	throws BadResponseException, IOException {
		
		final DocumentBuilderFactory factory = 
			DocumentBuilderFactory.newInstance();
		factory.setIgnoringComments( true );
		factory.setCoalescing( true );
		
		final DocumentBuilder parser;
		final Document document;
		
		try {
			parser = factory.newDocumentBuilder();
			document = parser.parse( responseStream );
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause(e);
			throw ise;
		} catch (final SAXException e) {
			e.printStackTrace();
			throw new BadResponseException(e);
		} catch (final IOException e) {
			e.printStackTrace();
			throw (e);
		} 
			
		return _findChildElement( document, "result" );

	}
	
	/**
	 * @throws 	IllegalStateException
	 * 			If java's xml parser configuration is bad
	 */
	
	
	private Document getMetaDocument() {
		
		/*
		 * Sample _meta.xml file:
		 * 
		 * <metadata>
		 *   <collection>opensource_movies</collection>
		 *   <mediatype>movies</mediatype>
		 *   <title>My Home Movie</title>
		 *   <runtime>2:30</runtime>
		 *   <director>Joe Producer</director>
		 * </metadata>    
		 * 
		 */
		
		final String _metadataElement = "metadata";
		final String _collectionElement = "collection";
		final String _titleElement = "title";
		final String _licenseUrlElement = "licenseurl";
		
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document document = db.newDocument();
			
			final Element metadataElement = document.createElement(_metadataElement);
			document.appendChild( metadataElement );
			
			final Element collectionElement = document.createElement( _collectionElement );
			metadataElement.appendChild( collectionElement );
			collectionElement.appendChild( document.createTextNode( Archives.getMediaString( getMedia())));
			
			final Element titleElement = document.createElement( _titleElement );
			metadataElement.appendChild( titleElement );
			titleElement.appendChild( document.createTextNode( Archives.getCollectionString( getCollection())));
			
			//take licenseurl from the first File
			
			final Iterator filesIterator = getFiles().iterator();
			
			if ( filesIterator.hasNext() ) {
				final File firstFile = (File) filesIterator.next();
				final String licenseUrl = firstFile.getLicenseUrl();
				if ( licenseUrl != null ) {
					final Element licenseUrlElement = document.createElement( _licenseUrlElement );
					metadataElement.appendChild( licenseUrlElement );
					licenseUrlElement.appendChild( document.createTextNode( licenseUrl ));
				}
			}
			
			// now build user-defined elements
			final Map userFields = getFields();
			for ( final Iterator i = userFields.keySet().iterator(); i.hasNext(); ) {
				final Object field = i.next();
				final Object value = userFields.get( field );  
				
				if ( field instanceof String ) {
					final Element e = document.createElement( (String) field );
					metadataElement.appendChild( e );
					
					if ( value != null && value instanceof String) {
						e.appendChild( document.createTextNode( (String) value ));
					}
				}
			}
			
			return document;
			
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
			
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		}	
	}
	


	
	/**
	 * @throws 	IllegalStateException
	 * 			If java's xml configuration is bad
	 * @return
	 */
	
	private Document getFilesDocument() {
		/*
		 * Sample _files.xml file:
		 * 
		 * <files>
		 *   <file name="MyHomeMovie.mpeg" source="original">
	     *     <runtime>2:30</runtime>
	     *     <format>MPEG2</format>
	     *   </file>
	     * </files>    
		 * 
		 */
		
		final String _filesElement = "files";
		
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document document = db.newDocument();
			
			final Element filesElement = document.createElement( _filesElement );
			document.appendChild( filesElement );
			
			Collection files = getFiles();
			
			for (final Iterator i = files.iterator(); i.hasNext();) {
				final File file = (File) i.next();
				
				final Element fileElement = file.getElement( document );
				filesElement.appendChild( fileElement );
			}
			
			return document;
			
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		}	
		
	}
	
	/**
	 * @throws	IllegalStateException
	 * 			If java's DOMImplementationLS class is screwed up or
	 * 			this code is buggy
	 * @param document
	 * @return
	 */
	private String serializeDocument( Document document ) {
		
		try {
			System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMImplementationSourceImpl");
			final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			
			final DOMImplementationLS impl = 
				(DOMImplementationLS)registry.getDOMImplementation("LS");
			
			final LSSerializer writer = impl.createLSSerializer();
			final String str = writer.writeToString( document );
			
			return str;
			
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		} catch (final InstantiationException e) {
			e.printStackTrace();
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause( e );
			throw ise;
		}
		
	}
	
}
