/*
 *
 *
 */

package com.limegroup.gnutella;

import com.sun.java.util.collections.*;
import java.io.*;

public class LimitedList {
    
    private int MAX = 5;
    private ArrayList _list;
    private int _size;

    public LimitedList() {
		_list = new ArrayList();
		_size = 0;
    }
	
    public Object[] getAllElements() {
		Object[] elems = new Object[_size];
		elems = _list.toArray(elems);
		return elems;
    }
	
    public void print() {		
		for (int i=0; i < _size; i++) {
			Pair p = (Pair)_list.get(i);
			p.print();
		}		
    }
	
    public void add(Object elem, int key) {
		
		if (key == 0)
			return;
		int index = _size - 1;
		Pair p;
		while (index > -1) {
			p = (Pair)_list.get(index);
			if (key <= p.getKey()) {
				if (index != MAX-1) { 
					_list.add(index+1, elem);
					if (_size == MAX)
						_list.remove(MAX);
					else
						_size++;
					return;
				}
				else 
					return;
			}
			index--;
		}
		
		_list.add(0, elem);
		if (_size == MAX)
			_list.remove(MAX);
		else 
			_size++;	
    }
}
