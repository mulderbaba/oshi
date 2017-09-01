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
package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB.HostCpuLoadInfo;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.unix.CLibrary.Timeval;
import oshi.util.ExecutingCommand;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CPU.
 *
 * @author alessandro[at]perucchi[dot]org
 * @author alessio.fachechi[at]gmail[dot]com
 * @author widdis[at]gmail[dot]com
 */
public class MacCentralProcessor extends AbstractCentralProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(MacCentralProcessor.class.getName());

    private static final long BOOTTIME;
    static {
        Timeval tv = new Timeval();
        if (!SysctlUtil.sysctl("kern.boottime", tv) || tv.tv_sec == 0) {
            // Usually this works. If it doesn't, fall back to text parsing.
            // Boot time will be the first consecutive string of digits.
            BOOTTIME = ParseUtil.parseLongOrDefault(
                    ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""),
                    System.currentTimeMillis() / 1000);
        } else {
            // tv now points to a 64-bit timeval structure for boot time.
            // First 4 bytes are seconds, second 4 bytes are microseconds
            // (we ignore)
            BOOTTIME = tv.tv_sec;
        }
    }

    /**
     * Create a Processor
     */
    public MacCentralProcessor() {
        super();
        // Initialize class variables
        initVars();
        // Initialize tick arrays
        initTicks();

        LOG.log(Level.FINE, "Initialized Processor");
    }

    private void initVars() {
        setVendor(SysctlUtil.sysctl("machdep.cpu.vendor", ""));
        setName(SysctlUtil.sysctl("machdep.cpu.brand_string", ""));
        setCpu64(SysctlUtil.sysctl("hw.cpu64bit_capable", 0) != 0);
        int i = SysctlUtil.sysctl("machdep.cpu.stepping", -1);
        setStepping(i < 0 ? "" : Integer.toString(i));
        i = SysctlUtil.sysctl("machdep.cpu.model", -1);
        setModel(i < 0 ? "" : Integer.toString(i));
        i = SysctlUtil.sysctl("machdep.cpu.family", -1);
        setFamily(i < 0 ? "" : Integer.toString(i));
        long processorID = 0L;
        processorID |= SysctlUtil.sysctl("machdep.cpu.signature", 0);
        processorID |= (SysctlUtil.sysctl("machdep.cpu.feature_bits", 0L) & 0xffffffff) << 32;
        setProcessorID(String.format("%016X", processorID));
    }

    /**
     * Updates logical and physical processor counts from sysctl calls
     */
    @Override
    protected void calculateProcessorCounts() {
        this.logicalProcessorCount = SysctlUtil.sysctl("hw.logicalcpu", 1);
        this.physicalProcessorCount = SysctlUtil.sysctl("hw.physicalcpu", 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getSystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int machPort = SystemB.INSTANCE.mach_host_self();
        HostCpuLoadInfo cpuLoadInfo = new HostCpuLoadInfo();
        if (0 != SystemB.INSTANCE.host_statistics(machPort, SystemB.HOST_CPU_LOAD_INFO, cpuLoadInfo,
                new IntByReference(cpuLoadInfo.size()))) {
            LOG.log(Level.SEVERE, "Failed to get System CPU ticks. Error code: " + Native.getLastError());
            return ticks;
        }

        ticks[TickType.USER.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_USER];
        ticks[TickType.NICE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_NICE];
        ticks[TickType.SYSTEM.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_SYSTEM];
        ticks[TickType.IDLE.getIndex()] = cpuLoadInfo.cpu_ticks[SystemB.CPU_STATE_IDLE];
        // Leave IOWait and IRQ values as 0
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = SystemB.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[][] getProcessorCpuLoadTicks() {
        long[][] ticks = new long[this.logicalProcessorCount][TickType.values().length];

        int machPort = SystemB.INSTANCE.mach_host_self();

        IntByReference procCount = new IntByReference();
        PointerByReference procCpuLoadInfo = new PointerByReference();
        IntByReference procInfoCount = new IntByReference();
        if (0 != SystemB.INSTANCE.host_processor_info(machPort, SystemB.PROCESSOR_CPU_LOAD_INFO, procCount,
                procCpuLoadInfo, procInfoCount)) {
            LOG.log(Level.SEVERE, "Failed to update CPU Load. Error code: " + Native.getLastError());
            return ticks;
        }

        int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0, procInfoCount.getValue());
        for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
            int offset = cpu * SystemB.CPU_STATE_MAX;
            ticks[cpu][TickType.USER.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_USER]);
            ticks[cpu][TickType.NICE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_NICE]);
            ticks[cpu][TickType.SYSTEM.getIndex()] = FormatUtil
                    .getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_SYSTEM]);
            ticks[cpu][TickType.IDLE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + SystemB.CPU_STATE_IDLE]);
        }
        return ticks;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSystemUptime() {
        return System.currentTimeMillis() / 1000 - BOOTTIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public String getSystemSerialNumber() {
        return new MacComputerSystem().getSerialNumber();
    }
}
