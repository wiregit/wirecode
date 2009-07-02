package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.Timer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.BusyPainter;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.friends.actions.BrowseOrLoginAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class FriendsButton extends LimeComboBox {
    
    @Resource private Icon friendOnlineIcon;
    @Resource private Icon friendOfflineIcon;
    @Resource private Icon friendLoadingIcon;
    @Resource private Icon friendOnlineSelectedIcon;
    @Resource private Icon friendOfflineSelectedIcon;
    @Resource private Icon friendLoadingSelectedIcon;
    
    private final BusyPainter busyPainter;
    
    private Timer busy;
    
    @Inject
    public FriendsButton(ComboBoxDecorator comboBoxDecorator, BrowseOrLoginAction browseOrLoginAction) {
     
        GuiUtils.assignResources(this);
        
        comboBoxDecorator.decorateIconComboBox(this);
        setToolTipText(I18n.tr("Friends"));
        setText(null);
        setIconTextGap(1);

        setForegroundPainter(new AbstractPainter<JXButton>() {
            @Override
            protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
                Icon icon;
                if (object.getModel().isPressed()) {
                    icon = object.getPressedIcon();
                } else {
                    icon = object.getIcon();
                }
                icon.paintIcon(object, g, 0, (height-icon.getIconHeight())/2);
                
                // the width/heigth offset is very specific to the loading img.
                if(busy != null) {
                    g.translate(21, 22);
                    busyPainter.paint(g, object, width, height);
                    g.translate(-21, -22);
                }
            }
        });
        setPopupPosition(new Point(0, -4));  
        
        busyPainter = new BusyPainter() {
            @Override
            protected void init(Shape point, Shape trajectory, Color b, Color h) {
                super.init(getScaledDefaultPoint(8), 
                        getScaledDefaultTrajectory(8),
                        Color.decode("#acacac"),  Color.decode("#545454"));
            }
        };
        
        addActionListener(browseOrLoginAction);
    }
    
    private void setIconFromType(FriendConnectionEvent.Type eventType) {
        if(eventType == null) {
            eventType = FriendConnectionEvent.Type.DISCONNECTED;
        }
        
        switch(eventType) {
        case CONNECT_FAILED:
        case DISCONNECTED:
            setIcon(friendOfflineIcon);
            setPressedIcon(friendOfflineSelectedIcon);
            stopAnimation();
            break;
        case CONNECTED:
            setIcon(friendOnlineIcon);
            setPressedIcon(friendOnlineSelectedIcon);
            stopAnimation();
            break;
        case CONNECTING:
            setIcon(friendLoadingIcon);
            setPressedIcon(friendLoadingSelectedIcon);
            startAnimation();
        }
    }

    
    @Inject
    void register(EventBean<FriendConnectionEvent> connectBean, ListenerSupport<FriendConnectionEvent> connectionSupport) {
        setIconFromType(EventUtils.getType(connectBean));        
        connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                setIconFromType(event.getType());
            }
        });
    }
    
    
    // animation code ripped from JXBusyLabel
    private void startAnimation() {
        if(busy != null) {
            stopAnimation();
        }
        
        busy = new Timer(100, new ActionListener() {
            int frame = busyPainter.getPoints();
            public void actionPerformed(ActionEvent e) {
                frame = (frame+1)%busyPainter.getPoints();
                busyPainter.setFrame(frame);
                repaint();
            }
        });
        busy.start();
    }    
    
    private void stopAnimation() {
        if (busy != null) {
            busy.stop();
            busyPainter.setFrame(-1);
            repaint();
            busy = null;
        }
    }
}
