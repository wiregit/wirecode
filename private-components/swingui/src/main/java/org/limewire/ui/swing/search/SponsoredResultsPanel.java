package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.sponsored.SponsoredResultTarget;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

public class SponsoredResultsPanel extends JXPanel {
  
    private final Navigator navigator;
    private final StorePanel storePanel;

    @Resource
    private Color linkColor;

    @Resource
    private Color visibleUrlColor;
    
    @Resource
    private Font headingFont;
    
    @Resource 
    private Font bodyFont;
    
    private JLabel title;

    
    @Inject
    public SponsoredResultsPanel(Navigator navigator, StorePanel storePanel) {
        GuiUtils.assignResources(this);
        this.navigator = navigator;
        this.storePanel = storePanel;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        
        title = createTitleLabel();
        add(title, gbc);
    }
    
    void setTitleVisible(boolean visible) {
        title.setVisible(visible);
    }
    
    JLabel createTitleLabel() {
        JLabel title = new JLabel(I18n.tr("Sponsored Results"));
        title.setFont(headingFont);
        FontUtils.bold(title);
        return title;
    }
   

    public void addEntry(SponsoredResult result){
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.insets.top = 10; // leave space above each entry that follow
        gbc.insets.left = 5; // indent entries a little
        add(new SponsoredResultView(result), gbc);
    }
        
    private class SponsoredResultView extends JPanel {
        
        private SponsoredResult result;

        public SponsoredResultView(SponsoredResult result){
         super(new VerticalLayout());
            this.result = result;
            
            JXHyperlink link = new JXHyperlink();
            link.setUnclickedColor(linkColor);
            link.setText("<HTML><U>" + result.getTitle() + "</U></HTML>");
            link.setFont(bodyFont);
            link.addActionListener(new SponsoredResultListener(result));
            
            JTextArea textArea = new JTextArea(result.getText());
            textArea.setEditable(false);
            textArea.setFont(bodyFont);
            
            JLabel urlLabel = new JLabel(result.getVisibleUrl());
            urlLabel.setForeground(visibleUrlColor);
            urlLabel.setFont(bodyFont);
            
            add(link);
            add(textArea);
            add(urlLabel);
        }

        
        public SponsoredResult getSponsoredResult() {
            return result;
        } 
        
    }
    
    private class SponsoredResultListener implements ActionListener{

        private SponsoredResult result;
        public SponsoredResultListener(SponsoredResult result){
            this.result = result;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (result.getTarget() == SponsoredResultTarget.EXTERNAL) {
                NativeLaunchUtils.openURL(result.getUrl());
            } else {
                storePanel.load(result.getUrl());
                NavItem item = navigator.getNavItem(NavCategory.LIMEWIRE, StorePanel.NAME);
                if(item != null) {
                    item.select();
                }
            }
        }
        
    }
    
}
