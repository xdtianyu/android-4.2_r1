[TOC]
#Android Security Overview

##Introduction

Android is a modern mobile platform that was designed to be truly open. Android
applications make use of advanced hardware and software, as well as local and
served data, exposed through the platform to bring innovation and value to
consumers. To protect that value, the platform must offer an application
environment that ensures the security of users, data, applications, the device,
and the network.

Securing an open platform requires a robust security architecture and rigorous
security programs.  Android was designed with multi-layered security that
provides the flexibility required for an open platform, while providing
protection for all users of the platform.

Android was designed with developers in mind. Security controls were designed
to reduce the burden on developers. Security-savvy developers can easily work
with and rely on flexible security controls.  Developers less familiar with
security will be protected by safe defaults.

Android was designed with device users in mind. Users are provided visibility
into how applications work, and control over those applications.  This design
includes the expectation that attackers would attempt to perform common
attacks, such as social engineering attacks to convince device users to install
malware, and attacks on third-party applications on Android. Android was
designed to both reduce the probability of these attacks and greatly limit the
impact of the attack in the event it was successful.

This document outlines the goals of the Android security program, describes the
fundamentals of the Android security architecture, and answers the most
pertinent questions for system architects and security analysts.  This document
focuses on the security features of Android's core platform and does not
discuss security issues that are unique to specific applications, such as those
related to the browser or SMS application. Recommended best practices for
building Android devices, deploying Android devices, or developing applications
for Android are not the goal of this document and are provided elsewhere.

# Background

Android provides an open source platform and application environment for mobile
devices.

The main Android platform building blocks are:

+ **Device Hardware**: Android runs on a wide range of hardware configurations
including smart phones, tablets, and set-top-boxes.  Android is
processor-agnostic, but it does take advantage of some hardware-specific
security capabilities such as ARM v6 eXecute-Never.

+ **Android Operating System**: The core operating system is built on top of
the Linux kernel. All device resources, like camera functions, GPS data,
Bluetooth functions, telephony functions, network connections, etc. are
accessed through the operating system.

+ **Android Application Runtime**: Android applications are most often written
in the Java programming language and run in the Dalvik virtual machine.
However, many applications, including core Android services and applications
are native applications or include native libraries. Both Dalvik and native
applications run within the same security environment, contained within the
Application Sandbox. Applications get a dedicated part of the filesystem in
which they can write private data, including databases and raw files.

Android applications extend the core Android operating system.  There are two
primary sources for applications:

+ **Pre-Installed Applications**: Android includes a set of pre-installed
applications including phone, email, calendar, web browser, and contacts. These
function both as user applications and to provide key device capabilities that
can be accessed by other applications.  Pre-installed applications may be part
of the open source Android platform, or they may be developed by an OEM for a
specific device.

+ **User-Installed Applications**: Android provides an open development
environment supporting any third-party application. Google Play offers
users hundreds of thousands of applications.

Google provides a set of cloud-based services that are available to any
compatible Android device.  The primary services are:

+ **Google Play**: Google Play is a collection of services that
allow users to discover, install, and purchase applications from their Android
device or the web.  Google Play makes it easy for developers to reach Android
users and potential customers.   Google Play also provides community review,
application [license
verification](https://developer.android.com/guide/publishing/licensing.html),
and other security services.

+ **Android Updates**: The Android update service delivers new capabilities and
security updates to Android devices, including updates through the web or over
the air (OTA).

+ **Application Services**: Frameworks that allow Android applications to use
cloud capabilities such as ([backing
up](https://developer.android.com/guide/topics/data/backup.html)) application
data and settings and cloud-to-device messaging
([C2DM](https://code.google.com/android/c2dm/index.html))
for push messaging.

These services are not part of the Android Open Source Project and are out
of scope for this document.  But they are relevant to the security of most
Android devices, so a related security document titled “Google Services for
Android: Security Overview” is available.

##Android Security Program Overview

Early on in development, the core Android development team recognized that a
robust security model was required to enable a vigorous ecosystem of
applications and devices built on and around the Android platform and supported
by cloud services. As a result, through its entire development lifecycle,
Android has been subjected to a professional security program. The Android team
has had the opportunity to observe how other mobile, desktop, and server platforms
prevented and reacted to security issues and built a security
program to address weak points observed in other offerings.

The key components of the Android Security Program include:

+ **Design Review**: The Android security process begins early in the
development lifecycle with the creation of a rich and configurable security
model and design. Each major feature of the platform is reviewed by engineering
and security resources, with appropriate security controls integrated into the
architecture of the system.
+ **Penetration Testing and Code Review**: During the development of the
platform, Android-created and open-source components are subject to vigorous
security reviews. These reviews are performed by the Android Security Team,
Google’s Information Security Engineering team, and independent security
consultants. The goal of these reviews is to identify weaknesses and possible
vulnerabilities well before the platform is open-sourced, and to simulate the
types of analysis that will be performed by external security experts upon
release.
+ **Open Source and Community Review**: The Android Open Source Project enables
broad security review by any interested party. Android also uses open source
technologies that have undergone significant external security review,
such as the Linux kernel.  Google Play provides a forum for users and companies
to provide information about specific applications directly to users.
+ **Incident Response**: Even with all of these precautions, security issues
may occur after shipping, which is why the Android project has created a
comprehensive security response process. A full-time Android security team
constantly monitors Android-specific and the general security community for
discussion of potential vulnerabilities. Upon the discovery of legitimate
issues, the Android team has a response process that enables the rapid
mitigation of vulnerabilities to ensure that potential risk to all Android
users is minimized.  These cloud-supported responses can include updating the
Android platform (over-the-air updates), removing applications from Google
Play, and removing applications from devices in the field.

##Android Platform Security Architecture

Android seeks to be the most secure and usable operating system for mobile
platforms by re-purposing traditional operating system security controls to:

+ Protect user data
+ Protect system resources (including the network)
+ Provide application isolation

To achieve these objectives, Android provides these key security features:

+ Robust security at the OS level through the Linux kernel
+ Mandatory application sandbox for all applications
+ Secure interprocess communication
+ Application signing
+ Application-defined and user-granted permissions

The sections below describe these and other security features of the Android
platform. *Figure 1* summarizes the security components and considerations of
the various levels of the Android software stack. Each component assumes that
the components below are properly secured. With the exception of a small amount
of Android OS code running as root, all code above the Linux Kernel is
restricted by the Application Sandbox.

![Figure 1: Android software stack](images/image00.png)

*Figure 1: Android software stack.*

#System and Kernel Level Security

At the operating system level, the Android platform provides the security of
the Linux kernel, as well as a secure inter-process communication (IPC)
facility to enable secure communication between applications running in
different processes. These security features at the OS level ensure that even
native code is constrained by the Application Sandbox.  Whether that code is
the result of included application behavior or a exploitation of an application
vulnerability, the system would prevent the rogue application from harming
other applications, the Android system, or the device itself.

##Linux Security

The foundation of the Android platform is the Linux kernel. The Linux kernel
itself has been in widespread use for years, and is used in millions of
security-sensitive environments. Through its history of constantly being
researched, attacked, and fixed by thousands of developers, Linux has become a
stable and secure kernel trusted by many corporations and security
professionals.

As the base for a mobile computing environment, the Linux kernel provides
Android with several key security features, including:

+ A user-based permissions model
+ Process isolation
+ Extensible mechanism for secure IPC
+ The ability to remove unnecessary and potentially insecure parts of the kernel

As a multiuser operating system, a fundamental security objective of the Linux
kernel is to isolate user resources from one another.  The Linux security
philosophy is to protect user resources from one another. Thus, Linux:

+ Prevents user A from reading user B's files
+ Ensures that user A does not exhaust user B's memory
+ Ensures that user A does not exhaust user B's CPU resources
+ Ensures that user A does not exhaust user B's devices (e.g. telephony, GPS,
bluetooth)

##The Application Sandbox

The Android platform takes advantage of the Linux user-based protection as a
means of identifying and isolating application resources.  The Android system
assigns a unique user ID (UID) to each Android application and runs it as that user
in a separate process.  This approach is different from other operating systems
(including the traditional Linux configuration), where multiple applications
run with the same user permissions.

This sets up a kernel-level Application Sandbox. The kernel enforces security
between applications and the system at the process level through standard Linux
facilities, such as user and group IDs that are assigned to applications.  By
default, applications cannot interact with each other and applications have
limited access to the operating system. If application A tries to do something
malicious like read application B's data or dial the phone without permission
(which is a separate application), then the operating system protects against
this because application A does not have the appropriate user privileges. The
sandbox is simple, auditable, and based on decades-old UNIX-style user
separation of processes and file permissions.

Since the Application Sandbox is in the kernel, this security model extends to
native code and to operating system applications. All of the software above the
kernel in *Figure 1*, including operating system libraries, application
framework, application runtime, and all applications run within the Application
Sandbox. On some platforms, developers are constrained to a specific
development framework, set of APIs, or language in order to enforce security.
On Android, there are no restrictions on how an application can be written that
are required to enforce security; in this respect, native code is just as
secure as interpreted code.

In some operating systems, memory corruption errors generally lead to
completely compromising the security of the device. This is not the case in
Android due to all applications and their resources being sandboxed at the OS
level. A memory corruption error will only allow arbitrary code execution in
the context of that particular application, with the permissions established by
the operating system.

Like all security features, the Application Sandbox is not unbreakable.
However, to break out of the Application Sandbox in a properly configured
device, one must compromise the security of the the Linux kernel.

##System Partition and Safe Mode

The system partition contains Android's kernel as well as the operating system
libraries, application runtime, application framework, and applications.  This
partition is set to read-only. When a user boots the device into Safe Mode,
only core Android applications are available. This ensures that the user can
boot their phone into an environment that is free of third-party software.

##Filesystem Permissions

In a UNIX-style environment, filesystem permissions ensure that one user cannot
alter or read another user's files. In the case of Android, each application
runs as its own user. Unless the developer explicitly exposes files to other
applications, files created by one application cannot be read or altered by
another application.

##Filesystem Encryption

Android 3.0 and later provides full filesystem encryption, so all user data can
be encrypted in the kernel using the dmcrypt implementation of AES128 with CBC
and ESSIV:SHA256.   The encryption key is protected by AES128 using a key
derived from the user password, preventing unauthorized access to stored data
without the user device password.   To provide resistance against systematic
password guessing attacks (e.g. “rainbow tables” or brute force), the
password is combined with a random salt and hashed repeatedly with SHA1 using
the standard PBKDF2 algorithm prior to being used to decrypt the filesystem
key. To provide resistance against dictionary password guessing attacks,
Android provides password complexity rules that can be set by the device
administrator and enforced by the operating system. Filesystem encryption
requires the use of a user password, pattern-based screen lock is not supported.

More details on implementation of filesystem encryption are available at
[https://source.android.com/tech/encryption/android_crypto_implementation.html](/
tech/encryption/android_crypto_implementation.html)

##Password Protection

Android can be configured to verify a user-supplied password prior to providing
access to a device. In addition to preventing unauthorized use of the device,
this password protects the cryptographic key for full filesystem encryption.

Use of a password and/or password complexity rules can be required by a device
administrator.

##Device Administration

Android 2.2 and later provide the Android Device Administration API, which
provides device administration features at the system level. For example, the
built-in Android Email application uses the APIs to improve Exchange support.
Through the Email application, Exchange administrators can enforce password
policies — including alphanumeric passwords or numeric PINs — across
devices. Administrators can also remotely wipe (that is, restore factory
defaults on) lost or stolen handsets.

In addition to use in applications included with the Android system, these APIs
are available to third-party providers of Device Management solutions. Details
on the API are provided here:
[https://developer.android.com/guide/topics/admin/device-admin.html](https://devel
oper.android.com/guide/topics/admin/device-admin.html).


##Memory Management Security Enhancements

Android includes many features that make common security issues harder to
exploit. The Android SDK, compilers, and OS use tools to make common memory
corruption issues significantly harder to exploit, including:

**Android 1.5+**

+ ProPolice to prevent stack buffer overruns (-fstack-protector)
+ safe_iop to reduce integer overflows
+ Extensions to OpenBSD dlmalloc to prevent double free() vulnerabilities and
to prevent chunk consolidation attacks.  Chunk consolidation attacks are a
common way to exploit heap corruption.
+ OpenBSD calloc to prevent integer overflows during memory allocation

**Android 2.3+**

+ Format string vulnerability protections (-Wformat-security -Werror=format-security)
+ Hardware-based No eXecute (NX) to prevent code execution on the stack and heap
+ Linux mmap_min_addr to mitigate null pointer dereference privilege
escalation (further enhanced in Android 4.1)

**Android 4.0+**

+ Address Space Layout Randomization (ASLR) to randomize key locations in memory

**Android 4.1+**

+ PIE (Position Independent Executable) support
+ Read-only relocations / immediate binding (-Wl,-z,relro -Wl,-z,now)
+ dmesg_restrict enabled (avoid leaking kernel addresses)
+ kptr_restrict enabled (avoid leaking kernel addresses)

##Rooting of Devices

By default, on Android only the kernel and a small subset of the core
applications run with root permissions. Android does not prevent a user or
application with root permissions from modifying the operating system, kernel,
and any other application.  In general, root has full access to all
applications and all application data. Users that change the permissions on an
Android device to grant root access to applications increase the security
exposure to malicious applications and potential application flaws.

The ability to modify an Android device they own is important to developers
working with the Android platform. On many Android devices users have the
ability to unlock the bootloader in order to allow installation of an alternate
operating system. These alternate operating systems may allow an owner to gain
root access for purposes of debugging applications and system components or to
access features not presented to applications by Android APIs.

On some devices, a person with physical control of a device and a USB cable is
able to install a new operating system that provides root privileges to the
user. To protect any existing user data from compromise the bootloader unlock
mechanism requires that the bootloader erase any existing user data as part of
the unlock step. Root access gained via exploiting a kernel bug or security
hole can bypass this protection.

Encrypting data with a key stored on-device does not protect the application
data from root users. Applications can add a layer of data protection using
encryption with a key stored off-device, such as on a server or a user
password.  This approach can provide temporary protection while the key is not
present, but at some point the key must be provided to the application and it
then becomes accessible to root users.

A more robust approach to protecting data from root users is through the use of
hardware solutions. OEMs may choose to implement hardware solutions that limit
access to specific types of content such as DRM for video playback, or the
NFC-related trusted storage for Google wallet.

In the case of a lost or stolen device, full filesystem encryption on Android
devices uses the device password to protect the encryption key, so modifying
the bootloader or operating system is not sufficient to access user data
without the user’s device password.

#Android Application Security

##Elements of Applications

Android provides an open source platform and application environment for mobile
devices. The core operating system is based on the Linux kernel. Android
applications are most often written in the Java programming language and run in
the Dalvik virtual machine. However, applications can also be written in native
code. Applications are installed from a single file with the .apk file
extension.

The main Android application building blocks are:

+ **AndroidManifest.xml**: The
[AndroidManifest.xml](https://developer.android.com/guide/topics/manifest/manifes
t-intro.html) file is the control file that tells the system what to do with
all the top-level components (specifically activities, services, broadcast
receivers, and content providers described below) in an application. This also
specifies which permissions are required.

+ **Activities**: An
[Activity](https://developer.android.com/guide/topics/fundamentals/activities.htm
l) is, generally, the code for a single, user-focused task.  It usually
includes displaying a UI to the user, but it does not have to -- some
Activities never display UIs.  Typically, one of the application's Activities
is the entry point to an application.

+ **Services**: A
[Service](https://developer.android.com/guide/topics/fundamentals/services.html)
is a body of code that runs in the background. It can run in its own process,
or in the context of another application's process. Other components "bind" to
a Service and invoke methods on it via remote procedure calls. An example of a
Service is a media player: even when the user quits the media-selection UI, the
user probably still intends for music to keep playing. A Service keeps the
music going even when the UI has completed.

+ **Broadcast Receiver**: A
[BroadcastReceiver](https://developer.android.com/reference/android/content/Broad
castReceiver.html) is an object that is instantiated when an IPC mechanism
known as an
[Intent](https://developer.android.com/reference/android/content/Intent.html)
is issued by the operating system or another application.  An application may
register a receiver for the low battery message, for example, and change its
behavior based on that information.


##The Android Permission Model: Accessing Protected APIs

By default, an Android application can only access a limited range of system
resources. The system manages Android application access to resources that, if
used incorrectly or maliciously, could adversely impact the user experience,
the network, or data on the device.

These restrictions are implemented in a variety of different forms.  Some
capabilities are restricted by an intentional lack of APIs to the sensitive
functionality (e.g. there is no Android API for directly manipulating the SIM
card).  In some instances, separation of roles provides a security measure, as
with the per-application isolation of storage. In other instances, the
sensitive APIs are intended for use by trusted applications and protected
through a security mechanism known as Permissions.

These protected APIs include:

+ Camera functions
+ Location data (GPS)
+ Bluetooth functions
+ Telephony functions
+ SMS/MMS functions
+ Network/data connections

These resources are only accessible through the operating system.  To make use
of the protected APIs on the device, an application must define the
capabilities it needs in its manifest.  When preparing to install an
application, the system displays a dialog to the user that indicates the
permissions requested and asks whether to continue the installation.  If the
user continues with the installation, the system accepts that the user has
granted all of the requested permissions. The user can not grant or deny
individual permissions -- the user must grant or deny all of the requested
permissions as a block.

Once granted, the permissions are applied to the application as long as it is
installed.  To avoid user confusion, the system does not notify the user again
of the permissions granted to the application, and applications that are
included in the core operating system or bundled by an OEM do not request
permissions from the user. Permissions are removed if an application is
uninstalled, so a subsequent re-installation will again result in display of
permissions.

Within the device settings, users are able to view permissions for applications
they have previously installed. Users can also turn off some functionality
globally when they choose, such as disabling GPS, radio, or wi-fi.

In the event that an application attempts to use a protected feature which has
not been declared in the application's manifest, the permission failure will
typically result in a security exception being thrown back to the application.
Protected API permission checks are enforced at the lowest possible level to
prevent circumvention. An example of the user messaging when an application is
installed while requesting access to protected APIs is shown in *Figure 2*.

The system default permissions are described at
[https://developer.android.com/reference/android/Manifest.permission.html](https://developer.android.com/reference/android/Manifest.permission.html).
Applications may declare their own permissions for other applications to use.
Such permissions are not listed in the above location.

When defining a permission a protectionLevel attribute tells the system how the
user is to be informed of applications requiring the permission, or who is
allowed to hold a permission. Details on creating and using application
specific permissions are described at
[https://developer.android.com/guide/topics/security/security.html](https://develo
per.android.com/guide/topics/security/security.html).

There are some device capabilities, such as the ability to send SMS broadcast
intents, that are not available to third-party applications, but that may be
used by applications pre-installed by the OEM. These permissions use the
signatureOrSystem permission.

##How Users Understand Third-Party Applications

Android strives to make it clear to users when they are interacting with
third-party applications and inform the user of the capabilities those
applications have.  Prior to installation of any application, the user is shown
a clear message about the different permissions the application is requesting.
After install, the user is not prompted again to confirm any permissions.

There are many reasons to show permissions immediately prior to installation
time. This is when user is actively reviewing information about the
application, developer, and functionality to determine whether it matches their
needs and expectations.  It is also important that they have not yet
established a mental or financial commitment to the app, and can easily compare
the application to other alternative applications.

Some other platforms use a different approach to user notification, requesting
permission at the start of each session or while applications are in use. The
vision of Android is to have users switching seamlessly between applications at
will. Providing confirmations each time would slow down the user and prevent
Android from delivering a great user experience. Having the user review
permissions at install time gives the user the option to not install the
application if they feel uncomfortable.

Also, many user interface studies have shown that over-prompting the user
causes the user to start saying "OK" to any dialog that is shown. One of
Android's security goals is to effectively convey important security
information to the user, which cannot be done using dialogs that the user will
be trained to ignore. By presenting the important information once, and only
when it is important, the user is more likely to think about what they are
agreeing to.

Some platforms choose not to show any information at all about application
functionality. That approach prevents users from easily understanding and
discussing application capabilities. While it is not possible for all users to
always make fully informed decisions, the Android permissions model makes
information about applications easily accessible to a wide range of users.  For
example, unexpected permissions requests can prompt more sophisticated users to
ask critical questions about application functionality and share their concerns
in places such as [Google Play](htts://play.google.com) where they
are visible to all users.

<table>
<tr>
<td><strong>Permissions at Application Install -- Google Maps</strong></td>
<td><strong>Permissions of an Installed Application -- gMail</strong></td>
</tr>
<tr>
<td>
<img alt="Permissions at Application Install -- Google Maps" width=250
src="images/image_install.png"/>
</td>
<td>
<img alt="Permissions of an Installed Application -- gMail" width=250
src="images/image_gmail_installed.png"/>
</td>
</tr>
</table>
*Figure 2: Display of permissions for applications*

##Interprocess Communication

Processes can communicate using any of the traditional UNIX-type mechanisms.
Examples include the filesystem, local sockets, or signals. However, the Linux
permissions still apply.

Android also provides new IPC mechanisms:

+ **Binder**: A lightweight capability-based remote procedure call mechanism
designed for high performance when performing in-process and cross-process
calls. Binder is implemented using a custom Linux driver. See
[https://developer.android.com/reference/android/os/Binder.html](https://developer
.android.com/reference/android/os/Binder.html).

+ **Services**: Services (discussed above) can provide interfaces directly
accessible using binder.

+ **Intents**: An Intent is a simple message object that represents an
"intention" to do something. For example, if your application wants to display
a web page, it expresses its "Intent" to view the URL by creating an Intent
instance and handing it off to the system. The system locates some other piece
of code (in this case, the Browser) that knows how to handle that Intent, and
runs it. Intents can also be used to broadcast interesting events (such as a
notification) system-wide. See
[https://developer.android.com/reference/android/content/Intent.html](https://developer.android.com/reference/android/content/Intent.html.

+ **ContentProviders**: A ContentProvider is a data storehouse that provides
access to data on the device; the classic example is the ContentProvider that
is used to access the user's list of contacts. An application can access data
that other applications have exposed via a ContentProvider, and an application
can also define its own ContentProviders to expose data of its own. See
[https://developer.android.com/reference/android/content/ContentProvider.html](https://developer.android.com/reference/android/content/ContentProvider.html).

While it is possible to implement IPC using other mechanisms such as network
sockets or world-writable files, these are the recommended Android IPC
frameworks. Android developers will be encouraged to use best practices around
securing users' data and avoiding the introduction of security vulnerabilities.

##Cost-Sensitive APIs

A cost sensitive API is any function that might generate a cost for the user or
the network. The Android platform has placed cost sensitive APIs in the list of
protected APIs controlled by the OS. The user will have to grant explicit
permission to third-party applications requesting use of cost sensitive APIs.
These APIs include:

+ Telephony
+ SMS/MMS
+ Network/Data
+ In-App Billing
+ NFC Access

##SIM Card Access

Low level access to the SIM card is not available to third-party apps. The OS
handles all communications with the SIM card including access to personal
information (contacts) on the SIM card memory. Applications also cannot access
AT commands, as these are managed exclusively by the Radio Interface Layer
(RIL). The RIL provides no high level APIs for these commands.

##Personal Information

Android has placed APIs that provide access to user data into the set of
protected APIs.  With normal usage, Android devices will also accumulate user
data within third-party applications installed by users.   Applications that
choose to share this information can use Android OS permission checks to
protect the data from third-party applications.

![Figure 3: Access to sensitive user data is only available through protected
APIs](images/image03.png)

*Figure 3: Access to sensitive user data is only available through protected
APIs*

System content providers that are likely to contain personal or personally
identifiable information such as contacts and calendar have been created with
clearly identified permissions. This granularity provides the user with clear
indication of the types of information that may be provided to the application.
 During installation, a third-party application may request permission to
access these resources.  If permission is granted, the application can be
installed and will have access to the data requested at any time when it is
installed.

Any applications which collect personal information will, by default, have that
data restricted only to the specific application.  If an application chooses to
make the data available to other applications though IPC, the application
granting access can apply permissions to the IPC mechanism that are enforced by
the operating system.

##Sensitive Data Input Devices

Android devices frequently provide sensitive data input devices that allow
applications to interact with the surrounding environment, such as camera,
microphone or GPS.  For a third-party application to access these devices, it
must first be explicitly provided access by the user through the use of Android
OS Permissions.  Upon installation, the installer will prompt the user
requesting permission to the sensor by name.

If an application wants to know the user's location, the application requires a
permission to access the user's location. Upon installation, the installer will
prompt the user asking if the application can access the user's location. At
any time, if the user does not want any application to access their location,
then the user can run the "Settings" application, go to "Location & Security",
and uncheck the "Use wireless networks" and "Enable GPS satellites". This will
disable location based services for all applications on the user's device.

##Device Metadata

Android also strives to restrict access to data that is not intrinsically
sensitive, but may indirectly reveal characteristics about the user, user
preferences, and the manner in which they use a device.

By default applications do not have access to operating system logs,
browser history, phone number, or hardware / network identification
information.  If an application requests access to this information at install
time, the installer will prompt the user asking if the application can access
the information. If the user does not grant access, the application will not be
installed.

##Application Signing

Code signing allows developers to identify the author of the application and to
update their application without creating complicated interfaces and
permissions. Every application that is run on the Android platform must be
signed by the developer.  Applications that attempt to install without being
signed will rejected by either Google Play or the package installer on
the Android device.

On Google Play, application signing bridges the trust Google has with the
developer and the trust the developer has with their application.  Developers
know their application is provided, unmodified to the Android device; and
developers can be held accountable for behavior of their application.

On Android, application signing is the first step to placing an application in
its Application Sandbox. The signed application certificate defines which user
id is associated with which application; different applications run under
different user IDs. Application signing ensures that one application cannot
access any other application except through well-defined IPC.

When an application (APK file) is installed onto an Android device, the Package
Manager verifies that the APK has been properly signed with the certificate
included in that APK.  If the certificate (or, more accurately, the public key
in the certificate) matches the key used to sign any other APK on the device,
the new APK has the option to specify in the manifest that it will share a UID
with the other similarly-signed APKs.

Applications can be signed by a third-party (OEM, operator, alternative market)
or self-signed. Android provides code signing using self-signed certificates
that developers can generate without external assistance or permission.
Applications do not have to be signed by a central authority. Android currently
does not perform CA verification for application certificates.

Applications are also able to declare security permissions at the Signature
protection level, restricting access only to applications signed with the same
key while maintaining distinct UIDs and Application Sandboxes. A closer
relationship with a shared Application Sandbox is allowed via the [shared UID
feature](https://developer.android.com/guide/topics/manifest/manifest-element.htm
l#uid) where two or more applications signed with same developer key can
declare a shared UID in their manifest.

##Digital Rights Management

The Android platform provides an extensible DRM framework that lets
applications manage rights-protected content according to the license
constraints that are associated with the content. The DRM framework supports
many DRM schemes; which DRM schemes a device supports is left to the device
manufacturer.

The [Android DRM
framework](https://developer.android.com/reference/android/drm/package-summary.ht
ml) is implemented in two architectural layers (see figure below):

+ A DRM framework API, which is exposed to applications through the Android
application framework and runs through the Dalvik VM for standard applications.

+ A native code DRM manager, which implements the DRM framework and exposes an
interface for DRM plug-ins (agents) to handle rights management and decryption
for various DRM schemes

![Figure 4: Architecture of Digital Rights Management on Android
platform](images/image02.png)

*Figure 4: Architecture of Digital Rights Management on Android platform*

#Android Updates

Android provides system updates for both security and feature related purposes.

There are two ways to update the code on most Android devices: over-the-air
(OTA updates) or side-loaded updates. OTA updates can be rolled out over a
defined time period or be pushed to all devices at once, depending on how the
OEM and/or carrier would like to push the updates. Side-loaded updates can be
provided from a central location for users to download as a zip file to their
local desktop machine or directly to their handset. Once the update is copied
or downloaded to the SD card on the device, Android will recognize the update,
verify its integrity and authenticity, and automatically update the device.

If a dangerous vulnerability is discovered internally or responsibly reported
to Google or the Android Open Source Project, the Android security team will
start the following process.

1. The Android team will notify companies who have signed NDAs regarding the
problem and begin discussing the solution.
2. The owners of code will begin the fix.
3. The Android team will fix Android-related security issues.
4. When a patch is available, the fix is provided to the NDA companies.
5. The Android team will publish the patch in the Android Open Source Project
6. OEM/carrier will push an update to customers.

The NDA is required to ensure that the security issue does not become public
prior to availabilty of a fix and put users at risk. Many OHA members run their
own code on Android devices such as the bootloader, wifi drivers, and the
radio. Once the Android Security team is notified of a security issue in this
partner code, they will consult with OHA partners to quickly find a fix for the
problem at hand and similar problems. However, the OHA member who wrote the
faulty code is ultimately responsible for fixing the problem.

If a dangerous vulnerability is not responsibly disclosed (e.g., if it is
posted to a public forum without warning), then Google and/or the Android Open
Source Project will work as quickly as possible to create a patch. The patch
will released to the public (and any partners) when the patch is tested and
ready for use.

At Google I/O 2011, many of the largest OHA partners committed to providing
updates to devices for 18 months after initial shipment. This will provide
users with access to the most recent Android features, as well as security
updates.

Any developer, Android user, or security researcher can notify the Android
security team of potential security issues by sending email to
security@android.com. If desired, communication can be encrypted using the
Android security team PGP key available here:
[https://developer.android.com/security_at_android_dot_com.txt](https://develope
r.android.com/security_at_android_dot_com.txt).

#Other Resources

Information about the Android Open Source Project is available at
[https://source.android.com](https://source.android.com).

Information for Android application developers is here:
[https://developer.android.com](https://developer.android.com).

The Android Security team can be reached at
[security@android.com](mailto:security@android.com).

Security information exists throughout the Android Open Source and Developer
Sites. A good place to start is here:
[https://developer.android.com/guide/topics/security/security.html](https://develo
per.android.com/guide/topics/security/security.html).

A Security FAQ for developers is located here:
[https://developer.android.com/resources/faq/security.html](https://developer.andr
oid.com/resources/faq/security.html).

A community resource for discussion about Android security exists here:
[https://groups.google.com/forum/?fromgroups#!forum/android-security-discuss](https://groups.google.com/forum/?fromgroups#!forum/android-security-discuss).

