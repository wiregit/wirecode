package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StorePanel extends JPanel {
    public static final String NAME = "LimeWire Store";

    private final Browser browser;
    private final Application application;
    private boolean loadedOnce = false;

    @Inject
    public StorePanel(Application application) {
        this.application = application;
        browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(browser, gbc);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if(!loadedOnce) {
                    loadedOnce = true;
                    load("http://store.limewire.com/");
                }
                removeComponentListener(this);
            }
        });
    }
    
    public void reloadIfNecessary() {
        if(loadedOnce && !browser.isLastRequestSuccess()) {
            browser.reload();
        }
    }

    public void load(String url) {
        loadedOnce = true;
        url = application.getUniqueUrl(url) + "&isClient=true";
        browser.load(url);
    }
}
