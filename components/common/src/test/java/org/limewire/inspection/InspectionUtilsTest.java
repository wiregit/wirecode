package org.limewire.inspection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class InspectionUtilsTest extends BaseTestCase {
    public InspectionUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(InspectionUtilsTest.class);
    }
    
    public void testTraverse() throws Exception {
        TestClass t1 = new TestClass();
        TestClass2 t2 = new TestClass2();
        
        /*
         * Loop:
         * t1 --> t2
         * t2 --> t1
         */
        TestClass.reference2 = t2;
        TestClass2.reference1 = t1;
        t1.reference = new InspectableClass("inspectable1");
        t2.reference = new InspectableClass("inspectable2");
        
        assertEquals("inspectable1",
                InspectionUtils.inspectValue(
                        "org.limewire.inspection.TestClass,reference2,reference1,reference"));

        assertEquals("inspectable2",
                InspectionUtils.inspectValue(
                        "org.limewire.inspection.TestClass2,reference1,reference2," +
                        "reference1,reference2,reference1,reference2,reference"));
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass");
            fail("invalid value");
        } catch (InspectionException expcted){}
        
        try {
            InspectionUtils.inspectValue("org.limewire.not.existing.class,a");
            fail("class should not exist");
        } catch (InspectionException expcted){}
        
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass,wrongField");
            fail("field should not be found");
        } catch (InspectionException expcted){}
    }
    
    public void testStaticMembersTraversed() throws Exception {
        TestClass t = new TestClass();
        TestClass2 t2 = new TestClass2();
        TestClass.reference2 = t2;
        TestClass.reference1 = t;
        TestClass2.reference1 = t;
        t.reference = t2;
        t2.reference = new InspectableClass("test");
        
        /*
         * traverse
         * t1 -> self reference(static) -> t2 -> t1(static) -> t2 (static) -> t1 ->t2 -> Inspectable
         */
        assertEquals("test",
                InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,reference,reference1,reference,reference"));
    }
    
    public void testInspectablePrimitive() throws Exception {
        TestClass t = new TestClass();
        t.memberString = "a";
        t.inspectableString = "b";
        TestClass.reference1 = t;
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,memberString");
            fail("should not be inspectable");
        } catch (InspectionException expcted){}
        
        String inspectable = InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,inspectableString");
        assertEquals("b", inspectable);
    }
    
    
    @SuppressWarnings("unchecked")
    public void testInspectableForSize() throws Exception {
        TestClass t = new TestClass();
        t.memberList = new ArrayList();
        t.inspectableList = new ArrayList();
        t.memberList.add(new Object());
        t.inspectableList.add(new Object());
        t.inspectableList.add(new Object());
        TestClass.reference1 = t;
        
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,memberList");
            fail("should not be inspectable for size");
        } catch (InspectionException expcted){}
        
        String inspectable = InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,inspectableList");
        assertEquals("2",inspectable);
    }
    
    public void testBoxing() throws Exception {
        PrivateInts t = new PrivateInts(1,2);
        PrivateInts.self = t;
        
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.PrivateInts,self,memberInt");
            fail("should not be inspectable");
        } catch (InspectionException expcted){}
        
        String inspectable = InspectionUtils.inspectValue("org.limewire.inspection.PrivateInts,self,inspectableInt");
        assertEquals("2",inspectable);
    }
    
    /**
     * tests that the field access is modified only for the internally
     * used Field object.
     */
    public void testAccess() throws Exception {
        PrivateInts t = new PrivateInts(1,2);
        PrivateInts.self = t;
        Field f = t.getClass().getDeclaredField("inspectableInt");
        assertFalse(f.isAccessible());
        String inspectable = InspectionUtils.inspectValue("org.limewire.inspection.PrivateInts,self,inspectableInt");
        assertEquals("2",inspectable);
        assertFalse(f.isAccessible());
    }
}

class TestClass {
    static TestClass reference1;
    static TestClass2 reference2;
    
    String memberString;
    @InspectablePrimitive
    String inspectableString;
    
    List memberList;
    @InspectableForSize
    List inspectableList;
    
    Object reference;
    
    public String toString() {
        return "testclass";
    }
}

class TestClass2 {
    static TestClass reference1;
    
    Object reference;
    
    public String toString() {
        return "testclass2";
    }
}

class InspectableClass implements Inspectable {
    String s;
    InspectableClass(String s) {
        this.s = s;
    }
    public String inspect() {
        return s;
    }
}

@SuppressWarnings("unused")
class PrivateInts {
    static PrivateInts self;
    private int memberInt;
    @InspectablePrimitive
    private int inspectableInt;
    PrivateInts(int a, int b) {
        memberInt = a;
        inspectableInt = b;
    }
}