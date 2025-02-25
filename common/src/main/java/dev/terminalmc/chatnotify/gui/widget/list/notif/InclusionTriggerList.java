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
import dev.terminalmc.chatnotify.config.Trigger;
import dev.terminalmc.chatnotify.gui.screen.OptionScreen;
import dev.terminalmc.chatnotify.gui.screen.TriggerOptionsScreen;
import dev.terminalmc.chatnotify.gui.widget.field.TextField;
import dev.terminalmc.chatnotify.gui.widget.list.DragReorderList;
import dev.terminalmc.chatnotify.gui.widget.list.OptionList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static dev.terminalmc.chatnotify.util.Localization.localized;

public class InclusionTriggerList extends DragReorderList {
    private final Notification notif;
    private String filterString = "";
    private @Nullable Pattern filterPattern = null;
    private OptionList.Entry.ActionButton addTriggerEntry;

    public InclusionTriggerList(Minecraft mc, int width, int height, int y, int entryWidth,
                                int entryHeight, int entrySpacing, Notification notif) {
        super(mc, width, height, y, entryWidth, entryHeight, entrySpacing, 
                new HashMap<>(Map.of(Entry.TriggerOptions.class, notif::moveInclusionTrigger)));
        this.notif = notif;

        addTriggerEntry = new OptionList.Entry.ActionButton(
                entryX, entryWidth, entryHeight, Component.literal("+"), null, -1,
                (button) -> {
                    notif.inclusionTriggers.add(new Trigger());
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
        children().removeIf((entry) -> entry instanceof Entry.TriggerOptions);
        // Get list start index
        int start = children().indexOf(addTriggerEntry);
        if (start == -1) start = children().size();
        // Add in reverse order
        for (int i = notif.inclusionTriggers.size() - 1; i >= 0; i--) {
            Trigger trigger = notif.inclusionTriggers.get(i);
            if (filterPattern == null || filterPattern.matcher(trigger.string).find()) {
                children().add(start, new Entry.TriggerOptions(dynWideEntryX,
                        dynWideEntryWidth, entryHeight, this, notif, trigger));
            }
        }
        clampScrollAmount();
    }

    // Sub-screen opening

    private void openKeyConfig(Trigger trigger) {
        mc.setScreen(new TriggerOptionsScreen(mc.screen, trigger, notif.textStyle, () -> {},
                TriggerOptionsScreen.TabKey.KEY_SELECTOR.key));
    }

    // Custom entries

    abstract static class Entry extends OptionList.Entry {

        private static class TriggerListHeader extends Entry {
            TriggerListHeader(int x, int width, int height, InclusionTriggerList list) {
                super();
                int cappedWidth = Math.min(width, OptionScreen.BASE_ROW_WIDTH);
                if (cappedWidth < width) {
                    x += (width - cappedWidth) / 2;
                    width = cappedWidth;
                }
                Component title = localized("option", "notif.inclusion.list", "ℹ");
                int titleWidth = Minecraft.getInstance().font.width(title) + 8;
                int statusButtonWidth = 25;
                int searchFieldMinWidth = 50;
                int searchFieldWidth = width - statusButtonWidth - titleWidth - SPACE * 2;
                if (searchFieldWidth < searchFieldMinWidth) {
                    int diff = searchFieldMinWidth - searchFieldWidth;
                    searchFieldWidth += diff;
                    titleWidth -= diff;
                }
                int movingX = x;

                StringWidget titleWidget = new StringWidget(movingX, 0, titleWidth, height,
                        title, list.mc.font);
                titleWidget.setTooltip(Tooltip.create(localized(
                        "option", "notif.inclusion.list.tooltip")));
                elements.add(titleWidget);
                movingX += titleWidth + SPACE;

                elements.add(CycleButton.booleanBuilder(
                        CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN), 
                                CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED))
                        .displayOnlyValue()
                        .withInitialValue(list.notif.inclusionEnabled)
                        .create(movingX, 0, statusButtonWidth, height, Component.empty(),
                                (button, status) -> list.notif.inclusionEnabled = status));
                movingX += statusButtonWidth + SPACE;
                
                TextField searchField = new TextField(movingX, 0, searchFieldWidth, height);
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

        private static class TriggerOptions extends Entry {
            private TriggerOptions(int x, int width, int height, InclusionTriggerList list,
                                   Notification notif, Trigger trigger) {
                super();
                int index = notif.inclusionTriggers.indexOf(trigger);
                boolean keyTrigger = trigger.type == Trigger.Type.KEY;
                int triggerFieldWidth = width - list.tinyWidgetWidth;
                if (keyTrigger) triggerFieldWidth -= list.tinyWidgetWidth;
                int movingX = x;

                // Index indicator
                Button indicatorButton = Button.builder(
                                Component.literal(String.valueOf(index + 1)), (button) -> {})
                        .pos(x - list.smallWidgetWidth - SPACE - list.tinyWidgetWidth, 0)
                        .size(list.tinyWidgetWidth, height)
                        .build();
                indicatorButton.active = false;
                elements.add(indicatorButton);

                // Drag reorder button
                Button dragButton = Button.builder(Component.literal("↑↓"),
                                (button) -> {
                                    this.setDragging(true);
                                    list.startDragging(this, null,
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
                                "option", "notif.trigger.type." + type + ".tooltip")))
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
                            "option", "notif.trigger.open.key_selector.tooltip")));
                    keySelectButton.setTooltipDelay(Duration.ofMillis(500));
                    elements.add(keySelectButton);
                    movingX += list.tinyWidgetWidth;
                }

                // Trigger field
                TextField triggerField = new TextField(movingX, 0, triggerFieldWidth, height);
                if (trigger.type == Trigger.Type.REGEX) triggerField.regexValidator();
                triggerField.withValidator(new TextField.Validator.UniqueTrigger(() ->
                        List.of(notif), (n) -> n.inclusionTriggers, notif, trigger));
                triggerField.setMaxLength(240);
                triggerField.setResponder((str) -> trigger.string = str.strip());
                triggerField.setValue(trigger.string);
                triggerField.setHint(localized("option", "notif.trigger.field.hint"));
                elements.add(triggerField);

                // Delete button
                elements.add(Button.builder(
                        Component.literal("❌").withStyle(ChatFormatting.RED),
                        (button) -> {
                            notif.inclusionTriggers.remove(index);
                            list.init();
                        })
                        .pos(x + width + SPACE, 0)
                        .size(list.smallWidgetWidth, height)
                        .build());
            }
        }
    }
}
