package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class implements the menu that is displayed
 * when the user right clicks a search result.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultMenu extends JPopupMenu {

    public SearchResultMenu(final BaseResultPanel brp,
        final Navigator navigator,
        final VisualSearchResult vsr,
        final int row,
        final RemoteHostActions fromActions) {

        add(new AbstractAction(tr("Download")) {
            public void actionPerformed(ActionEvent e) {
                brp.download(vsr, row);
            }
        });

        add(new AbstractAction(tr("Mark as spam")) {
            public void actionPerformed(ActionEvent e) {
                vsr.setSpam(true);
            }
        });

        add(new AbstractAction(tr("Copy link to clipboard")) {
            public void actionPerformed(ActionEvent e) {
                StringSelection sel = new StringSelection(vsr.getMagnetLink());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            }
        });

        addSeparator();

        Collection<RemoteHost> sources = vsr.getSources();
        RemoteHost[] hosts = sources.toArray(new RemoteHost[] {});
        boolean multipleSources = sources.size() > 1;

        if (multipleSources) {
            for (RemoteHost host : hosts) {
                addSubmenu(host, fromActions);
            }
        } else {
            RemoteHost host = hosts[0];
            addSubmenu(host, fromActions);
        }

        addSeparator();

        add(new AbstractAction(tr("Properties")) {
            public void actionPerformed(ActionEvent e) {
                //TODO
                throw new UnsupportedOperationException("Implement Me Properly!");
            }
        });
    }

    private void addSubmenu(final RemoteHost host, final RemoteHostActions fromActions) {
        JMenu menu = new JMenu(host.getRenderName());

        menu.add(new AbstractAction(tr("View library")) {
            public void actionPerformed(ActionEvent e) {
                fromActions.viewLibraryOf(host);
            }
        });

        menu.add(new AbstractAction(tr("Chat")) {
            public void actionPerformed(ActionEvent e) {
                fromActions.chatWith(host);
            }
        });

        add(menu);
    }
}