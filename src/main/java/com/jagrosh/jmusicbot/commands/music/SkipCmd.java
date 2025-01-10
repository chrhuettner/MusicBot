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
package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.RequestMetadata;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.utils.FormatUtil;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SkipCmd extends MusicCommand 
{
    private static final String COMMAND_NAME = "skip";

    private final SettingsManager settingsManager;

    public SkipCmd()
    {
        super(COMMAND_NAME);
        this.help = "votes to skip the current song";
        this.beListening = true;
        this.bePlaying = true;
        this.settingsManager = SettingsManager.getInstance();
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        RequestMetadata rm = handler.getRequestMetadata();
        double skipRatio = getSkipRatio(event);

        if (isRequesterOrSkipFree(event, rm, skipRatio)) {
            skipTrack(event, handler);
        } else {
            processSkipVote(event, handler, skipRatio);
        }
    }

    private double getSkipRatio(CommandEvent event) {
        double skipRatio = settingsManager.getSettings(event.getGuild()).getSkipRatio();
        return (skipRatio == -1) ? botConfig.getSkipRatio() : skipRatio;
    }

    private boolean isRequesterOrSkipFree(CommandEvent event, RequestMetadata rm, double skipRatio) {
        return event.getAuthor().getIdLong() == rm.getOwner() || skipRatio == 0;
    }

    private void skipTrack(CommandEvent event, AudioHandler handler) {
        event.reply(event.getClient().getSuccess() + " Skipped **" + handler.getPlayer().getPlayingTrack().getInfo().title + "**");
        handler.getPlayer().stopTrack();
    }

    private void processSkipVote(CommandEvent event, AudioHandler handler, double skipRatio) {
        int listeners = getActiveListeners(event);
        int requiredVotes = (int) Math.ceil(listeners * skipRatio);
        int currentVotes = updateVoteCount(event, handler);

        String voteMessage = formatVoteMessage(event, handler, currentVotes, requiredVotes, listeners);

        if (currentVotes >= requiredVotes) {
            voteMessage += "\n" + event.getClient().getSuccess() + " Skipped **" + handler.getPlayer().getPlayingTrack().getInfo().title
                    + "** " + getRequesterInfo(handler.getRequestMetadata());
            handler.getPlayer().stopTrack();
        }

        event.reply(voteMessage);
    }

    private int getActiveListeners(CommandEvent event) {
        return (int) event.getSelfMember().getVoiceState().getChannel().getMembers().stream()
                .filter(m -> !m.getUser().isBot() && !m.getVoiceState().isDeafened())
                .count();
    }

    private int updateVoteCount(CommandEvent event, AudioHandler handler) {
        if (!handler.getVotes().contains(event.getAuthor().getId())) {
            handler.getVotes().add(event.getAuthor().getId());
        }
        return (int) event.getSelfMember().getVoiceState().getChannel().getMembers().stream()
                .filter(m -> handler.getVotes().contains(m.getUser().getId()))
                .count();
    }

    private String formatVoteMessage(CommandEvent event, AudioHandler handler, int skippers, int required, int listeners) {
        String prefix = handler.getVotes().contains(event.getAuthor().getId())
                ? event.getClient().getWarning() + " You already voted to skip this song `["
                : event.getClient().getSuccess() + " You voted to skip the song `[";

        return prefix + skippers + " votes, " + required + "/" + listeners + " needed]`";
    }

    private String getRequesterInfo(RequestMetadata rm) {
        return (rm.getOwner() == 0L) ? "(autoplay)" : "(requested by **" + FormatUtil.formatUsername(rm.user) + "**)";
    }


}
