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

package dev.terminalmc.chatnotify.compat.commandkeys;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.chatnotify.mixin.accessor.KeyAccessor;
import dev.terminalmc.commandkeys.util.KeybindUtil;

public class CommandKeysUtil {
    public static final String DUAL_KEY_PATTERN_STRING = "^[a-z0-9.]+-[a-z0-9.]++$";

    /**
     * Parses the specified string into two {@link InputConstants.Key}
     * instances, and passes them to CommandKeys' keypress handler.
     */
    static void send(String str) {
        if (!str.matches(DUAL_KEY_PATTERN_STRING)) return;
        String[] splitStr = str.split("-");
        String limitKeyStr = splitStr[0];
        String KeyStr = splitStr[1];

        InputConstants.Key limitKey = KeyAccessor.getNameMap().get(limitKeyStr);
        InputConstants.Key key = KeyAccessor.getNameMap().get(KeyStr);
        if (key == null || limitKey == null) return;

        KeybindUtil.handleKeys(key, limitKey);
    }
}
