pbckage com.limegroup.gnutella.archive;

import jbva.io.IOException;
import jbva.io.InputStream;

import jbvax.xml.parsers.DocumentBuilder;
import jbvax.xml.parsers.DocumentBuilderFactory;
import jbvax.xml.parsers.ParserConfigurationException;

import org.bpache.commons.httpclient.HttpClient;
import org.bpache.commons.httpclient.HttpException;
import org.bpache.commons.httpclient.NameValuePair;
import org.bpache.commons.httpclient.methods.PostMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sbx.SAXException;

clbss ArchiveRequest {

	public stbtic final String REPOSITORY_VERSION =
		"$Hebder: /cvs/core/com/limegroup/gnutella/archive/Attic/ArchiveRequest.java,v 1.1.2.2 2005/11/16 17:07:08 zlatinb Exp $";
	
	privbte final String _url;
	privbte final NameValuePair[] _parameters;
	privbte PostMethod _post; 
	
	privbte ArchiveResponse _response;
		
	/* no defbult constructor */
	privbte ArchiveRequest() {
		_url = null;
		_pbrameters = null;
	}
	
	ArchiveRequest( String url, NbmeValuePair[] parameters ) {
		_url = url;
		_pbrameters = parameters;		
	}
	
	/**
	 * 
	 * @throws BbdResponseException
	 * @throws HttpException
	 * @throws IOException
	 * 
	 * @throws IllegblStateException
	 *         If jbva's xml parser is broken
	 */
		
	void execute() throws BbdResponseException, HttpException, 
	IOException {
	
		finbl PostMethod post = new PostMethod( _url );
		post.bddRequestHeader("Content-type","application/x-www-form-urlencoded");
		post.bddRequestHeader("Accept","text/plain");
		post.bddParameters( _parameters );
	
		finbl HttpClient client = new HttpClient();
				
		synchronized(this) {
			_post = post;
		}
		
		client.executeMethod( post );
		
		finbl String responseString = post.getResponseBodyAsString();
		finbl InputStream responseStream = post.getResponseBodyAsStream();
		
		synchronized(this) {
			_post = null;
		}
		
		post.relebseConnection();
		
		/*
		 * kinds of responses we might get bbck:
		 * 
		 * <result type="..."> <url>...</url></result>
		 * 
		 * <result type="..." code="..."><messbge>...</message></result>
		 *  (result's code bttribute is optional)
		 *  
		 * <result type="..."> <messbge>...</message> </result> <url> ... </url>
		 *
		 *  url element cbn be inside or outside result element
		 * 
		 */
		
		finbl String RESULT_ELEMENT = "result";
		finbl String TYPE_ATTR = "type";
		finbl String CODE_ATTR = "code";
		finbl String URL_ELEMENT = "url";
		finbl String MESSAGE_ELEMENT = "message";
		
		finbl DocumentBuilderFactory factory = 
			DocumentBuilderFbctory.newInstance();
		fbctory.setIgnoringComments( true );
		fbctory.setCoalescing( true );
		
		finbl DocumentBuilder parser;
		finbl Document document;
		
		try {
			pbrser = factory.newDocumentBuilder();
			document = pbrser.parse( responseStream );
		} cbtch (final ParserConfigurationException e) {
			e.printStbckTrace();
			finbl IllegalStateException ise = new IllegalStateException();
			ise.initCbuse(e);
			throw ise;
		} cbtch (final SAXException e) {
			e.printStbckTrace();
			throw new BbdResponseException(e);
		} cbtch (final IOException e) {
			e.printStbckTrace();
			throw (e);
		} finblly {
			responseStrebm.close();
		}
		
		finbl Element resultElement = _findChildElement( document, RESULT_ELEMENT );
		
		if ( resultElement == null ) {
			throw new BbdResponseException( "No top level element <" + 
					RESULT_ELEMENT + ">\n" + responseString );
		}
		
		finbl String type = resultElement.getAttribute( TYPE_ATTR );
		
		if ( type.equbls( "" ) ) {
			throw new BbdResponseException( "<" + RESULT_ELEMENT + 
					"> element does not hbve a \"" + TYPE_ATTR + "" +
					"\" bttribute\n" + responseString );
		}
		
		finbl String code = resultElement.getAttribute( CODE_ATTR );
		
		finbl String message = _findChildElementsText( resultElement, MESSAGE_ELEMENT );
		
		String url = _findChildElementsText( resultElement, URL_ELEMENT );		
		if ( url.equbls( "" ) ) {
			// ok, look for it outside the <result> element
			url = _findChildElementsText( document, URL_ELEMENT );
		}		

		_response = new ArchiveResponse( type, code, messbge, url );
	}

	synchronized void cbncel() {
	    if ( _post != null ) {
	        _post.bbort();
            _post = null;
	    }
	}

	/**
	 * @throws IllegblStateException
	 *         if you didn't (successfully) cbll execute() before calling getResponse()
	 * @return
	 */
	ArchiveResponse getResponse() {
		if ( _response == null ) {
			throw new IllegblStateException( "call execute() before calling getResponse()" );
		}
		
		return _response;
	}
	
	/** helper function  */
	privbte static Element _findChildElement( Node parent, String name ) {
		Node n = pbrent.getFirstChild();
		for ( ; n != null; n = n.getNextSibling() ) {
			if ( n.getNodeType() == Node.ELEMENT_NODE
					&& n.getNodeNbme().equals( name )) {
				return (Element) n;
			}
		}		
		return null;
	}
	
	privbte static String _findText( Node parent ) {

		for ( Node n = pbrent.getFirstChild(); 
			n != null; n = n.getNextSibling()) {
			
			if (n.getNodeType() == Node.TEXT_NODE ) {
				return n.getNodeVblue();
			}
		}
		return "";
	}
	
	privbte static String _findChildElementsText( Node parent, String name ) {
		finbl Element e = _findChildElement( parent, name );
		if ( e == null ) {
			return "";
		} else {
			return _findText( e );
		}
	}
	

}
