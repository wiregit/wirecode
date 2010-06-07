package org.limewire.mojito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

import junit.framework.TestSuite;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.RouteTableImpl;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;

public class SerializeTest extends MojitoTestCase {
    
    public SerializeTest(String name) {
        super(name);
    }
   
    public static TestSuite suite() {
        return buildTestSuite(SerializeTest.class);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSerializeRouteTable() throws IOException, ClassNotFoundException {
        RouteTable routeTable1 = new RouteTableImpl();
        for (int i = 0; i < 100; i++) {
            Contact node = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 2000 + i));
            routeTable1.add(node);
        }
        
        assertEquals(101, routeTable1.getContacts().size());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(routeTable1);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        RouteTable routeTable2 = (RouteTable)ois.readObject();
        ois.close();
        
        assertNotSame(routeTable1, routeTable2);
        assertEquals(101, routeTable2.getContacts().size());
        for (Contact node : routeTable1.getContacts()) {
            Contact other = routeTable2.get(node.getContactId());
            assertNotNull(other);
            assertEquals(node, other);
            assertNotSame(node, other);
        }
    }
}
