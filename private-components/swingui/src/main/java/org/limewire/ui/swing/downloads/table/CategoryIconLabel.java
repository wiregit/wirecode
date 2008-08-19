package org.limewire.ui.swing.downloads.table;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.util.GuiUtils;

public class CategoryIconLabel extends JLabel {
    
    public static enum Size{LARGE, SMALL};

    
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
    private Icon smallProgramIcon;
    @Resource
    private Icon smallOtherIcon;
    
    @Resource
    private Icon largeAudioIcon;
    @Resource
    private Icon largeImageIcon;
    @Resource
    private Icon largeVideoIcon;
    @Resource
    private Icon largeDocumentIcon;
    @Resource
    private Icon largeProgramIcon;
    @Resource
    private Icon largeOtherIcon;
    
    public CategoryIconLabel(Size size){
        GuiUtils.assignResources(this);
        if(size == Size.LARGE){
            audioIcon = largeAudioIcon;
            imageIcon = largeImageIcon;
            videoIcon = largeVideoIcon;
            documentIcon = largeDocumentIcon;
            programIcon = largeProgramIcon;
            otherIcon = largeOtherIcon;
        } else {
            audioIcon = smallAudioIcon;
            imageIcon = smallImageIcon;
            videoIcon = smallVideoIcon;
            documentIcon = smallDocumentIcon;
            programIcon = smallProgramIcon;
            otherIcon = smallOtherIcon;
        }
    }
    
    public void setIcon(DownloadItem.Category category){
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
