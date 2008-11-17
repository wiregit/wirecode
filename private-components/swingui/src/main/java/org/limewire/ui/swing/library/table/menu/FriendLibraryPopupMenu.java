package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.SaveAsDialogue;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

public class FriendLibraryPopupMenu extends JPopupMenu {
   
    private List<RemoteFileItem> fileItems;

    final private JSeparator separator = new JSeparator();
    final private JMenuItem linkItem;
    final private JMenuItem propertiesItem;

    final private DownloadListManager downloadListManager;

    private MagnetLinkFactory magnetFactory;
    private PropertiesFactory<RemoteFileItem> propertiesFactory;

    public FriendLibraryPopupMenu(DownloadListManager downloadListManager, MagnetLinkFactory magnetFactory, 
            PropertiesFactory<RemoteFileItem> propertiesFactory) {
        this.downloadListManager = downloadListManager;
        this.magnetFactory = magnetFactory;
        this.propertiesFactory = propertiesFactory;
        
        linkItem = new JMenuItem(linkAction);
        propertiesItem = new JMenuItem(propertiesAction);

        add(downloadAction);
        add(linkItem);
        add(separator);
        add(propertiesItem);

    }

    public void setFileItems(List<RemoteFileItem> items) {
        this.fileItems = items;   
          
        boolean isSingleSelection = fileItems.size() == 1;
        linkItem.setVisible(isSingleSelection);
        separator.setVisible(isSingleSelection);
        propertiesItem.setVisible(isSingleSelection);        
    }


    private RemoteFileItem[] createFileItemArray(){
        return fileItems.toArray(new RemoteFileItem[fileItems.size()]);
    }
    
   private Action downloadAction = new AbstractAction(I18n.tr("DownloadSelected")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final RemoteFileItem[] fileItemArray = createFileItemArray();
            
            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (final RemoteFileItem fileItem : fileItemArray) {
                        try {
                            downloadListManager.addDownload(fileItem);
                        } catch (SaveLocationException e) {
                            SaveAsDialogue.handleSaveLocationException(new SaveAsDialogue.DownLoadAction() {
                                @Override
                                public void download(File saveFile, boolean overwrite)
                                        throws SaveLocationException {
                                    downloadListManager.addDownload(fileItem, saveFile, overwrite);
                                }
                            }, e, null);
                        }
                    }
                }
            });
        }
    };

    private Action linkAction = new AbstractAction(I18n.tr("Copy Link to Clipboard")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            new MagnetLinkCopier().copyLinkToClipBoard(fileItems.get(0), magnetFactory);
        }
    };
    
    private Action propertiesAction = new AbstractAction(I18n.tr("View File Info")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO - Is this correct? Only to show props for first one?
            propertiesFactory.newProperties().showProperties(fileItems.get(0));
        }
    };
    

}
