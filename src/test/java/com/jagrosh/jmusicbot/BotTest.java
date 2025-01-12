package com.jagrosh.jmusicbot;

import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.gui.GUI;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.Presence;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BotTest {
    private static MockedStatic<ScheduledExecutorServiceProvider> executorStaticMock;
    private static ScheduledExecutorService executor;
    private static MockedStatic<BotConfig> configStaticMock;
    private static MockedStatic<SettingsManager> settingsManagerStaticMock;
    private static MockedStatic<JDAProvider> jdaProviderMockedStatic;
    private static JDA jdaProvider;




    @BeforeAll
    public static void beforeClass() {
        executorStaticMock = mockStatic(ScheduledExecutorServiceProvider.class);
        executor = mock(ScheduledExecutorService.class);
        executorStaticMock.when(ScheduledExecutorServiceProvider::getInstance).thenReturn(executor);
        configStaticMock = mockStatic(BotConfig.class);
        settingsManagerStaticMock = mockStatic(SettingsManager.class);
        jdaProviderMockedStatic  = mockStatic(JDAProvider.class);
        jdaProvider = mock(JDA.class);
    }

    @BeforeEach
    public void resetBot() {
        Bot.resetBot();
        reset(jdaProvider);
    }

    @AfterEach
    public void tearDown() {
        jdaProviderMockedStatic.reset();
    }

    @AfterAll
    public static void afterClass() {
        executorStaticMock.close();
        configStaticMock.close();
        settingsManagerStaticMock.close();
        jdaProviderMockedStatic.close();
    }

    @Test
    public void createBotTest() {
        assertDoesNotThrow(() -> Bot.getInstance());
        assertDoesNotThrow(() -> Bot.getInstance());
    }

    private static void setJdaProvider() {
        jdaProviderMockedStatic.when(JDAProvider::getInstance).thenReturn(jdaProvider);
    }

    @Test
    public void closeAudioConnectionTest() {
        when(jdaProvider.getGuildById(1)).thenReturn(mock(Guild.class));
        setJdaProvider();

        Bot bot = Bot.getInstance();
        bot.closeAudioConnection(1);

        verify(executor).submit(any(Runnable.class));
    }

    @Test
    public void closeAudioConnectionNoGuildTest() {
            when(jdaProvider.getGuildById(1)).thenReturn(null);
            setJdaProvider();

            Bot bot = Bot.getInstance();
            bot.closeAudioConnection(1);

            verify(executor, never()).submit(any(Runnable.class));
    }

    @Test
    public void resetGameTest() {
            Presence presence = mock(Presence.class);
            when(jdaProvider.getPresence()).thenReturn(presence);
            setJdaProvider();

            Activity activity = mock(Activity.class);
            when(activity.getName()).thenReturn("SomeGame");
            BotConfig config = mock(BotConfig.class);
            configStaticMock.when(BotConfig::getInstance).thenReturn(config);
            when(config.getGame()).thenReturn(activity);


            Bot bot = Bot.getInstance();
            bot.resetGame();

            verify(presence).setActivity(activity);
    }

    @Test
    public void resetGameNonExistentNameEqNoneTest() {
            Presence presence = mock(Presence.class);
            when(jdaProvider.getPresence()).thenReturn(presence);
            setJdaProvider();

            Activity activity = mock(Activity.class);
            when(activity.getName()).thenReturn("none");
            BotConfig config = mock(BotConfig.class);
            configStaticMock.when(BotConfig::getInstance).thenReturn(config);
            when(config.getGame()).thenReturn(activity);

            Bot bot = Bot.getInstance();
            bot.resetGame();

            verify(presence, never()).setActivity(any());
    }

    @Test
    public void resetGameEqNullTest() {
            Presence presence = mock(Presence.class);
            when(jdaProvider.getPresence()).thenReturn(presence);
            setJdaProvider();

            BotConfig config = mock(BotConfig.class);
            configStaticMock.when(BotConfig::getInstance).thenReturn(config);
            when(config.getGame()).thenReturn(null);

            Bot bot = Bot.getInstance();
            bot.resetGame();

            verify(presence, never()).setActivity(any());
    }

    @Test
    public void callShutdownWithShutdownEqTrue() {
        Bot bot = Bot.getInstance();
        setJdaProvider();

        bot.setShutdownFlag(true);
        bot.shutdown();

        verify(executor, never()).shutdownNow();
        verifyNoInteractions(jdaProvider);
    }

    @Test
    public void callShutdownJdaStatusEqShuttingDownGuiHasNoInstance() throws Exception {
        when(jdaProvider.getStatus()).thenReturn(JDA.Status.SHUTTING_DOWN);
        setJdaProvider();
        try (MockedStatic<GUI> guiStaticMock = mockStatic(GUI.class)) {
            guiStaticMock.when(GUI::hasInstance).thenReturn(false);

            Bot bot = Bot.getInstance();
            int statusCode = catchSystemExit(bot::shutdown);

            assertEquals(0, statusCode);
            guiStaticMock.verify(GUI::getInstance, never());
            verify(jdaProvider).getStatus();
            verifyNoMoreInteractions(jdaProvider);
        }
    }

    @Test
    public void callShutdownJdaStatusNeqShuttingDownGuiHasNoInstance() throws Exception {

        try (MockedStatic<GUI> guiStaticMock = mockStatic(GUI.class)) {
            guiStaticMock.when(GUI::hasInstance).thenReturn(false);
            Guild guild = mock(Guild.class);
            AudioManager audioManager = mock(AudioManager.class);
            when(guild.getAudioManager()).thenReturn(audioManager);
            AudioHandler handler = mock(AudioHandler.class);
            when(audioManager.getSendingHandler()).thenReturn(handler);
            AudioPlayer player = mock(AudioPlayer.class);
            when(handler.getPlayer()).thenReturn(player);
            when(jdaProvider.getGuilds()).thenReturn(List.of(guild));
            when(jdaProvider.getStatus()).thenReturn(JDA.Status.CONNECTED);
            setJdaProvider();

            Bot bot = Bot.getInstance();
            int statusCode = catchSystemExit(bot::shutdown);

            assertEquals(0, statusCode);
            verify(handler).stopAndClear();
            verify(player).destroy();
        }
    }

    @Test
    public void callShutdownJdaStatusEqShuttingDownGuiHasInstance() throws Exception {
        when(jdaProvider.getStatus()).thenReturn(JDA.Status.SHUTTING_DOWN);
        setJdaProvider();
        try (MockedStatic<GUI> guiStaticMock = mockStatic(GUI.class)) {
            GUI gui = mock(GUI.class);
            guiStaticMock.when(GUI::hasInstance).thenReturn(true);
            guiStaticMock.when(GUI::getInstance).thenReturn(gui);

            Bot bot = Bot.getInstance();
            int statusCode = catchSystemExit(bot::shutdown);

            assertEquals(0, statusCode);
            guiStaticMock.verify(GUI::getInstance);
            verify(gui).dispose();
            verify(jdaProvider).getStatus();
            verifyNoMoreInteractions(jdaProvider);
        }
    }

}
