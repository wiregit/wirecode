/**
 * auth: afisk
 * file: NativeLauncher.cpp
 * desc: makes the native call to launch files with
 *       their associated applications.
 */

#include "com_limegroup_gnutella_util_NativeLauncher.h"
#include "windows.h"

extern "C"
	/** launches a file with its associated application on Windows. */
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_NativeLauncher_nativeLaunchFileWindows
	(JNIEnv *env, jclass jc, jstring fileName) {
	const char* cFileName;
	cFileName = env->GetStringUTFChars(fileName, NULL);
	HINSTANCE handle = ShellExecute(NULL, "open", cFileName, "", "", SW_SHOWNA);
	// free the memory for the string
	env->ReleaseStringUTFChars(fileName, cFileName);

	// it's ok to make this cast, since the HINSTANCE is not a true
	// window handle, but rather an error message holder.
	return (jint)handle;
}
