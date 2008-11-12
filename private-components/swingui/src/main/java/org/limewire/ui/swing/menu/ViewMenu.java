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
        addSeparator();
        // TODO need to tie inot some as yet unwritten store of recent searches
        // show only active searches, or the recent ones as well?
        add(new JMenu(I18n.tr("Recent Searches")));

        addSeparator();
        // TODO should be checkable
        add(new AbstractAction(I18n.tr("List view")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.LIST.getId());
            }
        });
        add(new AbstractAction(I18n.tr("Classic view")) {
            // TODO should be checkable
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchSettings.SEARCH_VIEW_TYPE_ID.setValue(SearchViewType.TABLE.getId());
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Sort by")) {
            // TODO this might be a pain considering all the differant sort
            // options depending on the view
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        add(new AbstractAction(I18n.tr("Filter current view")) {
            // TODO make focus on filter box, will need to know what the current
            // view is, disabled if the screen does not have a filter.
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
    }
}
