package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.limewire.util.FileUtils;

public class LibraryManagerModel extends AbstractTreeTableModel {

    public static final int REMOVE_INDEX = 0;
    public static final int FOLDER_INDEX = 1;
    
    private static final int COLUMN_COUNT = 2;
    
    private Collection<File> excludedSubfolders;
    
    public LibraryManagerModel(RootLibraryManagerItem item, Collection<File> excludedSubfolders) {
        super(item);
        this.excludedSubfolders = excludedSubfolders;
    }
    
    @Override
    public int getHierarchicalColumn() {
        return FOLDER_INDEX;
    }
    
    /**
     * Gets the path from the root to the specified node.
     * 
     * @param aNode
     *            the node to query
     * @return an array of {@code LibraryManagerItem}s, where
     *         {@code arr[0].equals(getRoot())} and
     *         {@code arr[arr.length - 1].equals(aNode)}, or an empty array if
     *         the node is not found.
     * @throws NullPointerException
     *             if {@code aNode} is {@code null}
     */
    public LibraryManagerItem[] getPathToRoot(LibraryManagerItem item) {
        List<LibraryManagerItem> path = new ArrayList<LibraryManagerItem>();
        LibraryManagerItem node = item;
        while (node != root) {
            path.add(0, node);
            node = node.getParent();
        }
        if (node == root) {
            path.add(0, node);
        }
        return path.toArray(new LibraryManagerItem[0]);
    }
    
    public void addChild(LibraryManagerItem child, LibraryManagerItem parent) {
        int idx = parent.addChild(child);
        modelSupport.fireChildAdded(new TreePath(getPathToRoot(parent)), idx, child);
    }
    
    public void removeChild(LibraryManagerItem item) {
        LibraryManagerItem parent = item.getParent();
        if (parent == null) {
            throw new IllegalStateException("node does not have a parent.");
        }
        
        int index = parent.removeChild(item);
        modelSupport.fireChildRemoved(new TreePath(getPathToRoot(parent)), index, item);
    }
    
    public void excludeChild(LibraryManagerItem item) {
        if (getRootChildrenAsFiles().contains(item.getFile())) {
            //top level managed folders should not be added to exclude list
            unexcludeAllSubfolders(item.getFile());
        } else {
            //subfolders of managed folders should be excluded
            excludeFolder(item.getFile());
        }
        removeChild(item);
    }
    
    public void unexcludeAllSubfolders(File parent) {
        List<File> removeList = new ArrayList<File>();
        
        for (File child : excludedSubfolders) {
            if (FileUtils.isAncestor(parent, child)) {
                removeList.add(child);
            }
        }

        excludedSubfolders.removeAll(removeList);
    }
    
    /**Adds parent to list of excluded subfolders.  Removes all subfolders of parent 
     * with no intermediate managed root or excluded*/
    private void excludeFolder(File parent) {
        List<File> unExcludeList = new ArrayList<File>();
        for (File child : excludedSubfolders) {
            if (shouldRemoveFromExcludeList(child, parent)) {
                unExcludeList.add(child);
            }
        }

        excludedSubfolders.add(parent);
        excludedSubfolders.removeAll(unExcludeList);
    }
       
    /**
     * @return true if child is a subfolder of ancestor and there is no intermediate managed root or excluded folder.  
     */
    private boolean shouldRemoveFromExcludeList(File child, File ancestor) {
        Collection<File> roots = getRootChildrenAsFiles();        
        
        child = child.getParentFile();        
        
        while (child != null) {
            if (child.equals(ancestor)) {
                return true;
            } else if (excludedSubfolders.contains(child)) {
                return false;// there is an intermediate excluded folder
            } else if (roots.contains(child)) {
                return false; // there is an intermediate managed folder
            }

            child = child.getParentFile();
        }
        
        return false;
    }
    
    @Override
    public RootLibraryManagerItem getRoot() {
        return (RootLibraryManagerItem)super.getRoot();
    }
    
    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if(node instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem) node;
            switch (column) {
                case REMOVE_INDEX: return item;
                case FOLDER_INDEX: return item;
            }
        }
        return null;
    }
    
    public String getColumnName(int column) {
        switch(column) {
            case REMOVE_INDEX: return null;
            case FOLDER_INDEX: return null;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public LibraryManagerItem getChild(Object parent, int index) {
        if(parent instanceof LibraryManagerItem) {
            List<LibraryManagerItem> items = ((LibraryManagerItem)parent).getChildren();
            if(index < 0 || index >= items.size())
                return null;
            return items.get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if(parent instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem) parent;
            return item.getChildren().size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if(parent instanceof LibraryManagerItem && child instanceof LibraryManagerItem) {
            LibraryManagerItem childItem = (LibraryManagerItem) child;
            LibraryManagerItem parentItem = (LibraryManagerItem) parent;
            
            for(int i = 0; i < parentItem.getChildren().size(); i++) {
                LibraryManagerItem item = parentItem.getChildren().get(i);
                if(item.equals(childItem))
                    return i;
            }
        }
        
        return -1;
    }
    
    public Collection<File> getRootChildrenAsFiles() {
        Collection<File> manageRecursively = new HashSet<File>();
        List<LibraryManagerItem> children = getRoot().getChildren();
        for(LibraryManagerItem child : children) {
            manageRecursively.add(child.getFile());
        }
        return manageRecursively;
    }
    
    public Collection<File> getAllExcludedSubfolders() {
        return excludedSubfolders;
    }
  

    public void setRootChildren(List<LibraryManagerItem> children) {
        List<LibraryManagerItem> oldChildren = new ArrayList<LibraryManagerItem>(getRoot().getChildren());
        for(LibraryManagerItem child : oldChildren) {
            removeChild(child);
        }
        for(LibraryManagerItem child : children) {
            addChild(child, getRoot());
        }
        
    }

    /**Adds child to the tree model and removes it from excluded subfolders.*/
    public void restoreExcludedChild(LibraryManagerItemImpl libraryManagerItemImpl,
            LibraryManagerItem item) {
        addChild(libraryManagerItemImpl, item);
        excludedSubfolders.remove(item.getFile());
        
    }    
}
