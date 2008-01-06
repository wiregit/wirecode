package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.message.BasicHeader;
import org.apache.http.HttpException;

class DirectContribution extends ArchiveContribution {

	private String _identifier;
	private String _ftpServer;
	private String _ftpPath;
	private String _verificationUrl;
	

	private Object _requestLock = new Object();
	private ArchiveRequest _request = null;
	
	
	
	public DirectContribution(String username, String password, String title,
			String description, int media) 
	throws DescriptionTooShortException {
		super(username, password, title, description, media);
	}

	public DirectContribution(String username, String password, String title,
			String description, int media, int collection, int type) 
	throws DescriptionTooShortException {
		super(username, password, title, description, media, collection, type);
	}

	
	public String getIdentifier() {
		return _identifier;
	}

	public String getVerificationUrl() {
		return _verificationUrl;
	}

	protected String getFtpServer() {
		return _ftpServer;
	}

	protected String getFtpPath() {
		return _ftpPath;
	}

	protected boolean isFtpDirPreMade() {
		return true;
	}
	
	public void cancel() {
		super.cancel();
	
		synchronized( _requestLock ) {
			if ( _request != null ) {
				_request.cancel();
                _request = null;
			}
		}
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
	public String requestIdentifier(String identifier)
            throws IdentifierUnavailableException, BadResponseException,
            IOException, HttpException, URISyntaxException, InterruptedException {
			
			final String CREATE_ID_URL = "http://www.archive.org:80/create.php";
			
			_identifier = null;
			
			// normalize the identifier
			
			String nId = Archives.normalizeName( identifier );
			
            ArchiveRequest request = new ArchiveRequest( CREATE_ID_URL, new BasicHeader[] {
                    new BasicHeader( "xml", "1" ),
                    new BasicHeader( "user", getUsername() ),
                    new BasicHeader( "identifier", nId )});
			synchronized( _requestLock ) {
				_request = request;
			}
			
			ArchiveResponse response;
			try {
			    request.execute();
			    response = request.getResponse();
			} finally {
			    synchronized( _requestLock ){
			        _request = null;
			    }
			}
			
			final String resultType = response.getResultType();
			
			if ( resultType == ArchiveResponse.RESULT_TYPE_SUCCESS ) {
				
				final String url = response.getUrl();
				
				if ( url.equals( "" ) ) {
					throw new BadResponseException( "successful result, but no url given" );
				}
				
				final String[] urlSplit = url.split( "/", 2 );
				
				if ( urlSplit.length < 2 ) {
					throw new BadResponseException( "No slash (/) present to separate server from path: " + url );
				}
				
				// we're all good now
				
				_ftpServer = urlSplit[0];
				_ftpPath = "/" + urlSplit[1];
				
				
				_identifier = nId;
				
				
				// set verification URL
				
				_verificationUrl = "http://www.archive.org/details/" + _identifier;
				
				return _identifier;
				
			} else if ( resultType == ArchiveResponse.RESULT_TYPE_ERROR ) {
				throw new IdentifierUnavailableException( response.getMessage(), nId );
			} else {
				// unidentified type
				throw new BadResponseException ( "unidentified result type:" + resultType );
			}
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
	protected void checkin() throws BadResponseException, IOException, HttpException, URISyntaxException, InterruptedException {
		
		final String CHECKIN_URL = "http://www.archive.org/checkin.php";
		final String username = getUsername();
		
		if ( username == null ) {
			throw new IllegalStateException( "username not set" );			
		}
		if ( _identifier == null ) {
			throw new IllegalStateException( "identifier not set" );
		}
		
        ArchiveRequest request = new ArchiveRequest( CHECKIN_URL, new BasicHeader[] {
                new BasicHeader( "xml", "1" ),
                new BasicHeader( "user", username ),
                new BasicHeader( "identifier", _identifier )
        }); 
		synchronized( _requestLock ) {
			_request = request; 
		}
		
		ArchiveResponse response;
		try {
		    request.execute();
		    response = request.getResponse();
		} finally {
		    synchronized( _requestLock ) {
		        _request = null;
		    }
		}
		
		final String resultType = response.getResultType();
		
		if ( resultType == ArchiveResponse.RESULT_TYPE_SUCCESS ) {
			return;
		} else if ( resultType == ArchiveResponse.RESULT_TYPE_ERROR ) {
			throw new BadResponseException( "checkin failed: " + response.getMessage() );
		} else {
			throw new BadResponseException( "unidentified result type:" + resultType );
		}
	}



}
