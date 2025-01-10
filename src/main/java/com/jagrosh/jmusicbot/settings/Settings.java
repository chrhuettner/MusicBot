/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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

import com.jagrosh.jdautilities.command.GuildSettingsProvider;

import java.util.Collection;
import java.util.Collections;

import com.jagrosh.jmusicbot.audio.PlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.json.JSONObject;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class Settings implements GuildSettingsProvider {
    private final SettingsManager manager;
    protected long textId;
    protected long voiceId;
    protected long roleId;
    private int volume;
    private String defaultPlaylist;
    private RepeatMode repeatMode;
    private QueueType queueType;
    private String prefix;
    private double skipRatio;

    public Settings(SettingsManager manager, JSONObject jsonObject) {
        this.manager = manager;
        try {
            this.textId = Long.parseLong(jsonObject.has("text_channel_id") ? jsonObject.getString("text_channel_id") : "0");
        } catch (NumberFormatException e) {
            this.textId = 0;
        }
        try {
            this.voiceId = Long.parseLong(jsonObject.has("voice_channel_id") ? jsonObject.getString("voice_channel_id") : "0");
        } catch (NumberFormatException e) {
            this.voiceId = 0;
        }
        try {
            this.roleId = Long.parseLong(jsonObject.has("dj_role_id") ? jsonObject.getString("dj_role_id") : "0");
        } catch (NumberFormatException e) {
            this.roleId = 0;
        }
        this.volume = jsonObject.has("volume") ? jsonObject.getInt("volume") : 100;
        this.defaultPlaylist = jsonObject.has("default_playlist") ? jsonObject.getString("default_playlist") : null;
        this.repeatMode = jsonObject.has("repeat_mode") ? jsonObject.getEnum(RepeatMode.class, "repeat_mode") : RepeatMode.OFF;
        this.prefix = jsonObject.has("prefix") ? jsonObject.getString("prefix") : null;
        this.skipRatio = jsonObject.has("skip_ratio") ? jsonObject.getDouble("skip_ratio") : -1;
        this.queueType = jsonObject.has("queue_type") ? jsonObject.getEnum(QueueType.class, "queue_type") : QueueType.FAIR;
    }

    public Settings(SettingsManager manager) {
        this.manager = manager;
        this.textId = 0;
        this.voiceId = 0;
        this.roleId = 0;
        this.volume = 100;
        this.defaultPlaylist = null;
        this.repeatMode = RepeatMode.OFF;
        this.prefix = null;
        this.skipRatio = -1;
        this.queueType = QueueType.FAIR;
    }

    // Getters
    public TextChannel getTextChannel(Guild guild) {
        return guild == null ? null : guild.getTextChannelById(textId);
    }

    public VoiceChannel getVoiceChannel(Guild guild) {
        return guild == null ? null : guild.getVoiceChannelById(voiceId);
    }

    public Role getRole(Guild guild) {
        return guild == null ? null : guild.getRoleById(roleId);
    }

    public int getVolume() {
        return volume;
    }

    public String getDefaultPlaylist() {
        return defaultPlaylist;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public String getPrefix() {
        return prefix;
    }

    public double getSkipRatio() {
        return skipRatio;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    @Override
    public Collection<String> getPrefixes() {
        return prefix == null ? Collections.emptySet() : Collections.singleton(prefix);
    }

    // Setters
    public void setTextChannel(TextChannel tc) {
        this.textId = tc == null ? 0 : tc.getIdLong();
        this.manager.writeSettings();
    }

    public void setVoiceChannel(VoiceChannel vc) {
        this.voiceId = vc == null ? 0 : vc.getIdLong();
        this.manager.writeSettings();
    }

    public void setDJRole(Role role) {
        this.roleId = role == null ? 0 : role.getIdLong();
        this.manager.writeSettings();
    }

    public void setVolume(int volume) {
        this.volume = volume;
        this.manager.writeSettings();
    }

    public void setDefaultPlaylist(String defaultPlaylist) {
        this.defaultPlaylist = defaultPlaylist;
        this.manager.writeSettings();
    }

    public void setRepeatMode(RepeatMode mode) {
        this.repeatMode = mode;
        this.manager.writeSettings();
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        this.manager.writeSettings();
    }

    public void setSkipRatio(double skipRatio) {
        this.skipRatio = skipRatio;
        this.manager.writeSettings();
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
        this.manager.writeSettings();
    }
}
