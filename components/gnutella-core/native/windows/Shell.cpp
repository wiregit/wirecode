
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Include the standard Windows DLL header and more headers
#include "stdafx.h"
#include "SystemUtilities.h"
#include "Shell.h"

// Access the program, window, and icon handles
extern CSystemUtilities Handle;

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
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_util_SystemUtils_openURLNative(JNIEnv *e, jclass c, jstring url) {
	Run(GetJavaString(e, url));
}
JNIEXPORT void JNICALL Java_com_limegroup_gnutella_util_SystemUtils_openFileNative(JNIEnv *e, jclass c, jstring path) {
	Run(GetJavaString(e, path));
}
void Run(LPCTSTR path) {

	// Call ShellExecute() with all the defaults, this acts exactly like Run on the Start menu, and returns immediately
	ShellExecute(NULL, NULL, path, "", "", SW_SHOWNORMAL);
}

// Takes a path to a file on the disk, like "C:\Folder\file.ext", or a whole folder like "C:\Folder" without a trailing slash
// Moves it to the Windows Recycle Bin
// Returns false on error
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_recycleNative(JNIEnv *e, jclass c, jstring path) {
	return Recycle(GetJavaString(e, path));
}
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
JNIEXPORT jint JNICALL Java_com_limegroup_gnutella_util_SystemUtils_setFileWriteable(JNIEnv *e, jclass c, jstring path) {
	return SetFileWritable(GetJavaString(e, path));
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
	HMODULE module = LoadLibrary("user32.dll"); // If the DLL is already in our process space, LoadLibrary() will just get its handle
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
	return MakeJavaString(e, SetWindowIcon(e, c, frame, GetJavaString(e, bin), GetJavaString(e, icon)));
}
CString SetWindowIcon(JNIEnv *e, jclass c, jobject frame, LPCTSTR bin, LPCTSTR icon) {

	// Get the Window handle from Java
	CString message;
	HWND window = GetJavaWindowHandle(e, c, frame, bin, &message);
	if (!window) return message; // Return the message that tells what happened

	// If we don't already have the icons, load them from the given .ico file, or from our running .exe
	GetIcons(icon);

	// Set both sizes of the window's icon
	if (Handle.Icon)      SendMessage(window, WM_SETICON, ICON_BIG,   (LPARAM)Handle.Icon);
	if (Handle.SmallIcon) SendMessage(window, WM_SETICON, ICON_SMALL, (LPARAM)Handle.SmallIcon);
	return ""; // Return blank on success
}

// Takes a path to a .ico file on the disk, or blank to load the icons from our running .exe
// Loads the icons, keeping their handles in Handle.Icon and Handle.SmallIcon
void GetIcons(LPCTSTR icon) {

	// Don't load the icons twice
	if (Handle.Icon || Handle.SmallIcon) return;

	// No path to .ico file, we should load the icon from the .exe launcher
	if (CString(icon) == CString("")) {

		// Get the path to the program that is us running
		CString path = GetRunningPath();

		// Load the large and small icons from the program
		ExtractIconEx(
			path,                // Path to the .exe file with the icon
			0,                   // Extract the first icon in the program
			&(Handle.Icon),      // Handle for large icon
			&(Handle.SmallIcon), // Handle for small icon
			1);                  // Extract 1 set of icons

	// Load the icons from the given .ico file on the disk
	} else {

		Handle.Icon      = (HICON)LoadImage(NULL, icon, IMAGE_ICON, 32, 32, LR_LOADFROMFILE);
		Handle.SmallIcon = (HICON)LoadImage(NULL, icon, IMAGE_ICON, 16, 16, LR_LOADFROMFILE);
	}
}
