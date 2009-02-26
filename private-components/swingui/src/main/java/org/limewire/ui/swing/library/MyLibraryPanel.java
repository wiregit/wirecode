package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.DelegateList;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.ShareAllComboBox;
import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class MyLibraryPanel extends LibraryPanel implements EventListener<FriendEvent> {
    
    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride")
    private Color selectionPanelBackgroundOverride = null;
    @Resource
    private Icon closeButton;
    @Resource
    private Icon closeHoverButton;
    
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final PlayerPanel playerPanel;
    private final LibraryManager libraryManager;
    private final LibraryNavigator libraryNavigator;
    private final Map<Category, LibraryOperable<LocalFileItem>> selectableMap;
    private final ShareWidgetFactory shareFactory;    
    
    /** Map of JXLayers and categories they exist in */
    private Map<Category, JXLayer> map = new HashMap<Category, JXLayer>();
    
    private Timer repaintTimer;
    private ListenerSupport<XMPPConnectionEvent> connectionListeners;
    private ListenerSupport<FriendEvent> knownFriendsListeners;

    /**
     * set of known friends helps keep correct share numbers
     */
    private final Set<String> knownFriends = new HashSet<String>();
    
    private SharingFilterComboBox sharingComboBox;
    
    private final DelegateListChanger delegateListChanger;
    
    private SharingMessagePanel messagePanel;
    private NotSharingPanel notSharingPanel;
    
    private ShareAllComboBox shareAllComboBox;
    private GhostDropTargetListener ghostDropTargetListener;

    @Inject
    public MyLibraryPanel(LibraryManager libraryManager,
                          LibraryNavigator libraryNavigator,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareWidgetFactory shareFactory,
                          HeaderBarDecorator headerBarDecorator,
                          PlayerPanel player, 
                          GhostDragGlassPane ghostPane,
                          ListenerSupport<XMPPConnectionEvent> connectionListeners,
                          ShareListManager shareListManager,
                          TextFieldDecorator textFieldDecorator,
                          ComboBoxDecorator comboDecorator,
                          ButtonDecorator buttonDecorator,
                          XMPPService xmppService,
                          FriendsSignInPanel friendSignInPanel) {
        
        super(headerBarDecorator, textFieldDecorator);
        
        GuiUtils.assignResources(this);
        
        this.libraryManager = libraryManager;
        this.libraryNavigator = libraryNavigator;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;    
        this.shareFactory = shareFactory;
        this.playerPanel = player;
        this.selectableMap = new EnumMap<Category, LibraryOperable<LocalFileItem>>(Category.class);
        this.connectionListeners = connectionListeners;
        
        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }
        DelegateList<LocalFileItem> baseLibraryList = new DelegateList<LocalFileItem>(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
        delegateListChanger = new DelegateListChanger(baseLibraryList, libraryManager, shareListManager);
        sharingComboBox = new SharingFilterComboBox(delegateListChanger, this, shareListManager);
        comboDecorator.decorateLinkComboBox(sharingComboBox);
        sharingComboBox.setText(I18n.tr("What I'm Sharing"));
        
        getSelectionPanel().add(sharingComboBox, "gaptop 5, gapbottom 5, alignx 50%, hidemode 3");
        
        sharingComboBox.addSelectionListener(new SelectionListener(){
            @Override
            public void selectionChanged(Action item) {
                messagePanel.setMessage(delegateListChanger.getCurrentFriend());
                messagePanel.setVisible(true);
                sharingComboBox.setVisible(false);
            }
        });
        
        createMyCategories(baseLibraryList);
        createMyPlaylists();
        selectFirstVisible();

        this.knownFriends.add(Friend.P2P_FRIEND_ID);
        getSelectionPanel().updateCollectionShares(knownFriends);
        
        shareAllComboBox = new ShareAllComboBox(xmppService, shareFactory, this, friendSignInPanel, shareListManager);
        comboDecorator.decorateDarkFullComboBox(shareAllComboBox);
        shareAllComboBox.setText(I18n.tr("Share"));
        
        addHeaderComponent(shareAllComboBox, "cell 0 0, alignx left");
        addHeaderComponent(playerPanel, "cell 0 0, grow");
        playerPanel.setMaximumSize(new Dimension(999,999));
        playerPanel.setPreferredSize(new Dimension(999,999));

        
        setTransferHandler(new MyLibraryTransferHandler(null, libraryManager.getLibraryManagedList(), shareListManager, delegateListChanger));
        ghostDropTargetListener = new GhostDropTargetListener(this,ghostPane, delegateListChanger);
        try {
            getDropTarget().addDropTargetListener(ghostDropTargetListener);
        } catch (TooManyListenersException ignoreException) {            
        }      
        
        shareListManager.getCombinedShareList().getModel().addListEventListener(new ListEventListener<LocalFileItem>(){
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                //coalesces repaint calls. Updates usually come in bulk, ie you sign on/off,
                // share a collection, etc..
                if(repaintTimer.isRunning())
                    repaintTimer.restart();
                else
                    repaintTimer.start();
            }
        });
        
        repaintTimer = new Timer(250, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                MyLibraryPanel.this.repaint();
            }
        });
        repaintTimer.setRepeats(false);
        
        addComponentListener(new ComponentListener(){
            // when changing views, reset to show all files
            @Override
            public void componentHidden(ComponentEvent e) {
                showAllFiles();
            }
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}
        });
    }
    
    @Inject
    public void register(@Named("known") ListenerSupport<FriendEvent> knownFriendsListeners) {
        this.knownFriendsListeners = knownFriendsListeners;
        this.knownFriendsListeners.addListener(this);
    }


    @Override
    public void handleEvent(FriendEvent event) {
        Friend friend = event.getSource();

        switch (event.getType()) {
            case ADDED:
                sharingComboBox.addFriend(event.getSource());
                knownFriends.add(friend.getId());
                break;
            case REMOVED:
                sharingComboBox.removeFriend(event.getSource());
            case DELETE:
                knownFriends.remove(friend.getId());
                break;
            default:
                return;
        }
        getSelectionPanel().updateCollectionShares(knownFriends);
    }
    
    public Category getCategory() {
        return getSelectedCategory();
    }
    
    /**
	 * Will programtically select a friend to filter on.
	 */
    public void showSharingState(Friend friend) {
        sharingComboBox.selectFriend(friend);
    }
    
    /**
	 * Will cancel filtering and return My Library to a normal state.
	 */
    public void showAllFiles() {
        delegateListChanger.setFriend(null);
        sharingComboBox.setVisible(true);
        messagePanel.setVisible(false);
//        hideEmptyFriend();
        //reselect the current category in case we filtered on a friend that wasn't 
        //sharing anything
        Category category = getSelectedCategory();
        if(category == null)
            selectFirstVisible();
        else
            select(category);
    }
    
    /**
	 * Override the components to add in our message panel that gets displayed
     * above the table and nav when filtering on a friend.
	 */
    @Override
    protected void layoutComponent() {
        setLayout(new MigLayout("fill, gap 0, insets 0"));

        addHeaderPanel();
        addMessage();
        addNavPanel();
        addMainPanels();
    }
    
    /**
	 * Panel to display above nav and table when filtering is enabled.
	 */
    private void addMessage() {
        messagePanel = new SharingMessagePanel();
        
        add(messagePanel, "dock north, growx, hidemode 3");  
    }
    
    private void createMyCategories(EventList<LocalFileItem> sourceList) {
        // Display heading.
        addHeading(new HeadingPanel(I18n.tr("CATEGORIES")), Catalog.Type.CATEGORY);
        
        for(Category category : Category.getCategoriesInOrder()) {        
            CatalogSelectionCallback callback = null;
            if (category == Category.AUDIO) {
                callback = new CatalogSelectionCallback() {
                    @Override
                    public void catalogSelected(Catalog catalog, boolean state) {
                        playerPanel.setVisible(state);
                    }
                };
            }
            
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(sourceList//libraryFileList.getSwingModel()
                    , new CategoryFilter(category));
            addCategory(categoryIconManager.getIcon(category), category, 
                    createMyCategoryAction(category, filtered), filtered, callback);
            addDisposable(filtered);
            addLibraryInfoBar(category, filtered);
        }
    }

    private JComponent createMyCategoryAction(final Category category, EventList<LocalFileItem> filtered) {        
        final ShareWidget<File> fileShareWidget = shareFactory.createFileShareWidget();
        addDisposable(fileShareWidget);             
        JScrollPane scrollPane;        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable<LocalFileItem> table = tableFactory.createMyTable(category, filterList, delegateListChanger);
            table.enableMyLibrarySharing(fileShareWidget);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            selectableMap.put(category, table);
            scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());    
            addDisposable(table);
        } else { //Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createMyImagePanel(filterList, scrollPane, fileShareWidget, delegateListChanger);
            selectableMap.put(category, imagePanel);
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());            
            addDisposable(imagePanel);
        }           
        
        // The glass layer we display messages in.
        LockableUI blurUI = new LockedUI();
        JXLayer<JComponent> jxlayer = new JXLayer<JComponent>(scrollPane, blurUI);
        map.put(category, jxlayer);
        
        // only do this if the message hasn't been shown before
        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true || 
            SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.getValue() == true) {
            connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(XMPPConnectionEvent event) {
                    switch(event.getType()) { 
                    case CONNECTED:
                        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true && 
                           SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.getValue() == true) {
                            JPanel panel = new JPanel(new MigLayout("fill"));
                            panel.setOpaque(false);
                            panel.add(getFirstTimeMyLibraryMessageAndSignedInComponent(), "align 50% 40%");
                            JXLayer layer = map.get(category);
                            layer.getGlassPane().add(panel);
                            layer.getGlassPane().setVisible(true);
                            if(!SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.getValue()) {
                                libraryNavigator.selectLibrary();
                                SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.setValue(true);
                            }
                            SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.setValue(true);
                        } else if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true) {
                            JPanel panel = new JPanel(new MigLayout("fill"));
                            panel.setOpaque(false);
                            panel.add(getFirstTimeLoggedInMessageComponent(), "align 50% 40%");
                            JXLayer layer = map.get(category);
                            layer.getGlassPane().add(panel);
                            layer.getGlassPane().setVisible(true);
                            if(!SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.getValue()) {
                                libraryNavigator.selectLibrary();
                                SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.setValue(true);
                            }
                        }
                    }
                }
            });
            
            if(SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.getValue() == true) {
                JPanel panel = new JPanel(new MigLayout("fill"));
                panel.setOpaque(false);
                panel.add(getFirstTimeMyLibraryMessageComponent(), "align 50% 40%");
                JXLayer layer = map.get(category);
                layer.getGlassPane().add(panel);
                layer.getGlassPane().setVisible(true);
            }
        }

        return jxlayer;
    }
    
    /**
     * Adds the playlists to the container. 
     */
    private void createMyPlaylists() {
        // Display heading.
        addHeading(new HeadingPanel(I18n.tr("PLAYLISTS")), Catalog.Type.PLAYLIST);

        Catalog playlist = new Catalog(Catalog.Type.PLAYLIST, I18n.tr("Quicklist"));

        CatalogSelectionCallback callback = new CatalogSelectionCallback() {
            @Override
            public void catalogSelected(Catalog catalog, boolean state) {
                playerPanel.setVisible(state);
            }
        };
        
        // Create empty filtered list.
        //FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(
        //        libraryFileList.getSwingModel(), new CategoryFilter(category));
        FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(
                new BasicEventList<LocalFileItem>());
        
        // Add playlist to container. 
        // TODO create playlist icon manager
        addCatalog(categoryIconManager.getIcon(Category.OTHER), playlist,
                createMyPlaylistAction(playlist, filtered),
                null, filtered, callback);
        
        addDisposable(filtered);
        addLibraryInfoBar(playlist, filtered);
    }
    
    @Override
    protected void playListSelected(boolean value) {
        // hide the share button when a playlist is shown
        shareAllComboBox.setVisible(!value);
    }
    
    /**
     * Creates the component used to display a single playlist.
     */
    private JComponent createMyPlaylistAction(Catalog playlist, EventList<LocalFileItem> filtered) {
        // Create filtered list.
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        
        // TODO create factory method createPlaylistTable()
        LibraryTable<LocalFileItem> table = tableFactory.createMyTable(Category.AUDIO, filterList, delegateListChanger);
        // TODO review for possible inclusion/exclusion
        //table.enableMyLibrarySharing(fileShareWidget);
        //table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
        //selectableMap.put(category, table);
        addDisposable(table);
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportView(table);
        return scrollPane;
    }
    
    // TODO review createCatalogButton(), maybe override here
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    /**
	 * Displays the Not Sharing Message in this category.
	 */
    private void showEmptyFriend(Category category) {
        if(category != null) {
            JPanel panel = new JPanel(new MigLayout("fill"));
            panel.setOpaque(false);
            panel.add(getEmptyLibraryMessageComponent(delegateListChanger.getCurrentFriend()), "align 50% 40%");
            JXLayer layer = map.get(category);
            layer.getGlassPane().removeAll();
            layer.getGlassPane().add(panel);
            layer.getGlassPane().setVisible(true);
        }
    }
    
    /**
	 * Hides the Not Sharing Message in this particular category.
	 */
    private void hideEmptyFriend(Category category) {
        if(category != null) {
            JXLayer layer = map.get(category);
            layer.getGlassPane().setVisible(false);
        }
    }
    
    /**
	 * Hides the Not Sharing Message in all the tables.
	 */
    private void hideEmptyFriend() {
        for(Category category : Category.values()) {
            JXLayer layer = map.get(category);
            layer.getGlassPane().setVisible(false);
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        
        knownFriendsListeners.removeListener(this);
    }
    
    private static class MyLibraryDoubleClickHandler implements TableDoubleClickHandler{
        private LibraryTableModel<LocalFileItem> model;

        public MyLibraryDoubleClickHandler(LibraryTableModel<LocalFileItem> model){
            this.model = model;
        }

        @Override
        public void handleDoubleClick(int row) {
            File file = model.getFileItem(row).getFile();
            switch (model.getFileItem(row).getCategory()){
            case AUDIO:
                PlayerUtils.playOrLaunch(file);
                break;
            case OTHER:
            case PROGRAM:
                NativeLaunchUtils.launchExplorer(file);
                break;
            case IMAGE:
            case VIDEO:
            case DOCUMENT:
                NativeLaunchUtils.safeLaunchFile(file);
            }
        }
    }

    public LibraryFileList getLibrary() {
        return libraryManager.getLibraryManagedList();
    }

    public void selectItem(File file, Category category) {
        showAllFiles();
        select(category);
        selectableMap.get(category).selectAndScrollTo(file);
    }

    public File getNextItem(File file, Category category) {
        return selectableMap.get(category).getNextItem(file);
    }

    public File getPreviousItem(File file, Category category) {
        return selectableMap.get(category).getPreviousItem(file);
    }

    public void selectItem(URN urn, Category category) {
        showAllFiles();
        select(category);
        selectableMap.get(category).selectAndScrollTo(urn);        
    }    
    
    /**
	 * Returns the Table that is currently shown. Returns null if a playlist
	 * is being shown.
	 */ 
    public SelectAllable<LocalFileItem> getTable() {
        Category category = getSelectedCategory();
        if(category == null)
            return null;
        else
            return selectableMap.get(category);
    }
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        
        public LockedUI(LayerEffect... lockedEffects) {
            panel = new JXPanel(new MigLayout("aligny 50%, alignx 50%"));
            panel.setVisible(false);
        }
        
        @SuppressWarnings("unchecked")
        public void installUI(JComponent c) {
            super.installUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new BorderLayout());
            l.getGlassPane().add(panel, BorderLayout.CENTER);
        }
        
        @SuppressWarnings("unchecked")
        public void uninstall(JComponent c) {
            super.uninstallUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new FlowLayout());
            l.getGlassPane().remove(panel);
        }
        
        public void setLocked(boolean isLocked) {
            super.setLocked(isLocked);
            panel.setVisible(isLocked);
        }
        
        @Override
        public Cursor getLockedCursor() {
            return Cursor.getDefaultCursor();
        }
    }
    
    /**
     * Creates the MessageComponent when the user first signs in.
	 * This message is displayed atop the table and gives helpful
     * advice about what to do
     */
    public MessageComponent getFirstTimeLoggedInMessageComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("What Now?"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Click the share button to share or unshare files with a friend"));
        messageComponent.decorateSubLabel(minLabel);
        
        JLabel secondMinLabel = new JLabel(I18n.tr("Chat with them about using LimeWire 5"));
        messageComponent.decorateSubLabel(secondMinLabel);
        
        JButton cancelButton = new JButton(closeButton);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());
        cancelButton.setRolloverIcon(closeHoverButton);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for(JXLayer layer : map.values()) {
                    layer.getGlassPane().setVisible(false);
                }
                SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.setValue(false);
            }
        });

        messageComponent.addComponent(cancelButton, "span, alignx right");
        messageComponent.addComponent(headerLabel, "push, wrap");
        messageComponent.addComponent(minLabel, "wrap, gapright 16");
        messageComponent.addComponent(secondMinLabel, "gapright 16");

        return messageComponent;
    }
    
    /**
     * Creates the MessageComponent when the user goes to My Library
     * for the first time.
     */
    public MessageComponent getFirstTimeMyLibraryMessageComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("No more shared folders"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Click the share button to share or unshare files with the P2P Network"));
        messageComponent.decorateSubLabel(minLabel);
        
        JButton cancelButton = new JButton(closeButton);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());
        cancelButton.setRolloverIcon(closeHoverButton);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for(JXLayer layer : map.values()) {
                    layer.getGlassPane().setVisible(false);
                }
                SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.setValue(false);
            }
        });

        messageComponent.addComponent(cancelButton, "span, alignx right");
        messageComponent.addComponent(headerLabel, "push, wrap");
        messageComponent.addComponent(minLabel, "wrap, gapright 16");

        return messageComponent;
    }
    
    /**
     * Creates the MessageComponent when the user first signs in.
     * This message is displayed atop the table and gives helpful
     * advice about what to do
     */
    public MessageComponent getFirstTimeMyLibraryMessageAndSignedInComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("No more shared folders"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Click the share button to share or unshare files with the P2P Network"));
        messageComponent.decorateSubLabel(minLabel);
        
        JLabel minLabel2 = new JLabel(I18n.tr("Click the share button to share or unshare files with a friend"));
        messageComponent.decorateSubLabel(minLabel2);
        
        JButton cancelButton = new JButton(closeButton);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());
        cancelButton.setRolloverIcon(closeHoverButton);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for(JXLayer layer : map.values()) {
                    layer.getGlassPane().setVisible(false);
                }
                SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.setValue(false);
                SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.setValue(false);
            }
        });

        messageComponent.addComponent(cancelButton, "span, alignx right");
        messageComponent.addComponent(headerLabel, "push, wrap");
        messageComponent.addComponent(minLabel, "wrap, gapright 16");

        return messageComponent;
    }
    
    /**
	 * Returns the MessageComponent when not sharing with a friend.
	 */
    public MessageComponent getEmptyLibraryMessageComponent(Friend friend) {
        if(notSharingPanel == null) {
            notSharingPanel = new NotSharingPanel();          
        }
        notSharingPanel.setFriend(friend);
        return notSharingPanel.getMessageComponent();
    }

    /**
     * Message to be displayed when filtering on a friend and 
     * not sharing anything with them.
     */
    private class NotSharingPanel {
        private MessageComponent messageComponent;
        private JLabel headerLabel;
        private HyperlinkButton hyperlinkButton;
        
        public NotSharingPanel() {
            messageComponent = new MessageComponent(6, 22, 18, 6);
            
            headerLabel = new JLabel();
            messageComponent.decorateHeaderLabel(headerLabel);
            
            hyperlinkButton = new HyperlinkButton(I18n.tr("Show all Files"));
            hyperlinkButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAllFiles();
                }
            });
            
            messageComponent.addComponent(headerLabel, "wrap, gapright 16");
            messageComponent.addComponent(hyperlinkButton, "gapright 16");
        }
    
        public void setFriend(Friend friend) {
            if(friend.getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                headerLabel.setText(I18n.tr("You are not sharing any files with the {0}", friend.getRenderName()));
            else
                headerLabel.setText(I18n.tr("You are not sharing any files with {0}", friend.getRenderName()));
        }
        
        public MessageComponent getMessageComponent() {
            return messageComponent;
        }
    }
    
    /**
     * Component used to display catalog heading in the category/playlist
     * navigation bar.
     */
    private static class HeadingPanel extends JPanel {
        @Resource
        private Color textColor;
        @Resource
        private Font textFont;
        
        private JLabel label = new JLabel();
        
        public HeadingPanel(String text) {
            super(new MigLayout("insets 0, fill"));
            
            GuiUtils.assignResources(this);
            
            setOpaque(false);
            
            label.setBorder(BorderFactory.createEmptyBorder(2,8,2,0));
            label.setFont(textFont);
            label.setForeground(textColor);
            label.setText(text);
            
            add(label, "growx, push");
        }
    }
    
    /**
     * Message bar that sits atop main panel. Displayed when filtering on a sharing list.
     */
    private class SharingMessagePanel extends JPanel {
        @Resource 
        private Color backgroundColor;
        @Resource
        private Color labelColor;
        @Resource
        private Font labelFont;
        @Resource
        private Icon gnutellaIcon;
        @Resource
        private Icon friendIcon;
        @Resource
        private Color bottomLine;
        
        private final JLabel sharingLabel;
        private final HyperlinkButton showAllButton;
        
        public SharingMessagePanel() {            
            GuiUtils.assignResources(this);
            
            setLayout(new MigLayout("fill, alignx 50%, gap 0, insets 2 10 2 10", "", "25!"));
            setBackground(backgroundColor);
            
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, bottomLine));
            
            sharingLabel = new JLabel();
            sharingLabel.setForeground(labelColor);
            sharingLabel.setFont(labelFont);
            

            showAllButton = new HyperlinkButton(I18n.tr("Show All Files"));
            showAllButton.setFont(labelFont);
            FontUtils.bold(showAllButton);
            showAllButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAllFiles();
                }
            });

            add(sharingLabel, "push");
            add(showAllButton);
            
            setVisible(false);
        }
        
        /**	Sets the Friend's name to display when filtering on a Friend*/
        public void setMessage(Friend friend) {
            if(friend == null || friend.getId() == null) {
                setVisible(false);
                return;
            }
            if(friend.getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                sharingLabel.setIcon(gnutellaIcon);
            else
                sharingLabel.setIcon(friendIcon);
            sharingLabel.setText( "<html>" + I18n.tr("What I'm Sharing With {0}", boldName(friend.getRenderName())) + "</html>");
        }
        
        private String boldName(String name) {
            return "<b>" + name  + "</b>";
        }
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    protected <T extends FileItem> void addCatalogSizeListener(Catalog catalog,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        PlayListListener<T> listener = new PlayListListener<T>(action);
        addDisposable(listener);
    }
    
    private class PlayListListener<T> implements Disposable {
        private Action action;
        
        public PlayListListener(Action action) {
            this.action = action;
            
            delegateListChanger.addListener(new DelegateList.DelegateListener<LocalFileItem>() {
                @Override
                public void delegateChanged(DelegateList<LocalFileItem> delegateList,
                        EventList<LocalFileItem> theDelegate) {
                    updateList();
                }
            });
        }
        
        private void updateList() {
            //if not filtering
            if(delegateListChanger.getCurrentFriend() == null) {
                action.setEnabled(true);
            } else {
                action.setEnabled(false);
            }
        }
        
        @Override
        public void dispose() {
        }
    }
    
    /**
     * Hide any category that has no files in it.
     */
    private class ButtonSizeListener<T> implements Disposable, ListEventListener<T>, SettingListener {
        private final Category category;
        private final Action action;
        private final FilterList<T> list;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> list) {
            this.category = category;
            this.action = action;
            this.list = list;
            action.putValue(Action.NAME, I18n.tr(category.toString()));
            setText();
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.addSettingListener(this);
            }
            
            delegateListChanger.addListener(new DelegateList.DelegateListener<LocalFileItem>() {
                @Override
                public void delegateChanged(DelegateList<LocalFileItem> delegateList,
                        EventList<LocalFileItem> theDelegate) {
                    setText();
                }
            });
        }

        private void setText() {
            //if not filtering
            if(delegateListChanger.getCurrentFriend() == null) {
                //disable other category if size is 0
                if(category == Category.OTHER) {
                    action.setEnabled(list.size() > 0);
                } else if(category == Category.PROGRAM) { // hide program category is not enabled
                    action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
                } else {
                    action.setEnabled(true);
                }
            } else { //filtering on a friend
                if(category == Category.PROGRAM) { // hide program category is not enabled
                    action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
                }
                //disable any category if size is 0
                action.setEnabled(list.size() > 0);
                if(list.size() > 0)
                    hideEmptyFriend(category);
                else
                    showEmptyFriend(category);
            }
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.removeSettingListener(this);
            }
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    setText();                    
                }
            });
        }
    }    
}
