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

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.terminalmc.chatnotify.gui.widget.HorizontalList;
import dev.terminalmc.chatnotify.gui.widget.OverlayWidget;
import dev.terminalmc.chatnotify.gui.widget.list.OptionList;
import dev.terminalmc.chatnotify.mixin.accessor.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Contains one tightly-coupled {@link OptionList}, which is used to display
 * all option control widgets.
 * 
 * <p>Supports displaying a single {@link OverlayWidget}, which requires hiding
 * all other widgets to avoid rendering and click conflicts but is still simpler
 * than screen switching.</p>
 */
public abstract class OptionScreen extends OptionsSubScreen {
    public static final int HEADER_MARGIN = 32;
    public static final int FOOTER_MARGIN = 32;
    /**
     * If the Minecraft window is less than this width, it will attempt to
     * reduce the GUI scale. Thus, if the option list width does not exceed this
     * value, the widths of entry elements can be safely hardcoded.
     */
    public static final int BASE_ROW_WIDTH = Window.BASE_WIDTH;
    /**
     * Space on either side of list entries for the scrollbar.
     */
    public static final int SCROLL_BAR_MARGIN = 20;
    /**
     * Standard horizontal space between list entry elements.
     */
    public static final int ELEMENT_SPACING = 4;
    public static final int ELEMENT_SPACING_NARROW = 2;
    public static final int ELEMENT_SPACING_FINE = 1;
    /**
     * Maximum height of widgets in a list entry.
     */
    public static final int LIST_ENTRY_HEIGHT = 20;
    /**
     * Vertical space between list entries.
     */
    public static final int LIST_ENTRY_SPACING = 5;
    /**
     * Space on either side of list entries for hanging elements. Normally used
     * by drag-and-drop reposition buttons (left) and delete buttons (right).
     */
    public static final int HANGING_WIDGET_MARGIN = LIST_ENTRY_HEIGHT + ELEMENT_SPACING;
    /**
     * The maximum safe cumulative width for hardcoding list entry element
     * widths and spacing.
     */
    public static final int BASE_LIST_ENTRY_WIDTH = BASE_ROW_WIDTH
            - (SCROLL_BAR_MARGIN * 2)
            - (HANGING_WIDGET_MARGIN * 2);
    
    public static final int TAB_LIST_HEIGHT = HEADER_MARGIN - 4;
    public static final int TAB_LIST_Y = 2;
    public static final int TAB_LIST_MARGIN = 24;
    
    public static final int MIN_TAB_WIDTH = 40;
    public static final int MAX_TAB_WIDTH = 120;
    public static final int TAB_HEIGHT = 20;
    public static final int TAB_SPACING = 4;
    
    protected final HorizontalList<Button> tabs;
    private @Nullable OptionList list;
    private @Nullable OverlayWidget overlay = null;

    /**
     * The {@link OptionList} passed here is not required to have the correct 
     * bounds as it will be resized and initialized prior to being displayed.
     */
    public OptionScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.empty());
        this.tabs = new HorizontalList<>(TAB_LIST_MARGIN, TAB_LIST_Y,
                width - TAB_LIST_MARGIN * 2, TAB_LIST_HEIGHT, TAB_SPACING);
        this.tabs.topScrollbar = true;
    }
    
    protected void setTabs(List<TabButton> tabList, int index) {
        for (TabButton tab : tabList) {
            tabs.addEntry(Button.builder(tab.title, (button) -> {
                tabs.entries().forEach((b) -> b.active = true);
                button.active = false;
                setList(tab.listSupplier.get());
            }).size(Math.clamp(Minecraft.getInstance().font.width(tab.title) + 8,
                    MIN_TAB_WIDTH, MAX_TAB_WIDTH), TAB_HEIGHT).build());
        }

        this.tabs.getEntry(index).active = false;
        this.list = tabList.get(index).listSupplier().get();
        this.list.setScreen(this);
    }
    
    public record TabButton(Component title, Supplier<@NotNull OptionList> listSupplier) {
        
    }
    
    private void setList(@NotNull OptionList list) {
        this.list = list;
        this.list.setScreen(this);
        init();
    }
    
    @Override
    protected void init() {
        clearWidgets();
        clearFocus();
        
        addHeader();
        addContents();
        addFooter();
        
        setInitialFocus();
    }

    @Override
    public void resize(@NotNull Minecraft mc, int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    protected void addHeader() {
        tabs.setWidth(width - 24 * 2);
        addRenderableWidget(tabs);
    }

    @Override
    protected void addContents() {
        // Option list
        if (list != null) {
            list.updateSizeAndPosition(width, height - HEADER_MARGIN - FOOTER_MARGIN,
                    HEADER_MARGIN);
            addRenderableWidget(list);
        }
        
        // Overlay widget
        if (overlay != null) {
            overlay.updateBounds(width, height);
            setOverlay(overlay);
        }
    }

    @Override
    protected void addFooter() {
        int w = BASE_LIST_ENTRY_WIDTH;
        int h = LIST_ENTRY_HEIGHT;
        int x = (width / 2) - (w / 2);
        int y = Math.min(
                height - h, // Bottom of screen
                height - (FOOTER_MARGIN / 2) - (h / 2) // Center of margin
        );
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> onClose())
                .pos(x, y)
                .size(w, h)
                .build());
    }

    @Override
    protected void addOptions() {
        // Called only by OptionsSubScreen#addContents(), which we override so
        // this method is not used.
    }

    @Override
    public void onClose() {
        if (lastScreen instanceof OptionScreen screen) {
            screen.resize(Minecraft.getInstance(), width, height);
        }
        super.onClose();
    }

    // Overlay widget handling
    
    public void setOverlay(OverlayWidget widget) {
        removeOverlayWidget();
        overlay = widget;
        setChildrenVisible(false);
        ((ScreenAccessor)this).getChildren().addFirst(widget);
        ((ScreenAccessor)this).getNarratables().addFirst(widget);
        ((ScreenAccessor)this).getRenderables().addLast(widget);
    }

    public void removeOverlayWidget() {
        if (overlay != null) {
            removeWidget(overlay);
            overlay = null;
            setChildrenVisible(true);
        }
    }

    private void setChildrenVisible(boolean visible) {
        for (GuiEventListener listener : children()) {
            if (listener instanceof AbstractWidget widget) {
                widget.visible = visible;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (overlay != null) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                overlay.onClose();
                removeOverlayWidget();
            } else {
                overlay.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (overlay != null) {
            overlay.charTyped(chr, modifiers);
            return true;
        } else {
            return super.charTyped(chr, modifiers);
        }
    }
}
