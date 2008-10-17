package org.limewire.ui.swing.library.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class LibraryTable<T extends FileItem> extends MouseableTable {
    
    private final LibraryTableFormat<T> format;
    private final TableColors tableColors;
    private final EventList<T> listSelection;
    
    @Resource
    private Color disabledForegroundColor;
    
    private ShareTableRendererEditor shareEditor;
    private LibrarySharePanel librarySharePanel;
    
    public LibraryTable(EventList<T> libraryItems, LibraryTableFormat<T> format) {
        super(new LibraryTableModel<T>(libraryItems, format));
        
        GuiUtils.assignResources(this);
        
        this.format = format;

        tableColors = new TableColors();
        setStripesPainted(true);
        setColumnControlVisible(true);
        setShowHorizontalLines(false);
        
        EventSelectionModel<T> model = new EventSelectionModel<T>(libraryItems);
        setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.listSelection = model.getSelected();
        setHighlighters(tableColors.getEvenHighLighter(), tableColors.getOddHighLighter(), 
                new ColorHighlighter(new DisabledHighlightPredicate(this), null, disabledForegroundColor, null, disabledForegroundColor));
       
        setFillsViewportHeight(true);
        setDragEnabled(true);
    }
    
    public void enableSharing(LibrarySharePanel librarySharePanel) {
        this.librarySharePanel = librarySharePanel;
        shareEditor = new ShareTableRendererEditor(new ShareAction(I18n.tr("Share")));
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(shareEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(new ShareTableRendererEditor(null));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(shareEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(shareEditor.getPreferredSize().width);
        setRowHeight(shareEditor.getPreferredSize().height);
        hideColumns();
    }
    
    public void enableDownloading(DownloadListManager downloadListManager){
        ArrayList<RemoteFileItem> downloadingList = new ArrayList<RemoteFileItem>();
        
        DownloadAction downloadAction = new DownloadAction(I18n.tr("download"), downloadListManager, this, downloadingList);
        
        setDoubleClickHandler(new LibraryDownloadDoubleClickHandler(downloadAction));
        
        LibraryDownloadRendererEditor downloadEditor = new LibraryDownloadRendererEditor(downloadAction, downloadingList);
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(downloadEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(new LibraryDownloadRendererEditor(null, downloadingList));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(downloadEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(downloadEditor.getPreferredSize().width);
        setRowHeight(downloadEditor.getPreferredSize().height);
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
            librarySharePanel.setFileItem((LocalFileItem) ((LibraryTableModel)getModel()).getElementAt(convertRowIndexToModel(getEditingRow())));
            librarySharePanel.show(shareEditor, getVisibleRect());
            shareEditor.cancelCellEditing();
        }
        
    }
    
    
    private static class LibraryDownloadDoubleClickHandler implements TableDoubleClickHandler {

        private DownloadAction action;

        public LibraryDownloadDoubleClickHandler(DownloadAction action) {
            this.action = action;
        }

        @Override
        public void handleDoubleClick(int row) {
            action.download(row);
        }

    }

    private static class DownloadAction extends AbstractAction {
        private DownloadListManager downloadListManager;
        private LibraryTable table;
        private ArrayList<RemoteFileItem> downloadingList;
        public DownloadAction(String text, DownloadListManager downloadListManager, LibraryTable table, ArrayList<RemoteFileItem> downloadingList){
            super(text);
            this.downloadingList = downloadingList;
            this.downloadListManager = downloadListManager;
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            download(table.convertRowIndexToModel(table.getEditingRow()));
        }
        
        public void download(int row) {
            RemoteFileItem file = (RemoteFileItem) ((LibraryTableModel) table.getModel()).getElementAt(row);
            try {
                downloadListManager.addDownload(file);
                downloadingList.add(file);
                TableCellEditor editor = table.getCellEditor();
                if (editor != null) {          
                   editor.cancelCellEditing();
                }
            } catch (SaveLocationException e) {
                throw new RuntimeException(e);
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
}
