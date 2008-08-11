package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.ui.swing.nav.NavTree;
import org.limewire.ui.swing.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.sponsored.SponsoredResult.LinkTarget;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class SponsoredResultsPanel extends JPanel {
    
  
   private NavTree navTree;
    
    private GridBagConstraints gbc = new GridBagConstraints();
    
    public SponsoredResultsPanel(NavTree navTree) {
        this.navTree = navTree;
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
            link.setUnclickedColor(Color.BLUE);
            link.setText(result.getTitle());
            FontUtils.changeSize(link, -2);
            link.addActionListener(new SponsoredResultListener(result));
            
            JTextArea textArea = new JTextArea(result.getText());
            textArea.setEditable(false);
            FontUtils.changeSize(textArea, -2);
            
            JLabel urlLabel = new JLabel(result.getVisibleUrl());
            urlLabel.setForeground(Color.GREEN.darker());
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
           if(result.getTarget() == LinkTarget.EXTERNAL){
               NativeLaunchUtils.openURL(result.getUrl());
           } else {
               navTree.showStore();
               throw new RuntimeException("navigate MozSwing to "+result.getUrl());
           }
        }
        
    }
    
}
