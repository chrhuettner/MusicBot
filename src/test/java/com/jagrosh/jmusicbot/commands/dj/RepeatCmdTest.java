package com.jagrosh.jmusicbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.settings.RepeatMode;
import com.jagrosh.jmusicbot.settings.Settings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.MockedStatic;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class RepeatCmdTest {
    private static RepeatCmd cmd;
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
    public void setUp() {
        cmd = new RepeatCmd();
    }

    @Test
    public void noArgsRepeatModeOff() {
        CommandEvent event = mock(CommandEvent.class);
        Settings settings = mock(Settings.class);
        when(settings.getRepeatMode()).thenReturn(RepeatMode.OFF);
        when(event.getClient()).then(invocation -> {
            CommandClient client = mock(CommandClient.class);
            when(client.getSettingsFor(any())).thenReturn(settings);
            return client;
        });
        when(event.getArgs()).thenReturn("");

        cmd.doCommand(event);

        verify(settings).setRepeatMode(RepeatMode.ALL);
        verify(event).replySuccess(any());
    }

    @ParameterizedTest
    @MethodSource("repeatModes")
    public void noArgsRepeatModeOn(RepeatMode repeatMode) {
        CommandEvent event = mock(CommandEvent.class);
        Settings settings = mock(Settings.class);
        when(settings.getRepeatMode()).thenReturn(repeatMode);
        when(event.getClient()).then(invocation -> {
            CommandClient client = mock(CommandClient.class);
            when(client.getSettingsFor(any())).thenReturn(settings);
            return client;
        });
        when(event.getArgs()).thenReturn("");

        cmd.doCommand(event);

        verify(settings).setRepeatMode(RepeatMode.OFF);
        verify(event).replySuccess(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "off"})
    public void setRepeatModeOff(String arg) {
        CommandEvent event = mock(CommandEvent.class);
        Settings settings = mock(Settings.class);
        when(event.getClient()).then(invocation -> {
            CommandClient client = mock(CommandClient.class);
            when(client.getSettingsFor(any())).thenReturn(settings);
            return client;
        });
        when(event.getArgs()).thenReturn(arg);

        cmd.doCommand(event);

        verify(settings).setRepeatMode(RepeatMode.OFF);
        verify(event).replySuccess(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "on", "all"})
    public void setRepeatModeAll(String arg) {
        CommandEvent event = mock(CommandEvent.class);
        Settings settings = mock(Settings.class);
        when(event.getClient()).then(invocation -> {
            CommandClient client = mock(CommandClient.class);
            when(client.getSettingsFor(any())).thenReturn(settings);
            return client;
        });
        when(event.getArgs()).thenReturn(arg);

        cmd.doCommand(event);

        verify(settings).setRepeatMode(RepeatMode.ALL);
        verify(event).replySuccess(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"single", "one"})
    public void setRepeatModeSingle(String arg) {
        CommandEvent event = mock(CommandEvent.class);
        Settings settings = mock(Settings.class);
        when(event.getClient()).then(invocation -> {
            CommandClient client = mock(CommandClient.class);
            when(client.getSettingsFor(any())).thenReturn(settings);
            return client;
        });
        when(event.getArgs()).thenReturn(arg);

        cmd.doCommand(event);

        verify(settings).setRepeatMode(RepeatMode.SINGLE);
        verify(event).replySuccess(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"kjflsa", "djskladjl", "none"})
    public void invalidArguments(String arg) {
        CommandEvent event = mock(CommandEvent.class);
        Settings settings = mock(Settings.class);
        when(event.getClient()).then(invocation -> {
            CommandClient client = mock(CommandClient.class);
            when(client.getSettingsFor(any())).thenReturn(settings);
            return client;
        });
        when(event.getArgs()).thenReturn(arg);

        cmd.doCommand(event);

        verify(event).replyError(any());
        verify(settings, never()).setRepeatMode(any());
    }

    private static Stream<Arguments> repeatModes() {
        return Stream.of(
                Arguments.of(RepeatMode.ALL),
                Arguments.of(RepeatMode.SINGLE)
        );
    }
}
