#!/contrib/bin/tcsh
foreach x (1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)
    echo $x
    rm -rf Incomplete/
    java com.limegroup.gnutella.tests.downloader.DownloadTester
    mv log.log $x
end
