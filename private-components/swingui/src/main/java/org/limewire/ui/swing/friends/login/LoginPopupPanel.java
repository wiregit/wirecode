package org.limewire.ui.swing.friends.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class LoginPopupPanel extends Panel implements Resizable {

    @Resource private Dimension size;
    @Resource private Color background;
    @Resource private Color border;
    @Resource private Color titleBarBackground;
    @Resource private Color titleBarForeground;
    @Resource private Font titleBarFont;
    
    @Resource private Icon closeIcon;
    @Resource private Icon closeIconRollover;
    @Resource private Icon closeIconPressed;   
    
    private final Provider<ServiceSelectionLoginPanel> serviceSelectionLoginPanelProvider;
    private final XMPPUserEntryLoginPanelFactory xmppUserEntryLoginPanelFactory;
    
    private JXPanel frame = null;
    private JPanel contentPanel = null;
    
    @Inject
    public LoginPopupPanel(Provider<ServiceSelectionLoginPanel> serviceSelectionLoginPanelProvider,
            XMPPUserEntryLoginPanelFactory generalUserEntryLoginPanelFactory) {
        
        this.serviceSelectionLoginPanelProvider = serviceSelectionLoginPanelProvider;
        this.xmppUserEntryLoginPanelFactory = generalUserEntryLoginPanelFactory;
        
        GuiUtils.assignResources(this);
        
        setLayout(new BorderLayout());
        setVisible(false);
    }
    
    private void initContent() {
        frame = new JXPanel(new BorderLayout());
        frame.setPreferredSize(size);
        frame.setBackground(background);
        frame.setBorder(new LineBorder(border, 3));
        
        JLabel titleBarLabel = new JLabel(I18n.tr("File sharing with friends"));
        titleBarLabel.setOpaque(false);
        titleBarLabel.setFont(titleBarFont);
        titleBarLabel.setForeground(titleBarForeground);
          
        JXPanel headerBar = new JXPanel(new MigLayout("insets 0, gap 0, fill"));
        headerBar.setBackground(titleBarBackground);
        
        IconButton closeButton = new IconButton(closeIcon, closeIconRollover, closeIconPressed);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
            }
        });
        
        headerBar.add(titleBarLabel, "gapleft 3, gapbottom 3, dock west, growx");
        headerBar.add(closeButton, "gapright 2, gaptop 0, dock east, aligny top, pad -2 0 0 0");
        
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        frame.add(headerBar, BorderLayout.NORTH);
        frame.add(contentPanel, BorderLayout.CENTER);
       
        add(frame, BorderLayout.CENTER);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        if (frame == null && visible) {
            initContent();
        }
        
        if (visible) {
            start();
            resize();
            validate();
            frame.repaint();
        }
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = frame.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds((int)parentBounds.getWidth()/2-w/2,
                (int)parentBounds.getHeight()/2-h/2+20,
                w, h);
    }
    
    
    public void start() {
        contentPanel.add(serviceSelectionLoginPanelProvider.get(), BorderLayout.CENTER);
    }
    
    public void restart() {
        contentPanel.removeAll();
        start();
        contentPanel.repaint();
        contentPanel.validate();
    }
    
    public void finished() {
        this.setVisible(false);
        contentPanel.removeAll();
    }
    
    /**
     * Reports back a non xmmp service selection, (ie. facebook)
     */
    public void setSelectedService() {
        
    }
    
    /**
     * Reports back an XMMP service selection
     */
    public void setSelectedService(XMPPAccountConfiguration config) {
        contentPanel.removeAll();
        contentPanel.add(xmppUserEntryLoginPanelFactory.create(config), BorderLayout.CENTER);
        contentPanel.repaint();
        contentPanel.validate();
    }

}
