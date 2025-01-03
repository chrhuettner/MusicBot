package com.jagrosh.jmusicbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jmusicbot.BotConfig;

public abstract class AliasCommand extends Command {

    protected BotConfig botConfig;

    public AliasCommand(String commandName) {
        this.botConfig = BotConfig.getBotConfig();
        this.name = commandName;
        this.aliases = botConfig.getAliases(name);
    }
}
