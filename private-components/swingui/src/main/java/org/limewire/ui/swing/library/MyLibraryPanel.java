package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
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
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.SharingFilterComboBox;
import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor.Event;
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
    private final SharingMatchingEditor sharingMatchingEditor;
    
    private MessagePanel messagePanel;
    
    private LimeComboBox shareAllComboBox;

    @Inject
    public MyLibraryPanel(LibraryManager libraryManager,
                          LibraryNavigator libraryNavigator,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareWidgetFactory shareFactory,
                          HeaderBarDecorator headerBarFactory,
                          PlayerPanel player, 
                          GhostDragGlassPane ghostPane,
                          ListenerSupport<XMPPConnectionEvent> connectionListeners,
                          ShareListManager shareListManager,
                          TextFieldDecorator textFieldDecorator,
                          ComboBoxDecorator comboDecorator,
                          ButtonDecorator buttonDecorator) {
        
        super(headerBarFactory, textFieldDecorator);
        
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

        sharingMatchingEditor = new SharingMatchingEditor(shareListManager);
        sharingComboBox = new SharingFilterComboBox(sharingMatchingEditor);
        sharingComboBox.setMaximumSize(new Dimension(120, 30));
        sharingComboBox.setPreferredSize(sharingComboBox.getMaximumSize());

        buttonDecorator.decorateLightFullButton(messagePanel.getButton());
        
        comboDecorator.decorateLightFullComboBox(sharingComboBox);
        getSelectionPanel().add(sharingComboBox, "gaptop 5, gapbottom 5, alignx 50%, hidemode 3");
        
        sharingComboBox.addSelectionListener(new SelectionListener(){
            @Override
            public void selectionChanged(Action item) {
                messagePanel.setMessage(item.toString());
                messagePanel.setVisible(true);
                sharingComboBox.setVisible(false);
            }
        });
        
        FilterList<LocalFileItem> friendFilterList = GlazedListsFactory.filterList(libraryManager.getLibraryManagedList().getSwingModel(), 
                sharingMatchingEditor);
        createMyCategories(friendFilterList);
        createMyPlaylists();
        selectFirstVisible();

        this.knownFriends.add(Friend.P2P_FRIEND_ID);
        getSelectionPanel().updateCollectionShares(knownFriends);
        
        shareAllComboBox = new LimeComboBox();
        shareAllComboBox.setText("Share");
        
        addHeaderComponent(shareAllComboBox, "cell 0 0, alignx left");
        addHeaderComponent(playerPanel, "cell 0 0, grow");
        playerPanel.setMaximumSize(new Dimension(999,999));
        playerPanel.setPreferredSize(new Dimension(999,999));

        
        setTransferHandler(new MyLibraryTransferHandler(null, libraryManager.getLibraryManagedList()));
        try {
            getDropTarget().addDropTargetListener(new GhostDropTargetListener(this,ghostPane));
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
                repaintTimer.stop();
            }
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
    
    public void showSharingState(Friend friend) {
        sharingComboBox.selectFriend(friend);
        
    }
    
    public void showAllFiles() {
        sharingMatchingEditor.setFriend(null);
        sharingComboBox.setVisible(true);
        messagePanel.setVisible(false);
    }
    
    @Override
    protected void layoutComponent() {
        setLayout(new MigLayout("fill, gap 0, insets 0"));

        addHeaderPanel();
        addMessage();
        addNavPanel();
        addMainPanels();
    }
    
    private void addMessage() {
        messagePanel = new MessagePanel();
        
        add(messagePanel, "dock north, growx, hidemode 3");  
    }
    
    private void createMyCategories(EventList<LocalFileItem> sourceList) {
        // Display heading.
        addHeading(new HeadingPanel(I18n.tr("CATEGORIES")), false);
        
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
        //TODO: can this be a singleton??? 
        final ShareWidget<File> fileShareWidget = shareFactory.createFileShareWidget();
        addDisposable(fileShareWidget);             
        JScrollPane scrollPane;        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable<LocalFileItem> table = tableFactory.createMyTable(category, filterList);
            table.enableMyLibrarySharing(fileShareWidget);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            selectableMap.put(category, table);
            scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());    
            addDisposable(table);
        } else { //Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(filterList, scrollPane, fileShareWidget);
            selectableMap.put(category, imagePanel);
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());            
            addDisposable(imagePanel);
        }           
        // only do this if the message hasn't been shown before
        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true) {
            connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(XMPPConnectionEvent event) {
                    switch(event.getType()) { 
                    case CONNECTED:
                        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true) {
                            JPanel panel = new JPanel(new MigLayout("fill"));
                            panel.setOpaque(false);
                            panel.add(getMessageComponent(), "align 50% 40%");
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
            // this shouldn't be necesarry, the panel above isn't fill the viewport if we don't set this at
            // jxlayer creation time. 
            LockableUI blurUI = new LockedUI(category);
            JXLayer<JComponent> jxlayer = new JXLayer<JComponent>(scrollPane, blurUI);
            map.put(category, jxlayer);
            return jxlayer;
        }
        return scrollPane;
    }
    
    /**
     * Adds the playlists to the container. 
     */
    private void createMyPlaylists() {
        // Display heading.
        addHeading(new HeadingPanel(I18n.tr("PLAYLISTS")), true);

        Playlist playlist = new Playlist(I18n.tr("Quicklist"));

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
    
    /**
     * Creates the component used to display a single playlist.
     */
    private JComponent createMyPlaylistAction(Playlist playlist, EventList<LocalFileItem> filtered) {
        // Create filtered list.
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        
        // TODO create factory method createPlaylistTable()
        LibraryTable<LocalFileItem> table = tableFactory.createMyTable(Category.AUDIO, filterList);
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
                //TODO: image double click
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
        select(category);
        selectableMap.get(category).selectAndScrollTo(urn);        
    }    
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        
        public LockedUI(Category category, LayerEffect... lockedEffects) {
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
     */
    public MessageComponent getMessageComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("What Now?"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Share entire categories or individual files with your friends."));
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
    
    private class MessagePanel extends JPanel {
        @Resource 
        private Color backgroundColor;
        @Resource
        private Color labelColor;
        @Resource
        private Font labelFont;
        
        private final JLabel sharingLabel;
        private final JXButton showAllButton;
        
        public MessagePanel() {            
            GuiUtils.assignResources(this);
            
            setLayout(new MigLayout("fill, alignx 50%, gap 0, insets 2 10 2 10"));
            setBackground(backgroundColor);
            
            sharingLabel = new JLabel();
            sharingLabel.setForeground(labelColor);
            sharingLabel.setFont(labelFont);
            
            showAllButton = new JXButton(I18n.tr("Show All Files"));
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
        
        public JXButton getButton() {
            return showAllButton;
        }
        
        public void setMessage(String friendRenderName) {
            sharingLabel.setText(I18n.tr("What I'm Sharing With {0}", friendRenderName));
        }
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Comment this out & return null if you don't want sizes added to library panels.
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
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
            
            sharingMatchingEditor.addMatcherEditorListener(new MatcherEditor.Listener<LocalFileItem>(){
                @Override
                public void changedMatcher(Event<LocalFileItem> matcherEvent) {
                    setText();
                }
            });
        }

        private void setText() {
            if(sharingMatchingEditor.getCurrentFilter() == null) {
                //disable other category if size is 0
                if(category == Category.OTHER) {
                    action.setEnabled(list.size() > 0);
                } else if(category == Category.PROGRAM) { // hide program category is not enabled
                    action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
                } else {
                    action.setEnabled(true);
                }
            } else {
                if(category == Category.PROGRAM) { // hide program category is not enabled
                    action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
                }
                //disable any category if size is 0
                action.setEnabled(list.size() > 0);
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
