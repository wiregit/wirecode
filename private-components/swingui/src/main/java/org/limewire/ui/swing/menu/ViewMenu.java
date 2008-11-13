package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;

import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.mainframe.LeftPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ViewMenu extends JMenu {
    @Inject
    public ViewMenu(final LeftPanel leftPanel, final DownloadSummaryPanel downloadSummaryPanel) {
        super(I18n.tr("View"));

        // TODO text needs to change depending on whether library is showing or
        // not
        add(new AbstractAction(I18n.tr("Show/Hide Libraries bar")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanel.toggleVisibility();
            }
        });

        // TODO text needs to change depending on whether download is showing or
        // not
        add(new AbstractAction(I18n.tr("Show/Hide Download Tray")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSummaryPanel.toggleVisibility();
            }
        });

        // TODO text needs to change depending on whether chat is showing or not
        add(new AbstractAction(I18n.tr("Show/Hide chat window")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DisplayFriendsToggleEvent().publish();
            }
        });
    }
}
