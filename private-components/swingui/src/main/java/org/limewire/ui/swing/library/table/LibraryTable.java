package org.limewire.ui.swing.library.table;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.ColumnStateHandler;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.IconLabelRendererFactory;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableColumnSelector;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.EventListJXTableSorting;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryTable extends MouseableTable {

    private final int rowHeight = 20;

    private LibraryTableModel cachedLibraryTableModel;
    private DefaultEventSelectionModel<LocalFileItem> cachedEventSelectionModel;
    private EventListJXTableSorting cachedTableSorting;
    private SortedList<LocalFileItem> cachedSortedList;
    
    private AbstractLibraryFormat<LocalFileItem> fileItemFormat;
    private ColumnStateHandler columnStateHandler;
    private MousePopupListener mousePopupListener;
    
    private Provider<DefaultLibraryRenderer> defaultCellRenderer;
    private final Provider<TimeRenderer> timeRenderer;
    private final Provider<FileSizeRenderer> fileSizeRenderer;
    private final Provider<QualityRenderer> qualityRenderer;
    private final Provider<NameRenderer> nameRenderer;
    private final Provider<RemoveRenderer> removeRenderer;
    private final Provider<IsPlayingRenderer> isPlayingRenderer;
    private final IconLabelRenderer iconLabelRenderer;
    private final RemoveEditor removeEditor;
    
    @Inject
    public LibraryTable(Provider<DefaultLibraryRenderer> defaultCellRenderer,
            Provider<TimeRenderer> timeRenderer,
            Provider<FileSizeRenderer> fileSizeRenderer,
            Provider<QualityRenderer> qualityRenderer,
            Provider<NameRenderer> nameRenderer,
            Provider<LibraryPopupMenu> libraryPopupMenu,
            Provider<RemoveRenderer> removeRenderer,
            Provider<IsPlayingRenderer> isPlayingRenderer,
            IconLabelRendererFactory iconLabelRendererFactory,
            RemoveEditor removeEditor) {
        this.defaultCellRenderer = defaultCellRenderer;
        this.timeRenderer = timeRenderer;
        this.fileSizeRenderer = fileSizeRenderer;
        this.qualityRenderer = qualityRenderer;
        this.nameRenderer = nameRenderer;
        this.removeRenderer = removeRenderer;
        this.isPlayingRenderer = isPlayingRenderer;
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
        setDropMode(DropMode.ON);
        
        setDoubleClickHandler(new DoubleClickHandler());
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
        return cachedLibraryTableModel;
    }
    
    public LocalFileItem getSelectedItem() {
        if(getSelectedRow() >= 0)
            return cachedLibraryTableModel.getElementAt(getSelectedRow());
        else           
            return null;
    }
    
    public void setEventList(EventList<LocalFileItem> eventList, AbstractLibraryFormat<LocalFileItem> tableFormat) {
        uninstallListeners();
        
        fileItemFormat = tableFormat;
        
        SortedList<LocalFileItem> newSortedList = GlazedListsFactory.sortedList(eventList, null);
        LibraryTableModel newLibraryTableModel = new LibraryTableModel(newSortedList, tableFormat);
        DefaultEventSelectionModel<LocalFileItem> newEventSelectionModel = new DefaultEventSelectionModel<LocalFileItem>(newSortedList);
        
        if(cachedTableSorting != null) {
            cachedTableSorting.uninstall();
        }
        
        setModel(newLibraryTableModel);
        setSelectionModel(newEventSelectionModel);
        newEventSelectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        
        if(cachedLibraryTableModel != null) {
            cachedEventSelectionModel.dispose();
            cachedLibraryTableModel.dispose();
            cachedSortedList.dispose();
        }
        
        EventListJXTableSorting newTableSorting = EventListJXTableSorting.install(this, newSortedList, tableFormat);
        
        cachedSortedList = newSortedList;
        cachedLibraryTableModel = newLibraryTableModel;
        cachedEventSelectionModel = newEventSelectionModel;
        cachedTableSorting = newTableSorting;
        
        installListeners();
    }
    
    public boolean isRowDisabled(int row) {
        FileItem item = getLibraryTableModel().getFileItem(convertRowIndexToModel(row));
        if (item instanceof LocalFileItem) {
            return ((LocalFileItem) item).isIncomplete();
        }
        return item == null;
    }
    
    public void selectAndScrollTo(File file) {
        LibraryTableModel model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount(); y++) {
            FileItem fileItem = model.getElementAt(y);
            if(!(fileItem instanceof LocalFileItem)) {
                break; // Never going to find it.
            }
            if(file.equals(((LocalFileItem)fileItem).getFile())) {
                getSelectionModel().setSelectionInterval(y, y);
                break;
            }
        }
        ensureRowVisible(getSelectedRow());
    }
    
    public void selectAndScrollTo(URN urn) {
        LibraryTableModel model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount(); y++) {
            FileItem fileItem = model.getElementAt(y);
            if(urn.equals(fileItem.getUrn())) {
                getSelectionModel().setSelectionInterval(y, y);
                break;
            }
        }
        ensureRowVisible(getSelectedRow());
    }
        
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
            columnStateHandler.setupColumnVisibility(false);
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
                setCellRenderer(AudioTableFormat.PLAY_INDEX, isPlayingRenderer.get());
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

    /** Returns all currently selected LocalFileItems. */
    public List<LocalFileItem> getSelection() {
        return cachedEventSelectionModel.getSelected();
    }
    
    /**
     * Handles double clicking a row in the library table.
     */
    private class DoubleClickHandler implements TableDoubleClickHandler{
        @Override
        public void handleDoubleClick(int row) {
            if(getSelectedItem() == null) 
                getSelectionModel().setSelectionInterval(row, row);
            File file = getSelectedItem().getFile();
            PlayerUtils.playOrLaunch(file);
        }
    }
}
