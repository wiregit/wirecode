package org.limewire.ui.swing.downloads.table;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.OSUtils;

public class CategoryIconLabel extends JLabel {
    
    public static enum Size { LARGE, SMALL };
    
    private Icon audioIcon;
    private Icon imageIcon;
    private Icon videoIcon;
    private Icon documentIcon;
    private Icon programIcon;
    private Icon otherIcon;
    
    @Resource
    private Icon smallAudioIcon;
    @Resource
    private Icon smallImageIcon;
    @Resource
    private Icon smallVideoIcon;
    @Resource
    private Icon smallDocumentIcon;
    @Resource
    private Icon smallProgramIconWinVista;
    @Resource
    private Icon smallProgramIconWinXP;
    @Resource
    private Icon smallProgramIconOSX;
    @Resource
    private Icon smallProgramIconLinux;
    @Resource
    private Icon smallOtherIcon;
    
    @Resource
    private Icon largeAudioIcon;
    @Resource
    private Icon largeImageIcon;
    @Resource
    private Icon largeVideoIcon;
    @Resource
    private Icon largeProgramIconWinVista;
    @Resource
    private Icon largeProgramIconWinXP;
    @Resource
    private Icon largeProgramIconOSX;
    @Resource
    private Icon largeProgramIconLinux;
    @Resource
    private Icon largeDocumentIcon;
    @Resource
    private Icon largeOtherIcon;
    
    public CategoryIconLabel(Size size) {
        GuiUtils.assignResources(this);
        
        if (size == Size.LARGE) {
            audioIcon = largeAudioIcon;
            imageIcon = largeImageIcon;
            videoIcon = largeVideoIcon;
            documentIcon = largeDocumentIcon;
            otherIcon = largeOtherIcon;
            
            if (OSUtils.isAnyMac()) {
                programIcon = largeProgramIconOSX;
            } else if (OSUtils.isWindowsVista()) {
                programIcon = largeProgramIconWinVista;
            } else if (OSUtils.isWindowsXP()) {
                programIcon = largeProgramIconWinXP;
            } else {
                programIcon = largeProgramIconLinux;
            }
            
             
        } else {
            audioIcon = smallAudioIcon;
            imageIcon = smallImageIcon;
            videoIcon = smallVideoIcon;
            documentIcon = smallDocumentIcon;
            otherIcon = smallOtherIcon;
            
            if (OSUtils.isAnyMac()) {
                programIcon = smallProgramIconOSX;
            } else if (OSUtils.isWindowsVista()) {
                programIcon = smallProgramIconWinVista;
            } else if (OSUtils.isWindowsXP()) {
                programIcon = smallProgramIconWinXP;
            } else {
                programIcon = smallProgramIconLinux;
            }
        }
    }
    
    public void setIcon(Category category) {
        switch (category) {
        case AUDIO:
            setIcon(audioIcon);
            break;
        case DOCUMENT:
            setIcon(documentIcon);
            break;
        case IMAGE:
            setIcon(imageIcon);
            break;
        case VIDEO:
            setIcon(videoIcon);
            break;
        case PROGRAM:
            setIcon(programIcon);
            break;
        default:
            setIcon(otherIcon);
        }
    }
}