package org.limewire.ui.swing.library.image;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.sharing.FileShareModel;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.ShareTableRendererEditor;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryImageSubPanelFactoryImpl implements LibraryImageSubPanelFactory {

    private ThumbnailManager thumbnailManager;
    
    private Dimension subPanelDimension = new Dimension(ThumbnailManager.WIDTH,28);

    private LibraryManager libraryManager;
    
    @Inject
    public LibraryImageSubPanelFactoryImpl(ThumbnailManager thumbnailManager, LibraryManager libraryManager) {
        this.thumbnailManager = thumbnailManager;
        this.libraryManager = libraryManager;
    }
    
    @Override
    public LibraryImageSubPanel createMyLibraryImageSubPanel(String name,
            EventList<LocalFileItem> eventList, LocalFileList fileList,
            ImageLibraryPopupParams params, LibrarySharePanel sharePanel) {

        LibraryImageSubPanel panel = new LibraryImageSubPanel(name, eventList, fileList, params);
        ImageList list = panel.getImageList();
        list.setImageCellRenderer(enableMyLibraryRenderer(list));
        panel.setImageEditor(enableMyLibraryEditor(sharePanel, panel));
        TransferHandler transferHandler = new MyLibraryTransferHandler(getSelectionModel(list), libraryManager.getLibraryManagedList());
        list.setTransferHandler(transferHandler);
        panel.setTransferHandler(transferHandler);
        return panel;
    }

    @Override
    public LibraryImageSubPanel createSharingLibraryImageSubPanel(String name,
            EventList<LocalFileItem> eventList, LocalFileList fileList,
            ImageLibraryPopupParams params, LocalFileList currentFriendFileList) {
        LibraryImageSubPanel panel = new LibraryImageSubPanel(name, eventList, fileList, params);
        ImageList list = panel.getImageList();
        list.setImageCellRenderer(enableSharingRenderer(list, currentFriendFileList));
        panel.setImageEditor(enableSharingEditor(currentFriendFileList));
        
        return panel;
    }
    
    @SuppressWarnings("unchecked")
    private EventSelectionModel<LocalFileItem> getSelectionModel(ImageList list){
        return (EventSelectionModel<LocalFileItem>) list.getSelectionModel();
    }
    
    private ImageCellRenderer enableMyLibraryRenderer(ImageList imageList) {
        ImageCellRenderer renderer = new LibraryImageCellRenderer(imageList.getFixedCellWidth(), imageList.getFixedCellHeight() - 2, thumbnailManager);
        renderer.setOpaque(false);
        JComponent buttonRenderer = new ShareTableRendererEditor(null);
        buttonRenderer.setOpaque(false);
        buttonRenderer.setPreferredSize(subPanelDimension);
        buttonRenderer.setSize(subPanelDimension);
        renderer.setButtonComponent(buttonRenderer);
        
        return renderer;
    }
    
    private TableRendererEditor enableMyLibraryEditor(LibrarySharePanel sharePanel, JComponent parent){
        ShareAction action = new ShareAction(I18n.tr("Sharing"), sharePanel, parent);
        ShareTableRendererEditor shareEditor = new ShareTableRendererEditor(action);
        action.setEditor(shareEditor);
        shareEditor.setPreferredSize(subPanelDimension);
        shareEditor.setSize(subPanelDimension);
        shareEditor.setOpaque(false);
        shareEditor.setVisible(false);
                
        return shareEditor;
    }

    private ImageCellRenderer enableSharingRenderer(ImageList imageList, LocalFileList fileList) {
        LibraryImageCellRenderer renderer = new LibraryImageCellRenderer(imageList.getFixedCellWidth(), imageList.getFixedCellHeight() - 2, thumbnailManager);
        renderer.setOpaque(false);
        JComponent buttonRenderer = new SharingCheckBoxSubPanel(fileList);
        buttonRenderer.setPreferredSize(subPanelDimension);
        buttonRenderer.setSize(subPanelDimension);
        renderer.setButtonComponent(buttonRenderer);

        return renderer;
    }
    
    
    private TableRendererEditor enableSharingEditor(LocalFileList fileList) {
        SharingCheckBoxEditor shareEditor = new SharingCheckBoxEditor(fileList);
        shareEditor.setPreferredSize(subPanelDimension);
        shareEditor.setSize(subPanelDimension);
        shareEditor.setOpaque(false);
        shareEditor.setVisible(false);
                
        return shareEditor;
    }
    
    private class ShareAction extends AbstractAction {
        private ShareTableRendererEditor shareEditor;
        private LibrarySharePanel librarySharePanel;
        private JComponent parent;

        public ShareAction(String text, LibrarySharePanel librarySharePanel, JComponent parent){
            super(text);
            this.librarySharePanel = librarySharePanel;
            this.parent = parent;
        }
        
        public void setEditor(ShareTableRendererEditor editor) {
            this.shareEditor = editor;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ((FileShareModel)librarySharePanel.getShareModel()).setFileItem(shareEditor.getLocalFileItem());
          
            Rectangle bounds = shareEditor.getShareButton().getBounds();
            Point convertedLocation = 
                SwingUtilities.convertPoint(shareEditor, shareEditor.getShareButton().getLocation(), parent.getParent());
            bounds.x = convertedLocation.x;
            bounds.y = convertedLocation.y;
            librarySharePanel.show(shareEditor.getShareButton());
            shareEditor.cancelCellEditing();
        }
        
    }
}
