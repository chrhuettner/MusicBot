/*
 * Copyright 2019 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jmusicbot.EventWaiterProvider;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.DJCommand;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Michaili K.
 */
public class ForceRemoveCmd extends DJCommand
{
    private static final String COMMAND_NAME = "forceremove";

    public ForceRemoveCmd()
    {
        super(COMMAND_NAME);
        this.help = "removes all entries by a user from the queue";
        this.arguments = "<user>";
        this.beListening = false;
        this.bePlaying = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (!validateArguments(event)) return;

        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (!validateQueue(event, handler)) return;

        List<Member> foundMembers = FinderUtil.findMembers(event.getArgs(), event.getGuild());
        if (!validateFoundMembers(event, foundMembers)) return;

        if (foundMembers.size() > 1) {
            promptUserSelection(event, foundMembers);
        } else {
            removeAllEntries(foundMembers.get(0).getUser(), event);
        }
    }

    private boolean validateArguments(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.replyError("You need to mention a user!");
            return false;
        }
        return true;
    }

    private boolean validateQueue(CommandEvent event, AudioHandler handler) {
        if (handler.getQueue().isEmpty()) {
            event.replyError("There is nothing in the queue!");
            return false;
        }
        return true;
    }

    private boolean validateFoundMembers(CommandEvent event, List<Member> found) {
        if (found.isEmpty()) {
            event.replyError("Unable to find the user!");
            return false;
        }
        return true;
    }

    private void promptUserSelection(CommandEvent event, List<Member> foundMembers) {
        OrderedMenu.Builder builder = new OrderedMenu.Builder();

        for (int i = 0; i < foundMembers.size() && i < 4; i++) {
            Member member = foundMembers.get(i);
            builder.addChoice("**" + member.getUser().getName() + "**#" + member.getUser().getDiscriminator());
        }

        builder.setSelection((msg, i) -> removeAllEntries(foundMembers.get(i - 1).getUser(), event))
                .setText("Found multiple users:")
                .setColor(event.getSelfMember().getColor())
                .useNumbers()
                .setUsers(event.getAuthor())
                .useCancelButton(true)
                .setCancel((msg) -> {})
                .setEventWaiter(EventWaiterProvider.getInstance())
                .setTimeout(1, TimeUnit.MINUTES)
                .build()
                .display(event.getChannel());
    }

    private void removeAllEntries(User target, CommandEvent event)
    {
        int count = ((AudioHandler) event.getGuild().getAudioManager().getSendingHandler()).getQueue().removeAll(target.getIdLong());
        if (count == 0)
        {
            event.replyWarning("**"+target.getName()+"** doesn't have any songs in the queue!");
        }
        else
        {
            event.replySuccess("Successfully removed `"+count+"` entries from "+FormatUtil.formatUsername(target)+".");
        }
    }

}
