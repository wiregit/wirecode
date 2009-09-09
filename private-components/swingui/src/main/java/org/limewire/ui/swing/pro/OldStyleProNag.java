package org.limewire.ui.swing.pro;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.statusbar.ProStatusPanel;
import org.limewire.ui.swing.statusbar.ProStatusPanel.InvisibilityCondition;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;

/**
 * This is a throw back to the 4.x style upgrade to Pro popup.
 */
public class OldStyleProNag extends JPanel {
    
    @Resource
    private Icon backgroundIcon;
    @Resource
    private Color backgroundColor;
    @Resource
    private Font regularFont;
    @Resource
    private Font titleFont;
    
    private final Application application;
    private final ProStatusPanel proStatusPanel;
    private final JButton yesButton;
    
    private String defaultURL = "http://www.limewire.com/download/pro/?";
    
    @Inject
    public OldStyleProNag(Application application, 
            ProStatusPanel proStatusPanel) {
        this.application = application;
        this.proStatusPanel = proStatusPanel;
        
        GuiUtils.assignResources(this);
        
        setBackground(backgroundColor);
        
        setLayout(new MigLayout("fill, insets 10 10 4 10, gap 7 5"));
                
        add(createContentArea(), 
                "span, grow, wrap");
        
        JLabel label = new JLabel(I18n.tr("Upgrade to LimeWire PRO?"));
        add(label, "span, wrap, alignx center");
                
        yesButton = createButton(new YesAction());
        yesButton.requestFocusInWindow();
        JButton whyButton = createButton(new WhyAction());
        JButton laterButton = createButton(new NoAction());
        
        add(yesButton, "alignx center, split 3");
        add(whyButton, "alignx center");
        add(laterButton, "alignx center");
        
        proStatusPanel.addCondition(InvisibilityCondition.PRO_ADD_SHOWN);
    }
    
    /**
     * Returns the default button that should be selected in the dialog.
     */
    public JButton getDefaultButton() {
        return yesButton;
    }
    
    private JButton createButton(Action action) {
        JButton button = new JButton(action);
        button.setFocusPainted(false);
        return button;
    }
    
    private JComponent createContentArea() {
        JLabel line1 = new JLabel("<HTML>" + I18n.tr("Upgrade to PRO today!") + "</HTML>");
        JLabel line2 = new JLabel("<HTML>" + I18n.tr("Turbo-charged downloads") + "</HTML>");
        JLabel line3 = new JLabel("<HTML>" + I18n.tr("More search results") + "</HTML>");
        JLabel line4 = new JLabel("<HTML>" + I18n.tr("Free tech support and upgrades") + "</HTML>");
        
        //set fonts
        line1.setFont(titleFont);
        line2.setFont(regularFont);
        line3.setFont(regularFont);
        line4.setFont(regularFont);
        
        JPanel textPanel = new IconPanel((ImageIcon)backgroundIcon);
        textPanel.setLayout(new MigLayout("insets 0, gap 0, nogrid, novisualpadding, aligny center"));

        //must be false to view the background image
        textPanel.setOpaque(false);
        //shift the text so as to not paint over the image
        textPanel.setBorder(new EmptyBorder(5,110,5,0));        
        
        textPanel.add(line1, "wrap");
        textPanel.add(line2, "wrap");
        textPanel.add(line3, "wrap");
        textPanel.add(line4);       
        
        JScrollPane scroller = new JScrollPane(textPanel);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setPreferredSize(new Dimension(350, 115));
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scroller;
    }
    
    /**
     * Closes the popup dialog and shows the status bar pro add.
     */
    private void close() {
        Window window = SwingUtilities.getWindowAncestor(OldStyleProNag.this);
        window.setVisible(false);
        window.dispose();
        
        proStatusPanel.removeCondition(InvisibilityCondition.PRO_ADD_SHOWN);  
    }
    
    /**
     * Opens a url to the pro purchase page and closes the popup.
     */
    private class YesAction extends AbstractAction {
        public YesAction() {
            super(I18n.tr("Yes"));
            
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Get LimeWire PRO Now"));        
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            String ref = "ref=";
            if(InstallSettings.isRandomNag()) {
                ref += "lwn2";    
            } else { // InstallSettings.NagStyles.MODAL
                ref += "lwn3";
            }
            NativeLaunchUtils.openURL(application.addClientInfoToUrl(defaultURL + ref));
            close();
        }
    }
    
    /**
     * Closes the popup without any url action.
     */
    private class NoAction extends AbstractAction {
        public NoAction() {
            super(I18n.tr("Later"));
            
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Get LimeWire PRO Later"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }
    
    /**
     * Opens URL to why page and closes the popup.
     */
    private class WhyAction extends AbstractAction {
        public WhyAction() {
            super(I18n.tr("Why"));
        
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("What does PRO give me?"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String ref = "ref=";
            if(InstallSettings.isRandomNag()) {
                ref += "lwn4";    
            } else { // InstallSettings.NagStyles.MODAL
                ref += "lwn5";
            }
            NativeLaunchUtils.openURL(application.addClientInfoToUrl(defaultURL + ref));
            close();
        }
    }
    
    /**JPanel that paints an image as its background.  The panel is non-opaque by default and the 
     * preferred size is the size of the image.*/
    private static class IconPanel extends JPanel {
        private final Image image;
        
        public IconPanel(ImageIcon iconImage){
            image = iconImage.getImage();
            setPreferredSize(new Dimension(iconImage.getIconWidth(), iconImage.getIconHeight()));
        }
        
        @Override
        public void paintComponent(Graphics g){        
            if( image != null && image.getWidth(this) > 0 && image.getHeight(this) > 0) {
                g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
            }
            
            super.paintComponents(g);
        }
    }
}
