package org.limewire.ui.swing.library.table;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.popup.LibraryPopupHandler;
import org.limewire.ui.swing.library.popup.LibraryPopupMenu;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.ColumnStateHandler;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.IconLabelRendererFactory;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableColumnSelector;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryTable extends MouseableTable {

    private final int rowHeight = 20;

    private LibraryTableModel libraryTableModel; 
    private AbstractLibraryFormat<LocalFileItem> fileItemFormat;
    private ColumnStateHandler columnStateHandler;
    private MousePopupListener mousePopupListener;
    
    private Provider<DefaultLibraryRenderer> defaultCellRenderer;
    private final Provider<TimeRenderer> timeRenderer;
    private final Provider<FileSizeRenderer> fileSizeRenderer;
    private final Provider<CalendarRenderer> calendarRenderer;
    private final Provider<QualityRenderer> qualityRenderer;
    private final Provider<NameRenderer> nameRenderer;
    private final Provider<IconManager> iconManager;
    private final Provider<RemoveRenderer> removeRenderer;
    private final IconLabelRenderer iconLabelRenderer;
    private final RemoveEditor removeEditor;
    
    @Inject
    public LibraryTable(Provider<DefaultLibraryRenderer> defaultCellRenderer,
            Provider<TimeRenderer> timeRenderer,
            Provider<FileSizeRenderer> fileSizeRenderer,
            Provider<CalendarRenderer> calendarRenderer,
            Provider<QualityRenderer> qualityRenderer,
            Provider<NameRenderer> nameRenderer,
            Provider<IconManager> iconManager,
            Provider<LibraryPopupMenu> libraryPopupMenu,
            Provider<RemoveRenderer> removeRenderer,
            IconLabelRendererFactory iconLabelRendererFactory,
            RemoveEditor removeEditor) {
        this.defaultCellRenderer = defaultCellRenderer;
        this.timeRenderer = timeRenderer;
        this.fileSizeRenderer = fileSizeRenderer;
        this.calendarRenderer = calendarRenderer;
        this.qualityRenderer = qualityRenderer;
        this.nameRenderer = nameRenderer;
        this.removeRenderer = removeRenderer;
        this.iconManager = iconManager;
        this.iconLabelRenderer = iconLabelRendererFactory.createIconRenderer(false);
        this.removeEditor = removeEditor;
        
        initTable();
        
        
        //TODO: anything below here should be initialized outside of the constructor
        mousePopupListener = new MousePopupListener() {
            @Override
            public void handlePopupMouseEvent(MouseEvent e) {
                showHeaderPopupMenu(e.getPoint());
            }
        }; 

        setPopupHandler(new LibraryPopupHandler(this, libraryPopupMenu));
    }
    
    private void initTable() {
        setStripesPainted(true);
        setShowHorizontalLines(false);
        setShowGrid(false, true);
        
        setFillsViewportHeight(true);
        setDragEnabled(true);
        setRowHeight(rowHeight);
    }
    
    private void uninstallListeners() {
        getTableHeader().removeMouseListener(mousePopupListener);
        
        if(columnStateHandler != null) {
            columnStateHandler.removeListeners();
        }
    }
    
    private void installListeners() {
        // Add mouse listener to display popup menu on column header.  We use 
        // MousePopupListener to detect the popup trigger, which differs on 
        // Windows, Mac, and Linux.
        JTableHeader header = getTableHeader();
        header.addMouseListener(mousePopupListener);
        // Install column state handler.
        columnStateHandler = new ColumnStateHandler(this, fileItemFormat);
    }
    
    public void showHeaderPopupMenu(Point p) {
        JPopupMenu menu = new TableColumnSelector(this, fileItemFormat).getPopupMenu();
        menu.show(getTableHeader(), p.x, p.y);
    }
    
    public LibraryTableModel getLibraryTableModel() {
        return libraryTableModel;
    }
    
    public void setEventList(EventList<LocalFileItem> eventList, AbstractLibraryFormat<LocalFileItem> tableFormat) {
        uninstallListeners();
        
        this.fileItemFormat = tableFormat;
        
        //TODO: try replacing tableformat rather replacing entire tablemodel
        // will need to create a filterator that we select a category on
        libraryTableModel = new LibraryTableModel(eventList, tableFormat);
        setModel(libraryTableModel);
        
        installListeners();
    }
    
    public void setTableFormat(AbstractLibraryFormat<LocalFileItem> tableFormat) {
     
        uninstallListeners();
        
        this.fileItemFormat = tableFormat;
        libraryTableModel.setTableFormat(tableFormat);
        
        installListeners();
    }
    
    public boolean isRowDisabled(int row) {
        FileItem item = getLibraryTableModel().getFileItem(convertRowIndexToModel(row));
        if (item instanceof LocalFileItem) {
            return ((LocalFileItem) item).isIncomplete();
        }
        return item == null;
    }
    
//    private static class DisabledHighlightPredicate implements HighlightPredicate {
//        private LibraryTab table;
//        public DisabledHighlightPredicate (LibraryTab table) {
//            this.table = table;
//        }
//        @Override
//        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {           
//            return table.isRowDisabled(adapter.row);
//        }
//    }
    
    /**
     * Loads the saved state of the columns. 
     *
     * <p>NOTE: This method must be called after the renderers and editors
     * have been loaded.  The settings must be applied in this order:
     * width/visibility/order.</p>
     */
    public void applySavedColumnSettings(){
        if (columnStateHandler != null) {
            columnStateHandler.setupColumnWidths();
            columnStateHandler.setupColumnVisibility();
            columnStateHandler.setupColumnOrder();
        }
    }
    
    public void setupCellRenderers(Category category, AbstractLibraryFormat format) {
        for (int i = 0; i < format.getColumnCount(); i++) {
            Class clazz = format.getColumnClass(i);
            if (clazz == String.class) {
                setCellRenderer(i, defaultCellRenderer.get());
            } 
        }   
        
        if(category != null) {
            switch(category) {
            case AUDIO:
                //TODO: setup play column renderer/editor
                setCellRenderer(AudioTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(AudioTableFormat.LENGTH_INDEX, timeRenderer.get());
                setCellRenderer(AudioTableFormat.QUALITY_INDEX, qualityRenderer.get());
                setCellRenderer(AudioTableFormat.TITLE_INDEX, nameRenderer.get());
                setCellRenderer(AudioTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(AudioTableFormat.ACTION_INDEX, removeEditor);
                break;
            case VIDEO:
                setCellRenderer(VideoTableFormat.LENGTH_INDEX, timeRenderer.get());
                setCellRenderer(VideoTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(VideoTableFormat.NAME_INDEX, nameRenderer.get());
                setCellRenderer(VideoTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(VideoTableFormat.ACTION_INDEX, removeEditor);
                break;
            case IMAGE:
//                setCellRenderer(ImageTableFormat.NAME_INDEX, nameRenderer.get());
                setCellRenderer(ImageTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(ImageTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(ImageTableFormat.ACTION_INDEX, removeEditor);
                break;
            case DOCUMENT:
                setCellRenderer(DocumentTableFormat.NAME_INDEX, iconLabelRenderer);
                setCellRenderer(DocumentTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(DocumentTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(DocumentTableFormat.ACTION_INDEX, removeEditor);
                break;
            case PROGRAM:
                setCellRenderer(ProgramTableFormat.NAME_INDEX, iconLabelRenderer);
                setCellRenderer(ProgramTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(ProgramTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(ProgramTableFormat.ACTION_INDEX, removeEditor);
                break;
            case OTHER:
                setCellRenderer(OtherTableFormat.NAME_INDEX, iconLabelRenderer);
                setCellRenderer(OtherTableFormat.SIZE_INDEX, fileSizeRenderer.get());
                setCellRenderer(OtherTableFormat.ACTION_INDEX, removeRenderer.get());
                setCellEditor(OtherTableFormat.ACTION_INDEX, removeEditor);
                break;
            default:
                throw new IllegalArgumentException("Unknown category:" + category);
            }
        } else {
            setCellRenderer(AllTableFormat.NAME_INDEX, iconLabelRenderer);
            setCellRenderer(AllTableFormat.SIZE_INDEX, fileSizeRenderer.get());
            setCellRenderer(AllTableFormat.ACTION_INDEX, removeRenderer.get());
            setCellEditor(AllTableFormat.ACTION_INDEX, removeEditor);
        }
    }
    
    private void setCellRenderer(int column, TableCellRenderer renderer) {
        getColumnModel().getColumn(column).setCellRenderer(renderer);
    }
    
    private void setCellEditor(int column, TableCellEditor editor) {
        getColumnModel().getColumn(column).setCellEditor(editor);
    }
}
