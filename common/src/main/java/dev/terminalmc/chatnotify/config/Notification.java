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
import dev.terminalmc.chatnotify.util.Functional;
import dev.terminalmc.chatnotify.util.JsonUtil;

import java.util.*;
import java.util.function.Supplier;

/**
 * Consists of:
 *
 * <p>A range of controls (boolean or enum) defining behavior.</p>
 *
 * <p>A {@link Sound} instance, defining the sound to be played on activation,
 * with volume and pitch controls.</p>
 *
 * <p>A {@link TextStyle} instance, defining how the triggering message should 
 * be restyled.</p>
 *
 * <p>A series of custom message strings, to be optionally displayed to the user
 * in various different ways on activation.</p>
 *
 * <p>A list of {@link Trigger} instances, to be checked against incoming 
 * messages to determine whether the {@link Notification} should be activated.
 * </p>
 *
 * <p>A list of inclusion {@link Trigger} instances which, if not matched,
 * prevent activation.</p>
 *
 * <p>A list of exclusion {@link Trigger} instances which, if matched, prevent
 * activation.</p>
 *
 * <p>A list of {@link ResponseMessage} instances, to be sent on activation.</p>
 */
public class Notification implements Functional.StringSupplier {
    public static final int VERSION = 7;
    public final int version = VERSION;

    /**
     * A status flag to indicate that this instance is being edited, and should 
     * not be activated irrespective of {@link Notification#enabled}.
     */
    public transient boolean editing = false;

    // Options

    /**
     * Whether this instance is set by the user to be eligible for activation.
     *
     * <p>Necessary but not sufficient condition; never activate if this is 
     * {@code false}, but it being {@code true} does not mean that this instance
     * can be safely activated. Instead, check {@link Notification#canActivate}.
     * </p>
     */
    public boolean enabled;
    public static final boolean enabledDefault = true;

    /**
     * Controls whether this instance is eligible for activation on user-sent 
     * messages.
     *
     * <p>{@link CheckOwnMode#DEFER} means that {@link Config#checkOwnMessages}
     * should be used instead.</p>
     */
    public CheckOwnMode checkOwnMode;
    public enum CheckOwnMode {
        DEFER,
        ON,
        OFF,
    }

    /**
     * Whether this instance allows use of inclusion triggers.
     */
    public boolean inclusionEnabled;
    public static final boolean inclusionEnabledDefault = false;

    /**
     * Whether this instance allows use of exclusion triggers.
     */
    public boolean exclusionEnabled;
    public static final boolean exclusionEnabledDefault = false;

    /**
     * Whether this instance allows use of response messages.
     */
    public boolean responseEnabled;
    public static final boolean responseEnabledDefault = false;

    /**
     * The {@link Sound} to play on activation.
     */
    public final Sound sound;
    public static final Supplier<Sound> soundDefault = Sound::new;

    /**
     * The {@link TextStyle} to use for restyling messages on activation.
     */
    public final TextStyle textStyle;
    public static final Supplier<TextStyle> textStyleDefault = TextStyle::new;

    /**
     * Optional message to replace the triggering chat message.
     */
    public String replacementMsg;
    public static final String replacementMsgDefault = "";
    public boolean replacementMsgEnabled;
    public static final boolean replacementMsgEnabledDefault = false;

    /**
     * Optional message to display in the status bar (above the hotbar).
     */
    public String statusBarMsg;
    public static final String statusBarMsgDefault = "";
    public boolean statusBarMsgEnabled;
    public static final boolean statusBarMsgEnabledDefault = false;

    /**
     * Optional message to display in title text (large-font, center-screen).
     */
    public String titleMsg;
    public static final String titleMsgDefault = "";
    public boolean titleMsgEnabled;
    public static final boolean titleMsgEnabledDefault = false;

    /**
     * Optional message to display in a toast popup.
     */
    public String toastMsg;
    public static final String toastMsgDefault = "";
    public boolean toastMsgEnabled;
    public static final boolean toastMsgEnabledDefault = false;

    /**
     * The list of {@link Trigger}s which can activate this instance.
     */
    public final List<Trigger> triggers;
    public static final Supplier<List<Trigger>> triggersDefault = ArrayList::new;

    /**
     * The list of {@link Trigger}s which are required for activation of this 
     * instance.
     *
     * <p><b>Note:</b> For simplicity, this list uses the same {@link Trigger}
     * class as {@link Notification#triggers}. However, instances in this list
     * will never use the {@link Trigger#styleTarget} capability.</p>
     */
    public final List<Trigger> inclusionTriggers;
    public static final Supplier<List<Trigger>> inclusionTriggersDefault = ArrayList::new;

    /**
     * The list of {@link Trigger}s which prevent activation of this instance.
     *
     * <p><b>Note:</b> For simplicity, this list uses the same {@link Trigger}
     * class as {@link Notification#triggers}. However, instances in this list
     * will never use the {@link Trigger#styleTarget} capability.</p>
     */
    public final List<Trigger> exclusionTriggers;
    public static final Supplier<List<Trigger>> exclusionTriggersDefault = ArrayList::new;

