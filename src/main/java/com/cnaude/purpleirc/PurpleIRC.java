/*
 * Copyright (C) 2014 cnaude
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.cnaude.purpleirc;

import com.cnaude.purpleirc.Events.IRCMessageEvent;
import com.cnaude.purpleirc.GameListeners.GamePlayerChatListener;
import com.cnaude.purpleirc.GameListeners.GamePlayerCommandPreprocessingListener;
import com.cnaude.purpleirc.GameListeners.GamePlayerDeathListener;
import com.cnaude.purpleirc.GameListeners.GamePlayerGameModeChangeListener;
import com.cnaude.purpleirc.GameListeners.GamePlayerJoinListener;
import com.cnaude.purpleirc.GameListeners.GamePlayerKickListener;
import com.cnaude.purpleirc.GameListeners.GamePlayerPlayerAdvancementDoneListener;
import com.cnaude.purpleirc.GameListeners.GamePlayerQuitListener;
import com.cnaude.purpleirc.GameListeners.GameServerCommandListener;
import com.cnaude.purpleirc.GameListeners.IRCMessageListener;
import com.cnaude.purpleirc.GameListeners.VanishNoPacketListener;
import com.cnaude.purpleirc.Hooks.GriefPreventionHook;
import com.cnaude.purpleirc.Hooks.VanishHook;
import com.cnaude.purpleirc.Utilities.CaseInsensitiveMap;
import com.cnaude.purpleirc.Utilities.ChatTokenizer;
import com.cnaude.purpleirc.Utilities.ColorConverter;

import com.cnaude.purpleirc.Utilities.Query;
import com.cnaude.purpleirc.Utilities.RegexGlobber;
import com.cnaude.purpleirc.Utilities.UpdateChecker;
import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.pircbotx.IdentServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Chris Naudé
 */
public class PurpleIRC extends JavaPlugin {

    public String LOG_HEADER;
    public String LOG_HEADER_F;
    static final Logger LOG = Logger.getLogger("Minecraft");
    private final String sampleFileName;
    private final String MAINCONFIG;
    private File pluginFolder;
    public File botsFolder;
    private File configFile;
    public static long startTime;
    public boolean identServerEnabled;
    public boolean identServerStarted = false;
    private boolean autoSave;
    public boolean pingFixTemplate;
    private final CaseInsensitiveMap<HashMap<String, String>> messageTmpl;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> ircHeroChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> ircHeroActionChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> ircVentureChatChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> ircVentureChatActionChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> ircTownyChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> heroChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> heroActionChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> ventureChatChannelMessages;
    private final CaseInsensitiveMap<CaseInsensitiveMap<String>> ventureChatActionChannelMessages;
    private final Map<String, String> hostCache;
    public String defaultPlayerSuffix,
            defaultPlayerPrefix,
            defaultPlayerGroup,
            defaultGroupPrefix,
            defaultPlayerWorld,
            defaultGroupSuffix,
            customTabPrefix,
            heroChatEmoteFormat,
            heroPrivateChatFormat,
            listFormat,
            listSeparator,
            listPlayer,
            ircNickPrefixIrcOp,
            ircNickPrefixOwner,
            ircNickPrefixSuperOp,
            ircNickPrefixOp,
            ircNickPrefixHalfOp,
            ircNickPrefixVoice;
    private final CaseInsensitiveMap<String> displayNameCache;
    public CaseInsensitiveMap<UUID> uuidCache;

    public ArrayList<String> kickedPlayers = new ArrayList<>();

    public final String invalidBotName = ChatColor.RED + "Invalid bot name: "
            + ChatColor.WHITE + "%BOT%"
            + ChatColor.RED + ". Type '" + ChatColor.WHITE + "/irc listbots"
            + ChatColor.RED + "' to see valid bots.";

    public final String invalidChannelName = ChatColor.RED + "Invalid channel name: "
            + ChatColor.WHITE + "%CHANNEL%";

    public final String invalidChannel = ChatColor.RED + "Invalid channel: "
            + ChatColor.WHITE + "%CHANNEL%";
    public final String noPermission = ChatColor.RED + "You do not have permission to use this command.";

    private boolean updateCheckerEnabled;
    private String updateCheckerMode;
    private boolean debugEnabled;
    private boolean stripGameColors;
    private boolean stripIRCColors;
    private boolean stripIRCBackgroundColors;
    protected boolean stripGameColorsFromIrc;
    public boolean broadcastChatToConsole;
    public boolean customTabList;
    public String customTabGamemode;
    private boolean listSortByName;
    public boolean exactNickMatch;
    public boolean ignoreChatCancel;
    public boolean ventureChatEnabled;
    public Long ircConnCheckInterval;
    public Long ircChannelCheckInterval;
    public Long joinDelay;
    public long quitDelay;
    public ChannelWatcher channelWatcher;
    public LinkUpdater linkUpdater;
    public ColorConverter colorConverter;
    public RegexGlobber regexGlobber;
    public CaseInsensitiveMap<PurpleBot> ircBots;
    public GriefPreventionHook griefPreventionHook;
    public CommandHandlers commandHandlers;
    public PurpleTabCompleter ircTabCompleter;
    private BotWatcher botWatcher;
    public IRCMessageHandler ircMessageHandler;

    public CommandQueueWatcher commandQueue;
    public SynchronousEventWatcher eventQueue;
    public MessageQueueWatcher messageQueue;
    public UpdateChecker updateChecker;
    public ChatTokenizer tokenizer;
    private File heroConfigFile;
    public VanishHook vanishHook;
    private YamlConfiguration heroConfig;
    private final File cacheFile;
    private final File uuidCacheFile;
    public int reconnectSuppression;

    final String PL_VANISHNOPACKET = "VanishNoPacket";
    final String PL_GRIEFPREVENTION = "GriefPrevention";

