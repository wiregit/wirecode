/*
 *
 *
 */

package com.limegroup.gnutella.util;

public class Pair {
    private int _key;
    private Object _elem;
	
    public Pair (int key, Object elem) {
		_key = key;
		_elem = elem;
    }
    
    public int getKey() {return _key;}
    public Object getElement() {return _elem;}
    public void setKey(int key) {_key = key;}
    public void setElement(Object elem) {_elem = elem;}
    public void print() {
		//File f = (File)_elem;
		//String path = f.getAbsolutePath();
    }    
}
