package org.limewire.ui.swing.menu;

import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.menu.actions.AddFileAction;
import org.limewire.ui.swing.menu.actions.ExitAction;
import org.limewire.ui.swing.menu.actions.ExitAfterTransferAction;
import org.limewire.ui.swing.menu.actions.OpenFileAction;
import org.limewire.ui.swing.menu.actions.OpenLinkAction;
import org.limewire.ui.swing.menu.actions.RecentDownloadsMenu;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

public class FileMenu extends MnemonicMenu {
    @Inject
    public FileMenu(OpenFileAction openFileAction, OpenLinkAction openLinkAction,
            RecentDownloadsMenu recentDownloadsMenu, AddFileAction addFileAction,
            ExitAfterTransferAction exitAfterTransferAction,
            ExitAction exitAction) {
        super(I18n.tr("&File"));
        
        add(openFileAction);
        add(openLinkAction);
        add(recentDownloadsMenu);
        addSeparator();
        add(addFileAction);

        // Add exit actions.
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(exitAfterTransferAction);
            add(exitAction);
        }
    }
}
