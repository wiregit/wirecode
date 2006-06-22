package com.limegroup.gnutella.guess;

import com.limegroup.gnutella.messages.QueryRequest;

public class GUESSStatistics {

    public static void getAckStatisticsAndPrint(String host, int port) {
        Object[] retObjs = getAckStatistics(host, port);
        float numSent = ((Float)retObjs[1]).floatValue();
        float numGot = ((Float)retObjs[0]).floatValue();
        System.out.println("GUESSStatistics.getAckStatistics():" +
                           " Num Queries Sent : " + numSent +
                           ", Num Acks Received : " + numGot + 
                           " (" + ((numGot/numSent)*100) +
                           "%)");
    }

    /* @return a Object[] of length 3.  First is a Float - num Received, second
     * is a Float - num Sent, and last is the average time for a reply.
     */
    public static Object[] getAckStatistics(String host, int port) {
        float numAttempted = 0, numReceived = 0, timeSum = 0;
        GUESSTester tester = new GUESSTester("whatever");
        while (numAttempted < 20) {
            try {
                long timeTook = tester.testAck(host, port);
                if (timeTook > 0) {
                    numReceived++;
                    timeSum += timeTook;
                }
            }
            catch (Exception ignored) {}
            numAttempted++;
        }
        Object[] retObjs = new Object[3];
        retObjs[0] = new Float(numReceived);
        retObjs[1] = new Float(numAttempted);
        retObjs[2] = new Float(timeSum/numReceived);
        return retObjs;
    }


    /* @return a Object[] of length 3.  First is a Float - num Received, second
     * is a Float - num Sent, and last is the average time for a reply.
     */
    public static Object[] getPingStatistics(String host, int port) {
        float numAttempted = 0, numReceived = 0, timeSum = 0;
        GUESSTester tester = new GUESSTester("whatever");
        while (numAttempted < 5) {
            try {
                long timeTook = tester.testPing(host, port);
                if (timeTook > 0) {
                    numReceived++;
                    timeSum += timeTook;
                }
                Thread.sleep(2*1000); // wait a couple of seconds for pings...
            }
            catch (Exception ignored) {}
            numAttempted++;
        }
        Object[] retObjs = new Object[3];
        retObjs[0] = new Float(numReceived);
        retObjs[1] = new Float(numAttempted);
        retObjs[2] = new Float(timeSum/numReceived);
        return retObjs;
    }


    public static void getQueryStatistics(String host, int port,
                                          String searchKey) {
        float numAttempted = 0, numReceived = 0;
        GUESSTester tester = new GUESSTester("whatever");
        int size = searchKey.length();
        int chop = 0;
        while (numAttempted < 10) {
			QueryRequest qr = 
				QueryRequest.createQuery(searchKey.substring(0,size-chop), (byte)1);
            if (++chop > 3)
                chop = 0;
            try {
                if (tester.testQuery(host, port, qr) != null)
                    numReceived++;
            }
            catch (Exception ignored) {}
            numAttempted++;
        }
        System.out.println("GUESSStatistics.getQueryStatistics():" +
                           " Num Queries Sent : " + numAttempted +
                           ", Num Replies Received : " + numReceived +
                           " (" + ((numReceived/numAttempted)*100) +
                           "%)");
    }


    public static void main(String argv[]) {
        String host = argv[0];
        int    port = Integer.parseInt(argv[1]);
        getAckStatisticsAndPrint(host, port);
        if (argv.length >= 3)
            getQueryStatistics(host, port, argv[2]);
        else
            getQueryStatistics(host, port, "LimeWireWin");
        System.exit(0);
    }

}
