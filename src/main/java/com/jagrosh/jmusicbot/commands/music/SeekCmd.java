/*
 * Copyright 2020 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.commands.DJCommand;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Whew., Inc.
 */
public class SeekCmd extends MusicCommand
{
    private final static Logger LOG = LoggerFactory.getLogger("Seeking");

    private static final String COMMAND_NAME = "seek";

    public SeekCmd()
    {
        super(COMMAND_NAME);
        this.help = "seeks the current song";
        this.arguments = "[+ | -] <HH:MM:SS | MM:SS | SS>|<0h0m0s | 0m0s | 0s>";
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        AudioTrack playingTrack = handler.getPlayer().getPlayingTrack();

        if (!isTrackSeekable(event, playingTrack)) return;
        if (!hasSeekPermission(event, playingTrack)) return;

        TimeUtil.SeekTime seekTime = parseSeekTime(event);
        if (seekTime == null) return;

        if (!isSeekWithinBounds(event, playingTrack, seekTime)) return;

        seekTrack(event, playingTrack, seekTime);
    }

    private boolean isTrackSeekable(CommandEvent event, AudioTrack playingTrack) {
        if (!playingTrack.isSeekable()) {
            event.replyError("This track is not seekable.");
            return false;
        }
        return true;
    }

    private boolean hasSeekPermission(CommandEvent event, AudioTrack playingTrack) {
        if (!DJCommand.checkDJPermission(event) && playingTrack.getUserData(RequestMetadata.class).getOwner() != event.getAuthor().getIdLong()) {
            event.replyError("You cannot seek **" + playingTrack.getInfo().title + "** because you didn't add it!");
            return false;
        }
        return true;
    }

    private TimeUtil.SeekTime parseSeekTime(CommandEvent event) {
        TimeUtil.SeekTime seekTime = TimeUtil.parseTime(event.getArgs());
        if (seekTime == null) {
            event.replyError("Invalid seek! Expected format: " + arguments + "\nExamples: `1:02:23` `+1:10` `-90`, `1h10m`, `+90s`");
        }
        return seekTime;
    }

    private boolean isSeekWithinBounds(CommandEvent event, AudioTrack playingTrack, TimeUtil.SeekTime seekTime) {
        long seekMilliseconds = seekTime.relative ? playingTrack.getPosition() + seekTime.milliseconds : seekTime.milliseconds;
        if (seekMilliseconds > playingTrack.getDuration()) {
            event.replyError("Cannot seek to `" + TimeUtil.formatTime(seekMilliseconds) + "` because the current track is `" + TimeUtil.formatTime(playingTrack.getDuration()) + "` long!");
            return false;
        }
        return true;
    }

    private void seekTrack(CommandEvent event, AudioTrack playingTrack, TimeUtil.SeekTime seekTime) {
        try {
            playingTrack.setPosition(seekTime.relative ? playingTrack.getPosition() + seekTime.milliseconds : seekTime.milliseconds);
            event.replySuccess("Successfully seeked to `" + TimeUtil.formatTime(playingTrack.getPosition()) + "/" + TimeUtil.formatTime(playingTrack.getDuration()) + "`!");
        } catch (Exception e) {
            event.replyError("An error occurred while trying to seek: " + e.getMessage());
            LOG.warn("Failed to seek track " + playingTrack.getIdentifier(), e);
        }
    }

}
