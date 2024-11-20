/*
 * Copyright 2024 TerminalMC
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
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * <p>ChatNotify configuration options class with default values and validation.
 * </p>
 *
 * <p><b>Note:</b> The list of notifications is required to maintain a
 * notification at index 0 for the user's name. This notification is handled
 * differently in several ways.</p>
 *
 * <p>In versions prior to and including v1.2.0, serialization of config is done
 * automatically by gson, and deserialization used a single custom deserializer.
 * Starting in v1.3.0 beta versions, each configuration class requiring custom
 * serialization and/or deserialization has its own serializer and deserializer.
 * </p>
 *
 * <p>Every configuration class has a final int field "version" which can be
 * used by the class deserializer to determine how to interpret the json.</p>
 *
 * <p>Config files generated by versions 1.2.0-pre.3 to 1.2.0 are deserialized
 * by {@link IntermediaryConfigDeserializer}. Files generated by v1.2.0-pre.2
 * and earlier versions are deserialized by {@link LegacyConfigDeserializer}.
 * </p>
 */
public class Config {
    public final int version = 4;
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = ChatNotify.MOD_ID + ".json";
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Notification.class, new Notification.Deserializer())
            .registerTypeAdapter(Sound.class, new Sound.Deserializer())
            .registerTypeAdapter(TextStyle.class, new TextStyle.Deserializer())
            .registerTypeAdapter(TitleText.class, new TitleText.Deserializer())
            .registerTypeAdapter(Trigger.class, new Trigger.Deserializer())
            .registerTypeAdapter(ResponseMessage.class, new ResponseMessage.Deserializer())
            .setPrettyPrinting()
            .create();
    public static final Gson INTERMEDIARY_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new IntermediaryConfigDeserializer())
            .create();
    public static final Gson LEGACY_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new LegacyConfigDeserializer())
            .create();

    // Options

    public static final int DEFAULT_COLOR = 16761856; // #FFC400
    public static final Sound DEFAULT_SOUND = new Sound();
    public static final SoundSource DEFAULT_SOUND_SOURCE = SoundSource.PLAYERS;
    public static final List<String> DEFAULT_PREFIXES = List.of("/shout", "!");

    public TriState mixinEarly;
    public TriState debugShowKey;
    public boolean checkOwnMessages;
    public boolean compatSendMode;
    public SoundSource soundSource;
    public int defaultColor;
    public Sound defaultSound;
    public final List<String> prefixes;
    private final List<Notification> notifications;

    /**
     * Initializes default configuration with one notification for the user's
     * name.
     */
    public Config() {
        this(new TriState(), new TriState(), true, false, 
                DEFAULT_SOUND_SOURCE, DEFAULT_COLOR, DEFAULT_SOUND, 
                new ArrayList<>(DEFAULT_PREFIXES), 
                new ArrayList<>(List.of(Notification.createUser())));
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    Config(TriState mixinEarly, TriState debugShowKey, 
           boolean checkOwnMessages, boolean compatSendMode,
           SoundSource soundSource, int defaultColor, Sound defaultSound,
           List<String> prefixes, List<Notification> notifications) {
        this.mixinEarly = mixinEarly;
        this.debugShowKey = debugShowKey;
        this.checkOwnMessages = checkOwnMessages;
        this.compatSendMode = compatSendMode;
        this.soundSource = soundSource;
        this.defaultColor = defaultColor;
        this.defaultSound = defaultSound;
        this.prefixes = prefixes;
        this.notifications = notifications;
    }

    // Username

    public Notification getUserNotif() {
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
     * @return an unmodifiable view of the notification list.
     */
    public List<Notification> getNotifs() {
        return Collections.unmodifiableList(notifications);
    }

    /**
     * Adds a new notification with default values.
     */
    public void addNotif() {
        notifications.add(Notification.createBlank(new Sound(defaultSound), new TextStyle(defaultColor)));
    }

    /**
     * Removes the notification at the specified index, if possible.
     *
     * <p><b>Note:</b> Will fail without error if the specified index is 0.</p>
     * @param index the index of the notification.
     * @return {@code true} if a notification was removed, {@code false}
     * otherwise.
     */
    public boolean removeNotif(int index) {
        if (index != 0) {
            notifications.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Moves the {@link Notification} at the source index to the destination
     * index.
     *
     * <p><b>Note:</b> Will fail without error if either index is 0.</p>
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     */
    public void changeNotifPriority(int sourceIndex, int destIndex) {
        if (sourceIndex > 0 && destIndex > 0 && sourceIndex != destIndex) {
            notifications.add(destIndex, notifications.remove(sourceIndex));
        }
    }

    public void cleanup() {
        // Remove blank prefixes and sort by decreasing length
        prefixes.removeIf(String::isBlank);
        prefixes.sort(Comparator.comparingInt(String::length).reversed());

        Notification notif;
        Iterator<Notification> iterNotifs = notifications.iterator();

        // Username notification (cannot be removed)
        notif = iterNotifs.next();
        notif.purgeTriggers();
        notif.purgeExclusionTriggers();
        notif.purgeResponseMessages();
        notif.autoDisable();

        // All other notifications
        while (iterNotifs.hasNext()) {
            notif = iterNotifs.next();
            notif.purgeTriggers();
            notif.purgeExclusionTriggers();
            notif.purgeResponseMessages();

            if (notif.triggers.isEmpty() && notif.exclusionTriggers.isEmpty()
                    && notif.responseMessages.isEmpty()) {
                iterNotifs.remove();
            } else {
                notif.autoDisable();
            }
        }
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) { // Fallback to intermediary
                ChatNotify.LOG.info("Attempting deserialization using intermediary deserializer.");
                config = load(file, INTERMEDIARY_GSON);
            }
            if (config == null) { // Fallback to legacy
                ChatNotify.LOG.info("Attempting deserialization using legacy deserializer.");
                config = load(file, LEGACY_GSON);
            }
        }
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            ChatNotify.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.cleanup();
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            ChatNotify.onConfigSaved(instance);
        } catch (IOException e) {
            ChatNotify.LOG.error("Unable to save config.", e);
        }
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.get("version").getAsInt();

            TriState mixinEarly = version == 1
                    ? new TriState(obj.get("mixinEarly").getAsBoolean() ? TriState.State.ON : TriState.State.DISABLED)
                    : ctx.deserialize(obj.get("mixinEarly"), TriState.class);
            TriState debugShowKey = version == 1
                    ? new TriState(obj.get("debugShowKey").getAsBoolean() ? TriState.State.ON : TriState.State.DISABLED)
                    : ctx.deserialize(obj.get("debugShowKey"), TriState.class);
            boolean checkOwnMessages = obj.get("checkOwnMessages").getAsBoolean();
            boolean compatSendMode = version >= 4 ? obj.get("compatSendMode").getAsBoolean() : false;
            SoundSource soundSource = SoundSource.valueOf(obj.get("soundSource").getAsString());
            int defaultColor = obj.get("defaultColor").getAsInt();
            Sound defaultSound = ctx.deserialize(obj.get("defaultSound"), Sound.class);
            List<String> prefixes = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("prefixes")) {
                prefixes.add(je.getAsString());
            }
            List<Notification> notifications = new ArrayList<>();
            for (JsonElement je : obj.getAsJsonArray("notifications")) {
                Notification n = ctx.deserialize(je, Notification.class);
                if (n != null) notifications.add(n);
            }

            // Validation
            if (mixinEarly == null) throw new JsonParseException("Config #1");
            if (debugShowKey == null) throw new JsonParseException("Config #2");
            if (defaultColor < 0 || defaultColor > 16777215) throw new JsonParseException("Config #4");
            if (defaultSound == null) throw new JsonParseException("Config #3");

            if (notifications.isEmpty()) {
                notifications.add(Notification.createUser());
            } else if (notifications.getFirst().triggers.size() < 2) {
                notifications.set(0, Notification.createUser());
            }

            return new Config(mixinEarly, debugShowKey, checkOwnMessages, compatSendMode, 
                    soundSource, defaultColor, defaultSound, prefixes, notifications);
        }
    }
}
