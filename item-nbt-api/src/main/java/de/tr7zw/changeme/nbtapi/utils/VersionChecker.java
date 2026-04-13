package de.tr7zw.changeme.nbtapi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.bukkit.configuration.file.YamlConfiguration;

import de.tr7zw.changeme.nbtapi.NBTItem;

/**
 * This class checks plugin identity for logging purposes
 *
 */
public class VersionChecker {

    public static boolean hideOk = false;

    protected static String getPlugin() {

        ClassLoader classLoader = VersionChecker.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("plugin.yml");

        if (inputStream != null) {

            try (InputStreamReader reader = new InputStreamReader(inputStream)) {

                YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(reader);
                return pluginYml.getString("name");

            } catch (IOException e) {

                // ignored
            }

        }

        return NBTItem.class.getPackage().getName();

    }

}
