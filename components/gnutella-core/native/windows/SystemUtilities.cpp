
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Include the standard Windows DLL header which we've edited to include the Java headers and more headers
#include "stdafx.h"

// A Windows DLL has a DllMain method that Windows calls when it loads the DLL
BOOL APIENTRY DllMain(HANDLE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {

	// Indicate success
	return TRUE;
}

// Takes the JNI environment object, and a Java string
// Safely gets the text from the Java string, copies it into a new CString, and frees the Java string
// Returns the CString
CString GetString(JNIEnv *e, jstring j) {

	// If j is a null pointer, just return a blank string
	if (!j) return CString("");

	// Get a character pointer into the text characters of the Java string
	const char *c = e->GetStringUTFChars(j, FALSE);

	// Make a new Windows CString object with that text
	CString s = c; // The CString constructor copies the characters it reads at c into memory it manages

	// Tell the JNI environment that we're done with the character pointer
	e->ReleaseStringUTFChars(j, c);

	// Return the CString we made
	return s;
}

// Takes the JNI environment object, and a pointer to text, like a CString cast to LPCTSTR
// Makes a new Java string from the text that we can return back to Java
// Returns the jstring object
jstring MakeJavaString(JNIEnv *e, LPCTSTR t) {

	// Have the JNI environment make a new Java string from the given text, and return it
	return e->NewStringUTF(t);
}
