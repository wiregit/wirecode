
// Microsoft Visual Studio compiles this Windows native code into SystemUtilities.dll
// LimeWire uses these functions from the class com.limegroup.gnutella.util.SystemUtils

// Include the standard Windows DLL header and more headers
#include "stdafx.h"
#include "SystemUtilities.h"
#include "Shell.h"
#include "Tray.h"

// Access the program, window, and icon handles
extern CSystemUtilities Handle;

// icon is the path to a .ico file, or blank to use the icon in the .exe launcher that's running
// tip is the text for the tooltip, like "LimeWire"
// Adds the tray icon, and calls a Java method when the user clicks it
// Make a thread to call this method, you won't get it back until you remove the tray icon
// The thread has to be a non-daemon thread
// Returns false right away on error, or true on success much later when you remove the icon
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_addTrayIconNative(JNIEnv *e, jclass c, jstring icon, jstring tip) {
	return TrayAdd(e, c, GetJavaString(e, icon), GetJavaString(e, tip));
}
bool TrayAdd(JNIEnv *e, jclass c, LPCTSTR icon, LPCTSTR tip) {

	// The tray icon is already there
	if (Handle.Window) return false;

	// Save and get Java objects to be able to call from this native code back into Java
	Handle.Environment = e;
	Handle.Class = c;
	Handle.Method = e->GetStaticMethodID(
		c,                 // Java class
		"clickedTrayIcon", // Name of the Java method in that class
		"(I)V");           // Text that describes what the method takes and returns, takes an int, returns void
	if (!Handle.Method) return false;

	// If we don't already have the icons, load them from the given .ico file, or from our running .exe
	GetIcons(icon);

	// Register the window class for a window that won't show up on the screen, but will get messages about clicks on the tray icon
	if (!Handle.Instance) return false;
	WNDCLASSEX windowinfo;
	windowinfo.cbSize        = sizeof(WNDCLASSEX);           // Size of this structure
	windowinfo.style         = 0;                            // Default style
	windowinfo.lpfnWndProc   = WindowProcedure;              // Function pointer to the window procedure
	windowinfo.cbClsExtra    = 0;                            // No extra bytes
	windowinfo.cbWndExtra    = 0;
	windowinfo.hInstance     = Handle.Instance;              // Instance handle
	windowinfo.hIcon         = NULL;                         // This window will never show, so it doesn't need icons or a mouse pointer
	windowinfo.hIconSm       = NULL;
	windowinfo.hCursor       = NULL;
	windowinfo.hbrBackground = (HBRUSH)(COLOR_3DFACE + 1);   // Background brush
	windowinfo.lpszMenuName  = NULL;                         // Menu
	windowinfo.lpszClassName = "LimeWireSystemUtilitiesDll"; // Window class name
	RegisterClassEx(&windowinfo);

	// Create the window and save the handle
	Handle.Window = CreateWindow(
		"LimeWireSystemUtilitiesDll",                               // Registered window class name
		tip,                                                        // Window name, text that would be displayed in the title bar
		WS_OVERLAPPEDWINDOW,                                        // Window style
		CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, CW_USEDEFAULT, // Window position and size
		NULL,                                                       // Handle to parent window
		NULL,                                                       // No menu
		Handle.Instance,                                            // Handle to application instance
		NULL);                                                      // No optional window creation data
	if (!Handle.Window) return false;

	// Add the tray icon
	if (!Handle.SmallIcon) return false;
	NOTIFYICONDATA info;
	info.cbSize           = sizeof(info);                     // Size of this structure
	info.hWnd             = Handle.Window;                    // Handle to the window that will receive messages
	info.uID              = 0;                                // Program-defined identifier
	info.uFlags           = NIF_MESSAGE | NIF_ICON | NIF_TIP; // Mask for message, icon, and tip
	info.uCallbackMessage = MESSAGE_TRAY;                     // Program-defined message identifier
	info.hIcon            = Handle.SmallIcon;                 // Icon
	lstrcpyn(info.szTip, tip, 64);                            // 64-character buffer for tooltip text
	Shell_NotifyIcon(NIM_ADD, &info);

	// Enter the message loop of the hidden window
	MSG message;
	while (GetMessage(&message, NULL, 0, 0)) { // When GetMessage gets the quit message, it will return zero

		// Process the message, calls WindowProcedure below
		TranslateMessage(&message);
		DispatchMessage(&message);
	}

	// Another thread removed the icon and closed the hidden window, clear the handle and report success
	Handle.Window = NULL;
	return true;
}

// Message loop for the hidden window that gets messages when the user clicks the tray icon
LRESULT CALLBACK WindowProcedure(HWND window, UINT message, WPARAM wparam, LPARAM lparam) {

	// Sort based on the message
	switch (message) {

	// The program is closing, the WM_CLOSE message leads to this one
	case WM_DESTROY:

		// Exit the message loop
		PostQuitMessage(0);
		return(0);

	// A message from our tray icon
	case MESSAGE_TRAY:

		// Sort based on the message parameter
		switch (lparam) {

		// The user double-clicked the tray icon
		case WM_LBUTTONDBLCLK:

			// Call the Java method, reporting the double-click with a 2
			if (Handle.Environment) (Handle.Environment)->CallStaticIntMethod(Handle.Class, Handle.Method, 2);
			break;

		// The user right-clicked the tray icon
		case WM_RBUTTONUP:

			// Call the Java method, reporting the right-click with a 3
			if (Handle.Environment) (Handle.Environment)->CallStaticIntMethod(Handle.Class, Handle.Method, 3);
			break;
		}
	}

	// Pass unprocessed messages to Windows
	return(DefWindowProc(window, message, wparam, lparam));
}

// Remove the tray icon
JNIEXPORT jboolean JNICALL Java_com_limegroup_gnutella_util_SystemUtils_removeTrayIconNative(JNIEnv *e, jclass c) {
	return TrayRemove();
}
bool TrayRemove() {

	// There is no tray icon to remove
	if (!Handle.Window) return false;

	// Remove the tray icon
	NOTIFYICONDATA info;
	info.cbSize = sizeof(info);  // Size of this structure
	info.hWnd   = Handle.Window; // Handle to the window that will get messages
	info.uID    = 0;             // Program-defined identifier
	Shell_NotifyIcon(NIM_DELETE, &info);

	// Close the hidden window, causing the thread stuck in its message loop above to exit
	SendMessage(Handle.Window, WM_CLOSE, 0, 0);

	// Report success
	return true;
}
