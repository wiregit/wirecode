package org.limewire.ui.swing.search.resultpanel;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class implements the menu that is displayed
 * when the user right clicks a search result.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultMenu extends JPopupMenu {

    public SearchResultMenu(Window owner, final VisualSearchResult vsr) {
        add(new AbstractAction("Download") {
            public void actionPerformed(ActionEvent e) {
                vsr.download();
            }
        });

        add(new AbstractAction("Mark as junk") {
            public void actionPerformed(ActionEvent e) {
                // TODO: RMV Implement this!
            }
        });

        add(new AbstractAction("Copy link to clipboard") {
            public void actionPerformed(ActionEvent e) {
                // TODO: RMV Implement this!
            }
        });

        addSeparator();

        Collection<RemoteHost> sources = vsr.getSources();
        RemoteHost[] hosts = sources.toArray(new RemoteHost[] {});
        boolean multipleSources = sources.size() > 1;

        if (multipleSources) {
            for (RemoteHost host : hosts) {
                addSubmenu(host);
            }
        } else {
            RemoteHost host = hosts[0];
            addSubmenu(host);
        }

        addSeparator();

        add(new AbstractAction("Properties") {
            public void actionPerformed(ActionEvent e) {
                // TODO: RMV Implement this!
            }
        });
    }

    private void addSubmenu(RemoteHost host) {
        JMenu menu = new JMenu(host.getHostDescription());

        menu.add(new AbstractAction("View library") {
            public void actionPerformed(ActionEvent e) {
                // TODO: RMV Implement this!
            }
        });

        menu.add(new AbstractAction("Chat") {
            public void actionPerformed(ActionEvent e) {
                // TODO: RMV Implement this!
            }
        });

        add(menu);
    }
}