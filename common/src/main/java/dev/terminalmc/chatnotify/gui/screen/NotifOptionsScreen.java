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
import dev.terminalmc.chatnotify.gui.widget.list.*;
import dev.terminalmc.chatnotify.gui.widget.list.notif.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static dev.terminalmc.chatnotify.util.Localization.localized;

/**
 * Options screen for a {@link Notification}.
 */
public class NotifOptionsScreen extends OptionScreen {
    private final Notification notif;
    private final Tab defaultTab;
    
    public NotifOptionsScreen(Screen lastScreen, Notification notif, Tab defaultTab) {
        super(lastScreen);
        this.notif = notif;
        this.defaultTab = defaultTab;
        addTabs();
    }

    private void addTabs() {
        List<TabButton> tabs = Arrays.stream(Tab.values()).map((tab) ->
                new TabButton(tab.title, () -> tab.getList(this))).toList();
        super.setTabs(tabs, Arrays.stream(Tab.values()).toList().indexOf(defaultTab));
    }

    public enum Tab {
        TRIGGERS(localized("option", "notif.trigger"), (screen) ->
                new TriggerList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                        screen.notif
                )),
        FORMAT(localized("option", "notif.format"), (screen) ->
                new FormatList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                        screen.notif
                )),
        SOUND(localized("option", "notif.sound"), (screen) ->
                new SoundOptionList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT,
                        screen.notif.sound
                )),
        INCLUSION(localized("option", "notif.inclusion"), (screen) ->
                new InclusionTriggerList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                        screen.notif
                )),
        EXCLUSION(localized("option", "notif.exclusion"), (screen) ->
                new ExclusionTriggerList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                        screen.notif
                )),
        RESPONSES(localized("option", "notif.response"), (screen) ->
                new ResponseMessageList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                        screen.notif
                )),
        MISC(localized("option", "notif.misc"), (screen) ->
                new MiscOptionList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                        screen.notif
                ));
        
        final Component title;
        private final Function<NotifOptionsScreen, @NotNull OptionList> supplier;
        private @Nullable OptionList list = null;

        Tab(Component title, Function<NotifOptionsScreen, OptionList> supplier) {
            this.title = title;
            this.supplier = supplier;
        }

        public @NotNull OptionList getList(NotifOptionsScreen screen) {
            if (list == null) {
                list = supplier.apply(screen);
            }
            return list;
        }
    }

    @Override
    public void onClose() {
        notif.editing = false;
        super.onClose();
    }
}
