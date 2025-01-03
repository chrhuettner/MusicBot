/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.owner;

import java.io.IOException;
import java.util.List;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.commands.OwnerCommand;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistCmd extends OwnerCommand 
{
    private static final String COMMAND_NAME = "playlist";

    private final PlaylistLoader playlistLoader;

    public PlaylistCmd()
    {
        super(COMMAND_NAME);
        this.guildOnly = false;
        this.arguments = "<append|delete|make|setdefault>";
        this.help = "playlist management";
        this.children = new OwnerCommand[]{
            new ListCmd(),
            new AppendlistCmd(),
            new DeletelistCmd(),
            new MakelistCmd(),
            new DefaultlistCmd()
        };
        this.playlistLoader = PlaylistLoader.getInstance();
    }

    @Override
    public void execute(CommandEvent event) 
    {
        StringBuilder builder = new StringBuilder(event.getClient().getWarning()+" Playlist Management Commands:\n");
        for(Command cmd: this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments()==null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }


    
    public class MakelistCmd extends OwnerCommand 
    {
        private static final String COMMAND_NAME = "make";

        public MakelistCmd()
        {
            super(COMMAND_NAME);
            this.aliases = new String[]{"create"};
            this.help = "makes a new playlist";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            pname = pname.replaceAll("[*?|\\/\":<>]", "");
            if(pname == null || pname.isEmpty()) 
            {
                event.replyError("Please provide a name for the playlist!");
            } 
            else if(playlistLoader.getPlaylist(pname) == null)
            {
                try
                {
                    playlistLoader.createPlaylist(pname);
                    event.reply(event.getClient().getSuccess()+" Successfully created playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to create the playlist: "+e.getLocalizedMessage());
                }
            }
            else
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` already exists!");
        }

    }
    
    public class DeletelistCmd extends OwnerCommand 
    {
        private static final String COMMAND_NAME = "delete";

        public DeletelistCmd()
        {
            super(COMMAND_NAME);
            this.aliases = new String[]{"remove"};
            this.help = "deletes an existing playlist";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if(playlistLoader.getPlaylist(pname)==null)
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` doesn't exist!");
            else
            {
                try
                {
                    playlistLoader.deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess()+" Successfully deleted playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to delete the playlist: "+e.getLocalizedMessage());
                }
            }
        }

    }
    
    public class AppendlistCmd extends OwnerCommand 
    {
        private static final String COMMAND_NAME = "append";

        public AppendlistCmd()
        {
            super(COMMAND_NAME);
            this.aliases = new String[]{"add"};
            this.help = "appends songs to an existing playlist";
            this.arguments = "<name> <URL> | <URL> | ...";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            String[] parts = event.getArgs().split("\\s+", 2);
            if(parts.length<2)
            {
                event.reply(event.getClient().getError()+" Please include a playlist name and URLs to add!");
                return;
            }
            String pname = parts[0];
            Playlist playlist = playlistLoader.getPlaylist(pname);
            if(playlist==null)
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` doesn't exist!");
            else
            {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = parts[1].split("\\|");
                for(String url: urls)
                {
                    String u = url.trim();
                    if(u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length()-1);
                    builder.append("\r\n").append(u);
                }
                try
                {
                    playlistLoader.writePlaylist(pname, builder.toString());
                    event.reply(event.getClient().getSuccess()+" Successfully added "+urls.length+" items to playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to append to the playlist: "+e.getLocalizedMessage());
                }
            }
        }

    }
    
    public class DefaultlistCmd extends AutoplaylistCmd 
    {
        private static final String COMMAND_NAME = "setdefault";

        public DefaultlistCmd()
        {
            super(COMMAND_NAME);
            this.aliases = new String[]{"default"};
            this.arguments = "<playlistname|NONE>";
            this.guildOnly = true;
        }

    }
    
    public class ListCmd extends OwnerCommand 
    {
        private static final String COMMAND_NAME = "all";

        public ListCmd()
        {
            super(COMMAND_NAME);
            this.aliases = new String[]{"available","list"};
            this.help = "lists all available playlists";
            this.guildOnly = true;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            if(!playlistLoader.folderExists())
                playlistLoader.createFolder();
            if(!playlistLoader.folderExists())
            {
                event.reply(event.getClient().getWarning()+" Playlists folder does not exist and could not be created!");
                return;
            }
            List<String> list = playlistLoader.getPlaylistNames();
            if(list==null)
                event.reply(event.getClient().getError()+" Failed to load available playlists!");
            else if(list.isEmpty())
                event.reply(event.getClient().getWarning()+" There are no playlists in the Playlists folder!");
            else
            {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess()+" Available playlists:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }

    }
}
