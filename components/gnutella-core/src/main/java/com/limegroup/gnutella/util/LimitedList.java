/*
 *
 *
 */

package com.limegroup.gnutella.util;


public class LimitedList {
    
    private int MAX = 5;
    // private ArrayList _list;
    private LimitedArray _list;
    private int _size;

    public LimitedList() {
        // _list = new ArrayList();
        _list = new LimitedArray(MAX);
        _size = 0;
    }
        
    public Object[] getAllElements() {
        Object[] elems = new Object[_size];
        elems = _list.toArray();
        return elems;
    }
    
    public void print() {               
        for (int i=0; i < _size; i++) {
            Pair p = (Pair)_list.get(i);
			if (p != null)
				p.print();
        }               
    }
        
    public void add(Object elem, int key) {
                
        if (key == 0)
            return;
        int index = _size - 1;
		Pair p;
		int pkey;


		if (index > -1) {
			while( (p = (Pair)_list.get(index)) == null ) {
				index--;
				if( index==0 ) break;
			}
		}
		
		while (index > -1) {
			p = (Pair)_list.get(index);
			
			pkey = p.getKey();

			if (key <= pkey) {
				if (index != MAX-1) { 
					_list.add(index+1, elem);
					_size++;
					return;
				}
				else 
  					return;
			}
			index--;
		}
		
		_list.add(0, elem);
		_size++;

    }
}
