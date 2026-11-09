// Copyright 2023-2026 Citra Emulator Project / Azahar Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the misc/licenses/gplv2.txt file included.

package org.citra.citra_emu.features.settings.model

import android.text.TextUtils
import java.util.TreeMap
import org.citra.citra_emu.CitraApplication
import org.citra.citra_emu.R
import org.citra.citra_emu.features.settings.ui.SettingsActivityView
import org.citra.citra_emu.features.settings.utils.SettingsFile

class Settings {
    private var gameId: String? = null

    var isLoaded = false

    private var loadedGeneration = 0

    /**
     * A HashMap<String></String>, SettingSection> that constructs a new SettingSection instead of returning null
     * when getting a key not already in the map
     */
    class SettingsSectionMap : HashMap<String, SettingSection?>() {
        override operator fun get(key: String): SettingSection? {
            if (!super.containsKey(key)) {
                val section = SettingSection(key)
                super.put(key, section)
                return section
            }
            return super.get(key)
        }
    }

    var sections: HashMap<String, SettingSection?> = SettingsSectionMap()

    fun getSection(sectionName: String): SettingSection? = sections[sectionName]

    val isEmpty: Boolean
        get() = sections.isEmpty()

    fun loadSettings(view: SettingsActivityView? = null) {
        val generation = ++loadGeneration
        sections = SettingsSectionMap()
        // Clear values left by a previous game's overrides before loading the global baseline.
        BooleanSetting.clear()
        FloatSetting.clear()
        ScaledFloatSetting.clear()
        IntSetting.clear()
        IntListSetting.clear()
        StringSetting.clear()
        loadCitraSettings(view)
        if (!TextUtils.isEmpty(gameId)) {
            val legacyGlobalValue = BooleanSetting.USE_PER_GAME_SETTINGS.boolean
            val gameSections = SettingsFile.readCustomGameSettings(gameId!!, view)
            val customDebugSection = gameSections.entries
                .firstOrNull { it.key == SECTION_DEBUG }
                ?.value
            val gameToggle = customDebugSection?.getSetting(BooleanSetting.USE_PER_GAME_SETTINGS.key)
                as? AbstractBooleanSetting
            val useGameSettings = gameToggle?.boolean
                ?: (legacyGlobalValue && gameSections.isNotEmpty())

            BooleanSetting.USE_PER_GAME_SETTINGS.boolean = useGameSettings
            if (gameToggle == null && useGameSettings) {
                // Migrate existing per-game files created while the old global switch was on.
                SettingsFile.saveCustomGameSetting(gameId!!, BooleanSetting.USE_PER_GAME_SETTINGS)
            }
            if (useGameSettings) {
                mergeSections(gameSections)
            } else {
                sections[SECTION_DEBUG]!!.putSetting(BooleanSetting.USE_PER_GAME_SETTINGS)
            }
        }
        isLoaded = true
        loadedGeneration = generation
    }

    /**
     * Reloads settings if another [Settings] instance loaded since this one, since all
     * settings share global backing objects that would otherwise be overwritten.
     */
    fun reloadIfStale(view: SettingsActivityView? = null): Boolean {
        if (isLoaded && loadedGeneration != loadGeneration) {
            loadSettings(view)
            return true
        }
        return false
    }

    private fun loadCitraSettings(view: SettingsActivityView?) {
        for ((fileName) in configFileSectionsMap) {
            sections.putAll(SettingsFile.readFile(fileName, view))
        }
    }

    private fun mergeSections(updatedSections: HashMap<String, SettingSection?>) {
        for ((key, updatedSection) in updatedSections) {
            val filteredSection = SettingSection(updatedSection?.name ?: key)
            if (updatedSection != null) {
                for (setting in updatedSection.settings.values) {
                    // Global-only device settings are ignored in per-game files, so do not
                    // let stale copies shadow the global values in the UI either.
                    if (setting.key in GLOBAL_ONLY_DEBUG_KEYS) {
                        continue
                    }
                    filteredSection.putSetting(setting)
                }
            }
            if (filteredSection.settings.isEmpty()) {
                continue
            }
            if (sections.containsKey(key)) {
                val originalSection = sections[key]
                originalSection!!.mergeSection(filteredSection)
            } else {
                sections[key] = filteredSection
            }
        }
    }

