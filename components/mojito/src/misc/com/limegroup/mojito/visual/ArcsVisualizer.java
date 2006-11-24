package com.limegroup.mojito.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageDispatcher.MessageDispatcherEvent;
import com.limegroup.mojito.io.MessageDispatcher.MessageDispatcherListener;
import com.limegroup.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;

public class ArcsVisualizer extends JPanel implements MessageDispatcherListener {

    private static final int SCALE = 10;
    
    private static final long SLEEP = 100L;
    
    private static final long DURATION = 3000L;
    
    private static final BigDecimal MAX_ID = new BigDecimal(KUID.MAXIMUM.toBigInteger(), SCALE);
    
    private static final float FONT_SIZE = 24f;
    private static final float DOT_SIZE = 6f;
    
    private List<Node> nodes = new ArrayList<Node>();
    
    private BigDecimal nodeId;
    
    private volatile boolean running = true;
    
    public static ArcsVisualizer show(final Context context) {
        final ArcsVisualizer arcs = new ArcsVisualizer(context.getLocalNodeID());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame(context.getName());
                frame.getContentPane().add(arcs);
                frame.setBounds(20, 30, 640, 640);
                frame.addWindowListener(new WindowListener() {
                    public void windowActivated(WindowEvent e) {}
                    public void windowClosed(WindowEvent e) {}
                    public void windowClosing(WindowEvent e) {
                        arcs.running = false;
                        context.getMessageDispatcher().removeMessageDispatcherListener(arcs);
                    }
                    public void windowDeactivated(WindowEvent e) {}
                    public void windowDeiconified(WindowEvent e) {}
                    public void windowIconified(WindowEvent e) {}
                    public void windowOpened(WindowEvent e) {}
                });
                frame.setVisible(true);
            }
        });
        
        context.getMessageDispatcher().addMessageDispatcherListener(arcs);
        return arcs;
    }
    
    public ArcsVisualizer(KUID nodeId) {
        this.nodeId = new BigDecimal(nodeId.toBigInteger(), SCALE);
        
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
        g2.drawString("Output", width-width/4f-fm.stringWidth("Output")/2f, 30);
        
        g2.setColor(Color.orange);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(new Line2D.Float(width/2f, 0f, width/2f, height));
        
        float x = (width - DOT_SIZE)/2f;
        float y = position(nodeId)-DOT_SIZE/2f;
        
        synchronized (nodes) {
            for (Iterator<Node> it = nodes.iterator(); it.hasNext(); ) {
                if (it.next().paint(x, y, width, height, g2)) {
                    it.remove();
                }
            }
        }
        
        g2.setColor(Color.orange);
        g2.fill(new Ellipse2D.Float(x, y, DOT_SIZE, DOT_SIZE));
    }
    
    private float position(KUID nodeId) {
        return position(nodeId, getHeight());
    }
    
    private float position(BigDecimal nodeId) {
        return position(nodeId, getHeight());
    }
    
    private static float position(KUID nodeId, float scale) {
        return position(new BigDecimal(nodeId.toBigInteger(), SCALE), scale);
    }
    
    private static float position(BigDecimal nodeId, float scale) {
        return nodeId.divide(MAX_ID, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(scale)).floatValue();
    }
    
    public void handleMessageDispatcherEvent(MessageDispatcherEvent evt) {
        EventType type = evt.getEventType();
        KUID nodeId = evt.getNodeID();
        
        if (type.equals(EventType.MESSAGE_RECEIVED)) {
            nodeId = evt.getMessage().getContact().getNodeID();
        } else if (type.equals(EventType.MESSAGE_SEND)) {
            nodeId = evt.getNodeID();
        }
        
        if (nodeId == null) {
            return;
        }
        
        synchronized (nodes) {
            nodes.add(new Node(type, nodeId));
        }
    }
    
    private static class Node {
        
        private static final Stroke STROKE = new BasicStroke(1.0f);
        
        private EventType type;
        
        private KUID nodeId;
        
        private long timeStamp = System.currentTimeMillis();
        
        public Node(EventType type, KUID nodeId) {
            this.type = type;
            this.nodeId = nodeId;
        }
        
        private int alpha() {
            long delta = System.currentTimeMillis() - timeStamp;
            if (delta < DURATION) {
                return 255 - (int)(255f/DURATION * delta);
            }
            return 0;
        }
        
        private float extent() {
            long delta = System.currentTimeMillis() - timeStamp;
            if (delta < DURATION/3L) {
                return 3f * 180f/DURATION * delta;
            }
            return 180f;
        }
        
        public boolean paint(float localX, float localY, float width, float height, Graphics2D g) {
            
            float nodeY = position(nodeId, height)-DOT_SIZE/2f;
            float distance = Math.max(localY, nodeY) - Math.min(localY, nodeY);
            float bow = width * distance/height;
            float nodeX = (width-bow)/2f;
            
            float arcX = nodeX;
            float arcY = (localY < nodeY) ? nodeY-distance : nodeY;
            
            float start = 0f;
            float extent = 0f;
            
            if (type.equals(EventType.MESSAGE_SEND)) {
                g.setColor(new Color(255, 0, 0, alpha()));
                if (localY < nodeY) {
                    start = 90f;
                    extent = -extent();
                } else {
                    start = -90f;
                    extent = extent();
                }
            } else {
                g.setColor(new Color(0, 255, 0, alpha()));
                if (localY < nodeY) {
                    start = -90f;
                    extent = -extent();
                } else {
                    start = 90f;
                    extent = extent();
                }
            }

            g.setStroke(STROKE);
            g.draw(new Arc2D.Float(arcX, arcY+DOT_SIZE/2f, bow, distance, start, extent, Arc2D.OPEN));
            
            return (System.currentTimeMillis() - timeStamp) >= DURATION;
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new ArcsVisualizer(KUID.createRandomID()));
        frame.setBounds(20, 30, 640, 480);
        frame.setVisible(true);
    }
}
