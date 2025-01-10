package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Message;

public abstract class LoadSingleHandler {
    protected final Message m;
    protected final CommandEvent event;
    protected final boolean ytsearch;

    protected final BotConfig botConfig;

    public LoadSingleHandler(Message m, CommandEvent event, boolean ytsearch)
    {
        this.m = m;
        this.event = event;
        this.ytsearch = ytsearch;
        this.botConfig = BotConfig.getInstance();
    }
    protected boolean checkTrackLength(AudioTrack track) {
        if (botConfig.isTooLong(track)) {
            m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " This track (**" + track.getInfo().title + "**) is longer than the allowed maximum: `"
                    + TimeUtil.formatTime(track.getDuration()) + "` > `" + TimeUtil.formatTime(botConfig.getMaxSeconds() * 1000) + "`")).queue();
            return false;
        }
        return true;
    }
}
