package com.limegroup.gnutella.performance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Performance test to measure latency of queries of shared files
 */
public class KeywordIndexTestPerformance {

    private static final Log LOG = LogFactory.getLog(KeywordIndexTestPerformance.class);

    private KeywordIndexPerformanceSearcher searcher;
    private KeywordIndexPerformanceAnalyzer analyzer;


    public KeywordIndexTestPerformance(KeywordIndexPerformanceConfig config) {
        this.searcher = new KeywordIndexPerformanceSearcher(config.getSearchConfig());
        this.analyzer = new KeywordIndexPerformanceAnalyzer(config.getAnalyzeConfig());
    }

    public static void main(String[] args) {
        KeywordIndexPerformanceConfig config = parseArgs(args);
        try {
            KeywordIndexTestPerformance tester = new KeywordIndexTestPerformance(config);
            tester.execute();
        } catch (KeywordIndexPerformanceException e) {
            LOG.error("Encountered error during performance test: " +
                                e.getMessage());
        }
    }

    private static KeywordIndexPerformanceConfig parseArgs(String[] args) {
        KeywordIndexPerformanceConfig config = new KeywordIndexPerformanceConfig();
        int numFilesToShare=0;

        if (args.length >= 1) {
            try {
                numFilesToShare = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // use default
                LOG.error("Invalid argument: " + args[0] + ". Must specify number of files " +
                          "to index for this performance test. " +
                        " Using default of " + KeywordIndexPerformanceConfig.DEFAULT_NUMBER_FILES_INDEXED);
            }
        }

        if (numFilesToShare > 10) {
            config.setNumberOfFilesIndexed(numFilesToShare);
        }
        return config;
    }

    public void execute() throws KeywordIndexPerformanceException {

        // perform search
        searcher.execute();

        // analyze the results
        analyzer.execute();
        
    }
}