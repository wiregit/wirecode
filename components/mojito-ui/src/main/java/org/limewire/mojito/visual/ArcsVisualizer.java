package org.limewire.mojito.visual;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.limewire.mojito.DHT;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherListener;
import org.limewire.mojito.message.Message;
import org.limewire.mojito.routing.Contact;

/**
 * Paints different representations of the Distributed Hash Table (DHT). 
 * <code>ArcsVisualizer</code> is a small framework for visually representing 
 * the DHT messages distinguished by color and dash patterns.
 */
public class ArcsVisualizer extends JPanel {

    private static final int SLEEP = 100;
    
    private static final float FONT_SIZE = 24f;
    
    private final MessageDispatcherListener listener 
            = new MessageDispatcherListener() {
        @Override
        public void messageSent(KUID contactId, 
                SocketAddress dst, Message message) {
            
            if (contactId != null) {
                synchronized (lock) {
                    painter.handle(true, contactId, dst, message);
                }
            }
        }
        
        @Override
        public void messageReceived(Message message) {
            Contact contact = message.getContact();
            KUID contactId = contact.getContactId();
            SocketAddress src = contact.getContactAddress();
            
            synchronized (lock) {
                painter.handle(false, contactId, src, message);
            }
        }
    };
    
    private final List<Painter> painters = new ArrayList<Painter>();

    private final Object lock = new Object();

    private final DHT dht;
    
    private Painter painter;
    
    private int painterIndex = 0;

    private Timer timer;
    
    public static ArcsVisualizer show(final DHT dht) {
        final ArcsVisualizer arcs = new ArcsVisualizer(
                dht, dht.getLocalhost().getContactId());
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("HELLO" + dht.getName());
                frame.getContentPane().add(arcs);
                frame.setBounds(20, 30, 640, 640);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        arcs.stopArcs();
                    }
                });
                frame.setVisible(true);
            }
        });
        
        arcs.startArcs();
        return arcs;
    }
    
    public void startArcs() {
        timer.start();
        
        if (dht != null) {
            MessageDispatcher messageDispatcher = dht.getMessageDispatcher();
            messageDispatcher.addMessageDispatcherListener(listener);
        }
    }
    
    public void stopArcs() {
        timer.stop();
        
        if (dht != null) {
            MessageDispatcher messageDispatcher = dht.getMessageDispatcher();
            messageDispatcher.removeMessageDispatcherListener(listener);
        }
    }
    
    public ArcsVisualizer(DHT dht, KUID nodeId) {
        this.dht = dht;
        addPainter(new SnowMan(nodeId));
        addPainter(new PlasmaLamp(nodeId));
        addPainter(new DartBoard(nodeId));
        
        painter = painters.get(painterIndex);
        
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                timer = new Timer(SLEEP, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        repaint();
                    }
                });
                
                // Make sure the JPanel is focusable or it won't fire 
                // FocusEvent and Component's hasFocus() method returns 
                // always false
                setFocusable(true);
                
                addMouseListener(new MouseAdapter() {
                    
                    private volatile boolean hadFocus = false;
                    
                    @Override
                    public void mousePressed(MouseEvent e) {
                        hadFocus = e.getComponent().isFocusOwner();
                        // Request focus if not focused.  This can occur if the 
                        // window is newly activated.
                        if (!hadFocus) {
                            ArcsVisualizer.this.requestFocusInWindow();
                        }
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (!hadFocus) {
                            return;
                        }
                        
                        synchronized (lock) {
                            painter.clear();
                            
                            painterIndex = (painterIndex + 1) % painters.size();
                            painter = painters.get(painterIndex);
                        }
                    }
                });
            }
        };
        
        if (SwingUtilities.isEventDispatchThread()) {
            runner.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runner);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public void addPainter(Painter painter) {
        synchronized (lock) {
            painters.add(painter);
        }
    }
    
    @Override
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

    /*@SuppressWarnings({"InfiniteLoopStatement"})
    public static void main(String[] args) throws Exception {
        ArcsVisualizer arcs = new ArcsVisualizer(null, KUID.createRandomID());
        arcs.startArcs();
        
        List<Message> messages = new ArrayList<Message>();
        messages.add(new DefaultPingRequest(null, null));
        messages.add(new DefaultPingResponse(null, null, (SocketAddress)null, null));
        messages.add(new DefaultNodeRequest(null, null, null));
        messages.add(new DefaultNodeResponse(null, null, null, null));
        messages.add(new DefaultValueRequest(null, null, null, null, null));
        messages.add(new DefaultValueResponse(null, null, 0, null, null));
        messages.add(new DefaultStoreRequest(null, null, null, null));
        messages.add(new DefaultStoreResponse(null, null, null));
        
        JFrame frame = new JFrame();
        frame.getContentPane().add(arcs);
        frame.setBounds(20, 30, 800, 600);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Random generator = new Random();
        
        final int sleep = 100;
        
        while(true) {
            
            // Simulate an received or sent message
            KUID nodeId = KUID.createRandomID();
            boolean outgoing = generator.nextBoolean();
            
            Message message = messages.get(generator.nextInt(messages.size()));
            
            arcs.painter.handle(outgoing, nodeId, null, message);
            
            // Sleep a bit...
            //Thread.sleep(sleep);
            Thread.sleep(generator.nextInt(sleep));
            
            // Send every now an then a response
            if (generator.nextBoolean()) {
                //arcs.painter.handle(!outgoing, nodeId, null, message);
            }
        }
    }*/
}
