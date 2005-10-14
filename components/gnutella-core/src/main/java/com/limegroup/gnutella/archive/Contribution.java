package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;



public class Contribution {
	
	private String __id__ = "$Id: Contribution.java,v 1.1.2.1 2005-10-14 23:27:03 tolsen Exp $";
	
	private String _title;
	private int _type;
	private int _collection;
	private String _identifier;
	private String _username;
	
	// if by chance this class becomes serializable, and
	// you wish to write the password to disk, then feel free
	// to take out the transient keyword
	private transient String _password;
	
	/** String -> String */
	private HashMap _fields = new HashMap();

	private URL _ftpUrl;
	
	

	
	// no default constructor
	private Contribution() {
	}
	
	
	// see MEDIA_* types in ArchiveConstants
	public Contribution( String username, String password, 
			String title, int type ) 
	 throws IllegalArgumentException {
		this( username, password, title, type, 
				ArchiveConstants.defaultCollectionForType( type ));
	}
	
	public Contribution( String username, String password, 
			String title, int type, int collection )
	throws IllegalArgumentException {
		setUsername( username );
		setPassword( password );
		setTitle( title );
		setType( type );
		setCollection( collection );	
	}
	
	
	public void setTitle( String title ) {
		_title = title;
	}
	
	public String getTitle() {
		return _title;
	}
	
	public void setType( int type ) throws IllegalArgumentException {
		if ( ArchiveConstants.getMediaString( type ) == null ) {
			throw new IllegalArgumentException( "Invalid media type: " 
					+ type );
		}
		_type = type;
	}
	
	public int getType() {
		return _type;
	}
	
	public void setCollection( int collection ) throws IllegalArgumentException {
		if ( ArchiveConstants.getCollectionString( collection ) == null ) {
			throw new IllegalArgumentException( "Invalid collection type: " 
					+ collection );
		}
		_collection = collection;
	}
	
	public int getCollection() {
		return _collection;
	}
	
	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public String getUsername() {
		return _username;
	}

	public void setUsername(String username) {
		_username = username;
	}
		
	
	/**
	 * Fields You can include whatever fields you like, but the following will
	 * currently be shown on the Internet Archive website
	 * 
	 * Movies and Audio: date, description, runtime
	 * 
	 * Audio: creator, notes, source, taper 	 
	 *  
	 * Movies: color, contact, country, credits, director, producer,
	 *		production_company, segments, segments, sound, sponsor, shotlist 
	 */
	

	public void setField( String field, String value ) {
		_fields.put( field, value );
	}
	
	public String getField( String field ) {
		return (String) _fields.get( field );
	}
	
	public void removeField( String field ) {
		_fields.remove( field );
	}


	
	// only allow alphanumerics and . - _
	private static final Pattern _badChars = 
		Pattern.compile( "[\\p{Alnum}\\.\\-_]" );
	private static final String _replaceStr = "_";
	
	private static final String _createIdURL =
		"http://www.archive.org:80/create.php";
	
	/** normalizes identifier and checks with Internet Archive
	 *  if identifier is available.
	 *  throws a IdentifierUnavailableException if the identifier
	 *  is not available
	 *  otherwise, returns normalized identifier 
	 */
	
	public String setIdentifier( String identifier ) 
	throws IdentifierUnavailableException, BadResponseException,
	HttpException, IOException {
		_identifier = null;
		// normalize the identifier
		final String nId = _badChars.matcher( identifier ).replaceAll(_replaceStr);
		
        final HttpClient client = new HttpClient();

        final PostMethod post = new PostMethod( _createIdURL );
        post.addRequestHeader("Content-type","application/x-www-form-urlencoded");
        post.addRequestHeader("Accept","text/plain");

        final NameValuePair[] nameVal = new NameValuePair[] {
                        new NameValuePair("xml","1"),
                        new NameValuePair("user", _username),
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
			// this shouldn't happen.  I don't think....
			e.printStackTrace();
			// think of it as a bad response from the XML
			// library
			throw new BadResponseException(e);
		} catch (final SAXException e) {
			e.printStackTrace();
			throw new BadResponseException(e);
		} catch (final IOException e) {
			e.printStackTrace();
			throw (e);
		}	
		

		
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
			final URL url = new URL( urlString );
			
			// check that it's FTP
			if ( !url.getProtocol().equalsIgnoreCase("ftp") ) {
				throw new BadResponseException( "Not an ftp url: "
						+ urlString + "\n" + responseString  );
			}
			
			// we're all good now
			_ftpUrl = url;
			_identifier = nId;
			return _identifier;
		} else if ( type.equals( "error") ) {
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

	
}
