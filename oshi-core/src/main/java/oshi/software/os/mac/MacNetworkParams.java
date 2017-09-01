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
package oshi.software.os.mac;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.mac.SystemB;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

public class MacNetworkParams extends AbstractNetworkParams {

    private static final Logger LOG = Logger.getLogger(MacNetworkParams.class.getName());

    private static final long serialVersionUID = 1L;

    private static final String IPV6_ROUTE_HEADER = "Internet6:";

    private static final String DEFAULT_GATEWAY = "default";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainName() {
        SystemB.Addrinfo hint = new SystemB.Addrinfo();
        hint.ai_flags = SystemB.AI_CANONNAME;
        String hostname = "";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.log(Level.SEVERE, MessageFormat.format("Unknown host exception when getting address of local host: {0}",
                    e));
            return "";
        }
        PointerByReference ptr = new PointerByReference();
        int res = SystemB.INSTANCE.getaddrinfo(hostname, null, hint, ptr);
        if (res > 0) {
            LOG.log(Level.SEVERE, MessageFormat.format("Failed getaddrinfo(): {0}", SystemB.INSTANCE.gai_strerror(res)));
            return "";
        }
        SystemB.Addrinfo info = new SystemB.Addrinfo(ptr.getValue());
        String canonname = info.ai_canonname.trim();
        SystemB.INSTANCE.freeaddrinfo(ptr.getValue());
        return canonname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv4DefaultGateway() {
        return searchGateway(ExecutingCommand.runNative("route -n get default"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIpv6DefaultGateway() {
        List<String> lines = ExecutingCommand.runNative("netstat -nr");
        boolean v6Table = false;
        for (String line : lines) {
            if (v6Table && line.startsWith(DEFAULT_GATEWAY)) {
                String[] fields = line.split("\\s+");
                if (fields.length > 2 && fields[2].contains("G")) {
                    return fields[1].split("%")[0];
                }
            } else if (line.startsWith(IPV6_ROUTE_HEADER)) {
                v6Table = true;
            }
        }
        return "";
    }
}