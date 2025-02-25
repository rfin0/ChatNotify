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

import dev.terminalmc.chatnotify.config.Notification;
import dev.terminalmc.chatnotify.gui.widget.list.notif.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static dev.terminalmc.chatnotify.util.Localization.translationKey;

/**
 * Options screen for a {@link Notification}.
 */
public class NotifOptionsScreen extends OptionScreen {
    private final Notification notif;
    
    public NotifOptionsScreen(Screen lastScreen, Notification notif, String defaultKey) {
        super(lastScreen);
        this.notif = notif;
        addTabs(defaultKey);
    }

    private void addTabs(String defaultKey) {
        List<Tab> tabs = List.of(
                new Tab(TabKey.TRIGGERS.key, (screen) ->
                        new TriggerList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                                cast(screen).notif
                        )),
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
                new Tab(TabKey.INCLUSION.key, (screen) ->
                        new InclusionTriggerList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                                cast(screen).notif
                        )),
                new Tab(TabKey.EXCLUSION.key, (screen) ->
                        new ExclusionTriggerList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                                cast(screen).notif
                        )),
                new Tab(TabKey.RESPONSES.key, (screen) ->
                        new ResponseMessageList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                                cast(screen).notif
                        )),
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
