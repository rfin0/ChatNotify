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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import dev.terminalmc.chatnotify.gui.widget.list.MainOptionList;
import net.minecraft.network.chat.CommonComponents;

import static dev.terminalmc.chatnotify.util.Localization.localized;

/**
 * The root {@link OptionScreen}, containing a {@link MainOptionList}.
 * 
 * <p>Config is saved only when this {@link Screen} is closed.</p>
 */
public class MainOptionScreen extends OptionScreen {
    public MainOptionScreen(Screen lastScreen) {
        super(lastScreen, localized("option", "main"),
                new MainOptionList(
                        Minecraft.getInstance(), 
                        0,
                        0,
                        OptionScreen.HEADER_MARGIN,
                        OptionScreen.BASE_LIST_ENTRY_WIDTH,
                        OptionScreen.LIST_ENTRY_HEIGHT,
                        OptionScreen.LIST_ENTRY_SPACING
                ));
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
                                localized("option", "main.exit_without_saving"),
                                localized("option", "main.exit_without_saving.confirm"))))
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
