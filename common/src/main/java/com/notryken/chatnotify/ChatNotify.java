/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.chatnotify;

import com.mojang.datafixers.util.Pair;
import com.notryken.chatnotify.config.Config;
import com.notryken.chatnotify.config.ResponseMessage;
import com.notryken.chatnotify.util.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class ChatNotify {
    public static final String MOD_ID = "chatnotify";
    public static final String MOD_NAME = "ChatNotify";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);

    public static final List<Pair<Long, String>> recentMessages = new ArrayList<>();
    public static final List<ResponseMessage> responseMessages = new ArrayList<>();

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft mc) {
        tickResponseMessages(mc);
    }

    private static void tickResponseMessages(Minecraft mc) {
        List<String> sending = new ArrayList<>();
        for (ResponseMessage resMsg : responseMessages) {
            resMsg.countdown--;
            if (resMsg.countdown <= 0 && resMsg.sendingString != null) {
                sending.add(resMsg.sendingString);
            }
        }
        responseMessages.removeIf((resMsg) -> resMsg.countdown <= 0);
        if (mc.getConnection() != null && mc.getConnection().isAcceptingMessages()) {
            if (!sending.isEmpty()) {
                Screen oldScreen = mc.screen;
                mc.setScreen(new ChatScreen(""));
                for (String msg : sending) {
                    if (mc.screen instanceof ChatScreen cs) {
                        cs.handleChatInput(msg, false);
                    }
                }
                mc.setScreen(oldScreen);
            }
        }
        else {
            responseMessages.clear();
        }
    }
}
