package org.limewire.mojito.visual;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.math.BigDecimal;
import java.net.SocketAddress;

import org.limewire.mojito.KUID;
import org.limewire.mojito2.message.Message;
import org.limewire.mojito2.message.NodeRequest;
import org.limewire.mojito2.message.NodeResponse;
import org.limewire.mojito2.message.PingRequest;
import org.limewire.mojito2.message.PingResponse;
import org.limewire.mojito2.message.StoreRequest;
import org.limewire.mojito2.message.StoreResponse;
import org.limewire.mojito2.message.ValueRequest;
import org.limewire.mojito2.message.ValueResponse;

/**<p>
 * Draws graphical representations of the DHT. <code>Painter</code> creates 
 * different dash patterns depending on the DHT message type.
 * </p><p>
 * You need to implement:
 * <ul>
 * <li> {@link #paint(Component, Graphics2D)}: <code>paint</code> uses a 
 * {@link Graphics2D} object to draw.</li>
 * <li>    
 * <code>handle(EventType, KUID, SocketAddress, OpCode, boolean)</code>: 
 * takes care of messages sent or received, the socket
 * address and the DHT message code. <code>handle</code> 
 * can accumulate the information and later <code>paint</code> that information.</li>
 * <li>
 * {@link #clear()}: removes the accumulated information in which 
 * <code>handle</code> stores.</li>
 * </ul>
 */
public abstract class Painter {
    
    private static final int SCALE = 10;
    
    public static final BigDecimal MAX_ID = new BigDecimal(KUID.MAXIMUM.toBigInteger(), SCALE);
    
    public static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);
    
    public static final Stroke TWO_PIXEL_STROKE = new BasicStroke(2.0f);
    
    public abstract void paint(Component c, Graphics2D g);
    
    public abstract void handle(boolean outgoing, KUID nodeId, 
            SocketAddress dst, Message message);
    
    public abstract void clear();
    
    public static Stroke getStrokeForMessage(Message message) {
        float dash_phase = (float)Math.random() * 10f;
        
        if (message instanceof PingRequest
                || message instanceof PingResponse) {
            return new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
                    BasicStroke.JOIN_ROUND, 10.0f, 
                    new float[]{ 2f, 2f }, dash_phase);
        } else if (message instanceof NodeRequest
                || message instanceof NodeResponse) {
            return new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
                    BasicStroke.JOIN_ROUND, 10.0f, 
                    new float[]{ 1f, 5f, 5f }, dash_phase);
        } else if (message instanceof ValueRequest
                || message instanceof ValueResponse) {
            return new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
                    BasicStroke.JOIN_ROUND, 10.0f, 
                    new float[]{ 5f, 5f }, dash_phase);
        } else if (message instanceof StoreRequest
                || message instanceof StoreResponse) {
            return new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
                    BasicStroke.JOIN_ROUND, 10.0f, 
                    new float[]{ 5f, 3f }, dash_phase);
        } else {
            return DEFAULT_STROKE;
        }
    }
    
    public static double position(KUID nodeId, double scale) {
        return position(new BigDecimal(nodeId.toBigInteger(), SCALE), scale);
    }
    
    public static double position(BigDecimal nodeId, double scale) {
        return nodeId.divide(MAX_ID, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(scale)).doubleValue();
    }
}
