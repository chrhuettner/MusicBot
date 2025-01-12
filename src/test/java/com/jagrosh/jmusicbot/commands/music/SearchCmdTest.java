package com.jagrosh.jmusicbot.commands.music;

import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.PlayerManager;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import net.dv8tion.jda.api.entities.Member;


import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchCmdTest {

    private SearchCmd searchCmd;
    private CommandEvent mockEvent;
    private PlayerManager mockPlayerManager;
    private CommandClient mockClient;

    private static BotConfig botConfig;
    private static MockedStatic<BotConfig> botConfigMockedStatic;

    @BeforeAll
    public static void beforeClass() {
        botConfigMockedStatic = mockStatic(BotConfig.class);
        botConfig = mock(BotConfig.class);
        botConfigMockedStatic.when(BotConfig::getInstance).thenReturn(botConfig);
    }

    @AfterAll
    public static void afterClass() {
        botConfigMockedStatic.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        mockPlayerManager = mock(PlayerManager.class);
        mockEvent = mock(CommandEvent.class);
        mockClient = mock(CommandClient.class);

        when(botConfig.getSearching()).thenReturn(":mag:");
        when(botConfig.isTooLong(any())).thenReturn(false);
        when(botConfig.getMaxTime()).thenReturn("300000");

        when(mockEvent.getClient()).thenReturn(mockClient);
        when(mockClient.getWarning()).thenReturn(":warning:");
        when(mockClient.getSuccess()).thenReturn(":white_check_mark:");
        when(mockClient.getError()).thenReturn(":x:");

        searchCmd = new SearchCmd();

        var playerManagerField = SearchCmd.class.getDeclaredField("playerManager");
        playerManagerField.setAccessible(true);
        playerManagerField.set(searchCmd, mockPlayerManager);
    }


    @Test
    void testCommandWithoutArguments() {
        when(mockEvent.getArgs()).thenReturn("");

        searchCmd.doCommand(mockEvent);

        verify(mockEvent).replyError("Please include a query.");
    }

    @Test
    void testCommandWithValidQuery() {
        when(mockEvent.getArgs()).thenReturn("test query");

        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(2);
            success.run();
            return null;
        }).when(mockPlayerManager).loadItemOrdered(any(), anyString(), any());

        searchCmd.doCommand(mockEvent);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(mockEvent).reply(captor.capture(), any());
        String reply = captor.getValue();

        assertTrue(reply.contains(":mag: Searching... `[test query]`"));
    }

    @Test
    void testNoMatches() throws Exception {
        Message mockMessage = mock(Message.class);
        MessageAction mockMessageAction = mock(MessageAction.class);
        when(mockMessage.editMessage(any(CharSequence.class))).thenReturn(mockMessageAction);
        doNothing().when(mockMessageAction).queue();

        Object handler = createResultHandler(searchCmd, mockMessage, mockEvent);

        Method noMatchesMethod = handler.getClass().getDeclaredMethod("noMatches");
        noMatchesMethod.setAccessible(true);
        noMatchesMethod.invoke(handler);

        verify(mockMessage).editMessage(contains("No results found"));
        verify(mockMessageAction).queue();
    }

    @Test
    void testTrackLoaded() throws Exception {
        // Create a new event + guild mocking
        CommandEvent event = mock(CommandEvent.class);
        CommandClient client = mock(CommandClient.class);
        when(event.getClient()).thenReturn(client);
        when(client.getSuccess()).thenReturn(":white_check_mark:");
        when(event.getArgs()).thenReturn("test query");

        Guild mockGuild = mock(Guild.class);
        AudioManager audioManager = mock(AudioManager.class);
        AudioHandler audioHandler = mock(AudioHandler.class);

        when(event.getGuild()).thenReturn(mockGuild);
        when(mockGuild.getAudioManager()).thenReturn(audioManager);
        when(audioManager.getSendingHandler()).thenReturn(audioHandler);

        // track is not too long
        when(botConfig.isTooLong(any())).thenReturn(false);
        when(audioHandler.addTrack(any())).thenReturn(1);

        // Setup the editing message
        Message mockMessage = mock(Message.class);
        MessageAction mockMessageAction = mock(MessageAction.class);
        when(mockMessage.editMessage(any(CharSequence.class))).thenReturn(mockMessageAction);
        doNothing().when(mockMessageAction).queue();

        // Build an AudioTrack
        AudioTrackInfo mockTrackInfo = new AudioTrackInfo("Test Track", "Test Author", 180_000L, "TestTrackId", false, "https://example.com");
        AudioTrack mockTrack = mock(AudioTrack.class);
        when(mockTrack.getInfo()).thenReturn(mockTrackInfo);

        // Create ResultHandler instance
        Object handler = createResultHandler(searchCmd, mockMessage, event);

        // trackLoaded(...) method
        Method trackLoaded = handler.getClass().getDeclaredMethod("trackLoaded", AudioTrack.class);
        trackLoaded.setAccessible(true);
        trackLoaded.invoke(handler, mockTrack);

        verify(mockMessage).editMessage(contains(":white_check_mark: Added **Test Track** (`00:00`)  to the queue at position 2"));
        verify(mockMessageAction).queue();
    }

    @Test
    void testTrackLoaded_tooLong() throws Exception {
        when(botConfig.isTooLong(any())).thenReturn(true); // Force the "too long" branch

        Message mockMessage = mock(Message.class);
        MessageAction mockMessageAction = mock(MessageAction.class);
        when(mockMessage.editMessage(any(CharSequence.class))).thenReturn(mockMessageAction);
        doNothing().when(mockMessageAction).queue();

        Object handler = createResultHandler(searchCmd, mockMessage, mockEvent);

        AudioTrackInfo info = new AudioTrackInfo("Long Track", "Test Author", 999_999L, "LongTrackId", false, "uri");
        AudioTrack mockTrack = mock(AudioTrack.class);
        when(mockTrack.getInfo()).thenReturn(info);

        Method trackLoaded = handler.getClass().getDeclaredMethod("trackLoaded", AudioTrack.class);
        trackLoaded.setAccessible(true);
        trackLoaded.invoke(handler, mockTrack);

        verify(mockMessage).editMessage(contains("is longer than the allowed maximum"));
        verify(mockMessageAction).queue();
    }

    @Test
    void testLoadFailed_commonSeverity() throws Exception {
        // Setup
        Message mockMessage = mock(Message.class);
        MessageAction mockMessageAction = mock(MessageAction.class);
        when(mockMessage.editMessage(any(CharSequence.class))).thenReturn(mockMessageAction);
        doNothing().when(mockMessageAction).queue();

        FriendlyException fe = new FriendlyException("Test Common Error", FriendlyException.Severity.COMMON, null);

        Object handler = createResultHandler(searchCmd, mockMessage, mockEvent);
        Method loadFailed = handler.getClass().getDeclaredMethod("loadFailed", FriendlyException.class);
        loadFailed.setAccessible(true);
        loadFailed.invoke(handler, fe);

        // Verify
        verify(mockMessage).editMessage(contains("Error loading: Test Common Error"));
        verify(mockMessageAction).queue();
    }


    @Test
    void testLoadFailed_nonCommon() throws Exception {
        Message mockMessage = mock(Message.class);
        MessageAction mockMessageAction = mock(MessageAction.class);
        when(mockMessage.editMessage(any(CharSequence.class))).thenReturn(mockMessageAction);
        doNothing().when(mockMessageAction).queue();

        FriendlyException fe = new FriendlyException("Serious Error", FriendlyException.Severity.FAULT, null);

        Object handler = createResultHandler(searchCmd, mockMessage, mockEvent);
        Method loadFailed = handler.getClass().getDeclaredMethod("loadFailed", FriendlyException.class);
        loadFailed.setAccessible(true);
        loadFailed.invoke(handler, fe);

        // Verify
        verify(mockMessage).editMessage(contains("Error loading track."));
        verify(mockMessageAction).queue();
    }

    // --------------------------------------------------------------
    // Utility to create the private ResultHandler instance
    // --------------------------------------------------------------

    private Object createResultHandler(SearchCmd searchCmd, Message message, CommandEvent event) throws Exception {
        Constructor<?> constructor = SearchCmd.class.getDeclaredClasses()[0]
                .getDeclaredConstructor(SearchCmd.class, Message.class, CommandEvent.class);
        constructor.setAccessible(true);
        return constructor.newInstance(searchCmd, message, event);
    }
}
