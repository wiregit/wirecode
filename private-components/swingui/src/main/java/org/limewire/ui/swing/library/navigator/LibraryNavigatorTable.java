package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;

@LazySingleton
public class LibraryNavigatorTable extends JXTable {

    @Resource private Color backgroundColor;
    
    private TablePopupHandler popupHandler;
    private EventList<LibraryNavItem> eventList;
    
    
    @Inject
    public LibraryNavigatorTable() {        
        GuiUtils.assignResources(this);
        
        initialize();
        
        eventList = new BasicEventList<LibraryNavItem>();
        
        setModel(new EventTableModel<LibraryNavItem>(eventList, new NavTableFormat()));
    }
    
    public void addLibraryNavItem(String name, String id) {
        eventList.add(new LibraryNavItem(name, id));
    }
    
    public LibraryNavItem getSelectedItem() {
        return eventList.get(getSelectedRow());
    }
    
    private void initialize() {
        setFillsViewportHeight(true);
        setBackground(backgroundColor);
        setShowGrid(false, false);
        setTableHeader(null);
        setRowHeight(26);
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
    
    public void setPopupHandler(TablePopupHandler popupHandler) {
        this.popupHandler = popupHandler;
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
}
