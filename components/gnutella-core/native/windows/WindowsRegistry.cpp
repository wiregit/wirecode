
// Include the standard Windows DLL header which we've edited to include the Java headers and more headers
#include "stdafx.h"

// Takes a text name of a registry root key, like "HKEY_LOCAL_MACHINE"
// Returns the HKEY value Windows defines for it, or NULL if not found
HKEY RegistryName(LPCTSTR name) {

	// Look at the text name to return the matching registry root key handle value
	CString s = name;
	if      (s == "HKEY_CLASSES_ROOT")   return HKEY_CLASSES_ROOT;
	else if (s == "HKEY_CURRENT_CONFIG") return HKEY_CURRENT_CONFIG;
	else if (s == "HKEY_CURRENT_USER")   return HKEY_CURRENT_USER;
	else if (s == "HKEY_LOCAL_MACHINE")  return HKEY_LOCAL_MACHINE;
	else if (s == "HKEY_USERS")          return HKEY_USERS;
	else return NULL;
}

// Takes a root key handle and a key path, and the desired level of access
// Opens or creates and opens the key with full access
// Returns false on error
bool CRegistry::Open(HKEY root, LPCTSTR path, DWORD access) {

	// Open or create and open the key
	HKEY k;
	DWORD info;
	int result = RegCreateKeyEx(
		root,                    // Handle to open root key
		path,                    // Subkey name
		0,
		"",
		REG_OPTION_NON_VOLATILE, // Save information in the registry file
		access,                  // Given access flags
		NULL,
		&k,                      // The opened or created key handle is put here
		&info);                  // Tells if the key was opened or created and opened
	if (result != ERROR_SUCCESS) return false;

	// Save the open key in this CRegistry object
	key = k;
	return true;
}

// Takes a root key handle, a key path, a registry variable name, and access to an integer to write the value
// Gets the information from the registry
// Writes i and returns true, or false if not found or any error
bool RegistryReadNumber(HKEY root, LPCTSTR path, LPCTSTR name, int *i) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_READ)) return false;

	// Read the number value
	DWORD d;
	DWORD size = sizeof(DWORD);
	int result = RegQueryValueEx(
		registry.key, // Handle to an open key
		name,         // Name of the value to read
		0,
		NULL,
		(LPBYTE)&d,   // Data buffer
		&size);       // Size of data buffer
	if (result != ERROR_SUCCESS) return false;

	// Take the number
	*i = d;
	return true;
}

// Takes a root key handle, a key path, a registry variable name, and access to a string to write the value
// Gets the information from the registry
// Writes s and returns true, or false if not found or any error
bool RegistryReadText(HKEY root, LPCTSTR path, LPCTSTR name, CString *s) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_READ)) return false;

	// Get the size required
	DWORD size;
	int result = RegQueryValueEx(
		registry.key, // Handle to an open key
		name,         // Name of the value to read
		0,
		NULL,
		NULL,         // No data buffer, we're requesting the size
		&size);       // The function will write the required size here
	if (result != ERROR_SUCCESS) return false;

	// Open a string
	CString buffer_string;
	LPTSTR buffer_write = buffer_string.GetBuffer(size);

	// Read the binary data
	result = RegQueryValueEx(
		registry.key,         // Handle to an open key
		name,                 // Name of the value to read
		0,
		NULL,
		(LPBYTE)buffer_write, // Data buffer
		&size);               // Size of data buffer
	buffer_string.ReleaseBuffer();
	if (result != ERROR_SUCCESS) return false;

	// Take the string
	*s = buffer_string;
	return true;
}

// Takes a root key handle, a key path, a registry variable name, and an integer
// Stores the information in the registry
// Returns false on error
bool RegistryWriteNumber(HKEY root, LPCTSTR path, LPCTSTR name, int i) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_ALL_ACCESS)) return false;

	// Set or make and set the number value
	int result = RegSetValueEx(
		registry.key,     // Handle to an open key
		name,             // Name of the value to set or make and set
		0,
		REG_DWORD,        // Variable type is a 32-bit number
		(const BYTE *)&i, // Address of the value data to load
		sizeof(DWORD));   // Size of the value data
	if (result != ERROR_SUCCESS) return false;
	return true;
}

// Takes a root key handle, a key path, a registry variable name, and value text
// Stores the information in the registry
// Returns false on error
bool RegistryWriteText(HKEY root, LPCTSTR path, LPCTSTR name, LPCTSTR t) {

	// Open the key
	CRegistry registry;
	if (!registry.Open(root, path, KEY_ALL_ACCESS)) return false;

	// Set or make and set the text value
	int result = RegSetValueEx(
		registry.key,       // Handle to an open key
		name,               // Name of the valeu to set or make and set
		0,
		REG_SZ,             // Variable type is a null-terminated string
		(const BYTE *)t,    // Address of the value data to load
		lstrlen(t) + 1);    // Size of the value data, add 1 to write the null terminator
	if (result != ERROR_SUCCESS) return false;
	return true;
}

// Takes a root key handle, the path to a key that has no subkeys, and a registry variable name, or blank to delete the key
// Deletes the registry variable or key from the registry
// Returns false on error
bool RegistryDelete(HKEY root, LPCTSTR path, LPCTSTR name) {

	// Delete the registry key variable
	int result;
	if (CString(name) != "") {

		// Open the key
		CRegistry registry;
		if (!registry.Open(root, path, KEY_ALL_ACCESS)) return false;

		// Delete the variable
		result = RegDeleteValue(registry.key, name);
		if (result != ERROR_SUCCESS && result != ERROR_FILE_NOT_FOUND) return false;
		return true;

	// Delete the registry key
	} else {

		// Delete the key
		result = RegDeleteKey(root, path);
		if (result != ERROR_SUCCESS && result != ERROR_FILE_NOT_FOUND) return false;
		return true;
	}
}
