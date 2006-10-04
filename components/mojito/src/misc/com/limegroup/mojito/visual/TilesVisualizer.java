package com.limegroup.mojito.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.net.SocketAddress;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.io.MessageDispatcher.MessageDispatcherCallback;
import com.limegroup.mojito.messages.DHTMessage;

@SuppressWarnings("serial")
public class TilesVisualizer extends JPanel implements MessageDispatcherCallback {
    
    private static final long SLEEP = 100L;
    
    private static final long DURATION = 3000L;
    
    private static final int TILES = 16;
    
    private static final int TILE_WIDTH = 8;
    
    private static final int TILE_HEIGHT = 8;
    
    private static final int TILE_GAP = 1;
    
    private Object lock = new Object();
    
    private Tile[] tiles = new Tile[TILES * TILES];
    
    private KUID localNodeId;
    
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
                while(true) {
                    repaint();
                    
                    try { 
                        Thread.sleep(SLEEP); 
                    } catch (InterruptedException err) {}
                }
            }
        };
        
        new Thread(repaint).start();
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

    public void send(KUID nodeId, SocketAddress dst, DHTMessage message) {
        if (nodeId == null) {
            return;
        }
        
        paintTile(nodeId, true);
    }
    
    public void receive(DHTMessage message) {
        paintTile(message.getContact().getNodeID(), false);
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
        
        //int row = (data[0] >> 1) & 0x7F;
        //int column = ((data[0] & 1) << 6) | (data[1] >> 1) & 0x7F;
        
        int row = (data[0] >> 4) & 0xF;
        int column = (data[0] ) & 0xF;
        
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
        
        private int toColor(long delta) {
            return (int)(255f/DURATION * delta);
        }
        
        public void paint(Graphics g, int width, int height) {
            if (local) {
                //g.setColor(Color.red);
                g.setColor(Color.black);
            } else {
                long delta = System.currentTimeMillis() - timeStamp;
                if (delta < DURATION) {
                    if (send) {
                        /*int red = 255;
                        int green = 255;
                        int blue = toColor(delta);*/
                        int red = 0;
                        int green = 0;
                        int blue = 255;
                        g.setColor(new Color(red, green, blue));
                    } else {
                        /*int red = 255;
                        int green = toColor(delta);
                        int blue = 255;*/
                        int red = 0;
                        int green = 255;
                        int blue = 0;
                        g.setColor(new Color(red, green, blue));
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
