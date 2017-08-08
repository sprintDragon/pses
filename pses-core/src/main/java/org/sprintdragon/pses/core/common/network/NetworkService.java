/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.sprintdragon.pses.core.common.network;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sprintdragon.pses.core.common.component.AbstractComponent;
import org.sprintdragon.pses.core.common.settings.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
@Slf4j
public class NetworkService extends AbstractComponent {
    /**
     * A custom name resolver can support custom lookup keys (my_net_key:ipv4) and also change
     * the default inet address used in case no settings is provided.
     */
    public interface CustomNameResolver {
        /**
         * Resolves the default value if possible. If not, return <tt>null</tt>.
         */
        InetAddress[] resolveDefault();

        /**
         * Resolves a custom value handling, return <tt>null</tt> if can't handle it.
         */
        InetAddress[] resolveIfPossible(String value) throws IOException;
    }

    private final List<CustomNameResolver> customNameResolvers;

    public NetworkService(Settings settings, List<CustomNameResolver> customNameResolvers) {
        super(settings);
        IfConfig.logIfNecessary();
        this.customNameResolvers = customNameResolvers;
    }

    /**
     * Resolves {@code bindHosts} to a list of internet addresses. The list will
     * not contain duplicate addresses.
     *
     * @param bindHosts list of hosts to bind to. this may contain special pseudo-hostnames
     *                  such as _local_ (see the documentation). if it is null, it will be populated
     *                  based on global default settings.
     * @return unique set of internet addresses
     */
    public InetAddress[] resolveBindHostAddresses(String bindHosts[]) throws IOException {
        // first check settings
        InetAddress addresses[] = resolveInetAddresses(bindHosts);

        // try to deal with some (mis)configuration
        for (InetAddress address : addresses) {
            // check if its multicast: flat out mistake
            if (address.isMulticastAddress()) {
                throw new IllegalArgumentException("bind address: {" + NetworkAddress.format(address) + "} is invalid: multicast address");
            }
            // check if its a wildcard address: this is only ok if its the only address!
            if (address.isAnyLocalAddress() && addresses.length > 1) {
                throw new IllegalArgumentException("bind address: {" + NetworkAddress.format(address) + "} is wildcard, but multiple addresses specified: this makes no sense");
            }
        }
        return addresses;
    }

    /**
     * Resolves {@code publishHosts} to a single publish address. The fact that it returns
     * only one address is just a current limitation.
     * <p>
     * If {@code publishHosts} resolves to more than one address, <b>then one is selected with magic</b>
     *
     * @param publishHosts list of hosts to publish as. this may contain special pseudo-hostnames
     *                     such as _local_ (see the documentation). if it is null, it will be populated
     *                     based on global default settings.
     * @return single internet address
     */
    // TODO: needs to be InetAddress[]
    public InetAddress resolvePublishHostAddresses(String publishHosts[]) throws IOException {
        InetAddress addresses[] = resolveInetAddresses(publishHosts);
        // TODO: allow publishing multiple addresses
        // for now... the hack begins

        // 1. single wildcard address, probably set by network.host: expand to all interface addresses.
        if (addresses.length == 1 && addresses[0].isAnyLocalAddress()) {
            HashSet<InetAddress> all = new HashSet<>(Arrays.asList(NetworkUtils.getAllAddresses()));
            addresses = all.toArray(new InetAddress[all.size()]);
        }

        // 2. try to deal with some (mis)configuration
        for (InetAddress address : addresses) {
            // check if its multicast: flat out mistake
            if (address.isMulticastAddress()) {
                throw new IllegalArgumentException("publish address: {" + NetworkAddress.format(address) + "} is invalid: multicast address");
            }
            // check if its a wildcard address: this is only ok if its the only address!
            // (if it was a single wildcard address, it was replaced by step 1 above)
            if (address.isAnyLocalAddress()) {
                throw new IllegalArgumentException("publish address: {" + NetworkAddress.format(address) + "} is wildcard, but multiple addresses specified: this makes no sense");
            }
        }

        // 3. if we end out with multiple publish addresses, select by preference.
        // don't warn the user, or they will get confused by bind_host vs publish_host etc.
        if (addresses.length > 1) {
            List<InetAddress> sorted = new ArrayList<>(Arrays.asList(addresses));
            NetworkUtils.sortAddresses(sorted);
            addresses = new InetAddress[]{sorted.get(0)};
        }
        return addresses[0];
    }

    /**
     * resolves (and deduplicates) host specification
     */
    private InetAddress[] resolveInetAddresses(String hosts[]) throws IOException {
        if (hosts.length == 0) {
            throw new IllegalArgumentException("empty host specification");
        }
        // deduplicate, in case of resolver misconfiguration
        // stuff like https://bugzilla.redhat.com/show_bug.cgi?id=496300
        HashSet<InetAddress> set = new HashSet<>();
        for (String host : hosts) {
            set.addAll(Arrays.asList(resolveInternal(host)));
        }
        return set.toArray(new InetAddress[set.size()]);
    }

