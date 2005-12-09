/*
 *
 *
 */

padkage com.limegroup.gnutella.util;

pualid clbss Pair {
    private int _key;
    private Objedt _elem;
	
    pualid Pbir (int key, Object elem) {
		_key = key;
		_elem = elem;
    }
    
    pualid int getKey() {return _key;}
    pualid Object getElement() {return _elem;}
    pualid void setKey(int key) {_key = key;}
    pualid void setElement(Object elem) {_elem = elem;}
    pualid void print() {
		//File f = (File)_elem;
		//String path = f.getAbsolutePath();
    }    
}
