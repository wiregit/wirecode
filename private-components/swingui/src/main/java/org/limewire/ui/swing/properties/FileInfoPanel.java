package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.MetaDataException;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.Address;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.BitziLookupAction;
import org.limewire.ui.swing.components.CollectionBackedComboBoxModel;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.MagnetLinkCopier;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.CopyMagnetLinkToClipboardAction;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Panel within a FileInfo Dialog. Displays relevant information about the 
 * file being viewed.
 */
public class FileInfoPanel extends JPanel {
    
    @Resource private Color backgroundColor;
    @Resource private Font smallFont;
    @Resource private Font smallBoldFont;
    @Resource private Font mediumFont;
    @Resource private Font largeFont;
    @Resource private Icon removeIcon;
    @Resource private Icon removeIconRollover;
    @Resource private Icon removeIconPressed;
    
    private final Provider<IconManager> iconManager;
    private final CategoryIconManager categoryIconManager;
    private final ThumbnailManager thumbnailManager;
    private final MagnetLinkFactory magnetLinkFactory;
    private final LibraryNavigator libraryNavigator;
    private final PropertyDictionary propertyDictionary;
//    private final SharedFileListManager shareListManager;
//    private final FriendManager friendManager;
    private final SpamManager spamManager;
    private final MetaDataManager metaDataManager;
    
    private final Map<FilePropertyKey, JComponent> changedProps = new HashMap<FilePropertyKey, JComponent>();
    
    private final FileInfoType type;
    private final PropertiableFile propertiableFile;
    
    private DownloadStatus downloadStatus;
    
    @Inject
    public FileInfoPanel(@Assisted FileInfoType type, @Assisted PropertiableFile propertiableFile, 
            Provider<IconManager> iconManager, CategoryIconManager categoryIconManager,
            ThumbnailManager thumbnailManager, MagnetLinkFactory magnetLinkFactory,
            LibraryNavigator libraryNavigator, PropertyDictionary propertyDictionary,
            SharedFileListManager shareListManager, FriendManager friendManager,
            SpamManager spamManager, MetaDataManager metaDataManager) {
        this.iconManager = iconManager;
        this.categoryIconManager = categoryIconManager;
        this.thumbnailManager = thumbnailManager;
        this.magnetLinkFactory = magnetLinkFactory;
        this.libraryNavigator = libraryNavigator;
        this.propertyDictionary = propertyDictionary;
//        this.shareListManager = shareListManager;
//        this.friendManager = friendManager;
        this.spamManager = spamManager;
        this.metaDataManager = metaDataManager;
        
        this.type = type;
        this.propertiableFile = propertiableFile;
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("gap 0, insets 0, fill, wrap"));
        
        createOverview();
        createDetails();
        createLocation();
        createSharing();
        createDownloading();
        
