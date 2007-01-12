package org.limewire.mojito.visual;

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


@SuppressWarnings("serial")
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
        KUID nodeId = null;
        
        if (type.equals(EventType.MESSAGE_RECEIVED)) {
            nodeId = evt.getMessage().getContact().getNodeID();
        } else if (type.equals(EventType.MESSAGE_SEND)) {
            nodeId = evt.getNodeID();
        } else {
            return;
        }
        
        boolean request = (evt.getMessage() instanceof RequestMessage);
        handle(type, nodeId, request);
    }
    
    private void handle(EventType type, KUID nodeId, boolean request) {
        synchronized (nodes) {
            nodes.add(new Node(type, nodeId, request));
        }
    }
    
    private static class Node {
        
        private static final Stroke STROKE = new BasicStroke(1.0f);
        
        private EventType type;
        
        private KUID nodeId;
        
        private boolean request;
        
        private long timeStamp = System.currentTimeMillis();
        
        
        public Node(EventType type, KUID nodeId, boolean request) {
            this.type = type;
            this.nodeId = nodeId;
            this.request = request;
            
            if (nodeId == null) {
                assert (request && type.equals(EventType.MESSAGE_SEND));
            }
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
            
            if (nodeId != null) {
                paintArc(localX, localY, width, height, g);
            } else {
                paintLine(localX, localY, width, height, g);
            }
            
            return (System.currentTimeMillis() - timeStamp) >= DURATION;
        }
        
        private void paintArc(float localX, float localY, float width, float height, Graphics2D g) {
            
            float nodeY = position(nodeId, height)-DOT_SIZE/2f;
            float distance = Math.max(localY, nodeY) - Math.min(localY, nodeY);
            float bow = distance;
            float nodeX = (width-bow)/2f;
            
            float arcX = nodeX;
            float arcY = (localY < nodeY) ? nodeY-distance : nodeY;
            
            float start = 0f;
            float extent = 0f;
            
            int red = 0;
            int green = 0;
            int blue = 0;
            
            if (type.equals(EventType.MESSAGE_SEND)) {
                red = 255;
                if (!request) {
                    blue = 255;
                }
                if (localY < nodeY) {
                    start = 90f;
                    extent = -extent();
                } else {
                    start = -90f;
                    extent = extent();
                }
            } else {
                green = 255;
                if (request) {
                    blue = 255;
                }
                if (localY < nodeY) {
                    start = -90f;
                    extent = -extent();
                } else {
                    start = 90f;
                    extent = extent();
                }
            }

            g.setStroke(STROKE);
            g.setColor(new Color(red, green, blue, alpha()));
            g.draw(new Arc2D.Float(arcX, arcY+DOT_SIZE/2f, bow, distance, start, extent, Arc2D.OPEN));
        }
        
        private void paintLine(float localX, float localY, float width, float height, Graphics2D g) {
            g.setStroke(STROKE);
            g.setColor(new Color(255, 0, 0, alpha()));
            
            float x1 = localX;
            float y1 = localY + DOT_SIZE/2f;
            float x2 = x1 + (width/(2f*180f)) * extent();
            float y2 = y1;
            g.draw(new Line2D.Float(x1, y1, x2, y2));
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
        
        final int sleep = 1000;
        
        while(true) {
            
            // Simulate an received or sent message
            nodeId = KUID.createRandomID();
            if (generator.nextBoolean()) {
                type = EventType.MESSAGE_RECEIVED;
            } else {
                type = EventType.MESSAGE_SEND;
            }
            
            arcs.handle(type, nodeId, true);
            
            // Sleep a bit...
            //Thread.sleep(sleep);
            Thread.sleep(generator.nextInt(sleep));
            
            // Send every now an then a response
            if (generator.nextBoolean()) {
                if (type.equals(EventType.MESSAGE_SEND)) {
                    arcs.handle(EventType.MESSAGE_RECEIVED, nodeId, false);
                } else {
                    arcs.handle(EventType.MESSAGE_SEND, nodeId, false);
                }
            }
        }
    }
}
