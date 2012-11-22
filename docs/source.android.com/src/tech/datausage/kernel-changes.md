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

This is a summary of the main changes in the kernel that diverge from mainline.

* added net/netfilter/xt_qtaguid*
* imported then modified net/netfilter/xt_quota2.c from xtables-addons project
* fixes in net/netfilter/ip6_tables.c
* modified ip*t_REJECT.c
* modified net/netfilter/xt_socket.c

A few comments on the kernel configuration:

* xt_qtaguid masquerades as xt_owner and relies on xt_socket and itself relies on the connection tracker.
* The connection tracker can't handle large SIP packets, it must be disabled.
* The modified xt_quota2 uses the NFLOG support to notify userspace.
