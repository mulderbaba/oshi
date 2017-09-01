/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2017 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.hardware.platform.unix.solaris.SolarisDisplay;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * A Display
 *
 * @author widdis[at]gmail[dot]com
 */
public class LinuxDisplay extends AbstractDisplay {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(LinuxDisplay.class.getName());

    public LinuxDisplay(byte[] edid) {
        super(edid);
        LOG.log(Level.FINE, "Initialized LinuxDisplay");
    }

    /**
     * Gets Display Information
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static Display[] getDisplays() {
        List<String> xrandr = ExecutingCommand.runNative("xrandr --verbose");
        // xrandr reports edid in multiple lines. After seeing a line containing
        // EDID, read subsequent lines of hex until 256 characters are reached
        if (xrandr.isEmpty()) {
            return new Display[0];
        }
        List<Display> displays = new ArrayList<>();
        StringBuilder sb = null;
        for (String s : xrandr) {
            if (s.contains("EDID")) {
                sb = new StringBuilder();
            } else if (sb != null) {
                sb.append(s.trim());
                if (sb.length() < 256) {
                    continue;
                }
                String edidStr = sb.toString();
                LOG.log(Level.FINE, MessageFormat.format("Parsed EDID: {0}", edidStr));
                byte[] edid = ParseUtil.hexStringToByteArray(edidStr);
                if (edid.length >= 128) {
                    displays.add(new SolarisDisplay(edid));
                }
                sb = null;
            }
        }

        return displays.toArray(new Display[displays.size()]);
    }
}