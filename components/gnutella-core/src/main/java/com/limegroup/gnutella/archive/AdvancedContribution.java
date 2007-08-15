package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;


class AdvancedContribution extends ArchiveContribution {

	private String _identifier;
	private String _ftpServer;
	private String _ftpPath;
	private String _verificationUrl;
	

	private Object _requestLock = new Object();
	private ArchiveRequest _request = null;
	
	private void initFtpServer() {
		_ftpServer = Archives.getMediaString( getMedia() ) +
			"-uploads.archive.org";
	}
	
	public AdvancedContribution(String username, String password, String title,
			String description, int media) 
	throws DescriptionTooShortException {
		super(username, password, title, description, media);
		initFtpServer();
	}

	public AdvancedContribution(String username, String password, String title,
			String description, int media, int collection, int type) 
	throws DescriptionTooShortException{
		super(username, password, title, description, media, collection, type);
		initFtpServer();
	}
	
	/**
	 * an advanced contribution requires a description of at least 5 words
	 * 
	 * @throws DescriptionTooShortException
	 *         If the description is less than 5 words
	 */

	private static final int DESCRIPTION_MIN_WORDS = 5;
	private static final String MIN_WORDS_REGEX;
	
	static {
		
		if ( DESCRIPTION_MIN_WORDS == 1 ) {
			MIN_WORDS_REGEX = "\\W*\\w+.*";
		} else if ( DESCRIPTION_MIN_WORDS > 1 ) {
			MIN_WORDS_REGEX = "\\W*(\\w+\\W+){"
				+ Integer.toString( DESCRIPTION_MIN_WORDS - 1 ) 
				+ "}\\w+.*";
		} else {
			MIN_WORDS_REGEX = ".*";
		}
	}
	
	private static final Pattern MIN_WORDS_PATTERN =
		Pattern.compile( MIN_WORDS_REGEX );
	
	static void checkDescription( String description ) 
	throws DescriptionTooShortException {
		if (!MIN_WORDS_PATTERN.matcher( description ).matches()) {
			throw new DescriptionTooShortException( description, DESCRIPTION_MIN_WORDS );
		}
	}
	
	public void setDescription( String description ) throws DescriptionTooShortException {
		checkDescription( description );
		super.setDescription( description );			
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
		return false;
	}
	
	public void cancel() {
		super.cancel();
	
		synchronized( _requestLock ) {
			if ( _request != null ) {
				_request.cancel();
			}
		}
	}

	public String requestIdentifier(String identifier) 
	throws IdentifierUnavailableException, BadResponseException, 
	HttpException, IOException {
		
		final String CREATE_ID_URL = "http://www.archive.org/services/check_identifier.php";
		
		_identifier = null;
		
		// normalize the identifier
		
		String nId = Archives.normalizeName( identifier );
		
		synchronized( _requestLock ) {
			_request = new ArchiveRequest( CREATE_ID_URL, new NameValuePair[] {
					new NameValuePair( "identifier", nId )
			});
		}
		
		_request.execute();
		final ArchiveResponse response = _request.getResponse();
		
		synchronized( _requestLock ){
			_request = null;
		}
		
		final String resultType = response.getResultType();
		
		if ( resultType == ArchiveResponse.RESULT_TYPE_SUCCESS ) {
			
			// we're all good now
			
			_ftpPath = _identifier = nId;
			
			// set verification URL
			_verificationUrl =  "http://www.archive.org/" +
				Archives.getMediaString( getMedia() ) + "/" +
				Archives.getMediaString( getMedia() ) +
				"-details-db.php?collection=" +
				Archives.getCollectionString( getCollection() ) +
				"&collectionid=" + _identifier;
			
			return _identifier;
			
		} else if ( resultType == ArchiveResponse.RESULT_TYPE_ERROR ) {
			throw new IdentifierUnavailableException( response.getMessage(), nId );
		} else {
			// unidentified type
			throw new BadResponseException ( "unidentified result type:" + resultType );
		}
	}
	

	protected void checkin() throws HttpException, BadResponseException, IOException {
	
		final String CHECKIN_URL = "http://www.archive.org/services/contrib-submit.php";
		final String username = getUsername();
		
		if ( username == null ) {
			throw new IllegalStateException( "username not set" );			
		}
		if ( _identifier == null ) {
			throw new IllegalStateException( "identifier not set" );
		}
		
		synchronized( _requestLock ) {
			_request = new ArchiveRequest( CHECKIN_URL, new NameValuePair[] {
					new NameValuePair( "user_email", username ),
					new NameValuePair( "server", getFtpServer() ),
					new NameValuePair( "dir", _identifier )
			});
		}
		
		_request.execute();
		final ArchiveResponse response = _request.getResponse();
		
		synchronized( _requestLock ) {
			_request = null;
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
