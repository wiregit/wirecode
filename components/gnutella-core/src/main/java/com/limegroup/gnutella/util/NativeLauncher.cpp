/**
 * auth: afisk
 * file: NativeLauncher.cpp
 * desc: makes the native call to launch files with
 *       their associated applications.
 */

#include "com_limegroup_gnutella_util_NativeLauncher.h"
#include "windows.h"

extern "C"
	/** @effects launches a file with its associated application on Windows. */
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_util_NativeLauncher_nativeLaunchFileWindows
	(JNIEnv *env, jclass jc, jstring fileName) {
	const char* cFileName;
	cFileName = env->GetStringUTFChars(fileName, NULL);
	ShellExecute(NULL, "open", cFileName, "", "", 3);
	env->ReleaseStringUTFChars(fileName, cFileName);
}
