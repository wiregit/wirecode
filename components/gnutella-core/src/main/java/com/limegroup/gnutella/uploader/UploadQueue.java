package com.limegroup.gnutella.uploader;
/**
 * Implements the Queue.java interface.  Will store the
 * requested uploads in order.  Follows the singleton pattern.
 *
 */

import java.util.ArrayList;
import java.net.Socket;

//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class UploadQueue implements Queue {
	
	/************ MEMBER VARIABLE DECLARATION *****************/
	// the default size of the list under the queue.
	private final int DEFAULT_QUEUE_SIZE = 10;
	// the instance of this class, for singleton pattern
	private UploadQueue _uploadQueue;
	// the list upon which the queue is based
	private java.util.ArrayList _list;
	// the capacity of the queue.  
	private int _capacity;
	// the number of elements currently in the list
	private int _size;

	/******************* PUBLIC INTERFACE *****************/

	/**
	 * returns an instance of the UploadQueue, following the singleton
	 * pattern.
	 */
	public UploadQueue instance() {
		if (_uploadQueue == null)
			_uploadQueue = new UploadQueue();
		return _uploadQueue;
	}

	/**
	 * inserts a new element into the queue
	 *
	 * Because multiple threads may be calling add and remove
	 * on the instance of this, I have put these methods into
	 * synchronized blocks.
	 */
	public void insert(Object a) throws IndexOutOfBoundsException {
		synchronized(UploadQueue.this) {
			// test whether or not the list has reached its capacity
			// limit.  if it has, throw an IndexOutOfBoundsException.
			if (_size >= _capacity) 
				throw new IndexOutOfBoundsException();
			// test to see if it is really a socket being inserted.
			// if it isn't, then do not allow the insertion, and 
			// throw an exception.
			try {
				Socket s = (Socket)a;
			} catch  (ClassCastException e) {
				throw new IndexOutOfBoundsException();
			}
			// increment the number of elements in the list
			_size++;
			// add the element into the list at the numElements position
			_list.add(_size, a);
		}
	}
	
	/**
	 * removes an element from the queue.  this will only 
	 * return non-null socket values.  if there are no non
	 * null values in the list, it will throw an 
	 * IndexOutOfBoundsException.
	 *
	 * Because multiple threads may be calling add and remove
	 * on the instance of this, I have put these methods into
	 * synchronized blocks.
	 */
	public Object remove() throws IndexOutOfBoundsException {

		Socket socket;
		while (true) {
			synchronized(UploadQueue.this) {
				// removes the first element in the list.
				try {
					socket = (Socket)_list.remove(0);
				} catch (ClassCastException e) {
					// this isn't really correct, but this
					// instance should not happen
					throw new IndexOutOfBoundsException();
				}
				// if an exception is thrown, then there is nothing
				// in the list and there is no reason to decrement
				// the size;
				_size--;
				// only return a non-null socket value.  if the downloader
				// cancels the connection, then the socket value will 
				// become null.
				if (socket != null)
					break;
			}
		}
		return socket;
	}
	
	/**
	 * returns true if the number of elements in the
	 * queue is 0.  otherwise, it returns false.
	 */ 
	public boolean isEmpty() {
		if (_size == 0) 
			return true;
		return false;
	}
	
	/**
	 * returns the number of elements in the queue
	 */
	public int size() {
		return _size;
	}

	/**
	 * set the capacity of the queue
	 */ 
	public void setCapacity(int capacity) {
		synchronized (UploadQueue.this) {
			_capacity = capacity;
		}
	}

	/**
	 * get the capacity of the queue
	 */
	public int getCapacity() {
		return _capacity;
	}

	/******************* CONSTRUCTOR *****************/

	private UploadQueue() {
		_list = new ArrayList(DEFAULT_QUEUE_SIZE);
		_size = 0;
		_capacity = DEFAULT_QUEUE_SIZE;
	}

}
