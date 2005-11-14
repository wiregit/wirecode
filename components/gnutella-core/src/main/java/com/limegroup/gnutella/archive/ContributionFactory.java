package com.limegroup.gnutella.archive;

public class ContributionFactory {
	
	private ContributionFactory() {}

	public static Contribution createContribution( String username, String password, 
			String title, int media) {
		return new DirectContribution( username, password, title, media );
	}
	
	public static Contribution createContribution( String username, String password, 
			String title, int media, int collection, int type ) {
		return new DirectContribution( username, password, title, media, collection, type );
	}

}
