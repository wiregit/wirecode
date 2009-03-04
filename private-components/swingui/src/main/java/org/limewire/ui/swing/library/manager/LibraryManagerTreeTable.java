package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.TreeTableModel;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.table.MouseableTreeTable;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.FileUtils;

public class LibraryManagerTreeTable extends MouseableTreeTable {
    
    private final LibraryData libraryData;
    private final ExcludedFolderCollectionManager excludedFolders;

    public LibraryManagerTreeTable(IconManager iconManager, LibraryData libraryData, ExcludedFolderCollectionManager excludedFolders) {
        setTableHeader(null); // No table header for this table.
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionBackground(getBackground());
        setEditable(true);
        setHorizontalScrollEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);
        setTreeCellRenderer(new FolderRenderer(iconManager));
        setLeafIcon(null);
        setClosedIcon(null);
        setOpenIcon(null);
        
        this.libraryData = libraryData;
        this.excludedFolders = excludedFolders;
    }
    
    public LibraryManagerModel getLibraryModel() {
        return (LibraryManagerModel)super.getTreeTableModel();
    }
    
    @Override
    public void setTreeTableModel(TreeTableModel model) {
        super.setTreeTableModel(model);
        
        RemoveButtonRenderer renderer = new RemoveButtonRenderer();
        RemoveButtonEditor editor = new RemoveButtonEditor(this);
        TableColumn removeColumn = getColumn(LibraryManagerModel.REMOVE_INDEX);        
        removeColumn.setCellEditor(editor);
        removeColumn.setCellRenderer(renderer);
        removeColumn.setMaxWidth(renderer.getPreferredSize().width);
        removeColumn.setMinWidth(renderer.getPreferredSize().width);
    }

    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return getColumnModel().getColumn(col).getCellEditor() != null;
    }


    /** Adds a new directory into this model. */
    public void addDirectory(File directory) {
        directory = FileUtils.canonicalize(directory);
        
        boolean expand = false;
        LibraryManagerItem root = getLibraryModel().getRoot();
        LibraryManagerItem parent = findParent(root, directory);
        LibraryManagerItem item = null;        
        
        // If no parent, it's going to be added to root.
        if(parent == null) {
            parent = root;
        }
        
        item = parent.getChildFor(directory);
        
        if(item == null) {
            item = new LibraryManagerItemImpl(parent, libraryData, excludedFolders, directory);
            getLibraryModel().addChild(item, parent);
            getLibraryModel().getAllExcludedSubfolders().remove(item.getFile());
            getLibraryModel().unexcludeAllSubfolders(item.getFile());
            
            removeDuplicateRoots(root, directory);
        } else {
            removeDuplicateRoots(root, directory);
            
            // If the item already exists, go through all its excluded children and explicitly add them.
            // work off a copy because we'll be modifying the list as we iterate through it.
            for(File excludedChild : getExcludedDescendents(item.getFile())) {
                expand = true;
                LibraryManagerItem excludedParent = findParent(item, excludedChild);
                getLibraryModel().addChild(new LibraryManagerItemImpl(excludedParent, libraryData, excludedFolders, excludedChild), excludedParent);
            }
            getLibraryModel().unexcludeAllSubfolders(item.getFile());
        }
        
        TreePath path = new TreePath(getLibraryModel().getPathToRoot(item));
        scrollPathToVisible(path);
        if(expand) {
            expandPath(path);
        }
        int row = getRowForPath(path);
        getSelectionModel().setSelectionInterval(row, row);
    }
    
    /**
     * Returns the excluded children of this file
     */
    private List<File> getExcludedDescendents(File parent) {
        List<File> excludedChildren = new ArrayList<File>();
        for(File file : getLibraryModel().getAllExcludedSubfolders()) {
            if(FileUtils.isAncestor(parent, file))
                excludedChildren.add(file);
        }
        return excludedChildren;
    }
    
    /**
     * Check all the top level roots to see if they are descendant of this directory.
     * If they are, remove them and their excluded children.
     */
    private void removeDuplicateRoots(LibraryManagerItem root, File directory) {
        // Make sure that we're not the ancestor of any existing item in the list
        // If we are, we remove that item.
        List<LibraryManagerItem> toRemove = new ArrayList<LibraryManagerItem>();
        for(LibraryManagerItem child : root.getChildren()) {
            if(FileUtils.isAncestor(directory, child.getFile()) && !directory.equals(child.getFile())) {
                toRemove.add(child);
            }
        }
        // updating the model that we just iterated over
        for(LibraryManagerItem child : toRemove) {
            getLibraryModel().removeChild(child);
            getLibraryModel().unexcludeAllSubfolders(child.getFile());
        }
    }
    
    private LibraryManagerItem findParent(LibraryManagerItem start, File directory) {
        if(start.getFile() != null && start.getFile().equals(directory.getParentFile())) {
            return start;
        }
        
        for(LibraryManagerItem child : start.getChildren()) {
            if(FileUtils.isAncestor(child.getFile(), directory)) {
                LibraryManagerItem found = findParent(child, directory);
                // Don't return immediately -- it's possible there could
                // be two ancestors listed if a parent directory is excluded
                // and this is explicitly added on its own.
                if(found != null) {
                    return found;
                }
            }
        }
        return null;
    }    
}
