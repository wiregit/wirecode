package org.limewire.inspection;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.inspection.Inspectable.InspectableForSize;
import org.limewire.inspection.Inspectable.InspectablePrimitive;
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
        String invalid = InspectionUtils.inspectValue("org.limewire.inspection.TestClass");
        assertEquals("invalid field", invalid);
        String notFound = InspectionUtils.inspectValue("org.limewire.not.existing.class,a");
        assertTrue(notFound.contains("Exception"));
        String methodNotFound = InspectionUtils.inspectValue("org.limewire.inspection.TestClass,wrongField");
        assertTrue(methodNotFound.contains("Exception"));
    }
    
    public void testInspectablePrimitive() throws Exception {
        String notInspectable = InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference2");
        assertTrue(notInspectable.contains("not inspectable"));
        
        TestClass t = new TestClass();
        t.memberString = "a";
        t.inspectableString = "b";
        TestClass.reference1 = t;
        notInspectable = InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,memberString");
        assertTrue(notInspectable.contains("not inspectable"));
        
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
        
        String notInspectable = InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,memberList");
        assertTrue(notInspectable.contains("not inspectable"));
        
        String inspectable = InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,inspectableList");
        assertEquals("2",inspectable);
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
