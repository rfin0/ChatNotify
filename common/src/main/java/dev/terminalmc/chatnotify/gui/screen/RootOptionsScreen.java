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
import dev.terminalmc.chatnotify.gui.widget.list.main.GlobalOptionList;
import dev.terminalmc.chatnotify.gui.widget.list.main.NotificationList;
import dev.terminalmc.chatnotify.gui.widget.list.OptionList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static dev.terminalmc.chatnotify.util.Localization.localized;

/**
 * Root options screen, including global options and a list of
 * {@link dev.terminalmc.chatnotify.config.Notification} configuration entries.
 * 
 * <p>Config is saved only when this is closed.</p>
 */
public class RootOptionsScreen extends OptionScreen {
    private final Tab defaultTab;

    public RootOptionsScreen(Screen lastScreen) {
        this(lastScreen, Tab.NOTIFICATIONS);
    }
    
    public RootOptionsScreen(Screen lastScreen, Tab defaultTab) {
        super(lastScreen);
        this.defaultTab = defaultTab;
        addTabs();
    }
    
    private void addTabs() {
        List<TabButton> tabs = Arrays.stream(Tab.values()).map((tab) ->
                new TabButton(tab.title, () -> tab.getList(this))).toList();
        super.setTabs(tabs, Arrays.stream(Tab.values()).toList().indexOf(defaultTab));
    }

    public enum Tab {
        NOTIFICATIONS(localized("option", "notif"), (screen) ->
                new NotificationList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING
                )),
        GLOBAL_OPTIONS(localized("option", "global"), (screen) ->
                new GlobalOptionList(Minecraft.getInstance(), 0, 0, 0,
                        BASE_LIST_ENTRY_WIDTH, LIST_ENTRY_HEIGHT, LIST_ENTRY_SPACING
                ));

        final Component title;
        private final Function<RootOptionsScreen, @NotNull OptionList> supplier;
        private @Nullable OptionList list = null;

        Tab(Component title, Function<RootOptionsScreen, OptionList> supplier) {
            this.title = title;
            this.supplier = supplier;
        }

        public @NotNull OptionList getList(RootOptionsScreen screen) {
            if (list == null) {
                list = supplier.apply(screen);
            }
            return list;
        }
    }

    @Override
    protected void addFooter() {
        int spacing = 4;
        int w = BASE_LIST_ENTRY_WIDTH / 2 - spacing;
        int h = LIST_ENTRY_HEIGHT;
        int x1 = width / 2 - w - spacing / 2;
        int x2 = width / 2 + spacing / 2;
        int y = Math.min(
                height - h, // Bottom of screen
                height - FOOTER_MARGIN / 2 - h / 2 // Center of margin
        );
        
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL,
                        (button) -> Minecraft.getInstance().setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        Config.reload();
                                        Minecraft.getInstance().setScreen(this);
                                        onClose();
                                    } else {
                                        Minecraft.getInstance().setScreen(this);
                                    }
                                }, 
                                localized("option", "root.exit_without_saving"),
                                localized("option", "root.exit_without_saving.confirm"))))
                .pos(x1, y)
                .size(w, h)
                .build());
        
        addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE, 
                (button) -> onClose())
                .pos(x2, y)
                .size(w, h)
                .build());
    }

    @Override
    public void onClose() {
        super.onClose();
        Config.save();
    }
}