    /**
     * resolves a single host specification
     */
    private InetAddress[] resolveInternal(String host) throws IOException {
        if ((host.startsWith("#") && host.endsWith("#")) || (host.startsWith("_") && host.endsWith("_"))) {
            host = host.substring(1, host.length() - 1);
            // next check any registered custom resolvers if any
            if (customNameResolvers != null) {
                for (CustomNameResolver customNameResolver : customNameResolvers) {
                    InetAddress addresses[] = customNameResolver.resolveIfPossible(host);
                    if (addresses != null) {
                        return addresses;
                    }
                }
            }
            switch (host) {
                case "local":
                    return NetworkUtils.getLoopbackAddresses();
                case "local:ipv4":
                    return NetworkUtils.filterIPV4(NetworkUtils.getLoopbackAddresses());
                case "local:ipv6":
                    return NetworkUtils.filterIPV6(NetworkUtils.getLoopbackAddresses());
                case "site":
                    return NetworkUtils.getSiteLocalAddresses();
                case "site:ipv4":
                    return NetworkUtils.filterIPV4(NetworkUtils.getSiteLocalAddresses());
                case "site:ipv6":
                    return NetworkUtils.filterIPV6(NetworkUtils.getSiteLocalAddresses());
                case "global":
                    return NetworkUtils.getGlobalAddresses();
                case "global:ipv4":
                    return NetworkUtils.filterIPV4(NetworkUtils.getGlobalAddresses());
                case "global:ipv6":
                    return NetworkUtils.filterIPV6(NetworkUtils.getGlobalAddresses());
                default:
                    /* an interface specification */
                    if (host.endsWith(":ipv4")) {
                        host = host.substring(0, host.length() - 5);
                        return NetworkUtils.filterIPV4(NetworkUtils.getAddressesForInterface(host));
                    } else if (host.endsWith(":ipv6")) {
                        host = host.substring(0, host.length() - 5);
                        return NetworkUtils.filterIPV6(NetworkUtils.getAddressesForInterface(host));
                    } else {
                        return NetworkUtils.getAddressesForInterface(host);
                    }
            }
        }
        return InetAddress.getAllByName(host);
    }

    // TODO: needs to be InetAddress[]
    public InetAddress resolvePublishHostAddress(String publishHost) throws IOException {
        // next check any registered custom resolvers
        if (publishHost == null) {
            for (CustomNameResolver customNameResolver : customNameResolvers) {
                InetAddress addresses[] = customNameResolver.resolveDefault();
                if (addresses != null) {
                    return addresses[0];
                }
            }
        }
        // finally, fill with our default
        // TODO: allow publishing multiple addresses
        InetAddress address = resolveInetAddress(publishHost)[0];

        // try to deal with some (mis)configuration
        if (address != null) {
            // check if its multicast: flat out mistake
            if (address.isMulticastAddress()) {
                throw new IllegalArgumentException("publish address: {" + NetworkAddress.format(address) + "} is invalid: multicast address");
            }
            // wildcard address, probably set by network.host
            if (address.isAnyLocalAddress()) {
                InetAddress old = address;
                address = NetworkUtils.getFirstNonLoopbackAddresses()[0];
                log.warn("publish address: {{}} is a wildcard address, falling back to first non-loopback: {{}}",
                        NetworkAddress.format(old), NetworkAddress.format(address));
            }
        }
        return address;
    }

    private InetAddress[] resolveInetAddress(String host) throws IOException {
        if ((host.startsWith("#") && host.endsWith("#")) || (host.startsWith("_") && host.endsWith("_"))) {
            host = host.substring(1, host.length() - 1);
            // allow custom resolvers to have special names
            for (CustomNameResolver customNameResolver : customNameResolvers) {
                InetAddress addresses[] = customNameResolver.resolveIfPossible(host);
                if (addresses != null) {
                    return addresses;
                }
            }
            switch (host) {
                case "local":
                    return NetworkUtils.getLoopbackAddresses();
                case "local:ipv4":
                    return NetworkUtils.filterIPV4(NetworkUtils.getLoopbackAddresses());
                case "local:ipv6":
                    return NetworkUtils.filterIPV6(NetworkUtils.getLoopbackAddresses());
                case "non_loopback":
                    return NetworkUtils.getFirstNonLoopbackAddresses();
                case "non_loopback:ipv4":
                    return NetworkUtils.filterIPV4(NetworkUtils.getFirstNonLoopbackAddresses());
                case "non_loopback:ipv6":
                    return NetworkUtils.filterIPV6(NetworkUtils.getFirstNonLoopbackAddresses());
                default:
                    /* an interface specification */
                    if (host.endsWith(":ipv4")) {
                        host = host.substring(0, host.length() - 5);
                        return NetworkUtils.filterIPV4(NetworkUtils.getAddressesForInterface(host));
                    } else if (host.endsWith(":ipv6")) {
                        host = host.substring(0, host.length() - 5);
                        return NetworkUtils.filterIPV6(NetworkUtils.getAddressesForInterface(host));
                    } else {
                        return NetworkUtils.getAddressesForInterface(host);
                    }
            }
        }
        return NetworkUtils.getAllByName(host);
    }
}
