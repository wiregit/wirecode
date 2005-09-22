package com.limegroup.gnutella.mail;

import java.util.ArrayList;

/**
 * This class defines a file filter used when sending download notifications.
 * Each filter is composed of single filter conditions associated with
 * the AND and the OR binary operators
 * 
 * @see com.limegroup.gnutella.mail.SingleFilterCondition
 *
 */
public class NotificationFilter {
	
	/**
	 * The name of this notification filter
	 */
	private String _name;

	/**
	 * The list of single filter conditions associated to this filter
	 */
	private ArrayList _singleConditions = new ArrayList();
	
	/**
	 * The list of AND OR binary operators linking the conditions together
	 */
	private ArrayList _andOrList = new ArrayList();
	
	/**
	 * Constructor that builds the notification filter out of a 
	 * string array containing the filter name in first position and then
	 * triplets defining each single filter condition separated by the binary operator
	 * 
	 * @param filterString The string array defining this filter
	 */
	public NotificationFilter(String[] filterString) {
		_name = filterString[0];
		for (int j = 1; j < filterString.length; j+=4) {
			String[] fi = new String[4];
			System.arraycopy(filterString,j,fi,0,3);
			SingleFilterCondition filter = new SingleFilterCondition(fi);
			_singleConditions.add(filter);
			if(filterString.length>j+3){
				_andOrList.add(filterString[4]);
			}
		}
	}
	
	public NotificationFilter() {}
	
	/**
	 * Returns the binary operator linking the single filter condition i
	 * with the condition i+1
	 * 
	 * @param i The index of the binary operator
	 * @return an AND or OR binary operator
	 */
	public String getAndOr(int i) {
		return (String)_andOrList.get(i);
	}

	public String getName() {
		return _name;
	}
	
	public void setName(String name) {
		_name = name;
	}
	
	/**
	 * Returns the single filter condition at position conditionIndex
	 * 
	 * @param conditionIndex the position of the condition in the filter
	 * @return a single filter condition
	 */
	public SingleFilterCondition getSingleFilter(int conditionIndex) {
		return (SingleFilterCondition)_singleConditions.get(conditionIndex);
	}
	public int getTotalConditions() {
		return _singleConditions.size();
	}
	
	/**
	 * Serializes the filter to an array of Strings
	 * 
	 * @return the string array containing the serialized filter
	 */
	public String[] serializeFilter() {
		String[] finalFilter = new String[_singleConditions.size()*4];
		finalFilter[0] = _name;
		for (int i = 0; i < _singleConditions.size(); i++) {
			String[] singleFilter = ((SingleFilterCondition)_singleConditions.get(i)).serializeFilter();
			System.arraycopy(singleFilter,0,finalFilter,1+4*i,3);		
			if(i<_andOrList.size())finalFilter[(i+1)*4]=(String)_andOrList.get(i);
		}
		return finalFilter;
	}
	
	/**
	 * Adds the single filter condition to the list of conditions of this filter
	 * 
	 * @param singleFilter the SingleFilterCondition to add
	 */
	public void addSingleFilter(SingleFilterCondition singleFilter) {
		_singleConditions.add(singleFilter);
	}
	
	/**
	 * Adds a AND or OR binary operator to the filter
	 * 
	 * @param andOr the AND or OR binary operator
	 */
	public void addAndOr(String andOr) {
		_andOrList.add(andOr);
	}
}
