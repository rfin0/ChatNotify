/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.chatnotify.config;

import com.google.gson.*;
import net.minecraft.sounds.SoundSource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatible with config files generated by ChatNotify versions 1.0.2 to
 * 1.2.0-pre.2 (inclusive).
 */
public class LegacyConfigDeserializer implements JsonDeserializer<Config> {
    @Override
    public Config deserialize(JsonElement jsonGuiEventListener, Type type,
                              JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = jsonGuiEventListener.getAsJsonObject();

        TriState mixinEarly = new TriState(obj.has("mixinEarly")
                && obj.get("mixinEarly").getAsBoolean() ? TriState.State.ON : TriState.State.DISABLED);
        TriState debugShowKey = new TriState();
        boolean checkOwnMessages = !obj.get("ignoreOwnMessages").getAsBoolean();
        SoundSource soundSource = obj.has("notifSoundSource") ?
                SoundSource.valueOf(obj.get("notifSoundSource").getAsString()) :
                Config.DEFAULT_SOUND_SOURCE;
        boolean allowRegex = false;
        int defaultColor = Config.DEFAULT_COLOR;
        Sound defaultSound = Config.DEFAULT_SOUND;
        List<String> messagePrefixes = new ArrayList<>(
                obj.getAsJsonArray("messagePrefixes")
                        .asList().stream().map(JsonElement::getAsString).toList());
        List<Notification> notifications = new ArrayList<>();

        for (JsonElement je : obj.get("notifications").getAsJsonArray()) {
            JsonObject notifObj = je.getAsJsonObject();

            boolean regexEnabled = notifObj.get("regexEnabled").getAsBoolean();
            allowRegex = allowRegex || regexEnabled;

            // Legacy-only data

            boolean triggerIsKey;
            ArrayList<Boolean> controls = new ArrayList<>();
            ArrayList<Boolean> formatControls = new ArrayList<>();
            ArrayList<String> triggerStrings = new ArrayList<>();
            ArrayList<String> exclusionTriggerStrings = new ArrayList<>();

            triggerIsKey = notifObj.get("triggerIsKey").getAsBoolean();

            for (JsonElement je2 : notifObj.get("controls").getAsJsonArray()) {
                controls.add(je2.getAsBoolean());
            }

            for (JsonElement je2 : notifObj.get("formatControls").getAsJsonArray()) {
                formatControls.add(je2.getAsBoolean());
            }

            for (JsonElement je2 : notifObj.get("triggers").getAsJsonArray()) {
                triggerStrings.add(je2.getAsString());
            }

            for (JsonElement je2 : notifObj.get("exclusionTriggers").getAsJsonArray()) {
                exclusionTriggerStrings.add(je2.getAsString());
            }

            // Current data

            boolean enabled;
            boolean exclusionEnabled;
            boolean responseEnabled;
            Sound sound;
            TextStyle textStyle;
            List<Trigger> triggers = new ArrayList<>();
            List<Trigger> exclusionTriggers = new ArrayList<>();
            List<ResponseMessage> responseMessages = new ArrayList<>();

            enabled = notifObj.get("enabled").getAsBoolean();

            exclusionEnabled = notifObj.get("exclusionEnabled").getAsBoolean();

            responseEnabled = notifObj.get("responseEnabled").getAsBoolean();

            String id = Sound.DEFAULT_SOUND_ID;
            JsonObject soundObj = notifObj.get("sound").getAsJsonObject();
            if (soundObj.has("path")) { // NeoForge
                id = soundObj.get("path").getAsString();
            }
            else if (soundObj.has("field_13355")) { // Fabric
                id = soundObj.get("field_13355").getAsString();
            }
            else if (soundObj.has("f_135805_")) {
                id = soundObj.get("f_135805_").getAsString(); // Forge
            }
            sound = new Sound(
                    controls.get(2),
                    id,
                    notifObj.get("soundVolume").getAsFloat(),
                    notifObj.get("soundPitch").getAsFloat());

            int color = Config.DEFAULT_COLOR;
            if (notifObj.has("color")) {
                JsonObject colorObj = notifObj.get("color").getAsJsonObject();
                if (colorObj.has("value")) { // NeoForge
                    color = colorObj.get("value").getAsInt();
                }
                else if (colorObj.has("field_24364")) { // Fabric
                    color = colorObj.get("field_24364").getAsInt();
                }
                else if (colorObj.has("f_131257_")) {
                    color = colorObj.get("f_131257_").getAsInt(); // Forge
                }
            }
            textStyle = new TextStyle(
                    controls.getFirst(),
                    color,
                    formatControls.get(0) ? new TriState(TriState.State.ON) : new TriState(TriState.State.DISABLED),
                    formatControls.get(1) ? new TriState(TriState.State.ON) : new TriState(TriState.State.DISABLED),
                    formatControls.get(2) ? new TriState(TriState.State.ON) : new TriState(TriState.State.DISABLED),
                    formatControls.get(3) ? new TriState(TriState.State.ON) : new TriState(TriState.State.DISABLED),
                    formatControls.get(4) ? new TriState(TriState.State.ON) : new TriState(TriState.State.DISABLED));

            for (String triggerStr : triggerStrings) {
                triggers.add(new Trigger(true, triggerStr, null, triggerIsKey, regexEnabled));
            }

            for (String exclTriggerStr : exclusionTriggerStrings) {
                exclusionTriggers.add(new Trigger(true, exclTriggerStr, null, triggerIsKey, regexEnabled));
            }

            for (JsonElement je2 : notifObj.get("responseMessages").getAsJsonArray()) {
                responseMessages.add(new ResponseMessage(true, je2.getAsString(), false, 0));
            }

            notifications.add(new Notification(enabled, exclusionEnabled, responseEnabled,
                    sound, textStyle, triggers, exclusionTriggers, responseMessages));
        }

        // Ensure username Notification is valid
        if (notifications.isEmpty()) {
            notifications.add(Notification.createUser());
        }
        else if (notifications.getFirst().triggers.size() < 2) {
            notifications.set(0, Notification.createUser());
        }

        return new Config(mixinEarly, debugShowKey, checkOwnMessages, soundSource,
                allowRegex, defaultColor, defaultSound, messagePrefixes, notifications);
    }
}