    List<String> hookList = new ArrayList<>();
    public static final String PURPLETAG = RandomStringUtils.randomAlphanumeric(10) + "UHVycGxlSVJDCg==";
    public static final String TOWNYTAG = RandomStringUtils.randomAlphanumeric(10) + "VG93bnlDaGF0Cg==";
    public static final String LINK_CMD = "PurpleIRC-Link:";
    public boolean overrideMsgCmd = false;
    public String smsgAlias = "/m";
    public String smsgReplyAlias = "/r";
    public CaseInsensitiveMap<String> privateMsgReply;

    public PurpleIRC() {
        this.MAINCONFIG = "MAIN-CONFIG";
        this.sampleFileName = "SampleBot.yml";
        this.ircBots = new CaseInsensitiveMap<>();
        this.messageTmpl = new CaseInsensitiveMap<>();
        this.ircHeroChannelMessages = new CaseInsensitiveMap<>();
        this.ircHeroActionChannelMessages = new CaseInsensitiveMap<>();
        this.ircVentureChatChannelMessages = new CaseInsensitiveMap<>();
        this.ircVentureChatActionChannelMessages = new CaseInsensitiveMap<>();
        this.ircTownyChannelMessages = new CaseInsensitiveMap<>();
        this.heroChannelMessages = new CaseInsensitiveMap<>();
        this.heroActionChannelMessages = new CaseInsensitiveMap<>();
        this.ventureChatChannelMessages = new CaseInsensitiveMap<>();
        this.ventureChatActionChannelMessages = new CaseInsensitiveMap<>();
        this.displayNameCache = new CaseInsensitiveMap<>();
        this.uuidCache = new CaseInsensitiveMap<>();
        this.hostCache = new HashMap<>();
        this.cacheFile = new File("plugins/PurpleIRC/displayName.cache");
        this.uuidCacheFile = new File("plugins/PurpleIRC/uuid.cache");
        this.reconnectSuppression = 0;
        this.privateMsgReply = new CaseInsensitiveMap<>();
    }

    /**
     * Very first method that gets called when starting the plugin.
     */
    @Override
    public void onEnable() {
        LOG_HEADER = "[" + this.getName() + "]";
        LOG_HEADER_F = ChatColor.LIGHT_PURPLE + "[" + this.getName() + "]" + ChatColor.RESET;
        pluginFolder = getDataFolder();
        botsFolder = new File(pluginFolder + "/bots");
        configFile = new File(pluginFolder, "config.yml");
        createConfigDirs();
        createConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadConfig();
        loadDisplayNameCache();
        loadUuidCache();
        if (identServerEnabled) {
            logInfo("Starting Ident Server ...");
            try {
                IdentServer.startServer();
                identServerStarted = true;
            } catch (Exception ex) {                
                logError(ex.getMessage());
            }
        }
        getServer().getPluginManager().registerEvents(new IRCMessageListener(this), this);
        Pattern p = Pattern.compile("MC: [0-9]\\.([0-9]+).*");
        logInfo("MC version detected: " + getServer().getVersion());
        Matcher m = p.matcher(getServer().getVersion());
        if (m.find()) {
            int x = Integer.parseInt(m.group(1));
            if (x >= 12) {
                logInfo("Registering GamePlayerPlayerAdvancementDoneListener because version >= 1.12");
                getServer().getPluginManager().registerEvents(new GamePlayerPlayerAdvancementDoneListener(this), this);
            }
        } else {
            logError("Pattern mismatch!: " + getServer().getVersion());
        }
        getServer().getPluginManager().registerEvents(new GamePlayerGameModeChangeListener(this), this);
        getServer().getPluginManager().registerEvents(new GamePlayerChatListener(this), this);
        getServer().getPluginManager().registerEvents(new GamePlayerCommandPreprocessingListener(this), this);
        getServer().getPluginManager().registerEvents(new GamePlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GamePlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new GamePlayerKickListener(this), this);
        getServer().getPluginManager().registerEvents(new GamePlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new GameServerCommandListener(this), this);
        detectHooks();
        if (debugEnabled) {
            getPurpleHooks(getServer().getConsoleSender(), false);
        }
        commandHandlers = new CommandHandlers(this);
        ircTabCompleter = new PurpleTabCompleter(this);
        getCommand("irc").setExecutor(commandHandlers);
        getCommand("irc").setTabCompleter(ircTabCompleter);
        regexGlobber = new RegexGlobber();
        tokenizer = new ChatTokenizer(this);
        loadBots();
        createSampleBot();
        channelWatcher = new ChannelWatcher(this);
        linkUpdater = new LinkUpdater(this);
        setupVault();
        botWatcher = new BotWatcher(this);
        ircMessageHandler = new IRCMessageHandler(this);
        commandQueue = new CommandQueueWatcher(this);
        messageQueue = new MessageQueueWatcher(this);
        updateChecker = new UpdateChecker(this);
        eventQueue = new SynchronousEventWatcher(this);
    }

    /**
     * Called when plugin is told to stop.
     */
    @Override
    public void onDisable() {
        if (channelWatcher != null) {
            logDebug("Disabling channelWatcher ...");
            channelWatcher.cancel();
        }
        if (linkUpdater != null) {
            logDebug("Disabling linkUpdater ...");
            linkUpdater.cancel();
        }
        if (botWatcher != null) {
            logDebug("Disabling botWatcher ...");
            botWatcher.cancel();
        }
        if (updateChecker != null) {
            logDebug("Disabling updateChecker ...");
            updateChecker.cancel();
        }
        if (ircBots.isEmpty()) {
            logInfo("No IRC bots to disconnect.");
        } else {
            logInfo("Disconnecting IRC bots.");
            for (PurpleBot ircBot : ircBots.values()) {
                commandQueue.cancel();
                messageQueue.cancel();
                ircBot.stopTailers();
                if (autoSave) {
                    ircBot.saveConfig(getServer().getConsoleSender());
                }
                ircBot.quit();
            }
            ircBots.clear();
        }
        if (identServerEnabled && identServerStarted) {
            logInfo("Stopping Ident Server");
            try {
                IdentServer.stopServer();
            } catch (IOException ex) {
                logError(ex.getMessage());
            }
        }
        saveDisplayNameCache();
        saveUuidCache();
    }

