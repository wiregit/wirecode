#!/contrib/bin/tcsh
foreach test (0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15)
    #foreach test (0 1 2 3 4 5 6 7 8 9 10)
    foreach iter (0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)
        echo "Iteration " $iter ", test " $test
        rm -rf Incomplete/  
        rm -f DownloadTester2834343.out
        java com.limegroup.gnutella.tests.downloader.DownloadTester $test
        #mv log.log $iter.$test
    end
end
