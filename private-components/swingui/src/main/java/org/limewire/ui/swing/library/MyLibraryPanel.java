/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.AllFriendsList;
import org.limewire.ui.swing.library.sharing.CategoryShareModel;
import org.limewire.ui.swing.library.sharing.FileShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class MyLibraryPanel extends LibraryPanel {
    private AllFriendsList allFriendsList;
    private ShareListManager shareListManager;
    private LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;

    private LibrarySharePanel shareAllPanel = null;
    
    @AssistedInject
    public MyLibraryPanel(  @Assisted Friend friend,
                            @Assisted EventList<LocalFileItem> eventList,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareListManager shareListManager,
                          AllFriendsList allFriendsList,
                          LimeHeaderBarFactory headerBarFactory){
        super(null, true, headerBarFactory);
        
        this.shareListManager = shareListManager;
        this.allFriendsList = allFriendsList;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;
       
        loadHeader();
        loadSelectionPanel();
        createMyCategories(eventList);
        
        selectFirst();
    }

    @Override
    public void loadHeader() {
        shareAllPanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        shareAllPanel.setShareModel(new CategoryShareModel(shareListManager));
    }

    @Override
    public void loadSelectionPanel() {
    }
    
    private Map<Category, JComponent> createMyCategories(EventList<LocalFileItem> eventList) {
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            createButton(categoryIconManager.getIcon(category), category, 
                    createMyCategoryAction(category, filtered), filtered);
        }
        return categories;
    }

    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> filtered) {
        
        //TODO: can this be a singleton??? 
        final LibrarySharePanel sharePanel = new LibrarySharePanel(allFriendsList.getAllFriends());
        addDisposable(sharePanel);
        
        sharePanel.setShareModel(new FileShareModel(shareListManager));
        
        final JScrollPane scrollPane;
        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createTable(category, filterList, null);
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
    protected JComponent createSelectionButton(Action action, Category category) {
        return new MySelectionPanel(action, new ShareAllAction(category), category);
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
            
            putValue(Action.NAME, I18n.tr("Share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share collection"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
          ((CategoryShareModel)shareAllPanel.getShareModel()).setCategory(category);
          shareAllPanel.setBottomLabel(
                  I18n.tr("Sharing your {0} collection automatically shares new {0} files added to your Library", category));
          shareAllPanel.show((JComponent)e.getSource());
        }
    }
    
    //TODO: use a button painter and JXButton
    private class MySelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Color nonSelectedBackground;
        @Resource Color selectedTextColor;
        @Resource Color textColor;
        @Resource Color linkColor;
        
        private JButton button;
        private JButton shareButton;
        
        public MySelectionPanel(Action action, Action shareAction, Category category) {
            super(new MigLayout("insets 0, fill"));

            GuiUtils.assignResources(this);

            button = new JButton(action);           
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,8,2,0));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        MySelectionPanel.this.repaint();
                    }
                }
            });
            
            add(button, "growx, push");
            
            // only add a share category button if its an audio/video/image category
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                shareButton = new JButton(shareAction);
                shareButton.setContentAreaFilled(false);
                shareButton.setBorderPainted(false);
                shareButton.setFocusPainted(false);
                shareButton.setBorder(BorderFactory.createEmptyBorder(2,0,2,4));
                shareButton.setOpaque(false);
                shareButton.setForeground(linkColor);
                shareButton.setVisible(false);
                shareButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                FontUtils.underline(shareButton);
                shareButton.setName("my name yo");
                add(shareButton);
            }
        
            addNavigation(button);
        }
        
        @Override
        public void paintComponent(Graphics g) {
            if(Boolean.TRUE.equals(button.getAction().getValue(Action.SELECTED_KEY))) {
                setBackground(selectedBackground);
                button.setForeground(selectedTextColor);
                if(shareButton != null)
                    shareButton.setVisible(true);
            } else {
                setBackground(nonSelectedBackground);
                button.setForeground(textColor);
                if(shareButton != null)
                    shareButton.setVisible(false);
            }
            super.paintComponent(g);
        }
        
        public JButton getButton() {
            return button;
        }
    }    
}