    /**
     *
     * @param debug
     */
    public void debugMode(boolean debug) {
        debugEnabled = debug;
        getConfig().set("Debug", debug);
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            logError("Problem saving to " + configFile.getName() + ": " + ex.getMessage());
        }
    }

    /**
     * Return the current debug mode status
     *
     * @return
     */
    public boolean debugMode() {
        return debugEnabled;
    }

    public String getHeroMsgTemplate(String botName, String tmpl) {
        if (messageTmpl.containsKey(botName)) {
            if (messageTmpl.get(botName).containsKey(tmpl)) {
                return messageTmpl.get(botName).get(tmpl);
            }
        }
        if (messageTmpl.get(MAINCONFIG).containsKey(tmpl)) {
            return messageTmpl.get(MAINCONFIG).get(tmpl);
        }
        return "";
    }

    /**
     *
     * @param botName
     * @param channelName
     * @param template
     * @return
     */
    public String getMessageTemplate(String botName, String channelName, String template) {
        if (messageTmpl.containsKey(botName + "." + channelName)) {
            if (messageTmpl.get(botName + "." + channelName).containsKey(template)) {
                return messageTmpl.get(botName + "." + channelName).get(template);
            }
        }
        if (messageTmpl.containsKey(botName)) {
            if (messageTmpl.get(botName).containsKey(template)) {
                return messageTmpl.get(botName).get(template);
            }
        }
        if (messageTmpl.get(MAINCONFIG).containsKey(template)) {
            return messageTmpl.get(MAINCONFIG).get(template);
        }
        logDebug("No such template: " + template);
        return "";
    }

    public String getMessageTemplate(String template) {
        return getMessageTemplate(MAINCONFIG, "", template);
    }

    /**
     * Get message template for HeroChat, VentureChat or TownyChat based on
     * channel name
     *
     * @param templateMap map of message templates for specific chat plugin
     * @param botName our bot name
     * @param channel channel for plugin
     * @param template default template to look for if other is not found
     * @return message template
     */
    private String getMessageTemplate(CaseInsensitiveMap<CaseInsensitiveMap<String>> templateMap,
            String botName, String channel, String template) {
        if (templateMap.containsKey(botName)) {
            logDebug("HC1 => " + channel);
            for (String s : templateMap.get(botName).keySet()) {
                logDebug("HT => " + s);
            }
            if (templateMap.get(botName).containsKey(channel)) {
                logDebug("HC2 => " + channel);
                return templateMap.get(botName).get(channel);
            }
        }
        if (templateMap.containsKey(MAINCONFIG)) {
            logDebug("HC3 => " + channel);
            for (String s : templateMap.get(MAINCONFIG).keySet()) {
                logDebug("HT => " + s);
            }
            if (templateMap.get(MAINCONFIG).containsKey(channel)) {
                logDebug("HC4 => " + channel);
                return templateMap.get(MAINCONFIG).get(channel);
            }
        }
        return getMessageTemplate(MAINCONFIG, "", template);
    }

    public String getHeroChatTemplate(String botName, String channel) {
        return getMessageTemplate(heroChannelMessages, botName, channel, TemplateName.HERO_CHAT);
    }

    public String getHeroActionTemplate(String botName, String channel) {
        return getMessageTemplate(heroActionChannelMessages, botName, channel, TemplateName.HERO_ACTION);
    }

    public String getIrcHeroChatTemplate(String botName, String channel) {
        return getMessageTemplate(ircHeroChannelMessages, botName, channel, TemplateName.IRC_HERO_CHAT);
    }

    public String getIrcHeroActionTemplate(String botName, String channel) {
        return getMessageTemplate(ircHeroActionChannelMessages, botName, channel, TemplateName.IRC_HERO_ACTION);
    }

    public String getGameVentureChatTemplate(String botName, String channel) {
        return getMessageTemplate(ventureChatChannelMessages, botName, channel, TemplateName.VENTURE_CHAT);
    }

    public String getGameVentureChatActionTemplate(String botName, String channel) {
        return getMessageTemplate(ventureChatActionChannelMessages, botName, channel, TemplateName.VENTURE_CHAT_ACTION);
    }

    public String getIrcVentureChatTemplate(String botName, String channel) {
        return getMessageTemplate(ircVentureChatChannelMessages, botName, channel, TemplateName.IRC_VENTURE_CHAT);
    }

    public String getIrcVentureChatActionTemplate(String botName, String channel) {
        return getMessageTemplate(ircVentureChatActionChannelMessages, botName, channel, TemplateName.IRC_VENTURE_ACTION);
    }

    public String getIrcTownyChatTemplate(String botName, String channel) {
        return getMessageTemplate(ircTownyChannelMessages, botName, channel, TemplateName.IRC_TOWNY_CHAT);
    }

    public void loadCustomColors(YamlConfiguration config) {
        for (String t : config.getConfigurationSection("irc-color-map").getKeys(false)) {
            colorConverter.addIrcColorMap(t, config.getString("irc-color-map." + t));
        }
        for (String t : config.getConfigurationSection("game-color-map").getKeys(false)) {
            colorConverter.addGameColorMap(t, config.getString("game-color-map." + t));
        }
    }

    public void loadTemplates(YamlConfiguration config, String configName, String section) {
        messageTmpl.put(configName, new HashMap<String, String>());

        ircHeroChannelMessages.put(configName, new CaseInsensitiveMap<String>());
        ircHeroActionChannelMessages.put(configName, new CaseInsensitiveMap<String>());
        heroChannelMessages.put(configName, new CaseInsensitiveMap<String>());
        heroActionChannelMessages.put(configName, new CaseInsensitiveMap<String>());

        ircVentureChatChannelMessages.put(configName, new CaseInsensitiveMap<String>());
        ircVentureChatActionChannelMessages.put(configName, new CaseInsensitiveMap<String>());
        ventureChatChannelMessages.put(configName, new CaseInsensitiveMap<String>());
        ventureChatActionChannelMessages.put(configName, new CaseInsensitiveMap<String>());

        ircTownyChannelMessages.put(configName, new CaseInsensitiveMap<String>());

        if (config.contains(section)) {
            for (String t : config.getConfigurationSection(section).getKeys(false)) {
                if (!t.startsWith("MemorySection")) {
                    messageTmpl.get(configName).put(t, ChatColor.translateAlternateColorCodes('&',
                            config.getString(section + "." + t, "")));
                    logDebug(section + ": " + t + " => " + messageTmpl.get(configName).get(t));
                }
            }

            if (config.contains(section + ".irc-hero-channels")) {
                for (String hChannelName : config.getConfigurationSection(section + ".irc-hero-channels").getKeys(false)) {
                    ircHeroChannelMessages.get(configName).put(hChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".irc-hero-channels."
                                            + hChannelName)));
                    logDebug(section + ".irc-hero-channels: " + hChannelName
                            + " => " + ircHeroChannelMessages.get(configName).get(hChannelName));
                }
            }
            if (config.contains(section + ".irc-hero-action-channels")) {
                for (String hChannelName : config.getConfigurationSection(section + ".irc-hero-action-channels").getKeys(false)) {
                    ircHeroActionChannelMessages.get(configName).put(hChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".irc-hero-action-channels."
                                            + hChannelName)));
                    logDebug(section + ".irc-hero-action-channels: " + hChannelName
                            + " => " + ircHeroActionChannelMessages.get(configName).get(hChannelName));
                }
            }

            if (config.contains(section + ".irc-towny-channels")) {
                for (String tChannelName : config.getConfigurationSection(section + ".irc-towny-channels").getKeys(false)) {
                    ircTownyChannelMessages.get(configName).put(tChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".irc-towny-channels."
                                            + tChannelName)));
                    logDebug(section + ".irc-towny-channels: " + tChannelName
                            + " => " + ircTownyChannelMessages.get(configName).get(tChannelName));
                }
            }

            if (config.contains(section + ".hero-channels")) {
                for (String hChannelName : config.getConfigurationSection(section + ".hero-channels").getKeys(false)) {
                    heroChannelMessages.get(configName).put(hChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".hero-channels."
                                            + hChannelName)));
                    logDebug(section + ".hero-channels: " + hChannelName
                            + " => " + heroChannelMessages.get(configName).get(hChannelName));
                }
            }
            if (config.contains(section + ".hero-action-channels")) {
                for (String hChannelName : config.getConfigurationSection(section + ".hero-action-channels").getKeys(false)) {
                    heroActionChannelMessages.get(configName).put(hChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".hero-action-channels."
                                            + hChannelName)));
                    logDebug(section + ".hero-action-channels: " + hChannelName
                            + " => " + heroActionChannelMessages.get(configName).get(hChannelName));
                }
            }

            if (config.contains(section + ".venture-channels")) {
                for (String mvChannelName : config.getConfigurationSection(section + ".venture-channels").getKeys(false)) {
                    ventureChatChannelMessages.get(configName).put(mvChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".venture-channels."
                                            + mvChannelName)));
                    logDebug(section + ".venture-channels: " + mvChannelName
                            + " => " + ventureChatChannelMessages.get(configName).get(mvChannelName));
                }
            }
            if (config.contains(section + ".venture-action-channels")) {
                for (String mvChannelName : config.getConfigurationSection(section + ".venture-action-channels").getKeys(false)) {
                    ventureChatActionChannelMessages.get(configName).put(mvChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".venture-action-channels."
                                            + mvChannelName)));
                    logDebug(section + ".venture-action-channels: " + mvChannelName
                            + " => " + ventureChatActionChannelMessages.get(configName).get(mvChannelName));
                }
            }

            if (config.contains(section + ".irc-venture-channels")) {
                for (String mvChannelName : config.getConfigurationSection(section + ".irc-venture-channels").getKeys(false)) {
                    ircVentureChatChannelMessages.get(configName).put(mvChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".irc-venture-channels."
                                            + mvChannelName)));
                    logDebug(section + ".irc-venture-channels: " + mvChannelName
                            + " => " + ircVentureChatChannelMessages.get(configName).get(mvChannelName));
                }
            }
            if (config.contains(section + ".irc-venture-action-channels")) {
                for (String mvChannelName : config.getConfigurationSection(section + ".irc-venture-action-channels").getKeys(false)) {
                    ircVentureChatActionChannelMessages.get(configName).put(mvChannelName,
                            ChatColor.translateAlternateColorCodes('&',
                                    config.getString(section + ".irc-venture-action-channels."
                                            + mvChannelName)));
                    logDebug(section + ".irc-venture-action-channels: " + mvChannelName
                            + " => " + ircVentureChatActionChannelMessages.get(configName).get(mvChannelName));
                }
            }
        } else {
            logDebug("No message-format section found for " + configName);
        }
    }

    private void loadConfig() {
        try {
            getConfig().load(configFile);
        } catch (IOException | InvalidConfigurationException ex) {
            logError(ex.getMessage());
        }
        autoSave = getConfig().getBoolean("save-on-shutdown", false);
        pingFixTemplate = getConfig().getBoolean("chat-ping-fix", false);
        overrideMsgCmd = getConfig().getBoolean("override-msg-cmd", false);
        smsgAlias = getConfig().getString("smsg-alias", "/m");
        smsgReplyAlias = getConfig().getString("smsg-reply-alias", "/r");
        updateCheckerEnabled = getConfig().getBoolean("update-checker", true);
        updateCheckerMode = getConfig().getString("update-checker-mode", "stable");
        debugEnabled = getConfig().getBoolean("Debug");
        identServerEnabled = getConfig().getBoolean("enable-ident-server");
        logDebug("Debug enabled");
        stripGameColors = getConfig().getBoolean("strip-game-colors", false);
        stripIRCColors = getConfig().getBoolean("strip-irc-colors", false);
        stripIRCBackgroundColors = getConfig().getBoolean("strip-irc-bg-colors", true);
        stripGameColorsFromIrc = getConfig().getBoolean("strip-game-colors-from-irc", true);
        exactNickMatch = getConfig().getBoolean("nick-exact-match", true);
        ignoreChatCancel = getConfig().getBoolean("ignore-chat-cancel", false);
        colorConverter = new ColorConverter(this, stripGameColors, stripIRCColors, stripIRCBackgroundColors, stripGameColorsFromIrc);
        logDebug("strip-game-colors: " + stripGameColors);
        logDebug("strip-irc-colors: " + stripIRCColors);

        String msgFormatSection = "message-format";
        loadTemplates((YamlConfiguration) this.getConfig(), MAINCONFIG, msgFormatSection);
        loadCustomColors((YamlConfiguration) this.getConfig());

        defaultPlayerSuffix = ChatColor.translateAlternateColorCodes('&', getConfig().getString(msgFormatSection + ".default-player-suffix", ""));
        defaultPlayerPrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString(msgFormatSection + ".default-player-prefix", ""));
        defaultPlayerGroup = ChatColor.translateAlternateColorCodes('&', getConfig().getString(msgFormatSection + ".default-player-group", ""));
        defaultGroupSuffix = ChatColor.translateAlternateColorCodes('&', getConfig().getString(msgFormatSection + ".default-group-suffix", ""));
        defaultGroupPrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString(msgFormatSection + ".default-group-prefix", ""));
        defaultPlayerWorld = ChatColor.translateAlternateColorCodes('&', getConfig().getString(msgFormatSection + ".default-player-world", ""));

        ircNickPrefixIrcOp = ChatColor.translateAlternateColorCodes('&', getConfig().getString("nick-prefixes.ircop", "~"));
        ircNickPrefixOwner = ChatColor.translateAlternateColorCodes('&', getConfig().getString("nick-prefixes.owner", "@"));
        ircNickPrefixSuperOp = ChatColor.translateAlternateColorCodes('&', getConfig().getString("nick-prefixes.ircsuperop", "&&"));
        ircNickPrefixOp = ChatColor.translateAlternateColorCodes('&', getConfig().getString("nick-prefixes.op", "@"));
        ircNickPrefixHalfOp = ChatColor.translateAlternateColorCodes('&', getConfig().getString("nick-prefixes.halfop", "%"));
        ircNickPrefixVoice = ChatColor.translateAlternateColorCodes('&', getConfig().getString("nick-prefixes.voice", "+"));

        listFormat = ChatColor.translateAlternateColorCodes('&', getConfig().getString("list-format", ""));
        listSeparator = ChatColor.translateAlternateColorCodes('&', getConfig().getString("list-separator", ""));
        listPlayer = ChatColor.translateAlternateColorCodes('&', getConfig().getString("list-player", ""));
        listSortByName = getConfig().getBoolean("list-sort-by-name", true);

        ircConnCheckInterval = getConfig().getLong("conn-check-interval");
        reconnectSuppression = getConfig().getInt("reconnect-fail-message-count", 10);
        ircChannelCheckInterval = getConfig().getLong("channel-check-interval");
        joinDelay = getConfig().getLong("join-delay", 20);
        quitDelay = getConfig().getLong("quit-delay", 20);

        customTabGamemode = getConfig().getString("custom-tab-gamemode", "SPECTATOR");
        customTabList = getConfig().getBoolean("custom-tab-list", false);
        customTabPrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("custom-tab-prefix", "[IRC] "));
        logDebug("custom-tab-list: " + customTabList);
        logDebug("custom-tab-prefix: " + customTabPrefix);
        logDebug("custom-tab-gamemode: " + customTabGamemode);
        broadcastChatToConsole = getConfig().getBoolean("broadcast-chat-to-console", true);
    }

    private void loadBots() {
        if (botsFolder.exists()) {
            logInfo("Checking for bot files in " + botsFolder);
            for (final File file : botsFolder.listFiles()) {
                if (file.getName().toLowerCase().endsWith(".yml")) {
                    loadBot(getServer().getConsoleSender(), file);
                }
            }
        }
    }

    public void loadBot(CommandSender sender, File file) {
        String fileName = file.getName();
        if (fileName.toLowerCase().endsWith(".yml")) {
            if (file.exists()) {
                String bot = fileName.replaceAll("(?i).yml", "");
                if (ircBots.containsKey(bot)) {
                    sender.sendMessage(ChatColor.RED + "Sorry that bot is already loaded. Try to unload it first.");
                    return;
                }
                sender.sendMessage(ChatColor.WHITE + "Loading " + fileName + "...");
                PurpleBot ircBot = new PurpleBot(file, this);
                if (ircBot.goodBot) {
                    ircBots.put(bot, ircBot);
                    sender.sendMessage(LOG_HEADER_F + " " + ChatColor.WHITE + "Bot loaded from " + fileName
                            + ChatColor.LIGHT_PURPLE + " [" + ChatColor.WHITE + ircBot.botNick + ChatColor.LIGHT_PURPLE + "/"
                            + ChatColor.WHITE + ircBot.botServer + ChatColor.LIGHT_PURPLE + ":"
                            + ChatColor.WHITE + ircBot.botServerPort + ChatColor.LIGHT_PURPLE + "]");
                } else {
                    logError("Unable to load " + fileName);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "No such bot file: " + ChatColor.WHITE + fileName);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid file name.");
        }
    }

    /**
     *
     * @return
     */
    public boolean isFactionsEnabled() {
        if (getServer().getPluginManager().getPlugin("Factions") != null) {
            String v = getServer().getPluginManager().getPlugin("Factions").getDescription().getVersion();
            logDebug("Factions version => " + v);
            if (v.startsWith("2.")) {
                return true;
            } else {
                logInfo("Invalid Factions version! Only version 2.0.0 and higher is supported.");
            }
        }
        return false;
    }

    public boolean isPluginEnabled(String pluginName) {
        return getServer().getPluginManager().getPlugin(pluginName) != null;
    }

    private void createSampleBot() {
        File file = new File(pluginFolder + "/" + sampleFileName);
        try {
            try (InputStream in = PurpleIRC.class.getResourceAsStream("/" + sampleFileName)) {
                byte[] buf = new byte[1024];
                int len;
                try (OutputStream out = new FileOutputStream(file)) {
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        } catch (IOException ex) {
            logError("Problem creating sample bot: " + ex.getMessage());
        }
    }

    /**
     *
     * @param worldName
     * @return
     */
    public String getWorldAlias(String worldName) {
        String alias = worldName;
        if (alias == null) {
            alias = worldName;
        }
        logDebug("getWorldAlias: worldName => " + worldName);
        logDebug("getWorldAlias: alias => " + alias);
        return alias;
    }

    /**
     *
     * @param sender
     */
    public void reloadMainConfig(CommandSender sender) {
        sender.sendMessage(LOG_HEADER_F + " Reloading config.yml ...");
        reloadConfig();
        getConfig().options().copyDefaults(false);
        loadConfig();
        sender.sendMessage(LOG_HEADER_F + " Done.");
    }

    private void createConfigDirs() {
        if (!pluginFolder.exists()) {
            try {
                logInfo("Creating " + pluginFolder.getAbsolutePath());
                pluginFolder.mkdir();
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }

        if (!botsFolder.exists()) {
            try {
                logInfo("Creating " + botsFolder.getAbsolutePath());
                botsFolder.mkdir();
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }
    }

    private void createConfig() {
        if (!configFile.exists()) {
            try {
                logInfo("Creating config.yml");
                configFile.createNewFile();
            } catch (IOException e) {
                logError(e.getMessage());
            }
        }

    }

    /**
     *
     * @param message
     */
    public void logInfo(String message) {
        LOG.log(Level.INFO, String.format("%s %s", LOG_HEADER, message));
    }

    /**
     *
     * @param message
     */
    public void logError(String message) {
        LOG.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, message));
    }

    /**
     *
     * @param message
     */
    public void logDebug(String message) {
        if (debugEnabled) {
            LOG.log(Level.INFO, String.format("%s [DEBUG] %s", LOG_HEADER, message));
        }
    }

    /**
     *
     * @return
     */
    public String getMCUptime() {
        long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String msg = "Server uptime: " + (int) (jvmUptime / 86400000L) + " days"
                + " " + (int) (jvmUptime / 3600000L % 24L) + " hours"
                + " " + (int) (jvmUptime / 60000L % 60L) + " minutes"
                + " " + (int) (jvmUptime / 1000L % 60L) + " seconds.";
        return msg;
    }

    public String getServerMotd() {
        return "MOTD: " + getServer().getMotd();
    }

    /**
     *
     * @param ircBot
     * @param channelName
     * @return
     */
    public String getMCPlayers(PurpleBot ircBot, String channelName) {
        PlayerList pl = getMCPlayerList(ircBot, channelName);

        String msg = listFormat
                .replace("%COUNT%", Integer.toString(pl.count))
                .replace("%MAX%", Integer.toString(pl.max))
                .replace("%PLAYERS%", pl.list);

        return colorConverter.gameColorsToIrc(msg);
    }

    /**
     *
     * @param ircBot
     * @param channelName
     * @return
     */
    public PlayerList getMCPlayerList(PurpleBot ircBot, String channelName) {
        PlayerList pl = new PlayerList();

        Map<String, String> playerList = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Player player : getServer().getOnlinePlayers()) {
            if (ircBot.hideListWhenVanished.get(channelName)) {
                logDebug("List: Checking if player " + player.getName() + " is vanished.");
                if (vanishHook.isVanished(player)) {
                    logDebug("Not adding player to list command" + player.getName() + " due to being vanished.");
                    continue;
                }
            }
            String pName = tokenizer.playerTokenizer(player, listPlayer);
            playerList.put(player.getName(), pName);
        }

        String pList;
        if (!listSortByName) {
            // sort as before
            ArrayList<String> tmp = new ArrayList<>(playerList.values());
            Collections.sort(tmp, Collator.getInstance());
            pList = Joiner.on(listSeparator).join(tmp);
        } else {
            // sort without nick prefixes
            pList = Joiner.on(listSeparator).join(playerList.values());
        }

        pl.count = playerList.size();
        pl.max = getServer().getMaxPlayers();
        pl.list = pList;

        return pl;

    }

    public String getRemotePlayers(String commandArgs) {
        if (commandArgs != null) {
            String host;
            int port = 25565;
            if (commandArgs.contains(":")) {
                host = commandArgs.split(":")[0];
                port = Integer.parseInt(commandArgs.split(":")[1]);
            } else {
                host = commandArgs;
            }
            Query query = new Query(host, port);
            try {
                query.sendQuery();
            } catch (IOException ex) {
                return ex.getMessage();
            }
            String players[] = query.getOnlineUsernames();
            String m;
            if (players.length == 0) {
                m = "There are no players on " + host
                        + ":" + port;
            } else {
                m = "Players on " + host + "("
                        + players.length
                        + "): " + Joiner.on(", ")
                                .join(players);
            }
            return m;
        } else {
            return "Invalid host.";
        }
    }

    /**
     *
     */
    public void setupVault() {

    }

    /**
     *
     * @param player
     * @return
     */
    public String getPlayerGroup(Player player) {
        String groupName = "";
        if (groupName == null) {
            groupName = "";
        }
        return ChatColor.translateAlternateColorCodes('&', groupName);
    }

    /**
     *
     * @param player
     * @return
     */
    public UUID getPlayerUuid(String player) {
        if (uuidCache.containsKey(player)) {
            return uuidCache.get(player);
        }
        return null;
    }

    /**
     *
     * @param worldName
     * @param player
     * @return
     */
    public String getPlayerGroup(String worldName, String player) {
        String groupName = "";
        try {
            UUID uuid = getPlayerUuid(player);
        } catch (Exception ex) {
            logDebug("getPlayerGroup (" + player + "): " + ex.getMessage());
        }
        if (groupName == null) {
            groupName = "";
        }
        return ChatColor.translateAlternateColorCodes('&', groupName);
    }

    /**
     *
     * @param player
     * @return
     */
    public String getPlayerPrefix(Player player) {
        String prefix = "";
        if (prefix == null) {
            prefix = "";
        }
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     *
     * @param worldName
     * @param player
     * @return
     */
    public String getPlayerPrefix(String worldName, String player) {
        String prefix = "";
        try {
            UUID uuid = getPlayerUuid(player);
        } catch (Exception ex) {
            logDebug("getPlayerPrefix (" + player + "): " + ex.getMessage());
        }
        if (prefix == null) {
            prefix = "";
        }
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     *
     * @param player
     * @return
     */
    public String getPlayerSuffix(Player player) {
        String suffix = "";
        if (suffix == null) {
            suffix = "";
        }
        return ChatColor.translateAlternateColorCodes('&', suffix);
    }

    /**
     *
     * @param worldName
     * @param player
     * @return
     */
    public String getPlayerSuffix(String worldName, String player) {
        String suffix = "";
        try {
            UUID uuid = getPlayerUuid(player);
        } catch (Exception ex) {
            logDebug("getPlayerSuffix (" + player + "): " + ex.getMessage());
        }
        if (suffix == null) {
            suffix = "";
        }
        return ChatColor.translateAlternateColorCodes('&', suffix);
    }

    /**
     *
     * @param pName
     * @return
     */
    public String getDisplayName(String pName) {
        String displayName = null;
        Player player = this.getServer().getPlayer(pName);
        logDebug("player: " + player);
        if (player != null) {
            displayName = player.getDisplayName();
        }
        if (displayName != null) {
            logDebug("Caching displayName for " + pName + " = " + displayName);
            displayNameCache.put(pName, displayName);
        } else if (displayNameCache.containsKey(pName)) {
            displayName = displayNameCache.get(pName);
        } else {
            displayName = pName;
        }
        return displayName;
    }

    /**
     *
     * @param player
     */
    public void updateDisplayNameCache(Player player) {
        logDebug("Caching displayName for " + player.getName() + " = " + player.getDisplayName());
        displayNameCache.put(player.getName(), player.getDisplayName());
    }

    /**
     *
     * @param player
     * @param displayName
     */
    public void updateDisplayNameCache(String player, String displayName) {
        logDebug("Caching displayName for " + player + " = " + displayName);
        displayNameCache.put(player, displayName);
    }

    /**
     *
     * @param player
     */
    public void updateUuidCache(Player player) {
        logDebug("Caching UUID for " + player.getName() + " = " + player.getUniqueId().toString());
        uuidCache.put(player.getName(), player.getUniqueId());
    }

    /**
     *
     * @param player
     * @param uuid
     */
    public void updateUuidCache(String player, UUID uuid) {
        logDebug("Caching UUID for " + player + " = " + uuid.toString());
        uuidCache.put(player, uuid);
    }

    /**
     *
     * @param player
     * @return
     */
    public String getGroupPrefix(Player player) {
        String prefix = "";
        if (prefix == null) {
            prefix = "";
        }
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     *
     * @param worldName
     * @param player
     * @return
     */
    public String getGroupPrefix(String worldName, String player) {
        logDebug("getGroupPrefix: 1");
        String prefix = "";
        try {
            logDebug("getGroupPrefix: 2");
            UUID uuid = getPlayerUuid(player);
            logDebug("getGroupPrefix: 3");
            if (prefix == null) {
                prefix = "";
            }
        } catch (Exception ex) {
            logDebug("getGroupPrefix (" + player + "): " + ex.getMessage());
        }
        logDebug("getGroupPrefix: 14");
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    /**
     *
     * @param worldName
     * @return
     */
    public String getWorldColor(String worldName) {
        String color = worldName;
        if (color == null) {
            color = worldName;
        }
        logDebug("getWorldColor: worldName => " + worldName);
        logDebug("getWorldColor: color => " + color);
        return color;
    }

    /**
     *
     * @param player
     * @return
     */
    public String getGroupSuffix(Player player) {
        String suffix = "";
        if (suffix == null) {
            suffix = "";
        }
        return ChatColor.translateAlternateColorCodes('&', suffix);
    }

    /**
     *
     * @param worldName
     * @param player
     * @return
     */
    public String getGroupSuffix(String worldName, String player) {
        String suffix = "";
        UUID uuid = getPlayerUuid(player);
        if (suffix == null) {
            suffix = "";
        }
        return ChatColor.translateAlternateColorCodes('&', suffix);
    }

    /**
     *
     * @return
     */
    public boolean checkForProtocolLib() {
        return (getServer().getPluginManager().getPlugin("ProtocolLib") != null);
    }

    public boolean isPrismEnabled() {
        return (getServer().getPluginManager().getPlugin("Prism") != null);
    }

    public void saveDisplayNameCache() {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(cacheFile));
        } catch (IOException ex) {
            logError(ex.getMessage());
            return;
        }

        try {
            for (String s : displayNameCache.keySet()) {
                logDebug("Saving to displayName.cache: " + s + "\t" + displayNameCache.get(s));
                writer.write(s + "\t" + displayNameCache.get(s) + "\n");
            }
            writer.close();
        } catch (IOException ex) {
            logError(ex.getMessage());
        }
    }

    public void loadDisplayNameCache() {
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(cacheFile))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("\n")) {
                        continue;
                    }
                    String[] parts = line.split("\t", 2);
                    updateDisplayNameCache(parts[0], parts[1]);
                }
            }
        } catch (IOException | NumberFormatException e) {
            logError(e.getMessage());
        }
    }

    public void saveUuidCache() {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(uuidCacheFile));
        } catch (IOException ex) {
            logError(ex.getMessage());
            return;
        }

        try {
            for (String s : uuidCache.keySet()) {
                logDebug("Saving to uuid.cache: " + s + "\t" + uuidCache.get(s).toString());
                writer.write(s + "\t" + uuidCache.get(s).toString() + "\n");
            }
            writer.close();
        } catch (IOException ex) {
            logError(ex.getMessage());
        }
    }

    public void loadUuidCache() {
        try {
            try (BufferedReader in = new BufferedReader(new FileReader(uuidCacheFile))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("\n")) {
                        continue;
                    }
                    String[] parts = line.split("\t", 2);
                    updateUuidCache(parts[0], UUID.fromString(parts[1]));
                }
            }
        } catch (IOException | NumberFormatException e) {
            logError(e.getMessage());
        }
    }

    public String getPlayerHost(final String playerIP) {
        if (playerIP == null) {
            return "unknown";
        }
        if (hostCache.containsKey(playerIP)) {
            return hostCache.get(playerIP);
        } else {
            getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                @Override
                public void run() {
                    long a = System.currentTimeMillis();
                    InetAddress addr = null;
                    try {
                        addr = InetAddress.getByName(playerIP);
                    } catch (UnknownHostException ex) {
                        logError(ex.getMessage());
                    }
                    String host;
                    if (addr != null) {
                        host = addr.getHostName();
                    } else {
                        host = playerIP;
                    }
                    hostCache.put(playerIP, host);
                    logDebug("getPlayerHost[" + (System.currentTimeMillis() - a) + "ms] " + playerIP + " = " + host);
                }
            }, 0);
            return playerIP;
        }
    }

    public void clearHostCache(final Player player) {
        String playerIP = player.getAddress().getAddress().getHostAddress();
        if (hostCache.containsKey(playerIP)) {
            hostCache.remove(playerIP);
        }
    }

    public boolean isUpdateCheckerEnabled() {
        return updateCheckerEnabled;
    }

    public String updateCheckerMode() {
        return updateCheckerMode;
    }

    private String hookFormat(String name, boolean enabled) {
        String message;

        if (enabled) {
            String version = getServer().getPluginManager().getPlugin(name).getDescription().getVersion();
            logInfo("Hook enabled: " + name);
            message = ChatColor.WHITE + "[" + ChatColor.GREEN + "Y" + ChatColor.WHITE + "]";
            message = message + " [" + ChatColor.GOLD + name + ChatColor.WHITE + "] ["
                    + ChatColor.GOLD + "v" + version + ChatColor.WHITE + "]";
        } else {
            logInfo("Hook NOT enabled: " + name);
            message = ChatColor.WHITE + "[" + ChatColor.RED + "N" + ChatColor.WHITE + "]";
            message = message + " [" + ChatColor.GRAY + name + ChatColor.WHITE + "]";
        }

        return message;
    }

    private void detectHooks() {
        logInfo("Detecting plugin hooks...");
        vanishHook = new VanishHook(this);
        if (isPluginEnabled(PL_GRIEFPREVENTION)) {
            Class cls = null;
            boolean hooked = false;
            try {
                cls = Class.forName("me.ryanhamshire.GriefPrevention.DataStore");
            } catch (ClassNotFoundException ex) {
                logDebug(ex.getMessage());
            }
            if (cls != null) {
                for (Method m : cls.getMethods()) {
                    if (m.getName().equals("isSoftMuted")) {
                        hookList.add(hookFormat(PL_GRIEFPREVENTION, true));
                        griefPreventionHook = new GriefPreventionHook(this);
                        hooked = true;
                        break;
                    }
                }
            }
            if (!hooked) {
                hookList.add(hookFormat(PL_GRIEFPREVENTION, false));
            }
        } else {
            hookList.add(hookFormat(PL_GRIEFPREVENTION, false));
        }
        if (isPluginEnabled(PL_VANISHNOPACKET)) {
            hookList.add(hookFormat(PL_VANISHNOPACKET, true));
            getServer().getPluginManager().registerEvents(new VanishNoPacketListener(this), this);
        } else {
            hookList.add(hookFormat(PL_VANISHNOPACKET, false));
        }
        Collections.sort(hookList);
    }

    public void getPurpleHooks(CommandSender sender, boolean colors) {
        String header = ChatColor.LIGHT_PURPLE + "-----[" + ChatColor.WHITE
                + " PurpleIRC " + ChatColor.LIGHT_PURPLE
                + "-" + ChatColor.WHITE + " Plugin Hooks " + ChatColor.LIGHT_PURPLE + "]-----";
        String footer = ChatColor.LIGHT_PURPLE + "-------------------------------------";
        if (colors) {
            sender.sendMessage(header);
        } else {
            sender.sendMessage(ChatColor.stripColor(header));
        }
        for (String s : hookList) {
            if (colors) {
                sender.sendMessage(s);
            } else {
                sender.sendMessage(ChatColor.stripColor(s));
            }
        }
        if (colors) {
            sender.sendMessage(footer);
        } else {
            sender.sendMessage(ChatColor.stripColor(footer));
        }
    }

    public void broadcastToGame(final String message, final String channel, final String permission) {
        eventQueue.add(new IRCMessageEvent(message, channel, permission));
    }

    public void broadcastToPlayer(final String message, final String channel, final String permission, final Player player) {
        eventQueue.add(new IRCMessageEvent(message, channel, permission, player));
    }

    /**
     *
     * @param cmd
     * @param msg
     * @return
     */
    public String encodeLinkMsg(String cmd, String msg) {
        String encodedText = new String(Base64.encodeBase64(msg.getBytes()));
        return String.format("%s:%s", cmd, encodedText);
    }

    /**
     * Generic player counter. CB uses Player[] and Spigot uses List<>().
     *
     * @return
     */
    public int getOnlinePlayerCount() {
        int count = 0;
        for (Player p : getServer().getOnlinePlayers()) {
            count++;
        }
        return count;
    }

    public boolean isMuted(Player player) {
        if (griefPreventionHook != null) {
            if (griefPreventionHook.isMuted(player)) {
                logDebug("GP: Player " + player.getDisplayName() + " is muted.");
                return true;
            }
        }
        return false;
    }

}
