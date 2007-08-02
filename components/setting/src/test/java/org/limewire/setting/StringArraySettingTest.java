package org.limewire.setting;

import java.util.Properties;

import org.limewire.util.BaseTestCase;

public class StringArraySettingTest extends BaseTestCase {

    public StringArraySettingTest(String name) {
        super(name);
    }

    public void testSetValueStringArray() {
        StringArraySetting setting = new StringArraySetting(new Properties(), new Properties(), "key");
        assertEquals(0, setting.getValue().length);
        setting.setValue("hello", "world");
        assertEquals(new String[] { "hello", "world" }, setting.getValue());
        setting.setValue();
        assertEquals(0, setting.getValue().length);
        setting.setValue("hello");
        assertEquals(new String[] { "hello" }, setting.getValue());
    }

}
