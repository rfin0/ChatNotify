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

package dev.terminalmc.chatnotify.gui.widget.list.root.notif;

import dev.terminalmc.chatnotify.config.Config;
import dev.terminalmc.chatnotify.config.Notification;
import dev.terminalmc.chatnotify.gui.widget.field.TextField;
import dev.terminalmc.chatnotify.gui.widget.list.OptionList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static dev.terminalmc.chatnotify.util.Localization.localized;

public class MiscOptionList extends OptionList {
    private final Notification notif;

    public MiscOptionList(Minecraft mc, int width, int height, int y, int entryWidth,
                          int entryHeight, int entrySpacing, Notification notif) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpacing);
        this.notif = notif;
    }

    @Override
    protected void addEntries() {
        Minecraft mc = Minecraft.getInstance();
        
        addEntry(new OptionList.Entry.Text(entryX, entryWidth, entryHeight,
                localized("option", "notif.misc.control"), null, -1));
        addEntry(new Entry.Controls(entryX, entryWidth, entryHeight, notif));

        addEntry(new OptionList.Entry.Text(entryX, entryWidth, entryHeight,
                localized("option", "notif.misc.msg", "\u2139"),
                Tooltip.create(localized("option", "notif.misc.msg.tooltip.format_codes")
                        .append("\n\n")
                        .append(localized("option", "notif.misc.msg.tooltip.regex_groups"))), -1));
        addEntry(new Entry.CustomMessage(dynWideEntryX, dynWideEntryWidth, entryHeight,
                () -> notif.replacementMsg, (str) -> notif.replacementMsg = str,
                () -> notif.replacementMsgEnabled, (val) -> notif.replacementMsgEnabled = val,
                localized("option", "notif.misc.msg.replacement"),
                localized("option", "notif.misc.msg.replacement").append(".\n")
                        .append(localized("option", "notif.misc.msg.replacement.tooltip"))
                        .append("\n\n").append(localized("option", "notif.misc.msg.tooltip.blank_hide"))));
        addEntry(new Entry.CustomMessage(dynWideEntryX, dynWideEntryWidth, entryHeight,
                () -> notif.statusBarMsg, (str) -> notif.statusBarMsg = str,
                () -> notif.statusBarMsgEnabled, (val) -> notif.statusBarMsgEnabled = val,
                localized("option", "notif.misc.msg.status_bar"),
                localized("option", "notif.misc.msg.status_bar").append(".\n")
                        .append(localized("option", "notif.misc.msg.status_bar.tooltip"))
                        .append("\n\n").append(localized("option", "notif.misc.msg.tooltip.blank_original"))));
        addEntry(new Entry.CustomMessage(dynWideEntryX, dynWideEntryWidth, entryHeight,
                () -> notif.titleMsg, (str) -> notif.titleMsg = str,
                () -> notif.titleMsgEnabled, (val) -> notif.titleMsgEnabled = val,
                localized("option", "notif.misc.msg.title"),
                localized("option", "notif.misc.msg.title").append(".\n")
                        .append(localized("option", "notif.misc.msg.title.tooltip"))
                        .append("\n\n").append(localized("option", "notif.misc.msg.tooltip.blank_original"))));
        addEntry(new Entry.CustomMessage(dynWideEntryX, dynWideEntryWidth, entryHeight,
                () -> notif.toastMsg, (str) -> notif.toastMsg = str,
                () -> notif.toastMsgEnabled, (val) -> notif.toastMsgEnabled = val,
                localized("option", "notif.misc.msg.toast"),
                localized("option", "notif.misc.msg.toast").append(".\n")
                        .append(localized("option", "notif.misc.msg.toast.tooltip"))
                        .append("\n\n").append(localized("option", "notif.misc.msg.tooltip.blank_original"))));
        
        addEntry(new OptionList.Entry.Text(entryX, entryWidth, entryHeight,
                localized("option", "notif.misc.reset"), null, -1));

        addEntry(new OptionList.Entry.ActionButton(entryX, entryWidth, entryHeight,
                localized("option", "notif.misc.reset.button"),
                Tooltip.create(localized("option", "notif.misc.reset.tooltip")),
                -1,
                (button) -> mc.setScreen(new ConfirmScreen(
                        (value) -> {
                            if (value) {
                                Config.resetAndSave();
                                mc.setScreen(null);
                            }
                            else {
                                mc.setScreen(screen);
                                init();
                            }},
                        localized("option", "notif.misc.reset"),
                        localized("option", "notif.misc.reset.confirm")))));
    }
    
    // Custom entries

    private abstract static class Entry extends OptionList.Entry {

        private static class Controls extends Entry {
            Controls(int x, int width, int height, Notification notif) {
                super();

                elements.add(CycleButton.<Notification.CheckOwnMode>builder((status) ->
                                localized("option", "notif.misc.control.self_notify.status." + status.name()))
                        .withValues(Notification.CheckOwnMode.values())
                        .withInitialValue(notif.checkOwnMode)
                        .withTooltip((status) -> Tooltip.create(localized(
                                "option", "notif.misc.control.self_notify.status." + status.name() + ".tooltip")))
                        .create(x, 0, width, height, localized("option", "global.self_notify"),
                                (button, status) ->
                                        notif.checkOwnMode = status));
            }
        }

        private static class CustomMessage extends TriggerList.Entry {
            CustomMessage(int x, int width, int height,
                          Supplier<String> textSupplier, Consumer<String> textConsumer,
                          Supplier<Boolean> statusSupplier, Consumer<Boolean> statusConsumer,
                          Component hint, Component tooltip) {
                super();
                int statusButtonWidth = Math.max(24, height);
                int fieldWidth = width - statusButtonWidth - SPACE;

                // String field
                TextField titleField = new TextField(x, 0, fieldWidth, height);
                titleField.setMaxLength(256);
                titleField.setValue(textSupplier.get());
                titleField.setResponder(textConsumer);
                titleField.setHint(hint);
                titleField.setTooltip(Tooltip.create(tooltip));
                elements.add(titleField);

                // Status button
                elements.add(CycleButton.booleanBuilder(
                        CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                        CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .displayOnlyValue()
                        .withInitialValue(statusSupplier.get())
                        .create(x + width - statusButtonWidth, 0, statusButtonWidth, height,
                                Component.empty(), (button, status) ->
                                        statusConsumer.accept(status)));
            }
        }
    }
}