    /**
     * The list of {@link ResponseMessage}s to be sent on activation.
     */
    public final List<ResponseMessage> responseMessages;
    public static final Supplier<List<ResponseMessage>> responseMessagesDefault = ArrayList::new;

    /**
     * Not validated.
     */
    Notification(
            boolean enabled,
            CheckOwnMode checkOwnMode,
            boolean inclusionEnabled,
            boolean exclusionEnabled,
            boolean responseEnabled,
            Sound sound,
            TextStyle textStyle,
            String replacementMsg,
            boolean replacementMsgEnabled,
            String statusBarMsg,
            boolean statusBarMsgEnabled,
            String titleMsg,
            boolean titleMsgEnabled,
            String toastMsg,
            boolean toastMsgEnabled,
            List<Trigger> triggers,
            List<Trigger> inclusionTriggers,
            List<Trigger> exclusionTriggers,
            List<ResponseMessage> responseMessages
    ) {
        this.enabled = enabled;
        this.checkOwnMode = checkOwnMode;
        this.inclusionEnabled = inclusionEnabled;
        this.exclusionEnabled = exclusionEnabled;
        this.responseEnabled = responseEnabled;
        this.sound = sound;
        this.textStyle = textStyle;
        this.replacementMsg = replacementMsg;
        this.replacementMsgEnabled = replacementMsgEnabled;
        this.statusBarMsg = statusBarMsg;
        this.statusBarMsgEnabled = statusBarMsgEnabled;
        this.titleMsg = titleMsg;
        this.titleMsgEnabled = titleMsgEnabled;
        this.toastMsg = toastMsg;
        this.toastMsgEnabled = toastMsgEnabled;
        this.triggers = triggers;
        this.inclusionTriggers = inclusionTriggers;
        this.exclusionTriggers = exclusionTriggers;
        this.responseMessages = responseMessages;
    }

    /**
     * Creates a new {@link Notification} for the user's name, with two default 
     * placeholder {@link Trigger}s.
     */
    static Notification createUser() {
        return new Notification(
                enabledDefault,
                CheckOwnMode.values()[0],
                inclusionEnabledDefault,
                exclusionEnabledDefault,
                responseEnabledDefault,
                soundDefault.get(),
                textStyleDefault.get(),
                replacementMsgDefault,
                replacementMsgEnabledDefault,
                statusBarMsgDefault,
                statusBarMsgEnabledDefault,
                titleMsgDefault,
                titleMsgEnabledDefault,
                toastMsgDefault,
                toastMsgEnabledDefault,
                new ArrayList<>(List.of(
                        new Trigger("Profile name"),
                        new Trigger("Display name")
                )),
                inclusionTriggersDefault.get(),
                exclusionTriggersDefault.get(),
                responseMessagesDefault.get()
        );
    }

    /**
     * Creates a new generic {@link Notification}, with a single blank 
     * {@link Trigger}.
     */
    static Notification createBlank(Sound sound, TextStyle textStyle) {
        return new Notification(
                enabledDefault,
                CheckOwnMode.values()[0],
                inclusionEnabledDefault,
                exclusionEnabledDefault,
                responseEnabledDefault,
                sound,
                textStyle,
                replacementMsgDefault,
                replacementMsgEnabledDefault,
                statusBarMsgDefault,
                statusBarMsgEnabledDefault,
                titleMsgDefault,
                titleMsgEnabledDefault,
                toastMsgDefault,
                toastMsgEnabledDefault,
                new ArrayList<>(List.of(
                        new Trigger("")
                )),
                inclusionTriggersDefault.get(),
                exclusionTriggersDefault.get(),
                responseMessagesDefault.get()
        );
    }

    /**
     * @return {@code true} if this instance is eligible for activation (on a
     * message sent by the user if {@code ownMsg} is {@code true}).
     */
    public boolean canActivate(boolean ownMsg) {
        if (enabled && !editing) {
            if (ownMsg) {
                return switch(checkOwnMode) {
                    case DEFER -> Config.get().checkOwnMessages;
                    case ON -> true;
                    case OFF -> false;
                };
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    // List reordering

    /**
     * Moves the {@link Trigger} at the source index to the destination index 
     * in the list.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     * @return {@code true} if the list was modified.
     */
    public boolean moveTrigger(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            triggers.add(destIndex, triggers.remove(sourceIndex));
            return true;
        }
        return false;
    }

    /**
     * Moves the inclusion {@link Trigger} at the source index to the
     * destination index in the list.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     * @return {@code true} if the list was modified.
     */
    public boolean moveInclusionTrigger(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            inclusionTriggers.add(destIndex, inclusionTriggers.remove(sourceIndex));
            return true;
        }
        return false;
    }

    /**
     * Moves the exclusion {@link Trigger} at the source index to the
     * destination index in the list.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     * @return {@code true} if the list was modified.
     */
    public boolean moveExclusionTrigger(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            exclusionTriggers.add(destIndex, exclusionTriggers.remove(sourceIndex));
            return true;
        }
        return false;
    }

