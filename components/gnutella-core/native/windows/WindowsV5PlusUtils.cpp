#include "jni.h"
#include <windows.h>

/**
 * Retrieves the idle time.  Available only on Windows2000+.
 */  
extern "C"
JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime
  (JNIEnv *env, jclass clazz) {
    
    LASTINPUTINFO lpi;
    lpi.cbSize = sizeof(lpi);
    GetLastInputInfo(&lpi);
    DWORD dwStart = GetTickCount();
    return (jlong)(dwStart - lpi.dwTime);
}

/**
 * Returns the number of Megabytes free on a given drive, or -1 it there was an error (no drive, etc)
 */  
extern "C"
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_getFreeDiskSpaceMB
(JNIEnv *env, jclass clazz, jstring drive)
{
	typedef BOOL (WINAPI *PGETDISKFREESPACEEX)(LPCSTR,
	PULARGE_INTEGER, PULARGE_INTEGER, PULARGE_INTEGER);
	PGETDISKFREESPACEEX pGetDiskFreeSpaceEx;

	pGetDiskFreeSpaceEx = (PGETDISKFREESPACEEX) GetProcAddress( 
                           GetModuleHandle("kernel32.dll"),
                          "GetDiskFreeSpaceExA");


	long retVal=-1;
	const char* szDrive;
    szDrive = env->GetStringUTFChars(drive, NULL);

	ULARGE_INTEGER avail, total, totalFree;

	BOOL bRet=FALSE;

	if( pGetDiskFreeSpaceEx )
		bRet=pGetDiskFreeSpaceEx(
			 szDrive,
			&avail,
			&total,
			&totalFree );
	
	if( bRet!=FALSE )
		retVal=avail.QuadPart>>20;	//	divide by 1024*1024 to get MB instead of B

	env->ReleaseStringUTFChars(drive, szDrive);

	return (jint)retVal;
}

//#define TESTING
#ifdef TESTING
extern "C"
JNIEXPORT jlong JNICALL Java_me_Foo_getFreeDiskSpaceMB
  (JNIEnv *env, jclass clazz, jstring drive) {
	  return Java_com_limegroup_gnutella_util_SystemUtils_getFreeDiskSpaceMB( env, clazz, drive );
}
#endif





