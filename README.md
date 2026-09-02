[English](README.md) [日本語](README_ja.md)
<!--
### ⚠️ This project is still a work in progress. While the IntDev version source code is available for early access, it may contain bugs.<br>It is highly recommended to wait for the stable release on May 12th.
-->

# Glyph Barty

A Glyph visualizer that is cooler than the official feature and supports a wider range of devices.

## Overview

Up until the Phone (3a), Nothing smartphones had an official feature that allowed the Glyph lights to pulse in sync with music.

However, this feature was removed in later models. Furthermore, the official implementation felt somewhat random and wasn't particularly "cool."

To solve this, I created a "Better" Glyph visualizer that works on unsupported devices like the Phone (3) and offers a more polished visual experience than the original.

### Features

* **Real-time Visualization:** The Glyph Interface lights up in sync with your currently playing music.
* **Quick Settings Integration:** Easily toggle the feature ON/OFF via a Quick Settings tile.
  * It feels just like a native OS feature!
* Extra features (e.g. displaying charging status on the Glyph Interface)
* **No Screen Recording Required:** Unlike other apps that require screen recording permissions to capture audio, this app works without them.

## Supported Devices

* Nothing Phone (2)
* Nothing Phone (2a)
* Nothing Phone (2a) Plus
* Nothing Phone (3a)
* Nothing Phone (3a) Pro
* Nothing Phone (3)
* Nothing Phone (4b)
* Nothing Phone (4a)
* Nothing Phone (4a) Pro

### Supported Devices (Conditional)

* Nothing Phone (1)
  * The **Glyph Interface debug mode** must be enabled. You can do this by running the ADB command ``` adb shell settings put global nt_glyph_interface_debug_enable 1 ```, or automatically from within the app if you use Shizuku.

<br>

Compatible with almost all Nothing Phone models.

*Note: Phone (3a) Lite is not supported as the SDK has not been released.*

## How to Use

1. Download and install the latest APK from the **Releases** section.
2. Grant the "Record Audio" permission when prompted by the app.
3. Tap the **Start** button and play your favorite music.
4. Select your preferred lighting pattern.
5. Adjust the parameters to customize the movement to your liking.

## Contributing & Support

If you encounter any issues, please open an **Issue** or submit a **Pull Request**.

Please note that I am a student developing this as a hobby, so I may not be able to respond immediately. Your patience is appreciated!

## License

This project is licensed under the **MIT License**.
Feel free to modify and use it as you wish.

## Screenshots

### UI mode: Nothing-like
<img width="200" alt="Screenshot_20260827-004135" src="https://github.com/user-attachments/assets/1d218638-f23f-4260-981e-8373c8572099" />
<img width="200" alt="Screenshot_20260827-004137" src="https://github.com/user-attachments/assets/647e8ce5-7b30-4866-ab2b-e7c908e590e8" />
<img width="200" alt="Screenshot_20260827-004141" src="https://github.com/user-attachments/assets/8f0cbad0-42d2-4c0b-9c39-63592ec9dc98" />

### UI mode: Material 3
<img width="200" alt="Screenshot_20260827-010432" src="https://github.com/user-attachments/assets/9922b02b-6b4d-4ed1-a2a5-d9e877ae4d7f" />
<img width="200" alt="Screenshot_20260827-010434" src="https://github.com/user-attachments/assets/9e9f03e3-c8f0-452e-b3cc-2c8e18d7e90d" />
<img width="200" alt="Screenshot_20260827-010439" src="https://github.com/user-attachments/assets/e2b23462-dbb4-4cb8-acbe-9ec2b77fdd81" />





