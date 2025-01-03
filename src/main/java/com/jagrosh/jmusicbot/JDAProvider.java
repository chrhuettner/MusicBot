package com.jagrosh.jmusicbot;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.examples.command.AboutCommand;
import com.jagrosh.jdautilities.examples.command.PingCommand;
import com.jagrosh.jmusicbot.commands.admin.*;
import com.jagrosh.jmusicbot.commands.dj.*;
import com.jagrosh.jmusicbot.commands.general.SettingsCmd;
import com.jagrosh.jmusicbot.commands.music.*;
import com.jagrosh.jmusicbot.commands.owner.*;
import com.jagrosh.jmusicbot.entities.Prompt;
import com.jagrosh.jmusicbot.settings.SettingsManager;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.Arrays;

public class JDAProvider {

    public final static Logger LOG = LoggerFactory.getLogger(JDAProvider.class);
    private static JDA jda;

    private static BotConfig botConfig = BotConfig.getInstance();

    public static SettingsManager settingsManager = SettingsManager.getInstance();

    public final static GatewayIntent[] INTENTS = {GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES};

    public final static Permission[] RECOMMENDED_PERMS = {Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_MANAGE, Permission.MESSAGE_EXT_EMOJI,
            Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.NICKNAME_CHANGE};

    // The Listener immediately tries to access JDA via getInstance to send update available message to the discord user on a separate thread, we therefore use synchronize to avoid duplicated JDA creation
    public static synchronized JDA getInstance(){
        if(jda == null){
            Prompt prompt = Prompt.getInstance();
            try {
                jda = JDABuilder.create(botConfig.getToken(), Arrays.asList(INTENTS))
                        .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                        .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE, CacheFlag.ONLINE_STATUS)
                        .setActivity(botConfig.isGameNone() ? null : Activity.playing("loading..."))
                        .setStatus(botConfig.getStatus() == OnlineStatus.INVISIBLE || botConfig.getStatus() == OnlineStatus.OFFLINE
                                ? OnlineStatus.INVISIBLE : OnlineStatus.DO_NOT_DISTURB)
                        .addEventListeners(createCommandClient(), EventWaiterProvider.getInstance(), new Listener())
                        .setBulkDeleteSplittingEnabled(true)
                        .build();

                checkUnsupportedBot(jda, prompt);
                checkPrefixWarning(jda, botConfig);
            } catch (LoginException | IllegalArgumentException | ErrorResponseException ex) {
                handleStartupException(ex, prompt, botConfig);
            }
        }
        return jda;
    }

    private static void checkUnsupportedBot(JDA jda, Prompt prompt) {
        String unsupportedReason = OtherUtil.getUnsupportedBotReason(jda);
        if (unsupportedReason != null) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", "JMusicBot cannot be run on this Discord bot: " + unsupportedReason);
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            jda.shutdown();
            System.exit(1);
        }
    }

    private static void checkPrefixWarning(JDA jda, BotConfig config) {
        if (!"@mention".equals(config.getPrefix())) {
            LOG.info("JMusicBot", "You currently have a custom prefix set. If your prefix is not working, make sure that the 'MESSAGE CONTENT INTENT' is Enabled " +
                    "on https://discord.com/developers/applications/" + jda.getSelfUser().getId() + "/bot");
        }
    }

    private static void handleStartupException(Exception ex, Prompt prompt, BotConfig config) {
        if (ex instanceof LoginException) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", ex + "\nPlease make sure you are editing the correct config.txt file, " +
                    "and that you have used the correct token (not the 'secret'!)\nConfig Location: " + config.getConfigLocation());
        } else if (ex instanceof IllegalArgumentException) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", "Some aspect of the configuration is invalid: " + ex +
                    "\nConfig Location: " + config.getConfigLocation());
        } else if (ex instanceof ErrorResponseException) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", ex + "\nInvalid response returned when attempting to connect, " +
                    "please make sure you're connected to the internet");
        }
        System.exit(1);
    }

    private static CommandClient createCommandClient() {
        AboutCommand aboutCommand = createAboutCommand();
        CommandClientBuilder cb = setupCommandClient(settingsManager, aboutCommand);
        return cb.build();
    }

    private static AboutCommand createAboutCommand() {
        AboutCommand aboutCommand = new AboutCommand(Color.BLUE.brighter(),
                "a music bot that is [easy to host yourself!](https://github.com/jagrosh/MusicBot) (v" + OtherUtil.getCurrentVersion() + ")",
                new String[]{"High-quality music playback", "FairQueue™ Technology", "Easy to host yourself"},
                RECOMMENDED_PERMS);
        aboutCommand.setIsAuthor(false);
        aboutCommand.setReplacementCharacter("\uD83C\uDFB6"); // 🎶
        return aboutCommand;
    }

    private static CommandClientBuilder setupCommandClient(SettingsManager settings, AboutCommand aboutCommand) {
        CommandClientBuilder cb = new CommandClientBuilder()
                .setPrefix(botConfig.getPrefix())
                .setAlternativePrefix(botConfig.getAltPrefix())
                .setOwnerId(Long.toString(botConfig.getOwnerId()))
                .setEmojis(botConfig.getSuccess(), botConfig.getWarning(), botConfig.getError())
                .setHelpWord(botConfig.getHelp())
                .setLinkedCacheSize(200)
                .setGuildSettingsManager(settings)
                .addCommands(aboutCommand, new PingCommand(), new SettingsCmd(),
                        new LyricsCmd(), new NowplayingCmd(), new PlayCmd(), new PlaylistsCmd(), new QueueCmd(), new RemoveCmd(),
                        new SearchCmd(), new SCSearchCmd(), new SeekCmd(), new ShuffleCmd(), new SkipCmd(),
                        new ForceRemoveCmd(), new ForceskipCmd(), new MoveTrackCmd(), new PauseCmd(),
                        new PlaynextCmd(), new RepeatCmd(), new SkiptoCmd(), new StopCmd(), new VolumeCmd(),
                        new PrefixCmd(), new QueueTypeCmd(), new SetdjCmd(), new SkipratioCmd(),
                        new SettcCmd(), new SetvcCmd(),
                        new AutoplaylistCmd(), new DebugCmd(), new PlaylistCmd(),
                        new SetavatarCmd(), new SetgameCmd(), new SetnameCmd(), new SetstatusCmd(), new ShutdownCmd());

        if (botConfig.useEval()) cb.addCommand(new EvalCmd());
        if (botConfig.getStatus() != OnlineStatus.UNKNOWN) cb.setStatus(botConfig.getStatus());
        if (botConfig.getGame() == null) cb.useDefaultGame();
        else if (botConfig.isGameNone()) cb.setActivity(null);
        else cb.setActivity(botConfig.getGame());

        return cb;
    }

    public static Permission[] getRecommendedPermissions(){
        return RECOMMENDED_PERMS;
    }
}
