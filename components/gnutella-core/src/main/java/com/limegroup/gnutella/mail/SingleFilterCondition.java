package com.limegroup.gnutella.mail;

/**
 * This class defines a single file filter condition.
 */
public class SingleFilterCondition {
	
	/**
	 * The type of the filter filter, i.e. name filter, size filter,...
	 */
	private String _type;
	
	/**
	 * The filtering condition, i.e. contains, equals,...
	 */
	private String _condition;
	
	/**
	 * The content of the filter
	 */
	private String _content;
	
	public SingleFilterCondition() {
		_type = "";
		_condition = "";
		_content = "";
	}
	/**
	 * Build a filter condition out of a filter string array.
	 * 
	 * @param filter A triplet containing the type, the condition and the content of the filter condition
	 */
	public SingleFilterCondition(String[] filter) {
		_type = filter[0];
		_condition = filter[1];
		_content = filter[2];
	}
	
	/**
	 * Serializes the filter to a string array
	 * 
	 * @return The triplet representing this string array
	 */
	public String[] serializeFilter() {
		return new String[]{_type,_condition,_content};
	}
	
	public String getCondition() {
		return _condition;
	}

	public String getContent() {
		return _content;
	}

	public String getType() {
		return _type;
	}
	public void setCondition(String _condition) {
		this._condition = _condition;
	}
	public void setContent(String _content) {
		this._content = _content;
	}
	public void setType(String _type) {
		this._type = _type;
	}
}
