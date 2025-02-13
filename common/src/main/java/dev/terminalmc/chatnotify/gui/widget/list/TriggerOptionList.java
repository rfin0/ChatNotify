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

package dev.terminalmc.chatnotify.gui.widget.list;

import com.mojang.datafixers.util.Pair;
import dev.terminalmc.chatnotify.ChatNotify;
import dev.terminalmc.chatnotify.config.*;
import dev.terminalmc.chatnotify.gui.screen.OptionScreen;
import dev.terminalmc.chatnotify.gui.widget.HsvColorPicker;
import dev.terminalmc.chatnotify.gui.widget.field.MultiLineTextField;
import dev.terminalmc.chatnotify.gui.widget.field.TextField;
import dev.terminalmc.chatnotify.util.FormatUtil;
import dev.terminalmc.chatnotify.util.MessageUtil;
import dev.terminalmc.chatnotify.util.StyleUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static dev.terminalmc.chatnotify.util.Localization.localized;

/**
 * Contains trigger and style target editing fields, no-responder display fields
 * for message text and key, and a list of recent messages.
 * 
 * <p>The message list can be restyled and/or filtered based on the trigger.</p>
 */
public class TriggerOptionList extends OptionList {
    private final Trigger trigger;
    private final TextStyle textStyle;
    private final List<Component> recentChat;
    private boolean filter;
    private boolean restyle;
    private MultiLineTextField textDisplayField;
    private String displayText = "";
    private TextField keyDisplayField;
    private String displayKey = "";
    
