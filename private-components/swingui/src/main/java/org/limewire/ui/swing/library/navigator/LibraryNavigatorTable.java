package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.DropMode;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.dnd.LibraryNavTransferHandler;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;

@LazySingleton
public class LibraryNavigatorTable extends JXTable {

    @Resource private Color backgroundColor;
    
    private TablePopupHandler popupHandler;
    private final EventList<LibraryNavItem> eventList;
    
    @Inject
    public LibraryNavigatorTable(LibraryNavTransferHandler libraryNavTransferHandler) {        
        GuiUtils.assignResources(this);
        
        initialize();
        
        eventList = new BasicEventList<LibraryNavItem>();
        SortedList<LibraryNavItem> sortedList = new SortedList<LibraryNavItem>(eventList, new LibraryNavItemComparator());
        
        setModel(new EventTableModel<LibraryNavItem>(sortedList, new NavTableFormat()));
        setDropMode(DropMode.ON);
        setTransferHandler(libraryNavTransferHandler);
        setEditable(false);
    }
    
    public void addLibraryNavItem(String name, String id, LocalFileList localFileList, NavType type) {
        eventList.add(new LibraryNavItem(name, id, localFileList, type));
    }
    
    public void removeLibraryNavItem(SharedFileList fileList) {
        eventList.getReadWriteLock().writeLock().lock();
        try {
            for(LibraryNavItem item : eventList) {
                if(item.getLocalFileList() == fileList) {
                    eventList.remove(item);
                }
            }
        } finally {
            eventList.getReadWriteLock().writeLock().unlock();
        }
    }
    
    public void selectLibraryNavItem(String id) {
        for(int i = 0; i < getModel().getRowCount(); i++) {
            Object value = getModel().getValueAt(i, 0);
            if(value instanceof LibraryNavItem) {
                if( ((LibraryNavItem) value).getTabID() == id) {
                    getSelectionModel().setSelectionInterval(i,i);
                    break;
                }
            }
        }
    }
    
    public LibraryNavItem getSelectedItem() {
        if(getSelectedRow() >= 0)
            return eventList.get(getSelectedRow());
        else if(getRowCount() > 0) {
            getSelectionModel().setSelectionInterval(0, 0);
            return eventList.get(0);
        } else            
            return null;
    }
    
    private void initialize() {
        setFillsViewportHeight(true);
        setBackground(backgroundColor);
        setShowGrid(false, false);
        setTableHeader(null);
        setRowHeight(24);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {//adding this to editor messes up popups
//
//                int col = columnAtPoint(e.getPoint());
//                int row = rowAtPoint(e.getPoint());
//
//                if (row >= 0 && col >= 0) {
//                    if (rowDoubleClickHandler != null || columnDoubleClickHandler != null) {
//                        Component component = e.getComponent();
//                        //launch file on double click unless the click is on a button
//                        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)
//                                && !(component.getComponentAt(e.getPoint()) instanceof JButton)) {
//                            if (rowDoubleClickHandler != null) {
//                                rowDoubleClickHandler.handleDoubleClick(row);
//                            }
//                            if (columnDoubleClickHandler != null) {
//                                columnDoubleClickHandler.handleDoubleClick(col);
//                            }
//                        }
//                    }                   
//                }
            }
            

            @Override
            public void mouseExited(MouseEvent e) {
//                maybeCancelEditing();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
//                int col = columnAtPoint(e.getPoint());
//                int row = rowAtPoint(e.getPoint());
//                if (isEditing() && isCellEditable(row, col)) { 
//                    TableCellEditor editor = getCellEditor(row, col);
//                    if (editor != null) {
//                        // force update editor colors
//                        prepareEditor(editor, row, col);
//                    }                        
//                }
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && popupHandler != null) {
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        popupHandler.maybeShowPopup(
                            e.getComponent(), e.getX(), e.getY());
                        TableCellEditor editor = getCellEditor();
                        if (editor != null) {
                            editor.cancelCellEditing();
                        }
                    }
                }
            }
        });
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(super.getPreferredScrollableViewportSize().width, getModel().getRowCount() * getRowHeight());
    }
    
    public void setPopupHandler(TablePopupHandler popupHandler) {
        this.popupHandler = popupHandler;
    }
    
    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {    
            Object value = editor.getCellEditorValue();
            if(value instanceof String) {
                handleRename((String)value);
            }
            removeEditor();
        }
        setEditable(false);
    }
    
    /**
     * Renames the selected LibraryNavItem if the new Name
     * is a valid String.
     */
    private void handleRename(String newName) {
        if(newName == null || newName.length() == 0)
            return;
        LibraryNavItem item = getSelectedItem();
        LocalFileList fileList = item.getLocalFileList();
        if(fileList instanceof SharedFileList) {
            SharedFileList sharedFileList = (SharedFileList) fileList;
            if(sharedFileList.isNameChangeAllowed() && !sharedFileList.getCollectionName().equals(newName)) {
                sharedFileList.setCollectionName(newName);
            }
        }
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (!isEditable() || row >= getRowCount() || col >= getColumnCount() || row < 2 || col < 0) {
            return false;
        }
        return true;
    }
    
    private class NavTableFormat implements TableFormat<LibraryNavItem> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getColumnValue(LibraryNavItem baseObject, int column) {
            return baseObject;
        }
    }
    
    /**
     * Technically shouldn't be necessary but ensures that Library and Public Shared
     * always appear first and second in the table. The other items appear as they are
     * loaded.
     */
    private class LibraryNavItemComparator implements Comparator<LibraryNavItem> {

        @Override
        public int compare(LibraryNavItem nav1, LibraryNavItem nav2) {
            NavType type1 = nav1.getType();
            NavType type2 = nav2.getType();
            if(type1 == NavType.LIBRARY)
                return -1;
            else if(type2 == NavType.LIBRARY)
                return 1;
            else if(type1 == NavType.PUBLIC_SHARED)
                return -1;
            else if(type2 == NavType.PUBLIC_SHARED)
                return 1;
            return 0;
        }
    }
}
