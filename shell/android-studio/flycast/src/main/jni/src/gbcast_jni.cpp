#include <jni.h>

#include "cfg/option.h"
#include "emulator.h"
#include "cheats.h"
#include "imgread/common.h"
#include "stdclass.h"
#include "ui/gui.h"

#include <climits>
#include <string>

// GBCast reads and writes Flycast's real config::Option objects. Unknown keys are ignored.

namespace {

struct BoolOpt {
	const char *section;
	const char *name;
	bool (*get)();
	void (*set)(bool);
};

struct IntOpt {
	const char *section;
	const char *name;
	int (*get)();
	void (*set)(int);
};

static bool getWide() { return config::Widescreen; }
static void setWide(bool v) { config::Widescreen = v; }
static bool getSuperWide() { return config::SuperWidescreen; }
static void setSuperWide(bool v) { config::SuperWidescreen = v; }
static bool getFps() { return config::ShowFPS; }
static void setFps(bool v) { config::ShowFPS = v; }
static bool getVsync() { return config::VSync; }
static void setVsync(bool v) { config::VSync = v; }
static bool getInteger() { return config::IntegerScale; }
static void setInteger(bool v) { config::IntegerScale = v; }
static bool getThreaded() { return config::ThreadedRendering; }
static void setThreaded(bool v) { config::ThreadedRendering = v; }
static bool getPacing() { return config::FramePacing; }
static void setPacing(bool v) { config::FramePacing = v; }
static bool getFog() { return config::Fog; }
static void setFog(bool v) { config::Fog = v; }
static bool getMip() { return config::UseMipmaps; }
static void setMip(bool v) { config::UseMipmaps = v; }
static bool getDelay() { return config::DelayFrameSwapping; }
static void setDelay(bool v) { config::DelayFrameSwapping = v; }
static bool getHacks() { return config::WidescreenGameHacks; }
static void setHacks(bool v) { config::WidescreenGameHacks = v; }
static bool getRotate() { return config::Rotate90; }
static void setRotate(bool v) { config::Rotate90 = v; }
static bool getLinear() { return config::LinearInterpolation; }
static void setLinear(bool v) { config::LinearInterpolation = v; }
static bool getStrips() { return config::PerStripSorting; }
static void setStrips(bool v) { config::PerStripSorting = v; }
static bool getFb() { return config::EmulateFramebuffer; }
static void setFb(bool v) { config::EmulateFramebuffer = v; }
static bool getDsp() { return config::DSPEnabled; }
static void setDsp(bool v) { config::DSPEnabled = v; }
static bool getLatency() { return config::AutoLatency; }
static void setLatency(bool v) { config::AutoLatency = v; }
static bool getVmu() { return config::VmuSound; }
static void setVmu(bool v) { config::VmuSound = v; }
static bool getDynarec() { return config::DynarecEnabled; }
static void setDynarec(bool v) { config::DynarecEnabled = v; }
static bool getAutoLoad() { return config::AutoLoadState; }
static void setAutoLoad(bool v) { config::AutoLoadState = v; }
static bool getAutoSave() { return config::AutoSaveState; }
static void setAutoSave(bool v) { config::AutoSaveState = v; }
static bool getFreePlay() { return config::ForceFreePlay; }
static void setFreePlay(bool v) { config::ForceFreePlay = v; }
static bool getFastGd() { return config::FastGDRomLoad; }
static void setFastGd(bool v) { config::FastGDRomLoad = v; }
static bool getRam() { return config::RamMod32MB; }
static void setRam(bool v) { config::RamMod32MB = v; }
static bool getReios() { return config::UseReios; }
static void setReios(bool v) { config::UseReios = v; }
static bool getNet() { return config::NetworkEnable; }
static void setNet(bool v) { config::NetworkEnable = v; }
static bool getBba() { return config::EmulateBBA; }
static void setBba(bool v) { config::EmulateBBA = v; }
static bool getGgpo() { return config::GGPOEnable; }
static void setGgpo(bool v) { config::GGPOEnable = v; }
static bool getBattle() { return config::BattleCableEnable; }
static void setBattle(bool v) { config::BattleCableEnable = v; }
static bool getRa() { return config::EnableAchievements; }
static void setRa(bool v) { config::EnableAchievements = v; }
static bool getHardcore() { return config::AchievementsHardcoreMode; }
static void setHardcore(bool v) { config::AchievementsHardcoreMode = v; }
static bool getCrash() { return config::UploadCrashLogs; }
static void setCrash(bool v) { config::UploadCrashLogs = v; }

static const BoolOpt kBools[] = {
	{"config", "rend.WideScreen", getWide, setWide},
	{"config", "rend.SuperWideScreen", getSuperWide, setSuperWide},
	{"config", "rend.ShowFPS", getFps, setFps},
	{"config", "rend.vsync", getVsync, setVsync},
	{"config", "rend.IntegerScale", getInteger, setInteger},
	{"config", "rend.ThreadedRendering", getThreaded, setThreaded},
	{"config", "rend.FramePacing", getPacing, setPacing},
	{"config", "rend.Fog", getFog, setFog},
	{"config", "rend.UseMipmaps", getMip, setMip},
	{"config", "rend.DelayFrameSwapping", getDelay, setDelay},
	{"config", "rend.WidescreenGameHacks", getHacks, setHacks},
	{"config", "rend.Rotate90", getRotate, setRotate},
	{"config", "rend.LinearInterpolation", getLinear, setLinear},
	{"config", "rend.PerStripSorting", getStrips, setStrips},
	{"config", "rend.EmulateFramebuffer", getFb, setFb},
	{"config", "aica.DSPEnabled", getDsp, setDsp},
	{"config", "aica.AutoLatency", getLatency, setLatency},
	{"audio", "VmuSound", getVmu, setVmu},
	{"config", "Dynarec.Enabled", getDynarec, setDynarec},
	{"config", "Dreamcast.AutoLoadState", getAutoLoad, setAutoLoad},
	{"config", "Dreamcast.AutoSaveState", getAutoSave, setAutoSave},
	{"config", "ForceFreePlay", getFreePlay, setFreePlay},
	{"config", "FastGDRomLoad", getFastGd, setFastGd},
	{"config", "Dreamcast.RamMod32MB", getRam, setRam},
	{"config", "UseReios", getReios, setReios},
	{"network", "Enable", getNet, setNet},
	{"network", "EmulateBBA", getBba, setBba},
	{"network", "GGPO", getGgpo, setGgpo},
	{"network", "BattleCable", getBattle, setBattle},
	{"achievements", "Enabled", getRa, setRa},
	{"achievements", "HardcoreMode", getHardcore, setHardcore},
	{"config", "UploadCrashLogs", getCrash, setCrash},
};

static int getRes() { return config::RenderResolution; }
static void setRes(int v) { config::RenderResolution = v; }
static int getFilter() { return config::TextureFiltering; }
static void setFilter(int v) { config::TextureFiltering = v; }
static int getAniso() { return config::AnisotropicFiltering; }
static void setAniso(int v) { config::AnisotropicFiltering = v; }
static int getRenderer() { return (int)(RenderType)config::RendererType; }
static void setRenderer(int v) { config::RendererType = (RenderType)v; }
static int getCable() { return config::Cable; }
static void setCable(int v) { config::Cable = v; }
static int getRegion() { return config::Region; }
static void setRegion(int v) { config::Region = v; }
static int getBroadcast() { return config::Broadcast; }
static void setBroadcast(int v) { config::Broadcast = v; }
static int getLanguage() { return config::Language; }
static void setLanguage(int v) { config::Language = v; }
static int getAudio() { return config::AudioBufferSize; }
static void setAudio(int v) { config::AudioBufferSize = v; }
static int getPad() { return (int)(MapleDeviceType)config::MapleMainDevices[0]; }
static void setPad(int v) { config::MapleMainDevices[0] = (MapleDeviceType)v; }
static int getVibe() { return config::VirtualGamepadVibration; }
static void setVibe(int v) { config::VirtualGamepadVibration = v; }
static int getSlot() { return config::SavestateSlot; }
static void setSlot(int v) { config::SavestateSlot = v; }
static int getSkip() { return config::SkipFrame; }
static void setSkip(int v) { config::SkipFrame = v; }
static int getAutoSkip() { return config::AutoSkipFrame; }
static void setAutoSkip(int v) { config::AutoSkipFrame = v; }

static const IntOpt kInts[] = {
	{"config", "rend.Resolution", getRes, setRes},
	{"config", "rend.TextureFiltering", getFilter, setFilter},
	{"config", "rend.AnisotropicFiltering", getAniso, setAniso},
	{"config", "pvr.rend", getRenderer, setRenderer},
	{"config", "Dreamcast.Cable", getCable, setCable},
	{"config", "Dreamcast.Region", getRegion, setRegion},
	{"config", "Dreamcast.Broadcast", getBroadcast, setBroadcast},
	{"config", "Dreamcast.Language", getLanguage, setLanguage},
	{"config", "aica.BufferSize", getAudio, setAudio},
	{"input", "device1", getPad, setPad},
	{"input", "VirtualGamepadVibration", getVibe, setVibe},
	{"config", "Dreamcast.SavestateSlot", getSlot, setSlot},
	{"config", "ta.skip", getSkip, setSkip},
	{"config", "pvr.AutoSkipFrame", getAutoSkip, setAutoSkip},
};

static bool same(const char *a, const std::string &b) { return b == a; }

static const BoolOpt *findBool(const std::string &section, const std::string &name) {
	for (const auto &opt : kBools)
		if (same(opt.section, section) && same(opt.name, name))
			return &opt;
	return nullptr;
}

static const IntOpt *findInt(const std::string &section, const std::string &name) {
	for (const auto &opt : kInts)
		if (same(opt.section, section) && same(opt.name, name))
			return &opt;
	return nullptr;
}

static std::string jstr(JNIEnv *env, jstring value) {
	if (value == nullptr)
		return {};
	const char *chars = env->GetStringUTFChars(value, nullptr);
	std::string out = chars != nullptr ? chars : "";
	if (chars != nullptr)
		env->ReleaseStringUTFChars(value, chars);
	return out;
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_configGetBool(JNIEnv *env, jclass, jstring section, jstring name) {
	const BoolOpt *opt = findBool(jstr(env, section), jstr(env, name));
	return opt != nullptr && opt->get();
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_configSetBool(JNIEnv *env, jclass, jstring section, jstring name, jboolean value) {
	const BoolOpt *opt = findBool(jstr(env, section), jstr(env, name));
	if (opt == nullptr)
		return JNI_FALSE;
	opt->set(value == JNI_TRUE);
	SaveSettings();
	return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL Java_com_flycast_emulator_emu_JNIdc_configGetInt(JNIEnv *env, jclass, jstring section, jstring name) {
	const IntOpt *opt = findInt(jstr(env, section), jstr(env, name));
	return opt != nullptr ? opt->get() : INT32_MIN;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_configSetInt(JNIEnv *env, jclass, jstring section, jstring name, jint value) {
	const IntOpt *opt = findInt(jstr(env, section), jstr(env, name));
	if (opt == nullptr)
		return JNI_FALSE;
	opt->set(value);
	SaveSettings();
	return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_configSave(JNIEnv *, jclass) {
	SaveSettings();
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_hasPerGameConfig(JNIEnv *, jclass) {
	return config::Settings::instance().hasPerGameConfig();
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_setPerGameConfig(JNIEnv *, jclass, jboolean enabled) {
	config::Settings::instance().setPerGameConfig(enabled == JNI_TRUE);
	SaveSettings();
}

extern "C" JNIEXPORT jstring JNICALL Java_com_flycast_emulator_emu_JNIdc_dataDirectory(JNIEnv *env, jclass) {
	return env->NewStringUTF(get_writable_data_path("").c_str());
}

extern "C" JNIEXPORT jstring JNICALL Java_com_flycast_emulator_emu_JNIdc_currentGamePath(JNIEnv *env, jclass) {
	return env->NewStringUTF(settings.content.path.c_str());
}

extern "C" JNIEXPORT jint JNICALL Java_com_flycast_emulator_emu_JNIdc_guiState(JNIEnv *, jclass) {
	return (jint)gui_state;
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_guiSetState(JNIEnv *, jclass, jint state) {
	gui_setState((GuiState)state);
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_guiTogglePause(JNIEnv *, jclass) {
	gui_togglePause();
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_restartGame(JNIEnv *, jclass) {
	std::string path = settings.content.path;
	if (gui_state == GuiState::Pause)
		gui_setState(GuiState::Closed);
	gui_stop_game();
	if (!path.empty())
		gui_start_game(path);
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_setFastForwardMode(JNIEnv *, jclass, jboolean enabled) {
    settings.input.fastForwardMode = enabled == JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_saveCurrentSlot(JNIEnv *, jclass) {
	const bool running = gui_state == GuiState::Closed;
	gui_saveState(running, false);
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_loadCurrentSlot(JNIEnv *, jclass) {
	if (!dc_savestateAllowed() || settings.raHardcoreMode)
		return JNI_FALSE;
	if (gui_state == GuiState::Pause) {
		dc_loadstate(config::SavestateSlot);
		return JNI_TRUE;
	}
	if (gui_state != GuiState::Closed)
		return JNI_FALSE;
	gui_loadState(false);
	return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL Java_com_flycast_emulator_emu_JNIdc_insertDisk(JNIEnv *env, jclass, jstring path) {
	try {
		gdr::insertDisk(jstr(env, path));
		return nullptr;
	} catch (const FlycastException &e) {
		return env->NewStringUTF(e.what());
	} catch (const std::exception &e) {
		return env->NewStringUTF(e.what());
	}
}

extern "C" JNIEXPORT jint JNICALL Java_com_flycast_emulator_emu_JNIdc_cheatCount(JNIEnv *, jclass) {
	return (jint)cheatManager.cheatCount();
}

extern "C" JNIEXPORT jstring JNICALL Java_com_flycast_emulator_emu_JNIdc_cheatDescription(JNIEnv *env, jclass, jint index) {
	if (index < 0 || (size_t)index >= cheatManager.cheatCount())
		return nullptr;
	return env->NewStringUTF(cheatManager.cheatDescription((size_t)index).c_str());
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_cheatEnabled(JNIEnv *, jclass, jint index) {
	if (index < 0 || (size_t)index >= cheatManager.cheatCount())
		return JNI_FALSE;
	return cheatManager.cheatEnabled((size_t)index) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_cheatBuiltIn(JNIEnv *, jclass, jint index) {
	if (index < 0 || (size_t)index >= cheatManager.cheatCount())
		return JNI_FALSE;
	return cheatManager.cheatBuiltIn((size_t)index) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_flycast_emulator_emu_JNIdc_setCheatEnabled(JNIEnv *, jclass, jint index, jboolean enabled) {
	if (index < 0 || (size_t)index >= cheatManager.cheatCount() || settings.raHardcoreMode)
		return JNI_FALSE;
	cheatManager.enableCheat((size_t)index, enabled == JNI_TRUE);
	cheatManager.apply();
	return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_com_flycast_emulator_emu_JNIdc_removeCheat(JNIEnv *, jclass, jint index) {
	if (index >= 0 && (size_t)index < cheatManager.cheatCount() && !cheatManager.cheatBuiltIn((size_t)index))
		cheatManager.removeCheat((size_t)index);
}

extern "C" JNIEXPORT jstring JNICALL Java_com_flycast_emulator_emu_JNIdc_addGameSharkCheat(JNIEnv *env, jclass, jstring name, jstring code) {
	if (settings.raHardcoreMode)
		return env->NewStringUTF("Cheats are disabled while RetroAchievements hardcore mode is active.");
	try {
		cheatManager.addGameSharkCheat(jstr(env, name), jstr(env, code));
		return nullptr;
	} catch (const std::exception &e) {
		return env->NewStringUTF(e.what());
	} catch (...) {
		return env->NewStringUTF("Flycast could not parse that GameShark code.");
	}
}
