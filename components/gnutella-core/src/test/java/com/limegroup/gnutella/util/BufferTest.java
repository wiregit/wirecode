package com.limegroup.gnutella.util;

import junit.framework.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.Assert;

public class BufferTest extends TestCase {
    public BufferTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(BufferTest.class);
    }

    public void testLegacy() {
        //1. Tests of old methods.
        Buffer buf=new Buffer(4);
        Iterator iter=null;

        assertTrue(buf.getCapacity()==4);
        assertTrue(buf.getSize()==0);
        iter=buf.iterator();
        assertTrue(!iter.hasNext());
        try {
            iter.next();
            assertTrue(false);
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }

        buf.add("test");
        assertTrue(buf.getSize()==1);
        iter=buf.iterator();
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test"));
        assertTrue(!iter.hasNext());
        try {
            iter.next();
            assertTrue(false);
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }

        buf.add("test2");
        assertTrue(buf.getSize()==2);
        buf.add("test3");
        assertTrue(buf.getSize()==3);
        iter=buf.iterator();
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test3"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test2"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test"));
        assertTrue(!iter.hasNext());

        buf.add("test4");
        assertTrue(buf.getSize()==4);
        buf.add("test5");
        assertTrue(buf.getSize()==4);
        iter=buf.iterator();
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test5"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test4"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test3"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("test2"));
        assertTrue(!iter.hasNext());	    

        buf.add("test6");
        assertTrue(buf.getSize()==4);

        //2.  Tests of new methods.  These are definitely not sufficient.
        buf=new Buffer(4);
        assertTrue(buf.getSize()==0);
        assertTrue(buf.addLast("a")==null);
        assertTrue(buf.getSize()==1);
        assertTrue(buf.addLast("b")==null);
        assertTrue(buf.getSize()==2);
        assertTrue(buf.addLast("c")==null);
        assertTrue(buf.getSize()==3);
        assertTrue(buf.addLast("d")==null);
        assertTrue(buf.getSize()==4);
        assertTrue(buf.first().equals("a"));
        assertTrue(buf.removeFirst().equals("a"));
        assertTrue(buf.getSize()==3);
        assertTrue(buf.first().equals("b"));
        assertTrue(buf.removeFirst().equals("b"));
        assertTrue(buf.getSize()==2);
        assertTrue(buf.addFirst("b")==null);
        assertTrue(buf.getSize()==3);
        assertTrue(buf.addFirst("a")==null);
        assertTrue(buf.getSize()==4);
        //buf=[a b c d]

        assertTrue(buf.addLast("e").equals("a"));
        //buf=[b c d e]
        assertTrue(buf.last().equals("e"));
        assertTrue(buf.first().equals("b"));
        assertTrue(buf.removeLast().equals("e"));
        assertTrue(buf.removeLast().equals("d"));		

        buf=new Buffer(4);
        iter=buf.iterator();
        buf.addFirst("a");
        try {
            iter.hasNext();
            assertTrue(false);
        } catch (ConcurrentModificationException e) {
            assertTrue(true);
        }

        buf=new Buffer(4);
        buf.addFirst("a");
        buf.addLast("b");
        buf.clear();
        assertTrue(buf.getSize()==0);
        iter=buf.iterator();
        assertTrue(! iter.hasNext());

        //3. Tests of get and set.
        buf=new Buffer(3);
        try {
            buf.get(0);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            buf.get(-1);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) {
        }

        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");  //clobbers a!
        assertTrue(buf.get(0).equals("b"));
        assertTrue(buf.get(1).equals("c"));
        assertTrue(buf.get(2).equals("d"));
        buf.set(2,"bb");
        assertTrue(buf.get(2).equals("bb"));        

        //4. Tests of remove and removeAll methods.
        buf=new Buffer(4);
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(-1);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertTrue(("a").equals(buf.remove(0)));
        assertTrue(buf.size()==3);
        assertTrue(buf.get(0).equals("b"));
        assertTrue(buf.get(1).equals("c"));
        assertTrue(buf.get(2).equals("d")); 

        buf=new Buffer(4);
        buf.addLast("x");
        buf.addLast("y");
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(5);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertTrue(("a").equals(buf.remove(0)));
        assertTrue(buf.size()==3);
        assertTrue(buf.get(0).equals("b"));
        assertTrue(buf.get(1).equals("c"));
        assertTrue(buf.get(2).equals("d"));  

        buf=new Buffer(4);
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(5);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertTrue(("d").equals(buf.remove(3)));
        assertTrue(buf.size()==3);
        assertTrue(buf.get(0).equals("a"));
        assertTrue(buf.get(1).equals("b"));
        assertTrue(buf.get(2).equals("c"));  

        buf=new Buffer(4);
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(5);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertTrue(("b").equals(buf.remove(1)));
        assertTrue(buf.size()==3);
        assertTrue(buf.get(0).equals("a"));
        assertTrue(buf.get(1).equals("c"));
        assertTrue(buf.get(2).equals("d")); 

        buf=new Buffer(4);
        buf.addLast("b");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        assertTrue(buf.remove("d")==true);
        assertTrue(buf.remove("b")==true);
        assertTrue(buf.size()==2);
        assertTrue(buf.get(0).equals("b"));
        assertTrue(buf.get(1).equals("c"));

        buf=new Buffer(4);
        buf.addLast("b");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("b");
        assertTrue(buf.removeAll("b")==true);
        assertTrue(buf.size()==1);
        assertTrue(buf.get(0).equals("c"));

        //5. Test clone() method.
        buf=new Buffer(2);
        buf.addLast("a");
        buf.addLast("b");
        Buffer buf2=new Buffer(buf);
        assertTrue(buf2.size()==buf.size());
        assertTrue(buf2.getCapacity()==buf.getCapacity());
        assertTrue(buf2.first().equals(buf.first()));
        assertTrue(buf2.last().equals(buf.last()));
        
        assertTrue(buf.removeFirst()!=null); //buf2 unmodified
        assertTrue(buf.size()==1);
        assertTrue(buf2.size()==2);
        assertTrue(buf.first().equals("b"));
        assertTrue(buf2.first().equals("a"));
        assertTrue(buf2.last().equals("b"));
    }

}
