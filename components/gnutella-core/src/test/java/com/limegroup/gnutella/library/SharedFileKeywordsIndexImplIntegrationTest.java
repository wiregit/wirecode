package com.limegroup.gnutella.library;

import static com.limegroup.gnutella.library.FileManagerTestUtils.assertAdds;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileChangedFails;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileChanges;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertFileRenames;
import static com.limegroup.gnutella.library.FileManagerTestUtils.assertLoads;
import static com.limegroup.gnutella.library.FileManagerTestUtils.change;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewExtensionTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewNamedTestFile;
import static com.limegroup.gnutella.library.FileManagerTestUtils.createNewTestFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.SearchSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.io.URNImpl;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.metadata.MetaDataFactoryImplTest;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class SharedFileKeywordsIndexImplIntegrationTest extends LimeTestCase {

    @Inject private QueryRequestFactory queryRequestFactory;
    @Inject private Library managedList;
    @Inject @GnutellaFiles private FileCollection fileList;
    @Inject private SharedFilesKeywordIndex keywordIndex;
    private Response[] responses;
    @Inject private Injector injector;
    @Inject private ResponseFactory responseFactory;

    private File f1, f2, f3;

    public SharedFileKeywordsIndexImplIntegrationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SharedFileKeywordsIndexImplIntegrationTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        LimeTestUtils.createInjectorNonEagerly(LimeTestUtils.createModule(this));
        injector.getInstance(ServiceRegistry.class).initialize();
        assertLoads(managedList); // Ensure it starts up & schemas load & all.
    }
    
    public void testOneSharedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);

        assertAdds(fileList, f1);

        // it is important to check the query at all bounds,
        // // including tests for case.
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("FileManager", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("test", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("file", (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
        responses = keywordIndex.query(queryRequestFactory.createQuery("FileManager UNIT tEsT",
                (byte) 3));
        assertEquals("Unexpected number of responses", 1, responses.length);
    }
    
    public void testResponseContainsNMS1Urn() throws Exception {
        SearchSettings.DESIRES_NMS1_URNS.setValue(true);
        f1 = createNewExtensionTestFile(4096, "mp3", _scratchDir);
        assertAdds(fileList, f1);
        FileDesc fileDesc = fileList.getFileDesc(f1);
        fileDesc.addUrn(URNImpl.createNMS1FromBytes(new byte[20]));
        URNImpl nms1Urn = fileDesc.getNMS1Urn();
        assertNotNull(nms1Urn);
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit"));
        assertEquals("Unexpected number of responses", 1, responses.length);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        responses[0].writeToStream(out);
        Response response = responseFactory.createFromStream(new ByteArrayInputStream(out.toByteArray()));
        assertContains(response.getUrns(), nms1Urn);
    }
    
    public void testResponseDoesNotContainNMS1Urn() throws Exception {
        SearchSettings.DESIRES_NMS1_URNS.setValue(false);
        f1 = createNewExtensionTestFile(4096, "mp3", _scratchDir);
        assertAdds(fileList, f1);
        FileDesc fileDesc = fileList.getFileDesc(f1);
        fileDesc.addUrn(URNImpl.createNMS1FromBytes(new byte[20]));
        URNImpl nms1Urn = fileDesc.getNMS1Urn();
        assertNotNull(nms1Urn);
        QueryRequest request = queryRequestFactory.createQuery("unit");
        assertFalse(request.desiresNMS1Urn());
        responses = keywordIndex.query(request);
        assertEquals("Unexpected number of responses", 1, responses.length);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        responses[0].writeToStream(out);
        Response response = responseFactory.createFromStream(new ByteArrayInputStream(out.toByteArray()));
        assertNotContains(response.getUrns(), nms1Urn);
    }
    
    public void testResponseContainsTorrentMetaData() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", MetaDataFactoryImplTest.class);
        assertAdds(fileList, torrentFile);
        responses = keywordIndex.query(queryRequestFactory.createQuery("messages"));
        assertEquals(1, responses.length);
        LimeXMLDocument xmlDocument = responses[0].getDocument();
        assertNotNull(xmlDocument);
        assertEquals("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ", xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH));
        assertEquals("http://localhost/announce", xmlDocument.getValue(LimeXMLNames.TORRENT_TRACKERS));
    }
    
    public void testFilesInsideOfTorrentAreIndexed() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", MetaDataFactoryImplTest.class);
        assertAdds(fileList, torrentFile);
        responses = keywordIndex.query(queryRequestFactory.createQuery("BTChokeTest"));
        assertEquals(1, responses.length);
        LimeXMLDocument xmlDocument = responses[0].getDocument();
        assertNotNull(xmlDocument);
        assertEquals("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ", xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH));
        assertEquals("http://localhost/announce", xmlDocument.getValue(LimeXMLNames.TORRENT_TRACKERS));
    }
    
    public void testFilesizesInsideOfTorrentAreIndexed() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", MetaDataFactoryImplTest.class);
        assertAdds(fileList, torrentFile);
        responses = keywordIndex.query(queryRequestFactory.createQuery("1311"));
        assertEquals(1, responses.length);
        LimeXMLDocument xmlDocument = responses[0].getDocument();
        assertNotNull(xmlDocument);
        assertEquals("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ", xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH));
        assertEquals("http://localhost/announce", xmlDocument.getValue(LimeXMLNames.TORRENT_TRACKERS));
    }
    
    public void testInfoHashInsideOfTorrentAreIndexed() throws Exception {
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", MetaDataFactoryImplTest.class);
        assertAdds(fileList, torrentFile);
        responses = keywordIndex.query(queryRequestFactory.createQuery("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ"));
        assertEquals(1, responses.length);
        LimeXMLDocument xmlDocument = responses[0].getDocument();
        assertNotNull(xmlDocument);
        assertEquals("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ", xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH));
        assertEquals("http://localhost/announce", xmlDocument.getValue(LimeXMLNames.TORRENT_TRACKERS));
    }
    
    public void testReturnsTorrentForTorrentKeyWordSearch() throws Exception {
        SearchSettings.APPEND_TORRENT_TO_TORRENT_QUERIES.set(true);
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", MetaDataFactoryImplTest.class);
        assertAdds(fileList, torrentFile);
        QueryRequest queryRequest = queryRequestFactory.createQuery(new GUID().bytes(), "messages", "", SearchCategory.TORRENT);
        assertTrue(queryRequest.getQuery().contains("torrent"));
        responses = keywordIndex.query(queryRequest);
        assertEquals(1, responses.length);
        LimeXMLDocument xmlDocument = responses[0].getDocument();
        assertNotNull(xmlDocument);
        assertEquals("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ", xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH));
        
        responses = keywordIndex.query(queryRequestFactory.createQuery(new GUID().bytes(), "messages", "", SearchCategory.AUDIO));
        assertEquals(0, responses.length);
    }
    
    public void testReturnsTorrentForTorrentTypeSearch() throws Exception {
        SearchSettings.APPEND_TORRENT_TO_TORRENT_QUERIES.set(false);
        File torrentFile = TestUtils.getResourceInPackage("messages.torrent", MetaDataFactoryImplTest.class);
        assertAdds(fileList, torrentFile);
        QueryRequest queryRequest = queryRequestFactory.createQuery(new GUID().bytes(), "messages", "", SearchCategory.TORRENT);
        assertFalse(queryRequest.getQuery().contains("torrent"));
        responses = keywordIndex.query(queryRequest);
        assertEquals(1, responses.length);
        LimeXMLDocument xmlDocument = responses[0].getDocument();
        assertNotNull(xmlDocument);
        assertEquals("OWVPM7JN7ZDRX6EHFMZAPJPHI3JXIUUZ", xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH));
        
        responses = keywordIndex.query(queryRequestFactory.createQuery(new GUID().bytes(), "messages", "", SearchCategory.AUDIO));
        assertEquals(0, responses.length);
    }
    
    public void testAddAnotherSharedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        
        assertAdds(fileList, f1, f2);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertNotEquals("responses gave same index", responses[0].getIndex(), responses[1].getIndex());
        for (int i = 0; i < responses.length; i++) {
            assertTrue("responses should be expected indexes",
                    responses[i].getIndex() == 0 || responses[i].getIndex() == 1);
        }
    }
    
    public void testRemovingOneSharedFile() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);

        // Remove file that's shared. Back to 1 file.
        fileList.remove(f2);
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
    }

    public void testAddAnotherSharedFileDifferentIndex() throws Exception {
        f1 = createNewTestFile(1, _scratchDir);
        f2 = createNewTestFile(3, _scratchDir);
        f3 = createNewTestFile(11, _scratchDir);
        
        assertAdds(fileList, f1, f2);
        fileList.remove(f2);
        assertAdds(fileList, f3);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("unit", (byte) 3));
        assertEquals("unexpected response length", 2, responses.length);
        assertNotEquals("unexpected response[0] index", 1, responses[0].getIndex());
        assertNotEquals("unexpected response[1] index", 1, responses[1].getIndex());
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("*unit*", (byte) 3));
        assertEquals("unexpected responses length", 2, responses.length);
    }
    
    public void testRenameFiles() throws Exception {
        f1 = createNewNamedTestFile(1, "1111", _scratchDir);
        f2 = createNewNamedTestFile(3, "2222", _scratchDir);
        f3 = createNewNamedTestFile(11, "3333", _scratchDir);
        
        assertAdds(fileList, f1, f3);
        assertEquals(2, fileList.size());       
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        try {
            managedList.fileRenamed(f1, new File(_scratchDir, "!<invalid file>")).get(1, TimeUnit.SECONDS);
            fail("should have failed");
        } catch(ExecutionException expected) {}
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        assertFileRenames(managedList, f3, f2);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        try {
            managedList.fileRenamed(f1, f3).get(1, TimeUnit.SECONDS);
            fail("should have failed");
        } catch(ExecutionException expected) {}
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("1111", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        responses = keywordIndex.query(queryRequestFactory.createQuery("2222", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);

        responses = keywordIndex.query(queryRequestFactory.createQuery("3333", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
    }

    public void testChangeSharedFile() throws Exception {
        f1 = createNewNamedTestFile(100, "name", _scratchDir);
        f2 = createNewTestFile(10, _scratchDir);
        
        assertAdds(fileList, f1);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);        
        
        change(f1);
        assertFileChanges(managedList, f1);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 1, responses.length);
        
        f1.delete();
        assertFileChangedFails(FileViewChangeFailedException.Reason.NOT_MANAGEABLE, managedList, f1);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
        
        assertFileChangedFails(FileViewChangeFailedException.Reason.OLD_WASNT_MANAGED, managedList, f2);
        responses = keywordIndex.query(queryRequestFactory.createQuery("name", (byte) 3));
        assertEquals("unexpected response length", 0, responses.length);
    }
    
}
