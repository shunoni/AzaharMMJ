// Copyright 2023-2026 Citra Emulator Project / Azahar Emulator Project
// Licensed under GPLv2 or any later version
// Refer to the misc/licenses/gplv2.txt file included.

package org.citra.citra_emu.features.settings.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.util.TreeMap
import org.citra.citra_emu.CitraApplication
import org.citra.citra_emu.NativeLibrary
import org.citra.citra_emu.R
import org.citra.citra_emu.features.settings.model.AbstractBooleanSetting
import org.citra.citra_emu.features.settings.model.AbstractSetting
import org.citra.citra_emu.features.settings.model.BooleanSetting
import org.citra.citra_emu.features.settings.model.FloatSetting
import org.citra.citra_emu.features.settings.model.IntListSetting
import org.citra.citra_emu.features.settings.model.IntSetting
import org.citra.citra_emu.features.settings.model.ScaledFloatSetting
import org.citra.citra_emu.features.settings.model.SettingSection
import org.citra.citra_emu.features.settings.model.Settings
import org.citra.citra_emu.features.settings.model.Settings.SettingsSectionMap
import org.citra.citra_emu.features.settings.model.StringSetting
import org.citra.citra_emu.features.settings.ui.SettingsActivityView
import org.citra.citra_emu.utils.BiMap
import org.citra.citra_emu.utils.DirectoryInitialization.userDirectory
import org.citra.citra_emu.utils.Log
import org.ini4j.Wini

/**
 * Contains static methods for interacting with .ini files in which settings are stored.
 */
object SettingsFile {
    const val FILE_NAME_CONFIG = "config"

    private var sectionsMap = BiMap<String?, String?>()

