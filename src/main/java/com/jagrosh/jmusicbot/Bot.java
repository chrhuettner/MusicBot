/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
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

import java.util.concurrent.ScheduledExecutorService;

import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.gui.GUI;

import java.util.Objects;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class Bot
{
    private boolean shuttingDown = false;
    private final ScheduledExecutorService executorService;
    private static Bot bot;

    public static Bot getInstance(){
        if(bot == null){
            bot = new Bot();
        }
        return bot;
    }
    private Bot() {
        this.executorService = ScheduledExecutorServiceProvider.getInstance();
    }
    
    public void closeAudioConnection(long guildId)
    {
        JDA jda = JDAProvider.getInstance();
        Guild guild = jda.getGuildById(guildId);
        if(guild!=null)
            executorService.submit(() -> guild.getAudioManager().closeAudioConnection());
    }
    
    public void resetGame()
    {
        JDA jda = JDAProvider.getInstance();
        BotConfig config = BotConfig.getInstance();
        Activity game = config.getGame()==null || config.getGame().getName().equalsIgnoreCase("none") ? null : config.getGame();
        if(!Objects.equals(jda.getPresence().getActivity(), game))
            jda.getPresence().setActivity(game);
    }

    public void shutdown()
    {
        JDA jda = JDAProvider.getInstance();
        if(shuttingDown)
            return;
        shuttingDown = true;
        executorService.shutdownNow();
        if(jda.getStatus()!=JDA.Status.SHUTTING_DOWN)
        {
            jda.getGuilds().stream().forEach(g -> 
            {
                g.getAudioManager().closeAudioConnection();
                AudioHandler ah = (AudioHandler)g.getAudioManager().getSendingHandler();
                if(ah!=null)
                {
                    ah.stopAndClear();
                    ah.getPlayer().destroy();
                }
            });
            jda.shutdown();
        }
        if(GUI.hasInstance())
            GUI.getInstance().dispose();
        System.exit(0);
    }
}
