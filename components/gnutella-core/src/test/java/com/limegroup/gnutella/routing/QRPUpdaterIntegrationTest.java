package com.limegroup.gnutella.routing;

import java.io.File;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.metadata.MetaDataFactoryImplTest;
import com.limegroup.gnutella.routing.QRPUpdaterTest.QueryRequestImpl;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class QRPUpdaterIntegrationTest extends LimeTestCase {
    
    @Inject private QRPUpdater qrpUpdater;
    @Inject private Injector injector;
    @Inject @GnutellaFiles private FileCollection gnutellaFileCollection;
    @Inject private Library managedList;
    
    public static Test suite() {
        return buildTestSuite(QRPUpdaterIntegrationTest.class);
    }
        
    @Override
    protected void setUp() throws Exception {
        LimeTestUtils.createInjectorNonEagerly(LimeTestUtils.createModule(this));
        injector.getInstance(ServiceRegistry.class).initialize();
        FileManagerTestUtils.assertLoads(managedList);
    }
    
    public void testQRTDoesNotContainCertainTorrentMetaData() throws Exception {
        QueryRouteTable table = qrpUpdater.getQRT();
        
        assertFalse(table.contains(new QueryRequestImpl("1307")));
        
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", MetaDataFactoryImplTest.class);
        assertNotNull(torrentFile);
        FileDesc fileDesc = gnutellaFileCollection.add(torrentFile).get(2, TimeUnit.SECONDS);
        assertNotNull(fileDesc);
        LimeXMLDocument xmlDocument = fileDesc.getXMLDocument();
        assertNotNull(xmlDocument);
        assertEquals("1307 1896 1247 1434 1286 1311 1942 1902 1263", xmlDocument.getValue(LimeXMLNames.TORRENT_FILE_SIZES));
        
        table = qrpUpdater.getQRT();
        assertFalse(table.contains(new QueryRequestImpl("1307")));
        assertTrue(table.contains(new QueryRequestImpl("torrent")));
        assertTrue(table.contains(new QueryRequestImpl("messages")));
        assertTrue(table.contains(new QueryRequestImpl("BTChokeTest")));
        assertTrue(table.contains(new QueryRequestImpl("BTChokeTest.class")));
        assertFalse(table.contains(new QueryRequestImpl("localhost")));
        assertFalse(table.contains(new QueryRequestImpl("http")));
        assertFalse(table.contains(new QueryRequestImpl("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ")));
    }
}
