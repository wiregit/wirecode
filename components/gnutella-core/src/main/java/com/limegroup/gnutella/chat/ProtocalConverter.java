package com.limegroup.gnutella.chat;
/**
 * handles converting text to desired protocal, and back.
 */

public class ProtocalConverter {
	
	/** variable for singleton */
	private static ProtocalConverter _protocalConverter;

	/** private constructor for singleton */
	private ProtocalConverter() {

	}
	
	/** returns an instance of this class, singleton */
	public static ProtocalConverter instance() {
		if (_protocalConverter == null)
			_protocalConverter = new ProtocalConverter();
		return _protocalConverter;
	}
	
	/** converts a plain text format to a "lime" format */
	public String toLime(String plain) {
		return plain;
	}

	/** converts a "lime" format to a plain text format */
	public String toPlain(String lime) {
		return lime;
	}

}
