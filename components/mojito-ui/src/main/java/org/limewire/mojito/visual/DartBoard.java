package org.limewire.mojito.visual;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import org.limewire.mojito.messages.DHTMessage.OpCode;

public class DartBoard extends Painter {
    
    private static final float DOT_SIZE = 6f;
    
    private static final long ATTACK = 250L;
    
    private static final long RELEASE = 2750L;
    
    private static final long DURATION = ATTACK + RELEASE;
    
    private static final int RESOLUTION = 16;
    static {
        assert RESOLUTION > 1 && RESOLUTION < 31;
    }
    
    private final List<Node> nodes = new LinkedList<Node>();
    
    private final Ellipse2D.Double ellipse = new Ellipse2D.Double();
    
    private final Ellipse2D.Double dot = new Ellipse2D.Double();
    
    private final Point2D.Double localhost = new Point2D.Double();
    
    private final KUID nodeId;
    
    public DartBoard(KUID nodeId) {
        this.nodeId = nodeId;
    }
    
    @Override
    public void paint(Component c, Graphics2D g) {
        double width = c.getWidth();
        double height = c.getHeight();
        
        double gap = 50d;
        double radius = Math.max(Math.min(width/2d, height/2d) - gap, gap);
        
        double arc_x = width/2d-radius;
        double arc_y = height/2d-radius;
        double arc_width = 2d*radius;
        double arc_height = arc_width;
        
        g.setColor(Color.orange);
        g.setStroke(TWO_PIXEL_STROKE);
        
        ellipse.setFrame(arc_x, arc_y, arc_width, arc_height);
        g.draw(ellipse);
        
        double dx = width/2d;
        double dy = height/2d;
        for (int i = 0; i < RESOLUTION; i++) {
            g.setColor(Color.green); // what else
            g.setStroke(DEFAULT_STROKE);
            int innerWidth = (int)(arc_width * i / RESOLUTION);
            int innerHeight = (int)(arc_height *  i / RESOLUTION);
            g.drawOval((int)dx - innerWidth / 2, (int)dy - innerHeight /2 , innerWidth, innerHeight);
        }
        
        dot.setFrame(dx - DOT_SIZE/2d, dy - DOT_SIZE/2d, 
                DOT_SIZE, DOT_SIZE);
        
        localhost.setLocation(dx, dy);
        
        synchronized (nodes) {
            for (Iterator<Node> it = nodes.iterator(); it.hasNext(); ) {
                if (it.next().paint(localhost, width, height, radius, g)) {
                    it.remove();
                }
            }
        }
        
        g.setColor(Color.orange);
        g.setStroke(DEFAULT_STROKE);
        g.fill(dot);
    }
    
    @Override
    public void handle(EventType type, KUID nodeId, SocketAddress dst, OpCode opcode, boolean request) {
        if (nodeId == null) {
            return;
        }
        
        synchronized (nodes) {
            nodes.add(new Node(type, nodeId.xor(this.nodeId), opcode, request));
        }
    }
    
    @Override
    public void clear() {
        synchronized (nodes) {
            nodes.clear();
        }
    }
    
    private static class Node {
        
        private final long timeStamp = System.currentTimeMillis();
        
        private final EventType type;
        
        private final KUID nodeId;
        
        private final boolean request;
        
        private final Stroke stroke;
        
        public Node(EventType type, KUID nodeId, OpCode opcode, boolean request) {
            this.type = type;
            this.nodeId = nodeId;
            this.request = request;
            
            this.stroke = getStrokeForOpCode(opcode);
        }
        
        private int alpha() {
            long delta = System.currentTimeMillis() - timeStamp;
            
            if (delta < ATTACK) {
                return (int)(255f/ATTACK * delta);
            }
            
            return Math.max(255 - (int)(255f/DURATION * delta), 0);
        }
        
        public boolean paint(Point2D.Double localhost, 
                double width, double height, double radius, Graphics2D g) {
            
            int power = 0;
            byte []temp = nodeId.toBigInteger().toByteArray();
            byte []small = new byte[4];
            System.arraycopy(temp,0, small, 0, 4);
            BigInteger smallBI = new BigInteger(small);
            int intId = smallBI.intValue() >>> (31 - RESOLUTION);
            assert intId >= 0;
            while (intId > 0) {
                intId >>= 1;
                power++;
            }
            power = Math.max(0,power-1);
            assert power <= RESOLUTION;
            double distance = power * radius / RESOLUTION;
            
            BigInteger twoPower = BigInteger.ZERO.setBit(power + 160 - RESOLUTION);
            BigDecimal angleBD = new BigDecimal(nodeId.toBigInteger().subtract(twoPower));
            angleBD = angleBD.divide(new BigDecimal(twoPower));
            double angle = angleBD.doubleValue() * 360;
            
            double x1 = localhost.x;
            double y1 = localhost.y;
            double x2 = x1 + Math.cos(angle) * distance;
            double y2 = y1 + Math.sin(angle) * distance;
            
            int red = 0;
            int green = 0;
            int blue = 0;
            
            if (type.equals(EventType.MESSAGE_SENT)) {
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

            // this is pointless, but pretty
            int size = (int)(Math.random() * RESOLUTION);
            
            g.setColor(new Color(red, green, blue, alpha()));
            g.setStroke(stroke);
            g.drawOval((int)x2 - size/2 , (int)y2 - size/2 , size, size);
            
            return System.currentTimeMillis() - timeStamp >= DURATION;
        }
    }
}
