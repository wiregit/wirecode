/*
 * LogGenerator.java
 *
 * Created on March 27, 2001, 4:10 PM
 */

package com.limegroup.gnutella.util;

import java.io.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Generates a buffered log. It buffers the input and dumps it out after 
 * @author  asingla
 * @version
 */
public class LogGenerator
{
    /**
     * The output stream to write to
     */
    private PrintStream _out;
    
    
    private static final int BUFFER_INITIAL_CAPACITY = 10000;
    
    /**
     * The internal buffer
     */
    private StringBuffer _buffer = new StringBuffer(BUFFER_INITIAL_CAPACITY);
    
    /**
     * The number of calls to print/append methods that are buffered, 
     * before actually
     * outputting the result to the out stream.
     */
    private int _bufferingLimit;
    
    /**
     * Number of calls that have been made to print/append methods, after the 
     * buffer was last written out to the 'out' stream
     */
    private int _count = 0;
    
    /**
     * A single date insance, that will get updated periodically
     */
    private static volatile Date _date = new Date();
    
    static
    {
        //keep updating the date every 2 minutes
        Timer timer = new Timer();
        
        //schedule date updating task
        timer.schedule(
            new TimerTask()
            {
                public void run()
                {
                    _date = new Date();
                }
            },
            0, 120000);
    }
    
    
    /**
     * Creates new LogGenerator with specified out stream and buffering size
     * @param out The output stream to write to
     * @param bufferingLimit The number of calls to print/append methods that 
     * are buffered, before actually
     * outputting the result to the out stream.
     * @requires The parameter 'out' should be pre-initialized.
     * @requires bufferingLimit be greater than or equal to zero
     */
    public LogGenerator(PrintStream out, int bufferingLimit)
    {
        this._out = out;
        this._bufferingLimit = bufferingLimit;
    }
    
    /**
     * schedules the passed object for writing out to the stream
     */
    public void println(Object o)
    {
        //append to the buffer
        _buffer.append(o);
        _buffer.append("\n");
        
        //increment the count
        _count++;
        
        //if count reached threshold
        if(_count >= _bufferingLimit)
        {
            //write the buffer out to the stream
            writeBufferOut();
            //reinitialize the count
            _count = 0;
        }
    }//end of fn append
    
    public void printlnWithDateStamp(Object o)
    {
        _buffer.append(_date);
        _buffer.append(" ");
        this.println(o);
    }
    
    /**
     * Writes the buffer to the 'out' stream
     */
    private void writeBufferOut()
    {
        //write out
        _out.print(_buffer);
        _out.flush();
        //reinitialize buffer
        _buffer = new StringBuffer(BUFFER_INITIAL_CAPACITY);
    }
    
    /**
     * Forces the internal buffer to be written out to the out stream
     */
    public void flush()
    {
        writeBufferOut();
    }
    
}
