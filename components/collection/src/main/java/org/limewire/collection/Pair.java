/*
 *
 *
 */

package org.limewire.collection;

public class Pair implements Comparable<Pair> {
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

	public int compareTo(Pair p) {
		return _key - p._key;
	}
}
