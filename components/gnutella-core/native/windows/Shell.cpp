
// Include the standard Windows DLL header which we've edited to include the Java headers and more headers
#include "stdafx.h"

// Returns the path of this running program, like "C:\Folder\Program.exe", or blank if error
JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_getRunningPathNative(JNIEnv *e, jclass c) {
	return MakeJavaString(e, GetRunningPath());
}
CString GetRunningPath() {

	// Ask Windows for our path
	TCHAR bay[MAX_PATH];
	if (!GetModuleFileName(NULL, bay, MAX_PATH)) return("");
	return bay;
}

// Takes a path to a file like "C:\Folder\Song.mp3" or a Web address like "http://www.site.com/"
// Opens it with the default program or the default Web browser
void Run(LPCTSTR path) {

	// Call ShellExecute() with all the defaults
	ShellExecute(NULL, NULL, path, "", "", SW_SHOWNORMAL); // This acts exactly like Run on the Start menu
}

// Takes a path to a file on the disk, like "C:\Folder\file.ext", or a whole folder like "C:\Folder" without a trailing slash
// Moves it to the Windows Recycle Bin
// Returns false on error
bool Recycle(LPCTSTR path) {

	// Make a buffer that contains the text of the path followed by 2 null terminators, as SHFILEOPSTRUCT.pFrom requires
	CString s = path;                                 // Make a CString from the given path text
	int length = lstrlen(s);                          // Get the number of characters not including the null terminator, "hello" is 5
	LPTSTR buffer = s.GetBufferSetLength(length + 1); // Expand the buffer to hold one more character before the null terminator, like "hello0-"
	buffer[length + 1] = 0;                           // Set the byte beyond the null terminator to 0, making it "hello00"

	// Move the file to the Recycle Bin
	SHFILEOPSTRUCT info;                 // Make a shell file operation structure to fill out with the details of the operation
	ZeroMemory(&info, sizeof(info));     // Zero the memory of the structure, setting parts not mentioned here to NULL and 0
	info.wFunc = FO_DELETE;              // Delete operation
	info.pFrom = buffer;                 // The path and file name, terminated by 2 zero bytes
	info.fFlags = FOF_ALLOWUNDO      |   // Move the file into the Recycle Bin instead of deleting it
		          FOF_NOCONFIRMATION |   // Don't ask the user if they're sure
				  FOF_NOERRORUI      |   // Don't show the user an error if one happens
				  FOF_SILENT;            // Hide the progress bar dialog box
	int result = SHFileOperation(&info); // Have the Windows shell perform the operation, and get the result code

	// Remember to release the CString buffer we obtained
	s.ReleaseBuffer();

	// If SHFileOperation() succeeds, it returns 0, have this method return true
	return !result;
}

// Takes a path to a file on the disk, like "C:\Folder\file.txt"
// Removes its read-only setting
// Returns the result from _chmod
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setFileWriteable(JNIEnv *e, jclass c, jstring j) {
	return SetFileWritable(GetString(e, j));
}
int SetFileWritable(LPCTSTR path) {

	// Use the Windows implementation of the Unix _chmod file function
	return _chmod(path, _S_IWRITE);
}

// Returns the tick count when the user last moved the mouse or pressed a key, or 0 on error
JNIEXPORT jlong JNICALL Java_com_limegroup_gnutella_util_SystemUtils_idleTime(JNIEnv *e, jclass c) {
	return GetIdleTime();
}
DWORD GetIdleTime() {

	// Get a function pointer to GetLastInputInfo() in user32.dll, which won't be there in Windows 98
	HMODULE module = GetModuleHandle("user32.dll");
	if (!module) module = LoadLibrary("user32.dll"); // The DLL is probably already in our process space
	if (!module) return 0;
	GetLastInputInfoSignature call = (GetLastInputInfoSignature)GetProcAddress(module, "GetLastInputInfo");
	if (!call) return 0;

	// Make an 8-byte LASTINPUTINFO structure for GetLastInputInfo() to write its answer in
	LASTINPUTINFO info;
	info.cbSize = sizeof(info); // Write the size at the start to tell what version this is
	info.dwTime = 0;

	// Get the tick count when the user last gave the computer input
	BOOL result = call(&info);
	if (!result) return 0; // GetLastInputInfo() returns 0 on error
	DWORD user = info.dwTime; // Pull out the information the function wrote

	// Get the tick count now
	DWORD now = GetTickCount();

	// Return the number of milliseconds the user has been away
	if (user <= now) return now - user;
	else return ((DWORD)0) - user + now; // Handle the 49.7 day rollover case
}