    /**
     * Reads a given .ini file from disk and returns it as a HashMap of Settings, themselves
     * effectively a HashMap of key/value settings. If unsuccessful, outputs an error telling why it
     * failed.
     *
     * @param ini          The ini file to load the settings from
     * @param isCustomGame
     * @param view         The current view.
     * @return An Observable that emits a HashMap of the file's contents, then completes.
     */
    fun readFile(
        ini: DocumentFile,
        isCustomGame: Boolean,
        view: SettingsActivityView?
    ): HashMap<String, SettingSection?> {
        val sections: HashMap<String, SettingSection?> = SettingsSectionMap()
        var reader: BufferedReader? = null
        try {
            val context: Context = CitraApplication.appContext
            val inputStream = context.contentResolver.openInputStream(ini.uri)
            reader = BufferedReader(InputStreamReader(inputStream))
            var current: SettingSection? = null
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("[") && line!!.endsWith("]")) {
                    current = sectionFromLine(line!!, isCustomGame)
                    sections[current.name] = current
                } else if (current != null) {
                    val setting = settingFromLine(line!!)
                    if (setting != null) {
                        current.putSetting(setting)
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            Log.error("[SettingsFile] File not found: " + ini.uri + e.message)
            view?.onSettingsFileNotFound()
        } catch (e: IOException) {
            Log.error("[SettingsFile] Error reading from: " + ini.uri + e.message)
            view?.onSettingsFileNotFound()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Log.error("[SettingsFile] Error closing: " + ini.uri + e.message)
                }
            }
        }
        return sections
    }

    fun readFile(fileName: String, view: SettingsActivityView?): HashMap<String, SettingSection?> =
        readFile(getSettingsFile(fileName), false, view)

    fun readFile(fileName: String): HashMap<String, SettingSection?> = readFile(fileName, null)

    /**
     * Reads a given .ini file from disk and returns it as a HashMap of SettingSections, themselves
     * effectively a HashMap of key/value settings. If unsuccessful, outputs an error telling why it
     * failed.
     *
     * @param gameId the id of the game to load it's settings.
     * @param view   The current view.
     */
    fun readCustomGameSettings(
        gameId: String,
        view: SettingsActivityView?
    ): HashMap<String, SettingSection?> = getCustomGameSettingsFile(gameId, false)?.let {
        readFile(it, true, view)
    } ?: SettingsSectionMap()

    fun saveCustomGameSettings(
        gameId: String,
        sections: TreeMap<String, SettingSection?>,
        view: SettingsActivityView
    ) {
        val ini = getCustomGameSettingsFile(gameId, true)
        if (ini == null) {
            view.showToastMessage(
                CitraApplication.appContext.getString(
                    R.string.error_saving,
                    gameId,
                    "Could not create the GameSettings file"
                ),
                false
            )
            return
        }

        try {
            val context: Context = CitraApplication.appContext
            val inputStream = context.contentResolver.openInputStream(ini.uri)
            val writer = Wini(inputStream)
            for (section in sections.values) {
                if (section != null) {
                    writeSection(writer, section)
                }
            }
            inputStream!!.close()
            val outputStream = context.contentResolver.openOutputStream(ini.uri, "wt")
            writer.store(outputStream)
            outputStream!!.flush()
            outputStream.close()
            view.showToastMessage(CitraApplication.appContext.getString(R.string.ini_saved), false)
        } catch (e: Exception) {
            Log.error("[SettingsFile] Error saving game config $gameId.ini: ${e.message}")
            view.showToastMessage(
                CitraApplication.appContext.getString(R.string.error_saving, gameId, e.message),
                false
            )
        }
    }

    fun saveCustomGameSetting(gameId: String, setting: AbstractSetting): Boolean {
        return saveCustomGameSetting(gameId, setting, setting.valueAsString)
    }

    fun saveCustomGameSetting(
        gameId: String,
        setting: AbstractSetting,
        value: String
    ): Boolean {
        val ini = getCustomGameSettingsFile(gameId, true) ?: return false
        try {
            val context: Context = CitraApplication.appContext
            val writer = try {
                val inputStream = context.contentResolver.openInputStream(ini.uri)
                if (inputStream == null) {
                    if (ini.length() == 0L) {
                        Wini()
                    } else {
                        return false
                    }
                } else {
                    inputStream.use { Wini(it) }
                }
            } catch (e: Exception) {
                Log.error("[SettingsFile] Error reading game config $gameId.ini: ${e.message}")
                if (ini.length() == 0L) Wini() else return false
            }
            writer.put(mapSectionNameToIni(setting.section!!), setting.key, value)
            val outputStream = context.contentResolver.openOutputStream(ini.uri, "wt")
                ?: return false
            outputStream.use {
                writer.store(it)
                it.flush()
            }
            return true
        } catch (e: Exception) {
            Log.error("[SettingsFile] Error saving game config $gameId.ini: ${e.message}")
            return false
        }
    }

    fun readCustomGameBooleanSetting(
        gameId: String,
        setting: AbstractBooleanSetting
    ): Boolean? {
        val ini = getCustomGameSettingsFile(gameId, false) ?: return null
        return try {
            val inputStream = CitraApplication.appContext.contentResolver.openInputStream(ini.uri)
                ?: return null
            inputStream.use {
                val value = Wini(it).get(mapSectionNameToIni(setting.section!!), setting.key)
                    ?: return null
                val parsed = when (value.trim().lowercase()) {
                    "true", "1" -> true
                    "false", "0" -> false
                    else -> null
                }
                parsed.also {
                    if (parsed == null) {
                        Log.error(
                            "[SettingsFile] Invalid boolean value for ${setting.key} in " +
                                "$gameId.ini: $value"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.error("[SettingsFile] Error reading game config $gameId.ini: ${e.message}")
            null
        }
    }

    fun saveCurrentGameSetting(setting: AbstractSetting, fileName: String = FILE_NAME_CONFIG) {
        if (NativeLibrary.isRunning()) {
            val gameId = String.format("%016X", NativeLibrary.getRunningTitleId())
            if (isPerGameSettingsEnabled(gameId)) {
                saveCustomGameSetting(gameId, setting)
                return
            }
        }
        saveFile(fileName, setting)
    }

    private fun isPerGameSettingsEnabled(gameId: String): Boolean {
        val ini = getCustomGameSettingsFile(gameId, false) ?: return false
        return try {
            val inputStream = CitraApplication.appContext.contentResolver.openInputStream(ini.uri)
                ?: return false
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var inDebugSection = false
                while (true) {
                    val line = reader.readLine() ?: break
                    val trimmedLine = line.trim()
                    if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                        inDebugSection = trimmedLine.substring(1, trimmedLine.length - 1)
                            .equals("Debugging", ignoreCase = true)
                    } else if (inDebugSection) {
                        val separator = trimmedLine.indexOf('=')
                        if (separator >= 0 && trimmedLine.substring(0, separator).trim()
                                .equals(BooleanSetting.USE_PER_GAME_SETTINGS.key, ignoreCase = true)
                        ) {
                            return trimmedLine.substring(separator + 1).trim()
                                .equals("true", ignoreCase = true)
                        }
                    }
                }
                false
            }
        } catch (e: Exception) {
            Log.error("[SettingsFile] Error reading per-game mode for $gameId: ${e.message}")
            false
        }
    }

    /**
     * Saves a Settings HashMap to a given .ini file on disk. If unsuccessful, outputs an error
     * telling why it failed.
     *
     * @param fileName The target filename without a path or extension.
     * @param sections The HashMap containing the Settings we want to serialize.
     * @param view     The current view.
     */
    fun saveFile(
        fileName: String,
        sections: TreeMap<String, SettingSection?>,
        view: SettingsActivityView
    ) {
        val ini = getSettingsFile(fileName)
        try {
            val context: Context = CitraApplication.appContext
            val inputStream = context.contentResolver.openInputStream(ini.uri)
            val writer = Wini(inputStream)
            val keySet: Set<String> = sections.keys
            for (key in keySet) {
                val section = sections[key]
                writeSection(writer, section!!)
            }
            inputStream!!.close()
            val outputStream = context.contentResolver.openOutputStream(ini.uri, "wt")
            writer.store(outputStream)
            outputStream!!.flush()
            outputStream.close()
        } catch (e: Exception) {
            Log.error("[SettingsFile] File not found: $fileName.ini: ${e.message}")
            view.showToastMessage(
                CitraApplication.appContext
                    .getString(R.string.error_saving, fileName, e.message),
                false
            )
        }
    }

    fun saveFile(fileName: String, setting: AbstractSetting) {
        val ini = getSettingsFile(fileName)
        try {
            val context: Context = CitraApplication.appContext
            val inputStream = context.contentResolver.openInputStream(ini.uri)
            val writer = Wini(inputStream)
            writer.put(setting.section, setting.key, setting.valueAsString)
            inputStream!!.close()
            val outputStream = context.contentResolver.openOutputStream(ini.uri, "wt")
            writer.store(outputStream)
            outputStream!!.flush()
            outputStream.close()
        } catch (e: Exception) {
            Log.error("[SettingsFile] File not found: $fileName.ini: ${e.message}")
        }
    }

    private fun mapSectionNameFromIni(generalSectionName: String): String? =
        if (sectionsMap.getForward(generalSectionName) != null) {
            sectionsMap.getForward(generalSectionName)
        } else {
            generalSectionName
        }

    private fun mapSectionNameToIni(generalSectionName: String): String =
        if (sectionsMap.getBackward(generalSectionName) != null) {
            sectionsMap.getBackward(generalSectionName).toString()
        } else {
            generalSectionName
        }

    fun getSettingsFile(fileName: String): DocumentFile {
        val root = DocumentFile.fromTreeUri(CitraApplication.appContext, Uri.parse(userDirectory))
        val configDirectory = root!!.findFile("config")
        return configDirectory!!.findFile("$fileName.ini")!!
    }

    private fun getCustomGameSettingsFile(gameId: String, create: Boolean): DocumentFile? {
        val root = DocumentFile.fromTreeUri(CitraApplication.appContext, Uri.parse(userDirectory))
            ?: return null
        val directory = root.findFile("GameSettings") ?: run {
            if (!create) {
                return null
            }
            root.createDirectory("GameSettings") ?: return null
        }
        val fileName = "$gameId.ini"
        // Some document providers append an extra extension (for example .txt) when
        // creating text files. Accept any variant so saves and reads stay consistent.
        directory.findFile(fileName)?.let { return it }
        directory.listFiles().firstOrNull {
            it.name == fileName || it.name?.startsWith("$fileName.") == true
        }?.let { return it }
        return if (create) directory.createFile("text/plain", fileName) else null
    }

    private fun sectionFromLine(line: String, isCustomGame: Boolean): SettingSection {
        var sectionName: String = line.substring(1, line.length - 1)
        if (isCustomGame) {
            sectionName = mapSectionNameToIni(sectionName)
        }
        return SettingSection(sectionName)
    }

    /**
     * For a line of text, determines what type of data is being represented, and returns
     * a Setting object containing this data.
     *
     * @param line    The line of text being parsed.
     * @return A typed Setting containing the key/value contained in the line.
     */
    private fun settingFromLine(line: String): AbstractSetting? {
        val splitLine = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (splitLine.size != 2) {
            return null
        }
        val key = splitLine[0].trim { it <= ' ' }
        val value = splitLine[1].trim { it <= ' ' }
        if (value.isEmpty()) {
            return null
        }

        val booleanSetting = BooleanSetting.from(key)
        if (booleanSetting != null) {
            booleanSetting.boolean = value.toBoolean()
            return booleanSetting
        }

        val intSetting = IntSetting.from(key)
        if (intSetting != null) {
            try {
                intSetting.int = value.toInt()
            } catch (e: NumberFormatException) {
                intSetting.int = if (value.toBoolean()) 1 else 0
            }
            return intSetting
        }

        val scaledFloatSetting = ScaledFloatSetting.from(key)
        if (scaledFloatSetting != null) {
            scaledFloatSetting.float = value.toFloat() * scaledFloatSetting.scale
            return scaledFloatSetting
        }

        val floatSetting = FloatSetting.from(key)
        if (floatSetting != null) {
            floatSetting.float = value.toFloat()
            return floatSetting
        }

        val stringSetting = StringSetting.from(key)
        if (stringSetting != null) {
            stringSetting.string = value
            return stringSetting
        }

        val intListSetting = IntListSetting.from(key)
        if (intListSetting != null) {
            intListSetting.list = value.split(", ").map { it.toInt() }
        }

        return null
    }

    /**
     * Writes the contents of a Section HashMap to disk.
     *
     * @param parser  A Wini pointed at a file on disk.
     * @param section A section containing settings to be written to the file.
     */
    private fun writeSection(parser: Wini, section: SettingSection) {
        // Write the section header.
        val header = mapSectionNameToIni(section.name)

        // Write this section's values.
        val settings = section.settings
        val keySet: Set<String> = settings.keys
        for (key in keySet) {
            val setting = settings[key]
            parser.put(header, setting!!.key, setting.valueAsString)
        }
    }
}
