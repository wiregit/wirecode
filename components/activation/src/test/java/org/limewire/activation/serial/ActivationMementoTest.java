package org.limewire.activation.serial;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.gnutella.tests.LimeTestCase;

public class ActivationMementoTest extends LimeTestCase {

    public ActivationMementoTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActivationMementoTest.class);
    }
    
    public void testNullFields() throws Exception {
        ActivationMemento memento = new ActivationMementoImpl();
        assertEquals(-1, memento.getDateExpired());
        assertEquals(-1, memento.getDatePurchased());
        assertEquals(-1, memento.getID());
        assertEquals(null, memento.getLicenseName());
        assertEquals(null, memento.getStatus());
    }

    public void testExistingFields() throws Exception {
        ActivationMemento memento = new ActivationMementoImpl();
        memento.setID(0);
        memento.setLicenseName("test module");
        memento.setDatePurchased(101);
        memento.setDateExpired(100);
        memento.setStatus(Status.ACTIVE);
        
        File tmp = File.createTempFile("lwc", "save");
        tmp.delete();
        tmp.deleteOnExit();
        
        List<ActivationMemento> mementos = new ArrayList<ActivationMemento>();
        mementos.add(memento);
        
        ActivationSerializerSettings settings  = new ActivationSerializerSettingsStub(tmp, tmp);
        ActivationSerializer serializer = new ActivationSerializerImpl(settings);
        serializer.writeToDisk(mementos);
        
        mementos = serializer.readFromDisk();

        assertEquals(mementos.get(0).getID(), memento.getID());
        assertEquals(mementos.get(0).getDatePurchased(), memento.getDatePurchased());
        assertEquals(mementos.get(0).getDateExpired(), memento.getDateExpired());
        assertEquals(mementos.get(0).getLicenseName(), memento.getLicenseName());
        assertEquals(mementos.get(0).getStatus(), memento.getStatus());
    }
    
}
