package com.limegroup.gnutella.util;

/** 
 * this data structure is to be used in place of the 
 * ArrayList in the LimitedList class.  Even though
 * it is an array, it behaves like a list in how it
 * shifts its elements. 
 */


public class LimitedArray {

    private int _size;
    private Object[] _array;

    public LimitedArray(int size) {
		_size = size;
		_array = new Object[_size];
    }

    public Object[] toArray() {return _array;}
 
    public Object get(int index) {
		if (index > _size-1)  /* should i check for index out of bounds */
			return null;
		else return _array[index];
    }

    public void remove(int index) {
	
		for (int i = index; i < _size-1; i++) {
			_array[i] = _array[i+1];
		}
	
		_array[_size-1] = null;

    }
    
    /** 
	 * when you add at an index, all the others 
	 * elements  have to shift down 
	 */
    public void add(int index, Object elem) {
	
		/* shift all other elements down */
		for (int i = _size-1; i > index; i--) {
			_array[i] = _array[i-1];
		}
		_array[index] = elem;
	
    }

}
