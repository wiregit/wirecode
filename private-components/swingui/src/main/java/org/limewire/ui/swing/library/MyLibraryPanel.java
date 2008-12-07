/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.CategoryShareModel;
import org.limewire.ui.swing.library.sharing.FileShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class MyLibraryPanel extends LibraryPanel {
    
    private final Collection<Friend> allFriends;
    private final ShareListManager shareListManager;
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final PlayerPanel playerPanel;
    private final LibraryManager libraryManager;
    
    private LibrarySharePanel shareAllPanel = null;
    
    @Inject
    public MyLibraryPanel(LibraryManager libraryManager,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareListManager shareListManager,
                          @Named("known") Collection<Friend> allFriends,
                          LimeHeaderBarFactory headerBarFactory,
                          PlayerPanel player) {
        super(headerBarFactory);
        
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.allFriends = allFriends;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;       
        this.playerPanel = player;
        
        getHeaderPanel().setText(I18n.tr("My Library"));
        shareAllPanel = new LibrarySharePanel(allFriends);
        shareAllPanel.setShareModel(new CategoryShareModel(shareListManager));
        createMyCategories(libraryManager.getLibraryManagedList().getSwingModel());
        
        selectFirst();
        
        addHeaderComponent(playerPanel, "cell 0 0, grow");
        playerPanel.setMaximumSize(new Dimension(999,999));
        playerPanel.setPreferredSize(new Dimension(999,999));        
    }
    
    private void createMyCategories(EventList<LocalFileItem> eventList) {
        for(Category category : Category.getCategoriesInOrder()) {
        
            CategorySelectionCallback callback = null;
            if (category == Category.AUDIO) {
                callback = new CategorySelectionCallback() {
                    @Override
                    public void call(Category category, boolean state) {
                        playerPanel.setVisible(state);
                    }
                };
            }
            
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            addCategory(categoryIconManager.getIcon(category), category, 
                    createMyCategoryAction(category, filtered), filtered, callback);
            addDisposable(filtered);
        }
    }

    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> filtered) {
        
        //TODO: can this be a singleton??? 
        final LibrarySharePanel sharePanel = new LibrarySharePanel(allFriends);
        addDisposable(sharePanel);
        
        sharePanel.setShareModel(new FileShareModel(shareListManager));
        
        final JScrollPane scrollPane;
        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createMyTable(category, filterList);
            table.enableMyLibrarySharing(sharePanel);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            
            scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());    

            addDisposable(table);
//            librarySelectable = table;

        } else {//Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(filterList, scrollPane, sharePanel);
            
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            addDisposable(imagePanel);
//            librarySelectable = imagePanel;
        }
                      
        return scrollPane;
    }
    
    @Override
    protected JComponent createCategoryButton(Action action, Category category) {
        MySelectionPanel component = new MySelectionPanel(action, new ShareAllAction(category), category);
        addNavigation(component.getButton());
        return component;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    @Override
    public void dispose() {
        super.dispose();
        
        shareAllPanel.dispose();
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
                if (PlayerUtils.isPlayableFile(file)){
                    PlayerUtils.play(file);
                } else {                
                    NativeLaunchUtils.launchFile(file);
                }
                break;
            case OTHER:
            case PROGRAM:
                NativeLaunchUtils.launchExplorer(file);
                break;
            case IMAGE:
                //TODO: image double click
            case VIDEO:
            case DOCUMENT:
                NativeLaunchUtils.launchFile(file);
            }
        }
    }
    
    /**
     * Display the Share Collection widget when pressed
     */
    private class ShareAllAction extends AbstractAction {

        private Category category;
        
        public ShareAllAction(Category category) {
            this.category = category;
            
            putValue(Action.NAME, I18n.tr("share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share collection"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
          ((CategoryShareModel)shareAllPanel.getShareModel()).setCategory(category);
          String catStr = category.toString();
          shareAllPanel.setBottomLabel(
                  I18n.tr("Sharing your {0} collection shares new {1} files that automatically get added to your Library", catStr, catStr.toLowerCase()));
          shareAllPanel.show((JComponent)e.getSource());
        }
    }
    
    //TODO: use a button painter and JXButton
    private static class MySelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Color selectedTextColor;
        @Resource Color textColor;
        @Resource Font shareButtonFont;
        @Resource Color shareForegroundColor;
        @Resource Color shareMouseOverColor;
        
        private JButton button;
        private HyperLinkButton shareButton;
        private JLabel collectionLabel;
        
        public MySelectionPanel(Action action, Action shareAction, Category category) {
            super(new MigLayout("insets 0, fill, hidemode 3"));

            GuiUtils.assignResources(this);
            setOpaque(false);

            button = new JButton(action);           
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,8,2,0));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setOpaque(false);
            button.getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(Boolean.TRUE.equals(evt.getNewValue())) {
                            setOpaque(true);
                            setBackground(selectedBackground);
                            button.setForeground(selectedTextColor);
                            if(shareButton != null) {
                                shareButton.setVisible(true);
                                collectionLabel.setVisible(collectionLabel.isEnabled());
                            }
                        } else {
                            setOpaque(false);
                            button.setForeground(textColor);
                            if(shareButton != null) {
                                shareButton.setVisible(false);
                                collectionLabel.setVisible(false);
                            }
                        }
                        repaint();
                    }   
                }
                    
            });
            
            add(button, "growx, push");
            
            // only add a share category button if its an audio/video/image category
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                shareButton = new HyperLinkButton(null, shareAction);
                shareButton.setContentAreaFilled(false);
                shareButton.setBorderPainted(false);
                shareButton.setFocusPainted(false);
                shareButton.setBorder(BorderFactory.createEmptyBorder(2,0,2,4));
                shareButton.setOpaque(false);
                shareButton.setVisible(false);
                FontUtils.underline(shareButton);
                shareButton.setFont(shareButtonFont);
                shareButton.setForeground(shareForegroundColor);
                shareButton.setMouseOverColor(shareMouseOverColor);
                add(shareButton, "wrap");
                
                collectionLabel = new JLabel();
                collectionLabel.setVisible(false);
                setLabelText(0);
                add(collectionLabel, "span 2, gapleft 10");
                
                //TODO: there should be a better way of adding listeners here
                if(category == Category.AUDIO) {
                    setLabelText(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.getValue().length);
                    LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.addSettingListener(new SettingListener(){
                        @Override
                        public void settingChanged(SettingEvent evt) {
                            setLabelText(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.getValue().length);
                        }
                     });
                } else if(category == Category.VIDEO) {
                    setLabelText(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.getValue().length);
                    LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.addSettingListener(new SettingListener(){
                        @Override
                        public void settingChanged(SettingEvent evt) {
                            setLabelText(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.getValue().length);
                        }
                     });
                } else if(category == Category.IMAGE) { 
                    setLabelText(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.getValue().length);
                    LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.addSettingListener(new SettingListener(){
                        @Override
                        public void settingChanged(SettingEvent evt) {
                            setLabelText(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.getValue().length);
                        }
                     });
                }
            }
        }
        
        private void setLabelText(int numSharedCollections) {
            collectionLabel.setText(I18n.tr("Sharing collection: {0}", numSharedCollections));
            collectionLabel.setEnabled(numSharedCollections > 0);
        }
        
        public JButton getButton() {
            return button;
        }
    }

    public LibraryFileList getLibrary() {
        return libraryManager.getLibraryManagedList();
    }    
}
