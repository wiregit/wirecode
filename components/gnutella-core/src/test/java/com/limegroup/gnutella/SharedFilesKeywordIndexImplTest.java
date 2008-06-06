package com.limegroup.gnutella;

import java.io.File;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.IntSet;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.FileManagerEvent.Type;

public class SharedFilesKeywordIndexImplTest extends BaseTestCase {


    private SharedFilesKeywordIndexImpl keywordIndex;
    private Mockery context;
    private FileManager fileManager;
    private FileList sharedFileList;

    public SharedFilesKeywordIndexImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileManager = context.mock(FileManager.class);
        sharedFileList = context.mock(FileList.class);
        keywordIndex = new SharedFilesKeywordIndexImpl(fileManager, null, null, null, null);
    }
    
    public void testRenamedFilesEvent() throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        final FileDesc originalFile = new FileDesc(new File("hello world"), new UrnSet(urn), 1);
        final FileDesc newFile = new FileDesc(new File("goodbye world"), new UrnSet(urn), 2);
        
        
        context.checking(new Expectations() {
            {
                atLeast(1).of(fileManager).getSharedFileList();
                will(returnValue(sharedFileList));
                atLeast(1).of(sharedFileList).contains(originalFile.getFile());
                will(returnValue(true));
                
                atLeast(1).of(fileManager).getSharedFileList();
                will(returnValue(sharedFileList));
                atLeast(1).of(sharedFileList).contains(newFile.getFile());
                will(returnValue(true));
            }
        });
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