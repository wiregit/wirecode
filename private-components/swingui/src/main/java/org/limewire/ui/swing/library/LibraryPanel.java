package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
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
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.library.LibraryFilterPanel.LibraryCategoryListener;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.library.sharing.LibrarySharingAction;
import org.limewire.ui.swing.library.sharing.LibrarySharingPanel;
import org.limewire.ui.swing.library.table.AbstractLibraryFormat;
import org.limewire.ui.swing.library.table.LibraryImageTable;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.player.PlayerMediatorListener;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryPanel extends JPanel {

    @Resource private Icon plusIcon;
    @Resource private Font fileCountFont;
    @Resource private Color fileCountColor;
    
    private static final String TABLE = "TABLE";
    private static final String LIST = "LIST";
    
    private final HeaderBar headerBar = new HeaderBar();
    private final LibraryTable libraryTable;
    private final LibraryNavigatorPanel libraryNavigatorPanel;
    private final LibrarySharingPanel librarySharingPanel;
    private final PublicSharedFeedbackPanel publicSharedFeedbackPanel;
    private final LibraryFilterPanel libraryFilterPanel;
    private final PlayerPanel playerPanel;
    private final ButtonDecorator buttonDecorator;
    private final LocalFileListTransferHandler transferHandler;
    private final Provider<LibraryImageTable> libraryImagePanelProvider;
    private LibraryImageTable libraryImagePanel;
    
    private JPanel tableListPanel;
    private JScrollPane libraryScrollPane;
    private CardLayout tableListLayout;
    
    private JXButton addFilesButton;
    private JLabel fileCountLabel;
    private JXButton filterToggleButton;
    
    private Category selectedCategory;
    private EventList<LocalFileItem> eventList;
    private FilterList<LocalFileItem> filteredList;
    private FilterList<LocalFileItem> textFilterList;
    private FileCountListener fileCountListener;
    
    private LibraryNavItem selectedNavItem;
    
    @Inject
    public LibraryPanel(LibraryNavigatorPanel navPanel, HeaderBarDecorator headerBarDecorator, LibraryTable libraryTable,
            LibrarySharingPanel sharingPanel, LibraryFilterPanel libraryFilterPanel,
            PublicSharedFeedbackPanel publicSharedFeedbackPanel, PlayerPanel playerPanel, AddFileAction addFileAction,
            ButtonDecorator buttonDecorator, LibraryTransferHandler transferHandler,
            Provider<LibraryImageTable> libraryImagePanelProvider,
            LibrarySharingAction libraryAction) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        this.libraryNavigatorPanel = navPanel;
        this.libraryTable = libraryTable;
        this.librarySharingPanel = sharingPanel;
        this.libraryFilterPanel = libraryFilterPanel;
        this.publicSharedFeedbackPanel = publicSharedFeedbackPanel;
        this.playerPanel = playerPanel;
        this.buttonDecorator = buttonDecorator;
        this.transferHandler = transferHandler;
        this.libraryImagePanelProvider = libraryImagePanelProvider;
        
        GuiUtils.assignResources(this);
        
        layoutComponents(headerBarDecorator, playerPanel, addFileAction, libraryAction);

        setEventListOnTable(new BasicEventList<LocalFileItem>());
    }
    
    private void layoutComponents(HeaderBarDecorator headerBarDecorator, PlayerPanel playerPanel, AddFileAction addFileAction, LibrarySharingAction libraryAction) {
        headerBarDecorator.decorateBasic(headerBar);
        
        headerBar.setLayout(new MigLayout("insets 0, gap 0, fill"));
        headerBar.setDefaultComponentHeight(-1);
        createAddFilesButton(addFileAction, libraryAction);
        headerBar.add(addFilesButton, "push");
        headerBar.add(playerPanel, "pos 0.5al 0.5al");
        fileCountLabel = new JLabel();
        fileCountLabel.setForeground(fileCountColor);
        fileCountLabel.setFont(fileCountFont);
        headerBar.add(fileCountLabel, "aligny 50%, gapright 10");
        createFilterToggleButton();
        headerBar.add(filterToggleButton, "alignx right, gapright 5");
        
        tableListLayout = new CardLayout();
        tableListPanel = new JPanel(tableListLayout);
        
        libraryTable.setTransferHandler(transferHandler);
        libraryScrollPane = new JScrollPane(libraryTable);
        libraryScrollPane.setBorder(BorderFactory.createEmptyBorder());  

        tableListPanel.add(libraryScrollPane, TABLE);
        
        setupHighlighters();
        
        add(libraryNavigatorPanel, "dock west, growy");
        add(headerBar, "dock north, growx");
        add(libraryFilterPanel.getComponent(), "dock north, growx, hidemode 3");
        add(librarySharingPanel.getComponent(), "dock west, growy, hidemode 3");
        add(tableListPanel, "grow");
        add(publicSharedFeedbackPanel.getComponent(), "dock south, growx, hidemode 3");    
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
    void register(LibraryManager libraryManager, PlayerMediator playerMediator) {        
        //Loads the Library after Component has been realized.
        final LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                selectedNavItem = libraryNavigatorPanel.getSelectedNavItem();
                eventList = libraryList.getSwingModel();
                selectTable(libraryFilterPanel.getSelectedTableFormat(), libraryFilterPanel.getSelectedCategory());
                configureEnclosingScrollPane(libraryScrollPane);
            }
        });
        
        // listen for selection changes in the combo box and filter the table
        // replacing the table header as you do.
        libraryFilterPanel.addSearchTabListener(new LibraryCategoryListener(){

            @Override
            public void categorySelected(Category category) {
                // If selected navItem is playlist and old category playable 
                // and new category not playable, then save playlist.
                PlayerMediator playerMediator = playerPanel.getPlayerMediator();
                if (playerMediator.isActivePlaylist(selectedNavItem) &&
                        isPlayable(selectedCategory) &&
                        !isPlayable(libraryFilterPanel.getSelectedCategory())) {
                    playerMediator.setPlaylist(libraryTable.getPlayableList());
                }
                
                selectTable(libraryFilterPanel.getSelectedTableFormat(), libraryFilterPanel.getSelectedCategory());
            }
        });
        libraryNavigatorPanel.addTableSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e) {
                LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();
                
                // If previous navItem was playlist and new navItem not playlist 
                // and category playable, then save playlist.
                PlayerMediator playerMediator = playerPanel.getPlayerMediator();
                if (playerMediator.isActivePlaylist(selectedNavItem) &&
                        !playerMediator.isActivePlaylist(navItem) &&
                        isPlayable(selectedCategory)) {
                    playerMediator.setPlaylist(libraryTable.getPlayableList());
                }
                selectedNavItem = navItem;

                clearFilters();
                setPublicSharedComponentVisible(navItem);
                eventList = navItem.getLocalFileList().getSwingModel();
                selectSharing(navItem);
                selectTable(libraryFilterPanel.getSelectedTableFormat(), libraryFilterPanel.getSelectedCategory());
            }
        });
        
        // Add player listener to repaint table when song changes or player stops.
        playerMediator.addMediatorListener(new PlayerMediatorListener(){
            @Override
            public void progressUpdated(float progress) {
            }

            @Override
            public void songChanged(String name) {
                if(libraryTable.isVisible() && (libraryFilterPanel.getSelectedCategory() == Category.AUDIO || 
                        libraryFilterPanel.getSelectedCategory() == null)) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            libraryTable.repaint();     
                        }
                    });
                }
            }

            @Override
            public void stateChanged(PlayerState state) {
                if ((state == PlayerState.STOPPED) && libraryTable.isVisible() &&
                        isPlayable(libraryFilterPanel.getSelectedCategory())) {
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            libraryTable.repaint();
                        }
                    });
                }
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
        if(file != null)
            libraryTable.selectAndScrollTo(file);
    }
    
    public void selectAndScrollTo(URN urn) {
        if(urn != null)
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
        tableListPanel.add(libraryImagePanel, LIST); 
    }
    
    /**
     * Returns true if the specified category is playable.
     */
    public boolean isPlayable(Category category) {
        return (category == null) || (category == Category.AUDIO);
    }
    
    /**
     * Returns the current list of playable file items.
     */
    public EventList<LocalFileItem> getPlayableList() {
        return libraryTable.getPlayableList();
    }
    
    /**
     * Returns the selected display category.
     */
    public Category getSelectedCategory() {
        return selectedCategory;
    }
    
    /**
     * Returns the selected library item.
     */
    public LibraryNavItem getSelectedNavItem() {
        return selectedNavItem;
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
    
    
    private void createFilterToggleButton() {
        filterToggleButton = new JXButton(I18n.tr("Find"));
        filterToggleButton.setSelected(SwingUiSettings.SHOW_LIBRARY_FILTERS.getValue());
        filterToggleButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                filterToggleButton.setSelected(!filterToggleButton.isSelected());
                libraryFilterPanel.getComponent().setVisible(filterToggleButton.isSelected());
                libraryFilterPanel.clearFilters();
                SwingUiSettings.SHOW_LIBRARY_FILTERS.setValue(filterToggleButton.isSelected());
            }
        });
        buttonDecorator.decorateDarkFullImageButton(filterToggleButton, AccentType.SHADOW);
    }
    
    private EventList<LocalFileItem> recreateFilterList(final EventList<LocalFileItem> eventList) {
        if(filteredList != null) {
            filteredList.dispose();
            filteredList = null;
        }
        if(textFilterList != null) {
            textFilterList.removeListEventListener(fileCountListener);
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
        MatcherEditor<LocalFileItem> textMatcherEditor = new TextComponentMatcherEditor<LocalFileItem>(libraryFilterPanel.getFilterField(), new LocalFileItemFilterator(selectedCategory) );
        textFilterList = GlazedListsFactory.filterList(filteredList == null ? eventList : filteredList, textMatcherEditor);
        setFileCount(textFilterList.size(), filteredList == null ? eventList.size() : filteredList.size());
        if(fileCountListener == null)
            fileCountListener = new FileCountListener();
        textFilterList.addListEventListener(fileCountListener);
        
        return textFilterList;
    }
    
    private void setEventListOnTable(EventList<LocalFileItem> eventList) {
        libraryTable.setEventList(recreateFilterList(eventList), libraryFilterPanel.getSelectedTableFormat(), isPlayable(selectedCategory));
    }
    
    private void setEventListOnImages(EventList<LocalFileItem> eventList) {
        libraryImagePanel.setEventList(recreateFilterList(eventList));
    }
    
    private void setPublicSharedComponentVisible(LibraryNavItem navItem) {
        publicSharedFeedbackPanel.getComponent().setVisible(navItem != null && navItem.getType() == NavType.PUBLIC_SHARED);
    }
    
    private void setupHighlighters() {
        TableColors tableColors = new TableColors();
        ColorHighlighter storeHighlighter = new ColorHighlighter(new GrayHighlightPredicate(), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        
        libraryTable.addHighlighter(storeHighlighter);
    }
    
    private void setFileCount(int filterSize, int totalCount) {
        if(filterSize < 0) {
            fileCountLabel.setText(I18n.tr("{0} files", totalCount));
        } else { 
            fileCountLabel.setText(I18n.tr("{0} of {1} files", filterSize, totalCount));
        }
    }
    
    private class GrayHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();
            
            LocalFileItem item = libraryTable.getLibraryTableModel().getElementAt(adapter.row);
            if( navItem.getType() == NavType.PUBLIC_SHARED || (navItem.getType() == NavType.LIST && ((SharedFileList)navItem.getLocalFileList()).getFriendIds().size() > 0))
                return !item.isShareable(); 

            return !item.isLoaded();
        }
    }
    
    /**
	 * Clears any active filters on the library.
	 */
    public void clearFilters() {
        libraryFilterPanel.clearFilters();
    }

    /**
     * Selects the specified SharedFileList in the library nav and starts editing on its name.
     * @param sharedFileList can not be the public shared list
     */
    public void editSharedListName(SharedFileList sharedFileList) {
        libraryNavigatorPanel.editSharedListName(sharedFileList);     
    }
    
    /**
     * Listens for changes on the current list and updates the 
     * file count label.
     */
    private class FileCountListener implements ListEventListener<LocalFileItem> {
        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            setFileCount(textFilterList.size(), filteredList == null ? eventList.size() : filteredList.size());
        }
    }
}
