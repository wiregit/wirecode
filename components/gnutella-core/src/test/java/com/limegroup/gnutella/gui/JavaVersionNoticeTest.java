package com.limegroup.gnutella.gui;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.settings.QuestionsHandler;

public class JavaVersionNoticeTest extends BaseTestCase {

    public JavaVersionNoticeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(JavaVersionNoticeTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testUpgradeRequired() {
        assertTrue(JavaVersionNotice.upgradeRequired("1.4.0"));
        assertFalse(JavaVersionNotice.upgradeRequired("1.5.0"));
        assertFalse(JavaVersionNotice.upgradeRequired(JavaVersionNotice.REQUIRED));
    }

    public void testUpgradeRecommended() {
        assertFalse(JavaVersionNotice.upgradeRecommended("1.4.0"));
        assertFalse(JavaVersionNotice.upgradeRecommended("1.5.0"));
        assertFalse(JavaVersionNotice.upgradeRecommended("1.6.0"));
        assertTrue(JavaVersionNotice.upgradeRecommended("1.6.0-beta"));
        assertFalse(JavaVersionNotice.upgradeRecommended(JavaVersionNotice.RECOMMENDED_16));
    }

    public void testUpgradeRecommendedSetting() {
        String orginalValue = QuestionsHandler.LAST_CHECKED_JAVA_VERSION.getValue();
        try {
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue("1.4.0");
            assertTrue(JavaVersionNotice.upgradeRecommended("1.6.0-beta"));
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue("1.6.0-beta");
            assertFalse(JavaVersionNotice.upgradeRecommended("1.6.0-beta"));
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue("");
            assertTrue(JavaVersionNotice.upgradeRecommended("1.6.0-beta"));
        } finally {
            QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue(orginalValue);
        }
    }

}
