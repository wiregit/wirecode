package com.limegroup.gnutella.guess;

import com.limegroup.gnutella.QueryRequest;

public class GUESSStatistics {

    public static void getAckStatistics(String host, int port) {
        float numAttempted = 0, numReceived = 0;
        GUESSTester tester = new GUESSTester("whatever");
        while (numAttempted < 500) {
            try {
                if (tester.testAck(host, port))
                    numReceived++;
            }
            catch (Exception ignored) {}
            numAttempted++;
        }
        System.out.println("GUESSStatistics.getAckStatistics():" +
                           " Num Queries Sent : " + numAttempted +
                           ", Num Acks Received : " + numReceived +
                           " (" + ((numReceived/numAttempted)*100) +
                           "%)");
    }


    public static void getQueryStatistics(String host, int port,
                                          String searchKey) {
        float numAttempted = 0, numReceived = 0;
        GUESSTester tester = new GUESSTester("whatever");
        while (numAttempted < 100) {
            QueryRequest qr = new QueryRequest((byte)1, 0, searchKey);
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
        getAckStatistics(host, port);
        if (argv.length >= 3)
            getQueryStatistics(host, port, argv[2]);
        else
            getQueryStatistics(host, port, "LimeWireWin");
        System.exit(0);
    }

}
