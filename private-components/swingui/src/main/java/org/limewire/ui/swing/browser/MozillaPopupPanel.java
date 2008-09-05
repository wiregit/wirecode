package org.limewire.ui.swing.browser;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.SystemUtils;
import org.mozilla.browser.IMozillaWindow;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.MozillaWindow;
import org.mozilla.browser.impl.ChromeAdapter;

class MozillaPopupPanel extends MozillaPanel {

    @Resource
    private Icon limeIcon;
    
    @Resource
    private String limeFrameIconLocation;
    
    private File icoFile;
   
    
    MozillaPopupPanel(MozillaWindow window, boolean attachNewBrowserOnCreation) {
        super(window, attachNewBrowserOnCreation, null, null);
        GuiUtils.assignResources(this);
        icoFile = new File(URI.create(ClassLoader.getSystemResource(limeFrameIconLocation).getFile()).getPath()).getAbsoluteFile();
        initialize();
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        SystemUtils.setWindowIcon(this, icoFile);
    }
    
    private void initialize() {
        
        IMozillaWindow mozillaWindow = getContainerWindow();
        if(mozillaWindow != null && mozillaWindow instanceof JFrame) {
            JFrame frame = (JFrame)mozillaWindow;
            frame.setIconImage(((ImageIcon)limeIcon).getImage());    
        }

        JToolBar toolbar = getToolbar();
        toolbar.add(new AbstractAction("Out") {
            // TODO: Add a picture.
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL(getUrl());
            }
        });
    }
    
    @Override
    public void onAttachBrowser(final ChromeAdapter chromeAdapter, ChromeAdapter parentChromeAdapter) {
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);                   
        BrowserUtils.addDomListener(chromeAdapter);
        SwingUtils.invokeLater(new Runnable() {
            public void run() {               
                addKeyListener(new MozillaKeyListener(chromeAdapter));
            }
        });
    }
    
    @Override
    public void onDetachBrowser() {
        if(getChromeAdapter() != null) {
            BrowserUtils.removeDomListener(getChromeAdapter());
        }        
        super.onDetachBrowser();
    }
}
