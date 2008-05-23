package com.limegroup.gnutella.gui.search;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.limewire.io.NetworkUtils;
import org.limewire.security.SecureMessage;
import org.xml.sax.SAXException;

import com.google.inject.Injector;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.SchemaNotFoundException;

public class SharedSearchResultTest extends GUIBaseTestCase {
    
    Injector injector;
    
    public SharedSearchResultTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        injector = LimeTestUtils.createInjector(GUI_CORE_MEDIATOR_INJECTION);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    private SharedSearchResult getSearchResult(FileDesc fileDesc) {
        CreationTimeCache creationTimeCache = injector.getInstance(CreationTimeCache.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        return new SharedSearchResult(fileDesc, creationTimeCache, networkManager);
    }
    
    public void testSharedSearchResult() throws IOException, InterruptedException, SAXException, SchemaNotFoundException {        
        Set<URN> urns = new HashSet<URN>();
        File file = new File("pom.xml");
        URN sha1Urn = URN.createSHA1Urn(file);
        urns.add(sha1Urn);
        FileDesc fileDesc = new FileDesc(file, urns, 0);
        
        CreationTimeCache creationTimeCache = injector.getInstance(CreationTimeCache.class);
        creationTimeCache.addTime(sha1Urn, fileDesc.getFile().lastModified());
        creationTimeCache.commitTime(sha1Urn);
        
        LimeXMLDocument document = injector.getInstance(LimeXMLDocumentFactory.class).createLimeXMLDocument("<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "  <audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
        "</audios>");
        fileDesc.addLimeXMLDocument(document);
        SharedSearchResult result = getSearchResult(fileDesc);
        assertEquals(false, result.isDownloading());
        assertEquals(false, result.isMeasuredSpeed());
        assertEquals(creationTimeCache.getCreationTimeAsLong(fileDesc.getSHA1Urn()), result.getCreationTime());
        assertEquals("pom.xml", result.getFileName());
        assertEquals(NetworkUtils.ip2string(GuiCoreMediator.getNetworkManager().getAddress()), result.getHost());
        assertEquals(0, result.getQuality());
        assertEquals(SecureMessage.INSECURE, result.getSecureStatus());
        assertEquals(sha1Urn, result.getSHA1Urn());
        assertEquals(fileDesc.getFile().length(), result.getSize());
        assertEquals(0.f, result.getSpamRating());
        assertEquals(0, result.getSpeed());
        assertEquals(LimeWireUtils.QHD_VENDOR_NAME, result.getVendor());
        assertEquals(document, result.getXMLDocument());
    }
    
    public void testMultipleLimeXMLDocs() throws IOException, InterruptedException, SAXException, SchemaNotFoundException {        
        Set<URN> urns = new HashSet<URN>();
        File file = new File("pom.xml");
        URN sha1Urn = URN.createSHA1Urn(file);
        urns.add(sha1Urn);
        FileDesc fileDesc = new FileDesc(file, urns, 0);
        
        CreationTimeCache creationTimeCache = injector.getInstance(CreationTimeCache.class);
        creationTimeCache.addTime(sha1Urn, fileDesc.getFile().lastModified());
        creationTimeCache.commitTime(sha1Urn);
        
        LimeXMLDocument document = injector.getInstance(LimeXMLDocumentFactory.class).createLimeXMLDocument("<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "  <audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
        "</audios>");
        fileDesc.addLimeXMLDocument(document);
        LimeXMLDocument document2 = injector.getInstance(LimeXMLDocumentFactory.class).createLimeXMLDocument("<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "  <audio genre=\"Rock\" identifier=\"def2.txt\" bitrate=\"190\"/>"+
        "</audios>");
        fileDesc.addLimeXMLDocument(document2);
        SharedSearchResult result = getSearchResult(fileDesc);
        assertEquals(null, result.getXMLDocument());
    }
    
    public void testNullLimeXMLDocs() throws IOException, InterruptedException, SAXException, SchemaNotFoundException {        
        Set<URN> urns = new HashSet<URN>();
        File file = new File("pom.xml");
        URN sha1Urn = URN.createSHA1Urn(file);
        urns.add(sha1Urn);
        FileDesc fileDesc = new FileDesc(file, urns, 0);
        
        CreationTimeCache creationTimeCache = injector.getInstance(CreationTimeCache.class);
        creationTimeCache.addTime(sha1Urn, fileDesc.getFile().lastModified());
        creationTimeCache.commitTime(sha1Urn);
        
        SharedSearchResult result = getSearchResult(fileDesc);
        assertEquals(null, result.getXMLDocument());
    }
}
