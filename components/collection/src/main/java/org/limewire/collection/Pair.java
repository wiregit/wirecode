
package org.limewire.collection;

/**
 *  Contains an int property key and its corresponding value type {@link Object}.
 <pre>
    void sampleCodePair(){
        Pair p1 = new Pair(0, "A");
        Pair p2 = new Pair(10, "B");
        System.out.println("Compare A to B: " + p1.compareTo(p2));
        System.out.println("Get element p1: " + p1.getElement());   
    }

 </pre>
 */
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
