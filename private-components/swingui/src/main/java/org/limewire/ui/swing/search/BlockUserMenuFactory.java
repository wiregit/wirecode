package org.limewire.ui.swing.search;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JMenu;
import javax.swing.JOptionPane;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;


import com.google.inject.Inject;

public class BlockUserMenuFactory {
    
    private final SpamManager spamManager;

    @Inject
    public BlockUserMenuFactory(SpamManager spamManager, DownloadMediator downloadMediator){
        this.spamManager = spamManager;
    }
    
    /**
     * @param allHosts the hosts to add to the menu - duplicates and friends will be ignored
     */
    public JMenu createDownloadBlockMenu(Collection<RemoteHost> allHosts){
        return createBlockMenu(allHosts, new DownloadBlockHandler(allHosts));
    }
    

    /**
     * @param allHosts the hosts to add to the menu - duplicates and friends will be ignored
     * @param searchResults the VisualSearchResults affected
     */
    public JMenu createSearchBlockMenu(Collection<RemoteHost> allHosts, Collection<VisualSearchResult> searchResults){
        return createBlockMenu(allHosts, new SearchBlockHandler());
    }
    

    private JMenu createBlockMenu(Collection<RemoteHost> allHosts, final BlockHandler blockHandler) {
        final Collection<Friend> p2pUsers = new HashSet<Friend>();
        for (RemoteHost host : allHosts) {
            Friend friend = host.getFriendPresence().getFriend();
            if (friend.isAnonymous()) {
                p2pUsers.add(friend);
            }
        }

        if (p2pUsers.size() == 0) {
            return null;
        }

        JMenu blockMenu = new JMenu(I18n.tr("Block User"));

        if (p2pUsers.size() > 1) {
            blockMenu.add(new AbstractAction(I18n.tr("All P2P Users")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (blockHandler.confirmBlockAll()) {
                        for (Friend user : p2pUsers) {
                            spamManager.addToBlackList(user.getName());
                        }
                        blockHandler.handleSideEffects();
                    }
                }
            });
            blockMenu.addSeparator();
        }

        for (final Friend user : p2pUsers) {
            blockMenu.add(new AbstractAction(user.getRenderName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (blockHandler.confirmBlock(user.getRenderName())) {
                        spamManager.addToBlackList(user.getName());
                        blockHandler.handleSideEffects();
                    }
                }
            });
        }

        return blockMenu;
    }
    
    private interface BlockHandler {
        /**whether or not to block a single host*/
        public boolean confirmBlock(String name);
        
        /**whether or not to block the hosts*/
        public boolean confirmBlockAll();
        
        
        /** done after blocking hosts*/
        public void handleSideEffects();
    }
    
    private class DownloadBlockHandler implements BlockHandler {
       // private final Collection<RemoteHost> allHosts;

        public DownloadBlockHandler(Collection<RemoteHost> allHosts){
           // this.allHosts = allHosts;
        }

        @Override
        public boolean confirmBlock(String name) {
            //{0}: P2P user's name
            return confirm(I18n.tr("Block P2P user {0}?", name));
        }

        @Override
        public boolean confirmBlockAll() {
            //{0}: P2P user's name
            return confirm(I18n.tr("Block P2P users?"));
        }
        
        @Override
        public void handleSideEffects() {
            //do nothing for now
//            EventList<DownloadItem> downloadItems = downloadMediator.getDownloadList();
//            downloadItems.getReadWriteLock().writeLock().lock();
//            try {
//                for (DownloadItem item : downloadItems) {
//                    for (RemoteHost host : allHosts){
//                        if (item.getRemoteHosts().contains(host)) {
//                            item.cancel();
//                            break;
//                        }
//                    }
//                }
//            } finally {
//                downloadItems.getReadWriteLock().writeLock().unlock();
//            }
        }

        
    }   
    
    private class SearchBlockHandler implements BlockHandler {
        //the block messages will be different for search and download when we handle side effects of blocking
        @Override
        public boolean confirmBlock(String name) {
            //{0}: P2P user's name
            return confirm(I18n.tr("Block P2P user {0}?", name));
        }

        @Override
        public boolean confirmBlockAll() {
            //{0}: P2P user's name
            return confirm(I18n.tr("Block P2P users?"));
        }

        @Override
        public void handleSideEffects() {
            //do nothing for now
        }
        
    }
    
    private boolean confirm(String message){
        return FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(), message, null, 
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;  
    }
    
    
}