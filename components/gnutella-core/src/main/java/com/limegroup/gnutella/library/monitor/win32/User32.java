package com.limegroup.gnutella.library.monitor.win32;

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

import com.limegroup.gnutella.library.monitor.win32.GDI32.RECT;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * Provides access to the w32 user32 library. Incomplete implementation to
 * support demos.
 * 
 * @author Todd Fast, todd.fast@sun.com
 * @author twall@users.sf.net
 */
public interface User32 extends W32API {

    public User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class, DEFAULT_OPTIONS);

    public HDC GetDC(HWND hWnd);

    public int ReleaseDC(HWND hWnd, HDC hDC);

    public int FLASHW_STOP = 0;

    public int FLASHW_CAPTION = 1;

    public int FLASHW_TRAY = 2;

    public int FLASHW_ALL = (FLASHW_CAPTION | FLASHW_TRAY);

    public int FLASHW_TIMER = 4;

    public int FLASHW_TIMERNOFG = 12;

    public class FLASHWINFO extends Structure {
        public int cbSize;

        public HANDLE hWnd;

        public int dwFlags;

        public int uCount;

        public int dwTimeout;
    }

    public int IMAGE_BITMAP = 0;

    public int IMAGE_ICON = 1;

    public int IMAGE_CURSOR = 2;

    public int IMAGE_ENHMETAFILE = 3;

    public int LR_DEFAULTCOLOR = 0x0000;

    public int LR_MONOCHROME = 0x0001;

    public int LR_COLOR = 0x0002;

    public int LR_COPYRETURNORG = 0x0004;

    public int LR_COPYDELETEORG = 0x0008;

    public int LR_LOADFROMFILE = 0x0010;

    public int LR_LOADTRANSPARENT = 0x0020;

    public int LR_DEFAULTSIZE = 0x0040;

    public int LR_VGACOLOR = 0x0080;

    public int LR_LOADMAP3DCOLORS = 0x1000;

    public int LR_CREATEDIBSECTION = 0x2000;

    public int LR_COPYFROMRESOURCE = 0x4000;

    public int LR_SHARED = 0x8000;

    public HWND FindWindow(String winClass, String title);

    public int GetClassName(HWND hWnd, byte[] lpClassName, int nMaxCount);

    public class GUITHREADINFO extends Structure {
        public int cbSize = size();

        public int flags;

        public HWND hwndActive;

        public HWND hwndFocus;

        public HWND hwndCapture;

        public HWND hwndMenuOwner;

        public HWND hwndMoveSize;

        public HWND hwndCaret;

        public RECT rcCaret;
    }

    public boolean GetGUIThreadInfo(int idThread, GUITHREADINFO lpgui);

    public class WINDOWINFO extends Structure {
        public int cbSize = size();

        public RECT rcWindow;

        public RECT rcClient;

        public int dwStyle;

        public int dwExStyle;

        public int dwWindowStatus;

        public int cxWindowBorders;

        public int cyWindowBorders;

        public short atomWindowType;

        public short wCreatorVersion;
    }

    public boolean GetWindowInfo(HWND hWnd, WINDOWINFO pwi);

    public boolean GetWindowRect(HWND hWnd, RECT rect);

    public int GetWindowText(HWND hWnd, byte[] lpString, int nMaxCount);

    public int GetWindowTextLength(HWND hWnd);

    public int GetWindowModuleFileName(HWND hWnd, byte[] lpszFileName, int cchFileNameMax);

    public int GetWindowThreadProcessId(HWND hWnd, IntByReference lpdwProcessId);

    public interface WNDENUMPROC extends StdCallCallback {
        /** Return whether to continue enumeration. */
        boolean callback(HWND hWnd, Pointer data);
    }

    public boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer data);

    public boolean EnumThreadWindows(int dwThreadId, WNDENUMPROC lpEnumFunc, Pointer data);

    public boolean FlashWindowEx(FLASHWINFO info);

    public HICON LoadIcon(HINSTANCE hInstance, String iconName);

    public HANDLE LoadImage(HINSTANCE hinst, // handle to instance
            String name, // image to load
            int type, // image type
            int xDesired, // desired width
            int yDesired, // desired height
            int load // load options
    );

    public boolean DestroyIcon(HICON hicon);

    public int GWL_EXSTYLE = -20;

    public int GWL_STYLE = -16;

    public int GWL_WNDPROC = -4;

    public int GWL_HINSTANCE = -6;

    public int GWL_ID = -12;

    public int GWL_USERDATA = -21;

    public int DWL_DLGPROC = 4;

    public int DWL_MSGRESULT = 0;

    public int DWL_USER = 8;

    public int WS_EX_COMPOSITED = 0x20000000;

    public int WS_EX_LAYERED = 0x80000;

    public int WS_EX_TRANSPARENT = 32;

    public int GetWindowLong(HWND hWnd, int nIndex);

    public int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong);

    // Do not use this version on win64
    public Pointer SetWindowLong(HWND hWnd, int nIndex, Pointer dwNewLong);

    public LONG_PTR GetWindowLongPtr(HWND hWnd, int nIndex);

    public LONG_PTR SetWindowLongPtr(HWND hWnd, int nIndex, LONG_PTR dwNewLongPtr);

    public Pointer SetWindowLongPtr(HWND hWnd, int nIndex, Pointer dwNewLongPtr);

    public int LWA_COLORKEY = 1;

    public int LWA_ALPHA = 2;

    public int ULW_COLORKEY = 1;

    public int ULW_ALPHA = 2;

    public int ULW_OPAQUE = 4;

    public boolean SetLayeredWindowAttributes(HWND hwnd, int crKey, byte bAlpha, int dwFlags);

    public boolean GetLayeredWindowAttributes(HWND hwnd, IntByReference pcrKey,
            ByteByReference pbAlpha, IntByReference pdwFlags);

    /** Defines the x- and y-coordinates of a point. */
    public class POINT extends Structure {
        public int x, y;

        public POINT() {
        }

        public POINT(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Specifies the width and height of a rectangle. */
    public class SIZE extends Structure {
        public int cx, cy;

        public SIZE() {
        }

        public SIZE(int w, int h) {
            this.cx = w;
            this.cy = h;
        }
    }

    public int AC_SRC_OVER = 0x00;

    public int AC_SRC_ALPHA = 0x01;

    public int AC_SRC_NO_PREMULT_ALPHA = 0x01;

    public int AC_SRC_NO_ALPHA = 0x02;

    public class BLENDFUNCTION extends Structure {
        public byte BlendOp = AC_SRC_OVER; // only valid value

        public byte BlendFlags = 0; // only valid value

        public byte SourceConstantAlpha;

        public byte AlphaFormat;
    }

    public boolean UpdateLayeredWindow(HWND hwnd, HDC hdcDst, POINT pptDst, SIZE psize, HDC hdcSrc,
            POINT pptSrc, int crKey, BLENDFUNCTION pblend, int dwFlags);

    public int SetWindowRgn(HWND hWnd, HRGN hRgn, boolean bRedraw);

    public int VK_SHIFT = 16;

    public int VK_LSHIFT = 0xA0;

    public int VK_RSHIFT = 0xA1;

    public int VK_CONTROL = 17;

    public int VK_LCONTROL = 0xA2;

    public int VK_RCONTROL = 0xA3;

    public int VK_MENU = 18;

    public int VK_LMENU = 0xA4;

    public int VK_RMENU = 0xA5;

    public boolean GetKeyboardState(byte[] state);

    public short GetAsyncKeyState(int vKey);

    public int WH_KEYBOARD = 2;

    public int WH_MOUSE = 7;

    public int WH_KEYBOARD_LL = 13;

    public int WH_MOUSE_LL = 14;

    public class HHOOK extends HANDLE {
    }

    public interface HOOKPROC extends StdCallCallback {
    }

    public int WM_KEYDOWN = 256;

    public int WM_KEYUP = 257;

    public int WM_SYSKEYDOWN = 260;

    public int WM_SYSKEYUP = 261;

    public class KBDLLHOOKSTRUCT extends Structure {
        public int vkCode;

        public int scanCode;

        public int flags;

        public int time;

        public ULONG_PTR dwExtraInfo;
    }

    public interface LowLevelKeyboardProc extends HOOKPROC {
        LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT lParam);
    }

    public HHOOK SetWindowsHookEx(int idHook, HOOKPROC lpfn, HINSTANCE hMod, int dwThreadId);

    public LRESULT CallNextHookEx(HHOOK hhk, int nCode, WPARAM wParam, LPARAM lParam);

    public LRESULT CallNextHookEx(HHOOK hhk, int nCode, WPARAM wParam, Pointer lParam);

    public boolean UnhookWindowsHookEx(HHOOK hhk);

    public class MSG extends Structure {
        public HWND hWnd;

        public int message;

        public WPARAM wParam;

        public LPARAM lParam;

        public int time;

        public POINT pt;
    }

    public int GetMessage(MSG lpMsg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax);

    public boolean PeekMessage(MSG lpMsg, HWND hWnd, int wMsgFilterMin, int wMsgFilterMax,
            int wRemoveMsg);

    public boolean TranslateMessage(MSG lpMsg);

    public LRESULT DispatchMessage(MSG lpMsg);

    public void PostMessage(HWND hWnd, int msg, WPARAM wParam, LPARAM lParam);

    public void PostQuitMessage(int nExitCode);
}
