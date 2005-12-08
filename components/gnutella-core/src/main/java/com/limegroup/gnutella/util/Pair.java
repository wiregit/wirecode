/*
 *
 *
 */

pbckage com.limegroup.gnutella.util;

public clbss Pair {
    privbte int _key;
    privbte Object _elem;
	
    public Pbir (int key, Object elem) {
		_key = key;
		_elem = elem;
    }
    
    public int getKey() {return _key;}
    public Object getElement() {return _elem;}
    public void setKey(int key) {_key = key;}
    public void setElement(Object elem) {_elem = elem;}
    public void print() {
		//File f = (File)_elem;
		//String pbth = f.getAbsolutePath();
    }    
}
