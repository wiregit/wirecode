package com.limegroup.gnutella.tests;

import java.io.*;
import java.net.*;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Tests the HTTP/1.1 functionailty in the UploadManager
 * @author Anurag Singla
 */
public class HTTP11Test
{
    private String _host = "localhost";
    private int _port = 6346;
    private String _filename = "gpl.txt";
    private int _fileIndex = 0;
    
    private static final int CHUNK_SIZE = 128;
    
    private int _startRange = 0;
    
    private Socket _connection;
    private BufferedReader _in;
    private BufferedWriter _out;
    
    public void run()
    {
        try
        {
            //open connection
            openConnection();
            
            //send Request
            sendRequest();
            
            //start the reader thread
            Thread readerThread = new Thread(new Reader());
            readerThread.start();
            
            //send Request again
            _startRange += CHUNK_SIZE;
            sendRequest();
            
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
    
    private void sendRequest() throws IOException
    {
        _out.write("GET /get/" + _fileIndex + "/" + _filename
            + " HTTP/1.1\r\n");
        _out.write("User-Agent: "+CommonUtils.getVendor()+"\r\n");
        _out.write("Range: bytes=" + _startRange + "-"
            + (_startRange + CHUNK_SIZE) + "\r\n");
        _out.write("\r\n");
        _out.flush();
    }
    
    /**
     * opens connection to the host to be tested
     */
    private void openConnection() throws IOException
    {
        _connection = new Socket(_host, _port);
        _in = new BufferedReader(new InputStreamReader(
            _connection.getInputStream()));
        _out = new BufferedWriter(new OutputStreamWriter(
            _connection.getOutputStream()));
    }
    
    public static void main(String[] args)
    {
        HTTP11Test test = new HTTP11Test();
        test.run();
    }
    
    private class Reader implements Runnable
    {
        public void run()
        {
            try
            {
                String line;
                while((line = _in.readLine()) != null)
                {
                    System.out.println(line);
                }
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }
}
