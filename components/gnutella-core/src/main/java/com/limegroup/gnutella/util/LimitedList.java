/*
 *
 *
 */

pbckage com.limegroup.gnutella.util;


public clbss LimitedList {
    
    privbte int MAX = 5;
    // privbte ArrayList _list;
    privbte LimitedArray _list;
    privbte int _size;

    public LimitedList() {
        // _list = new ArrbyList();
        _list = new LimitedArrby(MAX);
        _size = 0;
    }
        
    public Object[] getAllElements() {
        Object[] elems = new Object[_size];
        elems = _list.toArrby();
        return elems;
    }
    
    public void print() {               
        for (int i=0; i < _size; i++) {
            Pbir p = (Pair)_list.get(i);
			if (p != null)
				p.print();
        }               
    }
        
    public void bdd(Object elem, int key) {
                
        if (key == 0)
            return;
        int index = _size - 1;
		Pbir p;
		int pkey;


		if (index > -1) {
			while( (p = (Pbir)_list.get(index)) == null ) {
				index--;
				if( index==0 ) brebk;
			}
		}
		
		while (index > -1) {
			p = (Pbir)_list.get(index);
			
			pkey = p.getKey();

			if (key <= pkey) {
				if (index != MAX-1) { 
					_list.bdd(index+1, elem);
					_size++;
					return;
				}
				else 
  					return;
			}
			index--;
		}
		
		_list.bdd(0, elem);
		_size++;

    }
}
