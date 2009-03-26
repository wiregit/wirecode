package com.limegroup.gnutella.library.monitor.win32.api;

import java.nio.Buffer;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class Kernel32 implements Kernel32Interface {
    private final Kernel32Interface nativeDelegate;

    public Kernel32() {
        nativeDelegate = (Kernel32Interface) Native.loadLibrary("kernel32",
                Kernel32Interface.class, DEFAULT_OPTIONS);
    }

    @Override
    public boolean CloseHandle(HANDLE object) {
        return nativeDelegate.CloseHandle(object);
    }

    @Override
    public boolean CreateDirectory() {
        return nativeDelegate.CreateDirectory();
    }

    @Override
    public HANDLE CreateFile(String lpFileName, int dwDesiredAccess, int dwShareMode,
            SECURITY_ATTRIBUTES lpSecurityAttributes, int dwCreationDisposition,
            int dwFlagsAndAttributes, HANDLE templateFile) {
        return nativeDelegate.CreateFile(lpFileName, dwDesiredAccess, dwShareMode,
                lpSecurityAttributes, dwCreationDisposition, dwFlagsAndAttributes, templateFile);
    }

    @Override
    public HANDLE CreateIoCompletionPort(HANDLE FileHandle, HANDLE ExistingCompletionPort,
            Pointer CompletionKey, int NumberOfConcurrentThreads) {
        return nativeDelegate.CreateIoCompletionPort(FileHandle, ExistingCompletionPort,
                CompletionKey, NumberOfConcurrentThreads);
    }

    @Override
    public boolean DuplicateHandle(HANDLE sourceProcessHandle, HANDLE sourceHandle,
            HANDLE targetProcessHandle, HANDLEByReference lpTargetHandle, int dwDesiredAccess,
            boolean inheritHandle, int dwOptions) {
        return nativeDelegate.DuplicateHandle(sourceProcessHandle, sourceHandle,
                targetProcessHandle, lpTargetHandle, dwDesiredAccess, inheritHandle, dwOptions);
    }

    @Override
    public int FormatMessage(int dwFlags, Pointer lpSource, int dwMessageId, int dwLanguageId,
            Buffer lpBuffer, int size, Pointer va_list) {
        return nativeDelegate.FormatMessage(dwFlags, lpSource, dwMessageId, dwLanguageId, lpBuffer,
                size, va_list);
    }

    @Override
    public int FormatMessage(int dwFlags, Pointer lpSource, int dwMessageId, int dwLanguageId,
            PointerByReference lpBuffer, int size, Pointer va_list) {
        return nativeDelegate.FormatMessage(dwFlags, lpSource, dwMessageId, dwLanguageId, lpBuffer,
                size, va_list);
    }

    @Override
    public HANDLE GetCurrentProcess() {
        return nativeDelegate.GetCurrentProcess();
    }

    @Override
    public int GetCurrentProcessId() {
        return nativeDelegate.GetCurrentProcessId();
    }

    @Override
    public HANDLE GetCurrentThread() {
        return nativeDelegate.GetCurrentThread();
    }

    @Override
    public int GetCurrentThreadId() {
        return nativeDelegate.GetCurrentThreadId();
    }

    @Override
    public int GetDriveType(String rootPathName) {
        return nativeDelegate.GetDriveType(rootPathName);
    }

    @Override
    public int GetLastError() {
        return nativeDelegate.GetLastError();
    }

    @Override
    public HMODULE GetModuleHandle(String name) {
        return nativeDelegate.GetModuleHandle(name);
    }

    @Override
    public int GetProcessId(HANDLE process) {
        return nativeDelegate.GetProcessId(process);
    }

    @Override
    public int GetProcessVersion(int processId) {
        return nativeDelegate.GetProcessVersion(processId);
    }

    @Override
    public boolean GetQueuedCompletionStatus(HANDLE CompletionPort, IntByReference lpNumberOfBytes,
            ByReference lpCompletionKey, PointerByReference lpOverlapped, int dwMilliseconds) {
        return nativeDelegate.GetQueuedCompletionStatus(CompletionPort, lpNumberOfBytes,
                lpCompletionKey, lpOverlapped, dwMilliseconds);
    }

    @Override
    public int GetShortPathName(String lpszLongPath, byte[] lpdzShortPath, int cchBuffer) {
        return nativeDelegate.GetShortPathName(lpszLongPath, lpdzShortPath, cchBuffer);
    }

    @Override
    public int GetShortPathName(String lpszLongPath, char[] lpdzShortPath, int cchBuffer) {
        return nativeDelegate.GetShortPathName(lpszLongPath, lpdzShortPath, cchBuffer);
    }

    @Override
    public void GetSystemTime(SYSTEMTIME result) {
        nativeDelegate.GetSystemTime(result);
    }

    @Override
    public Pointer GlobalFree(Pointer global) {
        return nativeDelegate.GlobalFree(global);
    }

    @Override
    public Pointer LocalFree(Pointer local) {
        return nativeDelegate.LocalFree(local);
    }

    @Override
    public boolean PostQueuedCompletionStatus(HANDLE CompletionPort,
            int dwNumberOfBytesTransferred, Pointer dwCompletionKey, OVERLAPPED lpOverlapped) {
        return nativeDelegate.PostQueuedCompletionStatus(CompletionPort,
                dwNumberOfBytesTransferred, dwCompletionKey, lpOverlapped);
    }

    @Override
    public boolean ReadDirectoryChangesW(HANDLE directory, FILE_NOTIFY_INFORMATION info,
            int length, boolean watchSubtree, int notifyFilter, IntByReference bytesReturned,
            OVERLAPPED overlapped, OVERLAPPED_COMPLETION_ROUTINE completionRoutine) {
        return nativeDelegate.ReadDirectoryChangesW(directory, info, length, watchSubtree,
                notifyFilter, bytesReturned, overlapped, completionRoutine);
    }

    @Override
    public void SetLastError(int dwErrCode) {
        nativeDelegate.SetLastError(dwErrCode);
    }

    @Override
    public int WaitForSingleObject(HANDLE handle, int dwMilliseconds) {
        return nativeDelegate.WaitForSingleObject(handle, dwMilliseconds);
    }

    public String getSystemError(int code) {
        PointerByReference pref = new PointerByReference();
        nativeDelegate.FormatMessage(Kernel32Interface.FORMAT_MESSAGE_ALLOCATE_BUFFER
                | Kernel32Interface.FORMAT_MESSAGE_FROM_SYSTEM
                | Kernel32Interface.FORMAT_MESSAGE_IGNORE_INSERTS, null, code, 0, pref, 0, null);
        String s = pref.getValue().getString(0, !Boolean.getBoolean("w32.ascii"));
        s = s.replace(".\r", ".").replace(".\n", ".");
        nativeDelegate.LocalFree(pref.getValue());
        return s;
    }

}
