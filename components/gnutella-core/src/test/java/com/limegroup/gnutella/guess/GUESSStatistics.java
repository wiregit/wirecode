package com.limegroup.gnutella.guess;

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

    public static void main(String argv[]) {
        String host = argv[0];
        int    port = Integer.parseInt(argv[1]);
        getAckStatistics(host, port);
        System.exit(0);
    }

}
