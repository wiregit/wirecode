package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

public class FriendLibraryPopupMenu extends JPopupMenu {
   
    private List<RemoteFileItem> fileItems;

    final private JSeparator separator = new JSeparator();
    final private JMenuItem linkItem;
    final private JMenuItem propertiesItem;

    final private DownloadListManager downloadListManager;

    public FriendLibraryPopupMenu(DownloadListManager downloadListManager) {
        this.downloadListManager = downloadListManager;
        
        linkItem = new JMenuItem(linkAction);
        propertiesItem = new JMenuItem(propertiesAction);

        add(downloadAction);
        add(linkItem);
        add(separator);
        add(propertiesItem);

    }

    public void setFileItems(List<RemoteFileItem> items) {
        this.fileItems = items;   
          
        boolean isMultipleSelection = fileItems.size() > 1;
        linkItem.setVisible(isMultipleSelection);
        separator.setVisible(isMultipleSelection);
        propertiesItem.setVisible(isMultipleSelection);        
    }


    private RemoteFileItem[] createFileItemArray(){
        return fileItems.toArray(new RemoteFileItem[fileItems.size()]);
    }
    
   private Action downloadAction = new AbstractAction(I18n.tr("DownloadSelected")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final RemoteFileItem[] fileItemArray = createFileItemArray();
            
            BackgroundExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    for (RemoteFileItem fileItem : fileItemArray) {
                        try {
                            downloadListManager.addDownload(fileItem);
                        } catch (SaveLocationException e) {
                            // TODO handle SaveLocationException
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }
    };

    private Action linkAction = new AbstractAction(I18n.tr("Copy Link to Clipboard")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO Copy Link to Clipboard
            throw new RuntimeException("Implement me");
        }
    };
    
    private Action propertiesAction = new AbstractAction(I18n.tr("Properties")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO Properties
            throw new RuntimeException("Implement me");
        }
    };
    

}
