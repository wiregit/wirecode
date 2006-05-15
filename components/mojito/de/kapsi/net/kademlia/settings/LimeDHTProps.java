/*
 * Lime Kademlia Distributed Hash Table (DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.kapsi.net.kademlia.settings;


/**
 * 
 */
public class LimeDHTProps /* extends AbstractSettings */ {
    
    private static final LimeDHTProps INSTANCE = new LimeDHTProps();
    
    // The FACTORY is used for subclasses of LimeProps, so they know
    // which factory to add classes to.
    //protected static final SettingsFactory FACTORY = INSTANCE.getFactory();
    protected static final SettingsFactory FACTORY = new SettingsFactory();
    
    // This is protected so that subclasses can extend from it, but
    // subclasses should NEVER instantiate a copy themselves.
    protected LimeDHTProps() {
        /*super("dht.props", "Lime DHT properties file");
        Assert.that( getClass() == LimeDHTProps.class,
            "should not have a subclass instantiate");*/
    }
    
    /**
     * Returns the only instance of this class.
     */
    public static LimeDHTProps instance() { return INSTANCE; }
    
    // STUBS STUBS STUBS STUBS STUBS STUBS STUBS STUBS STUBS STUBS STUBS
    
    protected static class SettingsFactory {
        
        private SettingsFactory() {}
        
        public BooleanSetting createBooleanSetting(String key, boolean value) {
            return new BooleanSetting(value);
        }
        
        public BooleanSetting createSettableBooleanSetting(String key, boolean value, String simppKey) {
            return new BooleanSetting(value);
        }
        
        public IntSetting createIntSetting(String key, int value) {
            return new IntSetting(value);
        }
        
        public IntSetting createSettableIntSetting(String key, int value, String simppKey, int min, int max) {
            return new IntSetting(value);
        }
        
        public LongSetting createLongSetting(String key, long value) {
            return new LongSetting(value);
        }
        
        public LongSetting createSettableLongSetting(String key, long value, String simppKey, long min, long max) {
            return new LongSetting(value);
        }
        
        public synchronized ByteSetting createByteSetting(String key, byte value) {
            return new ByteSetting(value);
        }
    }
    
    public static class BooleanSetting {
        private boolean value;
        public BooleanSetting(boolean value) { this.value = value; }
        public void setValue(boolean value) { this.value = value; }
        public boolean getValue() { return value; }
    }

    public static class IntSetting {
        private int value;
        public IntSetting(int value) { this.value = value; }
        public void setValue(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    public static class ByteSetting {
        private byte value;
        public ByteSetting(byte value) { this.value = value; }
        public void setValue(byte value) { this.value = value; }
        public byte getValue() { return value; }
    }
    
    public static class LongSetting {
        private long value;
        public LongSetting(long value) { this.value = value; }
        public void setValue(long value) { this.value = value; }
        public long getValue() { return value; }
    }
}
