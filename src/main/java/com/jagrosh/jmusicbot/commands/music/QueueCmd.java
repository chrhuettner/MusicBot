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
package com.jagrosh.jmusicbot.commands.music;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.Paginator;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.EventWaiterProvider;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.NowplayingHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.settings.QueueType;
import com.jagrosh.jmusicbot.settings.RepeatMode;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class QueueCmd extends MusicCommand 
{
    private final Paginator.Builder builder;

    private static final String COMMAND_NAME = "queue";

    private NowplayingHandler nowplayingHandler;

    public QueueCmd()
    {
        super(COMMAND_NAME);
        this.help = "shows the current queue";
        this.arguments = "[pagenum]";
        this.bePlaying = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_ADD_REACTION,Permission.MESSAGE_EMBED_LINKS};
        builder = new Paginator.Builder()
                .setColumns(1)
                .setFinalAction(m -> {try{m.clearReactions().queue();}catch(PermissionException ignore){}})
                .setItemsPerPage(10)
                .waitOnSinglePage(false)
                .useNumberedItems(true)
                .showPageNumbers(true)
                .wrapPageEnds(true)
                .setEventWaiter(EventWaiterProvider.getInstance())
                .setTimeout(1, TimeUnit.MINUTES);
        this.nowplayingHandler = NowplayingHandler.getInstance();
    }

    @Override
    public void doCommand(CommandEvent event) {
        int pagenum = parsePageNumber(event.getArgs());
        AudioHandler ah = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        List<QueuedTrack> list = ah.getQueue().getList();

        if (list.isEmpty()) {
            handleEmptyQueue(event, ah);
            return;
        }

        processQueue(event, ah, list, pagenum);
    }

    private int parsePageNumber(String args) {
        try {
            return Integer.parseInt(args);
        } catch (NumberFormatException ignore) {
            return 1;
        }
    }

    private void handleEmptyQueue(CommandEvent event, AudioHandler ah) {
        Message nowp = ah.getNowPlaying(event.getJDA());
        Message nonowp = ah.getNoMusicPlaying(event.getJDA());
        Message built = new MessageBuilder()
                .setContent(event.getClient().getWarning() + " There is no music in the queue!")
                .setEmbeds((nowp == null ? nonowp : nowp).getEmbeds().get(0)).build();
        event.reply(built, m -> {
            if (nowp != null)
                nowplayingHandler.setLastNPMessage(m);
        });
    }

    private void processQueue(CommandEvent event, AudioHandler ah, List<QueuedTrack> list, int pagenum) {
        String[] songs = new String[list.size()];
        long total = 0;

        for (int i = 0; i < list.size(); i++) {
            total += list.get(i).getTrack().getDuration();
            songs[i] = list.get(i).toString();
        }

        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        long finalTotal = total;

        builder.setText((i1, i2) -> getQueueTitle(ah, event.getClient().getSuccess(), songs.length, finalTotal, settings.getRepeatMode(), settings.getQueueType()))
                .setItems(songs)
                .setUsers(event.getAuthor())
                .setColor(event.getSelfMember().getColor());

        builder.build().paginate(event.getChannel(), pagenum);
    }
    
    private String getQueueTitle(AudioHandler ah, String success, int songslength, long total, RepeatMode repeatmode, QueueType queueType)
    {
        StringBuilder sb = new StringBuilder();
        if(ah.getPlayer().getPlayingTrack()!=null)
        {
            sb.append(ah.getStatusEmoji()).append(" **")
                    .append(ah.getPlayer().getPlayingTrack().getInfo().title).append("**\n");
        }
        return FormatUtil.filter(sb.append(success).append(" Current Queue | ").append(songslength)
                .append(" entries | `").append(TimeUtil.formatTime(total)).append("` ")
                .append("| ").append(queueType.getEmoji()).append(" `").append(queueType.getUserFriendlyName()).append('`')
                .append(repeatmode.getEmoji() != null ? " | "+repeatmode.getEmoji() : "").toString());
    }


}
