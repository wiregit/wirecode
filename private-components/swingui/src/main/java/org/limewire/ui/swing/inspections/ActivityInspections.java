package org.limewire.ui.swing.inspections;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.limewire.core.api.support.SessionInfo;
import org.limewire.inject.EagerSingleton;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SwingInspectable;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.SystemUtils;

import com.google.inject.Inject;

/**
 * Set of inspections for general system and limewire activity. 
 */
@EagerSingleton
public class ActivityInspections implements Service {
    
    private final SessionInfo sessionInfo;
    
    /**
     * The point of time in milliseconds when the window was activated the first time.
     * <p> -1 if not activated.
     */
    private long activatedTime = -1;
    
    /**
     * The point of time in milliseconds when the window was deactivated the first time.
     * <p> -1 if not activated.
     */
    private long deactivatedTime = -1;
    
    @Inject
    public ActivityInspections(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }
    
    @Inject
    public void register(ServiceRegistry serviceRegistry) {
        serviceRegistry.register(this).in(ServiceStage.VERY_LATE);
    }
    
    @SuppressWarnings("unused")
    @InspectableContainer
    private final class LazyInspectableContainer {
    
        @InspectablePrimitive(value = "system idle time", category = DataCategory.USAGE)
        private final Inspectable idleTime = new GetIdleTimeInspectable();
        
        @InspectablePrimitive(value = "current session uptime in milliseconds", category = DataCategory.USAGE)
        private final Inspectable currentSessionUptimeMilliSecs = new GetCurrentUptimeMilliSecsInspectable();

        @InspectablePrimitive(value = "system time the session began ", category = DataCategory.USAGE)
        private final Inspectable currentSessionStartTime = new GetSessionStartTimeInspectable();
        
        @InspectablePrimitive(value = "limewire visible", category = DataCategory.USAGE)
        private final Inspectable limewireVisible = new IsLimewireVisibleInspectable();
        
        @InspectablePrimitive(value = "limewire activated", category = DataCategory.USAGE)
        private final Inspectable limewireActive = new IsLimewireActiveInspectable();
        
        @InspectablePrimitive(value = "time the limewire window has been activated", category = DataCategory.USAGE)
        private final Inspectable limewireActivatedTime = new GetActivatedTimeInspectable();
        
        @InspectablePrimitive(value = "time the limewire window has been deactivated", category = DataCategory.USAGE)
        private final Inspectable limewireDeactivatedTime = new GetDeactivatedTimeInspectable();
    }

    @Override
    public String getServiceName() {
        return "Primary UI Inspections Service";
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
                
        if (GuiUtils.getMainFrame() != null) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                public void run() {
                    GuiUtils.getMainFrame().addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowActivated(WindowEvent e) {
                            deactivatedTime = -1;
                            if (activatedTime == -1) { 
                                activatedTime = System.currentTimeMillis();
                            }
                        }
                        @Override
                        public void windowDeactivated(WindowEvent e) {
                            activatedTime = -1;
                            if (deactivatedTime == -1) { 
                                deactivatedTime = System.currentTimeMillis();
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public void stop() {
    }
    
    private class GetCurrentUptimeMilliSecsInspectable implements Inspectable {
        @Override
        public Object inspect() {
            return sessionInfo.getCurrentUptime();
        }
    }

    private class GetSessionStartTimeInspectable implements Inspectable {
        @Override
        public Object inspect() {
            return sessionInfo.getStartTime();
        }
    }
    
    private static class IsLimewireVisibleInspectable extends SwingInspectable {
        @Override
        public Object inspectOnEDT() {
            JFrame frame = GuiUtils.getMainFrame();
            if (frame == null) {
                return false;
            } else {
                return frame.isVisible();
            }
        }
    }
    
    private static class IsLimewireActiveInspectable extends SwingInspectable {
        @Override
        public Object inspectOnEDT() {
            JFrame frame = GuiUtils.getMainFrame();
            if (frame == null) {
                return false;
            } else {
                return frame.isActive();
            }
        }
    }
    
    private class GetActivatedTimeInspectable extends SwingInspectable {
        @Override
        public Object inspectOnEDT() {
            if (activatedTime != -1) {
                return System.currentTimeMillis() - activatedTime;
            } else {
                return -1;
            }
        }
    }

    private class GetDeactivatedTimeInspectable extends SwingInspectable  {
        @Override
        public Object inspectOnEDT() {
            if (deactivatedTime != -1) {
                return System.currentTimeMillis() - deactivatedTime;
            } else {
                return -1;
            }
        }
    }
    
    private static class GetIdleTimeInspectable extends SwingInspectable { 
        @Override
        public Object inspectOnEDT() {
            if (SystemUtils.supportsIdleTime()) {
                return SystemUtils.getIdleTime();
            } else {
                return -1;
            }
        }
    }

}