    public TriggerOptionList(Minecraft mc, int width, int height, int y, int entryWidth,
                             int entryHeight, int entrySpacing, Runnable onClose, Trigger trigger,
                             TextStyle textStyle) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpacing, onClose);
        this.trigger = trigger;
        this.textStyle = textStyle;
        this.recentChat = ChatNotify.unmodifiedChat.stream().toList().reversed();
    }

    @Override
    protected void addEntries() {
        // Trigger editor
        addSpacedEntry(new Entry.TriggerOptions(
                dynWideEntryX, dynWideEntryWidth, entryHeight + itemHeight, this, trigger));
        if (trigger.styleTarget.enabled) {
            addEntry(new Entry.StyleTargetOptions(dynWideEntryX, dynWideEntryWidth, entryHeight,
                    this, trigger.styleTarget));
        }

        // Text display field
        textDisplayField = new MultiLineTextField(dynWideEntryX, 0, dynWideEntryWidth, entryHeight,
                localized("option", "trigger.display.text.hint"));
        textDisplayField.setValue(displayText);
        addSpacedEntry(new Entry.DisplayField(
                dynWideEntryX, dynWideEntryWidth, entryHeight + itemHeight,
                textDisplayField, localized("option", "trigger.display.text")));
        
        // Key display field
        keyDisplayField = new TextField(dynWideEntryX, 0, dynWideEntryWidth, entryHeight);
        keyDisplayField.setMaxLength(256);
        keyDisplayField.setValue(displayKey);
        addEntry(new Entry.DisplayField(dynWideEntryX, dynWideEntryWidth, entryHeight, keyDisplayField,
                localized("option", "trigger.display.key")));

        // Filter, restyle and color controls
        addEntry(new Entry.Controls(dynWideEntryX, dynWideEntryWidth, entryHeight, this));

        // Chat message list
        addChatMessages(this.recentChat);
    }
    
    // Display field utils
    
    private void setTextDisplayValue(String text) {
        displayText = text;
        textDisplayField.setValue(displayText);
    }

    private void setKeyDisplayValue(String key) {
        displayKey = key;
        keyDisplayField.setValue(displayKey);
    }
    
    // Chat message list
    
    private void addChatMessages(List<Component> recentChat) {
        boolean restyleAll = Config.get().restyleMode.equals(Config.RestyleMode.ALL_INSTANCES);
        
        // Filter and restyle, retaining original copies of messages to use
        // when displaying text and key of a clicked message.
        List<Pair<Component, Component>> displayChat = new ArrayList<>();
        for (Component msg : recentChat) {
            Component restyledMsg = msg.copy();
            Matcher matcher = null;
            String msgStr = FormatUtil.stripCodes(msg.getString());
            boolean hit = switch(trigger.type) {
                case NORMAL -> {
                    matcher = MessageUtil.normalSearch(msgStr, trigger.string);
                    yield matcher.find();
                }
                case REGEX -> {
                    try {
                        matcher = Pattern.compile(trigger.string).matcher(msgStr);
                        yield matcher.find();
                    } catch (PatternSyntaxException ignored) {
                        yield false;
                    }
                }
                case KEY -> MessageUtil.keySearch(msg, trigger.string);
            };
            if (filter && !hit) continue;
            else if (restyle && hit) {
                if (trigger.styleTarget.enabled) {
                    // Process style target string if required prior to restyle
                    if (trigger.styleTarget.type == StyleTarget.Type.REGEX) {
                        trigger.styleTarget.tryCompilePattern();
                    } else if (trigger.styleTarget.type == StyleTarget.Type.CAPTURING) {
                        trigger.styleTarget.tryParseIndexes();
                    }
                }
                restyledMsg = StyleUtil.restyle(
                        msg, msgStr, trigger, matcher, textStyle, restyleAll);
            }
            displayChat.add(new Pair<>(msg, restyledMsg));
        }
        
        // Add message entries
        displayChat.forEach((pair) -> {
            Entry.MessageEntry entry = new Entry.MessageEntry(dynWideEntryX, dynWideEntryWidth,
                    this, pair.getFirst(), pair.getSecond());
            addEntry(entry);
            int requiredHeight = 
                    mc.font.wordWrapHeight(pair.getFirst().getString(), dynWideEntryWidth) - itemHeight;
            while (requiredHeight > 0) {
                Entry.Space spaceEntry = new Entry.Space(entry);
                addEntry(spaceEntry);
                requiredHeight -= itemHeight;
            }
        });
        
        // If no message entries, add note
        if (!(children().getLast() instanceof Entry.MessageEntry)) {
            addEntry(new OptionList.Entry.Text(dynWideEntryX, dynWideEntryWidth, entryHeight,
                    localized("option", "trigger.recent_messages.none"), null, -1));
        }
    }
    
    // Sub-screen opening

    private void openKeyConfig() {
        minecraft.setScreen(new OptionScreen(minecraft.screen, localized("option", "key"),
                new KeyOptionList(minecraft, width, height, getY(), entryWidth, entryHeight,
                        () -> {}, trigger)));
    }

    // Custom entries

    abstract static class Entry extends OptionList.Entry {

        private static class TriggerOptions extends Entry {
            TriggerOptions(int x, int width, int height, TriggerOptionList list,
                           Trigger trigger) {
                super();
                int triggerFieldWidth = width - (list.tinyWidgetWidth * 2);
                boolean keyTrigger = trigger.type == Trigger.Type.KEY;
                if (keyTrigger) triggerFieldWidth -= list.tinyWidgetWidth;
                int movingX = x;

                // Type button
                CycleButton<Trigger.Type> typeButton = CycleButton.<Trigger.Type>builder(
                                (type) -> Component.literal(type.icon))
                        .withValues(Trigger.Type.values())
                        .displayOnlyValue()
                        .withInitialValue(trigger.type)
                        .withTooltip((type) -> Tooltip.create(localized(
                                "option", "trigger.type." + type + ".tooltip")))
                        .create(movingX, 0, list.tinyWidgetWidth, height, Component.empty(),
                                (button, type) -> {
                                    trigger.type = type;
                                    list.init();
                                });
                typeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(typeButton);
                movingX += list.tinyWidgetWidth;

                if (keyTrigger) {
                    // Key selection button
                    Button keySelectButton = Button.builder(Component.literal("\uD83D\uDD0D"),
                                    (button) -> list.openKeyConfig())
                            .pos(movingX, 0)
                            .size(list.tinyWidgetWidth, height)
                            .build();
                    keySelectButton.setTooltip(Tooltip.create(localized(
                            "option", "trigger.open.key_selector.tooltip")));
                    keySelectButton.setTooltipDelay(Duration.ofMillis(500));
                    elements.add(keySelectButton);
                    movingX += list.tinyWidgetWidth;
                }

                // Trigger field
                MultiLineTextField triggerField = new MultiLineTextField(movingX, 0, 
                        triggerFieldWidth, height, localized("option", "trigger.field.tooltip"));
                if (trigger.type == Trigger.Type.REGEX) triggerField.regexValidator();
                triggerField.withValidator(new TextField.Validator.UniqueTrigger(
                        () -> Config.get().getNotifs(), (n) -> n.triggers, null, trigger));
                triggerField.setValueListener((str) -> {
                    trigger.string = str.strip();
                    if (list.children().size() > 4) {
                        list.children().removeIf((entry) -> entry instanceof MessageEntry 
                                || entry instanceof Text 
                                || (entry instanceof Space && list.children().indexOf(entry) > 4));
                        list.addChatMessages(list.recentChat);
                    }
                });
                triggerField.setValue(trigger.string);
                triggerField.setTooltip(Tooltip.create(localized(
                        "option", "trigger.field.tooltip")));
                triggerField.setTooltipDelay(Duration.ofMillis(500));
                elements.add(triggerField);
                movingX += triggerFieldWidth;

                // Style string add button
                Button styleButton = Button.builder(Component.literal("+"),
                                (button) -> {
                                    trigger.styleTarget.enabled = true;
                                    list.init();
                                })
                        .pos(movingX, 0)
                        .size(list.tinyWidgetWidth, height)
                        .build();
                if (!trigger.styleTarget.enabled) {
                    styleButton.setTooltip(Tooltip.create(localized(
                            "option", "trigger.style_target.add.tooltip")));
                    styleButton.setTooltipDelay(Duration.ofMillis(500));
                } else {
                    styleButton.active = false;
                }
                elements.add(styleButton);
            }
        }

        private static class StyleTargetOptions extends Entry {
            StyleTargetOptions(int x, int width, int height, TriggerOptionList list,
                               StyleTarget styleTarget) {
                super();
                int stringFieldWidth = width - (list.tinyWidgetWidth * 4);
                int movingX = x + list.tinyWidgetWidth;

                // Info icon
                StringWidget infoIcon = new StringWidget(movingX, 0, list.tinyWidgetWidth, height,
                        Component.literal("\u2139"), Minecraft.getInstance().font);
                infoIcon.alignCenter();
                infoIcon.setTooltip(Tooltip.create(localized(
                        "option", "trigger.style_target.tooltip")));
                infoIcon.setTooltipDelay(Duration.ofMillis(500));
                elements.add(infoIcon);
                movingX += list.tinyWidgetWidth;

                // Type button
                CycleButton<StyleTarget.Type> typeButton = CycleButton.<StyleTarget.Type>builder(
                                (type) -> Component.literal(type.icon))
                        .withValues(StyleTarget.Type.values())
                        .displayOnlyValue()
                        .withInitialValue(styleTarget.type)
                        .withTooltip((type) -> Tooltip.create(localized(
                                "option", "trigger.style_target.type." + type + ".tooltip")))
                        .create(movingX, 0, list.tinyWidgetWidth, height, Component.empty(),
                                (button, type) -> {
                                    styleTarget.type = type;
                                    list.init();
                                });
                typeButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(typeButton);
                movingX += list.tinyWidgetWidth;

                // Style string field
                TextField stringField = new TextField(movingX, 0, stringFieldWidth, height);
                if (styleTarget.type == StyleTarget.Type.REGEX) stringField.regexValidator();
                stringField.setMaxLength(240);
                stringField.setValue(styleTarget.string);
                stringField.setResponder((string) -> {
                    styleTarget.string = string.strip();
                    list.children().removeIf((entry) -> entry instanceof MessageEntry
                            || entry instanceof Text
                            || (entry instanceof Space && list.children().indexOf(entry) > 4));
                    list.addChatMessages(list.recentChat);
                });
                stringField.setTooltip(Tooltip.create(localized(
                        "option", "trigger.style_target.field.tooltip")));
                stringField.setTooltipDelay(Duration.ofMillis(500));
                elements.add(stringField);
                movingX = x + width - list.tinyWidgetWidth;

                // Delete button
                elements.add(Button.builder(
                        Component.literal("\u274C").withStyle(ChatFormatting.RED),
                        (button) -> {
                            styleTarget.enabled = false;
                            list.init();
                        })
                        .pos(movingX, 0)
                        .size(list.tinyWidgetWidth, height)
                        .build());
            }
        }

        private static class Controls extends MainOptionList.Entry {
            Controls(int x, int width, int height, TriggerOptionList list) {
                super();
                int buttonWidth = (width - SPACE * 2) / 3;
                int movingX = x;

                elements.add(CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(list.filter)
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "trigger.filter"),
                                (button, status) -> {
                                    list.filter = status;
                                    list.init();
                                }));
                movingX += buttonWidth + SPACE;

                elements.add(CycleButton.booleanBuilder(
                                CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN),
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .withInitialValue(list.restyle)
                        .create(movingX, 0, buttonWidth, height,
                                localized("option", "trigger.restyle"),
                                (button, status) -> {
                                    list.restyle = status;
                                    list.init();
                                }));
                movingX = x + width - buttonWidth;
                
                elements.add(Button.builder(localized("option", "notif.color")
                                        .setStyle(Style.EMPTY.withColor(list.textStyle.color)), 
                                (button) -> {
                                    int cpHeight = HsvColorPicker.MIN_HEIGHT;
                                    int cpWidth = HsvColorPicker.MIN_WIDTH;
                                    list.screen.setOverlay(new HsvColorPicker(
                                            x + width / 2 - cpWidth / 2, 
                                            list.screen.height / 2 - cpHeight / 2, 
                                            cpWidth, cpHeight,
                                            () -> list.textStyle.color,
                                            (val) -> list.textStyle.color = val,
                                            (widget) -> {
                                                list.screen.removeOverlayWidget();
                                                list.init();
                                            }));
                                })
                        .pos(movingX, 0)
                        .size(buttonWidth, height)
                        .build());
            }
        }

        private static class DisplayField extends Entry {
            DisplayField(int x, int width, int height, AbstractWidget widget, Component label) {
                super();
                int labelWidth = 40;
                int fieldWidth = width - labelWidth - SPACE;

                Button labelButton = Button.builder(label, (button -> {}))
                        .pos(x, 0)
                        .size(labelWidth, height)
                        .build();
                labelButton.active = false;
                elements.add(labelButton);

                widget.setWidth(fieldWidth);
                widget.setHeight(height);
                widget.setX(x + width - fieldWidth);
                elements.add(widget);
            }
        }

        private static class MessageEntry extends Entry {
            private final TriggerOptionList list;
            private final Component msg;

            MessageEntry(int x, int width, TriggerOptionList list, 
                         Component msg, Component restyledMsg) {
                super();
                this.list = list;
                this.msg = msg;
                MultiLineTextWidget widget = new MultiLineTextWidget(x, 0, restyledMsg,
                        Minecraft.getInstance().font);
                widget.setMaxWidth(width);
                elements.add(widget);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                list.setTextDisplayValue(FormatUtil.stripCodes(msg.getString()));
                list.setKeyDisplayValue(msg.getContents() instanceof TranslatableContents tc
                        ? tc.getKey() : localized("option", "trigger.display.key.none").getString());
                list.setScrollAmount(0);
                return true;
            }
        }
    }
}
