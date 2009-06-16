package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.library.actions.AddFileAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.library.sharing.LibrarySharingPanel;
import org.limewire.ui.swing.library.sharing.PublicSharedFeedbackPanel;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;

import com.google.inject.Inject;

@LazySingleton
public class LibraryPanel extends JPanel {

    private final HeaderBar headerBar = new HeaderBar();
    private final LibraryTable libraryTable;
    private final LibraryNavigatorPanel navigatorComponent;
    private final LibrarySharingPanel librarySharingPanel;
    private final PublicSharedFeedbackPanel publicSharedFeedbackPanel;
    private final LocalFileListTransferHandler transferHandler;
    private final ButtonDecorator buttonDecorator;
    
    private JXButton addFilesButton;
    private LibraryTableSelectionComboBox tableSelectionComboBox;
    
    private LibraryCategoryMatcher categoryMatcher;
    private EventList<LocalFileItem> eventList;
    private SortedList<LocalFileItem> sortedList;
    private FilterList<LocalFileItem> filteredList;
    
    @Inject
    public LibraryPanel(LibraryNavigatorPanel navPanel, HeaderBarDecorator headerBarDecorator, LibraryTable libraryTable,
            LibrarySharingPanel sharingPanel, LibraryTableSelectionComboBox selectionComobBox, 
            PublicSharedFeedbackPanel publicSharedFeedbackPanel, PlayerPanel playerPanel, AddFileAction addFileAction,
            ButtonDecorator buttonDecorator, LibraryCategoryMatcher categoryMatcher, LocalFileListTransferHandler transferHandler) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        this.navigatorComponent = navPanel;
        this.libraryTable = libraryTable;
        this.librarySharingPanel = sharingPanel;
        this.tableSelectionComboBox = selectionComobBox;
        this.publicSharedFeedbackPanel = publicSharedFeedbackPanel;
        this.buttonDecorator = buttonDecorator;
        this.categoryMatcher = categoryMatcher;
        this.transferHandler = transferHandler;
        
        layoutComponents(headerBarDecorator, playerPanel, addFileAction);

        setEventList(new BasicEventList<LocalFileItem>());
    }
    
    private void layoutComponents(HeaderBarDecorator headerBarDecorator, PlayerPanel playerPanel, AddFileAction addFileAction) {
        headerBarDecorator.decorateBasic(headerBar);
        
        headerBar.setLayout(new MigLayout("insets 0, gap 0, fill"));
        headerBar.setDefaultComponentHeight(-1);
        createAddFilesButton(addFileAction);
        headerBar.add(addFilesButton);
        headerBar.add(playerPanel, "grow, align 50%");
        headerBar.add(tableSelectionComboBox, "alignx right, gapright 5");
        
        libraryTable.setTransferHandler(transferHandler);
        JScrollPane libraryScrollPane = new JScrollPane(libraryTable);
        libraryScrollPane.setBorder(BorderFactory.createEmptyBorder());  
        configureEnclosingScrollPane(libraryScrollPane);
        
        setupStoreHighlighter();
        
        add(navigatorComponent, "dock west, growy");
        add(headerBar, "dock north, growx");
        add(publicSharedFeedbackPanel.getComponent(), "dock north, growx, hidemode 3");
        add(librarySharingPanel.getComponent(), "dock west, growy, hidemode 3");
        add(libraryScrollPane, "grow");
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
                selectTable(tableSelectionComboBox.getSelectedTabelFormat(), tableSelectionComboBox.getSelectedCategory());
            }
        });
        
        // listen for selection changes in the combo box and filter the table
        // replacing the table header as you do.
        tableSelectionComboBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTable(tableSelectionComboBox.getSelectedTabelFormat(), tableSelectionComboBox.getSelectedCategory());
            }
        });
        
        navigatorComponent.addTableSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                LibraryNavItem navItem = navigatorComponent.getSelectedNavItem();

                setPublicSharedComponentVisible(navItem);
                eventList = navItem.getLocalFileList().getSwingModel();
                selectSharing(navItem);
                selectTable(tableSelectionComboBox.getSelectedTabelFormat(), tableSelectionComboBox.getSelectedCategory());
            }
        });
    }
    
    private void selectSharing(LibraryNavItem navItem) {
        if(navItem != null && navItem.getLocalFileList() instanceof SharedFileList)
            librarySharingPanel.setSharedFileList((SharedFileList)navItem.getLocalFileList());
        librarySharingPanel.getComponent().setVisible(navItem != null && navItem.getType() == NavType.LIST);
    }
    
    private void selectTable(AbstractLibraryFormat<LocalFileItem> libraryTableFormat, Category category) {       
        categoryMatcher.setCategoryFilter(category);
        setEventList(eventList);
        libraryTable.setupCellRenderers(category, libraryTableFormat);
        libraryTable.applySavedColumnSettings();
    }
    
    private void createAddFilesButton(AddFileAction addFileAction) {
        addFilesButton = new JXButton(addFileAction);
        addFilesButton.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        buttonDecorator.decorateDarkFullButton(addFilesButton);
    }
    
    private void setEventList(EventList<LocalFileItem> eventList) {
        sortedList = GlazedListsFactory.sortedList(eventList);
        filteredList = GlazedListsFactory.filterList(sortedList, categoryMatcher);
        libraryTable.setEventList(filteredList, tableSelectionComboBox.getSelectedTabelFormat());
    }
    
    private void setPublicSharedComponentVisible(LibraryNavItem navItem) {
        publicSharedFeedbackPanel.getComponent().setVisible(navItem != null && navItem.getType() == NavType.PUBLIC_SHARED);
    }
    
    public void setupStoreHighlighter() {
        TableColors tableColors = new TableColors();
        ColorHighlighter highlighter = new ColorHighlighter(new StoreHighlightPredicate(), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        libraryTable.addHighlighter(highlighter);
    }
    
    private class StoreHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LibraryNavItem navItem = navigatorComponent.getSelectedNavItem();
            if(navItem == null || navItem.getType() != NavType.LIST ||
                    (navItem.getType() == NavType.LIST && ((SharedFileList)navItem.getLocalFileList()).getFriendIds().size() == 0))
                return false;
            
            LocalFileItem item = libraryTable.getLibraryTableModel().getElementAt(adapter.row);
            return !item.isShareable();
        }
    }
}
