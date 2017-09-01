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
package oshi.hardware.platform.windows;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.platform.win32.Kernel32;

import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.windows.Psapi;
import oshi.jna.platform.windows.Psapi.PERFORMANCE_INFORMATION;
import oshi.util.platform.windows.WmiUtil;

/**
 * Memory obtained by GlobalMemoryStatusEx.
 *
 * @author dblock[at]dblock[dot]org
 */
public class WindowsGlobalMemory extends AbstractGlobalMemory {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(WindowsGlobalMemory.class.getName());

    private transient PERFORMANCE_INFORMATION perfInfo = new PERFORMANCE_INFORMATION();

    private long lastUpdate = 0;

    /**
     * Update the performance information no more frequently than every 100ms
     */
    @Override
    protected void updateMeminfo() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate > 100) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(this.perfInfo, this.perfInfo.size())) {
                LOG.log(Level.SEVERE, MessageFormat.format("Failed to get Performance Info. Error code: {0}",
                        Kernel32.INSTANCE.GetLastError()));
                return;
            }
            this.memAvailable = this.perfInfo.PageSize.longValue() * this.perfInfo.PhysicalAvailable.longValue();
            this.memTotal = this.perfInfo.PageSize.longValue() * this.perfInfo.PhysicalTotal.longValue();
            this.swapTotal = this.perfInfo.PageSize.longValue()
                    * (this.perfInfo.CommitLimit.longValue() - this.perfInfo.PhysicalTotal.longValue());
            this.lastUpdate = now;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateSwap() {
        updateMeminfo();
        Map<String, List<Long>> usage = WmiUtil.selectUint32sFrom(null, "Win32_PerfRawData_PerfOS_PagingFile",
                "PercentUsage,PercentUsage_Base", "WHERE Name=\"_Total\"");
        if (!usage.get("PercentUsage").isEmpty()) {
            this.swapUsed = this.swapTotal * usage.get("PercentUsage").get(0) / usage.get("PercentUsage_Base").get(0);
        }
    }
}
