package org.baconana.mybeautifulgit_chan

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.MONITORINFO
import com.sun.jna.platform.win32.WinUser.WindowProc
import com.sun.jna.win32.W32APIOptions
import org.baconana.mybeautifulgit_chan.domain.WindowHitRect
import org.baconana.mybeautifulgit_chan.domain.WindowHitTarget
import org.baconana.mybeautifulgit_chan.domain.windowHitTarget
import java.awt.Window

/** Native Windows chrome; all Swing/Compose content stays owned by the JVM. */
object NativeWindowChrome {
    private val isWindows =
        System.getProperty("os.name").orEmpty().contains("Windows", ignoreCase = true)

    /**
     * Fall back to the OS title bar where Snap/hit-test integration is not
     * available. This can also be forced with
     * `-Dmybeautifulgit.nativeTitleBar=true`.
     */
    fun useNativeTitleBar(): Boolean {
        val forced = System.getProperty("mybeautifulgit.nativeTitleBar")
            ?.trim()
            ?.toBooleanStrictOrNull()
        if (forced != null) return forced
        return !isWindows || !jnaAvailable()
    }

    fun installNow(
        window: Window,
        maximizeBounds: () -> WindowHitRect?,
    ): WindowChromeHandle? {
        if (!isWindows) return null
        val pointer = runCatching { Native.getWindowPointer(window) }.getOrNull() ?: return null
        return runCatching {
            val chrome = WindowsWindowChrome(HWND(pointer), maximizeBounds)
            WindowChromeHandle(
                disposeAction = { chrome.dispose() },
                refreshAction = { chrome.refresh() },
            )
        }.getOrNull()
    }

    private fun jnaAvailable(): Boolean =
        runCatching {
            Native.load("user32", User32::class.java, W32APIOptions.DEFAULT_OPTIONS)
        }.isSuccess
}

class WindowChromeHandle internal constructor(
    private val disposeAction: () -> Unit,
    private val refreshAction: () -> Unit,
) {
    fun dispose() = disposeAction()

    /** Re-scan for a recreated Compose/AWT canvas. */
    fun refresh() = refreshAction()
}

