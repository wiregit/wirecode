package org.limewire.ui.swing.library.table;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.dnd.RemoteFileTransferable;
import org.limewire.ui.swing.library.LibraryListSourceChanger;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactory;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.playlist.PlaylistLibraryTable;
import org.limewire.ui.swing.library.playlist.PlaylistPopupHandler;
import org.limewire.ui.swing.library.playlist.PlaylistTableFormat;
import org.limewire.ui.swing.library.playlist.PlaylistTransferHandler;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.table.menu.FriendLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactory;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidgetFactory;
import org.limewire.ui.swing.search.resultpanel.classic.FromTableCellRenderer;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.ui.swing.util.EventListJXTableSorting;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryTableFactoryImpl implements LibraryTableFactory {

    private LibraryManager libraryManager;

    private ShareListManager shareListManager;

    private AudioPlayer player;

    private DownloadListManager downloadListManager;

    private MagnetLinkFactory magnetLinkFactory;

    private PropertiesFactory<LocalFileItem> localItemPropFactory;

    private PropertiesFactory<RemoteFileItem> remoteItemPropFactory;
    
    private PropertiesFactory<DownloadItem> downloadItemPropFactory;
    
    private LibraryImageSubPanelFactory subPanelFactory;
    
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    
    private final SearchResultFromWidgetFactory fromWidgetFactory;
    
    private final TimeRenderer timeRenderer = new TimeRenderer();
    private final FileSizeRenderer fileSizeRenderer = new FileSizeRenderer();
    private final CalendarRenderer calendarRenderer = new CalendarRenderer();
    private final QualityRenderer qualityRenderer = new QualityRenderer();
    private final IconLabelRenderer iconLabelRenderer;
    private final NameRenderer nameRenderer = new NameRenderer();
    private final IconManager iconManager;
    private final ShareTableRendererEditorFactory shareTableRendererEditorFactory;
    private final GhostDragGlassPane ghostPane;
    private final SharingActionFactory sharingActionFactory;
    private final XMPPService xmppService;
    private final LibraryNavigator libraryNavigator;
    private final PlaylistManager playlistManager;

    @Inject
    public LibraryTableFactoryImpl(IconManager iconManager,
            LibraryManager libraryManager, 
            ShareListManager shareListManager, 
            AudioPlayer player,
            DownloadListManager downloadListManager, 
            MagnetLinkFactory magnetLinkFactory,
            PropertiesFactory<LocalFileItem> localItemPropFactory,
            PropertiesFactory<RemoteFileItem> remoteItemPropFactory,
            PropertiesFactory<DownloadItem> downloadItemPropFactory,
            LibraryImageSubPanelFactory factory, 
            SaveLocationExceptionHandler saveLocationExceptionHandler,
            ShareTableRendererEditorFactory shareTableRendererEditorFactory, 
            ShareWidgetFactory shareFactory,
            GhostDragGlassPane ghostPane, 
            CategoryIconManager categoryIconManager,
            SearchResultFromWidgetFactory fromWidgetfactory,
            SharingActionFactory sharingActionFactory,
            XMPPService xmppService,
            LibraryNavigator libraryNavigator,
            PlaylistManager playlistManager) {
        this.iconManager = iconManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.player = player;
        this.downloadListManager = downloadListManager;
        this.magnetLinkFactory = magnetLinkFactory;
        this.localItemPropFactory = localItemPropFactory;
        this.remoteItemPropFactory = remoteItemPropFactory;
        this.downloadItemPropFactory = downloadItemPropFactory;
        this.subPanelFactory = factory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.ghostPane = ghostPane;
        this.fromWidgetFactory = fromWidgetfactory;
        this.sharingActionFactory = sharingActionFactory;
        this.xmppService = xmppService;
        this.libraryNavigator = libraryNavigator;
        this.playlistManager = playlistManager;
        
        this.shareTableRendererEditorFactory = shareTableRendererEditorFactory;
        iconLabelRenderer = new IconLabelRenderer(iconManager, categoryIconManager, downloadListManager, libraryManager);
    }


    /**
     * Creates a table for MyLibrary
     */
    public <T extends LocalFileItem> LibraryTable<T> createMyTable(Category category,
            EventList<T> eventList, LibraryListSourceChanger listChanger) {

        final LibraryTable<T> libTable;
        SortedList<T> sortedList = new SortedList<T>(eventList);

        switch (category) {
        case AUDIO:
            libTable = new AudioLibraryTable<T>(sortedList, player, saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(AudioTableFormat.LENGTH_INDEX).setHeaderRenderer(new TableCellHeaderRenderer(JLabel.TRAILING));
            libTable.getColumnModel().getColumn(AudioTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer);
            break;
        case VIDEO:
            libTable = new LibraryTable<T>(sortedList,  new VideoTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(VideoTableFormat.LENGTH_INDEX).setHeaderRenderer(new TableCellHeaderRenderer(JLabel.TRAILING));
            libTable.getColumnModel().getColumn(VideoTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer);
            libTable.getColumnModel().getColumn(VideoTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
            libTable.getColumnModel().getColumn(VideoTableFormat.NAME_INDEX).setCellRenderer(nameRenderer);
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(sortedList, new DocumentTableFormat<T>(iconManager), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(DocumentTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(DocumentTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
            break;
        case OTHER:
            libTable = new LibraryTable<T>(sortedList, new OtherTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(OtherTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(OtherTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(sortedList, new ProgramTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(ProgramTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(ProgramTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }

        libTable.setTransferHandler(new MyLibraryTransferHandler(getSelectionModel(libTable), libraryManager.getLibraryManagedList(), shareListManager, listChanger));
        libTable.setPopupHandler(new MyLibraryPopupHandler(castToLocalLibraryTable(libTable),
                category, libraryManager, shareListManager, magnetLinkFactory,
                localItemPropFactory, sharingActionFactory, xmppService, 
                libraryNavigator, playlistManager));
        
        try {
            libTable.getDropTarget().addDropTargetListener(new GhostDropTargetListener(libTable, ghostPane, listChanger));
        } catch (TooManyListenersException ingoreException) {
        }

        EventListJXTableSorting.install(libTable, sortedList, libTable.getTableFormat());
        libTable.setDropMode(DropMode.ON);

        return libTable;
    }    

	/**
	 * Creates Image Library for My Library which displays thumbnails.
	 */
    @Override
    public LibraryImagePanel createMyImagePanel(EventList<LocalFileItem> eventList,
            JScrollPane scrollPane, ShareWidget<File> sharePanel,
            LibraryListSourceChanger listChanger) {
        LibraryImagePanel imagePanel = new LibraryImagePanel(I18n.tr(Category.IMAGE.name()), eventList,
                libraryManager.getLibraryManagedList(), scrollPane, subPanelFactory,
                sharePanel, listChanger);

        TransferHandler noDragTransferHandler = new MyLibraryTransferHandler(null, libraryManager
                .getLibraryManagedList(), shareListManager, listChanger) {
            @Override
            public int getSourceActions(JComponent comp) {
                return NONE;
            }
        };
        
        imagePanel.setTransferHandler(noDragTransferHandler);
        try {
            imagePanel.getDropTarget().addDropTargetListener(new GhostDropTargetListener(imagePanel, ghostPane, listChanger));
        } catch (TooManyListenersException ingoreException) {
        }
        return imagePanel;
    }    
    
    /**
     * Creates a table when viewing a Friend's Library or performing a BrowseHost
     */
    public <T extends RemoteFileItem> LibraryTable<T> createFriendTable(Category category, EventList<T> eventList, Friend friend) {

        LibraryTable<T> libTable;
        SortedList<T> sortedList = GlazedListsFactory.sortedList(eventList);

        switch (category) {
        case AUDIO:
            if(friend != null) {
                libTable = new LibraryTable<T>(sortedList, new RemoteAudioTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(RemoteAudioTableFormat.LENGTH_INDEX).setHeaderRenderer(new TableCellHeaderRenderer(JLabel.TRAILING));
                libTable.getColumnModel().getColumn(RemoteAudioTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(RemoteAudioTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer);
                libTable.getColumnModel().getColumn(RemoteAudioTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(RemoteAudioTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer);
            } else {
                libTable = new LibraryTable<T>(sortedList, new AllFriendAudioTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(AllFriendAudioTableFormat.LENGTH_INDEX).setHeaderRenderer(new TableCellHeaderRenderer(JLabel.TRAILING));
                libTable.getColumnModel().getColumn(AllFriendAudioTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(AllFriendAudioTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer);
                libTable.getColumnModel().getColumn(AllFriendAudioTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(AllFriendAudioTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer);
                libTable.getColumnModel().getColumn(AllFriendAudioTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(true)));
                libTable.getColumnModel().getColumn(AllFriendAudioTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(true)));
            }
            break;
        case VIDEO:
            if(friend != null) {
                libTable = new LibraryTable<T>(sortedList, new RemoteVideoTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(RemoteVideoTableFormat.LENGTH_INDEX).setHeaderRenderer(new TableCellHeaderRenderer(JLabel.TRAILING));
                libTable.getColumnModel().getColumn(RemoteVideoTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(RemoteVideoTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer);
                libTable.getColumnModel().getColumn(RemoteVideoTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(RemoteVideoTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer);
            } else {
                libTable = new LibraryTable<T>(sortedList, new AllFriendVideoTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(AllFriendVideoTableFormat.LENGTH_INDEX).setHeaderRenderer(new TableCellHeaderRenderer(JLabel.TRAILING));
                libTable.getColumnModel().getColumn(AllFriendVideoTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(AllFriendVideoTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer);
                libTable.getColumnModel().getColumn(AllFriendVideoTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(AllFriendVideoTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer);
                libTable.getColumnModel().getColumn(AllFriendVideoTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(true)));
                libTable.getColumnModel().getColumn(AllFriendVideoTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(true)));
            }
            break;
        case DOCUMENT:
            if(friend != null) {
                libTable = new LibraryTable<T>(sortedList, new RemoteDocumentTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.CREATED_INDEX).setCellRenderer(calendarRenderer);
            } else {
                libTable = new LibraryTable<T>(sortedList, new AllFriendDocumentTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(AllFriendDocumentTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(AllFriendDocumentTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(AllFriendDocumentTableFormat.CREATED_INDEX).setCellRenderer(calendarRenderer);
                libTable.getColumnModel().getColumn(AllFriendDocumentTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(true)));
                libTable.getColumnModel().getColumn(AllFriendDocumentTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(true)));
            }
            break;
        case IMAGE:
            if(friend != null) {
                libTable = new LibraryTable<T>(sortedList, new RemoteImageTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(RemoteImageTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(RemoteImageTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(RemoteImageTableFormat.CREATED_INDEX).setCellRenderer(calendarRenderer);
            } else {
                libTable = new LibraryTable<T>(sortedList, new AllFriendImageTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(AllFriendImageTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(AllFriendImageTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(AllFriendImageTableFormat.CREATED_INDEX).setCellRenderer(calendarRenderer);
                libTable.getColumnModel().getColumn(AllFriendImageTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(true)));
                libTable.getColumnModel().getColumn(AllFriendImageTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(true)));
            }
            break;
        case OTHER:
            if(friend != null) {
                libTable = new LibraryTable<T>(sortedList, new RemoteOtherTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(RemoteOtherTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(RemoteOtherTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
            } else {
                libTable = new LibraryTable<T>(sortedList, new AllFriendOtherTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(AllFriendOtherTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(AllFriendOtherTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(AllFriendOtherTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(true)));
                libTable.getColumnModel().getColumn(AllFriendOtherTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(true)));
            }
            break;
        case PROGRAM:
            if(friend != null) {
                libTable = new LibraryTable<T>(sortedList, new RemoteProgramTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(RemoteProgramTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(RemoteProgramTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
            } else {
                libTable = new LibraryTable<T>(sortedList, new AllFriendProgramTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
                libTable.getColumnModel().getColumn(AllFriendProgramTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
                libTable.getColumnModel().getColumn(AllFriendProgramTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer);
                libTable.getColumnModel().getColumn(AllFriendProgramTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(true)));
                libTable.getColumnModel().getColumn(AllFriendProgramTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(true)));
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }

        if(friend != null && !friend.isAnonymous()) {
            libTable.setTransferHandler(new FriendLibraryTransferHandler(libTable, friend));
            try {
                libTable.getDropTarget().addDropTargetListener(new GhostDropTargetListener(libTable,ghostPane, friend));
            } catch (TooManyListenersException ignoreException) {            
            }     
        }
        libTable.setPopupHandler(new FriendLibraryPopupHandler(
                castToRemoteLibraryTable(libTable), downloadListManager, remoteItemPropFactory, saveLocationExceptionHandler, downloadItemPropFactory, libraryManager));

        EventListJXTableSorting.install(libTable, sortedList, libTable.getTableFormat());
        libTable.setDropMode(DropMode.ON);

        return libTable;

    }    

    /**
     * Creates a table to display a playlist.
     */
    @Override
    public <T extends LocalFileItem> LibraryTable<T> createPlaylistTable(
            Playlist playlist, EventList<T> eventList) {
        // Create sorted list.
        SortedList<T> sortedList = GlazedListsFactory.sortedList(eventList);

        // Create table.
        LibraryTable<T> libTable = new PlaylistLibraryTable<T>(playlist, sortedList, 
                new PlaylistTableFormat<T>(), player,
                saveLocationExceptionHandler, shareTableRendererEditorFactory);
        
        // Install popup menu handler.
        libTable.setPopupHandler(new PlaylistPopupHandler(libTable, playlist,
                libraryNavigator, localItemPropFactory));

        // Install transfer handler to reorder playlist items.
        libTable.setTransferHandler(new PlaylistTransferHandler(playlist));
        
        // Possible drag-and-drop upgrades:
        // - Start drag to category buttons to remove file from playlist
        // - Use GhostDropTargetListener to display image on drag operation
        // - Accept drop from OS file explorer to add file to library/playlist
        
        // Install sort support.
        EventListJXTableSorting.install(libTable, sortedList, libTable.getTableFormat());
        
        return libTable;
    }
    
    @SuppressWarnings( { "unchecked", "cast" })
    private LibraryTable<RemoteFileItem> castToRemoteLibraryTable(LibraryTable table) {
        return (LibraryTable<RemoteFileItem>) table;
    }

    @SuppressWarnings( { "unchecked", "cast" })
    private LibraryTable<LocalFileItem> castToLocalLibraryTable(LibraryTable table) {
        return (LibraryTable<LocalFileItem>) table;
    }
    
    @SuppressWarnings("unchecked")
    private EventSelectionModel<LocalFileItem> getSelectionModel(LibraryTable table){
        return (EventSelectionModel<LocalFileItem>) table.getSelectionModel();
    }

    /**
     * Drops with this handler will add the file to the ManagedLibrary and share
     * with this friend
     */
    private class FriendLibraryTransferHandler extends TransferHandler {

        private LibraryTable table;

        private Friend friend;

        public FriendLibraryTransferHandler(LibraryTable table, Friend friend) {
            this.table = table;
            this.friend = friend;
        }

        @Override
        public int getSourceActions(JComponent comp) {
            return COPY;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return DNDUtils.containsFileFlavors(info);
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            Transferable t = info.getTransferable();

            final List<File> fileList;
            try {
                fileList = Arrays.asList(DNDUtils.getFiles(t));
            } catch (Exception e) {
                return false;
            }
            for (File file : fileList) {
                if (file.isDirectory()) {
                    shareListManager.getFriendShareList(friend).addFolder(file);
                } else {
                    shareListManager.getFriendShareList(friend).addFile(file);
                }
            }
            return true;
        }

        @Override
        public Transferable createTransferable(JComponent comp) {
            int indices[] = table.getSelectedRows();
            List<RemoteFileItem> files = new ArrayList<RemoteFileItem>();
            for (int i = 0; i < indices.length; i++) {
                files.add((RemoteFileItem) ((LibraryTableModel) table.getModel())
                        .getFileItem(indices[i]));
            }
            return new RemoteFileTransferable(files);
        }
    }
}
