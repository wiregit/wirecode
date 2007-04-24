package org.limewire.mojito.visual;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherListener;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import org.limewire.mojito.messages.RequestMessage;
import org.limewire.mojito.messages.DHTMessage.OpCode;

/**
 * A small framework to paint different representations of the DHT
 */
public class ArcsVisualizer extends JPanel implements MessageDispatcherListener {

    private static final long SLEEP = 100L;
    
    private static final float FONT_SIZE = 24f;
    
    private volatile boolean running = true;
    
    public static ArcsVisualizer show(final Context context) {
        final ArcsVisualizer arcs = new ArcsVisualizer(context.getLocalNodeID());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame(context.getName());
                frame.getContentPane().add(arcs);
                frame.setBounds(20, 30, 640, 640);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        arcs.running = false;
                        context.getMessageDispatcher().removeMessageDispatcherListener(arcs);
                    }
                });
                frame.setVisible(true);
            }
        });
        
        context.getMessageDispatcher().addMessageDispatcherListener(arcs);
        return arcs;
    }
    
    private final Object lock = new Object();
    
    private Painter painter;
    
    public ArcsVisualizer(final KUID nodeId) {
        
        painter = new SnowMan(nodeId);
        
        Runnable repaint = new Runnable() {
            public void run() {
                while(running) {
                    repaint();
                    
                    try { 
                        Thread.sleep(SLEEP); 
                    } catch (InterruptedException err) {}
                }
            }
        };
        
        Thread thread = new Thread(repaint);
        thread.setDaemon(true);
        thread.start();
        
        // Make sure the JPanel is focusable or it won't fire 
        // FocusEvent and Component's hasFocus() method returns 
        // always false
        setFocusable(true);
        
        addMouseListener(new MouseAdapter() {
            
            private volatile boolean hadFocus = false;
            
            @Override
            public void mousePressed(MouseEvent e) {
                hadFocus = e.getComponent().hasFocus();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!hadFocus) {
                    return;
                }
                
                synchronized (lock) {
                    painter.clear();
                    
                    if (painter instanceof SnowMan) {
                        painter = new PlasmaLamp(nodeId);
                    } else {
                        painter = new SnowMan(nodeId);
                    }
                }
            }
        });
    }
    
    public void paint(Graphics g) {
        float width = getWidth();
        float height = getHeight();
        
        Graphics2D g2 = (Graphics2D)g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(Color.black);
        g2.fill(new Rectangle2D.Float(0, 0, width, height));
        
        g2.setFont(getFont().deriveFont(FONT_SIZE));
        g2.setColor(Color.green);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("Input", width/4f - fm.stringWidth("Input")/2f, 30);
        g2.setColor(Color.red);
        g2.drawString("Output", width-width/4f - fm.stringWidth("Output")/2f, 30);
        
        g2.setFont(getFont().deriveFont(FONT_SIZE/2));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(0, 255, 0));
        g2.drawString("response", width/4f - fm.stringWidth("response")/2f, 48);
        g2.setColor(new Color(0, 255, 255));
        g2.drawString("request", width/4f - fm.stringWidth("request")/2f, 60);
        
        g2.setColor(new Color(255, 0, 0));
        g2.drawString("request", width-width/4f - fm.stringWidth("request")/2f, 48);
        g2.setColor(new Color(255, 0, 255));
        g2.drawString("response", width-width/4f - fm.stringWidth("response")/2f, 60);
        
        synchronized (lock) {
            painter.paint(this, g2);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.io.MessageDispatcher.MessageDispatcherListener#handleMessageDispatcherEvent(org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent)
     */
    public void handleMessageDispatcherEvent(MessageDispatcherEvent evt) {
        EventType type = evt.getEventType();
        KUID nodeId = null;
        
        if (type.equals(EventType.MESSAGE_RECEIVED)) {
            nodeId = evt.getMessage().getContact().getNodeID();
        } else if (type.equals(EventType.MESSAGE_SEND)) {
            nodeId = evt.getNodeID();
        } else {
            return;
        }
        
        OpCode opcode = evt.getMessage().getOpCode();
        boolean request = (evt.getMessage() instanceof RequestMessage);
        synchronized (lock) {
            painter.handle(type, nodeId, opcode, request);
        }
    }
    
    public static void main(String[] args) throws Exception {
        ArcsVisualizer arcs = new ArcsVisualizer(KUID.createRandomID());
        
        JFrame frame = new JFrame();
        frame.getContentPane().add(arcs);
        frame.setBounds(20, 30, 640, 480);
        frame.setVisible(true);
        
        Random generator = new Random();
        
        EventType type = null;
        KUID nodeId = null;
        
        final int sleep = 100;
        
        while(true) {
            
            // Simulate an received or sent message
            nodeId = KUID.createRandomID();
            if (generator.nextBoolean()) {
                type = EventType.MESSAGE_RECEIVED;
            } else {
                type = EventType.MESSAGE_SEND;
            }
            
            OpCode opcode = OpCode.valueOf(1 + generator.nextInt(OpCode.values().length-1));
            //OpCode opcode = OpCode.FIND_NODE_REQUEST;
            
            arcs.painter.handle(type, nodeId, opcode, true);
            
            // Sleep a bit...
            //Thread.sleep(sleep);
            Thread.sleep(generator.nextInt(sleep));
            
            // Send every now an then a response
            if (generator.nextBoolean()) {
                if (type.equals(EventType.MESSAGE_SEND)) {
                    arcs.painter.handle(EventType.MESSAGE_RECEIVED, nodeId, opcode, false);
                } else {
                    arcs.painter.handle(EventType.MESSAGE_SEND, nodeId, opcode, false);
                }
            }
        }
    }
}
