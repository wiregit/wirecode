package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;

import org.limewire.core.settings.SearchSettings;
import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.mainframe.LeftPanel;
import org.limewire.ui.swing.search.SearchViewType;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ViewMenu extends JMenu {
    @Inject
    public ViewMenu(final LeftPanel leftPanel, final DownloadSummaryPanel downloadSummaryPanel) {
        super(I18n.tr("View"));

        add(new AbstractAction(I18n.tr("Show/Hide Libraries bar")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanel.toggleVisibility();
            }
        });
        add(new AbstractAction(I18n.tr("Show/Hide Download Tray")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSummaryPanel.toggleVisibility();
            }
        });
        add(new AbstractAction(I18n.tr("Show/Hide chat window")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DisplayFriendsToggleEvent().publish();
            }
        });
        addSeparator();
        add(new JMenu(I18n.tr("Recent Searches")));
        
        addSeparator();
        add(new AbstractAction(I18n.tr("List view")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.LIST.getId());
            }
        });
        add(new AbstractAction(I18n.tr("Classic view")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.TABLE.getId());
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Sort by")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        add(new AbstractAction(I18n.tr("Filter current view")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
    }
}
