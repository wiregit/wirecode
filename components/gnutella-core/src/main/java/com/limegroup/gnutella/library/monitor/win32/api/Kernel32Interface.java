/* Copyright (c) 2007 Timothy Wall, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package com.limegroup.gnutella.library.monitor.win32.api;

import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;

/** Definition (incomplete) of <code>kernel32.dll</code>. */
public interface Kernel32Interface extends StdCallLibrary, W32Errors {
    /** Standard options to use the unicode version of a w32 API. */
    Map<String, Object> UNICODE_OPTIONS = new HashMap<String, Object>() {
        {
            put(OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
            put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
        }
    };

    /** Standard options to use the ASCII/MBCS version of a w32 API. */
    Map<String, Object> ASCII_OPTIONS = new HashMap<String, Object>() {
        {
            put(OPTION_TYPE_MAPPER, W32APITypeMapper.ASCII);
            put(OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.ASCII);
        }
    };

    Map DEFAULT_OPTIONS = Boolean.getBoolean("w32.ascii") ? ASCII_OPTIONS : UNICODE_OPTIONS;

    Pointer LocalFree(Pointer hLocal);

    /**
     * Frees the specified global memory object and invalidates its handle.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa366579.aspx
     */
    Pointer GlobalFree(Pointer hGlobal);

    /**
     * Retrieves a module handle for the specified module. The module must have
     * been loaded by the calling process.
     * 
     * To avoid the race conditions described in the Remarks section, use the
     * GetModuleHandleEx function.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms683199(VS.85).aspx
     */
    HMODULE GetModuleHandle(String name);

    /**
     * Retrieves the current system date and time. The system time is expressed
     * in Coordinated Universal Time (UTC).
     * 
     * To retrieve the current system date and time in local time, use the
     * GetLocalTime function.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms724390(VS.85).aspx
     */
    void GetSystemTime(SYSTEMTIME result);

    /**
     * Retrieves the thread identifier of the calling thread.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms683183(VS.85).aspx
     */
    int GetCurrentThreadId();

    /**
     * Retrieves a pseudo handle for the calling thread.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms683182(VS.85).aspx
     */
    HANDLE GetCurrentThread();

    /**
     * Retrieves the process identifier of the calling process.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms683180(VS.85).aspx
     */
    int GetCurrentProcessId();

    /**
     * Retrieves a pseudo handle for the current process.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms683179(VS.85).aspx
     */
    HANDLE GetCurrentProcess();

    /**
     * Retrieves the process identifier of the specified process.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms683215(VS.85).aspx
     */
    int GetProcessId(HANDLE process);

    /**
     * This function retrieves the major and minor version numbers of the system
     * on which the specified process expects to run.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms885636.aspx
     */
    int GetProcessVersion(int processId);

    /**
     * Retrieves the calling thread's last-error code value. The last-error code
     * is maintained on a per-thread basis. Multiple threads do not overwrite
     * each other's last-error code.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms679360(VS.85).aspx
     */
    int GetLastError();

    /**
     * This function sets the last-error code for the calling thread.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa909251.aspx
     */
    void SetLastError(int dwErrCode);

    /**
     * Determines whether a disk drive is a removable, fixed, CD-ROM, RAM disk,
     * or network drive.
     * 
     * To determine whether a drive is a USB-type drive, call
     * SetupDiGetDeviceRegistryProperty and specify the SPDRP_REMOVAL_POLICY
     * property.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa364939.aspx
     */
    int GetDriveType(String rootPathName);

    int FORMAT_MESSAGE_ALLOCATE_BUFFER = 0x0100;

    int FORMAT_MESSAGE_FROM_SYSTEM = 0x1000;

    int FORMAT_MESSAGE_IGNORE_INSERTS = 0x200;

    /**
     * Formats a message string. The function requires a message definition as
     * input. The message definition can come from a buffer passed into the
     * function. It can come from a message table resource in an already-loaded
     * module. Or the caller can ask the function to search the system's message
     * table resource(s) for the message definition. The function finds the
     * message definition in a message table resource based on a message
     * identifier and a language identifier. The function copies the formatted
     * message text to an output buffer, processing any embedded insert
     * sequences if requested.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms679351.aspx
     */
    int FormatMessage(int dwFlags, Pointer lpSource, int dwMessageId, int dwLanguageId,
            PointerByReference lpBuffer, int nSize, Pointer va_list);

    /**
     * @see #FormatMessage(int, Pointer, int, int, PointerByReference, int,
     *      Pointer)
     */
    int FormatMessage(int dwFlags, Pointer lpSource, int dwMessageId, int dwLanguageId,
            Buffer lpBuffer, int nSize, Pointer va_list);

    int FILE_LIST_DIRECTORY = 0x00000001;

    int FILE_SHARE_READ = 1;

    int FILE_SHARE_WRITE = 2;

    int FILE_SHARE_DELETE = 4;

    int CREATE_NEW = 1;

    int CREATE_ALWAYS = 2;

    int OPEN_EXISTING = 3;

    int OPEN_ALWAYS = 4;

    int TRUNCATE_EXISTING = 5;

    int FILE_FLAG_WRITE_THROUGH = 0x80000000;

    int FILE_FLAG_OVERLAPPED = 0x40000000;

    int FILE_FLAG_NO_BUFFERING = 0x20000000;

    int FILE_FLAG_RANDOM_ACCESS = 0x10000000;

    int FILE_FLAG_SEQUENTIAL_SCAN = 0x08000000;

    int FILE_FLAG_DELETE_ON_CLOSE = 0x04000000;

    int FILE_FLAG_BACKUP_SEMANTICS = 0x02000000;

    int FILE_FLAG_POSIX_SEMANTICS = 0x01000000;

    int FILE_FLAG_OPEN_REPARSE_POINT = 0x00200000;

    int FILE_FLAG_OPEN_NO_RECALL = 0x00100000;

    int FILE_ATTRIBUTE_READONLY = 0x00000001;

    int FILE_ATTRIBUTE_HIDDEN = 0x00000002;

    int FILE_ATTRIBUTE_SYSTEM = 0x00000004;

    int FILE_ATTRIBUTE_DIRECTORY = 0x00000010;

    int FILE_ATTRIBUTE_ARCHIVE = 0x00000020;

    int FILE_ATTRIBUTE_DEVICE = 0x00000040;

    int FILE_ATTRIBUTE_NORMAL = 0x00000080;

    int FILE_ATTRIBUTE_TEMPORARY = 0x00000100;

    int FILE_ATTRIBUTE_SPARSE_FILE = 0x00000200;

    int FILE_ATTRIBUTE_REPARSE_POINT = 0x00000400;

    int FILE_ATTRIBUTE_COMPRESSED = 0x00000800;

    int FILE_ATTRIBUTE_OFFLINE = 0x00001000;

    int FILE_ATTRIBUTE_NOT_CONTENT_INDEXED = 0x00002000;

    int FILE_ATTRIBUTE_ENCRYPTED = 0x00004000;

    int DRIVE_UNKNOWN = 0;

    int DRIVE_NO_ROOT_DIR = 1;

    int DRIVE_REMOVABLE = 2;

    int DRIVE_FIXED = 3;

    int DRIVE_REMOTE = 4;

    int DRIVE_CDROM = 5;

    int DRIVE_RAMDISK = 6;

    int GENERIC_WRITE = 0x40000000;

    /**
     * Creates or opens a file or I/O device. The most commonly used I/O devices
     * are as follows: file, file stream, directory, physical disk, volume,
     * console buffer, tape drive, communications resource, mailslot, and pipe.
     * The function returns a handle that can be used to access the file or
     * device for various types of I/O depending on the file or device and the
     * flags and attributes specified.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa363858.aspx
     */
    HANDLE CreateFile(String lpFileName, int dwDesiredAccess, int dwShareMode,
            SECURITY_ATTRIBUTES lpSecurityAttributes, int dwCreationDisposition,
            int dwFlagsAndAttributes, HANDLE hTemplateFile);

    /**
     * Creates a new directory. If the underlying file system supports security
     * on files and directories, the function applies a specified security
     * descriptor to the new directory.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa363855.aspx
     */
    boolean CreateDirectory();

    /**
     * Creates an input/output (I/O) completion port and associates it with a
     * specified file handle, or creates an I/O completion port that is not yet
     * associated with a file handle, allowing association at a later time.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa363862(VS.85).aspx
     */
    HANDLE CreateIoCompletionPort(HANDLE FileHandle, HANDLE ExistingCompletionPort,
            Pointer CompletionKey, int NumberOfConcurrentThreads);

    int INFINITE = 0xFFFFFFFF;

    /**
     * Attempts to dequeue an I/O completion packet from the specified I/O
     * completion port. If there is no completion packet queued, the function
     * waits for a pending I/O operation associated with the completion port to
     * complete.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa364986(VS.85).aspx
     */
    boolean GetQueuedCompletionStatus(HANDLE CompletionPort, IntByReference lpNumberOfBytes,
            ByReference lpCompletionKey, PointerByReference lpOverlapped, int dwMilliseconds);

    /**
     * Posts an I/O completion packet to an I/O completion port.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa365458(VS.85).aspx
     */
    boolean PostQueuedCompletionStatus(HANDLE CompletionPort, int dwNumberOfBytesTransferred,
            Pointer dwCompletionKey, OVERLAPPED lpOverlapped);

    /**
     * Waits until the specified object is in the signaled state or the time-out
     * interval elapses.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms687032.aspx
     */
    int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds);