    fun loadSettings(gameId: String, view: SettingsActivityView) {
        this.gameId = gameId
        loadSettings(view)
    }

    fun saveSettings(view: SettingsActivityView) {
        if (TextUtils.isEmpty(gameId)) {
            saveGlobalSettings(view)
        } else {
            if (BooleanSetting.USE_PER_GAME_SETTINGS.boolean) {
                val gameSections = TreeMap<String, SettingSection?>()
                for (sectionName in configFileSectionsMap[SettingsFile.FILE_NAME_CONFIG].orEmpty()) {
                    val original = sections[sectionName] ?: continue
                    val gameSection = SettingSection(sectionName)
                    for (setting in original.settings.values) {
                        // Performance hacks are device-level settings. Keeping them out of
                        // the per-game snapshot lets global changes apply and stops old game
                        // INIs from pinning stale values.
                        if (sectionName == SECTION_DEBUG &&
                            setting.key in GLOBAL_ONLY_DEBUG_KEYS
                        ) {
                            continue
                        }
                        gameSection.putSetting(setting)
                    }
                    if (gameSection.settings.isNotEmpty()) {
                        gameSections[sectionName] = gameSection
                    }
                }
                SettingsFile.saveCustomGameSettings(gameId!!, gameSections, view)
                saveGlobalOnlySettings(view)
            } else {
                saveGlobalSettings(view)
            }
        }
    }

    /**
     * Writes device-level performance hack settings to the global config. These must
     * not be overridden by a per-game INI.
     */
    private fun saveGlobalOnlySettings(view: SettingsActivityView) {
        val original = sections[SECTION_DEBUG] ?: return
        val globalSection = SettingSection(SECTION_DEBUG)
        for (setting in original.settings.values) {
            if (setting.key in GLOBAL_ONLY_DEBUG_KEYS) {
                globalSection.putSetting(setting)
            }
        }
        if (globalSection.settings.isEmpty()) {
            return
        }
        val iniSections = TreeMap<String, SettingSection?>()
        iniSections[SECTION_DEBUG] = globalSection
        SettingsFile.saveFile(SettingsFile.FILE_NAME_CONFIG, iniSections, view)
    }

    private fun saveGlobalSettings(view: SettingsActivityView) {
        view.showToastMessage(CitraApplication.appContext.getString(R.string.ini_saved), false)
        for ((fileName, sectionNames) in configFileSectionsMap.entries) {
            val iniSections = TreeMap<String, SettingSection?>()
            for (section in sectionNames) {
                val original = sections[section]
                if (section == SECTION_DEBUG && original != null) {
                    val globalSection = SettingSection(section)
                    for (setting in original.settings.values) {
                        if (setting.key != BooleanSetting.USE_PER_GAME_SETTINGS.key) {
                            globalSection.putSetting(setting)
                        }
                    }
                    iniSections[section] = globalSection
                } else {
                    iniSections[section] = original
                }
            }
            SettingsFile.saveFile(fileName, iniSections, view)
        }
    }

    fun saveSetting(setting: AbstractSetting, filename: String) {
        if (!TextUtils.isEmpty(gameId) && BooleanSetting.USE_PER_GAME_SETTINGS.boolean &&
            setting.key !in GLOBAL_ONLY_DEBUG_KEYS
        ) {
            SettingsFile.saveCustomGameSetting(gameId!!, setting)
        } else {
            SettingsFile.saveFile(filename, setting)
        }
    }

