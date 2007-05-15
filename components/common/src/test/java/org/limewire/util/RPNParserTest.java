package org.limewire.util;

import java.util.Properties;

import org.limewire.util.RPNParser.StringLookup;

import junit.framework.Test;

public class RPNParserTest extends BaseTestCase {

    public RPNParserTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RPNParserTest.class);
    }    
    
    public void testArithmetic() throws Exception {
        RPNParser p = new RPNParser();
        // 1 < 2
        assertTrue(p.evaluate(new String[] {
                "1","2","<"
        }));

        assertFalse(p.evaluate(new String[] {
                "1","2",">"
        }));

        // 0 == 0
        assertTrue(p.evaluate(new String[] {
                "0","0","=="
        }));
    }
    
    public void testOr() throws Exception {
        RPNParser p = new RPNParser();
        // true or true is true
        assertTrue(p.evaluate(new String[] {
                "true","true","OR"
        }));
        
        // true or false is true
        assertTrue(p.evaluate(new String[] {
                "true","false","OR"
        }));
        
        // false or true is true
        assertTrue(p.evaluate(new String[] {
                "false","true","OR"
        }));
        
        // false or false is false
        assertFalse(p.evaluate(new String[] {
                "false","false","OR"
        }));
    }
    
    public void testAnd() throws Exception {
        RPNParser p = new RPNParser();
        // true and true is true
        assertTrue(p.evaluate(new String[] {
                "true","true","AND"
        }));
        
        // true and false is false
        assertFalse(p.evaluate(new String[] {
                "true","false","AND"
        }));
        
        // false and true is true
        assertFalse(p.evaluate(new String[] {
                "false","true","AND"
        }));
        
        // false and false is false
        assertFalse(p.evaluate(new String[] {
                "false","false","AND"
        }));
    }
    
    public void testNot() throws Exception {
        RPNParser p = new RPNParser();
        
        // not true is false
        assertFalse(p.evaluate(new String[] {
                "true","NOT"
        }));
        
        // not false is true
        assertTrue(p.evaluate(new String[] {
                "false","NOT"
        }));
    }
    
    public void testInvalid() throws Exception {
        RPNParser p = new RPNParser();
        
        try {
            p.evaluate(new String[]{"asdf","NOT"});
            fail("invalid operand");
        } catch (IllegalArgumentException expected){}
        
        try {
            p.evaluate(new String[]{"3","N"});
            fail("invalid operation");
        } catch (IllegalArgumentException expected){}
        
        try {
            p.evaluate(new String[]{"3","1","NOT"});
            fail("logic on number");
        } catch (IllegalArgumentException expected){}
        
        try {
            p.evaluate(new String[]{"true","false","<"});
            fail("arithmetic on boolean");
        } catch (IllegalArgumentException expected){}
        
        try {
            p.evaluate(new String[]{"false","OR"});
            fail("not enough operands");
        } catch (IllegalArgumentException expected){}
        
        try {
            p.evaluate(new String[]{"true","true","NOT"});
            fail("too many operands");
        } catch (IllegalArgumentException expected){}
    }
    
    public void testComposite() throws Exception {
        RPNParser p = new RPNParser();
        
        // ((5 > 4) && (4 < 3)) || ( 5 > 0 && false) || (0 == 0)
        assertTrue(p.evaluate(new String[] {
                "5","4",">","4","3","<","AND","5","0",">","false","AND","OR","0","0","==","OR"
        }));
    }
    
    public void testPropsLookup() throws Exception {
        MyLookup p = new MyLookup();
        p.setProperty("x","1");
        p.setProperty("y", "2");
        p.setProperty("yes", "true");
        p.setProperty("no", "false");
        p.setProperty("maybe","whatever");
        
        RPNParser r = new RPNParser(p);
        
        assertTrue(r.evaluate(new String[]{
            "maybe","whatever","=="    
        }));
        
        // x < y ?
        assertTrue(r.evaluate(new String[] {
                "x","y","<"}));
        
        // x > 1 ?
        assertFalse(r.evaluate(new String[] {
                "x","1",">"}));
        
        // y == 2 ?
        assertTrue(r.evaluate(new String[] {
                "y","2","=="}));
        
        // x > 0 == yes?
        assertTrue(r.evaluate(new String[] {
                "x","0",">","yes","=="}));
        
        // yes == !no?
        assertTrue(r.evaluate(new String[] {
                "yes","no","NOT","=="}));
        
        // !yes ?
        assertFalse(r.evaluate(new String[] {
                "yes","NOT"}));
    }
    
    public void testContains() throws Exception {
        RPNParser p = new RPNParser();
        assertTrue(p.evaluate(new String[] {"badger","adge","CONTAINS"}));
        assertFalse(p.evaluate(new String[] {"adge","badger","CONTAINS"}));
    }

    private static class MyLookup extends Properties implements StringLookup {
        public String lookup(String key) {
            return getProperty(key);
        }
    }
}
