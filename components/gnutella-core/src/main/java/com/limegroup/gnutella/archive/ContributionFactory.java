package com.limegroup.gnutella.archive;

public class ContributionFactory {
	
	private ContributionFactory() {}

	public static Contribution createContribution( String username, String password, 
			String title, String description, int media)
	throws DescriptionTooShortException {
		return new AdvancedContribution( username, password, title, description, media );
	}
	
	public static Contribution createContribution( String username, String password, 
			String title, String description, int media, int collection, int type )
	throws DescriptionTooShortException {
		return new AdvancedContribution( username, password, title, description, media, collection, type );
	}

}
