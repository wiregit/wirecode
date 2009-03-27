package org.limewire.ui.swing.library.nav;

import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.action.AbstractAction;

import com.google.inject.Inject;

public class BrowseFloodAction extends AbstractAction {
    
    private final BrowseFactory browseFactory;
    
    private Friend friend;
    final private AtomicBoolean stop = new AtomicBoolean(false);
    
    @Inject
    public BrowseFloodAction(BrowseFactory browseFactory) {
        super("Flood");
        
        this.browseFactory = browseFactory;
    }
    
    public void setFriend(Friend friend) {
        this.friend = friend;
    }

    public void stop() {
        stop.set(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        
        if (friend == null) return;
        
        for (final FriendPresence presence : friend.getFriendPresences().values()) {
            System.out.println("flooding: " + presence);
            
            new Thread(new Runnable() {
                public void run() {
                    while (!stop.get()) {
                        browseFactory.createBrowse(presence).start(new BrowseListener() {

                            @Override
                            public void browseFinished(boolean success) {
                            }

                            @Override
                            public void handleBrowseResult(SearchResult searchResult) {
                            }
                        });

                        try {
                            Thread.sleep(10);
                        } catch (Exception e) {
                        }

                    }
                    System.out.println("stopped: " + presence);
                    stop.set(false);
                }
            }).start();
        }
        
        createCloser();
    }
    
    private void createCloser() {
        JFrame frame = new JFrame();
        
        JButton closeButton = new JButton(new AbstractAction("Stop Flooding " + friend.getId()){
            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
            }});
        
        frame.getContentPane().add(closeButton);
        frame.pack();
        frame.setVisible(true);
    }

}