        setBackground(backgroundColor);
    }
    
    /**
     * Creates the OverView Panel
     * - displays basic information about the file, icon,filesize, name, urn, etc..
     */
    private void createOverview() {
        JPanel overviewPanel = new JPanel(new MigLayout("fillx, insets 10 3 10 10"));
        overviewPanel.setOpaque(false);
        overviewPanel.add(createHeaderLabel(I18n.tr("Overview")),"dock north, gap 7, gaptop 10, gapright 7, wrap");
        overviewPanel.add(Line.createHorizontalLine(),"dock north, gap 7, gapright 7, gaptop 4, wrap");

        addOverviewCategory(propertiableFile, overviewPanel);

        
        HyperlinkButton copyToClipboard = new HyperlinkButton();
        copyToClipboard.setFont(smallFont);
        if(type == FileInfoType.LOCAL_FILE){
            if(propertiableFile instanceof LocalFileItem) {
                copyToClipboard.setAction(new AbstractAction(I18n.tr("Copy Link")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new MagnetLinkCopier().copyLinkToClipBoard((LocalFileItem)propertiableFile, magnetLinkFactory);
                    }
                });
            }
        } else if(type == FileInfoType.REMOTE_FILE) {
            if(propertiableFile instanceof VisualSearchResult)
                copyToClipboard.setAction(new CopyMagnetLinkToClipboardAction((VisualSearchResult)propertiableFile));
        } 
        HyperlinkButton moreFileInfo = new HyperlinkButton(new BitziLookupAction(propertiableFile));
        moreFileInfo.setFont(smallFont);
      
        overviewPanel.add(copyToClipboard, "cell 1 1, alignx right");
        overviewPanel.add(moreFileInfo, "cell 1 2, alignx right");
        
        add(overviewPanel, "growx");
    }
    
    /**
     * Creates Detail Panel
     * - Contains XML data about the file. If this file is stored locally, this
     *   information can be edited.
     */
    private void createDetails() {
        if(propertiableFile.getCategory() != Category.OTHER) {
            JPanel detailsPanel = createPanel(I18n.tr("Details"));
            addEditableDetails(propertiableFile, detailsPanel);
            add(detailsPanel, "growx");
        }
    }
    
    /**
     * Creates the Location Panel
     * - displays information about where the file is located.
     */
    private void createLocation() {
        JPanel locationPanel = createPanel(I18n.tr("Location"));
        
        switch(type) {
        case LOCAL_FILE:
            if(propertiableFile instanceof LocalFileItem) {
                locationPanel.add(createLabelField(((LocalFileItem)propertiableFile).getFile().getAbsolutePath()), "span, growx, wrap");
                
                HyperlinkButton locateOnDisk = new HyperlinkButton(
                    new AbstractAction(I18n.tr("Locate on Disk")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NativeLaunchUtils.launchExplorer(((LocalFileItem)propertiableFile).getFile());
                        }
                    });
                
                HyperlinkButton locateInLibrary = new HyperlinkButton( 
                    new AbstractAction(I18n.tr("Locate in Library")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setVisible(false);
                            libraryNavigator.selectInLibrary(((LocalFileItem)propertiableFile).getFile(), propertiableFile.getCategory());
                        }
                    });
                
                locationPanel.add(locateOnDisk, "span, alignx right, split");
                locationPanel.add(locateInLibrary, "gapleft 15, wrap");
            }
            break;
        case REMOTE_FILE:
            if(propertiableFile instanceof VisualSearchResult) {
                final ReadOnlyTableModel model = new ReadOnlyTableModel();
                final JTable table = new JTable(model);
                
                model.setColumnIdentifiers(new Object[] { tr("Name"), tr("Address"), tr("Filename") });
    
                for (SearchResult result : ((VisualSearchResult)propertiableFile).getCoreSearchResults()) {
                    for (RemoteHost host : result.getSources()) {
                        Friend f = host.getFriendPresence().getFriend();
                        model.addRow(new Object[] {
                                f.getRenderName(),
                                f.getName(),
                                result.getFileName()
                        });
                    }
                }
                locationPanel.add(new JScrollPane(table), "span, grow, wrap");
                
                table.addMouseListener(new MousePopupListener() {
                    @Override
                    public void handlePopupMouseEvent(final MouseEvent e) {
                        JPopupMenu blockingMenu = new JPopupMenu();
                        blockingMenu.add(new AbstractAction(tr("Block Address")) {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                int blockRow = table.rowAtPoint(e.getPoint());
                                table.getSelectionModel().setSelectionInterval(blockRow, blockRow);
                                Object value = model.getValueAt(blockRow, 1);
                                if (value != null) {
                                    addToFilterList(value.toString());
                                }
                            }
                        });
                        blockingMenu.show(table, e.getX(), e.getY());
                    }
                });
            } else if(propertiableFile instanceof RemoteFileItem) {
                RemoteFileItem item = (RemoteFileItem) propertiableFile;
                String friend = item.getSources().get(0).getFriendPresence().getFriend().getRenderName();
                locationPanel.add(createLabelField(friend), "span, growx, wrap");
            }
            break;
        case DOWNLOADING_FILE:
            if(propertiableFile instanceof DownloadItem) {
                locationPanel.add(createLabelField(((DownloadItem)propertiableFile).getDownloadingFile().getAbsolutePath()), "span, growx, wrap");
                
                HyperlinkButton locateOnDisk2 = new HyperlinkButton(
                    new AbstractAction(I18n.tr("Locate on Disk")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NativeLaunchUtils.launchExplorer(((DownloadItem)propertiableFile).getDownloadingFile());
                        }
                    });
                
                HyperlinkButton locateInLibrary2 = new HyperlinkButton( 
                    new AbstractAction(I18n.tr("Locate in Library")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setVisible(false);
                            libraryNavigator.selectInLibrary(((DownloadItem)propertiableFile).getDownloadingFile(), propertiableFile.getCategory());
                        }
                    });
                
                locationPanel.add(locateOnDisk2, "span, alignx right, split");
                locationPanel.add(locateInLibrary2, "gapleft 15, wrap");
            }
            break;
        }
        add(locationPanel, "growx");
    }
    
    /**
     * Creates Sharing Panel
     * - if the file is local, displays a list of names who this file is being
     *   shared with, otherwise this panel is empty.
     */
    private void createSharing() {
        switch(type) {
        case LOCAL_FILE:
            if(propertiableFile instanceof LocalFileItem) {
                List<Friend> sharedWithList = getSharedWithList((LocalFileItem)propertiableFile);
                if(sharedWithList.size() > 0) {
                    final JPanel sharingPanel = createPanel(I18n.tr("Sharing with"));
                    final JPanel listPanel = new JPanel(new MigLayout("fillx, nogrid, gap 0! 0!, insets 0 0 0 0"));
                    listPanel.setBackground(backgroundColor);
                    
                    for(Friend friend : sharedWithList) {
//                        final Friend shareFriend = friend;
                        final JLabel friendLabel = new JLabel(friend.getRenderName());
                        final JButton friendButton = new IconButton(removeIcon, removeIconRollover, removeIconPressed);
                        friendButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if(true) throw new NotImplementedException();
//                                if(shareFriend.getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId())) {
//                                    shareListManager.getGnutellaShareList().removeFile(((LocalFileItem)propertiableFile).getFile());
//                                } else {
//                                    shareListManager.getOrCreateFriendShareList(shareFriend).removeFile(((LocalFileItem)propertiableFile).getFile());
//                                }
                                listPanel.remove(friendLabel);
                                listPanel.remove(friendButton);
                                //make sure the friend actually disappears
                                sharingPanel.revalidate();
                                //make sure we don't get stuck on the hand cursor
                                setCursor(Cursor.getDefaultCursor());
                            }
                        });
                        
                        listPanel.add(friendButton);
                        listPanel.add(friendLabel, "gapright 20, wrap");
                    }
                    JScrollPane scroll = new JScrollPane(listPanel);
                    scroll.setOpaque(false);
                    scroll.setBorder(BorderFactory.createEmptyBorder());
                    sharingPanel.add(scroll, "grow");
                    
                    add(sharingPanel, "growx");
                }
            }
            break;
        }
    }
    
    /**
     * Creates Download Panel
     * - displays a list of addresses from which this file is being
     *   downloaded from. If this file is not being downloaded, this
     *   panel is empty
     */
    private void createDownloading() {
        switch(type) {
        case DOWNLOADING_FILE:
            if(propertiableFile instanceof DownloadItem) {
                JPanel downloadPanel = createPanel(I18n.tr("Downloading from"));
                downloadPanel.removeAll();
                downloadPanel.add(createHeaderLabel(I18n.tr("Downloading from")),"push");
                
                final JLabel percentLabel = createPlainLabel("test");            
                downloadPanel.add(percentLabel, "alignx right, wrap");
                downloadPanel.add(Line.createHorizontalLine(),"span, growx 100, gapbottom 4, wrap");
                
                final ReadOnlyTableModel model = new ReadOnlyTableModel();
                final JTable readOnlyInfo = new JTable(model);
                model.setColumnCount(2);
                model.setColumnIdentifiers(new Object[]{tr("Address"), tr("Filename")});
                
                for(Address source : ((DownloadItem)propertiableFile).getSources()) {
                    model.addRow(new Object[] {source.getAddressDescription(), 
                                               ((DownloadItem)propertiableFile).getDownloadingFile().getName() });
                }
                downloadStatus = new DownloadStatus(percentLabel);
                ((DownloadItem)propertiableFile).addPropertyChangeListener(downloadStatus);
                
                downloadPanel.add(new JScrollPane(readOnlyInfo), "span, grow, wrap");
                
                add(downloadPanel, "growx");
            }
            break;
        }
    }
    
    /**
     * Commits the changes to disk. If this file has been modified and this
     * file exists locally, the changes will be saved.
     */
    public void commit() {
        switch(type) {
        case LOCAL_FILE: 
            if (changedProps.size() != 0 && propertiableFile instanceof LocalFileItem) {
                LocalFileItem item = (LocalFileItem) propertiableFile;
                for (FilePropertyKey key : changedProps.keySet()) {
                    JComponent component = changedProps.get(key);
                    if(component instanceof JTextField) {
                        item.setProperty(key, ((JTextField)component).getText().trim());
                    } else if(component instanceof JTextArea) {
                        item.setProperty(key, ((JTextArea)component).getText().trim());
                    } else if(component instanceof JComboBox) {
                        item.setProperty(key, ((JComboBox)component).getSelectedItem());
                    }
                }
                try {
                    metaDataManager.save(item);
                } catch (MetaDataException e) {
                    String message = I18n.tr("Unable to save metadata changes.");
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), message, I18n.tr("View File Info"), 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }     
            break;
        }
    }
    
    /**
     * Removes any listeners which may have been registered.
     */
    public void unregisterListeners() {
        if(downloadStatus != null && propertiableFile instanceof DownloadItem) {
            ((DownloadItem)propertiableFile).removePropertyChangeListener(downloadStatus);
        }
    }
    
    private JPanel createPanel(String title) {
        JPanel detailsPanel = new JPanel(new MigLayout("fillx"));
        detailsPanel.setOpaque(false);
        detailsPanel.add(createHeaderLabel(title),"span, growx 100, wrap");
        detailsPanel.add(Line.createHorizontalLine(),"span, growx 100, gapbottom 4, wrap");
        return detailsPanel;
    }
        
    /**
     * Loads values for the Overview Panel. If a given field is null or 
     * doesn't exist, the field is ignored.
     */
    private void addOverviewCategory(PropertiableFile propertiableFile, JPanel panel) {
        Icon icon = getIcon(propertiableFile);
        JPanel iconDock = createPanel();
        iconDock.add(new JLabel(icon));
        panel.add(iconDock, "aligny top, growy, gap 7, gaptop 5, dock west");
        panel.add(createLabelField(propertiableFile.getFileName()), "growx, span, wrap");
        panel.add(createLabel(I18n.tr("Size:")), "split 2");
        panel.add(createLabelField(FileInfoUtils.getFileSize(propertiableFile)), "growx, wrap");
        
        switch(propertiableFile.getCategory()) {
        case AUDIO:
            String time = FileInfoUtils.getLength(propertiableFile);
            if(time != null) {
                panel.add(createLabel(I18n.tr("Length:")), "split 2");
                panel.add(createLabelField(time), "growx, wrap");
            }
            String bitrate = propertiableFile.getPropertyString(FilePropertyKey.BITRATE);
            if(bitrate != null) {
                panel.add(createLabel(I18n.tr("Bitrate:")), "split 2");
                panel.add(createLabelField(bitrate + " kbps (" + FileInfoUtils.getQuality(propertiableFile) + ")"), "growx, wrap");
            }
            break;
        case VIDEO:
            String width = propertiableFile.getPropertyString(FilePropertyKey.WIDTH);
            String height = propertiableFile.getPropertyString(FilePropertyKey.HEIGHT);
            if(width != null && height != null) {
                panel.add(createLabel(I18n.tr("Dimensions:")), "split 2");
                panel.add(createLabelField(width + "px X " + height + "px"), "growx, wrap");
            }
            break;
        case IMAGE:
            panel.add(createLabel(I18n.tr("Date Created:")), "split 2");
            panel.add(createLabelField(FileInfoUtils.convertDate(propertiableFile)), "growx, wrap");
            break;
        case DOCUMENT:
            panel.add(createLabel(I18n.tr("Type:")), "split 2");
            panel.add(createLabelField(iconManager.get().getMIMEDescription(propertiableFile)), "growx, wrap");
            panel.add(createLabel(I18n.tr("Date Created:")), "split 2");
            panel.add(createLabelField(FileInfoUtils.convertDate(propertiableFile)), "growx, wrap");
            break;
        case PROGRAM:
            break;
        case OTHER:
            panel.add(createLabel(I18n.tr("Type:")), "split 2");
            panel.add(createLabelField(iconManager.get().getMIMEDescription(propertiableFile)), "growx, wrap");
            break;
        }
        panel.add(createLabel(I18n.tr("Hash:")), "split 2");
        String urn = propertiableFile.getUrn() == null ? "" : propertiableFile.getUrn().toString();
        panel.add(createLabelField(urn), "growx, wrap");
    }
    
    /**
     * Loads the values for the Details Panel. If the file being viewed is not
     * local, the fields are non-editable.
     */
    private void addEditableDetails(PropertiableFile propertiableFile, JPanel panel) {
        JScrollPane descriptionScrollPane = new JScrollPane(createEditableTextArea(propertiableFile.getPropertyString(FilePropertyKey.DESCRIPTION), FilePropertyKey.DESCRIPTION), 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        switch(propertiableFile.getCategory()) {
        case AUDIO:
            panel.add(createPlainLabel(I18n.tr("Title")), "grow, push 100");
            panel.add(createPlainLabel(I18n.tr("Artist")), "grow, span 3, push 50, wrap");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TITLE), FilePropertyKey.TITLE), "growx, push 100, gapright unrelated");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.AUTHOR), FilePropertyKey.AUTHOR), "growx, span 3, push 50, wrap");
            //second line
            panel.add(createPlainLabel(I18n.tr("Album")), "push 100");
            panel.add(createPlainLabel(I18n.tr("Genre")), "push 17");
            panel.add(createPlainLabel(I18n.tr("Year")), "push 17");
            panel.add(createPlainLabel(I18n.tr("Track")), "push 17, wrap");
            
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.ALBUM), FilePropertyKey.ALBUM), "growx, push 100, gapright unrelated");
            panel.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.GENRE), getGenres(propertiableFile), FilePropertyKey.GENRE), "growx, push 17, gapright unrelated");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.YEAR), FilePropertyKey.YEAR), "growx, push 17, gapright unrelated");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TRACK_NUMBER), FilePropertyKey.TRACK_NUMBER), "growx, push 17, wrap");

            //third line
            panel.add(createPlainLabel(I18n.tr("Description")), "span, wrap");

            panel.add(descriptionScrollPane, "span, growx, hmin 42");
            break;
        case VIDEO:
            panel.add(createPlainLabel(I18n.tr("Title")), "wrap");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TITLE), FilePropertyKey.TITLE), "span, growx, wrap");
            panel.add(createPlainLabel(I18n.tr("Genre")), "push 35");
            panel.add(createPlainLabel(I18n.tr("Rating")), "push 35");
            panel.add(createPlainLabel(I18n.tr("Year")), "push 30, wrap");
            panel.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.GENRE), getGenres(propertiableFile), FilePropertyKey.GENRE), "growx, push 35, gapright unrelated");
            panel.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.RATING), getRatings(propertiableFile), FilePropertyKey.RATING), "growx, push 35, gapright unrelated");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.YEAR), FilePropertyKey.YEAR), "grow, push 30, wrap");
            panel.add(createPlainLabel(I18n.tr("Description")), "wrap");
            panel.add(descriptionScrollPane, "span, growx, hmin 42");
            break;
        case IMAGE:
            panel.add(createPlainLabel(I18n.tr("Title")), "wrap");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TITLE), FilePropertyKey.TITLE), "growx, wrap");
            panel.add(createPlainLabel(I18n.tr("Description")), "wrap");
            panel.add(descriptionScrollPane, "growx, hmin 42");
            break;
        case DOCUMENT:
            panel.add(createPlainLabel(I18n.tr("Author")), "wrap");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.AUTHOR), FilePropertyKey.AUTHOR), "growx, wrap");
            panel.add(createPlainLabel(I18n.tr("Description")), "wrap");
            panel.add(descriptionScrollPane, "growx, hmin 42");
            break;
        case PROGRAM:
            panel.add(createPlainLabel(I18n.tr("Author")), "wrap");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.AUTHOR), FilePropertyKey.AUTHOR), "growx, span, wrap");
            panel.add(createPlainLabel(I18n.tr("Platform")), "growx 30");
            panel.add(createPlainLabel(I18n.tr("Company")), "growx 70, wrap");
            panel.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.PLATFORM), getPlatforms(propertiableFile), FilePropertyKey.PLATFORM), "growx 30, gapright 5");
            panel.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.COMPANY), FilePropertyKey.COMPANY), "growx 70");
            break;
        case OTHER:
            break;
        }
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(largeFont);
        return label;
    }
      
    /**
     * Returns the appropriate Icon for the file type.
     */
    private Icon getIcon(PropertiableFile propertiableFile) {
        switch(type){
        case LOCAL_FILE:
            switch (propertiableFile.getCategory()) {
            case IMAGE:
                return thumbnailManager.getThumbnailForFile(((LocalFileItem)propertiableFile).getFile());
            default:
                return categoryIconManager.getIcon(propertiableFile);
            }
        case DOWNLOADING_FILE:
        case REMOTE_FILE:
        default:
            return categoryIconManager.getIcon(propertiableFile);
        }
    }
    
    private JLabel createPlainLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallFont);
        return label;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallBoldFont);
        return label;
    }
    
    private JTextField createLabelField(String text) {
        JTextField field = new JTextField(text);
        field.setOpaque(false);
        field.setFont(smallFont);
        field.setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
        return field;
    }
    
    private JTextField createEditableTextField(String text, FilePropertyKey key) {
        JTextField field = new JTextField(text);
        if(type != FileInfoType.LOCAL_FILE)
            field.setEditable(false);
        else
            changedProps.put(key, field);
        field.setFont(mediumFont);
        return field;
    }
    
    private JTextArea createEditableTextArea(String text, FilePropertyKey key) {
        JTextArea area = new JTextArea(text);
        if(type != FileInfoType.LOCAL_FILE) {
            area.setEditable(false);
            area.setBackground(UIManager.getLookAndFeel().getDefaults().getColor("TextField.disabledBackground"));
        } else {
            changedProps.put(key, area);
        }
        area.setFont(mediumFont);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }
    
    /**
     * Blacklists the given ipAddress.
     */
    private void addToFilterList(String ipAddress) {
        List<String> blackList = new ArrayList<String>(Arrays.asList(FilterSettings.BLACK_LISTED_IP_ADDRESSES.get()));
        blackList.add(ipAddress);
        FilterSettings.USE_NETWORK_FILTER.setValue(true);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(blackList.toArray(new String[blackList.size()]));
        spamManager.reloadIPFilter();
    }
    
    /**
     * If this combo box is editable, sets the model and selects the box, otherwise
     * returns a non-editable JTextField.
     */
    private JComponent createEditableComboBox(String selection, List<String> model, FilePropertyKey key) {
        if(type != FileInfoType.LOCAL_FILE) {
            return createEditableTextField(selection, null);
        } else {
            JComboBox comboBox = new JComboBox();
            changedProps.put(key, comboBox);
            setupComboBox(comboBox, selection, model);
            return comboBox;
        }
    }
    
    /**
     * Loads a combo box and selects the currently selected item.
     */
    private void setupComboBox(JComboBox comboBox, String current, List<String> possibles) {
        if(current == null) {
            current = "";
        }
        
        // If any are listed, current is non-empty, and possibles doesn't contain, add it in.
        if(!possibles.contains(current) && !current.equals("") && possibles.size() > 0) {
            possibles = new ArrayList<String>(possibles);            
            possibles.add(0, current);
            possibles = Collections.unmodifiableList(possibles);
        }
        
        ComboBoxModel model = new CollectionBackedComboBoxModel(possibles);
        comboBox.setModel(model);
        comboBox.setSelectedItem(current);
    }
    
    /**
     * Returns a list of values for the OS combobox.
     */
    private List<String> getPlatforms(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case PROGRAM:
            return propertyDictionary.getApplicationPlatforms();
        default:
            return Collections.emptyList();
        }
    }

    /**
     * Returns a list of values for the rating comboBox
     */
    private List<String> getRatings(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case VIDEO:
            return propertyDictionary.getVideoRatings();
        default:
            return Collections.emptyList();
        }
    }

    /**
     * Returns a List of values to populate the genre combobox with.
     */
    private List<String> getGenres(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case AUDIO:
            return propertyDictionary.getAudioGenres();
        case VIDEO:
            return propertyDictionary.getVideoGenres();
        default:
            return Collections.emptyList();
        }
    }
    
    private List<Friend> getSharedWithList(LocalFileItem fileItem) {
//        List<Friend> sharedWith = new ArrayList<Friend>();
//        
//        if(shareListManager.getGnutellaShareList().contains(fileItem.getFile()))
//            sharedWith.add(SharingTarget.GNUTELLA_SHARE.getFriend());
//        
//        for(Friend friend : friendManager.getKnownFriends()) {  
//            boolean isShared = shareListManager.getOrCreateFriendShareList(friend).contains(fileItem.getFile());
//            if (isShared) {
//                sharedWith.add(friend);
//            }
//        }
//        return sharedWith;
        throw new NotImplementedException();
    }
    
    private JPanel createPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }
    
    private class ReadOnlyTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
    
    /**
     * Listens for changes to the download status and updates the dialog.
     */
    private class DownloadStatus implements PropertyChangeListener {
        private final JLabel label;
        
        public DownloadStatus(JLabel label) {
            this.label = label;
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    label.setText(tr("{0}% complete", ((DownloadItem)propertiableFile).getPercentComplete()));
                } 
            });
        }
    }
}
