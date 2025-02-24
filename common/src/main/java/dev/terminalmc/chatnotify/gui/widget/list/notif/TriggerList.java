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

import dev.terminalmc.chatnotify.config.*;
import dev.terminalmc.chatnotify.gui.screen.OptionScreen;
import dev.terminalmc.chatnotify.gui.screen.TriggerOptionsScreen;
import dev.terminalmc.chatnotify.gui.widget.field.TextField;
import dev.terminalmc.chatnotify.gui.widget.list.DragReorderList;
import dev.terminalmc.chatnotify.gui.widget.list.OptionList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static dev.terminalmc.chatnotify.util.Localization.localized;

public class TriggerList extends DragReorderList {
    private final Notification notif;
    private String filterString = "";
    private @Nullable Pattern filterPattern = null;
    private OptionList.Entry.ActionButton addTriggerEntry;

    public TriggerList(Minecraft mc, int width, int height, int y, int entryWidth,
                       int entryHeight, int entrySpacing, Notification notif) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpacing,
                new HashMap<>(Map.of(Entry.TriggerOptions.class, (source, dest) ->
                        moveTrigger(notif, source, dest))));
        this.notif = notif;

        addTriggerEntry = new OptionList.Entry.ActionButton(
                entryX, entryWidth, entryHeight, Component.literal("+"), null, -1,
                (button) -> {
                    notif.triggers.add(new Trigger());
                    filterString = "";
                    filterPattern = null;
                    init();
                    ensureVisible(addTriggerEntry);
                });
    }

    @Override
    protected void addEntries() {
        addEntry(new Entry.TriggerListHeader(dynEntryX, dynEntryWidth, entryHeight, this));

        refreshTriggerSubList();
        addTriggerEntry.setBounds(entryX, entryWidth, entryHeight);
        addEntry(addTriggerEntry);
    }

    protected void refreshTriggerSubList() {
        children().removeIf((entry) -> entry instanceof Entry.TriggerOptions.Normal
                || entry instanceof Entry.StyleTargetOptions);
        // Get list start index
        int start = children().indexOf(addTriggerEntry);
        if (start == -1) start = children().size();
        boolean isUser = notif.equals(Config.get().getUserNotif());
        // Add in reverse order
        for (int i = notif.triggers.size() - 1; i >= 0; i--) {
            Trigger trigger = notif.triggers.get(i);
            if (filterPattern == null || filterPattern.matcher(trigger.string).find()) {
                if (isUser && i <= 1) {
                    children().add(start, new Entry.TriggerOptions.Locked(dynWideEntryX,
                            dynWideEntryWidth, entryHeight, this, notif, trigger));
                } else {
                    if (trigger.styleTarget.enabled) {
                        children().add(start, new Entry.StyleTargetOptions(dynWideEntryX,
                                dynWideEntryWidth, entryHeight, this, trigger.styleTarget));
                    }
                    children().add(start, new Entry.TriggerOptions.Normal(dynWideEntryX,
                            dynWideEntryWidth, entryHeight, this, notif, trigger));
                }
            }
        }
        clampScrollAmount();
    }

    // Sub-screen opening

    private void openTriggerConfig(Trigger trigger) {
        mc.setScreen(new TriggerOptionsScreen(mc.screen, trigger, notif.textStyle,
                () -> {}, TriggerOptionsScreen.Tab.TRIGGER_EDITOR));
    }

    private void openKeyConfig( Trigger trigger) {
        mc.setScreen(new TriggerOptionsScreen(mc.screen, trigger, notif.textStyle,
                () -> {}, TriggerOptionsScreen.Tab.KEY_SELECTOR));
    }

    // Re-ordering util

    private static boolean moveTrigger(Notification notif, int source, int dest) {
        if (notif == Config.get().getUserNotif()) {
            // Don't allow re-ordering of username triggers
            if (source <= 1) {
                return false;
            } else if (dest <= 1) {
                dest = 2;
            }
        }
        return notif.moveTrigger(source, dest);
    }

    // Custom entries

    abstract static class Entry extends OptionList.Entry {

        private static class TriggerListHeader extends Entry {
            TriggerListHeader(int x, int width, int height, TriggerList list) {
                super();
                int cappedWidth = Math.min(width, OptionScreen.BASE_ROW_WIDTH);
                if (cappedWidth < width) {
                    x += (width - cappedWidth) / 2;
                    width = cappedWidth;
                }
                Component title = localized("option", "notif.trigger.list", "ℹ");
                int titleWidth = Minecraft.getInstance().font.width(title) + 8;
                int searchFieldMinWidth = 50;
                int searchFieldWidth = width - titleWidth - SPACE;
                if (searchFieldWidth < searchFieldMinWidth) {
                    int diff = searchFieldMinWidth - searchFieldWidth;
                    searchFieldWidth += diff;
                    titleWidth -= diff;
                }

                StringWidget titleWidget = new StringWidget(x, 0, titleWidth, height,
                        title, list.mc.font);
                titleWidget.setTooltip(Tooltip.create(localized(
                        "option", "notif.trigger.list.tooltip")));
                elements.add(titleWidget);

                TextField searchField = new TextField(x + width - searchFieldWidth, 0,
                        searchFieldWidth, height);
                searchField.setMaxLength(64);
                searchField.setHint(localized("common", "search"));
                searchField.setValue(list.filterString);
                searchField.setResponder((str) -> {
                    list.filterString = str;
                    if (str.isBlank()) {
                        list.filterPattern = null;
                    } else {
                        list.filterPattern = Pattern.compile("(?iU)" + Pattern.quote(str));
                    }
                    list.refreshTriggerSubList();
                });
                elements.add(searchField);
            }
        }

        private abstract static class TriggerOptions extends Entry {
            enum Type {
                NORMAL,
                LOCKED,
            }

            private TriggerOptions(Type optionsType, int x, int width, int height,
                                   TriggerList list, Notification notif, Trigger trigger) {
                super();
                int index = notif.triggers.indexOf(trigger);
                boolean keyTrigger = trigger.type == Trigger.Type.KEY;
                int triggerFieldWidth = switch(optionsType) {
                    case NORMAL -> width - (list.tinyWidgetWidth * 3);
                    case LOCKED -> width;
                };
                if (keyTrigger) triggerFieldWidth -= list.tinyWidgetWidth;
                int movingX = x;

                if (optionsType.equals(Type.LOCKED)) {
                    // Non-editable display field only
                    TextField displayField = new TextField(x, 0, width, height);
                    displayField.setValue(trigger.string);
                    displayField.setTooltip(Tooltip.create(index == 0
                            ? localized("option", "notif.trigger.list.special.profile_name")
                            : localized("option", "notif.trigger.list.special.display_name")));
                    displayField.setTooltipDelay(Duration.ofMillis(500));
                    displayField.setEditable(false);
                    displayField.active = false;
                    elements.add(displayField);
                    return;
                }

                // Index indicator
                Button indicatorButton = Button.builder(
                                Component.literal(String.valueOf(index + 1)), (button) -> {})
                        .pos(x - list.smallWidgetWidth - SPACE - list.tinyWidgetWidth, 0)
                        .size(list.tinyWidgetWidth, height)
                        .build();
                indicatorButton.active = false;
                elements.add(indicatorButton);

                // Drag reorder button
                Button dragButton = Button.builder(Component.literal("\u2191\u2193"),
                                (button) -> {
                                    this.setDragging(true);
                                    list.startDragging(this, StyleTargetOptions.class,
                                            trigger.styleTarget.enabled);
                                })
                        .pos(x - list.smallWidgetWidth - SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build();
                dragButton.active = list.filterPattern == null;
                elements.add(dragButton);

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
                                    (button) -> list.openKeyConfig(trigger))
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
                TextField triggerField = new TextField(movingX, 0, triggerFieldWidth, height);
                if (trigger.type == Trigger.Type.REGEX) triggerField.regexValidator();
                triggerField.withValidator(new TextField.Validator.UniqueTrigger(
                        () -> Config.get().getNotifs(), (n) -> n.triggers, notif, trigger));
                triggerField.setMaxLength(240);
                triggerField.setResponder((str) -> trigger.string = str.strip());
                triggerField.setValue(trigger.string);
                triggerField.setHint(localized("option", "trigger.field.hint"));
                elements.add(triggerField);
                movingX += triggerFieldWidth;

                // Trigger editor button
                Button editorButton = Button.builder(Component.literal("\u270e"),
                                (button) -> list.openTriggerConfig(trigger))
                        .pos(movingX, 0)
                        .size(list.tinyWidgetWidth, height)
                        .build();
                editorButton.setTooltip(Tooltip.create(localized(
                        "option", "trigger.open.trigger_editor.tooltip")));
                editorButton.setTooltipDelay(Duration.ofMillis(500));
                elements.add(editorButton);
                movingX += list.tinyWidgetWidth;

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

                // Delete button
                elements.add(Button.builder(
                        Component.literal("\u274C").withStyle(ChatFormatting.RED),
                        (button) -> {
                            notif.triggers.remove(index);
                            list.init();
                        })
                        .pos(x + width + SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());
            }

            private static class Normal extends TriggerOptions {
                Normal(int x, int width, int height, TriggerList list,
                       Notification notif, Trigger trigger) {
                    super(Type.NORMAL, x, width, height, list, notif, trigger);
                }
            }

            private static class Locked extends TriggerOptions {
                Locked(int x, int width, int height, TriggerList list,
                       Notification notif, Trigger trigger) {
                    super(Type.LOCKED, x, width, height, list, notif, trigger);
                }
            }
        }

        private static class StyleTargetOptions extends Entry {
            StyleTargetOptions(int x, int width, int height, TriggerList list,
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
                stringField.setResponder((string) -> styleTarget.string = string.strip());
                stringField.setHint(localized("option", "trigger.style_target.field.hint"));
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
    }
}
