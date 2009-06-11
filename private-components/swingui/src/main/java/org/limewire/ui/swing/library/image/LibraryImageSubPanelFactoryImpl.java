package org.limewire.ui.swing.library.image;

import java.awt.Dimension;
import java.io.File;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.LibraryListSourceChanger;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryImageSubPanelFactoryImpl implements LibraryImageSubPanelFactory {

    private ThumbnailManager thumbnailManager;
    
    private Dimension subPanelDimension = new Dimension(ThumbnailManager.WIDTH,28);

    private LibraryManager libraryManager;
    
//    private final ShareTableRendererEditorFactory shareTableRendererEditorFactory;

    private final SharedFileListManager shareListManager;
    private final ComboBoxDecorator comboDecorator;
//    private final MyLibraryPopupMenuFactory libraryPopupFactory;
    private final Provider<LibraryImageFolderComboBox> libraryImageComboBox;
    
    @Inject
    public LibraryImageSubPanelFactoryImpl(ThumbnailManager thumbnailManager, LibraryManager libraryManager, 
            SharedFileListManager shareListManager, 
            ComboBoxDecorator comboDecorator, PlaylistManager playlistManager,
            FileInfoDialogFactory fileInfoFactory, 
            Provider<LibraryImageFolderComboBox> libraryImageComboBox) {
        this.thumbnailManager = thumbnailManager;
        this.libraryManager = libraryManager;
//        this.shareTableRendererEditorFactory = shareTableRendererEditorFactory;
        this.shareListManager = shareListManager;
        this.comboDecorator = comboDecorator;
//        this.libraryPopupFactory = libraryPopupFactory;
        this.libraryImageComboBox = libraryImageComboBox;
    }
    
    @Override
    public LibraryImageSubPanel createMyLibraryImageSubPanel(File parentFolder,
            EventList<LocalFileItem> eventList, LocalFileList fileList,
//            ShareWidget<File> shareWidget, 
            LibraryListSourceChanger listChanger) {

        LibraryImageFolderComboBox comboBox = libraryImageComboBox.get();
        comboDecorator.decorateLinkComboBox(comboBox);
        
        LibraryImageSubPanel panel = new LibraryImageSubPanel(parentFolder, eventList, fileList, comboBox);
//        panel.setPopupHandler(new MyImageLibraryPopupHandler(panel, libraryPopupFactory.createMyLibraryPopupMenu(Category.IMAGE)));
        comboBox.setSelectAllable(panel);

//        ImageList list = panel.getImageList();
//        list.setImageCellRenderer(enableMyLibraryRenderer(list));
//        panel.setImageEditor(enableMyLibraryEditor(panel));
//        TransferHandler transferHandler = new MyLibraryTransferHandler(getSelectionModel(list), libraryManager.getLibraryManagedList(), shareListManager, listChanger);
//        list.setTransferHandler(transferHandler);
//        panel.setTransferHandler(transferHandler);
        return panel;
    }
    
//    @SuppressWarnings("unchecked")
//    private EventSelectionModel<LocalFileItem> getSelectionModel(ImageList list){
//        return (EventSelectionModel<LocalFileItem>) list.getSelectionModel();
//    }
    
//    private ImageCellRenderer enableMyLibraryRenderer(ImageList imageList) {
//        ImageCellRenderer renderer = new LibraryImageCellRenderer(imageList.getFixedCellWidth(), imageList.getFixedCellHeight() - 2, thumbnailManager);
//        renderer.setOpaque(false);
//        JComponent buttonRenderer = shareTableRendererEditorFactory.createShareTableRendererEditor(null);
//        buttonRenderer.setOpaque(false);
//        buttonRenderer.setPreferredSize(subPanelDimension);
//        buttonRenderer.setSize(subPanelDimension);
//        renderer.setButtonComponent(buttonRenderer);
//        
//        return renderer;
//    }
//    
//    private TableRendererEditor enableMyLibraryEditor(LibraryImageSubPanel parent){
//        ShareAction action = new ShareAction(I18n.tr("Sharing"), parent);
//        ShareTableRendererEditor shareEditor = shareTableRendererEditorFactory.createShareTableRendererEditor(action);
//        action.setEditor(shareEditor);
//        shareEditor.setPreferredSize(subPanelDimension);
//        shareEditor.setSize(subPanelDimension);
//        shareEditor.setOpaque(false);
//        shareEditor.setVisible(false);
//                
//        return shareEditor;
//    }
//
//    private static class ShareAction extends AbstractAction {
//        private ShareTableRendererEditor shareEditor;
////        private ShareWidget<File> shareWidget;
//        private LibraryImageSubPanel parent;
//
//        public ShareAction(String text, LibraryImageSubPanel parent){
//            super(text);
////            this.shareWidget = shareWidget;
//            this.parent = parent;
//        }
//        
//        public void setEditor(ShareTableRendererEditor editor) {
//            this.shareEditor = editor;
//        }
//
//        @Override
//        public void actionPerformed(ActionEvent e) {
////            shareWidget.setShareable(shareEditor.getLocalFileItem().getFile());
//          
//            JComponent source = (JComponent)e.getSource();
//            Point convertedLocation = 
//                SwingUtilities.convertPoint(shareEditor, source.getLocation(), parent.getImageList());
//            selectImage(convertedLocation);
////            shareWidget.show(source);
//            shareEditor.cancelCellEditing();
//        }
//        
//        private void selectImage(Point point) {
//            int index = parent.getImageList().locationToIndex(point);
//            if(index > -1)
//                parent.getImageList().setSelectedIndex(index);
//        }
//    }
}
