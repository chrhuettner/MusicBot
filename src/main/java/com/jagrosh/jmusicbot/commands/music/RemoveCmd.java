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
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.settings.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class RemoveCmd extends MusicCommand 
{
    private static final String COMMAND_NAME = "remove";

    public RemoveCmd(Bot bot)
    {
        super(COMMAND_NAME, bot);
        this.help = "removes a song from the queue";
        this.arguments = "<position|ALL>";
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (handler.getQueue().isEmpty()) {
            event.replyError("There is nothing in the queue!");
            return;
        }

        if (event.getArgs().equalsIgnoreCase("all")) {
            removeAllUserEntries(event, handler);
            return;
        }

        int pos = parsePosition(event.getArgs(), handler.getQueue().size());
        if (pos == -1) {
            event.replyError("Position must be a valid integer between 1 and " + handler.getQueue().size() + "!");
            return;
        }

        removeSpecificEntry(event, handler, pos);
    }

    private void removeAllUserEntries(CommandEvent event, AudioHandler handler) {
        int count = handler.getQueue().removeAll(event.getAuthor().getIdLong());
        if (count == 0) {
            event.replyWarning("You don't have any songs in the queue!");
        } else {
            event.replySuccess("Successfully removed your " + count + " entries.");
        }
    }

    private int parsePosition(String args, int maxSize) {
        try {
            int pos = Integer.parseInt(args);
            if (pos < 1 || pos > maxSize) return -1;
            return pos;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void removeSpecificEntry(CommandEvent event, AudioHandler handler, int pos) {
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        boolean isDJ = checkDJPermission(event, settings);

        QueuedTrack qt = handler.getQueue().get(pos - 1);
        if (qt.getIdentifier() == event.getAuthor().getIdLong()) {
            handler.getQueue().remove(pos - 1);
            event.replySuccess("Removed **" + qt.getTrack().getInfo().title + "** from the queue");
        } else if (isDJ) {
            handler.getQueue().remove(pos - 1);
            notifyUserRemoval(event, qt);
        } else {
            event.replyError("You cannot remove **" + qt.getTrack().getInfo().title + "** because you didn't add it!");
        }
    }

    private boolean checkDJPermission(CommandEvent event, Settings settings) {
        return event.getMember().hasPermission(Permission.MANAGE_SERVER) ||
                event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
    }

    private void notifyUserRemoval(CommandEvent event, QueuedTrack qt) {
        User u;
        try {
            u = event.getJDA().getUserById(qt.getIdentifier());
        } catch (Exception e) {
            u = null;
        }
        event.replySuccess("Removed **" + qt.getTrack().getInfo().title + "** from the queue (requested by " +
                (u == null ? "someone" : "**" + u.getName() + "**") + ")");
    }


}
