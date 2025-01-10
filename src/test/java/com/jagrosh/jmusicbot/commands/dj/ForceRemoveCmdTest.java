package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.queue.AbstractQueue;
import com.jagrosh.jmusicbot.queue.FairQueue;
import com.jagrosh.jmusicbot.queue.LinearQueue;
import com.jagrosh.jmusicbot.queue.Queueable;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class ForceRemoveCmdTest {
    private static ForceRemoveCmd cmd;
    private static BotConfig botConfig;

    @BeforeClass
    public static void beforeClass() {
        MockedStatic<BotConfig> botConfigMockedStatic = mockStatic(BotConfig.class);
        botConfig = mock(BotConfig.class);
        botConfigMockedStatic.when(BotConfig::getInstance).thenReturn(botConfig);
    }

    @Before
    public void resetCmd() {
        cmd = new ForceRemoveCmd();
    }

    @Test
    public void noCommandArgs() {
        CommandEvent event = mock(CommandEvent.class);
        when(event.getArgs()).thenReturn("");

        cmd.doCommand(event);

        verify(event).replyError(anyString());
    }

    @Test
    public void noMusicInQueue() {
        CommandEvent event = mock(CommandEvent.class);
        when(event.getArgs()).thenReturn("someString");
        when(event.getGuild()).then(invocationOnMock -> {
           Guild guild = mock(Guild.class);
           AudioManager audioManager = mock(AudioManager.class);
           AudioHandler handler = mock(AudioHandler.class);
           when(handler.getQueue()).thenReturn(new FairQueue<>(null));
           when(audioManager.getSendingHandler()).thenReturn(handler);
           when(guild.getAudioManager()).thenReturn(audioManager);
           return guild;
        });

        cmd.doCommand(event);

        verify(event).replyError(anyString());
    }

    @Test
    public void noMembersFound() {
        CommandEvent event = mock(CommandEvent.class);
        when(event.getArgs()).thenReturn("someString");
        when(event.getGuild()).then(invocationOnMock -> {
            Guild guild = mock(Guild.class);
            AudioManager audioManager = mock(AudioManager.class);
            AudioHandler handler = mock(AudioHandler.class);
            FairQueue<QueuedTrack> queue = new FairQueue<>(null);
            queue.add(new QueuedTrack(mock(AudioTrack.class), null));
            when(handler.getQueue()).thenReturn(queue);
            when(audioManager.getSendingHandler()).thenReturn(handler);
            when(guild.getAudioManager()).thenReturn(audioManager);
            return guild;
        });
        mockStatic(FinderUtil.class).when(() -> FinderUtil.findMembers(any(), any())).thenReturn(Collections.emptyList());

        cmd.doCommand(event);

        verify(event).replyError(anyString());
    }
}
