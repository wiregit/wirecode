package org.limewire.io;

import java.util.Arrays;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class IpPortSetTest extends BaseTestCase {

    public IpPortSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IpPortSetTest.class);
    }
    
    public void testRemoveAll() throws Exception {
        IpPort ipPort1 = new IpPortImpl("191.1.1.1", 4545);
        IpPort ipPort2 = new IpPortImpl("111.1.1.1", 5555);
        
        IpPortSet set = new IpPortSet(ipPort1, ipPort2);
        
        set.removeAll(Arrays.asList(new IpPortImpl(ipPort1.getAddress(), ipPort1.getPort()),
                new IpPortImpl(ipPort2.getAddress(), ipPort2.getPort())));
        
        assertTrue(set.isEmpty());
    }
    
    public void testIpComparatorIgnoresPort() throws Exception{
        IpPort ipPort1 = new IpPortImpl("191.1.1.1", 4545);
        IpPort ipPort2 = new IpPortImpl("191.1.1.1", 5555);
        
        assertEquals(0, IpPort.IP_COMPARATOR.compare(ipPort1, ipPort2));        
    }
    
    public void testIpComparatorOrder() throws Exception{
        IpPort higherIP = new IpPortImpl("11.1.1.1", 0);
        IpPort lowerIP = new IpPortImpl("2.1.1.1", 0);

        assertTrue(IpPort.IP_COMPARATOR.compare(higherIP, lowerIP) > 0);  
        assertTrue(IpPort.IP_COMPARATOR.compare(lowerIP, higherIP) < 0);   
        
        higherIP = new IpPortImpl("191.13.1.1", 0);
        lowerIP = new IpPortImpl("191.2.1.1", 0);

        assertTrue(IpPort.IP_COMPARATOR.compare(higherIP, lowerIP) > 0);  
        assertTrue(IpPort.IP_COMPARATOR.compare(lowerIP, higherIP) < 0);

        higherIP = new IpPortImpl("191.1.12.1", 0);
        lowerIP = new IpPortImpl("191.1.2.1", 0);

        assertTrue(IpPort.IP_COMPARATOR.compare(higherIP, lowerIP) > 0);  
        assertTrue(IpPort.IP_COMPARATOR.compare(lowerIP, higherIP) < 0);  
        
        higherIP = new IpPortImpl("191.1.1.12", 0);
        lowerIP = new IpPortImpl("191.1.1.3", 0);

        assertTrue(IpPort.IP_COMPARATOR.compare(higherIP, lowerIP) > 0);  
        assertTrue(IpPort.IP_COMPARATOR.compare(lowerIP, higherIP) < 0);    
        

        //also make sure values above Byte.MAX_VALUE are correct
        higherIP = new IpPortImpl("220.1.1.1", 0);
        lowerIP = new IpPortImpl("5.1.1.1", 0);
        
        assertTrue(IpPort.IP_COMPARATOR.compare(higherIP, lowerIP) > 0);  
        assertTrue(IpPort.IP_COMPARATOR.compare(lowerIP, higherIP) < 0); 

        higherIP = new IpPortImpl("220.1.1.1", 0);
        lowerIP = new IpPortImpl("156.1.1.1", 0);
        
        assertTrue(IpPort.IP_COMPARATOR.compare(higherIP, lowerIP) > 0);  
        assertTrue(IpPort.IP_COMPARATOR.compare(lowerIP, higherIP) < 0); 
    }

}
