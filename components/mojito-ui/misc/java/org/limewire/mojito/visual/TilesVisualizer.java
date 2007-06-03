package org.limewire.mojito.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherListener;
import org.limewire.mojito.io.MessageDispatcher.MessageDispatcherEvent.EventType;
import org.limewire.mojito.messages.DHTMessage;


@SuppressWarnings("serial")
public class TilesVisualizer extends JPanel implements MessageDispatcherListener {
    
    private static final long SLEEP = 100L;
    
    private static final long DURATION = 3000L;
    
    private static final int TILES = 16;
    
    private static final int TILE_WIDTH = 8;
    
    private static final int TILE_HEIGHT = 8;
    
    private static final int TILE_GAP = 1;
    
    private Object lock = new Object();
    
    private Tile[] tiles = new Tile[TILES * TILES];
    
    private KUID localNodeId;
    
    private volatile boolean running = true;
    
    public static TilesVisualizer show(final Context context) {
        final TilesVisualizer tiles = new TilesVisualizer(context.getLocalNodeID());
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame(context.getName());
                frame.getContentPane().add(tiles);
                frame.setBounds(20, 30, 640, 640);
                frame.addWindowListener(new WindowListener() {
                    public void windowActivated(WindowEvent e) {}
                    public void windowClosed(WindowEvent e) {}
                    public void windowClosing(WindowEvent e) {
                        tiles.running = false;
                        context.getMessageDispatcher().removeMessageDispatcherListener(tiles);
                    }
                    public void windowDeactivated(WindowEvent e) {}
                    public void windowDeiconified(WindowEvent e) {}
                    public void windowIconified(WindowEvent e) {}
                    public void windowOpened(WindowEvent e) {}
                });
                frame.setVisible(true);
            }
        });
        
        context.getMessageDispatcher().addMessageDispatcherListener(tiles);
        return tiles;
    }
    
    public TilesVisualizer(KUID localNodeId) {
        this.localNodeId = localNodeId;
        
        int localIndex = getIndex(localNodeId);
        
        for (int row = 0; row < TILES; row++) {
            for (int column = 0; column < TILES; column++) {
                int index = (row * TILES) + column;
                tiles[index] = new Tile(row, column, index==localIndex);
            }
        }
        
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
        synchronized (lock) {
            int width = getWidth();
            int height = getHeight();
            
            g.setColor(Color.white);
            g.fillRect(0, 0, width, height);
            
            for (Tile tile : tiles) {
                tile.paint(g, width, height);
            }
            
            g.setColor(Color.black);
            g.drawRect(0, 0, 
                    (TILES * (TILE_WIDTH + TILE_GAP)) - TILE_GAP, 
                    (TILES * (TILE_HEIGHT + TILE_GAP)) - TILE_GAP);
        }
    }

    public void handleMessageDispatcherEvent(MessageDispatcherEvent evt) {
        EventType type = evt.getEventType();
        if (type.equals(EventType.MESSAGE_SENT)) {
            KUID nodeId = evt.getNodeID();
            if (nodeId != null) {
                paintTile(nodeId, true);
            }
        } else if (type.equals(EventType.MESSAGE_RECEIVED)) {
            DHTMessage message = evt.getMessage();
            paintTile(message.getContact().getNodeID(), false);
        }
    }
    
    public void paintTile(KUID nodeId, boolean send) {
        int index = getIndex(nodeId);
        synchronized (lock) {
            if (send) {
                tiles[index].send();
            } else {
                tiles[index].receive();
            }
        }
    }
    
    public int getIndex(KUID nodeId) {
        byte[] data = new byte[2];
        nodeId.xor(localNodeId).getBytes(0, data, 0, data.length);
        
        int row = ((data[0] & 0xF0) >> 4);
        int column = (data[0] & 0x0F);
        
        return (row * TILES) + column;
    }
    
    private static class Tile {

        private boolean local;
        private int row, column;
        
        private boolean send = false;
        private long timeStamp = 0L;
        
        public Tile(int row, int column, boolean local) {
            this.row = row;
            this.column = column;
            this.local = local;
        }
        
        public void send() {
            send = true;
            timeStamp = System.currentTimeMillis();
        }
        
        public void receive() {
            send = false;
            timeStamp = System.currentTimeMillis();
        }
        
        public void paint(Graphics g, int width, int height) {
            if (local) {
                g.setColor(Color.red);
            } else {
                long delta = System.currentTimeMillis() - timeStamp;
                if (delta < DURATION) {
                    int alpha = 255 - (int)(255f/DURATION * delta);
                    if (send) {
                        g.setColor(new Color(0, 0, 255, alpha));
                    } else {
                        g.setColor(new Color(0, 255, 0, alpha));
                    }
                } else {
                    g.setColor(Color.white);
                }
            }
            
            int x = column * TILE_WIDTH + (column * TILE_GAP);
            int y = row * TILE_HEIGHT + (row * TILE_GAP);
            g.fillRect(x, y, TILE_WIDTH, TILE_HEIGHT);
        }
    }
    
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new TilesVisualizer(KUID.createRandomID()));
        frame.setBounds(20, 30, 640, 640);
        frame.setVisible(true);
    }
}
