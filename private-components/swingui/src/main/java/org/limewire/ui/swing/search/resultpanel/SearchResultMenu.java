package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class implements the menu that is displayed
 * when the user right clicks a search result.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultMenu extends JPopupMenu {

//    private Navigator navigator;

    public SearchResultMenu(final BaseResultPanel brp,
        final Navigator navigator,
        final VisualSearchResult vsr,
        final int row) {

//        this.navigator = navigator;

        add(new AbstractAction(tr("Download")) {
            public void actionPerformed(ActionEvent e) {
                brp.download(vsr, row);
            }
        });

        add(new AbstractAction(tr("Mark as junk")) {
            public void actionPerformed(ActionEvent e) {
                vsr.setSpam(true);
            }
        });

        add(new AbstractAction(tr("Copy link to clipboard")) {
            public void actionPerformed(ActionEvent e) {
                //TODO
                throw new UnsupportedOperationException("Implement Me Properly!");
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

        add(new AbstractAction(tr("Properties")) {
            public void actionPerformed(ActionEvent e) {
                //TODO
                throw new UnsupportedOperationException("Implement Me Properly!");
            }
        });
    }

    private void addSubmenu(RemoteHost host) {
        JMenu menu = new JMenu("todo implement");

        menu.add(new AbstractAction(tr("View library")) {
            public void actionPerformed(ActionEvent e) {
                //TODO
                throw new UnsupportedOperationException("Implement Me Properly!");
            }
        });

        menu.add(new AbstractAction(tr("Chat")) {
            public void actionPerformed(ActionEvent e) {
                //TODO
                throw new UnsupportedOperationException("Implement Me Properly!");
            }
        });

        add(menu);
    }
}