// Takes the JNI environment and class
// The jobject frame is a AWT Component like a JFrame that is backed by a real Windows window
// bin is the path to the folder that has the file "jawt.dll", like "C:\Program Files\Java\jre1.5.0_05\bin"
// icon is the path to a Windows .ico file on the disk
// Gets the window handle, and uses it to set the icon.
// Returns blank on success, or a text message about what didn't work
// Do not call this function repeatedly, it creates two icon resources for each call
JNIEXPORT jstring JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setWindowIconNative(JNIEnv *e, jclass c, jobject frame, jstring bin, jstring icon) {
	return MakeJavaString(e, SetWindowIcon(e, c, frame, GetString(e, bin), GetString(e, icon)));
}
CString SetWindowIcon(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, LPCTSTR icon) {

	// Make variables for the window handle we'll get, and the message we'll return
	HWND handle = NULL;
	CString message = "Start of method";

	// Make sure neither of the paths are blank
	if (bin == CString("") || icon == CString("")) return CString("Blank paths");

	// Make a JAWT structure that will tell Java we're using Java 1.4
	JAWT awt;
	awt.version = JAWT_VERSION_1_4;

	// Load jawt.dll into our process space
	CString path = CString(bin) + CString("\\jawt.dll"); // Compose the complete path to the DLL, like "C:\Program Files\Java\jre1.5.0_05\bin\jawt.dll"
	HMODULE module = GetModuleHandle(path); // The DLL may already by in our process space
	if (!module) module = LoadLibrary(path);
	if (module) {
		message = "Got module";

		// Get a function pointer to JAWT_GetAWT() in the DLL
		JawtGetAwtSignature JawtGetAwt = (JawtGetAwtSignature)GetProcAddress(module, "_JAWT_GetAWT@8");
		if (JawtGetAwt) {
			message = "Got signature";

			// Access Java's Active Widget Toolkit
			jboolean result = JawtGetAwt(e, &awt);
			if (result != JNI_FALSE) {
				message = "Got AWT";

				// Get the drawing surface
				JAWT_DrawingSurface *surface = awt.GetDrawingSurface(e, frame);
				if (surface) {
					message = "Got surface";

					// Lock the drawing surface
					jint lock = surface->Lock(surface);
					if ((lock & JAWT_LOCK_ERROR) == 0) { // If the error bit is not set, keep going
						message = "Locked surface";

						// Get the drawing surface information
						JAWT_DrawingSurfaceInfo *info = surface->GetDrawingSurfaceInfo(surface);
						if (info) {
							message = "Got surface information";

							// Get the Windows-specific drawing surface information
							JAWT_Win32DrawingSurfaceInfo *win = (JAWT_Win32DrawingSurfaceInfo*)info->platformInfo;
							if (win) {
								message = "Got platform-specific surface information";

								// Get the window handle
								handle = win->hwnd;
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

	// Make sure we were able to get the handle
	if (!handle) return message;

	// Open the .ico file, getting handles to the large and small icons inside it
	HICON bigicon   = (HICON)LoadImage(NULL, icon, IMAGE_ICON, 32, 32, LR_LOADFROMFILE);
	HICON smallicon = (HICON)LoadImage(NULL, icon, IMAGE_ICON, 16, 16, LR_LOADFROMFILE);
	if (!bigicon || !smallicon) return CString("Unable to open icon file");

	/*
	 * It is important that you do not call this function repeatedly.
	 * Each LoadImage call above creates a HICON that leads to an icon resource.
	 * Windows graphics resources like icons take up a lot of memory.
	 * A Windows program should free icons with a call to DestroyIcon(HICON).
	 * We can't free them now, because they are on display in our window.
	 * When the Windows process exits, Windows will free the two icons.
	 */

	// Set both sizes of the window's icon
	SendMessage(handle, WM_SETICON, ICON_BIG,   (LPARAM)bigicon);
	SendMessage(handle, WM_SETICON, ICON_SMALL, (LPARAM)smallicon);
	return CString(""); // Return blank on success
}
