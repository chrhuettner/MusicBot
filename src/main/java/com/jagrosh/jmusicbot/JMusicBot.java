/*
 * Copyright 2016 John Grosh (jagrosh).
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
package com.jagrosh.jmusicbot;

import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.gui.GUI;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class JMusicBot 
{
    public final static Logger LOG = LoggerFactory.getLogger(JMusicBot.class);

    private static final Prompt prompt = Prompt.getInstance();
    private static BotConfig config;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if(args.length > 0)
            switch(args[0].toLowerCase())
            {
                case "generate-config":
                    BotConfig.writeDefaultConfig();
                    return;
                default:
            }
        startBot();
    }

    private static void startBot() {
        config = loadConfig();
        if (config == null) return;

        setLogLevel();

        initializeGUI();

        // Indirectly starts the JDA
        JDAProvider.getInstance();
    }

    private static BotConfig loadConfig() {
        OtherUtil.checkVersion(prompt);
        OtherUtil.checkJavaVersion(prompt);

        BotConfig config = BotConfig.createConfig(prompt);

        if (!config.isValid()) return null;

        LOG.info("Loaded config from " + config.getConfigLocation());
        return config;
    }

    private static void setLogLevel() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .setLevel(Level.toLevel(config.getLogLevel(), Level.INFO));
    }

    private static void initializeGUI() {
        if (!prompt.isNoGUI()) {
            try {
                GUI gui = GUI.getInstance();
                gui.init();
                LOG.info("Loaded config from " + config.getConfigLocation());
            } catch (Exception e) {
                LOG.error("Could not start GUI. If you are running on a server or in a location " +
                        "where you cannot display a window, please run in nogui mode using the -Dnogui=true flag.");
            }
        }
    }

}
