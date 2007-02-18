package org.limewire.mojito.visual;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
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
    
    private static final long ATTACK = 250L;
    
    private static final long RELEASE = 2750L;
    
    private static final long DURATION = ATTACK + RELEASE;
    
    private static final Stroke ONE_PIXEL_STROKE = new BasicStroke(1.0f);
    
    private static final Stroke TWO_PIXEL_STROKE = new BasicStroke(2.0f);
   
    private static final BigDecimal MAX_ID = new BigDecimal(KUID.MAXIMUM.toBigInteger(), SCALE);
    
    private static final float FONT_SIZE = 24f;
    private static final float DOT_SIZE = 6f;
    
    private final BigDecimal nodeId;
    
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
    
    private final Object lock = new Object();
    
    private Painter painter = new PainterOne();
    
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
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                synchronized (lock) {
                    painter.clear();
                    
                    if (painter instanceof PainterOne) {
                        painter = new PainterTwo();
                    } else {
                        painter = new PainterOne();
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
    
    private static double position(KUID nodeId, double scale) {
        return position(new BigDecimal(nodeId.toBigInteger(), SCALE), scale);
    }
    
    private static double position(BigDecimal nodeId, double scale) {
        return nodeId.divide(MAX_ID, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(scale)).doubleValue();
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
        synchronized (lock) {
            painter.handle(type, nodeId, request);
        }
    }
    
    private static interface Painter {
        public void paint(Component c, Graphics2D g);
        
        public void handle(EventType type, KUID nodeId, boolean request);
        
        public void clear();
    }
    
    /**
     * Draws the key space as a line
     */
    private class PainterOne implements Painter {

        private final List<ArcNode> nodes = new LinkedList<ArcNode>();
        
        private final Ellipse2D.Double dot = new Ellipse2D.Double();
        
        public void paint(Component c, Graphics2D g2) {
            int width = c.getWidth();
            int height = c.getHeight();
            
            g2.setColor(Color.orange);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new Line2D.Float(width/2f, 0f, width/2f, height));
            
            double x = (width - DOT_SIZE)/2f;
            double y = position(nodeId, height)-DOT_SIZE/2f;
            
            synchronized (nodes) {
                for (Iterator<ArcNode> it = nodes.iterator(); it.hasNext(); ) {
                    if (it.next().paint(x, y, width, height, g2)) {
                        it.remove();
                    }
                }
            }
            
            g2.setColor(Color.orange);
            dot.setFrame(x, y, DOT_SIZE, DOT_SIZE);
            g2.fill(dot);
        }
        
        public void handle(EventType type, KUID nodeId, boolean request) {
            synchronized (nodes) {
                nodes.add(new ArcNode(type, nodeId, request));
            }
        }
        
        public void clear() {
            synchronized (nodes) {
                nodes.clear();
            }
        }
    }
    
    private static class ArcNode {
        
        private final EventType type;
        
        private final KUID nodeId;
        
        private final boolean request;
        
        private final long timeStamp = System.currentTimeMillis();
        
        private final Arc2D.Double arc = new Arc2D.Double();
        
        public ArcNode(EventType type, KUID nodeId, boolean request) {
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
        
        private double extent() {
            long delta = System.currentTimeMillis() - timeStamp;
            if (delta < DURATION/3L) {
                return 3d * 180f/DURATION * delta;
            }
            return 180d;
        }
        
        public boolean paint(double localX, double localY, double width, double height, Graphics2D g) {
            
            if (nodeId != null) {
                paintArc(localX, localY, width, height, g);
            } else {
                paintLine(localX, localY, width, height, g);
            }
            
            return (System.currentTimeMillis() - timeStamp) >= DURATION;
        }
        
        private void paintArc(double localX, double localY, double width, double height, Graphics2D g) {
            
            double nodeY = position(nodeId, height)-DOT_SIZE/2f;
            double distance = Math.max(localY, nodeY) - Math.min(localY, nodeY);
            double bow = distance;
            double nodeX = (width-bow)/2f;
            
            double arcX = nodeX;
            double arcY = (localY < nodeY) ? nodeY-distance : nodeY;
            
            double start = 0f;
            double extent = 0f;
            
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

            g.setStroke(ONE_PIXEL_STROKE);
            g.setColor(new Color(red, green, blue, alpha()));
            arc.setArc(arcX, arcY+DOT_SIZE/2f, bow, distance, start, extent, Arc2D.OPEN);
            g.draw(arc);
        }
        
        private void paintLine(double localX, double localY, double width, double height, Graphics2D g) {
            g.setStroke(ONE_PIXEL_STROKE);
            g.setColor(new Color(255, 0, 0, alpha()));
            
            double x1 = localX;
            double y1 = localY + DOT_SIZE/2f;
            double x2 = x1 + (width/(2f*180f)) * extent();
            double y2 = y1;
            g.draw(new Line2D.Double(x1, y1, x2, y2));
        }
    }
    
    /**
     * Draws the key space as a cyrcle
     */
    private class PainterTwo implements Painter {

        private final List<CurveNode> nodes = new LinkedList<CurveNode>();
        
        private final Point2D.Double localhost = new Point2D.Double();
        
        private final Ellipse2D.Double ellipse = new Ellipse2D.Double();
        
        private final Ellipse2D.Double dot = new Ellipse2D.Double();
        
        public void paint(Component c, Graphics2D g2) {
            double width = c.getWidth();
            double height = c.getHeight();
            
            double gap = 50d;
            double radius = Math.max(Math.min(width/2d, height/2d) - gap, gap);
            
            double arc_x = width/2d-radius;
            double arc_y = height/2d-radius;
            double arc_width = 2d*radius;
            double arc_height = arc_width;
            
            g2.setColor(Color.orange);
            g2.setStroke(TWO_PIXEL_STROKE);
            
            ellipse.setFrame(arc_x, arc_y, arc_width, arc_height);
            g2.draw(ellipse);
            
            double fi = position(nodeId, 2d*Math.PI) - Math.PI/2d;
            double dx = width/2d + radius * Math.cos(fi);
            double dy = height/2d + radius * Math.sin(fi);
            
            localhost.setLocation(dx, dy);
            
            synchronized (nodes) {
                for (Iterator<CurveNode> it = nodes.iterator(); it.hasNext(); ) {
                    if (it.next().paint(localhost, width, height, radius, g2)) {
                        it.remove();
                    }
                }
            }
            
            g2.setColor(Color.orange);
            dot.setFrame(dx - DOT_SIZE/2d, dy - DOT_SIZE/2d, 
                    DOT_SIZE, DOT_SIZE);
            g2.fill(dot);
        }
        
        public void handle(EventType type, KUID nodeId, boolean request) {
            if (nodeId == null) {
                return;
            }
            
            synchronized (nodes) {
                nodes.add(new CurveNode(type, nodeId, request));
            }
        }
        
        public void clear() {
            synchronized (nodes) {
                nodes.clear();
            }
        }
    }
    
    private static class CurveNode {
        
        private static final Random GENERATOR = new Random();
        
        private final EventType type;
        
        private final KUID nodeId;
        
        private final boolean request;
        
        private final int noise;
        
        private final long timeStamp = System.currentTimeMillis();
        
        private final Point2D.Double remote = new Point2D.Double();
        
        private final Point2D.Double controlpoint = new Point2D.Double();
        
        private final QuadCurve2D.Double curve = new QuadCurve2D.Double();
        
        public CurveNode(EventType type, KUID nodeId, boolean request) {
            this.type = type;
            this.nodeId = nodeId;
            this.request = request;
            
            int noise = GENERATOR.nextInt(50);
            if (GENERATOR.nextBoolean()) {
                noise = -noise;
            }
            this.noise = noise;
        }
        
        private int alpha() {
            long delta = System.currentTimeMillis() - timeStamp;
            
            if (delta < ATTACK) {
                return (int)(255f/ATTACK * delta);
            }
            
            return Math.max(255 - (int)(255f/DURATION * delta), 0);
        }
        
        public boolean paint(Point2D.Double localhost, 
                double width, double height, double radius, Graphics2D g2) {
            
            double cx = width/2d;
            double cy = height/2d;
            
            double fi = position(nodeId, 2d*Math.PI) - Math.PI/2d;
            
            double dx = cx + radius * Math.cos(fi);
            double dy = cy + radius * Math.sin(fi);
            
            int red = 0;
            int green = 0;
            int blue = 0;
            
            if (type.equals(EventType.MESSAGE_SEND)) {
                red = 255;
                if (!request) {
                    blue = 255;
                }
            } else {
                green = 255;
                if (request) {
                    blue = 255;
                }
            }
            
            remote.setLocation(dx, dy);
            controlpoint.setLocation(cx+noise, cy+noise);
            curve.setCurve(localhost, controlpoint, remote);
            
            g2.setStroke(ONE_PIXEL_STROKE);
            g2.setColor(new Color(red, green, blue, alpha()));
            g2.draw(curve);
            
            return System.currentTimeMillis() - timeStamp >= DURATION;
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
            
            arcs.painter.handle(type, nodeId, true);
            
            // Sleep a bit...
            //Thread.sleep(sleep);
            Thread.sleep(generator.nextInt(sleep));
            
            // Send every now an then a response
            if (generator.nextBoolean()) {
                if (type.equals(EventType.MESSAGE_SEND)) {
                    arcs.painter.handle(EventType.MESSAGE_RECEIVED, nodeId, false);
                } else {
                    arcs.painter.handle(EventType.MESSAGE_SEND, nodeId, false);
                }
            }
        }
    }
}