    companion object {
        private var loadGeneration = 0

        /**
         * Performance hacks that must always come from the global config. These are
         * device-level toggles; a per-game INI snapshot must not override them.
         */
        val GLOBAL_ONLY_DEBUG_KEYS: Set<String?> = setOf(
            BooleanSetting.REALTIME_GPU.key,
            BooleanSetting.ENABLE_PERFORMANCE_HACKS.key,
            BooleanSetting.UNSAFE_CPU_OPTIMIZATIONS.key,
            BooleanSetting.SKIP_SLOW_DRAW_FALLBACK.key,
            BooleanSetting.FAST_TEXTURE_INVALIDATION.key,
            BooleanSetting.FAST_SHADER_PRECISION.key,
            BooleanSetting.UNSAFE_SKIP_TEXTURE_FLUSH.key,
            BooleanSetting.UNSAFE_SKIP_GPU_SYNC.key,
            BooleanSetting.UNSAFE_SKIP_MIPMAPS.key,
            BooleanSetting.UNSAFE_FAST_SHADER_MATH.key,
            BooleanSetting.UNSAFE_SKIP_PAGE_MARKING.key,
            BooleanSetting.UNSAFE_SKIP_SHADOW_RENDERING.key,
            BooleanSetting.UNSAFE_DISABLE_STENCIL_TEST.key,
            BooleanSetting.UNSAFE_DISABLE_ALPHA_BLEND.key,
            BooleanSetting.UNSAFE_SKIP_TEXTURE_COPIES.key,
            BooleanSetting.GL_UNSAFE_STREAM_BUFFER.key,
            BooleanSetting.UNSAFE_SKIP_UNRESOLVED_TEXTURE_COPIES.key,
            BooleanSetting.UNSAFE_TRUST_CACHED_TEXTURES.key,
        )

        const val SECTION_CORE = "Core"
        const val SECTION_SYSTEM = "System"
        const val SECTION_CAMERA = "Camera"
        const val SECTION_CONTROLS = "Controls"
        const val SECTION_RENDERER = "Renderer"
        const val SECTION_LAYOUT = "Layout"
        const val SECTION_UTILITY = "Utility"
        const val SECTION_NETWORK = "WebService"
        const val SECTION_AUDIO = "Audio"
        const val SECTION_DEBUG = "Debugging"
        const val SECTION_HACKS = "Hacks"
        const val SECTION_THEME = "Theme"
        const val SECTION_CUSTOM_LANDSCAPE = "Custom Landscape Layout"
        const val SECTION_CUSTOM_PORTRAIT = "Custom Portrait Layout"
        const val SECTION_PERFORMANCE_OVERLAY = "Performance Overlay"
        const val SECTION_STORAGE = "Storage"
        const val SECTION_MISC = "Miscellaneous"

        const val KEY_BUTTON_A = "button_a"
        const val KEY_BUTTON_B = "button_b"
        const val KEY_BUTTON_X = "button_x"
        const val KEY_BUTTON_Y = "button_y"
        const val KEY_BUTTON_SELECT = "button_select"
        const val KEY_BUTTON_START = "button_start"
        const val KEY_BUTTON_HOME = "button_home"
        const val KEY_BUTTON_UP = "button_up"
        const val KEY_BUTTON_DOWN = "button_down"
        const val KEY_BUTTON_LEFT = "button_left"
        const val KEY_BUTTON_RIGHT = "button_right"
        const val KEY_BUTTON_L = "button_l"
        const val KEY_BUTTON_R = "button_r"
        const val KEY_BUTTON_ZL = "button_zl"
        const val KEY_BUTTON_ZR = "button_zr"
        const val KEY_CIRCLEPAD_AXIS_VERTICAL = "circlepad_axis_vertical"
        const val KEY_CIRCLEPAD_AXIS_HORIZONTAL = "circlepad_axis_horizontal"
        const val KEY_CSTICK_AXIS_VERTICAL = "cstick_axis_vertical"
        const val KEY_CSTICK_AXIS_HORIZONTAL = "cstick_axis_horizontal"
        const val KEY_DPAD_AXIS_VERTICAL = "dpad_axis_vertical"
        const val KEY_DPAD_AXIS_HORIZONTAL = "dpad_axis_horizontal"
        const val HOTKEY_ENABLE = "hotkey_enable"
        const val HOTKEY_SCREEN_SWAP = "hotkey_screen_swap"
        const val HOTKEY_CYCLE_LAYOUT = "hotkey_toggle_layout"
        const val HOTKEY_CLOSE_GAME = "hotkey_close_game"
        const val HOTKEY_PAUSE_OR_RESUME = "hotkey_pause_or_resume_game"
        const val HOTKEY_QUICKSAVE = "hotkey_quickload"
        const val HOTKEY_QUICKLOAD = "hotkey_quickpause"
        const val HOTKEY_TURBO_LIMIT = "hotkey_turbo_limit"
        const val HOTKEY_BUTTON_COMBO = "hotkey_button_combo"

        val buttonKeys = listOf(
            KEY_BUTTON_A,
            KEY_BUTTON_B,
            KEY_BUTTON_X,
            KEY_BUTTON_Y,
            KEY_BUTTON_SELECT,
            KEY_BUTTON_START,
            KEY_BUTTON_HOME
        )
        val buttonTitles = listOf(
            R.string.button_a,
            R.string.button_b,
            R.string.button_x,
            R.string.button_y,
            R.string.button_select,
            R.string.button_start,
            R.string.button_home
        )
        val circlePadKeys = listOf(
            KEY_CIRCLEPAD_AXIS_VERTICAL,
            KEY_CIRCLEPAD_AXIS_HORIZONTAL
        )
        val cStickKeys = listOf(
            KEY_CSTICK_AXIS_VERTICAL,
            KEY_CSTICK_AXIS_HORIZONTAL
        )
        val dPadAxisKeys = listOf(
            KEY_DPAD_AXIS_VERTICAL,
            KEY_DPAD_AXIS_HORIZONTAL
        )
        val dPadButtonKeys = listOf(
            KEY_BUTTON_UP,
            KEY_BUTTON_DOWN,
            KEY_BUTTON_LEFT,
            KEY_BUTTON_RIGHT
        )
        val axisTitles = listOf(
            R.string.controller_axis_vertical,
            R.string.controller_axis_horizontal
        )
        val dPadTitles = listOf(
            R.string.direction_up,
            R.string.direction_down,
            R.string.direction_left,
            R.string.direction_right
        )
        val triggerKeys = listOf(
            KEY_BUTTON_L,
            KEY_BUTTON_R,
            KEY_BUTTON_ZL,
            KEY_BUTTON_ZR
        )
        val triggerTitles = listOf(
            R.string.button_l,
            R.string.button_r,
            R.string.button_zl,
            R.string.button_zr
        )
        val hotKeys = listOf(
            HOTKEY_ENABLE,
            HOTKEY_SCREEN_SWAP,
            HOTKEY_CYCLE_LAYOUT,
            HOTKEY_CLOSE_GAME,
            HOTKEY_PAUSE_OR_RESUME,
            HOTKEY_QUICKSAVE,
            HOTKEY_QUICKLOAD,
            HOTKEY_TURBO_LIMIT,
            HOTKEY_BUTTON_COMBO
        )
        val hotkeyTitles = listOf(
            R.string.controller_hotkey_enable_button,
            R.string.emulation_swap_screens,
            R.string.emulation_cycle_landscape_layouts,
            R.string.emulation_close_game,
            R.string.emulation_toggle_pause,
            R.string.emulation_quicksave,
            R.string.emulation_quickload,
            R.string.turbo_limit_hotkey,
            R.string.button_combo
        )

        // TODO: Move these in with the other setting keys in GenerateSettingKeys.cmake
        const val PREF_FIRST_APP_LAUNCH = "FirstApplicationLaunch"
        const val PREF_MATERIAL_YOU = "MaterialYouTheme"
        const val PREF_THEME_MODE = "ThemeMode"
        const val PREF_BLACK_BACKGROUNDS = "BlackBackgrounds"
        const val PREF_SHOW_HOME_APPS = "ShowHomeApps"
        const val PREF_STATIC_THEME_COLOR = "StaticThemeColor"

        private val configFileSectionsMap: MutableMap<String, List<String>> = HashMap()

        init {
            configFileSectionsMap[SettingsFile.FILE_NAME_CONFIG] =
                listOf(
                    SECTION_CORE,
                    SECTION_SYSTEM,
                    SECTION_CAMERA,
                    SECTION_CONTROLS,
                    SECTION_RENDERER,
                    SECTION_LAYOUT,
                    SECTION_NETWORK,
                    SECTION_UTILITY,
                    SECTION_AUDIO,
                    SECTION_DEBUG,
                    SECTION_THEME,
                    SECTION_CUSTOM_LANDSCAPE,
                    SECTION_CUSTOM_PORTRAIT,
                    SECTION_PERFORMANCE_OVERLAY,
                    SECTION_STORAGE,
                    SECTION_MISC
                )
        }
    }
}
