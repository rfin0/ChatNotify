/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.chatnotify.config;

import com.google.gson.*;
import dev.terminalmc.chatnotify.ChatNotify;
import dev.terminalmc.chatnotify.platform.Services;
import dev.terminalmc.chatnotify.util.JsonUtil;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

/**
 * Root configuration options class. Consists of:
 *
 * <p>A range of mostly-enum controls for global behavior.</p>
 *
 * <p>A set of default values for new {@link Notification} instances.</p>
 *
 * <p>A list of prefix strings for use in message sender detection.</p>
 *
 * <p>A list of {@link Notification} instances.</p>
 *
 * <p><b>Note:</b> The list of {@link Notification}s is required to maintain an
 * instance at index {@code 0} for the user's name. This instance is handled
 * differently in several ways, but is kept in the list for ease of iteration.
 * </p>
 *
 * <p><b>Note:</b> The {@code VERSION} constant of a config class must be
 * incremented whenever the json structure of the class is changed, to
 * facilitate correct conditional deserialization.</p>
 *
 * <p><b>Note:</b> For enum controls without a specified default value, the
 * first value of the enum should be used as the default.</p>
 */
public class Config {
    public static final int VERSION = 9;
    public final int version = VERSION;
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir();
    public static final String FILE_NAME = ChatNotify.MOD_ID + ".json";
    public static final String UNREADABLE_FILE_NAME = ChatNotify.MOD_ID + ".unreadable.json";
    public static final String OLD_FILE_NAME = ChatNotify.MOD_ID + ".old.json";
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Deserializer())
            .registerTypeAdapter(Notification.class, new Notification.Deserializer())
            .registerTypeAdapter(Sound.class, new Sound.Deserializer())
            .registerTypeAdapter(TextStyle.class, new TextStyle.Deserializer())
            .registerTypeAdapter(Trigger.class, new Trigger.Deserializer())
            .registerTypeAdapter(StyleTarget.class, new StyleTarget.Deserializer())
            .registerTypeAdapter(ResponseMessage.class, new ResponseMessage.Deserializer())
            .setPrettyPrinting()
            .create();

    // Controls

    /**
     * Controls how messages are intercepted.
     */
    public DetectionMode detectionMode;
    public enum DetectionMode {
        HUD_KNOWN_TAGS,
        HUD,
        PACKET,
    }

    /**
     * Controls debug logging.
     */
    public DebugMode debugMode;
    public enum DebugMode {
        NONE,
        ALL,
    }

    /**
     * Controls how many {@link Notification}s can be activated by a single
     * message.
     */
    public NotifMode notifMode;
    public enum NotifMode {
        ALL_SINGLE_SOUND,
        ALL,
        SINGLE,
    }

    /**
     * Controls message restyling.
     */
    public RestyleMode restyleMode;
    public enum RestyleMode {
        ALL_INSTANCES,
        SINGLE,
    }

    /**
     * Controls how {@link ResponseMessage}s are sent.
     */
    public SendMode sendMode;
    public enum SendMode {
        PACKET,
        SCREEN,
    }

    /**
     * Controls how messages are identified as sent by the user.
     */
    public SenderDetectionMode senderDetectionMode;
    public enum SenderDetectionMode {
        COMBINED,
        SENT_MATCH,
    }

    /**
     * Whether messages identified as sent by the user should be able to 
     * activate {@link Notification}s.
     */
    public boolean checkOwnMessages;
    public static final boolean checkOwnMessagesDefault = true;

    /**
     * The sound source (and thus, volume control category) of 
     * {@link Notification} sounds.
     */
    public SoundSource soundSource;
    public static final SoundSource soundSourceDefault = SoundSource.PLAYERS;

    // Defaults

    /**
     * The default {@link TextStyle} color for new {@link Notification}s.
     */
    public int defaultColor;
    public static final int defaultColorDefault = 0xffc400;

    /**
     * The default {@link Sound} identifier for new {@link Notification}s.
     */
    public final Sound defaultSound;
    public static final Supplier<Sound> defaultSoundDefault = Sound::new;

    // Prefixes

    /**
     * The list of prefix strings to be checked when evaluating whether a 
     * message was sent by the user using the sent-match heuristic.
     */
    public final List<String> prefixes;
    public static final Supplier<List<String>> prefixesDefault =
            () -> new ArrayList<>(List.of("/shout", "/me", "!"));

    // Notifications

    /**
     * The list of all {@link Notification} instances, guaranteed to contain
     * at least {@code 1} instance.
     */
    private final List<Notification> notifications;
    private static final Supplier<List<Notification>> notificationsDefault =
            () -> new ArrayList<>(List.of(Notification.createUser()));

    /**
     * Initializes default configuration.
     */
    public Config() {
        this(
                DetectionMode.values()[0],
                DebugMode.values()[0],
                NotifMode.values()[0],
                RestyleMode.values()[0],
                SendMode.values()[0],
                SenderDetectionMode.values()[0],
                checkOwnMessagesDefault,
                soundSourceDefault,
                defaultColorDefault,
                defaultSoundDefault.get(),
                prefixesDefault.get(),
                notificationsDefault.get()
        );
    }

    /**
     * Not validated.
     */
    Config(
            DetectionMode detectionMode,
            DebugMode debugMode,
            NotifMode notifMode,
            RestyleMode restyleMode,
            SendMode sendMode,
            SenderDetectionMode senderDetectionMode,
            boolean checkOwnMessages,
            SoundSource soundSource,
            int defaultColor,
            Sound defaultSound,
            List<String> prefixes,
            List<Notification> notifications
    ) {
        this.detectionMode = detectionMode;
        this.debugMode = debugMode;
        this.notifMode = notifMode;
        this.restyleMode = restyleMode;
        this.sendMode = sendMode;
        this.senderDetectionMode = senderDetectionMode;
        this.checkOwnMessages = checkOwnMessages;
        this.soundSource = soundSource;
        this.defaultColor = defaultColor;
        this.defaultSound = defaultSound;
        this.prefixes = prefixes;
        this.notifications = notifications;
    }

    // Username

    public Notification getUserNotif() {
        validateUserNotif();
        return notifications.getFirst();
    }

    public void setProfileName(String name) {
        getUserNotif().triggers.getFirst().string = name;
    }

    public void setDisplayName(String name) {
        getUserNotif().triggers.get(1).string = name;
    }

    // Notifications

    /**
     * @return an unmodifiable view of the {@link Notification} list.
     */
    public List<Notification> getNotifs() {
        return Collections.unmodifiableList(notifications);
    }

    /**
     * Adds a new {@link Notification} with default or blank values.
     */
    public void addNotif() {
        notifications.add(Notification.createBlank(
                new Sound(defaultSound), new TextStyle(defaultColor)
        ));
    }

    /**
     * Removes the {@link Notification} at the specified index in the list, if 
     * possible
     *
     * <p><b>Note:</b> Will fail without error if the index is {@code 0}.</p>
     * @param index the index of the notification.
     * @return {@code true} if the list was modified.
     */
    public boolean removeNotif(int index) {
        if (index != 0) {
            notifications.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Removes the {@link Notification} at the source index to the destination 
     * index in the list, if possible, 
     *
     * <p><b>Note:</b> Will fail without error if either index is {@code 0}.</p>
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     * @return {@code true} if the list was modified.
     */
    public boolean moveNotif(int sourceIndex, int destIndex) {
        if (sourceIndex > 0 && destIndex > 0 && sourceIndex != destIndex) {
            notifications.add(destIndex, notifications.remove(sourceIndex));
            return true;
        }
        return false;
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Config reload() {
        instance = null;
        get();
        ChatNotify.responseMessages.clear();
        ChatNotify.updateUsernameNotif(instance);
        return instance;
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = CONFIG_DIR.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            JsonUtil.reset();
            config = load(file, GSON);
            if (config == null) {
                backup(UNREADABLE_FILE_NAME);
                ChatNotify.LOG.warn("Resetting config");
                ChatNotify.hasResetConfig = true;
            } else if (JsonUtil.hasChanged) {
                backup(OLD_FILE_NAME);
            }
        }
        return config != null ? config : new Config();
    }

    @SuppressWarnings("SameParameterValue")
    private static @Nullable Config load(Path file, Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            ChatNotify.LOG.error("Unable to load config", e);
            return null;
        }
    }

    private static void backup(String path) {
        try {
            ChatNotify.LOG.warn("Copying {} to {}", FILE_NAME, path);
            if (!Files.isDirectory(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(path);
            Files.move(file, backupFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            ChatNotify.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.validate();
        try {
            if (!Files.isDirectory(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(tempFile.toFile()), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            ChatNotify.onConfigSaved(instance);
        } catch (IOException e) {
            ChatNotify.LOG.error("Unable to save config", e);
        }
    }

    // Validation

    /**
     * Validates this instance. To be called after editing and before saving.
     */
    private Config validate() {
        // Validate defaults
        if (defaultColor < 0 || defaultColor > 0xFFFFFF) defaultColor = defaultColorDefault;
        defaultSound.validate();

        // Remove blank prefixes and sort by decreasing length
        prefixes.removeIf(String::isBlank);
        prefixes.sort(Comparator.comparingInt(String::length).reversed());

        // Validate username notification
        validateUserNotif();

        // Validate notifications and remove any blank instances except first
        notifications.removeIf((n) -> {
            n.validate();
            return (
                    n != notifications.getFirst()
                            && n.triggers.isEmpty()
                            && n.exclusionTriggers.isEmpty()
                            && n.responseMessages.isEmpty()
            );
        });

        return this;
    }

    /**
     * Validates the existence of a {@link Notification} at index 0 for the
     * user's name.
     */
    private void validateUserNotif() {
        if (notifications.isEmpty()) {
            ChatNotify.LOG.error("Username notification does not exist! Creating...");
            notifications.add(Notification.createUser());
        } else if (notifications.getFirst().triggers.size() < 2) {
            ChatNotify.LOG.error("Username notification missing triggers! Recreating...");
            notifications.set(0, Notification.createUser());
        }
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.get("version").getAsInt();
            boolean silent = version != VERSION;

            DetectionMode detectionMode = JsonUtil.getOrDefault(obj, "detectionMode",
                    DetectionMode.class, DetectionMode.values()[0], silent);

            DebugMode debugMode = JsonUtil.getOrDefault(obj, "debugMode",
                    DebugMode.class, DebugMode.values()[0], silent);

            NotifMode notifMode = JsonUtil.getOrDefault(obj, "notifMode",
                    NotifMode.class, NotifMode.values()[0], silent);

            RestyleMode restyleMode = JsonUtil.getOrDefault(obj, "restyleMode",
                    RestyleMode.class, RestyleMode.values()[0], silent);

            SendMode sendMode = JsonUtil.getOrDefault(obj, "sendMode",
                    SendMode.class, SendMode.values()[0], silent);

            SenderDetectionMode senderDetectionMode = JsonUtil.getOrDefault(obj, "senderDetectionMode",
                    SenderDetectionMode.class, SenderDetectionMode.values()[0], silent);

            boolean checkOwnMessages = JsonUtil.getOrDefault(obj, "checkOwnMessages",
                    checkOwnMessagesDefault, silent);

            SoundSource soundSource = JsonUtil.getOrDefault(obj, "soundSource",
                    SoundSource.class, soundSourceDefault, silent);

            int defaultColor = JsonUtil.getOrDefault(obj, "defaultColor",
                    defaultColorDefault, silent);

            Sound defaultSound = JsonUtil.getOrDefault(ctx, obj, "defaultSound",
                    Sound.class, defaultSoundDefault.get(), silent);

            List<String> prefixes = JsonUtil.getOrDefault(obj, "prefixes",
                    prefixesDefault.get(), silent);

            List<Notification> notifications = JsonUtil.getOrDefault(ctx, obj, "notifications",
                    Notification.class, notificationsDefault.get(), silent);

            return new Config(
                    detectionMode,
                    debugMode,
                    notifMode,
                    restyleMode,
                    sendMode,
                    senderDetectionMode,
                    checkOwnMessages,
                    soundSource,
                    defaultColor,
                    defaultSound,
                    prefixes,
                    notifications
            ).validate();
        }
    }
}
