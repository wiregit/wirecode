package org.limewire.ui.swing.search.resultpanel;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class implements the menu that is displayed
 * when the user right clicks a search result.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultMenu extends JPopupMenu {

    private NavigableTree navTree;

    public SearchResultMenu(final BaseResultPanel brp,
        final NavigableTree navTree,
        final VisualSearchResult vsr,
        final int row) {

        this.navTree = navTree;

        add(new AbstractAction("Download") {
            public void actionPerformed(ActionEvent e) {
                brp.download(vsr, row);
            }
        });

        add(new AbstractAction("Mark as junk") {
            public void actionPerformed(ActionEvent e) {
                vsr.setMarkedAsJunk(true);
            }
        });

        add(new AbstractAction("Copy link to clipboard") {
            public void actionPerformed(ActionEvent e) {
                // TODO: RMV Implement this!
                System.out.println("not implemented yet");
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
                System.out.println("not implemented yet");
            }
        });
    }

    private void addSubmenu(RemoteHost host) {
        JMenu menu = new JMenu(host.getHostDescription());

        menu.add(new AbstractAction("View library") {
            public void actionPerformed(ActionEvent e) {
                navTree.getNavigableItemByName(
                    Navigator.NavCategory.LIBRARY,
                    MyLibraryPanel.NAME).select();
            }
        });

        menu.add(new AbstractAction("Chat") {
            public void actionPerformed(ActionEvent e) {
                // TODO: RMV Implement this!
                System.out.println("not implemented yet");
            }
        });

        add(menu);
    }
}