    /**
     * Moves the {@link ResponseMessage} at the source index to the destination
     * index in the list.
     * @param sourceIndex the index of the element to move.
     * @param destIndex the desired final index of the element.
     * @return {@code true} if the list was modified.
     */
    public boolean moveResponseMessage(int sourceIndex, int destIndex) {
        if (sourceIndex != destIndex) {
            responseMessages.add(destIndex, responseMessages.remove(sourceIndex));
            return true;
        }
        return false;
    }

    /**
     * Options UI utility.
     * @return a string formed by concatenating all {@link Trigger} strings
     * together.
     */
    @Override
    public String getString() {
        StringBuilder sb = new StringBuilder();
        for (Trigger t : triggers) {
            sb.append(t.getString());
        }
        return sb.toString();
    }

    // Validation

    /**
     * Validates this instance. To be called after editing and before saving.
     */
    Notification validate() {
        textStyle.validate();
        sound.validate();

        triggers.removeIf(t -> {
            t.validate();
            return t.string.isBlank();
        });

        inclusionTriggers.removeIf(t -> {
            t.validate();
            return t.string.isBlank();
        });

        exclusionTriggers.removeIf(t -> {
            t.validate();
            return t.string.isBlank();
        });

        responseMessages.removeIf(m -> {
            m.validate();
            return m.string.isBlank();
        });

        return this;
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Notification> {
        @Override
        public Notification deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            int version = obj.get("version").getAsInt();
            boolean silent = version != VERSION;

            boolean enabled = JsonUtil.getOrDefault(obj, "enabled",
                    enabledDefault, silent);

            boolean inclusionEnabled = JsonUtil.getOrDefault(obj, "inclusionEnabled",
                    inclusionEnabledDefault, silent);

            boolean exclusionEnabled = JsonUtil.getOrDefault(obj, "exclusionEnabled",
                    exclusionEnabledDefault, silent);

            CheckOwnMode checkOwnMode = JsonUtil.getOrDefault(obj, "checkOwnMode",
                    CheckOwnMode.class, CheckOwnMode.values()[0], silent);

            boolean responseEnabled = JsonUtil.getOrDefault(obj, "responseEnabled",
                    responseEnabledDefault, silent);

            Sound sound = JsonUtil.getOrDefault(ctx, obj, "sound",
                    Sound.class, soundDefault.get(), silent);

            TextStyle textStyle = JsonUtil.getOrDefault(ctx, obj, "textStyle",
                    TextStyle.class, textStyleDefault.get(), silent);

            String replacementMsg = JsonUtil.getOrDefault(obj, "replacementMsg",
                    replacementMsgDefault, silent);

            boolean replacementMsgEnabled = JsonUtil.getOrDefault(obj, "replacementMsgEnabled",
                    replacementMsgEnabledDefault, silent);

            String statusBarMsg = JsonUtil.getOrDefault(obj, "statusBarMsg",
                    statusBarMsgDefault, silent);

            boolean statusBarMsgEnabled = JsonUtil.getOrDefault(obj, "statusBarMsgEnabled",
                    statusBarMsgEnabledDefault, silent);

            String titleMsg = JsonUtil.getOrDefault(obj, "titleMsg",
                    titleMsgDefault, silent);

            boolean titleMsgEnabled = JsonUtil.getOrDefault(obj, "titleMsgEnabled",
                    titleMsgEnabledDefault, silent);

            String toastMsg = JsonUtil.getOrDefault(obj, "toastMsg",
                    toastMsgDefault, silent);

            boolean toastMsgEnabled = JsonUtil.getOrDefault(obj, "toastMsgEnabled",
                    toastMsgEnabledDefault, silent);

            List<Trigger> triggers = JsonUtil.getOrDefault(ctx, obj, "triggers",
                    Trigger.class, triggersDefault.get(), silent);

            List<Trigger> inclusionTriggers = JsonUtil.getOrDefault(ctx, obj, "inclusionTriggers",
                    Trigger.class, inclusionTriggersDefault.get(), silent);

            List<Trigger> exclusionTriggers = JsonUtil.getOrDefault(ctx, obj, "exclusionTriggers",
                    Trigger.class, exclusionTriggersDefault.get(), silent);

            List<ResponseMessage> responseMessages = JsonUtil.getOrDefault(ctx, obj, "responseMessages",
                    ResponseMessage.class, responseMessagesDefault.get(), silent);
            if (version <= 3) {
                int totalDelay = 0;
                for (ResponseMessage resMsg : responseMessages) {
                    resMsg.delayTicks -= totalDelay;
                    totalDelay += resMsg.delayTicks;
                }
            }

            return new Notification(
                    enabled,
                    checkOwnMode,
                    inclusionEnabled,
                    exclusionEnabled,
                    responseEnabled,
                    sound,
                    textStyle,
                    replacementMsg,
                    replacementMsgEnabled,
                    statusBarMsg,
                    statusBarMsgEnabled,
                    titleMsg,
                    titleMsgEnabled,
                    toastMsg,
                    toastMsgEnabled,
                    triggers,
                    inclusionTriggers,
                    exclusionTriggers,
                    responseMessages
            ).validate();
        }
    }
}