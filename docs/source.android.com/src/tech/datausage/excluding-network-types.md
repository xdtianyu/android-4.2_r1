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

A mobile operator may wish to exclude specific network types from the
total data usage calculated by a device.  For example, network traffic
over an MMS APN may be “zero-rated” by a mobile operator.  To support
this, the set of network types used to calculate total data usage can
be configured through the `config_data_usage_network_types` resource
at build time.

Some mobile radio implementations may have unique Linux network
interfaces for each active APN, while other radios may force multiple
APNs to coexist on a single interface.  Android can collect network
statistics from both designs, but `config_data_usage_network_types` is
not be effective at excluding APNs forced to coexist on a single
interface.
