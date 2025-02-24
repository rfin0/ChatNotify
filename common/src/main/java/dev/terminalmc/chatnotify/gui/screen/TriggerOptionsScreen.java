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

import dev.terminalmc.chatnotify.config.TextStyle;
import dev.terminalmc.chatnotify.config.Trigger;
import dev.terminalmc.chatnotify.gui.widget.list.*;
import dev.terminalmc.chatnotify.gui.widget.list.trigger.KeySelectorList;
import dev.terminalmc.chatnotify.gui.widget.list.trigger.TriggerEditorList;
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
 * Options screen for a {@link Trigger}.
 */
public class TriggerOptionsScreen extends OptionScreen {
    private final Trigger trigger;
    private final TextStyle textStyle;
    private final Runnable onClose;
    private final Tab defaultTab;
    
    public TriggerOptionsScreen(Screen lastScreen, Trigger trigger, TextStyle textStyle,
                                Runnable onClose, Tab defaultTab) {
        super(lastScreen);
        this.trigger = trigger;
        this.textStyle = textStyle;
        this.onClose = onClose;
        this.defaultTab = defaultTab;
        addTabs();
    }

    private void addTabs() {
        List<TabButton> tabs = Arrays.stream(Tab.values()).map((tab) ->
                new TabButton(tab.title, () -> tab.getList(this))).toList();
        super.setTabs(tabs, Arrays.stream(Tab.values()).toList().indexOf(defaultTab));
    }

    public enum Tab {
        TRIGGER_EDITOR(localized("option", "notif.trigger.editor"), (screen) ->
                new TriggerEditorList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                        screen.trigger, screen.textStyle
                )),
        KEY_SELECTOR(localized("option", "notif.trigger.selector"), (screen) ->
                new KeySelectorList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT,
                        screen.trigger
                ));

        final Component title;
        private final Function<TriggerOptionsScreen, @NotNull OptionList> supplier;
        private @Nullable OptionList list = null;

        Tab(Component title, Function<TriggerOptionsScreen, OptionList> supplier) {
            this.title = title;
            this.supplier = supplier;
        }

        public @NotNull OptionList getList(TriggerOptionsScreen screen) {
            if (list == null) {
                list = supplier.apply(screen);
            }
            return list;
        }
    }

    @Override
    public void onClose() {
        onClose.run();
        super.onClose();
    }
}