    /**
     * Duplicates an object handle.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms724251.aspx
     */
    boolean DuplicateHandle(HANDLE hSourceProcessHandle, HANDLE hSourceHandle,
            HANDLE hTargetProcessHandle, HANDLEByReference lpTargetHandle, int dwDesiredAccess,
            boolean bInheritHandle, int dwOptions);

    /**
     * Closes an open file handle.
     * 
     * @see http://msdn.microsoft.com/en-us/library/ms724211(VS.85).aspx
     */
    boolean CloseHandle(HANDLE hObject);

    /**
     * Retrieves information that describes the changes within the specified
     * directory. The function does not report changes to the specified
     * directory itself.
     * 
     * @see http://msdn.microsoft.com/en-us/library/aa363858.aspx
     */
    boolean ReadDirectoryChangesW(HANDLE directory, FILE_NOTIFY_INFORMATION info, int length,
            boolean watchSubtree, int notifyFilter, IntByReference bytesReturned,
            OVERLAPPED overlapped, OVERLAPPED_COMPLETION_ROUTINE completionRoutine);

    /**
     * ASCII version. Use {@link Native#toString(byte[])} to obtain the short
     * path from the <code>byte</code> array. Use only if
     * <code>w32.ascii==true</code>.
     */
    int GetShortPathName(String lpszLongPath, byte[] lpdzShortPath, int cchBuffer);

    /**
     * Unicode version (the default). Use {@link Native#toString(char[])} to
     * obtain the short path from the <code>char</code> array.
     */
    int GetShortPathName(String lpszLongPath, char[] lpdzShortPath, int cchBuffer);
}
