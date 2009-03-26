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

    Pointer GlobalFree(Pointer hGlobal);

    HMODULE GetModuleHandle(String name);

    void GetSystemTime(SYSTEMTIME result);

    int GetCurrentThreadId();

    HANDLE GetCurrentThread();

    int GetCurrentProcessId();

    HANDLE GetCurrentProcess();

    int GetProcessId(HANDLE process);

    int GetProcessVersion(int processId);

    int GetLastError();

    void SetLastError(int dwErrCode);

    int GetDriveType(String rootPathName);

    int FORMAT_MESSAGE_ALLOCATE_BUFFER = 0x0100;

    int FORMAT_MESSAGE_FROM_SYSTEM = 0x1000;

    int FORMAT_MESSAGE_IGNORE_INSERTS = 0x200;

    int FormatMessage(int dwFlags, Pointer lpSource, int dwMessageId, int dwLanguageId,
            PointerByReference lpBuffer, int nSize, Pointer va_list);

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

    HANDLE CreateFile(String lpFileName, int dwDesiredAccess, int dwShareMode,
            SECURITY_ATTRIBUTES lpSecurityAttributes, int dwCreationDisposition,
            int dwFlagsAndAttributes, HANDLE hTemplateFile);

    boolean CreateDirectory();

    HANDLE CreateIoCompletionPort(HANDLE FileHandle, HANDLE ExistingCompletionPort,
            Pointer CompletionKey, int NumberOfConcurrentThreads);

    int INFINITE = 0xFFFFFFFF;

    boolean GetQueuedCompletionStatus(HANDLE CompletionPort, IntByReference lpNumberOfBytes,
            ByReference lpCompletionKey, PointerByReference lpOverlapped, int dwMilliseconds);

    boolean PostQueuedCompletionStatus(HANDLE CompletionPort, int dwNumberOfBytesTransferred,
            Pointer dwCompletionKey, OVERLAPPED lpOverlapped);

    int WaitForSingleObject(HANDLE hHandle, int dwMilliseconds);

    boolean DuplicateHandle(HANDLE hSourceProcessHandle, HANDLE hSourceHandle,
            HANDLE hTargetProcessHandle, HANDLEByReference lpTargetHandle, int dwDesiredAccess,
            boolean bInheritHandle, int dwOptions);

    boolean CloseHandle(HANDLE hObject);

    /**
     * NOTE: only exists in unicode form (W suffix). Define this method
     * explicitly with the W suffix to avoid inadvertent calls in ASCII mode.
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
