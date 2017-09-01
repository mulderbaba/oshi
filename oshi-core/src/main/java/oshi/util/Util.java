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
package oshi.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * General utility methods
 *
 * @author widdis[at]gmail[dot]com
 */
public class Util {
    private static final Logger LOG = Logger.getLogger(Util.class.getName());

    private Util() {
    }

    /**
     * Sleeps for the specified number of milliseconds.
     *
     * @param ms
     *            How long to sleep
     */
    public static void sleep(long ms) {
        try {
            LOG.log(Level.FINEST, MessageFormat.format("Sleeping for {0} ms", ms));
            Thread.sleep(ms);
        } catch (InterruptedException e) { // NOSONAR squid:S2142
            LOG.log(Level.FINEST, "", e);
            LOG.log(Level.WARNING, MessageFormat.format("Interrupted while sleeping for {0} ms", ms));
        }
    }

    /**
     * Sleeps for the specified number of milliseconds after the given system
     * time in milliseconds. If that number of milliseconds has already elapsed,
     * does nothing.
     *
     * @param startTime
     *            System time in milliseconds to sleep after
     * @param ms
     *            How long after startTime to sleep
     */
    public static void sleepAfter(long startTime, long ms) {
        long now = System.currentTimeMillis();
        long until = startTime + ms;
        LOG.log(Level.FINEST, MessageFormat.format("Sleeping until {0}", until));
        if (now < until) {
            sleep(until - now);
        }
    }
}