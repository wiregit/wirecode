package com.limegroup.gnutella.tests;

import java.util.*;
import java.rmi.*;
import java.io.*;

import com.limegroup.gnutella.*;

/**
 * Class which controls all the test clients in the Test Framework.  Using RMI,
 * each client can connect or disconnect from another client (or disconnect 
 * entirely from the "test" network.  There should be twenty such Test clients 
 * running, ten on two seperate machines.  
 *
 * The controller will accept command-line input to monitor the test framework.
 * It creates connections and disconnects between test clients, and monitors
 * network activity, if desired (i.e., starts a timer which after some time
 * disconnects every client from the network and prints out stats for each 
 * client).
 *
 * Basically, this controller class is the means to use the test framework for
 * testing major functionality changes, such as pong-caching.
 */
public class TestFrameworkController
{
    //2 minutes for monitoring the network.
    private static final int NETWORK_MONITOR_TIME = 2 * 60 * 1000;
    
    //machine with ten test clients (startPort to startPort+9)
    private String IP1; 
    //machine with ten test clients (startPort+10 to startPort+19)
    private String IP2; 
    //lowest port of all clients.  Ports of clients are assumed to be in 
    //sequential order starting from startPort thru startPort+19.
    private int startPort; 

    private Vector activeClients; //list of test clients who are connected.

    public TestFrameworkController(String ip1, String ip2, int startPort)
    {
        this.IP1 = ip1;
        this.IP2 = ip2;
        this.startPort = startPort;
        activeClients = new Vector();
    }

    public static void main(String[] args)
    {
        if (args.length != 3)
        {
            System.out.println("Usage: TestFrameworkController [ip1] [ip2] " +
                "[starting port]");
            System.exit(1);
        }

        int startPort = Integer.parseInt(args[2]);
        
        TestFrameworkController controller = 
            new TestFrameworkController(args[0], args[1], startPort);
        controller.run();
    }

    /**
     * Main method which listens for command-line inputs from the user on how
     * to run the test framework.
     */
    public void run()
    {
        System.out.println("Commands to use:");
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        for (;;) //loop forever
        {
            printMenu();
            String command = null;
            try
            {
                command = in.readLine();
            }
            catch(IOException ioe)
            {
                System.out.println("Error reading command\n");
                continue;
            }
            if (command.equals("exit"))
            {
                shutdownAllConnections();
                break;
            }
            String[] commands = split(command);
            if ((commands[0].equals("connect")) && (commands.length==6))
                connectUserToUser(commands);
            else if ((commands[0].equals("disconnect")) && (commands.length==6))
                disconnectConnection(commands);
            else if ((commands[0].equals("disconnect")) && (commands.length==3))
                disconnectUser(commands);
            else if ((commands[0].equals("num")) && (commands.length==4))
                printNumOfConnections(commands);
            else if ((commands[0].equals("print")) && (commands.length==5))
                printConnectionInfo(commands);
            else if ((commands[0].equals("display")) && (commands.length==5))
                displayNetworkStats(commands);
            else if ((commands[0].equals("start")) && (commands.length==3))
                startNetworkMonitor();
            else
                System.out.println("Incorrect command entered\n");
        }
    }

    private void printMenu()
    {
        System.out.println("\n\"connect [ip] [port] to [ip] [port]\"");
        System.out.println("\"disconnect [ip] [port] from [ip] [port]\"");
        System.out.println("\"disconnect [ip] [port]\"");
        System.out.println("\"num connections [ip] [port]\"");
        System.out.println("\"print current connections [ip] [port]\"");
        System.out.println("\"display client stats [ip] [port]");
        System.out.println("\"start network monitor\"");
        System.out.println("\"exit\"\n");
    }

    /**
     * Shutdowns down all active test clients, where an active test client is one
     * that has at least one current connection.
     */
    private void shutdownAllConnections()
    {
        String lookupString;
        TestFrameworkInterface tf;

        if (activeClients.size() > 0)
            System.out.println("Shutting down active connections ....\n");
        for (int i = 0; i < activeClients.size(); i++)
        {
            RMIConnection client = (RMIConnection)activeClients.elementAt(i);
            tf = getTestClient(client);
            if (tf != null)
            {
                try 
                {
                    if (tf.getNumOfConnections() > 0)
                        tf.disconnect();
                }
                catch(RemoteException re)
                {
                    System.out.println("Error disconnecting: " + client.getIP() +
                    ":" + (client.getRMIPort() + 1000));
                }
            }
        }
    }

    /**
     * Splits a line into an array of seperate words.
     */
    private String[] split(String line)
    {
        StringTokenizer st = new StringTokenizer(line);
        Vector buf = new Vector();
        while (st.hasMoreTokens())
            buf.add(st.nextToken());

        String[] lineWords = new String[buf.size()];
        for (int i = 0; i < lineWords.length; i++)
            lineWords[i] = (String)buf.elementAt(i);
        
        return lineWords;
    }

