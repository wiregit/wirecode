/*
This library gets idle calls from systems that have X and screensavers installed.
(which means practically any linux user that would use limewire ;-))
*/

#include "com_limegroup_gnutella_util_SystemUtils.h"

#  include <X11/Xlib.h>
#  include <X11/Xutil.h>
# include <X11/X.h>
#  include <X11/extensions/scrnsaver.h>
//#include <iostream.h>


//will do these later
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setOpenFileLimit0
  (JNIEnv *, jclass, jint){return 0;}
  
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setFileWriteable
  (JNIEnv *, jclass, jstring){return 0;}
  
  
//gets the idle time from X
JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime
  (JNIEnv *, jclass){
  //cout << "entered method\n";
  
  	Display *display = XOpenDisplay(NULL);
	
	//cout << "got display\n";
	if (display == NULL)
		return -1;
		
	//cout << "display not null\n";
	
	Window window = DefaultRootWindow(display);
	
	//cout << "\ngot window\n";
	
  	XScreenSaverInfo *mit_info = NULL;
  	int event_base, error_base, idle_time;
		if (XScreenSaverQueryExtension(display, &event_base, &error_base)) {
	//		cout << "system supports screensavers\n";
			
			if (mit_info == NULL) {
				mit_info = XScreenSaverAllocInfo();
			}
			XScreenSaverQueryInfo(display, window, mit_info);
			
			idle_time = (mit_info->idle) / 1000;
			
	//		cout <<"queried successfully " << idle_time <<"\n";
		} else
			idle_time = 0;
			
	XFree(mit_info);
	XCloseDisplay(display);
	return idle_time;
  
  }
  
  int main() {return 0;}