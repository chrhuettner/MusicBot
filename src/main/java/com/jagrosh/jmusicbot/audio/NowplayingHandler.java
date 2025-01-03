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
package com.jagrosh.jmusicbot.audio;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.BotConfig;
import com.jagrosh.jmusicbot.JDAProvider;
import com.jagrosh.jmusicbot.ScheduledExecutorServiceProvider;
import com.jagrosh.jmusicbot.entities.Pair;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class NowplayingHandler
{
    private final Bot bot;
    private final HashMap<Long,Pair<Long,Long>> lastNP; // guild -> channel,message

    private final BotConfig botConfig;

    private final ScheduledExecutorService executorService;

    private static NowplayingHandler nowplayingHandler;

    public static NowplayingHandler getInstance(){
        if(nowplayingHandler == null){
            nowplayingHandler = new NowplayingHandler();
        }
        return nowplayingHandler;
    }
    private NowplayingHandler()
    {
        this.bot = Bot.getInstance();
        this.lastNP = new HashMap<>();
        this.botConfig = BotConfig.getInstance();
        this.executorService = ScheduledExecutorServiceProvider.getInstance();
        init();
    }
    
    public void init()
    {
        if(!botConfig.useNPImages())
            executorService.scheduleWithFixedDelay(this::updateAll, 0, 5, TimeUnit.SECONDS);
    }
    
    public void setLastNPMessage(Message m)
    {
        lastNP.put(m.getGuild().getIdLong(), new Pair<>(m.getTextChannel().getIdLong(), m.getIdLong()));
    }
    
    public void clearLastNPMessage(Guild guild)
    {
        lastNP.remove(guild.getIdLong());
    }
    
    private void updateAll()
    {
        Set<Long> toRemove = new HashSet<>();
        JDA jda = JDAProvider.getInstance();
        for(long guildId: lastNP.keySet())
        {
            Guild guild = jda.getGuildById(guildId);
            if(guild==null)
            {
                toRemove.add(guildId);
                continue;
            }
            Pair<Long,Long> pair = lastNP.get(guildId);
            TextChannel tc = guild.getTextChannelById(pair.getKey());
            if(tc==null)
            {
                toRemove.add(guildId);
                continue;
            }
            AudioHandler handler = (AudioHandler)guild.getAudioManager().getSendingHandler();
            Message msg = handler.getNowPlaying(jda);
            if(msg==null)
            {
                msg = handler.getNoMusicPlaying(jda);
                toRemove.add(guildId);
            }
            try 
            {
                tc.editMessageById(pair.getValue(), msg).queue(m->{}, t -> lastNP.remove(guildId));
            } 
            catch(Exception e) 
            {
                toRemove.add(guildId);
            }
        }
        toRemove.forEach(lastNP::remove);
    }

    // "event"-based methods
    public void onTrackUpdate(AudioTrack track)
    {
        JDA jda = JDAProvider.getInstance();

        // update bot status if applicable
        if(botConfig.getSongInStatus())
        {
            if(track!=null && jda.getGuilds().stream().filter(g -> g.getSelfMember().getVoiceState().inVoiceChannel()).count()<=1)
                jda.getPresence().setActivity(Activity.listening(track.getInfo().title));
            else
                bot.resetGame();
        }
    }
    
    public void onMessageDelete(Guild guild, long messageId)
    {
        Pair<Long,Long> pair = lastNP.get(guild.getIdLong());
        if(pair==null)
            return;
        if(pair.getValue() == messageId)
            lastNP.remove(guild.getIdLong());
    }
}
