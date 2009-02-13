package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.util.SwingTestCase;

public class LibraryManagerModelTest extends SwingTestCase {
    
    private Mockery context;
    private LibraryData libraryData;
    
    public LibraryManagerModelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LibraryManagerModelTest.class);
    }
    
    /**
     * Single root with no children
     */
    public void testSimpleRoot() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ArrayList<File>());
        
        LibraryManagerItem[] items = model.getPathToRoot(root);
        assertEquals(1, items.length);
        assertEquals(root, items[0]);
        
        RootLibraryManagerItem currentRoot = model.getRoot();
        assertEquals(root, currentRoot);
        
        assertEquals(0, model.getAllExcludedSubfolders().size());
        
        assertEquals(0, model.getRootChildrenAsFiles().size());
    }
    
    public void testSingleRootOneChild() throws Exception {
        //single root with one child
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        final File childFolder = new File(baseDir, "subFolder");
        childFolder.mkdir();
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, childFolder, false);
        
        root.addChild(item);
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ArrayList<File>());
        
        //child returns parent root
        LibraryManagerItem[] items = model.getPathToRoot(item);
        assertEquals(2, items.length);
        assertEquals(root, items[0]);
        
        //find child of root
        LibraryManagerItem childOf = model.getChild(root, 0);
        assertNotNull(childOf);
        assertEquals(item, childOf);
        // should only be one child of root
        childOf = model.getChild(root, 1);
        assertNull(childOf);
        
        //child count
        int numChildren = model.getChildCount(root);
        assertEquals(1, numChildren);
        numChildren = model.getChildCount(item);
        assertEquals(0, numChildren);
        
        //child files
        Collection<File> files = model.getRootChildrenAsFiles();
        assertEquals(1, files.size());
        assertEquals(childFolder, files.iterator().next());
        
        //excluded folders
        files = model.getAllExcludedSubfolders();
        assertEquals(0, files.size());
        
        assertEquals(0, model.getIndexOfChild(root, item));
        
        childFolder.delete();
    }
    
    /**
     * Recurses through the tree using the model to retrieve children
     * of a given node
     */
    public void testChildrenRecursion() throws Exception {
        setupMockery();
        
        final File firstFolder = new File(baseDir, "firstFolder");
        final File secondFolder = new File(firstFolder, "secondFolder");
        final File thirdFolder = new File(secondFolder, "thirdFolder");
        final File fourthFolder = new File(secondFolder, "fourthFolder");

        firstFolder.mkdir();
        secondFolder.mkdir();
        thirdFolder.mkdir();
        fourthFolder.mkdir();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
               
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(thirdFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(fourthFolder);
            will(returnValue(true));
        }});
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ArrayList<File>());
        
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, firstFolder, true);
        
        // add the firstFolder to the root
        model.addChild(firstItem, root);
        
        assertEquals(1, model.getChildCount(root));

        LibraryManagerItem item = model.getChild(root, 0);
        //look at firstFolder children
        assertEquals(1, model.getChildCount(item));
        assertEquals(firstFolder, item.getFile());
        
        //get firstFolder's child
        item = model.getChild(item, 0);
        assertEquals(2, model.getChildCount(item));
        assertEquals(secondFolder, item.getFile());
        
        firstFolder.delete();
        secondFolder.delete();
        thirdFolder.delete();
        fourthFolder.delete();
    }
    
    public void testAddRemoveChild() throws Exception {
        setupMockery();
        
        final File firstFolder = new File(baseDir, "firstFolder");
        final File secondFolder = new File(firstFolder, "secondFolder");
        
        firstFolder.mkdir();
        secondFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
        }});
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ArrayList<File>());
        
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, firstFolder, true);
        
        model.addChild(firstItem, root);
        
        assertEquals(1, model.getChildCount(firstItem));
        
        LibraryManagerItem childItem = firstItem.getChildFor(secondFolder);
        
        //remove child
        model.removeChild(childItem);
        assertEquals(0, model.getChildCount(firstItem));
        assertEquals(-1, model.getIndexOfChild(childItem, firstItem));
        
        // add child
        model.addChild(childItem, firstItem);
        assertEquals(1, model.getChildCount(firstItem));
        assertEquals(0, model.getIndexOfChild(firstItem, childItem));
        
        firstFolder.delete();
        secondFolder.delete();
    }
    
    /**
     * Removes current children of the root and replaces them with
     * new children.
     */
    public void testReplaceRoots() throws Exception {
        final File firstFolder = new File(baseDir, "firstFolder");
        final File secondFolder = new File(baseDir, "secondFolder");
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, firstFolder, true);
        root.addChild(firstItem);
        
        LibraryManagerItem secondItem = new LibraryManagerItemImpl(root, libraryData, secondFolder, true);
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ArrayList<File>());
        
        List<LibraryManagerItem> items = model.getRoot().getChildren();
        
        assertEquals(1, items.size());
        assertEquals(firstItem, items.get(0));
        
        model.setRootChildren(Arrays.asList(new LibraryManagerItem[]{secondItem}));
        items = model.getRoot().getChildren();
        assertEquals(1, items.size());
        assertEquals(secondItem, items.get(0));
        
        firstFolder.delete();
        secondFolder.delete();
    }

    public void testSingleRootExcludedChild() throws Exception {
        //single root with one excluded child
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        final File childFolder = new File(baseDir, "subFolder");
        childFolder.mkdir();
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, childFolder, false);
        
        root.addChild(item);
        root.removeChild(item);
        
        Collection<File> files = new ArrayList<File>();
        files.add(childFolder);
        LibraryManagerModel model = new LibraryManagerModel(root, files);
        
        Collection<File> exclusions = model.getAllExcludedSubfolders();
        assertEquals(1, exclusions.size());
        
        exclusions = model.getRootChildrenAsFiles();
        assertEquals(0, exclusions.size());
        
        assertEquals(-1, model.getIndexOfChild(root, item));
        
        model.unexcludeAllSubfolders(baseDir);
        exclusions = model.getAllExcludedSubfolders();
        assertEquals(0, exclusions.size());
    }
    
    /**
     * Unexcludes children of a folder.
     */
    public void testUnexcludeChildren() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        final File firstFolder = new File(baseDir, "firstFolder");
        final File secondFolder = new File(firstFolder, "secondFolder");
        final File anotherFolder = new File(baseDir, "anotherFolder");
        final File anotherExludedFolder = new File(anotherFolder, "anotherExcludedFolder");
        
        firstFolder.mkdir();
        secondFolder.mkdir();
        anotherFolder.mkdir();
        anotherExludedFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryExcluded(secondFolder);
            will(returnValue(true));
        }});
        
        //add two roots
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, firstFolder, false);
        LibraryManagerItem secondItem = new LibraryManagerItemImpl(firstItem, libraryData, secondFolder, true);
        firstItem.addChild(secondItem);
        root.addChild(firstItem);
        
        LibraryManagerItem anotherItem = new LibraryManagerItemImpl(root, libraryData, anotherFolder, false);
        root.addChild(anotherItem);
        
        //add both children of roots as excluded folders
        Collection<File> collection = new ArrayList<File>();
        collection.add(secondFolder);
        collection.add(anotherExludedFolder);
        
        LibraryManagerModel model = new LibraryManagerModel(root, collection);
        
        assertEquals(2, model.getRootChildrenAsFiles().size());
        assertEquals(2, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        
        assertEquals(1, firstItem.getChildren().size());
        assertEquals(1, firstItem.getExcludedChildren().size());
        assertEquals(secondFolder, firstItem.getExcludedChildren().iterator().next());
        
        //unexclude the child
        model.unexcludeAllSubfolders(firstFolder);
        
        //only anotherExcludedChild should still exist in the list
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertTrue(model.getAllExcludedSubfolders().contains(anotherExludedFolder));
    }
    
    /**
     * restoreChildren is called when the children haven't been explicitely added to the parent
     * and all its children are to be unexcluded.
     */
    public void testRestoreChildren() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        final File firstFolder = new File(baseDir, "firstFolder");
        final File secondFolder = new File(firstFolder, "secondFolder");
        final File anotherFolder = new File(baseDir, "anotherFolder");
        final File anotherExludedFolder = new File(anotherFolder, "anotherExcludedFolder");
        
        firstFolder.mkdir();
        secondFolder.mkdir();
        anotherFolder.mkdir();
        anotherExludedFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryExcluded(secondFolder);
            will(returnValue(true));
        }});
                
        //add two roots
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, firstFolder, false);
        root.addChild(firstItem);
        LibraryManagerItem anotherItem = new LibraryManagerItemImpl(root, libraryData, anotherFolder, false);
        root.addChild(anotherItem);
        
        //add both children of roots as excluded folders
        Collection<File> collection = new ArrayList<File>();
        collection.add(secondFolder);
        collection.add(anotherExludedFolder);
        
        LibraryManagerModel model = new LibraryManagerModel(root, collection);
        
        assertEquals(2, model.getRootChildrenAsFiles().size());
        assertEquals(2, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        
        assertEquals(0, firstItem.getChildren().size());
        assertEquals(1, firstItem.getExcludedChildren().size());
        assertEquals(secondFolder, firstItem.getExcludedChildren().iterator().next());
        
        //unexclude the child
        model.restoreExcludedChild(new LibraryManagerItemImpl(firstItem, libraryData, secondFolder, true), firstItem);
        
        //only anotherExcludedChild should still exist in the list
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertTrue(model.getAllExcludedSubfolders().contains(anotherExludedFolder));
    }
    
    /**
     * Add firstFolder to root, its child is excluded.
     * Excluding the firstFolder will remove it from root since its a parent,
     * its child will also be removed from the exclusion list.
     */
    public void testExcludeRootChild() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        final File firstFolder = new File(baseDir, "firstFolder");
        final File secondFolder = new File(firstFolder, "secondFolder");
        
        firstFolder.mkdir();
        secondFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryExcluded(secondFolder);
            will(returnValue(true));
        }});
        
        //add two roots
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, firstFolder, false);
        LibraryManagerItem secondItem = new LibraryManagerItemImpl(firstItem, libraryData, secondFolder, true);
        firstItem.addChild(secondItem);
        root.addChild(firstItem);
        
        //add both children of roots as excluded folders
        Collection<File> collection = new ArrayList<File>();
        collection.add(secondFolder);
        
        LibraryManagerModel model = new LibraryManagerModel(root, collection);
        
        //firstFolder is a child of the root
        assertEquals(1, model.getRootChildrenAsFiles().size());
        assertTrue(model.getRootChildrenAsFiles().contains(firstItem.getFile()));
        //firstFolder's child is in the exclusion list
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertTrue(model.getAllExcludedSubfolders().contains(secondFolder));
        
        // remove firstFolder
        model.excludeChild(firstItem);
        
        // firstFolder should be removed from root and its children from exclusion list
        assertEquals(0, model.getRootChildrenAsFiles().size());
        assertEquals(0, model.getAllExcludedSubfolders().size());
    }
    
    /**
     * root -> firstFolder -> secondFolder -> thirdFolder -> fourthFolder
     * Step 1) exclude secondFolder (will remove all its children also)
     * Step 2) unexclude thirdFolder (only second Folder is excluded)
     * Step 3) exclude fourthFolder
     */
    public void testFourSubFolders() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        final File firstFolder = new File(baseDir, "firstFolder");
        final File secondFolder = new File(firstFolder, "secondFolder");
        final File thirdFolder = new File(secondFolder, "thirdFolder");
        final File fourthFolder = new File(thirdFolder, "fourthFolder");

        firstFolder.mkdir();
        secondFolder.mkdir();
        thirdFolder.mkdir();
        fourthFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(thirdFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(fourthFolder);
            will(returnValue(true));
        }});
        
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, firstFolder, true);

        LibraryManagerModel model = new LibraryManagerModel(root, new ArrayList<File>());

        model.addChild(firstItem, root);

        
        assertEquals(1, model.getRootChildrenAsFiles().size());
        assertEquals(0, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        
        //exclude first folder's child, second folder
        LibraryManagerItem secondItem = firstItem.getChildFor(secondFolder);
        model.excludeChild(secondItem);
        
        assertEquals(1, model.getRootChildrenAsFiles().size());
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        assertContains(model.getAllExcludedSubfolders(), secondItem.getFile());
        
        //readd second folder's child, third folder
        LibraryManagerItem thirdItem = secondItem.getChildFor(thirdFolder);
        model.addChild(thirdItem, root);
        
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertContains(model.getAllExcludedSubfolders(), secondItem.getFile());
        assertEquals(2, model.getRootChildrenAsFiles().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        assertContains(model.getRootChildrenAsFiles(), thirdFolder);

        //exclude third folder's child, fourth folder
        LibraryManagerItem fourthItem = thirdItem.getChildFor(fourthFolder);
        model.excludeChild(fourthItem);

        assertEquals(2, model.getAllExcludedSubfolders().size());
        assertContains(model.getAllExcludedSubfolders(), secondItem.getFile());
        assertContains(model.getAllExcludedSubfolders(), fourthItem.getFile());
    }
    
    private void setupMockery() {
        context = new Mockery();
        libraryData = context.mock(LibraryData.class);
    }
}
