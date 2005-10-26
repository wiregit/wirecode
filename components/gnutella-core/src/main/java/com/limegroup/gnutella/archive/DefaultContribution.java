package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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



public class DefaultContribution extends AbstractContribution {
	
	public static final String repositoryVersion = 
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/DefaultContribution.java,v 1.1.2.1 2005-10-26 20:02:48 tolsen Exp $";

	private String _identifier;
	private URL _ftpUrl;
	
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
	 */
	
	public String getVerificationUrl( String identifier ) 
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
			final URL url = new URL( urlString );
			
			// check that it's FTP
			if ( !url.getProtocol().equalsIgnoreCase("ftp") ) {
				throw new BadResponseException( "Not an ftp url: "
						+ urlString + "\n" + responseString  );
			}
			
			// we're all good now
			_ftpUrl = url;
			_identifier = nId;
			//return _identifier;
			
			// construct verification URL
			
			return "http://www.archive.org/" +
				ArchiveConstants.getMediaString( getMedia() ) +
				"_details_db.php?collection=" +
				ArchiveConstants.getCollectionString( getCollection() ) +
				"&collectionid=" + _identifier;
			
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


	public void upload() {
		// TODO Auto-generated method stub
		
	}

	
}
