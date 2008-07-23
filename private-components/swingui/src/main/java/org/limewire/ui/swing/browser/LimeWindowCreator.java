package org.limewire.ui.swing.browser;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.SystemUtils;
import org.mozilla.browser.IMozillaWindow;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.impl.WindowCreator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebBrowserChrome;
import org.mozilla.xpcom.Mozilla;

class LimeWindowCreator extends WindowCreator {
    
    private final WindowCreator delegateCreator;
    
    @Resource
    private Icon limeIcon;
    
    @Resource
    private String limeFrameIconLocation;
    
    private File icoFile;
    
    LimeWindowCreator(WindowCreator delegateCreator) {
        GuiUtils.assignResources(this);
        icoFile = new File(URI.create(ClassLoader.getSystemResource(limeFrameIconLocation).getFile()).getPath()).getAbsoluteFile();
        this.delegateCreator = delegateCreator;
    }
    
    private void setLimeIcon(IMozillaWindow window) {
        if(window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            frame.setIconImage(((ImageIcon)limeIcon).getImage());
            SystemUtils.setWindowIcon(frame, icoFile);
        }
    }
    
    private void addGoToNativeButton(final IMozillaWindow window) {
        JToolBar toolbar = window.getToolbar();
        toolbar.add(new AbstractAction("Out") {
            // TODO: Add a picture.
            public void actionPerformed(ActionEvent e) {
                GuiUtils.openURL(window.getUrl());
            }
        });
    }
    
    private void addKeyListener(IMozillaWindow window, nsIWebBrowserChrome chrome) {
        if (window instanceof Component) {
            Component component = (Component) window;
            component.addKeyListener(new MozillaKeyListener(chrome));
        }
    }
    
    private void addClosingListener(IMozillaWindow window, nsIWebBrowserChrome chrome){
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            frame.addWindowListener(new MozillaClosingListener(chrome));
        }
    }
   
    @Override
    public nsIWebBrowserChrome createChromeWindow(nsIWebBrowserChrome parent, long chromeFlags) {
        final nsIWebBrowserChrome chrome = delegateCreator.createChromeWindow(parent, chromeFlags);
        final IMozillaWindow window = MozillaAutomation.findWindow(chrome);
        BrowserUtils.addDomListener(chrome);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Note: setLimeIcon requires that the window has had addNotify
                //       called and is displayable.  This is a precondition of moz's
                //       returning a window, so we're OK.
                setLimeIcon(window);
                addGoToNativeButton(window);
                addKeyListener(window, chrome);
                addClosingListener(window, chrome);
            }
        });
        return chrome;
    }

    @Override
    public void ensurePrecreatedWindows() {
        delegateCreator.ensurePrecreatedWindows();
    }

    @Override
    public void ensurePrecreatedWindows(int winNum) {
        delegateCreator.ensurePrecreatedWindows(winNum);
    }

    @Override
    public nsISupports queryInterface(String aiid) {
        return Mozilla.queryInterface(this, aiid);
    }

}
