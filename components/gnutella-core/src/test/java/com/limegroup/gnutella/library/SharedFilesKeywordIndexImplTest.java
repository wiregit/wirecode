package com.limegroup.gnutella.library;

import java.io.File;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.IntSet;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFilesKeywordIndexImpl;
import com.limegroup.gnutella.library.FileManagerEvent.Type;

public class SharedFilesKeywordIndexImplTest extends BaseTestCase {


    private SharedFilesKeywordIndexImpl keywordIndex;
    private Mockery context;
    private FileManager fileManager;
    private FileList sharedFileList;
    private FileList incompleteFileList;

    public SharedFilesKeywordIndexImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        fileManager = context.mock(FileManager.class);
        sharedFileList = context.mock(FileList.class);
        incompleteFileList = context.mock(FileList.class);
        keywordIndex = new SharedFilesKeywordIndexImpl(fileManager, null, null, null, null, null);
    }
    
    public void testRenamedFilesEvent() throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        final FileDesc originalFile = new FileDesc(new File("hello world"), new UrnSet(urn), 1);
        final FileDesc newFile = new FileDesc(new File("goodbye world"), new UrnSet(urn), 2);
        
        
        context.checking(new Expectations() {
            {
                atLeast(1).of(fileManager).getIncompleteFileList();
                will(returnValue(incompleteFileList));
                atLeast(1).of(incompleteFileList).contains(originalFile);
                will(returnValue(false));
                atLeast(1).of(fileManager).getGnutellaSharedFileList();
                will(returnValue(sharedFileList));
                atLeast(1).of(sharedFileList).contains(originalFile);
                will(returnValue(true));
                
                atLeast(1).of(fileManager).getIncompleteFileList();
                will(returnValue(incompleteFileList));
                atLeast(1).of(incompleteFileList).contains(originalFile);
                will(returnValue(false));
                atLeast(1).of(fileManager).getIncompleteFileList();
                will(returnValue(incompleteFileList));
                atLeast(1).of(incompleteFileList).contains(newFile);
                will(returnValue(false));
                atLeast(1).of(fileManager).getGnutellaSharedFileList();
                will(returnValue(sharedFileList));
                atLeast(1).of(sharedFileList).contains(newFile);
                will(returnValue(true));
            }
        });
        keywordIndex.handleEvent(new FileManagerEvent(fileManager, Type.ADD_FILE, originalFile));
        
        IntSet result = keywordIndex.search("world hello", null, false);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(1));
        
        result = keywordIndex.search("goodbye world", null, false);
        assertNull(result);

        keywordIndex.handleEvent(new FileManagerEvent(fileManager, Type.RENAME_FILE, originalFile, newFile));
        
        result = keywordIndex.search("world goodbye", null, false);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(2));
        
        result = keywordIndex.search("hello world", null, false);
        assertNull(result);
    }

}