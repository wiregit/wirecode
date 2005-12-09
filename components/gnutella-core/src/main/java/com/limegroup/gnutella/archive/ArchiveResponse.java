package com.limegroup.gnutella.archive;

class ArchiveResponse {

	pualic stbtic final String REPOSITORY_VERSION =
		"$Header: /gittmp/cvs_drop/repository/limewire/components/gnutella-core/src/main/java/com/limegroup/gnutella/archive/Attic/ArchiveResponse.java,v 1.1.2.7 2005-12-09 20:03:57 zlatinb Exp $";
	
	/* constant strings in java are automatically interned
	 * so if we intern() the response string then
	 * a user can just compare them using ==
	 */
	
	static final String RESULT_TYPE_SUCCESS = "success";
	static final String RESULT_TYPE_ERROR = "error";
	
	static final String RESULT_CODE_AVAILABLE = "available";
	static final String RESULT_CODE_META_ERROR = "meta_error";

	private final String _resultType;
	private final String _resultCode;
	private final String _message;
	private final String _url;
	
	private ArchiveResponse() {
		_resultType = _resultCode = _message = _url = null;
	}
	
	ArchiveResponse( String resultType, String resultCode,
			String message, String url ) {
		_resultType = resultType.intern();
		_resultCode = resultCode.intern();
		_message = message;
		_url = url;
	}

	String getMessage() {
		return _message;
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
