package com.limegroup.gnutella.routing;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.gui.*;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * A class used to gather statistics for an alpha test and send the statistics
 * periodically to a server for further analysis.  
 *
 * To add a statistic for sending to the server, just use the static method
 * addToAverage, passing in the statistic and value.
 *
 */ 
public class StatisticsRecorder
{
    /**
     * time to wait before sending next set of statistics
     */
    private static final long WAIT_TIME = 30 * 60 * 1000; //30 minutes
    private static final String SERVLET_URL = 
        "http://www.limewire.com:8080/update/servlet/AlphaTestStatsHandler";
    /**
     * The next time to send the statistics to the server.  Initialize to 
     * current time + WAIT_TIME so that the first time the statistics are sent
     * to the server is at least WAIT_TIME minutes after the first statistic has
     * been added.
     */
    private static long nextTimeToSendStats = 
        System.currentTimeMillis() + WAIT_TIME;

    /** 
     * The client GUID is always the first parameter sent to the servlet. 
     */
    private static String clientID = 
        SettingsManager.instance().getClientID();

    /**
     * Mapping of statistic name (i.e., "Route Table Size") to <sum, count>
     * (i.e., StatisticValue)
     * LOCKING: Obtain statisticsLock.
     */
    private static HashMap statistics = new HashMap();
    private static Object statisticsLock = new Object();

    /**
     * Adds the value to the statistic (specified by name).  If the statistic
     * doesn't exist, creates a new mapping for the new statistic.
     *
     * @param metric - the unit of measurement.
     * @requires - the statistic is an average or mean statistic (i.e., measures
     *             the average of mean of some value.
     */
    public synchronized static void addToAverage(String name, int value, 
                                                 String metricUnit)
    {
        //if the statistic already exists in the hash map, just add to the 
        //sum
        if (statistics.containsKey(name))
        {
            StatisticValue stat = (StatisticValue)statistics.get(name);
            stat.addToSum(value);
        }
        else
        {
            statistics.put(name, 
               new StatisticValue(value, true, metricUnit));
        }
        //if time to send the stats (if at least WAIT_TIME elapsed since 
        //last time), then start a thread to send the stats to the server.
        if (System.currentTimeMillis() >= nextTimeToSendStats)
        {
            (new StatisticsSenderThread()).start();
            nextTimeToSendStats = System.currentTimeMillis() + WAIT_TIME;
        }
    }

    /**
     * Adds the value to the statistic (specified by name).  If the statistic
     * doesn't exist, creates a new mapping for the new statistic.
     *
     * @param metric - the unit of measurement.
     * @requires - the statistic is a measure of the total of some value.
     */
    public synchronized static void addToTotal(String name, int value, 
                                               String metricUnit)
    {
        //if the statistic already exists in the hash map, just add to the 
        //sum
        if (statistics.containsKey(name))
        {
            StatisticValue stat = (StatisticValue)statistics.get(name);
            stat.addToSum(value);
        }
        else
        {
            statistics.put(name, 
                new StatisticValue(value, false, metricUnit));
        }
        //if time to send the stats (if at least WAIT_TIME elapsed since 
        //last time), then start a thread to send the stats to the server.
        if (System.currentTimeMillis() >= nextTimeToSendStats)
        {
            (new StatisticsSenderThread()).start();
            nextTimeToSendStats = System.currentTimeMillis() + WAIT_TIME;
        }
    }
    
    /**
     * Thread which sends the statistics recorded to the server.  Basically, 
     * creates a URL connection to a web server (via servlet) and then sends 
     * the statistical data with the clientID last (to be printed first).
     */
    private static class StatisticsSenderThread extends Thread
    {
        public void run()
        {
            try 
            {
                URL url = new URL(SERVLET_URL);
                URLConnection connection = url.openConnection();
                connection.setDoOutput(true);
                
                //send the client ID first
                PrintWriter out = new PrintWriter(connection.getOutputStream());
                out.print("clientID" + "=" +  URLEncoder.encode(clientID) +
                    "\n\n");
                out.print(generateSendString());
                out.close();
            }
            catch(IOException ie)
            {
                MessageService.showError("Unable to send network statistics " + 
                    "to server.  Please contact support@limepeer.com  Thank " +
                    "you.");
            }

        }

        /**
         * Generates the string with all the statistics to send to the server 
         * (via a servlet).
         */
        private String generateSendString()
        {
            StringBuffer sendBuffer = new StringBuffer();

            synchronized (statisticsLock) 
            {
                Set entries = statistics.entrySet();
                Map.Entry[] mappings = 
                    (Map.Entry[])entries.toArray(new Map.Entry[entries.size()]);

                //send stats as names: parameters to web server (via servlet)
                for (int i=0; i < mappings.length; i++)
                {
                    String statName = (String)mappings[i].getKey();
                    StatisticValue value = 
                        (StatisticValue)mappings[i].getValue();
                    String statValue = null;
                    if (value.isAverageStatistic())
                        statValue = new String(value.calculateAverage() + " " + 
                            value.getMetric());
                    else
                        statValue = new String(value.getTotal() + " " + 
                            value.getMetric());
                    sendBuffer.append(statName + "=" + 
                        URLEncoder.encode(statValue) + "\n");
                }
            }

            return sendBuffer.toString();
        }
    }
}

/**
 * Class to hold a sum and count, and hence, to calculate an average if 
 * needed.
 */
class StatisticValue
{
    private int sum;
    /**
     * If the statistic is one that measures the mean or average value,
     * then we'll need to use the count and average needed to calculate it.
     */
    private int count;
    private boolean averageNeeded;
    /**
     * Metric used (e.g., "KB", "bytes/sec", etc. )
     */
    private String metric;  

    public StatisticValue(int initialSum, boolean averageNeeded, String metric)
    {
        sum = initialSum;
        count = 1;
        this.averageNeeded = averageNeeded;
        this.metric = metric;
    }

    /**
     * add a specified amount to the sum.  If this statistic is one that 
     * measures the average of something, then increment the count.
     */
    void addToSum(int addition)
    {
        sum += addition;
        if (averageNeeded)
            count++;
    }

    /**
     * Calculates the average as a double.  Returns zero if the average is
     * not needed for this statistic.
     */
    double calculateAverage()
    {
        if (!averageNeeded)
            return 0;

        double avg = (double)sum / (double)count;
        return avg;
    }

    /**
     * Return the total of this statistic.  Returns 0 if this is a statistic 
     * used to measure the average of something.
     */
    int getTotal()
    {
        if (averageNeeded)
            return 0;
        else
            return sum;
    }

    /**
     * Return whether or not this statistic measures the average of some 
     * particular value.
     */
    boolean isAverageStatistic()
    {
        return averageNeeded;
    }

    String getMetric()
    {
        return metric;
    }
}
