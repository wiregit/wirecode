package com.limegroup.gnutella.privategroups;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.NIOServerSocket;
import org.limewire.nio.observer.AcceptChannelObserver;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.StubAcceptChannelObserver;
import org.limewire.nio.observer.StubConnectObserver;
import org.limewire.nio.observer.WriteObserver;

public class ClientNio {
    
    final static private int LISTEN_PORT = 9999;
    
    public ClientNio(){
        
    }
    
    public void initializeClientSocket(){
        
        
    }
    
    public static void main(String[] args) throws IOException {
        
        
        //set up server socket
        AcceptObserver acceptObserver = new AcceptObserver(){
            public void handleAccept(Socket socket) throws IOException {
                System.out.println("accept observer");
                
            }

            public void handleIOException(IOException iox) {
                // TODO Auto-generated method stub
                
            }

            public void shutdown() {
                // TODO Auto-generated method stub
                
            }
        };
          
        NIOServerSocket server = new NIOServerSocket(acceptObserver);
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(LISTEN_PORT));
        
        //server channel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        
        NIODispatcher.instance().registerAccept(serverChannel, new StubAcceptChannelObserver(){
            public void handleAcceptChannel(SocketChannel channel) throws IOException {
                System.out.println("got client connection");
            }

            public void handleIOException(IOException iox) {
                // TODO Auto-generated method stub
                
            }

            public void shutdown() {
                // TODO Auto-generated method stub
                
            }
        });
        

        SocketChannel clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);
        ConnectObserver connectObserver = new StubConnectObserver(){
            private void handleConnect(){
                System.out.println("client is trying to connect");
            }
        };
        NIODispatcher.instance().registerConnect(clientChannel, connectObserver, 3000);
        
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 9999);
        
        clientChannel.connect(addr);
       
        //NIOSocket client = new NIOSocket();
        //client.connect(addr);
     
        System.out.println("done");
    }
    
    public static void test(){
            try{
            NIOServerSocket server;
                    SocketChannel clientSocketChannel = SocketChannel.open();
                    clientSocketChannel.configureBlocking(false);
                    ServerSocketChannel serverSocketChannel = 
            ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);

                    NIODispatcher.instance().registerConnect(clientSocketChannel, new ConnectObserver(){
                     public void handleConnect(Socket socket) {
                     System.out.println("Inside anonymous ConnectObserver handleConnect");
                     }
                     public void handleIOException(IOException iox){
                     System.out.println("Inside anonymous ConnectObserver handleIOException");
                     }
                     public void shutdown(){
                     System.out.println("Inside anonymous ConnectObserver shutdown");
                     }
                     }, 3000);
   

                    InetSocketAddress LISTEN_ADDR;
                     int LISTEN_PORT = 9999;
                     server = new NIOServerSocket(
                     new AcceptObserver() {
                     public void handleAccept(Socket socket) {
                     System.out.println("Inside anonymous AcceptObserver handleAccept");
                     }
                     public void handleIOException(IOException iox){
                     System.out.println("Inside anonymous AcceptObserver handleIOException");
                     }
                     public void shutdown(){
                     System.out.println("Inside anonymous AcceptObserver shutdown");
                     }
                     });
                     server.setReuseAddress(true);
                    LISTEN_ADDR = new InetSocketAddress("127.0.0.1", LISTEN_PORT);
                    server.bind(LISTEN_ADDR, 0);

                    clientSocketChannel.connect(LISTEN_ADDR);



                    NIODispatcher.instance().registerWrite(clientSocketChannel, 
            new WriteObserver(){
                     public boolean handleWrite() {
                     System.out.println("Inside anonymous WriteObserver handleWrite ");
                     return false;
                     }
                     public void handleIOException(IOException iox){
                     System.out.println("Inside anonymous WriteObserver handleIOException");
                     }
                     public void shutdown(){
                     System.out.println("Inside anonymous WriteObserver shutdown");
                     }
                     });

                    if(clientSocketChannel.isConnected())
                    clientSocketChannel.close();
                    System.out.println("Done.");
                    Thread.sleep(10000);

            }
            catch(Exception e){
            e.printStackTrace();
            }
            }

        
        
    }

