package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.Application;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.mainframe.StoreMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.MozillaInitialization;

import com.google.inject.Inject;

public class SponsoredResultsPanel extends JXPanel {
  
    private final Application application;
    private final Navigator navigator;
    private final HomeMediator homeMediator;
    private final StoreMediator storeMediator;

    @Resource private int textWidth;
    @Resource private int areaWidth;
    @Resource private Color headingColor;
    @Resource private Color bodyColor;
    @Resource private Color urlColor;
    @Resource private Font headingFont;
    @Resource private Font bodyFont;
    @Resource private Font urlFont;
    
    @Inject
    public SponsoredResultsPanel(Navigator navigator, StoreMediator storeMediator, HomeMediator homeMediator, Application application) {
        GuiUtils.assignResources(this);
        this.application = application;
        this.navigator = navigator;
        this.storeMediator = storeMediator;
        this.homeMediator = homeMediator;
        setLayout(new VerticalLayout(10));
        setBorder(BorderFactory.createEmptyBorder(8,5,0,0));
        setMaximumSize(new Dimension(areaWidth, Integer.MAX_VALUE));
        setMinimumSize(new Dimension(areaWidth, Short.MAX_VALUE));
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = areaWidth;
        return d;
    }
   

    public void addEntry(SponsoredResult result){
        add(new SponsoredResultView(result));
    }
        
    private class SponsoredResultView extends JPanel {
        
        private SponsoredResult result;

        public SponsoredResultView(SponsoredResult result){
         super(new VerticalLayout());
            this.result = result;
            
            MultiLineLabel link = new MultiLineLabel("<html><u>"+result.getTitle()+"</u><html>", textWidth);
            link.setForeground(headingColor);
            link.setFont(headingFont);
            link.addMouseListener(new ActionHandListener(new SponsoredResultListener(result)));
            link.setBorder(BorderFactory.createEmptyBorder());
            
            MultiLineLabel textArea = new MultiLineLabel(result.getText(), textWidth);
            textArea.setForeground(bodyColor);
            textArea.setFont(bodyFont);
            textArea.setBorder(BorderFactory.createEmptyBorder());
            
            JLabel urlLabel = new JXLabel(result.getVisibleUrl());
            urlLabel.setForeground(urlColor);
            urlLabel.setFont(urlFont);
            urlLabel.setBorder(BorderFactory.createEmptyBorder());
            
            add(link);
            add(textArea);
            add(urlLabel);
        }

        
        public SponsoredResult getSponsoredResult() {
            return result;
        } 
        
    }
    
    private class SponsoredResultListener implements ActionListener {
        private final SponsoredResult result;
        
        public SponsoredResultListener(SponsoredResult result){
            this.result = result;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!MozillaInitialization.isInitialized()) {
                NativeLaunchUtils.openURL(application.addClientInfoToUrl(result.getUrl()));
            } else {            
                switch(result.getTarget()) {
                case HOME:
                    homeMediator.getComponent().load(result.getUrl());
                    navigator.getNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME).select();
                    break;
                case STORE:
                    storeMediator.getComponent().load(result.getUrl());
                    NavItem item = navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME);
                    if(item != null) {
                        item.select();
                    }
                    break;
                case EXTERNAL:
                default:
                    NativeLaunchUtils.openURL(application.addClientInfoToUrl(result.getUrl()));
                }
            }
        }
    }
    
}
