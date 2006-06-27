package com.limegroup.gnutella.archive;

/**
 * 
 * A contribution consists of one or more files that we upload to a location
 * such as the Internet Archive.
 * 
 * Follow these steps to do upload a contribution to the 
 * Internet Archive:
 * 
 * 	1.	create a Contribution object by calling ContributionFactory.createContribution()
 * 	2.	call reservetIdentifier() with your requested identifier
 * 	3.	if step 2 successful, call getVerificationUrl() to get the verification URL
 * 	4.	call addFile() for each file you want to add to the contribution
 * 	5.	call addListener() with your UploadListener
 * 	6.  call upload() to upload the contribution
 */
public enum ContributionState {
     NOT_CONNECTED,
     CONNECTED,
     FILE_STARTED,
     FILE_PROGRESSED,
     FILE_COMPLETED,
     CHECKIN_STARTED,
     CHECKIN_COMPLETED;   
}