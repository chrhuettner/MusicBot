package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jmusicbot.BotConfig;

public abstract class AliasCommand extends Command {

    protected final BotConfig botConfig;

    public AliasCommand(String commandName) {
        this.botConfig = BotConfig.getInstance();
        this.name = commandName;
        this.aliases = botConfig.getAliases(name);
    }
}
