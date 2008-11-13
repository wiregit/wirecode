package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;

import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.mainframe.FriendsPanel;
import org.limewire.ui.swing.mainframe.LeftPanel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;

public class ViewMenu extends JMenu {
    @Inject
    public ViewMenu(final LeftPanel leftPanel, final DownloadSummaryPanel downloadSummaryPanel, final FriendsPanel friendsPanel) {
        super(I18n.tr("View"));
        add(buildAction(leftPanel, I18n.tr("Hide Libraries bar"), I18n.tr("Show Libraries bar")));
        add(buildAction(downloadSummaryPanel, I18n.tr("Hide Download Tray"), I18n.tr("Show Download Tray")));
        add(buildAction(friendsPanel, I18n.tr("Hide Chat window"), I18n.tr("Show Chat window")));
    }

    private Action buildAction(final VisibleComponent component, final String visibleName,
            final String notVisibleName) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                component.toggleVisibility();
            }
        };

        component.addVisibilityListener(new VisibilityListener() {
            @Override
            public void visibilityChanged(boolean visible) {
                if (visible) {
                    action.putValue(Action.NAME, visibleName);
                } else {
                    action.putValue(Action.NAME, notVisibleName);
                }
            }
        });

        if (component.isVisible()) {
            action.putValue(Action.NAME, visibleName);
        } else {
            action.putValue(Action.NAME, notVisibleName);
        }

        return action;
    }
}
