
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
CString GetJavaString(JNIEnv *e, jstring j) {

	// If j is a null pointer, just return a blank string
	if (!j) return "";

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

// Takes the JNI environment and class
// The jobject frame is a AWT Component like a JFrame that is backed by a real Windows window
// bin is the path to the folder that has the file "jawt.dll", like "C:\Program Files\Java\jre1.5.0_05\bin"
// Gets the window handle from Java, and returns it
// Sets message to report what happened
HWND GetJavaWindowHandle(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, CString *message) {

	// Make a variable for the window handle we'll get
	HWND window = NULL;
	*message = "Start of method";

	// Make sure the path isn't blank
	if (bin == CString("")) { *message = "Blank path"; return NULL; }

	// Make a JAWT structure that will tell Java we're using Java 1.4
	JAWT awt;
	awt.version = JAWT_VERSION_1_4;

	// Load jawt.dll into our process space
	CString path = CString(bin) + CString("\\jawt.dll"); // Compose the complete path to the DLL, like "C:\Program Files\Java\jre1.5.0_05\bin\jawt.dll"
	HMODULE module = LoadLibrary(path); // If the DLL is already in our process space, LoadLibrary() will just get its handle
	if (module) {
		*message = "Got module";

		// Get a function pointer to JAWT_GetAWT() in the DLL
		JawtGetAwtSignature JawtGetAwt = (JawtGetAwtSignature)GetProcAddress(module, "_JAWT_GetAWT@8");
		if (JawtGetAwt) {
			*message = "Got signature";

			// Access Java's Active Widget Toolkit
			jboolean result = JawtGetAwt(e, &awt);
			if (result != JNI_FALSE) {
				*message = "Got AWT";

				// Get the drawing surface
				JAWT_DrawingSurface *surface = awt.GetDrawingSurface(e, frame);
				if (surface) {
					*message = "Got surface";

					// Lock the drawing surface
					jint lock = surface->Lock(surface);
					if ((lock & JAWT_LOCK_ERROR) == 0) { // If the error bit is not set, keep going
						*message = "Locked surface";

						// Get the drawing surface information
						JAWT_DrawingSurfaceInfo *info = surface->GetDrawingSurfaceInfo(surface);
						if (info) {
							*message = "Got surface information";

							// Get the Windows-specific drawing surface information
							JAWT_Win32DrawingSurfaceInfo *win = (JAWT_Win32DrawingSurfaceInfo*)info->platformInfo;
							if (win) {
								*message = "Got platform-specific surface information";

								// Get the window handle
								window = win->hwnd;
							}
						}

						// Unlock the drawing surface
						surface->Unlock(surface);
					}

					// Free the drawing surface
					awt.FreeDrawingSurface(surface);
				}
			}
		}
	}

	// Return the window handle Java told us
	return window;
}
