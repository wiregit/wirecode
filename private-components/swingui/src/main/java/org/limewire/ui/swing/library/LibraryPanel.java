package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.LibraryTable;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;

import com.google.inject.Inject;

public class LibraryPanel extends JPanel {

    private final HeaderBar headerBar = new HeaderBar();
    private final LibraryTable libraryTable;
    private final LibraryNavigatorPanel navigatorComponent;
    private final JComponent sharingComponent;
    private final SharedFileListManager sharedFileListManager;
    private final LibraryManager libraryManager;
    
    private JXButton addFilesButton;
    private LibraryTableSelectionComboBox tableSelectionComboBox;
    
    private LibraryCategoryMatcher categoryMatcher;
    private EventList<LocalFileItem> eventList;
    private SortedList<LocalFileItem> sortedList;
    private FilterList<LocalFileItem> filteredList;
    
//    private final PluggableList<LocalFileItem> baseLibraryList;
//    private final LibraryListSourceChanger currentFriendFilterChanger;
    
    @Inject
    public LibraryPanel(LibraryNavigatorPanel navPanel, HeaderBarDecorator headerBarDecorator, LibraryTable libraryTable,
            LibrarySharingPanel sharingPanel, LibraryTableSelectionComboBox selectionComobBox, LibraryManager libraryManager,
            SharedFileListManager sharedFileListManager) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        this.navigatorComponent = navPanel;
        this.libraryTable = libraryTable;
        this.sharingComponent = sharingPanel;
        this.tableSelectionComboBox = selectionComobBox;
        this.sharedFileListManager = sharedFileListManager;
        this.libraryManager = libraryManager;
        
        categoryMatcher = new LibraryCategoryMatcher();
        
        eventList = libraryManager.getLibraryManagedList().getSwingModel();
        //TODO: SharedFileLists use a different lock so we can't create a pluggable list with them
//        baseLibraryList = new PluggableList<LocalFileItem>(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
//        currentFriendFilterChanger = new LibraryListSourceChanger(baseLibraryList, libraryManager, sharedFileListManager);

        
        sortedList = GlazedListsFactory.sortedList(eventList);
        filteredList = GlazedListsFactory.filterList(sortedList, categoryMatcher);
        
        layoutComponents(headerBarDecorator);
        
        libraryTable.setEventList(sortedList, tableSelectionComboBox.getSelectedTabelFormat());
        
    }
    
    private void layoutComponents(HeaderBarDecorator headerBarDecorator) {
        headerBarDecorator.decorateBasic(headerBar);
        
        headerBar.setLayout(new MigLayout("insets 0, gap 0"));
        createAddFilesButton();
        headerBar.add(addFilesButton, "push");
        
        headerBar.add(tableSelectionComboBox, "alignx right, gapright 5");
        
        JScrollPane libraryScrollPane = new JScrollPane(libraryTable);
        libraryScrollPane.setBorder(BorderFactory.createEmptyBorder());  
        
        add(navigatorComponent, "dock west, growy");
        add(headerBar, "dock north, growx");
        add(sharingComponent, "dock west, growy, hidemode 3");
        add(libraryScrollPane, "grow");
        
    }
    
    public void registerListeners() {
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
                //TODO: this locks should be in a different thread
                SharedFileList fileList = sharedFileListManager.getSharedFileList(navigatorComponent.getSelectedNavItem().getTabID());
                if(fileList != null) {
//                    setEventList(fileList.getSwingModel());
                    eventList = fileList.getSwingModel();
                    selectTable(tableSelectionComboBox.getSelectedTabelFormat(), tableSelectionComboBox.getSelectedCategory());
                } else {
//                    setEventList(libraryManager.getLibraryManagedList().getSwingModel());
//                currentFriendFilterChanger.setCurrentList(navigatorComponent.getSelectedNavItem().getTabID());
                    eventList = libraryManager.getLibraryManagedList().getSwingModel();
                    selectTable(tableSelectionComboBox.getSelectedTabelFormat(), tableSelectionComboBox.getSelectedCategory());
                }
            }
        });
    }
    
    private void selectTable(AbstractLibraryFormat<LocalFileItem> libraryTableFormat, Category category) {
        //TODO: get appropriate filtered list
        // Create sorted list and set table model.
       
        categoryMatcher.setCategoryFilter(category);
        setEventList(eventList);
//        libraryTable.setTableFormat(libraryTableFormat);
        libraryTable.setupCellRenderers(category, libraryTableFormat);
        
        libraryTable.applySavedColumnSettings();
    }
    
    private void createAddFilesButton() {
        addFilesButton = new JXButton("Add Files");
    }
    
    private void setEventList(EventList<LocalFileItem> eventList) {
        sortedList = GlazedListsFactory.sortedList(eventList);
        filteredList = GlazedListsFactory.filterList(sortedList, categoryMatcher);
        libraryTable.setEventList(filteredList, tableSelectionComboBox.getSelectedTabelFormat());
    }

}
