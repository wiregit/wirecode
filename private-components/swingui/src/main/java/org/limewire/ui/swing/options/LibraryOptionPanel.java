package org.limewire.ui.swing.options;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryManagerTreeTable;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.wizard.AutoDirectoryManageConfig;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Library Option View
 */
public class LibraryOptionPanel extends OptionPanel {

    private final IconManager iconManager;    
    private final LibraryManager libraryManager;
    private final Provider<RemoteLibraryManager> remoteLibraries;
    private final Provider<ShareListManager> shareListManager;

    private LibraryManagerOptionPanel libraryManagerPanel;    
    private ShareCategoryPanel shareCategoryPanel;
    
    @Inject
    public LibraryOptionPanel(LibraryManager libraryManager, Provider<RemoteLibraryManager> remoteLibraries, 
            Provider<ShareListManager> shareListManager, IconManager iconManager) {
        this.iconManager = iconManager;
        this.libraryManager = libraryManager;
        this.remoteLibraries = remoteLibraries;
        this.shareListManager = shareListManager;
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getLibraryManagerPanel(), "pushx, growx");
        add(getShareCategoryPanel(), "pushx, growx");
    }
    
    private LibraryManagerOptionPanel getLibraryManagerPanel() {
        if(libraryManagerPanel == null) {
            libraryManagerPanel = new LibraryManagerOptionPanel(libraryManager.getLibraryData());
        }
        return libraryManagerPanel;
    }
    
    private OptionPanel getShareCategoryPanel() {
        if (shareCategoryPanel == null) {
            shareCategoryPanel = new ShareCategoryPanel();
        }
        return shareCategoryPanel;
    }

    @Override
    boolean applyOptions() {
        return getLibraryManagerPanel().applyOptions() ||
               getShareCategoryPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getLibraryManagerPanel().hasChanged() ||
               getShareCategoryPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getLibraryManagerPanel().initOptions();
        getShareCategoryPanel().initOptions();
    }
    
    
    /**
     * Share Category Panel
     */
    private class ShareCategoryPanel extends OptionPanel {

        private JRadioButton shareSnapshot;

        private JRadioButton shareCollection;

        private ButtonGroup buttonGroup;

        public ShareCategoryPanel() {
            super(I18n.tr("Sharing Categories"));
            setLayout(new MigLayout("", "", "[][][]"));

            shareSnapshot = new JRadioButton(I18n.tr("Only share files in that category at that point"));
            shareSnapshot.setContentAreaFilled(false);
            shareCollection = new JRadioButton(I18n.tr("Share my entire collection, including new files that automatically get added to My Library"));
            shareCollection.setContentAreaFilled(false);
            
            buttonGroup = new ButtonGroup();
            buttonGroup.add(shareSnapshot);
            buttonGroup.add(shareCollection);
            
            add(new JLabel(I18n.tr("When I share a category:")), "wrap");
            add(shareSnapshot, "gapleft 15, wrap");
            add(shareCollection, "gapleft 15");
        }
        
        @Override
        boolean applyOptions() {
            LibrarySettings.SNAPSHOT_SHARING_ENABLED.setValue(shareSnapshot.isSelected());
            // revert to defaults for collection sharing if true
            if(shareSnapshot.isSelected()) {
                //TODO: put this on a background thread??
                RemoteLibraryManager manager = remoteLibraries.get();
                ShareListManager shareManager = shareListManager.get();
                EventList<FriendLibrary> eventList = manager.getSwingFriendLibraryList();
                
                //clear the list of any friends that are currently signed on
                for(FriendLibrary friendLibrary : eventList) {
                    Friend friend = friendLibrary.getFriend();
                    FriendFileList fileList = shareManager.getFriendShareList(friend);
                    
                    fileList.setAddNewAudioAlways(false);
                    fileList.setAddNewImageAlways(false);
                    fileList.setAddNewVideoAlways(false);
                }
                // clear anyone that may be in an offline list 
                LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.revertToDefault();
                LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.revertToDefault();
                LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.revertToDefault();
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return shareSnapshot.isSelected() != LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue();
        }

        @Override
        public void initOptions() {
            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue())
                shareSnapshot.setSelected(true);
            else
                shareCollection.setSelected(true);
        }
    }
    
    /**
     * Library Manager Panel
     */
    public class LibraryManagerOptionPanel extends OptionPanel {

        private final LibraryData libraryData;
        
        private JButton addFolderButton;
        private JCheckBox audioCheckBox;
        private JCheckBox videoCheckBox;
        private JCheckBox imageCheckBox;
        private JCheckBox docCheckBox;
        private JCheckBox programCheckBox;
        private JCheckBox otherCheckBox;
        
        private LibraryManagerTreeTable treeTable;
        
        public LibraryManagerOptionPanel(LibraryData libraryData) {
            this.libraryData = libraryData;
            
            setLayout(new MigLayout("", "[300!][grow]", ""));
            
            setOpaque(false);
            
            createComponents();
            
            add(new JLabel(I18n.tr("LimeWire will automatically scan the folders below")), "span 2, gapbottom 10, wrap");
        
            add(new JScrollPane(treeTable), "span 2, growx, wrap");
            add(addFolderButton, "skip 1, alignx right, wrap");
            
            add(new JLabel(I18n.tr("and add the following types of files to My Library:")), "span 2, wrap");
            add(getCheckBoxPanel(), "gapbottom 20, span 2, wrap");
            
            add(new MultiLineLabel(I18n.tr("Scanning these folders into your Library will not automatically share your files unless you explicitly choose to share a collection.")), "span 2, growx");
        }
        
        private void createComponents() {
            treeTable = new LibraryManagerTreeTable(iconManager, libraryData);
            addFolderButton = new JButton(new AddDirectoryAction(this));
            
            audioCheckBox = new JCheckBox(I18n.tr(Category.AUDIO.toString()));
            audioCheckBox.setOpaque(false);
            audioCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
            
            videoCheckBox = new JCheckBox(I18n.tr(Category.VIDEO.toString()));
            videoCheckBox.setOpaque(false);
            videoCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
            
            imageCheckBox = new JCheckBox(I18n.tr(Category.IMAGE.toString()));
            imageCheckBox.setOpaque(false);
            imageCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
            
            docCheckBox = new JCheckBox(I18n.tr(Category.DOCUMENT.toString()));
            docCheckBox.setOpaque(false);
            docCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
            
            programCheckBox = new JCheckBox(I18n.tr(Category.PROGRAM.toString()));
            programCheckBox.setOpaque(false);
            programCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
            
            LibrarySettings.ALLOW_PROGRAMS.addSettingListener( new SettingListener() {
                @Override
                 public void settingChanged(SettingEvent evt) {
                    enablePrograms(LibrarySettings.ALLOW_PROGRAMS.getValue());
                }
             });
            
            otherCheckBox = new JCheckBox(I18n.tr(Category.OTHER.toString()));
            otherCheckBox.setOpaque(false);
            otherCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
        
        
        public void enablePrograms(boolean enable) {
            if(enable) {
                programCheckBox.setEnabled(true);
            } else {
                programCheckBox.setSelected(false);
                programCheckBox.setEnabled(false);
            }
        }
        private JPanel getCheckBoxPanel() {
            JPanel p = new JPanel();
            p.setOpaque(false);
            p.setLayout(new MigLayout("gapx 18"));
            
            p.add(audioCheckBox, "gapleft 25");        
            p.add(videoCheckBox);        
            p.add(imageCheckBox);        
            p.add(docCheckBox);
            p.add(programCheckBox);
            p.add(otherCheckBox);
            
            return p;
        }

        @Override
        public boolean applyOptions() {
            LibraryManagerModel model = treeTable.getLibraryModel();
            Collection<File> manage = model.getManagedDirectories();
            Collection<File> exclude = model.getExcludedDirectories();
            libraryData.setManagedOptions(manage, exclude, getManagedCategories());
            return false;
        }

        private Collection<Category> getManagedCategories() {
            Collection<Category> categories = EnumSet.noneOf(Category.class);
            if(audioCheckBox.isSelected()) {
                categories.add(Category.AUDIO);
            }
            if(videoCheckBox.isSelected()) {
                categories.add(Category.VIDEO);
            }
            if(imageCheckBox.isSelected()) {
                categories.add(Category.IMAGE);
            }
            if(docCheckBox.isSelected()) {
                categories.add(Category.DOCUMENT);
            }
            if(programCheckBox.isSelected()) {
                categories.add(Category.PROGRAM);
            }
            if(otherCheckBox.isSelected()) {
                categories.add(Category.OTHER);
            }
            return categories;
        }

        @Override
        boolean hasChanged() {
            LibraryManagerModel model = treeTable.getLibraryModel();
            Collection<File> manage = model.getManagedDirectories();
            Collection<File> exclude = model.getExcludedDirectories();
            Collection<Category> categories = getManagedCategories();
            return !manage.equals(libraryData.getDirectoriesToManageRecursively())
                || !exclude.equals(libraryData.getDirectoriesToExcludeFromManaging())
                || !categories.equals(libraryData.getManagedCategories());
        }

        @Override
        public void initOptions() {
            RootLibraryManagerItem root = new RootLibraryManagerItem(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
            for(File file : libraryData.getDirectoriesToManageRecursively()) {
                root.addChild(new LibraryManagerItemImpl(root, libraryData, file, false));
            }

            treeTable.setTreeTableModel(new LibraryManagerModel(root));
            
            Collection<Category> categories = libraryData.getManagedCategories();
            audioCheckBox.setSelected(categories.contains(Category.AUDIO));
            videoCheckBox.setSelected(categories.contains(Category.VIDEO));
            docCheckBox.setSelected(categories.contains(Category.DOCUMENT));
            imageCheckBox.setSelected(categories.contains(Category.IMAGE));
            programCheckBox.setSelected(categories.contains(Category.PROGRAM));
            programCheckBox.setEnabled(libraryData.isProgramManagingAllowed());
            otherCheckBox.setSelected(categories.contains(Category.OTHER));
        }
        
        private class AddDirectoryAction extends AbstractAction {

            private Container parent;
            
            public AddDirectoryAction(Container parent) {
                this.parent = parent;
                
                putValue(Action.NAME, I18n.tr("Add New Folder..."));
                putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a folder to automatically scan for changes"));
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                File folder = FileChooser.getInputDirectory(parent);            
                if(folder == null) {
                    return;
                } else {
                    if(libraryData.isDirectoryAllowed(folder)) {
                        treeTable.addDirectory(folder);
                    } else {
                    FocusJOptionPane.showMessageDialog(LibraryManagerOptionPanel.this,
                            I18n.tr("You selected: {0}\n\nLimeWire cannot manage " +
                                    "this folder because it is either not a folder " +
                                    "or cannot be read.\n\nPlease select another " +
                                    "folder to manage.", folder),
                            I18n.tr("Library Manager Error"),
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }    
    }

}
