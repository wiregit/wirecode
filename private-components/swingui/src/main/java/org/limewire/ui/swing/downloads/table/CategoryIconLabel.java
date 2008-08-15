package org.limewire.ui.swing.downloads.table;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.util.GuiUtils;

public class CategoryIconLabel extends JLabel {

    @Resource
    private Icon audioIcon;
    @Resource
    private Icon imageIcon;
    @Resource
    private Icon videoIcon;
    @Resource
    private Icon documentIcon;
    @Resource
    private Icon programIcon;
    @Resource
    private Icon otherIcon;
    
    public CategoryIconLabel(){
        GuiUtils.assignResources(this);
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
