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

package dev.terminalmc.chatnotify.gui.screen;

import dev.terminalmc.chatnotify.config.Config;
import dev.terminalmc.chatnotify.config.Notification;
import dev.terminalmc.chatnotify.config.ResponseMessage;
import dev.terminalmc.chatnotify.config.Trigger;
import dev.terminalmc.chatnotify.gui.widget.field.TextField;
import dev.terminalmc.chatnotify.gui.widget.list.FilterList;
import dev.terminalmc.chatnotify.gui.widget.list.root.notif.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static dev.terminalmc.chatnotify.util.Localization.localized;
import static dev.terminalmc.chatnotify.util.Localization.translationKey;

public class NotifOptionsScreen extends OptionScreen {
    private final Notification notif;

    public NotifOptionsScreen(Screen lastScreen, Notification notif) {
        this(lastScreen, notif, TabKey.TRIGGERS.key);
    }
    
    public NotifOptionsScreen(Screen lastScreen, Notification notif, String defaultKey) {
        super(lastScreen);
        this.notif = notif;
        addTabs(defaultKey);
    }

    private void addTabs(String defaultKey) {
        List<Tab> tabs = List.of(
                new Tab(TabKey.TRIGGERS.key, (screen) -> {
                    Notification notif = cast(screen).notif;
                    return new FilterList<>(Minecraft.getInstance(), 0, 0, 0,
                            BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                            FilterList.Entry.TriggerOptions.class,
                            (source, dest) -> notif == Config.get().getUserNotif()
                                    ? notif.moveTrigger(source + 2, dest + 2) 
                                    : notif.moveTrigger(source, dest),
                            localized("option", "notif.trigger.list", "ℹ"),
                            localized("option", "notif.trigger.list.tooltip"),
                            null,
                            null,
                            () -> notif.triggers,
                            (x, width, height, list, trigger, index) -> {
                                if (notif.equals(Config.get().getUserNotif()) && index <= 1) {
                                    return new FilterList.Entry.LockedTriggerOptions(
                                            x, width, height, trigger, index == 0 
                                            ? localized("option", 
                                            "notif.trigger.list.special.profile_name") 
                                            : localized("option", 
                                            "notif.trigger.list.special.display_name"));
                                } else {
                                    return new FilterList.Entry.TriggerOptions(
                                            x, width, height, list, trigger, notif.textStyle, index,
                                            (i) -> notif.triggers.remove((int) i),
                                            new TextField.Validator.UniqueTrigger(
                                                    () -> Config.get().getNotifs(),
                                                    (n) -> n.triggers, notif, trigger), true);
                                }
                            },
                            (x, width, height, list, trigger) -> trigger.styleTarget.enabled
                                    ? new FilterList.Entry.StyleTargetOptions(
                                        x, width, height, list, trigger.styleTarget)
                                    : null,
                            () -> notif.triggers.add(new Trigger())
                    );
                }),
                new Tab(TabKey.FORMAT.key, (screen) ->
                        new FormatList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                                cast(screen).notif
                        )),
                new Tab(TabKey.SOUND.key, (screen) ->
                        new SoundOptionList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT,
                                cast(screen).notif.sound
                        )),
                new Tab(TabKey.INCLUSION.key, (screen) -> {
                    Notification notif = cast(screen).notif;
                    return new FilterList<>(Minecraft.getInstance(), 0, 0, 0,
                            BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                            FilterList.Entry.TriggerOptions.class,
                            notif::moveInclusionTrigger,
                            localized("option", "notif.inclusion.list", "ℹ"),
                            localized("option", "notif.inclusion.list.tooltip"),
                            () -> notif.inclusionEnabled,
                            (status) -> notif.inclusionEnabled = status,
                            () -> notif.inclusionTriggers,
                            (x, width, height, list, trigger, index) ->
                                    new FilterList.Entry.TriggerOptions(
                                            x, width, height, list, trigger, notif.textStyle, index,
                                            (i) -> notif.inclusionTriggers.remove((int) i),
                                            new TextField.Validator.UniqueTrigger(
                                                    () -> List.of(notif),
                                                    (n) -> n.inclusionTriggers,
                                                    notif, trigger), false),
                            null,
                            () -> notif.inclusionTriggers.add(new Trigger())
                    );
                }),
                new Tab(TabKey.EXCLUSION.key, (screen) -> {
                    Notification notif = cast(screen).notif;
                    return new FilterList<>(Minecraft.getInstance(), 0, 0, 0,
                            BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                            FilterList.Entry.TriggerOptions.class,
                            notif::moveExclusionTrigger,
                            localized("option", "notif.exclusion.list", "ℹ"),
                            localized("option", "notif.exclusion.list.tooltip"),
                            () -> notif.exclusionEnabled,
                            (status) -> notif.exclusionEnabled = status,
                            () -> notif.exclusionTriggers,
                            (x, width, height, list, trigger, index) ->
                                    new FilterList.Entry.TriggerOptions(
                                            x, width, height, list, trigger, notif.textStyle, index, 
                                            (i) -> notif.exclusionTriggers.remove((int) i), 
                                            new TextField.Validator.UniqueTrigger(
                                                    () -> List.of(notif),
                                                    (n) -> n.exclusionTriggers,
                                                    notif, trigger), false),
                            null,
                            () -> notif.exclusionTriggers.add(new Trigger())
                    );
                }),
                new Tab(TabKey.RESPONSES.key, (screen) -> {
                    Notification notif = cast(screen).notif;
                    return new FilterList<>(Minecraft.getInstance(), 0, 0, 0,
                            BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                            FilterList.Entry.ResponseOptions.class,
                            notif::moveResponseMessage,
                            localized("option", "notif.response.list", "ℹ"),
                            localized("option", "notif.response.list.tooltip"),
                            () -> notif.responseEnabled,
                            (status) -> notif.responseEnabled = status,
                            () -> notif.responseMessages,
                            (x, width, height, list, trigger, index) ->
                                    new FilterList.Entry.ResponseOptions(
                                            x, width, height, list, trigger, index,
                                            (i) -> notif.responseMessages.remove((int)i)),
                            null,
                            () -> notif.responseMessages.add(new ResponseMessage())
                    );
                }),
                new Tab(TabKey.MISC.key, (screen) ->
                        new MiscOptionList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                                cast(screen).notif
                        ))
        );
        super.setTabs(tabs, defaultKey);
    }

    public enum TabKey {
        TRIGGERS(translationKey("option", "notif.trigger")),
        FORMAT(translationKey("option", "notif.format")),
        SOUND(translationKey("option", "notif.sound")),
        INCLUSION(translationKey("option", "notif.inclusion")),
        EXCLUSION(translationKey("option", "notif.exclusion")),
        RESPONSES(translationKey("option", "notif.response")),
        MISC(translationKey("option", "notif.misc"));

        public final String key;
        TabKey(String key) {
            this.key = key;
        }
    }

    private static NotifOptionsScreen cast(OptionScreen screen) {
        if (!(screen instanceof NotifOptionsScreen s)) throw new IllegalArgumentException(
                String.format("Option list supplier for class %s cannot use screen type %s",
                        NotifOptionsScreen.class.getName(), screen.getClass().getName()));
        return s;
    }

    @Override
    public void onClose() {
        notif.editing = false;
        super.onClose();
    }
}