    /**
     * Returns whether ip and port are valid based on parameters to the program
     */
    private boolean isValidIPAndPort(String ip, String strPort)
    {
        int port = Integer.parseInt(strPort);

        if ( !(ip.equals(IP1)) && !(ip.equals(IP2)))
            return false;
        if ( (port < startPort) || (port > (startPort+19)) )
            return false;
        return true;
    }

    /**
     * Creates a connection from one user to another. 
     */
    private void connectUserToUser(String[] commands)
    {
        //check for valid clients
        if (!isValidIPAndPort(commands[1], commands[2]))
        {
            System.out.println("Invalid IP:port of connecting client\n");
            return;
        }
        
        if (!isValidIPAndPort(commands[4], commands[5]))
        {
            System.out.println("Invalid IP:port of receiving client\n");
            return;
        }

        //first, get RMI Connection to initiator
        int rmiPort = Integer.parseInt(commands[2]) - 1000;
        RMIConnection conn = getRMIConnection(commands[1], rmiPort, true);
        TestFrameworkInterface client = getTestClient(conn); 
        if (client == null)
            return;
        
        //try connecting clients
        try
        {
            client.connect(commands[4], Integer.parseInt(commands[5]));
        }
        catch(RemoteException re)
        {
            System.out.println("Error connecting clients\n");
            return;
        }

        //add receiving client in list of active clients (if not there already)
        int receiverRMIPort = Integer.parseInt(commands[5]) - 1000;
        RMIConnection receiver = new RMIConnection(commands[4], receiverRMIPort);
        if (!activeClients.contains(receiver))
            activeClients.add(receiver);

        System.out.println("Connection established\n");
    }

    private void disconnectConnection(String[] commands)
    {
        //only check for valid disconnecting client, since for incoming 
        //connections, the port most likely will be not be in the range of
        //startPort - startPort+19
        if (!isValidIPAndPort(commands[4], commands[5]))
        {
            System.out.println("Invalid IP:port of disconnecting client\n");
            return;
        }

        //first, get RMI Connection to initiator
        int rmiPort = Integer.parseInt(commands[5]) - 1000;
        RMIConnection conn = getRMIConnection(commands[4], rmiPort, true);
        TestFrameworkInterface client = getTestClient(conn); 
        if (client == null)
            return;
        
        //try connecting clients
        try
        {
            if (client.disconnectFrom(commands[1], 
                Integer.parseInt(commands[2])))
                System.out.println("Successfully disconnected\n");
            else
                System.out.println("Unsuccessful attempt to disconnect");
                
        }
        catch(RemoteException re)
        {
            System.out.println("Error disconnecting clients\n");
            return;
        }
    }

    private void disconnectUser(String[] commands)
    {
        //check for valid client
        if (!isValidIPAndPort(commands[1], commands[2]))
        {
            System.out.println("Invalid IP:port of client\n");
            return;
        }
        
        int rmiPort = Integer.parseInt(commands[2]) - 1000;
        RMIConnection conn = getRMIConnection(commands[1], rmiPort, false);
        if (conn == null)
        {
            System.out.println("Client has no connections\n");
            return;
        }
        TestFrameworkInterface client = getTestClient(conn);
        if (client == null)
            return;
        
        try
        {
            client.disconnect();
        }
        catch(RemoteException re)
        {
            System.out.println("Error disconnecting client\n");
            return;
        }
        activeClients.remove(conn);
        System.out.println("Client disconnected\n");
    }

    private void printNumOfConnections(String[] commands)
    {
        //check for valid client
        if (!isValidIPAndPort(commands[2], commands[3]))
        {
            System.out.println("Invalid IP:port of client\n");
            return;
        }
        
        int rmiPort = Integer.parseInt(commands[3]) - 1000;
        RMIConnection conn = getRMIConnection(commands[2], rmiPort, false);
        if (conn == null)
        {
            System.out.println("Client has no connections\n");
            return;
        }
        TestFrameworkInterface client = getTestClient(conn);
        if (client == null)
            return;

        int numConnections;
        try
        {
            numConnections = client.getNumOfConnections();
        }
        catch(RemoteException re)
        {
            System.out.println("Error getting number of connections\n");
            return;
        }

        System.out.println("\nNum of connections: " + numConnections + "\n");
    }

    private void printConnectionInfo(String[] commands)
    {
       //check for valid client
        if (!isValidIPAndPort(commands[3], commands[4]))
        {
            System.out.println("Invalid IP:port of client\n");
            return;
        }
        
        int rmiPort = Integer.parseInt(commands[4]) - 1000;
        RMIConnection conn = getRMIConnection(commands[3], rmiPort, false);
        if (conn == null)
        {
            System.out.println("Client has no connections\n");
            return;
        }
        TestFrameworkInterface client = getTestClient(conn);
        if (client == null)
            return;

        String[] connections = null;
        try
        {
            connections = client.getCurrentConnections();
        }
        catch(RemoteException re)
        {
            System.out.println("Error printing connection info\n");
            return;
        }
        //only print if there are some connections to print.
        if (connections.length > 0)
        {
            for (int i=0; i < connections.length; i++)
            {
                System.out.println(connections[i]);
            }
        }
        else
        {
            System.out.println("Client has no current connections");
        }
    }                   

