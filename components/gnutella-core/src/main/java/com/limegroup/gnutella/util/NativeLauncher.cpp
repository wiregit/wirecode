#include "com_limegroup_gnutella_util_NativeLauncher.h"
#include "windows.h"

extern "C"
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_util_NativeLauncher_nativeLaunchWindowsFile
	(JNIEnv *env, jobject object, jstring fileName) {
	const char* cFileName;
	cFileName = env->GetStringUTFChars(fileName, NULL);
	ShellExecute(NULL, "open", cFileName, "", "", 3);
	env->ReleaseStringUTFChars(fileName, cFileName);
}
