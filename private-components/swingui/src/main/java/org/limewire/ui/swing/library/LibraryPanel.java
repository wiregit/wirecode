package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.library.sharing.LibrarySharingAction;
import org.limewire.ui.swing.library.sharing.LibrarySharingPanel;
import org.limewire.ui.swing.library.sharing.PublicSharedFeedbackPanel;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.LibraryImageTable;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LocalFileItemFilterator;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryPanel extends JPanel {

    @Resource
    private Icon plusIcon;
    
    private static final String TABLE = "TABLE";
    private static final String LIST = "LIST";
    
    private final HeaderBar headerBar = new HeaderBar();
    private final LibraryTable libraryTable;
    private final LibraryNavigatorPanel libraryNavigatorPanel;
    private final LibrarySharingPanel librarySharingPanel;
    private final PublicSharedFeedbackPanel publicSharedFeedbackPanel;
    private final ButtonDecorator buttonDecorator;
    private final LocalFileListTransferHandler transferHandler;
    private final Provider<LibraryImageTable> libraryImagePanelProvider;
    private LibraryImageTable libraryImagePanel;
//    private final GhostDragGlassPane ghostGlassPane;
    
    private JPanel tableListPanel;
    private JScrollPane libraryScrollPane;
    private CardLayout tableListLayout;
    
    private JXButton addFilesButton;
    private LibraryTableComboBox libraryTableComboBox;
    
    private Category selectedCategory;
    private EventList<LocalFileItem> eventList;
    private FilterList<LocalFileItem> filteredList;
    private FilterList<LocalFileItem> textFilterList;
    
    @Inject
    public LibraryPanel(LibraryNavigatorPanel navPanel, HeaderBarDecorator headerBarDecorator, LibraryTable libraryTable,
            LibrarySharingPanel sharingPanel, LibraryTableComboBox libraryTableComobBox, 
            PublicSharedFeedbackPanel publicSharedFeedbackPanel, PlayerPanel playerPanel, AddFileAction addFileAction,
            ButtonDecorator buttonDecorator, LibraryTransferHandler transferHandler,
            Provider<LibraryImageTable> libraryImagePanelProvider, ComboBoxDecorator comboBoxDecorator,
            GhostDragGlassPane ghostGlassPane, LibrarySharingAction libraryAction) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        this.libraryNavigatorPanel = navPanel;
        this.libraryTable = libraryTable;
        this.librarySharingPanel = sharingPanel;
        this.libraryTableComboBox = libraryTableComobBox;
        this.publicSharedFeedbackPanel = publicSharedFeedbackPanel;
        this.buttonDecorator = buttonDecorator;
        this.transferHandler = transferHandler;
        this.libraryImagePanelProvider = libraryImagePanelProvider;
//        this.ghostGlassPane = ghostGlassPane;
        
        GuiUtils.assignResources(this);
        
        layoutComponents(headerBarDecorator, playerPanel, addFileAction, libraryAction);

        setEventListOnTable(new BasicEventList<LocalFileItem>());
        
        comboBoxDecorator.decorateDarkFullComboBox(libraryTableComobBox);
    }
    
    private void layoutComponents(HeaderBarDecorator headerBarDecorator, PlayerPanel playerPanel, AddFileAction addFileAction, LibrarySharingAction libraryAction) {
        headerBarDecorator.decorateBasic(headerBar);
        
        headerBar.setLayout(new MigLayout("insets 0, gap 0, fill"));
        headerBar.setDefaultComponentHeight(-1);
        createAddFilesButton(addFileAction, libraryAction);
        headerBar.add(addFilesButton, "push");
        headerBar.add(playerPanel, "pos 0.5al 0.5al");
        libraryTableComboBox.setPreferredSize(new Dimension(getPreferredSize().width, 24));
        libraryTableComboBox.setMaximumSize(new Dimension(160, 24));
        headerBar.add(libraryTableComboBox, "alignx right, gapright 5");
        
        tableListLayout = new CardLayout();
        tableListPanel = new JPanel(tableListLayout);
        
        libraryTable.setTransferHandler(transferHandler);
        libraryScrollPane = new JScrollPane(libraryTable);
        libraryScrollPane.setBorder(BorderFactory.createEmptyBorder());  

        tableListPanel.add(libraryScrollPane, TABLE);
        
        setupStoreHighlighter();
        
        add(libraryNavigatorPanel, "dock west, growy");
        add(headerBar, "dock north, growx");
        add(publicSharedFeedbackPanel.getComponent(), "dock north, growx, hidemode 3");
        add(librarySharingPanel.getComponent(), "dock west, growy, hidemode 3");
        add(tableListPanel, "grow");
        
//        try {
//            libraryTable.getDropTarget().addDropTargetListener(new GhostDropTargetListener(libraryTable, ghostGlassPane));
//        } catch (TooManyListenersException e) {
//        }
//        
//        //hides system drag cursor when drag starts from within the app
//        DragSource source = DragSource.getDefaultDragSource();
//        source.addDragSourceListener(new DragSourceAdapter(){
//            @Override
//            public void dragEnter(java.awt.dnd.DragSourceDragEvent e) { 
//                DragSourceContext context = e.getDragSourceContext();
//                context.setCursor(Cursor.getDefaultCursor());
//            }
//        });
    }
    
    /**
     * Fills in the top right corner if a scrollbar appears with an empty table
     * header.
     */
    protected void configureEnclosingScrollPane(JScrollPane scrollPane) {
        JTableHeader th = new JTableHeader();
        th.setDefaultRenderer(new TableCellHeaderRenderer());
        // Put a dummy header in the upper-right corner.
        final Component renderer = th.getDefaultRenderer().getTableCellRendererComponent(null, "", false, false, -1, -1);
        JPanel cornerComponent = new JPanel(new BorderLayout());
        cornerComponent.add(renderer, BorderLayout.CENTER);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerComponent);
    }
    
    @Inject
    void register(LibraryManager libraryManager) {        
        //Loads the Library after Component has been realized.
        final LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                eventList = libraryList.getSwingModel();
                selectTable(libraryTableComboBox.getSelectedTableFormat(), libraryTableComboBox.getSelectedCategory());
                configureEnclosingScrollPane(libraryScrollPane);
            }
        });
        
        // listen for selection changes in the combo box and filter the table
        // replacing the table header as you do.
        libraryTableComboBox.addSelectionListener(new SelectionListener(){
            @Override
            public void selectionChanged(Action item) {
                selectTable(libraryTableComboBox.getSelectedTableFormat(), libraryTableComboBox.getSelectedCategory());
            }
        });
        libraryNavigatorPanel.addTableSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();

                setPublicSharedComponentVisible(navItem);
                eventList = navItem.getLocalFileList().getSwingModel();
                selectSharing(navItem);
                selectTable(libraryTableComboBox.getSelectedTableFormat(), libraryTableComboBox.getSelectedCategory());
            }
        });
    }
    
    /**
     * Returns a Rectangle contains the location and size of the table within
     * this container.
     */
    public Rectangle getTableListRect() {
        Point location = tableListPanel.getLocation();
        Dimension size = tableListPanel.getSize();
        return new Rectangle(location.x, location.y, size.width, size.height);
    }
    
    public void selectLocalFileList(LocalFileList localFileList) {
        libraryNavigatorPanel.selectLocalFileList(localFileList);
    }
    
    public void selectAndScrollTo(File file) {
        libraryTable.selectAndScrollTo(file);
    }
    
    public void selectAndScrollTo(URN urn) {
        libraryTable.selectAndScrollTo(urn);
    }
    
    private void selectSharing(LibraryNavItem navItem) {
        if(navItem != null && navItem.getLocalFileList() instanceof SharedFileList) {
            librarySharingPanel.setSharedFileList((SharedFileList)navItem.getLocalFileList());
        }
        librarySharingPanel.getComponent().setVisible(navItem != null && navItem.getType() == NavType.LIST);
    }
    
    private void createImageList() {
        libraryImagePanel = libraryImagePanelProvider.get();
        libraryImagePanel.setTransferHandler(transferHandler);
//        try {
//            libraryImagePanel.getDropTarget().addDropTargetListener(new GhostDropTargetListener(libraryImagePanel, ghostGlassPane));
//        } catch (TooManyListenersException e) {
//        }
        tableListPanel.add(libraryImagePanel, LIST); 
    }
    
    List<File> getSelectedFiles() {
        List<LocalFileItem> selected;
        if(selectedCategory == Category.IMAGE) {
            selected = libraryImagePanel.getSelection();
        } else {
            selected = libraryTable.getSelection();
        }
        
        List<File> files = new ArrayList<File>(selected.size());
        for(LocalFileItem item : selected) {
            files.add(item.getFile());
        }
        return files;
    }
    
    public List<LocalFileItem> getSelectedItems() {
        List<LocalFileItem> selected;
        if(selectedCategory == Category.IMAGE) {
            selected = libraryImagePanel.getSelection();
        } else {
            selected = libraryTable.getSelection();
        }
        return selected;
    }
    
    private void selectTable(AbstractLibraryFormat<LocalFileItem> libraryTableFormat, Category category) {       
        selectedCategory = category;

        if(category != Category.IMAGE) {
            tableListLayout.show(tableListPanel, TABLE);
            setEventListOnTable(eventList);
            libraryTable.setupCellRenderers(category, libraryTableFormat);
            libraryTable.applySavedColumnSettings();
            
            // hide the remove button for Library Tables
            TableColumnExt column = libraryTable.getColumnExt(libraryTableFormat.getColumnName(libraryTableFormat.getActionColumn()));
            if(column != null) {
                column.setVisible(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY);
            }
        } else {
            if(libraryImagePanel == null) {
                createImageList();
            }
            tableListLayout.show(tableListPanel, LIST);
            setEventListOnImages(eventList);
            // hide remove button for library
            libraryImagePanel.setShowButtons(libraryNavigatorPanel.getSelectedNavItem().getType() != NavType.LIBRARY);
        }
    }
    
    private void createAddFilesButton(AddFileAction addFileAction, LibrarySharingAction libraryAction) {
        addFilesButton = new JXButton(addFileAction);
        addFilesButton.setIcon(plusIcon);
        addFilesButton.setRolloverIcon(plusIcon);
        addFilesButton.setPressedIcon(plusIcon);
        addFilesButton.setPreferredSize(new Dimension(getPreferredSize().width, 23));
        addFilesButton.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        addFilesButton.addActionListener(libraryAction);
        buttonDecorator.decorateDarkFullImageButton(addFilesButton, AccentType.SHADOW);
    }
    
    private EventList<LocalFileItem> recreateFilterList(EventList<LocalFileItem> eventList) {
        if(filteredList != null) {
            filteredList.dispose();
            filteredList = null;
        }
        if(textFilterList != null) {
            textFilterList.dispose();
        }
        if(selectedCategory != null) {
            final Category category = selectedCategory;
            filteredList = GlazedListsFactory.filterList(eventList, new Matcher<LocalFileItem>() {
                @Override
                public boolean matches(LocalFileItem item) {
                    return item.getCategory().equals(category);
                }
            });
        }
        MatcherEditor<LocalFileItem> textMatcherEditor = new TextComponentMatcherEditor<LocalFileItem>(libraryTableComboBox.getFilterField(), new LocalFileItemFilterator(selectedCategory) );
        textFilterList = GlazedListsFactory.filterList(filteredList == null ? eventList : filteredList, textMatcherEditor);
        return textFilterList;
    }
    
    private void setEventListOnTable(EventList<LocalFileItem> eventList) {
        libraryTable.setEventList(recreateFilterList(eventList), libraryTableComboBox.getSelectedTableFormat());
    }
    
    private void setEventListOnImages(EventList<LocalFileItem> eventList) {
        libraryImagePanel.setEventList(recreateFilterList(eventList));
    }
    
    private void setPublicSharedComponentVisible(LibraryNavItem navItem) {
        publicSharedFeedbackPanel.getComponent().setVisible(navItem != null && navItem.getType() == NavType.PUBLIC_SHARED);
    }
    
    private void setupStoreHighlighter() {
        TableColors tableColors = new TableColors();
        ColorHighlighter highlighter = new ColorHighlighter(new StoreHighlightPredicate(), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        libraryTable.addHighlighter(highlighter);
    }
    
    private class StoreHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();
            if(navItem == null || navItem.getType() != NavType.LIST ||
                    (navItem.getType() == NavType.LIST && ((SharedFileList)navItem.getLocalFileList()).getFriendIds().size() == 0))
                return false;
            
            LocalFileItem item = libraryTable.getLibraryTableModel().getElementAt(adapter.row);
            return !item.isShareable();
        }
    }
    
    /**
	 * Clears any active filters on the library.
	 */
    public void clearFilters() {
        libraryTableComboBox.clearFilters();
    }
}
