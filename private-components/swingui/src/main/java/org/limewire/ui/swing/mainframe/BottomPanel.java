package org.limewire.ui.swing.mainframe;

import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.downloads.DownloadMediator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

/**
 * UI container for the tray displayed along the bottom of the application
 * window.  BottomPanel is used to present the Downloads and Uploads tables.
 */
public class BottomPanel extends JPanel {
    public enum TabId {
        DOWNLOADS, UPLOADS
    }
    
    @Resource private int preferredHeight;
    
    private final DownloadMediator downloadMediator;
    private final UploadMediator uploadMediator;
    
    private CardLayout cardLayout;
    
    /**
     * Constructs a BottomPanel with the specified components.
     */
    @Inject
    public BottomPanel(DownloadMediator downloadMediator,
            UploadMediator uploadMediator) {
        this.downloadMediator = downloadMediator;
        this.uploadMediator = uploadMediator;
        
        GuiUtils.assignResources(this);
        
        initializeComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initializeComponents() {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        
        int savedHeight = SwingUiSettings.BOTTOM_TRAY_SIZE.getValue();
        int height = (savedHeight == 0) ? preferredHeight : savedHeight;
        setPreferredSize(new Dimension(getPreferredSize().width, height));
        
        add(downloadMediator.getComponent(), TabId.DOWNLOADS.toString());
        add(uploadMediator.getComponent(), TabId.UPLOADS.toString());
    }
    
    /**
     * Returns the default preferred height for the bottom tray.
     */
    public int getDefaultPreferredHeight(){
        return preferredHeight;
    }
    
    /**
     * Displays the content associated with the specified tab id.
     */
    public void show(TabId tabId) {
        cardLayout.show(this, tabId.toString());
    }
}
