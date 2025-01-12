package com.jagrosh.jmusicbot.commands.music;

import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.audio.PlayerManager;
import com.jagrosh.jdautilities.command.CommandEvent;
import static org.mockito.Mockito.*;
import net.dv8tion.jda.api.entities.Message;
import org.mockito.MockedStatic;
import com.jagrosh.jdautilities.command.CommandClient;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

import java.lang.reflect.Method;

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
        when(mockEvent.getClient()).thenReturn(mockClient); // Mock CommandClient
        when(mockClient.getWarning()).thenReturn(":warning:");
        when(mockClient.getSuccess()).thenReturn(":white_check_mark:");
        when(mockClient.getError()).thenReturn(":x:");

        searchCmd = new SearchCmd();

        Field playerManagerField = SearchCmd.class.getDeclaredField("playerManager");
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
        Message mockMessage = mock(Message.class);

        doAnswer(invocation -> {
            Runnable success = invocation.getArgument(2);
            success.run();
            return null;
        }).when(mockPlayerManager).loadItemOrdered(any(), anyString(), any());

        searchCmd.doCommand(mockEvent);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockEvent).reply(captor.capture(), any());
        String reply = captor.getValue();

        assert(reply.contains(":mag: Searching... `[test query]`"));
    }

    @Test
    void testNoMatches() throws Exception {
        Message mockMessage = mock(Message.class);
        MessageAction mockMessageAction = mock(MessageAction.class);

        when(mockMessage.editMessage(any(CharSequence.class))).thenReturn(mockMessageAction);

        doNothing().when(mockMessageAction).queue();

        Constructor<?> constructor = SearchCmd.class.getDeclaredClasses()[0].getDeclaredConstructor(SearchCmd.class, Message.class, CommandEvent.class);
        constructor.setAccessible(true);
        Object handler = constructor.newInstance(searchCmd, mockMessage, mockEvent);

        Method noMatchesMethod = handler.getClass().getDeclaredMethod("noMatches");
        noMatchesMethod.setAccessible(true);
        noMatchesMethod.invoke(handler);

        verify(mockMessage).editMessage(contains("No results found"));
        verify(mockMessageAction).queue();
    }


}


