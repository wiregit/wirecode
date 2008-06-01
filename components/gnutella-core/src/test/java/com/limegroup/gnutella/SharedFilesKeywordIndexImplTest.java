package com.limegroup.gnutella;

import java.io.File;

import org.jmock.Mockery;
import org.limewire.collection.IntSet;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.FileManagerEvent.Type;

public class SharedFilesKeywordIndexImplTest extends BaseTestCase {


    private SharedFilesKeywordIndexImpl keywordIndex;
    private Mockery context;
    private FileManager fileManager;

    public SharedFilesKeywordIndexImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileManager = context.mock(FileManager.class);
        keywordIndex = new SharedFilesKeywordIndexImpl(fileManager, null, null, null, null);
    }
    
    public void testRenamedFilesEvent() throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        FileDesc originalFile = new FileDesc(new File("hello world"), new UrnSet(urn), 1);
        FileDesc newFile = new FileDesc(new File("goodbye world"), new UrnSet(urn), 2);
        
        keywordIndex.handleFileEvent(new FileManagerEvent(fileManager, Type.ADD_FILE, originalFile));
        
        IntSet result = keywordIndex.search("world hello", null, false);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(1));
        
        result = keywordIndex.search("goodbye world", null, false);
        assertNull(result);
        
        keywordIndex.handleFileEvent(new FileManagerEvent(fileManager, Type.RENAME_FILE, originalFile, newFile));
        
        result = keywordIndex.search("world goodbye", null, false);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(2));
        
        result = keywordIndex.search("hello world", null, false);
        assertNull(result);
    }
}