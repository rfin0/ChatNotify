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

package dev.terminalmc.chatnotify.gui.widget.list.notif;

import dev.terminalmc.chatnotify.config.Notification;
import dev.terminalmc.chatnotify.config.ResponseMessage;
import dev.terminalmc.chatnotify.gui.widget.field.DropdownTextField;
import dev.terminalmc.chatnotify.gui.widget.field.FakeTextField;
import dev.terminalmc.chatnotify.gui.widget.field.MultiLineTextField;
import dev.terminalmc.chatnotify.gui.widget.field.TextField;
import dev.terminalmc.chatnotify.gui.widget.list.DragReorderList;
import dev.terminalmc.chatnotify.gui.widget.list.OptionList;
import dev.terminalmc.chatnotify.mixin.accessor.KeyAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.terminalmc.chatnotify.util.Localization.localized;

public class ResponseMessageList extends DragReorderList {
    private final Notification notif;
    private OptionList.Entry.ActionButton addMessageEntry;

    public ResponseMessageList(Minecraft mc, int width, int height, int y, int entryWidth,
                               int entryHeight, int entrySpacing, Notification notif) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpacing,
                new HashMap<>(Map.of(Entry.MessageOptions.class, notif::moveResponseMessage)));
        this.notif = notif;

        addMessageEntry = new OptionList.Entry.ActionButton(
                entryX, entryWidth, entryHeight, Component.literal("+"), null, -1,
                (button) -> {
                    notif.responseMessages.add(new ResponseMessage());
                    init();
                    ensureVisible(addMessageEntry);
                });
    }

    @Override
    protected void addEntries() {
        addEntry(new Entry.MessageListHeader(entryX, entryWidth, entryHeight, this));

        refreshMessageSubList();
        addMessageEntry.setBounds(entryX, entryWidth, entryHeight);
        addEntry(addMessageEntry);
    }

    protected void refreshMessageSubList() {
        children().removeIf((entry) -> entry instanceof Entry.MessageOptions);
        // Get list start index
        int start = children().indexOf(addMessageEntry);
        if (start == -1) start = children().size();
        else start--;
        // Add in reverse order
        for (int i = notif.responseMessages.size() - 1; i >= 0; i--) {
            Entry e = new Entry.MessageOptions(dynWideEntryX, dynWideEntryWidth, entryHeight, this, 
                    notif, notif.responseMessages.get(i), i);
            children().add(start, new Entry.Space(e));
            children().add(start, e);
        }
        clampScrollAmount();
    }

    // Custom entries

    abstract static class Entry extends OptionList.Entry {

        private static class MessageListHeader extends Entry {
            MessageListHeader(int x, int width, int height, ResponseMessageList list) {
                super();
                int statusButtonWidth = 25;
                int titleWidth = width - statusButtonWidth - SPACE;
                int movingX = x;

                StringWidget titleWidget = new StringWidget(movingX, 0, titleWidth, height,
                        localized("option", "notif.response.list", "ℹ"), list.mc.font);
                titleWidget.setTooltip(Tooltip.create(
                        localized("option", "notif.response.list.tooltip")
                                .append("\n")
                                .append(localized("option", "notif.response.list.tooltip.warning")
                                        .withStyle(ChatFormatting.RED))));
                elements.add(titleWidget);
                movingX += titleWidth + SPACE;

                elements.add(CycleButton.booleanBuilder(
                        CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN), 
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .displayOnlyValue()
                        .withInitialValue(list.notif.responseEnabled)
                        .create(movingX, 0, statusButtonWidth, height, Component.empty(),
                                (button, status) -> list.notif.responseEnabled = status));
            }
        }

        private static class MessageOptions extends Entry {
            MessageOptions(int x, int width, int height, ResponseMessageList list,
                           Notification notif, ResponseMessage message, int index) {
                super();
                int fieldSpacing = 1;
                int timeFieldWidth = Minecraft.getInstance().font.width("00000");
                int msgFieldWidth = width - timeFieldWidth - list.tinyWidgetWidth - fieldSpacing * 2;
                int movingX = x;

                // Drag reorder button
                elements.add(Button.builder(Component.literal("↑↓"),
                                (button) -> {
                                    this.setDragging(true);
                                    list.startDragging(this, null, false);
                                })
                        .pos(x - list.smallWidgetWidth - SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());

                // Type button
                CycleButton<ResponseMessage.Type> typeButton = CycleButton.<ResponseMessage.Type>builder(
                                (type) -> Component.literal(type.icon))
                        .withValues(ResponseMessage.Type.values())
                        .displayOnlyValue()
                        .withInitialValue(message.type)
                        .withTooltip((type) -> Tooltip.create(localized(
                                "option", "notif.response.type." + type.name() + ".tooltip")))
                        .create(movingX, 0, list.tinyWidgetWidth, height, Component.empty(),
                                (button, type) -> {
                                    message.type = type;
                                    list.init();
                                });
                typeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(typeButton);
                movingX += list.tinyWidgetWidth + fieldSpacing;

                if (message.type.equals(ResponseMessage.Type.COMMANDKEYS)) {
                    int keyFieldWidth = msgFieldWidth / 2;
                    List<String> keys = KeyAccessor.getNameMap().keySet().stream().sorted().toList();
                    FakeTextField keyField1 = new FakeTextField(movingX, 0, keyFieldWidth, height,
                            () -> {
                                int wHeight = Math.max(DropdownTextField.MIN_HEIGHT, list.height);
                                int wWidth = Math.max(DropdownTextField.MIN_WIDTH, list.dynWideEntryWidth);
                                int wX = x + (width / 2) - (wWidth / 2);
                                int wY = list.getY();
                                list.screen.setOverlay(new DropdownTextField(
                                        wX, wY, wWidth, wHeight, Component.empty(),
                                        () -> message.string.matches(".+-.+")
                                                ? message.string.split("-")[0]
                                                : "",
                                        (val) -> message.string = val + "-"
                                                + (message.string.matches(".+-.+")
                                                        ? message.string.split("-")[1]
                                                        : "key.keyboard.unknown"),
                                        (widget) -> {
                                            list.screen.removeOverlayWidget();
                                            list.init();
                                        }, keys));
                            });
                    MutableComponent label1 = localized(
                            "option", "notif.response.commandkeys.limit_key");
                    keyField1.setHint(label1.copy());
                    keyField1.setTooltip(Tooltip.create(label1.append("\n\n").append(localized(
                            "option", "notif.response.commandkeys.limit_key.tooltip"))));
                    keyField1.setMaxLength(240);
                    keyField1.withValidator(new TextField.Validator.InputKey(keys));
                    keyField1.setValue(message.string.matches(".+-.+") ? message.string.split("-")[0] : "");
                    elements.add(keyField1);
                    movingX += keyFieldWidth;
                    FakeTextField keyField2 = new FakeTextField(movingX, 0, keyFieldWidth, height,
                            () -> {
                                int wHeight = Math.max(DropdownTextField.MIN_HEIGHT, list.height);
                                int wWidth = Math.max(DropdownTextField.MIN_WIDTH, list.dynWideEntryWidth);
                                int wX = x + (width / 2) - (wWidth / 2);
                                int wY = list.getY();
                                list.screen.setOverlay(new DropdownTextField(
                                        wX, wY, wWidth, wHeight, Component.empty(),
                                        () -> message.string.matches(".+-.+") ? message.string.split("-")[1] : "",
                                        (val) -> message.string = (message.string.matches(".+-.+") ? message.string.split("-")[0] + "-" + val : "key.keyboard.unknown"),
                                        (widget) -> {
                                            list.screen.removeOverlayWidget();
                                            list.init();
                                        }, keys));
                            });
                    MutableComponent label2 = localized(
                            "option", "notif.response.commandkeys.key");
                    keyField2.setHint(label2.copy());
                    keyField2.setTooltip(Tooltip.create(label2.append("\n\n").append(localized(
                            "option", "notif.response.commandkeys.key.tooltip"))));
                    keyField2.setMaxLength(240);
                    keyField2.withValidator(new TextField.Validator.InputKey(keys));
                    keyField2.setValue(message.string.matches(".+-.+")
                            ? message.string.split("-")[1]
                            : "");
                    elements.add(keyField2);
                } else {
                    // Response field
                    MultiLineTextField msgField = new MultiLineTextField(
                            movingX, 0, msgFieldWidth, height * 2);
                    msgField.setCharacterLimit(256);
                    msgField.setValue(message.string);
                    msgField.setValueListener((val) -> message.string = val.strip());
                    elements.add(msgField);
                }

                // Delay field
                TextField timeField = new TextField(
                        x + width - timeFieldWidth, 0, timeFieldWidth, height);
                timeField.posIntValidator().strict();
                timeField.setTooltip(Tooltip.create(localized(
                        "option", "notif.response.time.tooltip")));
                timeField.setTooltipDelay(Duration.ofMillis(500));
                timeField.setMaxLength(5);
                timeField.setResponder((str) -> message.delayTicks = Integer.parseInt(str.strip()));
                timeField.setValue(String.valueOf(message.delayTicks));
                elements.add(timeField);

                // Delete button
                elements.add(Button.builder(
                                Component.literal("❌").withStyle(ChatFormatting.RED),
                                (button) -> {
                                    notif.responseMessages.remove(index);
                                    list.init();
                                })
                        .pos(x + width + SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());
            }
        }
    }
}
