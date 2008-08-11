package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.nav.NavTree;
import org.limewire.ui.swing.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.sponsored.SponsoredResult.LinkTarget;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class SponsoredResultsPanel extends JPanel {
    
  
   private NavTree navTree;
   

   @Resource
   private Color linkColor = Color.BLUE;
   @Resource
   private Color visibleUrlColor = Color.GREEN.darker();
    
    private GridBagConstraints gbc = new GridBagConstraints();


    private StorePanel storePanel;
    
    public SponsoredResultsPanel(NavTree navTree, StorePanel storePanel) {
        this.navTree = navTree;
        this.storePanel = storePanel;
        setLayout(new GridBagLayout());
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        
        JLabel title = new JLabel("Sponsored Results");
        FontUtils.changeSize(title, 2);
        FontUtils.bold(title);
        add(title, gbc);
        
        gbc.insets.top = 10; // leave space above each entry that follow
        gbc.insets.left = 5; // indent entries a little
    }
   

    public void addEntry(SponsoredResult result){
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
            FontUtils.changeSize(link, -1);
            link.addActionListener(new SponsoredResultListener(result));
            
            JTextArea textArea = new JTextArea(result.getText());
            textArea.setEditable(false);
            FontUtils.changeSize(textArea, -3);
            
            JLabel urlLabel = new JLabel(result.getVisibleUrl());
            urlLabel.setForeground(visibleUrlColor);
            FontUtils.changeSize(urlLabel, -2);
            
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
            if (result.getTarget() == LinkTarget.EXTERNAL) {
                NativeLaunchUtils.openURL(result.getUrl());
            } else {
                storePanel.load(result.getUrl());
                navTree.showStore();
            }
        }
        
    }
    
}
