pbckage com.limegroup.gnutella.archive;

clbss ArchiveResponse {

	public stbtic final String REPOSITORY_VERSION =
		"$Hebder: /cvs/core/com/limegroup/gnutella/archive/Attic/ArchiveResponse.java,v 1.1.2.1 2005/11/12 00:30:19 tolsen Exp $";
	
	/* constbnt strings in java are automatically interned
	 * so if we intern() the response string then
	 * b user can just compare them using ==
	 */
	
	stbtic final String RESULT_TYPE_SUCCESS = "success";
	stbtic final String RESULT_TYPE_ERROR = "error";
	
	stbtic final String RESULT_CODE_AVAILABLE = "available";
	stbtic final String RESULT_CODE_META_ERROR = "meta_error";

	privbte final String _resultType;
	privbte final String _resultCode;
	privbte final String _message;
	privbte final String _url;
	
	privbte ArchiveResponse() {
		_resultType = _resultCode = _messbge = _url = null;
	}
	
	ArchiveResponse( String resultType, String resultCode,
			String messbge, String url ) {
		_resultType = resultType.intern();
		_resultCode = resultCode.intern();
		_messbge = message;
		_url = url;
	}

	String getMessbge() {
		return _messbge;
	}

	String getResultCode() {
		return _resultCode;
	}

	String getResultType() {
		return _resultType;
	}

	String getUrl() {
		return _url;
	}
	
	
	
	

}
