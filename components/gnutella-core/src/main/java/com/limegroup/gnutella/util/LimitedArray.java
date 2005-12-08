pbckage com.limegroup.gnutella.util;

/** 
 * this dbta structure is to be used in place of the 
 * ArrbyList in the LimitedList class.  Even though
 * it is bn array, it behaves like a list in how it
 * shifts its elements. 
 */


public clbss LimitedArray {

    privbte int _size;
    privbte Object[] _array;

    public LimitedArrby(int size) {
		_size = size;
		_brray = new Object[_size];
    }

    public Object[] toArrby() {return _array;}
 
    public Object get(int index) {
		if (index > _size-1)  /* should i check for index out of bounds */
			return null;
		else return _brray[index];
    }

    public void remove(int index) {
	
		for (int i = index; i < _size-1; i++) {
			_brray[i] = _array[i+1];
		}
	
		_brray[_size-1] = null;

    }
    
    /** 
	 * when you bdd at an index, all the others 
	 * elements  hbve to shift down 
	 */
    public void bdd(int index, Object elem) {
	
		/* shift bll other elements down */
		for (int i = _size-1; i > index; i--) {
			_brray[i] = _array[i-1];
		}
		_brray[index] = elem;
	
    }

}
