package com.jagrosh.jmusicbot.commands.music;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.jagrosh.jmusicbot.BotConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;

public class SCSearchCmdTest {

    private SCSearchCmd scSearchCmd;
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
        scSearchCmd = new SCSearchCmd();
    }

    @Test
    void testConstructor() {
        assertNotNull(scSearchCmd);

        try {
            Field commandNameField = SCSearchCmd.class.getDeclaredField("COMMAND_NAME");
            commandNameField.setAccessible(true);
            String commandName = (String) commandNameField.get(null);
            assertEquals("scsearch", commandName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access or retrieve COMMAND_NAME field");
        }

        assertEquals("scsearch:", scSearchCmd.searchPrefix);
        assertEquals("searches Soundcloud for a provided query", scSearchCmd.getHelp());
    }
}
