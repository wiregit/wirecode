package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.LibrarySelectable;
import org.limewire.ui.swing.library.Sharable;
import org.limewire.ui.swing.library.sharing.FileShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColumnSelector;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class LibraryTable<T extends FileItem> extends MouseableTable implements Sharable, Disposable, LibrarySelectable {
    
    private final LibraryTableFormat<T> format;
    private final TableColors tableColors;
    private final EventList<T> listSelection;
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    
    private ShareTableRendererEditor shareEditor;
    private LibrarySharePanel librarySharePanel;
    
    private TableCellRenderer defaultRenderer;
    
    protected final int rowHeight = 20;
    
    public LibraryTable(EventList<T> libraryItems, LibraryTableFormat<T> format, SaveLocationExceptionHandler saveLocationExceptionHandler) {
        super(new LibraryTableModel<T>(libraryItems, format));

        this.format = format;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        defaultRenderer = new DefaultLibraryRenderer();
        
        tableColors = new TableColors();
        setStripesPainted(true);
        setShowHorizontalLines(false);
        
        EventSelectionModel<T> model = new EventSelectionModel<T>(libraryItems);
        setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.listSelection = model.getSelected();
        setHighlighters(tableColors.getEvenHighLighter(), tableColors.getOddHighLighter(), 
                new ColorHighlighter(new DisabledHighlightPredicate(this), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
       
        setFillsViewportHeight(true);
        setDragEnabled(true);
        setDefaultRenderer(Object.class, defaultRenderer);
        setRowHeight(rowHeight);
        
        final JTableHeader header = getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)){
                    showHeaderPopupMenu(e.getPoint());
                }
            }
        });       

    }
    
    public void showHeaderPopupMenu(Point p) {
        JPopupMenu menu = createColumnMenu();
        menu.show(getTableHeader(), p.x, p.y);
    }
    
    public JPopupMenu createColumnMenu() {
        return new TableColumnSelector(this, format).getPopupMenu();
    }
    
    public void enableMyLibrarySharing(LibrarySharePanel librarySharePanel) {
        this.librarySharePanel = librarySharePanel;
        shareEditor = new ShareTableRendererEditor(new ShareAction(I18n.tr("Share")));
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(shareEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(new ShareTableRendererEditor(null));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(shareEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(shareEditor.getPreferredSize().width);
        setRowHeight(rowHeight);
        hideColumns();
        
        //this isn't necessary if we aren't showing "share" on mouse over
//        removeMouseMotionListener(mouseOverEditorListener);
//        addMouseMotionListener(new MouseMotionAdapter(){
//            @Override
//            public void mouseMoved(MouseEvent e) {
//                int editRow = rowAtPoint(e.getPoint());
//                editCellAt(editRow, convertColumnIndexToView(format.getActionColumn()));
//            }
//            
//        });
    }
    
    public void enableDownloading(DownloadListManager downloadListManager, LibraryFileList fileList){
        LibraryDownloadAction downloadAction = new LibraryDownloadAction(I18n.tr("download"), downloadListManager, this);
        
        setDoubleClickHandler(new LibraryDownloadDoubleClickHandler(downloadAction));
        
        LibraryDownloadRendererEditor downloadEditor = new LibraryDownloadRendererEditor(downloadAction, downloadListManager, fileList);
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(downloadEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(new LibraryDownloadRendererEditor(null, downloadListManager, fileList));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(downloadEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(downloadEditor.getPreferredSize().width);
        setRowHeight(rowHeight);
        hideColumns();        
    }
    
    public void dispose() {
        ((EventSelectionModel)getSelectionModel()).dispose();
    }
    
    @SuppressWarnings("unchecked")
    public LibraryTableModel<T> getLibraryTableModel(){
        return (LibraryTableModel<T>)getModel();
    }
    
    /** Returns a copy of all selected items. */
    public List<T> getSelectedItems() {
        return new ArrayList<T>(listSelection);
    }
    
    @Override
    public void setModel(TableModel newModel) {
        assert getModel() == null : "cannot change model!";
        super.setModel(newModel);
    }
    
    //TODO: this is quite brittle.  only works if column indexes are in descending order - need to do it a different way
    protected void hideColumns(){
        int[] hiddenColumns = format.getDefaultHiddenColums();
        for (int i = 0; i < hiddenColumns.length; i++) {
            ((TableColumnExt) getColumnModel().getColumn(hiddenColumns[i])).setVisible(false);
        }
    }
    
    private class ShareAction extends AbstractAction {
        
        public ShareAction(String text){
            super(text);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((FileShareModel)librarySharePanel.getShareModel()).setFileItem((LocalFileItem) ((LibraryTableModel)getModel()).getElementAt(convertRowIndexToModel(getEditingRow())));
            librarySharePanel.show(shareEditor);
            shareEditor.cancelCellEditing();
        }
        
    }
    
    private class LibraryDownloadDoubleClickHandler implements TableDoubleClickHandler {
        private LibraryDownloadAction action;

        public LibraryDownloadDoubleClickHandler(LibraryDownloadAction action) {
            this.action = action;
        }

        @Override
        public void handleDoubleClick(int row) {
            action.download(row);
        }
    }

    private class LibraryDownloadAction extends AbstractAction {
        private DownloadListManager downloadListManager;
        private LibraryTable table;
        
        public LibraryDownloadAction(String text, DownloadListManager downloadListManager, LibraryTable table){
            super(text);
            this.downloadListManager = downloadListManager;
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            download(table.convertRowIndexToModel(table.getEditingRow()));
        }
        
        public void download(int row) {
            final RemoteFileItem file = (RemoteFileItem) ((LibraryTableModel) table.getModel()).getElementAt(row);
            try {
                downloadListManager.addDownload(file);
                TableCellEditor editor = table.getCellEditor();
                if (editor != null) {          
                   editor.cancelCellEditing();
                }
            } catch (SaveLocationException e) {
                saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws SaveLocationException {
                        downloadListManager.addDownload(file, saveFile, overwrite);
                    }
                }, e, true);
            }
        }
    }
    
    /**
     * Ensures the selected row is visible.
     */
    public void ensureSelectionVisible() {
        ensureRowVisible(getSelectedRow());
    }
    
    public boolean isCellEditable(int row, int column) {
        return super.isCellEditable(row, column) && !isRowDisabled(row);
    }
    
    /**
     * Ensures the given row is visible.
     */
    public void ensureRowVisible(int row) {
        if(row != -1) {
            Rectangle cellRect = getCellRect(row, 0, false);
            Rectangle visibleRect = getVisibleRect();
            if( !visibleRect.intersects(cellRect) )
                scrollRectToVisible(cellRect);
        }
    }
    
    public boolean isRowDisabled(int row) {
        FileItem item = getLibraryTableModel().getFileItem(convertRowIndexToModel(row));
        if (item instanceof LocalFileItem) {
            return ((LocalFileItem) item).isIncomplete();
        }
        return item == null;
    }
    
    private static class DisabledHighlightPredicate implements HighlightPredicate {
        private LibraryTable table;
        public DisabledHighlightPredicate (LibraryTable table) {
            this.table = table;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {           
            return table.isRowDisabled(adapter.row);
        }
    }

    @Override
    public void selectAndScroll(Object selectedObject) {
        LibraryTableModel<T> model = getLibraryTableModel();
      for(int y=0; y < model.getRowCount(); y++) {
          FileItem localFileItem = model.getElementAt(y);
          if(selectedObject.equals(localFileItem.getUrn())) {
              getSelectionModel().setSelectionInterval(y, y);
              break;
          }
      }
      ensureSelectionVisible();
    }
}
