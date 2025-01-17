/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.settings;

import com.jagrosh.jdautilities.command.GuildSettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SettingsManager implements GuildSettingsManager<Settings>, SettingsWriter
{
    private final static Logger LOG = LoggerFactory.getLogger("Settings");
    private final static String SETTINGS_FILE = "serversettings.json";
    private final HashMap<Long,Settings> settings;

    private static SettingsManager settingsManager;
    public static SettingsManager getInstance(){
        if(settingsManager == null){
            settingsManager = new SettingsManager();
        }
        return settingsManager;
    }

    private SettingsManager()
    {
        this.settings = new HashMap<>();

        try {
            JSONObject loadedSettings = new JSONObject(new String(Files.readAllBytes(OtherUtil.getPath(SETTINGS_FILE))));
            loadedSettings.keySet().forEach((id) -> {
                JSONObject o = loadedSettings.getJSONObject(id);

                // Legacy version support: On versions 0.3.3 and older, the repeat mode was represented as a boolean.
                if (!o.has("repeat_mode") && o.has("repeat") && o.getBoolean("repeat"))
                    o.put("repeat_mode", RepeatMode.ALL);


                settings.put(Long.parseLong(id), new Settings(this, o));
            });
        } catch (NoSuchFileException e) {
            // create an empty json file
            try {
                LOG.info("serversettings.json will be created in " + OtherUtil.getPath("serversettings.json").toAbsolutePath());
                Files.write(OtherUtil.getPath("serversettings.json"), new JSONObject().toString(4).getBytes());
            } catch(IOException ex) {
                LOG.warn("Failed to create new settings file: "+ex);
            }
            return;
        } catch(IOException | JSONException e) {
            LOG.warn("Failed to load server settings: "+e);
        }

        LOG.info("serversettings.json loaded from " + OtherUtil.getPath("serversettings.json").toAbsolutePath());
    }

    /**
     * Gets non-null settings for a Guild
     *
     * @param guild the guild to get settings for
     * @return the existing settings, or new settings for that guild
     */
    @Override
    public Settings getSettings(Guild guild)
    {
        return getSettings(guild.getIdLong());
    }

    public Settings getSettings(long guildId)
    {
        return settings.computeIfAbsent(guildId, id -> createDefaultSettings());
    }

    private Settings createDefaultSettings()
    {
        return new Settings(this);
    }

    @Override
    public void writeSettings() {
        JSONObject obj = new JSONObject();
        settings.forEach((key, s) -> {
            JSONObject settingsJson = buildSettingsJson(s);
            obj.put(Long.toString(key), settingsJson);
        });

        try {
            Files.write(OtherUtil.getPath(SETTINGS_FILE), obj.toString(4).getBytes());
        } catch (IOException ex) {
            LOG.warn("Failed to write to file: " + ex);
        }
    }

    private JSONObject buildSettingsJson(Settings s) {
        JSONObject o = new JSONObject();

        if (s.textId != 0) o.put("text_channel_id", Long.toString(s.textId));
        if (s.voiceId != 0) o.put("voice_channel_id", Long.toString(s.voiceId));
        if (s.roleId != 0) o.put("dj_role_id", Long.toString(s.roleId));
        if (s.getVolume() != 100) o.put("volume", s.getVolume());
        if (s.getDefaultPlaylist() != null) o.put("default_playlist", s.getDefaultPlaylist());
        if (s.getRepeatMode() != RepeatMode.OFF) o.put("repeat_mode", s.getRepeatMode());
        if (s.getPrefix() != null) o.put("prefix", s.getPrefix());
        if (s.getSkipRatio() != -1) o.put("skip_ratio", s.getSkipRatio());
        if (s.getQueueType() != QueueType.FAIR) o.put("queue_type", s.getQueueType().name());

        return o;
    }
}
