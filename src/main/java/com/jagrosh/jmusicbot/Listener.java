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
package com.jagrosh.jmusicbot;

import com.jagrosh.jmusicbot.audio.AloneInVoiceHandler;
import com.jagrosh.jmusicbot.audio.NowplayingHandler;
import com.jagrosh.jmusicbot.audio.PlayerManager;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Listener extends ListenerAdapter {

    private BotConfig config;

    private SettingsManager settingsManager;

    private NowplayingHandler nowplayingHandler;

    private AloneInVoiceHandler aloneInVoiceHandler;

    private PlayerManager playerManager;

    private ScheduledExecutorService executorService;

    private Bot bot;

    public Listener() {
        this.bot = Bot.getInstance();
        this.config = BotConfig.getInstance();
        this.settingsManager = SettingsManager.getInstance();
        this.nowplayingHandler = NowplayingHandler.getInstance();
        this.aloneInVoiceHandler = AloneInVoiceHandler.getInstance();
        this.playerManager = PlayerManager.getInstance();
        this.executorService = ScheduledExecutorServiceProvider.getInstance();
    }

    @Override
    public void onReady(ReadyEvent event) {
        checkGuilds(event);
        credit();
        processGuilds(event);
        scheduleUpdateAlerts();
    }

    private void checkGuilds(ReadyEvent event) {
        if (event.getJDA().getGuildCache().isEmpty()) {
            Logger log = LoggerFactory.getLogger("MusicBot");
            log.warn("This bot is not on any guilds! Use the following link to add the bot to your guilds!");
            log.warn(event.getJDA().getInviteUrl(JDAProvider.getRecommendedPermissions()));
        }
    }

    private void processGuilds(ReadyEvent event) {
        event.getJDA().getGuilds().forEach(guild -> {
            try {
                String defpl = settingsManager.getSettings(guild).getDefaultPlaylist();
                VoiceChannel vc = settingsManager.getSettings(guild).getVoiceChannel(guild);
                if (defpl != null && vc != null && playerManager.setUpHandler(guild).playFromDefault()) {
                    guild.getAudioManager().openAudioConnection(vc);
                }
            } catch (Exception ignore) {
            }
        });
    }

    private void scheduleUpdateAlerts() {
        if (config.useUpdateAlerts()) {
            executorService.scheduleWithFixedDelay(() -> {
                try {
                    User owner = JDAProvider.getInstance().retrieveUserById(config.getOwnerId()).complete();
                    String currentVersion = OtherUtil.getCurrentVersion();
                    String latestVersion = OtherUtil.getLatestVersion();
                    if (latestVersion != null && !currentVersion.equalsIgnoreCase(latestVersion)) {
                        String msg = String.format(OtherUtil.NEW_VERSION_AVAILABLE, currentVersion, latestVersion);
                        owner.openPrivateChannel().queue(pc -> pc.sendMessage(msg).queue());
                    }
                } catch (Exception ignored) {
                }
            }, 0, 24, TimeUnit.HOURS);
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        nowplayingHandler.onMessageDelete(event.getGuild(), event.getMessageIdLong());
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        aloneInVoiceHandler.onVoiceUpdate(event);
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        bot.shutdown();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        credit();
    }

    // make sure people aren't adding clones to dbots
    private void credit() {
        JDA jda = JDAProvider.getInstance();

        Guild dbots = jda.getGuildById(110373943822540800L);
        if (dbots == null)
            return;
        if (config.getDBots())
            return;
        jda.getTextChannelById(119222314964353025L)
                .sendMessage("This account is running JMusicBot. Please do not list bot clones on this server, <@" + config.getOwnerId() + ">.").complete();
        dbots.leave().queue();
    }
}
