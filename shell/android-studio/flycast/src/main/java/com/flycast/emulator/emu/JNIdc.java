package com.flycast.emulator.emu;

import android.view.Surface;

import com.flycast.emulator.Emulator;
import com.flycast.emulator.periph.SipEmulator;

public final class JNIdc
{
	static { System.loadLibrary("flycast"); }

	public static native String initEnvironment(Emulator emulator, String filesDirectory, String homeDirectory, String locale);
	public static native void setExternalStorageDirectories(Object[] pathList);
	public static native void setGameUri(String fileName);
	public static native void pause();
	public static native void resume();
	public static native void stop();
	public static native void disableOmpAffinity();

	public static native void rendinitNative(Surface surface, int w, int h);

	public static native void setupMic(SipEmulator sip);

	public static native void screenCharacteristics(float screenDpi, float refreshRate);
	public static native void guiOpenSettings();
	public static native boolean guiIsOpen();
	public static native boolean guiIsContentBrowser();
	public static native void guiSetInsets(int left, int right, int top, int bottom);
	public static native void postRestore(String filesDir);

	// GBCast bridge. Keys are Flycast config section + option name (see core/cfg/option.cpp).
	public static native boolean configGetBool(String section, String name);
	public static native boolean configSetBool(String section, String name, boolean value);
	public static native int configGetInt(String section, String name);
	public static native boolean configSetInt(String section, String name, int value);
	public static native void configSave();
	public static native boolean hasPerGameConfig();
	public static native void setPerGameConfig(boolean enabled);
	public static native String dataDirectory();
	public static native String currentGamePath();
	public static native int guiState();
	public static native void guiSetState(int state);
	public static native void guiTogglePause();
	public static native void restartGame();
	public static native void saveCurrentSlot();
	public static native void setFastForwardMode(boolean enabled);
	public static native boolean loadCurrentSlot();
	public static native String insertDisk(String path);
	public static native int cheatCount();
	public static native String cheatDescription(int index);
	public static native boolean cheatEnabled(int index);
	public static native boolean cheatBuiltIn(int index);
	public static native boolean setCheatEnabled(int index, boolean enabled);
	public static native void removeCheat(int index);
	/** Returns null on success or an error message when Flycast rejects the code. */
	public static native String addGameSharkCheat(String name, String code);
}
