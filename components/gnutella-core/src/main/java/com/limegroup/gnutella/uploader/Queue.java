package com.limegroup.gnutella.uploader;

/**
 * this is just a simple interface for a queue. 
 * it will be implemented by the UploadQueue class.
 * a queue works FIFO or First In, First Out.
 */

public interface Queue {

	/**
	 * inserts a new element into the queue
	 */
	public void insert(Object a) throws IndexOutOfBoundsException;

	/**
	 * removes an element from the queue
	 */
	public Object remove() throws IndexOutOfBoundsException;
	
	/**
	 * returns true if the number of elements in the
	 * queue is 0.  otherwise, it returns false.
	 */ 
	public boolean isEmpty();

	/**
	 * returns the number of elements in the queue
	 */
	public int size();

	/**
	 * set the capacity of the queue
	 */ 
	public void setCapacity(int capacity);

	/**
	 * get the capacity of the queue
	 */
	public int getCapacity();

}