@Suppress("FunctionName")
private interface User32Ex : User32 {
    fun SetWindowLong(hWnd: HWND, nIndex: Int, wndProc: WindowProc): LONG_PTR
    fun SetWindowLongPtr(hWnd: HWND, nIndex: Int, wndProc: WindowProc): LONG_PTR
    fun IsZoomed(hWnd: HWND): Boolean
    fun ScreenToClient(hWnd: HWND, point: POINT): Boolean
    fun ReleaseCapture(): Boolean
    fun CallWindowProc(
        proc: LONG_PTR,
        hWnd: HWND,
        uMsg: Int,
        uParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT
}

private class WindowsWindowChrome(
    private val windowHandle: HWND,
    private val maximizeBounds: () -> WindowHitRect?,
) {
    private val user32Ex = Native.load("user32", User32Ex::class.java, W32APIOptions.DEFAULT_OPTIONS)
    private val slots = mutableListOf<WindowProcSlot>()
    private var disposed = false
    private var restoreBounds: RECT? = null

    init {
        enableNativeResize()
        refresh()
    }

    /**
     * Compose renders its whole client into a SunAwtCanvas child. Windows
     * hit-tests that child first, so the top-level WndProc alone cannot
     * trigger Snap Layouts. Subclass every native canvas and forward all
     * non-chrome messages back to AWT unchanged. AWT may replace the canvas
     * during startup/resize, so the caller refreshes this list periodically.
     */
    fun refresh() {
        if (disposed) return
        enableNativeResize()
        subclass(windowHandle)
        User32.INSTANCE.EnumChildWindows(
            windowHandle,
            object : WinUser.WNDENUMPROC {
                override fun callback(hWnd: HWND, lParam: Pointer?): Boolean {
                    subclass(hWnd)
                    return true
                }
            },
            null,
        )
    }

    private inner class WindowProcSlot(val handle: HWND) : WindowProc {
        var oldProc: LONG_PTR? = null

        override fun callback(hWnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT =
            handleMessage(this, hWnd, uMsg, wParam, lParam)
    }

    private fun subclass(handle: HWND) {
        if (slots.any { it.handle == handle }) return
        val slot = WindowProcSlot(handle)
        slot.oldProc = if (is64Bit()) {
            user32Ex.SetWindowLongPtr(handle, GWLP_WNDPROC, slot)
        } else {
            user32Ex.SetWindowLong(handle, GWLP_WNDPROC, slot)
        }
        slots += slot
    }

    private fun handleMessage(
        slot: WindowProcSlot,
        hWnd: HWND,
        uMsg: Int,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT {
        if (disposed) return callOld(slot, hWnd, uMsg, wParam, lParam)

        if (uMsg == WM_NCCALCSIZE && hWnd == windowHandle) {
            // WS_THICKFRAME adds a native resize border around an otherwise
            // undecorated window. In the light theme that border is a white
            // strip above our custom title bar. Keep the native resize/snap
            // hit-testing, but make the client area cover the whole frame.
            return LRESULT(0)
        }
        if (uMsg == WM_SYSCOMMAND && hWnd == windowHandle) {
            when (wParam.toLong() and 0xFFF0L) {
                SC_MAXIMIZE -> {
                    if (restoreBounds == null) toggleMaximized()
                    return LRESULT(0)
                }
                SC_RESTORE -> if (restoreBounds != null) {
                    toggleMaximized()
                    return LRESULT(0)
                }
            }
        }
        if (uMsg == WM_NCHITTEST) {
            val hit = hitTest(lParam)
            if (hit != null) return LRESULT(hit.toLong())
        } else if (uMsg == WM_NCLBUTTONDOWN) {
            val code = wParam.toLong()
            if (code == HTMAXBUTTON.toLong()) {
                toggleMaximized()
                return LRESULT(0)
            }
            if (slot.handle != windowHandle && code in HTLEFT.toLong()..HTBOTTOMRIGHT.toLong()) {
                // A child canvas cannot start the top-level sizing loop itself.
                // Hand the non-client drag to the frame, which owns the border.
                user32Ex.ReleaseCapture()
                User32.INSTANCE.SendMessage(windowHandle, WM_NCLBUTTONDOWN, wParam, lParam)
                return LRESULT(0)
            }
        } else if (uMsg == WM_NCDESTROY) {
            restoreSlot(slot)
        }
        return callOld(slot, hWnd, uMsg, wParam, lParam)
    }

    /**
     * Maximize into the monitor work area instead of using ShowWindow.
     * The window is WS_POPUP (undecorated), so a native SW_MAXIMIZE covers
     * the taskbar. Setting work-area bounds keeps the taskbar visible and
     * matches the previous windowed-fullscreen behavior.
     */
    private fun toggleMaximized() {
        val current = RECT()
        if (!User32.INSTANCE.GetWindowRect(windowHandle, current)) return
        val saved = restoreBounds
        if (saved == null) {
            val monitor = User32.INSTANCE.MonitorFromWindow(windowHandle, MONITOR_DEFAULTTONEAREST)
            val info = MONITORINFO()
            info.cbSize = info.size()
            val work = if (User32.INSTANCE.GetMonitorInfo(monitor, info).booleanValue()) {
                info.rcWork
            } else {
                current
            }
            restoreBounds = current
            User32.INSTANCE.SetWindowPos(
                windowHandle,
                null,
                work.left,
                work.top,
                work.right - work.left,
                work.bottom - work.top,
                SWP_NOZORDER or SWP_NOACTIVATE,
            )
        } else {
            User32.INSTANCE.SetWindowPos(
                windowHandle,
                null,
                saved.left,
                saved.top,
                saved.right - saved.left,
                saved.bottom - saved.top,
                SWP_NOZORDER or SWP_NOACTIVATE,
            )
            restoreBounds = null
        }
    }

    private fun hitTest(lParam: LPARAM): Int? {
        val value = lParam.toLong()
        val screenX = (value and 0xffff).toShort().toInt()
        val screenY = ((value shr 16) and 0xffff).toShort().toInt()
        val point = POINT(screenX, screenY)
        if (!user32Ex.ScreenToClient(windowHandle, point)) return null

        val client = RECT()
        if (!User32.INSTANCE.GetClientRect(windowHandle, client)) return null
        val width = client.right - client.left
        val height = client.bottom - client.top
        if (width <= 0 || height <= 0) return null

        val maximized = user32Ex.IsZoomed(windowHandle)
        val target = windowHitTarget(
            clientWidth = width,
            clientHeight = height,
            x = point.x - client.left,
            y = point.y - client.top,
            maximized = maximized,
            border = resizeBorder(),
            maximizeBounds = maximizeBounds(),
        )
        return when (target) {
            WindowHitTarget.Client -> null
            WindowHitTarget.Left -> HTLEFT
            WindowHitTarget.Right -> HTRIGHT
            WindowHitTarget.Top -> HTTOP
            WindowHitTarget.Bottom -> HTBOTTOM
            WindowHitTarget.TopLeft -> HTTOPLEFT
            WindowHitTarget.TopRight -> HTTOPRIGHT
            WindowHitTarget.BottomLeft -> HTBOTTOMLEFT
            WindowHitTarget.BottomRight -> HTBOTTOMRIGHT
            WindowHitTarget.MaximizeButton -> HTMAXBUTTON
        }
    }

    /** WS_THICKFRAME gives Windows ownership of smooth edge/corner resizing. */
    private fun enableNativeResize() {
        val style = User32.INSTANCE.GetWindowLong(windowHandle, GWL_STYLE)
        val wanted = style or WS_THICKFRAME or WS_MAXIMIZEBOX or WS_MINIMIZEBOX or WS_SYSMENU
        if (wanted != style) {
            User32.INSTANCE.SetWindowLong(windowHandle, GWL_STYLE, wanted)
            User32.INSTANCE.SetWindowPos(
                windowHandle,
                null,
                0,
                0,
                0,
                0,
                SWP_NOMOVE or SWP_NOSIZE or SWP_NOZORDER or SWP_FRAMECHANGED,
            )
        }
    }

    private fun resizeBorder(): Int {
        val frame = User32.INSTANCE.GetSystemMetrics(SM_CXSIZEFRAME)
        val padding = User32.INSTANCE.GetSystemMetrics(SM_CXPADDEDBORDER)
        return (frame + padding).coerceAtLeast(6)
    }

    private fun callOld(
        slot: WindowProcSlot,
        hWnd: HWND,
        uMsg: Int,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT {
        val previous = slot.oldProc ?: return LRESULT(0)
        return user32Ex.CallWindowProc(previous, hWnd, uMsg, wParam, lParam)
    }

    private fun restoreSlot(slot: WindowProcSlot) {
        if (!slots.remove(slot)) return
        val previous = slot.oldProc ?: return
        if (is64Bit()) {
            User32.INSTANCE.SetWindowLongPtr(
                slot.handle,
                GWLP_WNDPROC,
                Pointer(previous.toLong()),
            )
        } else {
            user32Ex.SetWindowLong(slot.handle, GWLP_WNDPROC, previous.toInt())
        }
    }

    private fun restore() {
        if (disposed) return
        disposed = true
        slots.toList().forEach(::restoreSlot)
    }

    fun dispose() {
        restore()
    }

    private fun is64Bit(): Boolean =
        System.getProperty("sun.arch.data.model") == "64" ||
            System.getProperty("com.ibm.vm.bitmode") == "64"

    private companion object {
        const val GWLP_WNDPROC = -4
        const val GWL_STYLE = -16
        const val WM_NCCALCSIZE = 0x0083
        const val WM_NCHITTEST = 0x0084
        const val WM_NCLBUTTONDOWN = 0x00A1
        const val WM_NCDESTROY = 0x0082
        const val WM_SYSCOMMAND = 0x0112

        const val HTMAXBUTTON = 9
        const val HTLEFT = 10
        const val HTRIGHT = 11
        const val HTTOP = 12
        const val HTTOPLEFT = 13
        const val HTTOPRIGHT = 14
        const val HTBOTTOM = 15
        const val HTBOTTOMLEFT = 16
        const val HTBOTTOMRIGHT = 17

        const val WS_THICKFRAME = 0x00040000
        const val WS_MAXIMIZEBOX = 0x00010000
        const val WS_MINIMIZEBOX = 0x00020000
        const val WS_SYSMENU = 0x00080000
        const val SC_MAXIMIZE = 0xF030L
        const val SC_RESTORE = 0xF120L
        const val MONITOR_DEFAULTTONEAREST = 2

        const val SWP_NOSIZE = 0x0001
        const val SWP_NOMOVE = 0x0002
        const val SWP_NOZORDER = 0x0004
        const val SWP_NOACTIVATE = 0x0010
        const val SWP_FRAMECHANGED = 0x0020

        const val SM_CXSIZEFRAME = 32
        const val SM_CXPADDEDBORDER = 92
    }
}
