package org.limewire.ui.swing.friends;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
@Singleton
class IconLibraryImpl implements IconLibrary {
    @Resource private Icon available;
    @Resource private Icon away;
    @Resource private Icon chatting;
    @Resource private Icon endChat;
    @Resource private Icon doNotDisturb;
    @Resource private Icon chatStatusStub;
    @Resource private Icon library;
    @Resource private Icon sharing;
    
    public IconLibraryImpl() {
        GuiUtils.assignResources(this);
    }

    public Icon getAvailable() {
        return available;
    }

    public Icon getAway() {
        return away;
    }
    
    public Icon getChatting() {
        return chatting;
    }

    public Icon getEndChat() {
        return endChat;
    }

    public Icon getDoNotDisturb() {
        return doNotDisturb;
    }

    public Icon getChatStatusStub() {
        return chatStatusStub;
    }

    public Icon getLibrary() {
        return library;
    }

    public Icon getSharing() {
        return sharing;
    }
}
