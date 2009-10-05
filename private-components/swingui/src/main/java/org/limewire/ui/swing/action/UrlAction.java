package org.limewire.ui.swing.action;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.ResizeUtils;

public class UrlAction extends AbstractAction {
    private LaunchType type = LaunchType.EXTERNAL_BROWSER;
    private final String url;
    
    /**
     * Constructs a UrlAction whose name is its URL, without any identifying
     * information added to the URL.
     */
    public UrlAction(String url, LaunchType type) {
        this(url, url);
        this.type = type;
    }
    
    /**
     * Constructs a UrlAction whose name is its URL, without any identifying
     * information added to the URL.
     */
    public UrlAction(String url) {
        this(url, url);
    }
    
    /**
     * Constructs a UrlAction with a specific name & url, without any identifying
     * information added to the URL.
     */
    public UrlAction(String name, String url) {
        super(name);
        this.url = url;
        putValue(Action.SHORT_DESCRIPTION, url);
    }
    
    /**
     * Constructs a UrlAction with a specific name & url, without any identifying
     * information added to the URL.
     */
    public UrlAction(String name, String url, LaunchType type) {
        super(name);
        this.url = url;
        this.type = type;
        putValue(Action.SHORT_DESCRIPTION, url);
    }
    
    /**
     * Constructs a UrlAction whose name is its URL, with identifying
     * information added to the URL.
     */
    public UrlAction(String url, Application application) {
        this(url, url, application);
    }
    
    /**
     * Constructs a UrlAction with a specific name & URL, with identifying
     * information added to the URL.
     */
    public UrlAction(String name, String url, Application application) {
        super(name);
        this.url = application.addClientInfoToUrl(url);
        putValue(Action.SHORT_DESCRIPTION, url);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (type == LaunchType.EXTERNAL_BROWSER) {
            NativeLaunchUtils.openURL(url);
        }
        else {
            showPopup(url);
        }
    }
    
    public enum LaunchType {
        EXTERNAL_BROWSER, POPUP;
    }
    
    private static void showPopup(final String url) {
        new LimeJDialog() {
            {   getContentPane().setLayout(new BorderLayout());
                HTMLPane browser = new HTMLPane();
                
                browser.addHyperlinkListener(new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType() == EventType.ACTIVATED) {
                            NativeLaunchUtils.openURL(e.getURL().toString());
                        }
                    }
                });
                
                JScrollPane scrollPane = new JScrollPane(browser, 
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                
                getContentPane().add(scrollPane);
                
                ResizeUtils.forceSize(this, new Dimension(600,400));
                setModal(true);
                setResizable(false);
                setAlwaysOnTop(true);
                getContentPane();
                pack();
                setLocationRelativeTo(null);
                
                // If popout browser does not work use the system browser.
                browser.setPageAsynchronous(url, null).addFutureListener(new EventListener<FutureEvent<Boolean>>() {

					@SwingEDTEvent
                    @Override
                    public void handleEvent(FutureEvent<Boolean> event) {
                        if (!(event.getResult() == Boolean.TRUE)) {
                            dispose();
                            NativeLaunchUtils.openURL(url);
                        }
                    }
                    
                });
                                
                setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                
                setVisible(true);
            }
        };
    }
}