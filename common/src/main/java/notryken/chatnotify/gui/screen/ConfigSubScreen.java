package notryken.chatnotify.gui.screen;

import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import notryken.chatnotify.gui.component.listwidget.ConfigListWidget;

/**
 * ChatNotify configuration options sub-screen. Contains a single
 * {@code ConfigListWidget} which must contain all required control widgets
 * (e.g. buttons, sliders).
 * <p>
 * Use of ConfigListWidget facilitates dynamic restructuring of configuration
 * options displays without requiring additional screens.
 * <p>
 * <b>Note:</b> If creating a ChatNotify config screen (e.g. for ModMenu
 * integration), instantiate {@code ConfigMainScreen}, not this class.
 */
public class ConfigSubScreen extends OptionsSubScreen {
    private ConfigListWidget listWidget;

    public ConfigSubScreen(Screen parent, Options gameOptions, Component title,
                           ConfigListWidget listWidget) {
        super(parent, gameOptions, title);
        this.listWidget = listWidget;
    }

    @Override
    protected void init() {
        this.listWidget = this.listWidget.resize(
                this.width, this.height, 32, this.height - 32);
        this.addWidget(listWidget);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
            assert this.minecraft != null;
            this.minecraft.setScreen(this.lastScreen);
        })
                .size(240, 20)
                .pos(this.width / 2 - 120, this.height - 27)
                .build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.listWidget.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xffffff);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderDirtBackground(context);
    }
}
