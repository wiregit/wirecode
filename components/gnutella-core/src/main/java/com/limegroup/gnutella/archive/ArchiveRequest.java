package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class ArchiveRequest {

	private final String _url;
	private final NameValuePair[] _parameters;
	private PostMethod _post; 
	
	private ArchiveResponse _response;
		
	/* no default constructor */
	private ArchiveRequest() {
		_url = null;
		_parameters = null;
	}
	
	ArchiveRequest( String url, NameValuePair[] parameters ) {
		_url = url;
		_parameters = parameters;		
	}
	
	/**
	 * 
	 * @throws BadResponseException
	 * @throws HttpException
	 * @throws IOException
	 * 
	 * @throws IllegalStateException
	 *         If java's xml parser is broken
	 */
		
	void execute() throws BadResponseException, HttpException, 
	IOException {
	
		final PostMethod post = new PostMethod( _url );
		post.addRequestHeader("Content-type","application/x-www-form-urlencoded");
		post.addRequestHeader("Accept","text/plain");
		post.addParameters( _parameters );
	
		final HttpClient client = new HttpClient();
				
		synchronized(this) {
			_post = post;
		}
		
		client.executeMethod( post );
		
		final String responseString = post.getResponseBodyAsString();
		final InputStream responseStream = post.getResponseBodyAsStream();
		
		synchronized(this) {
			_post = null;
		}
		
		post.releaseConnection();
		
		/*
		 * kinds of responses we might get back:
		 * 
		 * <result type="..."> <url>...</url></result>
		 * 
		 * <result type="..." code="..."><message>...</message></result>
		 *  (result's code attribute is optional)
		 *  
		 * <result type="..."> <message>...</message> </result> <url> ... </url>
		 *
		 *  url element can be inside or outside result element
		 * 
		 */
		
		final String RESULT_ELEMENT = "result";
		final String TYPE_ATTR = "type";
		final String CODE_ATTR = "code";
		final String URL_ELEMENT = "url";
		final String MESSAGE_ELEMENT = "message";
		
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
			final IllegalStateException ise = new IllegalStateException();
			ise.initCause(e);
			throw ise;
		} catch (final SAXException e) {
			throw new BadResponseException(e);
		} catch (final IOException e) {
			throw (e);
		} finally {
			responseStream.close();
		}
		
		final Element resultElement = _findChildElement( document, RESULT_ELEMENT );
		
		if ( resultElement == null ) {
			throw new BadResponseException( "No top level element <" + 
					RESULT_ELEMENT + ">\n" + responseString );
		}
		
		final String type = resultElement.getAttribute( TYPE_ATTR );
		
		if ( type.equals( "" ) ) {
			throw new BadResponseException( "<" + RESULT_ELEMENT + 
					"> element does not have a \"" + TYPE_ATTR + "" +
					"\" attribute\n" + responseString );
		}
		
		final String code = resultElement.getAttribute( CODE_ATTR );
		
		final String message = _findChildElementsText( resultElement, MESSAGE_ELEMENT );
		
		String url = _findChildElementsText( resultElement, URL_ELEMENT );		
		if ( url.equals( "" ) ) {
			// ok, look for it outside the <result> element
			url = _findChildElementsText( document, URL_ELEMENT );
		}		

		_response = new ArchiveResponse( type, code, message, url );
	}

	synchronized void cancel() {
	    if ( _post != null ) {
	        _post.abort();
            _post = null;
	    }
	}

	/**
	 * @throws IllegalStateException
	 *         if you didn't (successfully) call execute() before calling getResponse()
	 * @return
	 */
	ArchiveResponse getResponse() {
		if ( _response == null ) {
			throw new IllegalStateException( "call execute() before calling getResponse()" );
		}
		
		return _response;
	}
	
	/** helper function  */
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
	
	private static String _findText( Node parent ) {

		for ( Node n = parent.getFirstChild(); 
			n != null; n = n.getNextSibling()) {
			
			if (n.getNodeType() == Node.TEXT_NODE ) {
				return n.getNodeValue();
			}
		}
		return "";
	}
	
	private static String _findChildElementsText( Node parent, String name ) {
		final Element e = _findChildElement( parent, name );
		if ( e == null ) {
			return "";
		} else {
			return _findText( e );
		}
	}
	

}
