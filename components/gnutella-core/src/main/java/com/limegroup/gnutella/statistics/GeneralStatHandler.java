package com.limegroup.gnutella.statistics;

final class GeneralStatHandler extends AbstractStatHandler {

	GeneralStatHandler() {
		super(new AdvancedStatistic(), new AdvancedKilobytesStatistic(),
			  new AdvancedStatistic(), new AdvancedKilobytesStatistic());
	}
}
