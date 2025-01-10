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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jlyrics.Lyrics;
import com.jagrosh.jlyrics.LyricsClient;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

/**
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LyricsCmd extends MusicCommand {
    private final LyricsClient client = new LyricsClient();

    private static final String COMMAND_NAME = "lyrics";

    public LyricsCmd() {
        super(COMMAND_NAME);
        this.arguments = "[song name]";
        this.help = "shows the lyrics of a song";
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    public void doCommand(CommandEvent event) {
        String title = getTitleFromEvent(event);
        if (title == null) {
            return; // Error already handled in getTitleFromEvent
        }

        event.getChannel().sendTyping().queue();
        fetchAndReplyWithLyrics(event, title);
    }

    private String getTitleFromEvent(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            AudioHandler sendingHandler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (sendingHandler.isMusicPlaying(event.getJDA())) {
                return sendingHandler.getPlayer().getPlayingTrack().getInfo().title;
            } else {
                event.replyError("There must be music playing to use that!");
                return null;
            }
        }
        return event.getArgs();
    }

    private void fetchAndReplyWithLyrics(CommandEvent event, String title) {
        client.getLyrics(title).thenAccept(lyrics -> {
            if (lyrics == null) {
                handleLyricsNotFound(event, title);
            } else {
                handleLyricsFound(event, lyrics, title);
            }
        });
    }

    private void handleLyricsNotFound(CommandEvent event, String title) {
        String message = "Lyrics for `" + title + "` could not be found!"
                + (event.getArgs().isEmpty() ? " Try entering the song name manually (`lyrics [song name]`)" : "");
        event.replyError(message);
    }

    private void handleLyricsFound(CommandEvent event, Lyrics lyrics, String title) {
        EmbedBuilder eb = new EmbedBuilder()
                .setAuthor(lyrics.getAuthor())
                .setColor(event.getSelfMember().getColor())
                .setTitle(lyrics.getTitle(), lyrics.getURL());

        if (lyrics.getContent().length() > 15000) {
            event.replyWarning("Lyrics for `" + title + "` found but likely not correct: " + lyrics.getURL());
        } else if (lyrics.getContent().length() > 2000) {
            sendLongLyrics(event, lyrics.getContent(), eb);
        } else {
            event.reply(eb.setDescription(lyrics.getContent()).build());
        }
    }

    private void sendLongLyrics(CommandEvent event, String content, EmbedBuilder eb) {
        String trimmedContent = content.trim();
        while (trimmedContent.length() > 2000) {
            int index = getSplitIndex(trimmedContent, 2000);
            event.reply(eb.setDescription(trimmedContent.substring(0, index).trim()).build());
            trimmedContent = trimmedContent.substring(index).trim();
            eb.setAuthor(null).setTitle(null, null);
        }
        event.reply(eb.setDescription(trimmedContent).build());
    }

    private int getSplitIndex(String content, int maxLength) {
        int index = content.lastIndexOf("\n\n", maxLength);
        if (index == -1) index = content.lastIndexOf("\n", maxLength);
        if (index == -1) index = content.lastIndexOf(" ", maxLength);
        return (index == -1) ? maxLength : index;
    }

}
