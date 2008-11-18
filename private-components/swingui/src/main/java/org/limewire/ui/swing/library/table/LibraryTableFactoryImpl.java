package org.limewire.ui.swing.library.table;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.dnd.LocalFileTransferable;
import org.limewire.ui.swing.dnd.RemoteFileTransferable;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactory;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.sharing.SharingCheckBoxRendererEditor;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.table.menu.FriendLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryTableFactoryImpl implements LibraryTableFactory,
        RegisteringEventListener<RosterEvent> {

    private IconManager iconManager;

    private LibraryManager libraryManager;

    private ShareListManager shareListManager;

    private AudioPlayer player;

    // only accessed on EDT
    private List<SharingTarget> friendList = new ArrayList<SharingTarget>();

    private DownloadListManager downloadListManager;

    private MagnetLinkFactory magnetLinkFactory;

    private CategoryIconManager categoryIconManager;

    private PropertiesFactory<LocalFileItem> localItemPropFactory;

    private PropertiesFactory<RemoteFileItem> remoteItemPropFactory;
    
    private LibraryImageSubPanelFactory subPanelFactory;
    
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    @Inject
    public LibraryTableFactoryImpl(CategoryIconManager categoryIconManager, 
            IconManager iconManager,
            LibraryManager libraryManager, 
            ShareListManager shareListManager, 
            AudioPlayer player,
            DownloadListManager downloadListManager, 
            MagnetLinkFactory magnetLinkFactory,
            PropertiesFactory<LocalFileItem> localItemPropFactory,
            PropertiesFactory<RemoteFileItem> remoteItemPropFactory,
            LibraryImageSubPanelFactory factory, 
            SaveLocationExceptionHandler saveLocationExceptionHandler) {
        this.categoryIconManager = categoryIconManager;
        this.iconManager = iconManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.player = player;
        this.downloadListManager = downloadListManager;
        this.magnetLinkFactory = magnetLinkFactory;
        this.localItemPropFactory = localItemPropFactory;
        this.remoteItemPropFactory = remoteItemPropFactory;
        this.subPanelFactory = factory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        EventAnnotationProcessor.subscribe(this);
    }

    @Override
    public LibraryImagePanel createImagePanel(EventList<LocalFileItem> eventList,
            JScrollPane scrollPane, LibrarySharePanel sharePanel) {
        ImageLibraryPopupParams params = new ImageLibraryPopupParams(libraryManager,
                shareListManager, magnetLinkFactory, friendList, localItemPropFactory);
        return new LibraryImagePanel(I18n.tr(Category.IMAGE.name()), params, eventList,
                libraryManager.getLibraryManagedList(),
                categoryIconManager.getIcon(Category.IMAGE), scrollPane, subPanelFactory,
                sharePanel, null);
    }
    
    @Override
    public LibraryImagePanel createSharingImagePanel(EventList<LocalFileItem> eventList,
            JScrollPane scrollPane, LocalFileList currentFriendList) {
        ImageLibraryPopupParams params = new ImageLibraryPopupParams(libraryManager,
                shareListManager, magnetLinkFactory, friendList, localItemPropFactory);
        return new LibraryImagePanel(I18n.tr(Category.IMAGE.name()), params, eventList,
                libraryManager.getLibraryManagedList(),
                categoryIconManager.getIcon(Category.IMAGE), scrollPane,
                subPanelFactory,
                null, currentFriendList);
    }

    /**
     * 
     * @param friend null for MyLibrary
     * @return
     */
    public <T extends FileItem> LibraryTable<T> createTable(Category category,
            EventList<T> eventList, Friend friend) {

        final LibraryTable<T> libTable;

        switch (category) {
        case AUDIO:
            if (friend != null) {
                libTable = new LibraryTable<T>(eventList, new RemoteAudioTableFormat<T>(), saveLocationExceptionHandler);
                libTable.getColumnModel().getColumn(RemoteAudioTableFormat.LENGTH_COL).setCellRenderer(new TimeRenderer());
                libTable.getColumnModel().getColumn(RemoteAudioTableFormat.SIZE_COL).setCellRenderer(new FileSizeRenderer());
            } else {
                libTable = new AudioLibraryTable<T>(eventList, player, saveLocationExceptionHandler);
            }
            break;
        case VIDEO:
            libTable = new VideoLibraryTable<T>(eventList, saveLocationExceptionHandler);
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(eventList, new DocumentTableFormat<T>(), saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(DocumentTableFormat.NAME_COL).setCellRenderer(
                    new IconLabelRenderer(iconManager));
            libTable.getColumnModel().getColumn(DocumentTableFormat.SIZE_COL).setCellRenderer(
                    new FileSizeRenderer());
            break;
        case IMAGE:
            libTable = new LibraryTable<T>(eventList, new ImageTableFormat<T>(), saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(ImageTableFormat.SIZE_COL).setCellRenderer(new FileSizeRenderer());
            break;
        case OTHER:
            libTable = new LibraryTable<T>(eventList, new OtherTableFormat<T>(), saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(OtherTableFormat.NAME_COL).setCellRenderer(
                    new IconLabelRenderer(iconManager));
            libTable.getColumnModel().getColumn(OtherTableFormat.SIZE_COL).setCellRenderer(
                    new FileSizeRenderer());
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(eventList, new ProgramTableFormat<T>(), saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(ProgramTableFormat.NAME_COL).setCellRenderer(
                    new IconLabelRenderer(iconManager));
            libTable.getColumnModel().getColumn(ProgramTableFormat.SIZE_COL).setCellRenderer(
                    new FileSizeRenderer());
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }

        if (friend != null) {
            libTable.setTransferHandler(new FriendLibraryTransferHandler(libTable, friend));
            libTable.setPopupHandler(new FriendLibraryPopupHandler(
                    castToRemoteLibraryTable(libTable), downloadListManager, magnetLinkFactory,
                    remoteItemPropFactory, saveLocationExceptionHandler));
        } else {// Local
            libTable.setTransferHandler(new MyLibraryTransferHandler(libTable));
            libTable.setPopupHandler(new MyLibraryPopupHandler(castToLocalLibraryTable(libTable),
                    category, libraryManager, shareListManager, magnetLinkFactory, friendList,
                    localItemPropFactory));
        }

        libTable.setDropMode(DropMode.ON);

        return libTable;

    }    

    @Override
    public <T extends FileItem> LibraryTable<T> createSharingTable(Category category, EventList<T> eventList, LocalFileList friendFileList) {
        final LibraryTable<T> libTable;
        
        switch (category) {
        case AUDIO:
            libTable = new AudioLibraryTable<T>(eventList, player, saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(AudioTableFormat.ACTION_COL).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(AudioTableFormat.ACTION_COL).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(AudioTableFormat.ACTION_COL, 0);
            break;
        case VIDEO:
            libTable = new VideoLibraryTable<T>(eventList, saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(VideoTableFormat.ACTION_COL).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(VideoTableFormat.ACTION_COL).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(VideoTableFormat.ACTION_COL, 0);
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(eventList, new DocumentTableFormat<T>(), saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(DocumentTableFormat.NAME_COL).setCellRenderer(new IconLabelRenderer(iconManager));
            libTable.getColumnModel().getColumn(DocumentTableFormat.SIZE_COL).setCellRenderer(new FileSizeRenderer());
            libTable.getColumnModel().getColumn(DocumentTableFormat.ACTION_COL).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(DocumentTableFormat.ACTION_COL).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(DocumentTableFormat.ACTION_COL, 0);
            break;
        case IMAGE:
            libTable = new LibraryTable<T>(eventList, new ImageTableFormat<T>(), saveLocationExceptionHandler);
            break;
        case OTHER:
            libTable = new LibraryTable<T>(eventList, new OtherTableFormat<T>(), saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(OtherTableFormat.NAME_COL).setCellRenderer(new IconLabelRenderer(iconManager));
            libTable.getColumnModel().getColumn(OtherTableFormat.SIZE_COL).setCellRenderer(new FileSizeRenderer());
            libTable.getColumnModel().getColumn(OtherTableFormat.ACTION_COL).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(OtherTableFormat.ACTION_COL).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(OtherTableFormat.ACTION_COL, 0);
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(eventList, new ProgramTableFormat<T>(), saveLocationExceptionHandler);
            libTable.getColumnModel().getColumn(ProgramTableFormat.NAME_COL).setCellRenderer(new IconLabelRenderer(iconManager));
            libTable.getColumnModel().getColumn(ProgramTableFormat.SIZE_COL).setCellRenderer(new FileSizeRenderer());
            libTable.getColumnModel().getColumn(ProgramTableFormat.ACTION_COL).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(ProgramTableFormat.ACTION_COL).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(ProgramTableFormat.ACTION_COL, 0);
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        
        libTable.setTransferHandler(new MyLibraryTransferHandler(libTable));
        libTable.setPopupHandler(new MyLibraryPopupHandler(castToLocalLibraryTable(libTable), category, libraryManager, shareListManager, 
                    magnetLinkFactory, friendList, localItemPropFactory));
        
        libTable.setDropMode(DropMode.ON);
        
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

    private class MyLibraryTransferHandler extends TransferHandler {

        private LibraryTable table;

        public MyLibraryTransferHandler(LibraryTable table) {
            this.table = table;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public int getSourceActions(JComponent comp) {
            return COPY;
        }

        @SuppressWarnings("unchecked")
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            // Get the string that is being dropped.
            Transferable t = info.getTransferable();
            final List<File> fileList;
            try {
                fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            } catch (Exception e) {
                return false;
            }

            for (File file : fileList) {
                if(file.isDirectory()) {
                    libraryManager.getLibraryManagedList().addFolder(file);
                } else {
                    libraryManager.getLibraryManagedList().addFile(file);
                }
            }
            return true;
        }

        @Override
        public Transferable createTransferable(JComponent comp) {
            int indices[] = table.getSelectedRows();
            File[] files = new File[indices.length];
            for (int i = 0; i < indices.length; i++) {
                files[i] = ((LocalFileItem) ((LibraryTableModel) table.getModel())
                        .getFileItem(indices[i])).getFile();
            }
            return new LocalFileTransferable(files);
        }
    }

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
            return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            Transferable t = info.getTransferable();

            final List<File> fileList;
            try {
                fileList = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
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

    @Inject
    @Override
    public void register(ListenerSupport<RosterEvent> listenerSupport) {
        listenerSupport.addListener(this);
    }

    @Override
    public void handleEvent(RosterEvent event) {
        if (event.getType().equals(User.EventType.USER_ADDED)) {
            addFriend(event.getSource());
        } else if (event.getType().equals(User.EventType.USER_REMOVED)) {
            removeFriend(event.getSource());
        }
    }

    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                friendList.clear();
            }
        });
    }

    private void removeFriend(final Friend friend) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                friendList.remove(new SharingTarget(friend));
            }
        });
    }

    private void addFriend(final Friend friend) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                friendList.add(new SharingTarget(friend));
            }
        });
    }
}
