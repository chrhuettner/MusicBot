package com.jagrosh.jmusicbot;

import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotConfigTest {

    private static Prompt mockPrompt;
    private static Config mockConfig;
    private static Config mockAliasesConfig;


    @BeforeEach
    void resetSingleton() {
        try {
            Field f = BotConfig.class.getDeclaredField("botConfig");
            f.setAccessible(true);
            f.set(null, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not reset botConfig singleton: " + e.getMessage());
        }
    }

    @BeforeAll
    public static void setUp() {
        mockPrompt = mock(Prompt.class);
        mockConfig = mock(Config.class);
        mockAliasesConfig = mock(Config.class);
    }

    @Test
    void testCreateConfig_validConfig() {
        when(mockConfig.getString("token")).thenReturn("new_token");
        when(mockConfig.getLong("owner")).thenReturn(12345L);
        when(mockConfig.getString("prefix")).thenReturn("!");
        when(mockConfig.getString("altprefix")).thenReturn("?");
        when(mockConfig.getString("help")).thenReturn("help");
        when(mockConfig.getString("game")).thenReturn("test game");
        when(mockConfig.getString("status")).thenReturn("ONLINE");
        when(mockConfig.getBoolean("stayinchannel")).thenReturn(true);
        when(mockConfig.getBoolean("songinstatus")).thenReturn(true);
        when(mockConfig.getLong("maxtime")).thenReturn(300L);

        when(mockConfig.getString("success")).thenReturn(":white_check_mark:");
        when(mockConfig.getString("warning")).thenReturn(":warning:");
        when(mockConfig.getString("error")).thenReturn(":x:");
        when(mockConfig.getString("loading")).thenReturn(":arrows_counterclockwise:");
        when(mockConfig.getString("searching")).thenReturn(":mag:");

        when(mockConfig.getBoolean("npimages")).thenReturn(false);
        when(mockConfig.getBoolean("updatealerts")).thenReturn(true);
        when(mockConfig.getString("loglevel")).thenReturn("INFO");
        when(mockConfig.getBoolean("eval")).thenReturn(false);
        when(mockConfig.getString("evalengine")).thenReturn("nashorn");
        when(mockConfig.getInt("maxytplaylistpages")).thenReturn(5);
        when(mockConfig.getLong("alonetimeuntilstop")).thenReturn(600L);
        when(mockConfig.getString("playlistsfolder")).thenReturn("playlists");
        when(mockConfig.getDouble("skipratio")).thenReturn(0.55);

        Config mockAliasesConfig = mock(Config.class);
        Config mockTransformsConfig = mock(Config.class);
        when(mockConfig.getConfig("aliases")).thenReturn(mockAliasesConfig);
        when(mockConfig.getConfig("transforms")).thenReturn(mockTransformsConfig);

        try (MockedStatic<ConfigFactory> mockedFactory = mockStatic(ConfigFactory.class)) {
            mockedFactory.when(ConfigFactory::load).thenReturn(mockConfig);

            BotConfig botConfig = BotConfig.createConfig(mockPrompt);

            assertNotNull(botConfig);
            assertEquals("new_token", botConfig.getToken());
            assertEquals(12345L, botConfig.getOwnerId());
            assertEquals("!", botConfig.getPrefix());
            assertEquals("test game", botConfig.getGame().getName());
            assertEquals(OnlineStatus.ONLINE, botConfig.getStatus());
            //...
        }
    }

    @Test
    void testCreateConfig_missingToken() {
        when(mockConfig.getString("token")).thenReturn("");
        when(mockPrompt.prompt(anyString()))
                .thenReturn("new_token")
                .thenReturn("new_token");

        try (MockedStatic<ConfigFactory> mockedCF = mockStatic(ConfigFactory.class)) {
            mockedCF.when(ConfigFactory::load).thenReturn(mockConfig);

            BotConfig botConfig = BotConfig.createConfig(mockPrompt);
            assertEquals("new_token", botConfig.getToken());
        }

        verify(mockPrompt, atLeastOnce()).prompt(contains("Please provide a bot token"));
    }

    @Test
    void testCreateConfig_invalidOwner() {
        when(mockConfig.getLong("owner")).thenReturn(0L);
        when(mockPrompt.prompt(anyString()))
                .thenReturn("54321")
                .thenReturn("54321");

        try (MockedStatic<ConfigFactory> mockedFactory = mockStatic(ConfigFactory.class)) {
            mockedFactory.when(ConfigFactory::load).thenReturn(mockConfig);

            BotConfig botConfig = BotConfig.createConfig(mockPrompt);
            assertEquals(54321L, botConfig.getOwnerId());
        }

        verify(mockPrompt, atLeastOnce()).prompt(contains("Owner ID was missing"));
    }

    @Test
    void testWriteConfig() {

        when(mockConfig.getString("token")).thenReturn("valid-token");
        when(mockConfig.getLong("owner")).thenReturn(12345L);

        when(mockConfig.getString("prefix")).thenReturn("!");
        when(mockConfig.getString("success")).thenReturn(":white_check_mark:");
        when(mockConfig.getString("warning")).thenReturn(":warning:");
        when(mockConfig.getString("error")).thenReturn(":x:");
        when(mockConfig.getString("loading")).thenReturn(":arrows_counterclockwise:");
        when(mockConfig.getString("searching")).thenReturn(":mag:");
        when(mockConfig.getString("help")).thenReturn("help");
        when(mockConfig.getString("game")).thenReturn("Music");
        when(mockConfig.getString("status")).thenReturn("ONLINE");
        when(mockConfig.getBoolean("stayinchannel")).thenReturn(true);
        when(mockConfig.getBoolean("songinstatus")).thenReturn(false);
        when(mockConfig.getLong("maxtime")).thenReturn(600L);
        when(mockConfig.getInt("maxytplaylistpages")).thenReturn(5);
        when(mockConfig.getLong("alonetimeuntilstop")).thenReturn(300L);
        when(mockConfig.getString("playlistsfolder")).thenReturn("playlists");
        when(mockConfig.getString("loglevel")).thenReturn("INFO");
        when(mockConfig.getDouble("skipratio")).thenReturn(0.5);
        Config mockAliasesConfig = mock(Config.class);
        Config mockTransformsConfig = mock(Config.class);
        when(mockConfig.getConfig("aliases")).thenReturn(mockAliasesConfig);
        when(mockConfig.getConfig("transforms")).thenReturn(mockTransformsConfig);

        try (MockedStatic<ConfigFactory> mockedFactory = mockStatic(ConfigFactory.class)) {
            mockedFactory.when(ConfigFactory::load).thenReturn(mockConfig);

            BotConfig bc = BotConfig.createConfig(mockPrompt);
            assertTrue(bc.isValid());

            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.write(any(Path.class), any(byte[].class)))
                        .thenThrow(new IOException("Test IO Error"));

                Method writeToFileMethod = BotConfig.class.getDeclaredMethod("writeToFile");
                writeToFileMethod.setAccessible(true);

                writeToFileMethod.invoke(bc);

                verify(mockPrompt).alert(eq(Prompt.Level.WARNING), eq("Config"),
                        contains("Failed to write new config options to config.txt:"));
            }
            catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                fail("Reflection call to writeToFile() failed: " + e.getMessage());
            }
        }
    }

    @Test
    void testIsTooLong() throws NoSuchFieldException, IllegalAccessException {
        BotConfig bc = BotConfig.createConfig(mockPrompt);

        Field f = BotConfig.class.getDeclaredField("maxSeconds");
        f.setAccessible(true);
        f.set(bc, 3L);

        AudioTrack track = mock(AudioTrack.class);
        when(track.getDuration()).thenReturn(5000L);

        assertTrue(bc.isTooLong(track));
    }

    @Test
    void testIsNotTooLong() throws NoSuchFieldException, IllegalAccessException {
        BotConfig bc = BotConfig.createConfig(mockPrompt);

        Field f = BotConfig.class.getDeclaredField("maxSeconds");
        f.setAccessible(true);
        f.set(bc, 3L);

        AudioTrack track = mock(AudioTrack.class);
        when(track.getDuration()).thenReturn(2000L);

        assertFalse(bc.isTooLong(track));
    }

    @Test
    void testGetConfigLocation() {
        BotConfig bc = BotConfig.createConfig(mockPrompt);
        assertNotNull(bc.getConfigLocation());
    }

    @Test
    void testGetAliases() {
        Config mockAliases = mock(Config.class);
        when(mockAliases.getStringList("someCommand")).thenReturn(java.util.List.of("alias1", "alias2"));
        when(mockConfig.getConfig("aliases")).thenReturn(mockAliases);

        when(mockConfig.getString("token")).thenReturn("tokenX");
        when(mockConfig.getLong("owner")).thenReturn(123L);

        try (MockedStatic<ConfigFactory> cf = mockStatic(ConfigFactory.class)) {
            cf.when(ConfigFactory::load).thenReturn(mockConfig);

            BotConfig bc = BotConfig.createConfig(mockPrompt);

            try {
                Field aliasesField = BotConfig.class.getDeclaredField("aliases");
                aliasesField.setAccessible(true);
                aliasesField.set(bc, mockAliases);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fail("Failed to set aliases field via reflection");
            }

            String[] arr = bc.getAliases("someCommand");
            assertEquals(2, arr.length);
            assertEquals("alias1", arr[0]);
            assertEquals("alias2", arr[1]);
        }
    }

    @Test
    void testGetInstance_uninitialized() {
        assertThrows(RuntimeException.class, BotConfig::getInstance,
                "Should throw if BotConfig is not initialized yet");
    }

    @Test
    void testGetInstance_initialized() {
        when(mockConfig.getString("token")).thenReturn("tkn");
        when(mockConfig.getLong("owner")).thenReturn(456L);

        when(mockConfig.getString("success")).thenReturn(":check:");
        when(mockConfig.getString("warning")).thenReturn(":warn:");
        when(mockConfig.getString("error")).thenReturn(":x:");
        when(mockConfig.getString("loading")).thenReturn(":load:");
        when(mockConfig.getString("searching")).thenReturn(":search:");
        when(mockConfig.getString("prefix")).thenReturn("!");
        when(mockConfig.getString("altprefix")).thenReturn("??");
        when(mockConfig.getString("help")).thenReturn("help");
        when(mockConfig.getString("game")).thenReturn("none");
        when(mockConfig.getString("status")).thenReturn("ONLINE");
        when(mockConfig.getBoolean("stayinchannel")).thenReturn(false);
        when(mockConfig.getBoolean("songinstatus")).thenReturn(false);
        when(mockConfig.getBoolean("npimages")).thenReturn(false);
        when(mockConfig.getBoolean("updatealerts")).thenReturn(false);
        when(mockConfig.getBoolean("eval")).thenReturn(false);
        when(mockConfig.getString("evalengine")).thenReturn("nashorn");
        when(mockConfig.getLong("maxtime")).thenReturn(120L);
        when(mockConfig.getInt("maxytplaylistpages")).thenReturn(3);
        when(mockConfig.getLong("alonetimeuntilstop")).thenReturn(600L);
        when(mockConfig.getDouble("skipratio")).thenReturn(0.5);
        when(mockConfig.getString("playlistsfolder")).thenReturn("playlists");
        when(mockConfig.getString("loglevel")).thenReturn("INFO");

        Config mAliases = mock(Config.class);
        Config mTransforms = mock(Config.class);
        when(mockConfig.getConfig("aliases")).thenReturn(mAliases);
        when(mockConfig.getConfig("transforms")).thenReturn(mTransforms);

        try (MockedStatic<ConfigFactory> cf = mockStatic(ConfigFactory.class)) {
            cf.when(ConfigFactory::load).thenReturn(mockConfig);
            BotConfig created = BotConfig.createConfig(mockPrompt);
            assertNotNull(created);

            BotConfig instance = BotConfig.getInstance();
            assertSame(created, instance, "getInstance() should return the same object");
        }
    }


    @Test
    void testGetAltPrefix_none() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "altprefix", "NONE");

        assertNull(bc.getAltPrefix(), "Expected null if altprefix=NONE");
    }

    @Test
    void testGetAltPrefix_value() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "altprefix", "?");
        assertEquals("?", bc.getAltPrefix());
    }

    @Test
    void testGetSkipRatio() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "skipratio", 0.75);
        assertEquals(0.75, bc.getSkipRatio(), 1e-9);
    }

    @Test
    void testGetSuccess_viaReflection() throws Exception {
        BotConfig bc = minimalValidBotConfig();

        Map<String,String> mockEmojis = new HashMap<>();
        mockEmojis.put("success", ":check:");

        Field emojiMapField = BotConfig.class.getDeclaredField("emojiMap");
        emojiMapField.setAccessible(true);
        emojiMapField.set(bc, mockEmojis);

        assertEquals(":check:", bc.getSuccess());
    }

    @Test
    void testGetWarning() throws Exception {
        BotConfig bc = minimalValidBotConfig();

        Map<String,String> mockEmojis = new HashMap<>();
        mockEmojis.put("warning", ":warn:");

        Field emojiMapField = BotConfig.class.getDeclaredField("emojiMap");
        emojiMapField.setAccessible(true);
        emojiMapField.set(bc, mockEmojis);

        assertEquals(":warn:", bc.getWarning());
    }

    @Test
    void testGetError() throws Exception {
        BotConfig bc = minimalValidBotConfig();

        Map<String,String> mockEmojis = new HashMap<>();
        mockEmojis.put("error", ":x:");

        Field emojiMapField = BotConfig.class.getDeclaredField("emojiMap");
        emojiMapField.setAccessible(true);
        emojiMapField.set(bc, mockEmojis);

        assertEquals(":x:", bc.getError());
    }

    @Test
    void testGetLoading() throws Exception {
        BotConfig bc = minimalValidBotConfig();

        Map<String,String> mockEmojis = new HashMap<>();
        mockEmojis.put("loading", ":load:");

        Field emojiMapField = BotConfig.class.getDeclaredField("emojiMap");
        emojiMapField.setAccessible(true);
        emojiMapField.set(bc, mockEmojis);

        assertEquals(":load:", bc.getLoading());
    }

    @Test
    void testGetSearching() throws Exception {
        BotConfig bc = minimalValidBotConfig();

        Map<String,String> mockEmojis = new HashMap<>();
        mockEmojis.put("searching", ":search:");

        Field emojiMapField = BotConfig.class.getDeclaredField("emojiMap");
        emojiMapField.setAccessible(true);
        emojiMapField.set(bc, mockEmojis);

        assertEquals(":search:", bc.getSearching());
    }

    @Test
    void testIsGameNone_true() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "game", Activity.playing("none"));
        assertTrue(bc.isGameNone());
    }

    @Test
    void testIsGameNone_false() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "game", Activity.playing("Music"));
        assertFalse(bc.isGameNone());
    }

    @Test
    void testGetHelp() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "helpWord", "helpme");
        assertEquals("helpme", bc.getHelp());
    }

    @Test
    void testGetStay() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "stayInChannel", true);
        assertTrue(bc.getStay());
    }

    @Test
    void testGetSongInStatus() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "songInGame", true);
        assertTrue(bc.getSongInStatus());
    }

    @Test
    void testGetPlaylistsFolder() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "playlistsFolder", "myPl");
        assertEquals("myPl", bc.getPlaylistsFolder());
    }

    @Test
    void testGetDBots() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "dbots", true);
        assertTrue(bc.getDBots());
    }

    @Test
    void testUseUpdateAlerts() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "updatealerts", true);
        assertTrue(bc.useUpdateAlerts());
    }

    @Test
    void testGetLogLevel() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "logLevel", "DEBUG");
        assertEquals("DEBUG", bc.getLogLevel());
    }

    @Test
    void testUseEval() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "useEval", true);
        assertTrue(bc.useEval());
    }

    @Test
    void testGetEvalEngine() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "evalEngine", "NashornX");
        assertEquals("NashornX", bc.getEvalEngine());
    }

    @Test
    void testUseNPImages() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "npImages", true);
        assertTrue(bc.useNPImages());
    }

    @Test
    void testGetMaxSeconds() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "maxSeconds", 999L);
        assertEquals(999L, bc.getMaxSeconds());
    }

    @Test
    void testGetMaxYTPlaylistPages() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "maxYTPlaylistPages", 10);
        assertEquals(10, bc.getMaxYTPlaylistPages());
    }

    @Test
    void testGetMaxTime() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "maxSeconds", 300L);
        assertEquals("05:00", bc.getMaxTime());
    }

    @Test
    void testGetAloneTimeUntilStop() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        setPrivateField(bc, "aloneTimeUntilStop", 1234L);
        assertEquals(1234L, bc.getAloneTimeUntilStop());
    }

    @Test
    void testGetTransforms() throws Exception {
        BotConfig bc = minimalValidBotConfig();
        Config mockTransforms = mock(Config.class);
        setPrivateField(bc, "transforms", mockTransforms);
        assertSame(mockTransforms, bc.getTransforms());
    }

    @Test
    void testWriteDefaultConfig_mocked() {
        try (MockedConstruction<Prompt> promptConstruction = mockConstruction(Prompt.class)) {
            try (MockedStatic<Files> filesStaticMock = mockStatic(Files.class)) {
                filesStaticMock.when(() -> Files.write(any(Path.class), any(byte[].class)))
                        .thenAnswer(inv -> null);
                BotConfig.writeDefaultConfig();

                assertFalse(promptConstruction.constructed().isEmpty());

                Prompt promptCreated = promptConstruction.constructed().get(0);

                verify(promptCreated).alert(
                        eq(Prompt.Level.INFO),
                        eq("JMusicBot Config"),
                        contains("Generating default config file")
                );
                verify(promptCreated).alert(
                        eq(Prompt.Level.INFO),
                        eq("JMusicBot Config"),
                        contains("Writing default config file to")
                );
            }
        }
    }


    private BotConfig minimalValidBotConfig() {
        when(mockConfig.getString("token")).thenReturn("token_ok");
        when(mockConfig.getLong("owner")).thenReturn(123L);

        when(mockConfig.getString("success")).thenReturn(":check:");
        when(mockConfig.getString("warning")).thenReturn(":warn:");
        when(mockConfig.getString("error")).thenReturn(":x:");
        when(mockConfig.getString("loading")).thenReturn(":load:");
        when(mockConfig.getString("searching")).thenReturn(":search:");
        when(mockConfig.getString("prefix")).thenReturn("!");
        when(mockConfig.getString("altprefix")).thenReturn("??");
        when(mockConfig.getString("help")).thenReturn("help");
        when(mockConfig.getString("game")).thenReturn("Music");
        when(mockConfig.getString("status")).thenReturn("ONLINE");
        when(mockConfig.getBoolean("stayinchannel")).thenReturn(false);
        when(mockConfig.getBoolean("songinstatus")).thenReturn(false);
        when(mockConfig.getBoolean("npimages")).thenReturn(false);
        when(mockConfig.getBoolean("updatealerts")).thenReturn(false);
        when(mockConfig.getBoolean("eval")).thenReturn(false);
        when(mockConfig.getString("evalengine")).thenReturn("nashorn");
        when(mockConfig.getLong("maxtime")).thenReturn(120L);
        when(mockConfig.getInt("maxytplaylistpages")).thenReturn(3);
        when(mockConfig.getLong("alonetimeuntilstop")).thenReturn(600L);
        when(mockConfig.getDouble("skipratio")).thenReturn(0.5);
        when(mockConfig.getString("playlistsfolder")).thenReturn("playlists");
        when(mockConfig.getString("loglevel")).thenReturn("INFO");

        Config mockAliases = mock(Config.class);
        Config mockTransforms = mock(Config.class);
        when(mockConfig.getConfig("aliases")).thenReturn(mockAliases);
        when(mockConfig.getConfig("transforms")).thenReturn(mockTransforms);

        try (MockedStatic<ConfigFactory> cf = mockStatic(ConfigFactory.class)) {
            cf.when(ConfigFactory::load).thenReturn(mockConfig);
            return BotConfig.createConfig(mockPrompt);
        }
    }

    private void setPrivateField(BotConfig cfg, String fieldName, Object value) throws Exception {
        Field f = BotConfig.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(cfg, value);
    }
}
