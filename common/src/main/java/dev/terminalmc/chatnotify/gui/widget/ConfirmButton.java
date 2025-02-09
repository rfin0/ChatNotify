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

package dev.terminalmc.chatnotify.gui.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A {@link Button} that must be pressed twice to complete an action.
 */
public class ConfirmButton extends Button {
    private Component message;
    private Component confirmMessage;
    private boolean hasBeenPressed;
    
    public ConfirmButton(int x, int y, int width, int height, Component message, 
                         Component confirmMessage, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.message = message;
        this.confirmMessage = confirmMessage;
    }
    
    public void reset() {
        hasBeenPressed = false;
        this.setMessage(message);
    }

    @Override
    public void onPress() {
        if (!hasBeenPressed) {
            hasBeenPressed = true;
            this.setMessage(confirmMessage);
        } else {
            super.onPress();
        }
    }
}
