package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.xerces.dom3.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;



public class DefaultContribution extends AbstractContribution {
	
	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/DefaultContribution.java,v 1.1.2.5 2005-10-31 20:59:55 tolsen Exp $";

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
				ArchiveConstants.defaultCollectionForMedia( media ));
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

	
	// only allow alphanumerics and . - _
	private static final Pattern _badChars = 
		Pattern.compile( "[^\\p{Alnum}\\.\\-_]" );
	private static final String _replaceStr = "_";
	
	private static final String _createIdUrl =
		"http://www.archive.org:80/create.php";
	
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
		_identifier = null;
		// normalize the identifier
		final String nId = _badChars.matcher( identifier ).replaceAll(_replaceStr);
		
        final HttpClient client = new HttpClient();

        final PostMethod post = new PostMethod( _createIdUrl );
        post.addRequestHeader("Content-type","application/x-www-form-urlencoded");
        post.addRequestHeader("Accept","text/plain");

        final NameValuePair[] nameVal = new NameValuePair[] {
                        new NameValuePair("xml","1"),
                        new NameValuePair("user", getUsername() ),
                        new NameValuePair("identifier", nId )};

        post.addParameters(nameVal);
        client.executeMethod(post);
        
        final String responseString = post.getResponseBodyAsString();
        final InputStream responseStream = post.getResponseBodyAsStream();

        post.releaseConnection();
        
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
			IllegalStateException ise = new IllegalStateException();
			ise.initCause(e);
			throw ise;
		} catch (final SAXException e) {
			e.printStackTrace();
			throw new BadResponseException(e);
		} catch (final IOException e) {
			e.printStackTrace();
			throw (e);
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

		
		final Node resultNode = _findChildElement( document, "result" );
		
		if ( resultNode == null ) {
			throw new BadResponseException( "No top level element <result>\n"
					+ responseString );
		}
		
		final Node typeNode = resultNode.getAttributes().getNamedItem( "type" );
		
		if ( typeNode == null ) {
			throw new BadResponseException( "<result> node does not have a \"type\" attribute\n"
					+ responseString );
		}
		
		final String type = typeNode.getNodeValue();
		
		if ( type.equals( "success" ) ) {
			final Node urlNode = _findChildElement( resultNode, "url" ); 
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
				ArchiveConstants.getMediaString( getMedia() ) +
				"_details_db.php?collection=" +
				ArchiveConstants.getCollectionString( getCollection() ) +
				"&collectionid=" + _identifier;
			
			return _identifier;
			
		} else if ( type.equals( "error" ) ) {
			// let's fetch the message
			
			final Node msgNode = _findChildElement( resultNode, "message" );
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
	
	/** helper function for setIdentifier() */
	private static Node _findChildElement( Node parent, String name ) {
		Node n = parent.getFirstChild();
		for ( ; n != null; n = n.getNextSibling() ) {
			if ( n.getNodeType() == Node.ELEMENT_NODE
					&& n.getNodeName().equals( name )) {
				break;
			}
		}		
		return n;
	}
	
	/** helper function for setIdentifier() */
	private static String _findText( Node parent ) {

		for ( Node n = parent.getFirstChild(); 
			n != null; n = n.getNextSibling()) {
			
			if (n.getNodeType() == Node.TEXT_NODE ) {
				return n.getNodeValue();
			}
		}
		return "";
	}

	/**
	 * @throws	IllegalStateException
	 * 			If the contribution object is not ready to upload
	 * 			(no username, password, server, etc. set)
	 * 			or if java's xml parser is configured badly
	 */
	public void upload() {
		
		String username = getUsername();
		String password = getPassword();
		
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
		
		Set fileDetails = getFileDetails();
		
		int totalFiles = 0;
		long totalBytes = 0;
		
		
		
		try {
			// first connect
			FTPClient ftp = new FTPClient();
			ftp.enterLocalPassiveMode();
			
			
			ftp.connect( _ftpServer );
			
			final int reply = ftp.getReplyCode();
			if ( !FTPReply.isPositiveCompletion(reply) ) {
				ftp.disconnect();
				throw new RefusedConnection( _ftpServer + "refused FTP connection" );
			}
			// now login
			ftp.login( username, password );

			processUploadEvent( new UploadEvent( this, UploadEvent.CONNECTED ));
			
			// now change directory
			ftp.changeWorkingDirectory( _ftpPath );
			ftp.setFileType( FTP.BINARY_FILE_TYPE );

			
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
	}
	
	
	

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
	
	private static final String _metadataElement = "metadata";
	private static final String _collectionElement = "collection";
	private static final String _titleElement = "title";
	private static final String _licenseUrlElement = "licenseurl";
	
	/**
	 * @throws 	IllegalStateException
	 * 			If java's xml parser configuration is bad
	 */
	
	
	private Document getMetaDocument() {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document document = db.newDocument();
			
			final Element metadataElement = document.createElement(_metadataElement);
			document.appendChild( metadataElement );
			
			final Element collectionElement = document.createElement( _collectionElement );
			metadataElement.appendChild( collectionElement );
			collectionElement.appendChild( document.createTextNode( ArchiveConstants.getMediaString( getMedia())));
			
			final Element titleElement = document.createElement( _titleElement );
			metadataElement.appendChild( titleElement );
			titleElement.appendChild( document.createTextNode( ArchiveConstants.getCollectionString( getCollection())));
			
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
	
	private static final String _filesElement = "files";

	
	/**
	 * @throws 	IllegalStateException
	 * 			If java's xml configuration is bad
	 * @return
	 */
	
	private Document getFilesDocument() {
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document document = db.newDocument();
			
			final Element filesElement = document.createElement( _filesElement );
			document.appendChild( filesElement );
			
			Collection files = getFiles();
			
			for (final Iterator i = files.iterator(); i.hasNext();) {
				final File file = (File) i.next();
				
				final Element fileElement = file.getElement();
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