    /**
     * Prints out the network statistics of a client.
     */
    private void displayNetworkStats(String[] commands)
    {
       //check for valid client
        if (!isValidIPAndPort(commands[3], commands[4]))
        {
            System.out.println("Invalid IP:port of client\n");
            return;
        }
        
        int rmiPort = Integer.parseInt(commands[4]) - 1000;
        RMIConnection conn = getRMIConnection(commands[3], rmiPort, false);
        if (conn == null)
        {
            System.out.println("Client has no connections\n");
            return;
        }
        TestFrameworkInterface client = getTestClient(conn);
        if (client == null)
            return;

        int[] stats = null;
        try
        {
            stats = client.getNetworkStatistics();
        }
        catch(RemoteException re)
        {
            System.out.println("Error printing client stats info\n");
            return;
        }
        printStats(stats);
    }

    /**
     * Starts the network monitor thread.
     */
    private void startNetworkMonitor()
    {
        System.out.println("Starting network monitor ....");
        Thread networkMonitor = new Thread(new NetworkMonitorThread());
        networkMonitor.start();
    }

    /**
     * Returns a handle to an test client (via TestFrameworkInterface).  If an
     * exception is thrown, then a null is returned.
     */
    private TestFrameworkInterface getTestClient(RMIConnection conn)
    {
        TestFrameworkInterface tf = null;
        String lookupString = new String("rmi://" + conn.getIP() + ":" + 
                conn.getRMIPort() + "/" + TestFrameworkInterface.SERVICE_NAME);
        try 
        {
            tf = (TestFrameworkInterface)Naming.lookup(lookupString);
        }
        catch(Exception e)
        {
            System.out.println("Error getting handle to client: " +
                conn.getIP() + ":" + (conn.getRMIPort() + 1000) + " via RMI\n");
        }

        return tf;
    }

    /**
     * Returns the RMI Connection of an active test client specified by host and
     * port.  If the RMI Connection doesn't already exist in the list of active
     * clients and addNew is specified, then it creates a new RMI Connection and
     * returns that, otherwise, it returns null.
     */
    private RMIConnection getRMIConnection(String host, int port, 
                                           boolean addNew)
    {
        for (int i = 0; i < activeClients.size(); i++)
        {
            RMIConnection conn = (RMIConnection)activeClients.elementAt(i);
            if ((conn.getIP().equals(host)) && (conn.getRMIPort() == port))
                return conn;
        }

        RMIConnection newConn = null;
        if (addNew)
        {
            newConn = new RMIConnection(host, port);
            activeClients.add(newConn);
        }
        return newConn;
    }

    /**
     * Prints out the network statistics to the console.
     */
    private void printStats(int[] stats)
    {
        System.out.println("cached hosts: " + stats[0]);
        System.out.println("reserve hosts: " + stats[1]);
        System.out.println("ping requests processed: " + stats[2]);
        System.out.println("ping requests broadcasted: " + stats[3]);
    }

    private class NetworkMonitorThread implements Runnable
    {
        /**
         * Waits (i.e., sleeps) for the network monitor time and then
         * disconnects all clients and prints out stats.
         */
        public void run()
        {
            try
            {
                Thread.sleep(NETWORK_MONITOR_TIME);
            }
            catch(InterruptedException ie)
            {
                System.out.println("Network monitor interrupted. " + 
                    "Please restart network monitor.");
                return;
            }

            if (activeClients.size() == 0)
            {
                System.out.println("No active clients");
                return;
            }
            else
            {
                System.out.println("About to print network statistics");
                for (int i = 0; i < activeClients.size(); i++)
                {
                    RMIConnection conn = 
                        (RMIConnection)activeClients.elementAt(i);
                    TestFrameworkInterface client = getTestClient(conn);
                    if (client == null)
                        continue;

                    String host = conn.getIP();
                    int port = conn.getRMIPort() + 1000;
                    try
                    {
                        client.disconnect();
                        int[] stats = client.getNetworkStatistics();
                        System.out.println("\nNetwork statistics for: " + host + 
                            ":" + port);
                        printStats(stats);
                    }
                    catch(RemoteException re)
                    {
                        System.out.println("Error with test client: " +
                            host + ":" + port);
                        continue;
                    }  
                }
                //clear out active clients 
                activeClients.clear();
            }
        }
   }
}

/**
 * Class to hold onto an IP and RMI port for looking up instances to remote 
 * objects (i.e., Test Clients).
 */
class RMIConnection 
{
    private String IP;
    private int rmiPort;

    RMIConnection(String ip, int port)
    {
        IP = ip;
        rmiPort = port;
    }

    /**
     * Returns IP in string format.
     */
    public String getIP()
    {
        return IP;
    }

    public int getRMIPort()
    {
        return rmiPort;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof RMIConnection))
            return false;

        RMIConnection other = (RMIConnection)obj;
        return ((IP.equals(other.IP)) && (rmiPort == other.rmiPort));
    }
}

