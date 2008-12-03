package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.MouseableTable.TableColors;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SharingLibraryPanel extends LibraryPanel implements PropertyChangeListener {
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final BaseLibraryMediator basePanel;
    private final FriendFileList friendFileList;
    
    private final Map<Category, LockableUI> locked = new EnumMap<Category, LockableUI>(Category.class);
    private final Map<Category, SharingSelectionPanel> listeners = new EnumMap<Category, SharingSelectionPanel>(Category.class);
    
    @AssistedInject
    public SharingLibraryPanel( @Assisted BaseLibraryMediator basePanel,
                                @Assisted Friend friend,
                                @Assisted EventList<LocalFileItem> eventList,
                                @Assisted FriendFileList friendFileList,
                                IconManager iconManager,
                                CategoryIconManager categoryIconManager,
                                LibraryTableFactory tableFactory,
                                LimeHeaderBarFactory headerBarFactory,
                                ButtonDecorator buttonDecorator) {
        super(friend, false, headerBarFactory);
        
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.basePanel = basePanel;
        this.friendFileList = friendFileList;
        
        this.friendFileList.addPropertyChangeListener(this);
        
        addShareButton(new BackToLibraryAction(), buttonDecorator);
        createMyCategories(eventList, friend, friendFileList);
        
        selectFirst();
    }
    
    private Map<Category, JComponent> createMyCategories(EventList<LocalFileItem> eventList, Friend friend, LocalFileList friendFileList) {
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<LocalFileItem> filteredAll = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            FilterList<LocalFileItem> filteredShared = GlazedListsFactory.filterList(friendFileList.getSwingModel(), new CategoryFilter(category));
            createButton(categoryIconManager.getIcon(category), category,
                        createMyCategoryAction(category, filteredAll, friend, friendFileList), filteredAll, filteredShared, null);
        }
        return categories;
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> filtered, Friend friend, final LocalFileList friendFileList) {
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));

        Comparator<LocalFileItem> c = new java.util.Comparator<LocalFileItem>() {
            @Override
            public int compare(LocalFileItem fileItem1, LocalFileItem fileItem2) {
                boolean containsF1 = friendFileList.contains(fileItem1.getFile());
                boolean containsF2 = friendFileList.contains(fileItem2.getFile());
                if(containsF1 && containsF2)
                    return 0;
                else if(containsF1 && !containsF2)
                    return -1;
                else
                    return 1;
            }
        };
        
        SortedList<LocalFileItem> sortedList = new SortedList<LocalFileItem>(filterList, c);

        JScrollPane scrollPane;
        
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createSharingTable(category, sortedList, friendFileList);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            table.enableSharing();
            addDisposable(table);
            
            scrollPane = new JScrollPane(table);
            scrollPane.setColumnHeaderView(table.getTableHeader());
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            if (table.isColumnControlVisible()) {
                scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }
			TableColors tableColors = new TableColors();
            table.addHighlighter(new ColorHighlighter(new UnsharedHighlightPredicate(getTableModel(table), friendFileList), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
        } else {//Category.IMAGE
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createSharingImagePanel(sortedList, scrollPane, friendFileList);
            addDisposable(imagePanel);
            
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
        
        if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
            LockableUI blurUI = new LockedUI(category.toString());
            JXLayer<JComponent> jxlayer = new JXLayer<JComponent>(scrollPane, blurUI);
            
            if(category == Category.AUDIO && this.friendFileList.isAddNewAudioAlways()) {
                blurUI.setLocked(true);
            } else if(category == Category.VIDEO && this.friendFileList.isAddNewVideoAlways()) {
                blurUI.setLocked(true);
            } if(category == Category.IMAGE && this.friendFileList.isAddNewImageAlways()) {
                blurUI.setLocked(true);
            }
            locked.put(category, blurUI);
            return jxlayer;
        }
        return scrollPane;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    @Override
    protected JComponent createSelectionButton(Action action, Category category) {
        SharingSelectionPanel panel = new SharingSelectionPanel(action, category);
        listeners.put(category, panel);
        return panel;
    }
    
    @Override
    public void dispose() {
        super.dispose();
        friendFileList.removePropertyChangeListener(this);
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
    
    private static class UnsharedHighlightPredicate implements HighlightPredicate {
        LibraryTableModel<LocalFileItem> libraryTableModel;
        private LocalFileList friendFileList;
        public UnsharedHighlightPredicate (LibraryTableModel<LocalFileItem> libraryTableModel, LocalFileList friendFileList) {
            this.libraryTableModel = libraryTableModel;
            this.friendFileList = friendFileList;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LocalFileItem fileItem = libraryTableModel.getFileItem(adapter.row);
            //TODO cache values?
            return !(friendFileList.contains(fileItem.getUrn()));
        }       
    }
    
    private class BackToLibraryAction extends AbstractAction {

        public BackToLibraryAction() {
            putValue(Action.NAME, I18n.tr("Back"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Returns to what's being shared with you."));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            basePanel.showLibraryCard();
        }
    }
    
    private class SharingSelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Color nonSelectedBackground;
        @Resource Color selectedTextColor;
        @Resource Color textColor;
        
        private JCheckBox checkBox;
        private JButton button;
        
        public SharingSelectionPanel(Action action, final Category category) {
            super(new MigLayout("insets 0, fill"));
            
            GuiUtils.assignResources(this);     

            checkBox = new JCheckBox();                
            checkBox.setContentAreaFilled(false);
            checkBox.setBorderPainted(false);
            checkBox.setFocusPainted(false);
            checkBox.setBorder(BorderFactory.createEmptyBorder(2,2,2,0));
            checkBox.setOpaque(false);
            
            add(checkBox);
            
            if(category != Category.AUDIO && category != Category.VIDEO && category != Category.IMAGE) {
                checkBox.setVisible(false);
            } else {
                if(category == Category.AUDIO) {
                    checkBox.setSelected(friendFileList.isAddNewAudioAlways());
                } else if(category == Category.VIDEO) {
                    checkBox.setSelected(friendFileList.isAddNewVideoAlways());
                } else if(category == Category.IMAGE) {
                    checkBox.setSelected(friendFileList.isAddNewImageAlways());
                }
                
                checkBox.addItemListener(new ItemListener(){
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        select(category);
                        if(category == Category.AUDIO) {
                            friendFileList.setAddNewAudioAlways(checkBox.isSelected());
                        } else if(category == Category.VIDEO) {
                            friendFileList.setAddNewVideoAlways(checkBox.isSelected());
                        } else if(category == Category.IMAGE) {
                            friendFileList.setAddNewImageAlways(checkBox.isSelected());
                        }
                    }
                });
            }

            button = new JButton(action);           
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        SharingSelectionPanel.this.repaint();
                    }
                }
            });
            add(button, "growx");
        
            addNavigation(button);
        }
        
        @Override
        public void paintComponent(Graphics g) {
            if(Boolean.TRUE.equals(button.getAction().getValue(Action.SELECTED_KEY))) {
                setBackground(selectedBackground);
                button.setForeground(selectedTextColor);
            } else {
                setBackground(nonSelectedBackground);
                button.setForeground(textColor);
            }
            super.paintComponent(g);
        }
        
        public void setSelect(boolean value) {
            checkBox.setSelected(value);
        }
    }    
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        private JPanel messagePanel;
        private JLabel label;
        private JLabel minLabel;
        
        public LockedUI(String category, LayerEffect... lockedEffects) {
            super(lockedEffects);
            
            messagePanel = new JPanel(new MigLayout("insets 5, gapy 10, wrap, alignx 50%"));
            messagePanel.setBackground(new Color(209,247,144));
            
            label = new JLabel(I18n.tr("Sharing entire {0} Collection with {1}", category, getFriend().getRenderName()));
            FontUtils.setSize(label, 12);
            FontUtils.bold(label);
            
            minLabel = new JLabel(I18n.tr("Sharing your {0} collection automatically shares new {1} files added to your Library", category, category.toLowerCase()));
            FontUtils.setSize(minLabel, 10);
            
            panel = new JXPanel(new MigLayout("aligny 50%, alignx 50%"));
            panel.setBackground(new Color(147,170,209,80));
            panel.setVisible(false);
            
            messagePanel.add(label, "alignx 50%");
            messagePanel.add(minLabel, "alignx 50%");
            
            panel.add(messagePanel);
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals("audioCollection")) {
            locked.get(Category.AUDIO).setLocked((Boolean)evt.getNewValue());
            listeners.get(Category.AUDIO).setSelect((Boolean)evt.getNewValue());
        } else if(evt.getPropertyName().equals("videoCollection")) {
            locked.get(Category.VIDEO).setLocked((Boolean)evt.getNewValue());
            listeners.get(Category.VIDEO).setSelect((Boolean)evt.getNewValue());
        } else if(evt.getPropertyName().equals("imageCollection")) {
            locked.get(Category.IMAGE).setLocked((Boolean)evt.getNewValue());
            listeners.get(Category.IMAGE).setSelect((Boolean)evt.getNewValue());
        }
    }
}
