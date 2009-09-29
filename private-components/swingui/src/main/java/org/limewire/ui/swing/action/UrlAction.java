package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class UrlAction extends AbstractAction {
    private final String url;
    
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
        NativeLaunchUtils.openURL(url);
    }
}