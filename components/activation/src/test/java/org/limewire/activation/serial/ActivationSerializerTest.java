package org.limewire.activation.serial;

import java.io.File;

import junit.framework.Test;

import org.limewire.activation.api.ActivationItem;
import org.limewire.gnutella.tests.LimeTestCase;

public class ActivationSerializerTest extends LimeTestCase {

    public ActivationSerializerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActivationSerializerTest.class);
    }
    
    public void testLoadAndSaveEmptyList() throws Exception {
//        File tmp = getFile();
//        
//        ActivationSerializerSettings settings  = new ActivationSerializerSettingsStub(tmp, tmp);
//        ActivationSerializer serializer = new ActivationSerializerImpl(settings);
//        serializer.writeToDisk(new ArrayList<ActivationMemento>());
//        
//        List<ActivationMemento> mementos = serializer.readFromDisk();
//        assertEquals(0, mementos.size());
    }
    
    public void testLoadAndSaveSingular() throws Exception {
//        File tmp = getFile();
//        
//        ActivationItemFactory factory = new ActivationItemFactoryImpl();
//        
//        ActivationItem item = factory.createActivationItem(0, "Test Module", new Date(), new Date(), Status.ACTIVE);
//        ActivationMemento memento = ((ActivationItemImpl)item).toActivationMemento();
//        List<ActivationMemento> mementos = new ArrayList<ActivationMemento>();
//        mementos.add(memento);
//        
//        ActivationSerializerSettings settings  = new ActivationSerializerSettingsStub(tmp, tmp);
//        ActivationSerializer serializer = new ActivationSerializerImpl(settings);
//        serializer.writeToDisk(mementos);
//        
//        mementos = serializer.readFromDisk();
//        assertEquals(1, mementos.size());
//        ActivationItem deserializedItem  = factory.createActivationItem(mementos.get(0));
//        assertEquals(item, deserializedItem);
    }
    
    public void testLoadAndSaveList() throws Exception {
//        File tmp = getFile();
//        
//        ActivationItemFactory factory = new ActivationItemFactoryImpl();
//
//        List<ActivationMemento> mementos = new ArrayList<ActivationMemento>();
//        ActivationItem item1 = factory.createActivationItem(1, "Test Module1", new Date(1), new Date(1), Status.ACTIVE);
//        ActivationItem item2 = factory.createActivationItem(2, "Test Module2", new Date(2), new Date(2), Status.UNAVAILABLE);
//        ActivationItem item3 = factory.createActivationItem(3, "Test Module3", new Date(3), new Date(3), Status.EXPIRED);
//        
//        ActivationMemento memento1 = ((ActivationItemImpl)item1).toActivationMemento();
//        mementos.add(memento1);
//        ActivationMemento memento2 = ((ActivationItemImpl)item2).toActivationMemento();
//        mementos.add(memento2);
//        ActivationMemento memento3 = ((ActivationItemImpl)item3).toActivationMemento();
//        mementos.add(memento3);
//        
//        ActivationSerializerSettings settings  = new ActivationSerializerSettingsStub(tmp, tmp);
//        ActivationSerializer serializer = new ActivationSerializerImpl(settings);
//        serializer.writeToDisk(mementos);
//        
//        mementos = serializer.readFromDisk();
//        assertEquals(3, mementos.size());
//        ActivationItem deserializedItem1  = factory.createActivationItem(mementos.get(0));
//        assertEquals(item1, deserializedItem1);
//        ActivationItem deserializedItem2  = factory.createActivationItem(mementos.get(1));
//        assertEquals(item2, deserializedItem2);
//        ActivationItem deserializedItem3  = factory.createActivationItem(mementos.get(2));
//        assertEquals(item3, deserializedItem3);
    }
    
    private void assertEquals(ActivationItem item1, ActivationItem item2) {
        assertEquals(item1.getModuleID(), item2.getModuleID());
        assertEquals(item1.getLicenseName(), item2.getLicenseName());
        assertEquals(item1.getDatePurchased(), item2.getDatePurchased());
        assertEquals(item1.getDateExpired(), item2.getDateExpired());
        assertEquals(item1.getStatus(), item2.getStatus());
    }
    
    private File getFile() throws Exception {
        File tmp = File.createTempFile("lwc", "save");
        tmp.delete();
        tmp.deleteOnExit();
        
        return tmp;
    }
}
