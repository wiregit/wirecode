package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
//import java.awt.event.ActionEvent;
//
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

//import org.limewire.core.api.browse.BrowseFactory;
//import org.limewire.core.api.browse.BrowseListener;
//import org.limewire.core.api.search.SearchResult;
//import org.limewire.net.address.Address;

import com.google.inject.Inject;
import com.google.inject.Singleton;
//import com.limegroup.gnutella.net.address.gnutella.PushProxyHolePunchAddress;

@Singleton
public class StatusPanel extends JPanel {
    
    @Inject
    public StatusPanel(/*BrowseAction browseAction*/) {
        add(new JLabel("status"));
        setBackground(Color.GRAY);
        setMinimumSize(new Dimension(0, 20));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        setPreferredSize(new Dimension(1024, 20));
        //add(new JButton(browseAction));
    }
    
//    @Singleton
//    public static class BrowseAction extends AbstractAction {
//        private Address address;
//        private final BrowseFactory browseFactory;
//
//        @Inject
//        public BrowseAction(BrowseFactory browseFactory) {
//            super("browse: ");
//            this.browseFactory = browseFactory;
//        }
//
//        public void actionPerformed(ActionEvent e) {
//            browseFactory.createBrowse(address).start(new BrowseListener() {
//                public void handleBrowseResult(SearchResult searchResult) {
//                    System.out.println(searchResult.getDescription() + ": " + searchResult.getUrn());
//                    // TODO update UI
//                }
//            });
//        }
//        
//        public void setAddress(Address address) {
//            if(address instanceof PushProxyHolePunchAddress) {
//                if(!((PushProxyHolePunchAddress)address).getDirectConnectionAddress().getAddress().contains("66.108.19.102")) {
//                    this.address = address;
//                    putValue(Action.NAME, "browse: " + address);    
//                }
//            }            
//        }
//    }

}
