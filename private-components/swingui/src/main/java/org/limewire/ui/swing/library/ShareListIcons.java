package org.limewire.ui.swing.library;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.util.GuiUtils;

public class ShareListIcons {
    @Resource
    private Icon publicIcon;

    public ShareListIcons() {
        GuiUtils.assignResources(this);
    }

    public Icon getListIcon(SharedFileList sharedFileList) {
        return publicIcon;
    }
}
