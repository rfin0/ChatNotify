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
import dev.terminalmc.chatnotify.gui.widget.list.trigger.KeySelectorList;
import dev.terminalmc.chatnotify.gui.widget.list.trigger.TriggerEditorList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

import static dev.terminalmc.chatnotify.util.Localization.translationKey;

/**
 * Options screen for a {@link Trigger}.
 */
public class TriggerOptionsScreen extends OptionScreen {
    private final Trigger trigger;
    private final TextStyle textStyle;
    private final Runnable onClose;
    
    public TriggerOptionsScreen(Screen lastScreen, Trigger trigger, TextStyle textStyle,
                                Runnable onClose, String defaultKey) {
        super(lastScreen);
        this.trigger = trigger;
        this.textStyle = textStyle;
        this.onClose = onClose;
        addTabs(defaultKey);
    }

    private void addTabs(String defaultKey) {
        List<Tab> tabs = List.of(
                new Tab(TabKey.TRIGGER_EDITOR.key, (screen) ->
                        new TriggerEditorList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING,
                                cast(screen).trigger, cast(screen).textStyle
                        )),
                new Tab(TabKey.KEY_SELECTOR.key, (screen) ->
                        new KeySelectorList(Minecraft.getInstance(), 0, 0, 0,
                                BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT,
                                cast(screen).trigger
                        ))
        );
        super.setTabs(tabs, defaultKey);
    }

    public enum TabKey {
        TRIGGER_EDITOR(translationKey("option", "notif.trigger.editor")),
        KEY_SELECTOR(translationKey("option", "notif.trigger.selector"));

        public final String key;
        TabKey(String key) {
            this.key = key;
        }
    }

    private static TriggerOptionsScreen cast(OptionScreen screen) {
        if (!(screen instanceof TriggerOptionsScreen s)) throw new IllegalArgumentException(
                String.format("Option list supplier for class %s cannot use screen type %s",
                        TriggerOptionsScreen.class.getName(), screen.getClass().getName()));
        return s;
    }

    @Override
    public void onClose() {
        onClose.run();
        super.onClose();
    }
}
