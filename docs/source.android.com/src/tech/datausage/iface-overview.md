<!--
   Copyright 2012 The Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->


In Android 4.0, statistics reported by Linux network interfaces are
recorded over time, and are used to enforce network quota limits,
render user-visible charts, and more.

Each network device driver (Wi-Fi included) must follow the standard
kernel device lifecycle, and return correct statistics through
`dev_get_stats()`. In particular, statistics returned must remain
strictly monotonic while the interface is active. Drivers may reset
statistics only after successfully completing an `unregister_netdev()`
or the equivalent that generates a `NETDEV_UNREGISTER` event for
callbacks registered with `register_netdevice_notifier()` /
`register_inetaddr_notifier()` / `register_inet6addr_notifier()`.

Mobile operators typically measure data usage at the Internet layer
(IP). To match this approach in Android 4.0, we rely on the fact that
for the kernel devices we care about the `rx_bytes` and `tx_bytes`
values returned by `dev_get_stats()` return exactly the Internet layer
(`IP`) bytes transferred.  But we understand that for other devices it
might not be the case. For now, the feature relies on this
peculiarity. New drivers should have that property also, and the
`dev_get_stats()` values must not include any encapsulation overhead
of lower network layers (such as Ethernet headers), and should
preferably not include other traffic (such as ARP) unless it is
negligible.

The Android framework only collects statistics from network interfaces
associated with a `NetworkStateTracker` in `ConnectivityService`. This
enables the framework to concretely identify each network interface,
including its type (such as `TYPE_MOBILE` or `TYPE_WIFI`) and
subscriber identity (such as IMSI).  All network interfaces used to
route data should be represented by a `NetworkStateTracker` so that
statistics can be accounted correctly.
