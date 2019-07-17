# WiFi password application

<p align="left">
	<img src="https://github.com/HiroshiNohara/WIFI-password-application/blob/master/Screenshots/icon.png" alt="Sample"  width="100" height="100"></p>



Since Hydrogen and Oxygen Operating System for OnePlus do not support viewing the password of the connected WiFi, I developed this application to achieve this function. Obviously, it needs to work on a phone that already has **Root** privileges to access the `/data/misc/wifi/WifiConfigStore.xml` file.

## Interface

This application is very convenient, click the launcher icon and grant the necessary permissions to view the corresponding WiFi information:

### Main interface

<p align="left">
	<img src="https://github.com/HiroshiNohara/WIFI-password-application/blob/master/Screenshots/Screenshot0.jpg" alt="Sample"  width="360" height="780"></p>

### Scan QR code

<p align="left">
	<img src="https://github.com/HiroshiNohara/WIFI-password-application/blob/master/Screenshots/Screenshot1.jpg" alt="Sample"  width="360" height="780">
</p>

### Create WiFi QR code

<p align="left">
	<img src="https://github.com/HiroshiNohara/WIFI-password-application/blob/master/Screenshots/Screenshot2.jpg" alt="Sample"  width="360" height="780">
</p>

## Application function

### All features of this application will not generate any data traffic. The application function list is as follows:

- Generate QR code for all saved WiFi
- Scan WiFi QR code and automatically connect to the specified WLAN
- Generate any WiFi QR code for easy sharing
- List supports for hiding no password WiFi
- Support setting WiFi password is not visible
- Set notes for all saved WiFi
- Highlight the WiFi you specified in the list
- Convenient WiFi name and password copy
- Quickly fuzzy query saved WiFi
- Thorough Simplified Chinese, traditional Chinese and English support
- Quick entry(App shortcuts)
- More……

## Precautions

- This application requires you to provide **Root** and **Camera** permissions. **Camera** permission is only used to scan QR codes.
- Due to the change of the strategy of the Android 9.0 or higher system, this part of the device needs to provide additional **Location** permission to allow the application to access the information of the currently connected WiFi.
- This application will only read the `WifiConfigStore.xml` file and will not delete or modify the content.
- This application uses the WiFi name as the database unique identifier and will not store the saved WiFi password.
- This application has passed the test on Huawei Mate 9(Android 8.0), Xiaomi Mi 8(Android 9.0) and OnePlus 7(Android 9.0). In theory, it is suitable for Android 8.0 and above.

## Change logs

### 1.0

- Release the application

## Acknowledgement

- The application uses some open source frameworks during the development process, including: [FloatMenu](https://github.com/JavaNoober/FloatMenu), [LitePal](https://github.com/LitePalFramework/LitePal) and [zxing custom made by yuzhiqiang1993](https://github.com/yuzhiqiang1993/zxing). Thanks to these open source spirit of these framework authors.
- This application interface refers to another WiFi password viewing application [WiFi密码](https://www.coolapk.com/apk/com.wifi.password), thanks to the developer's beautiful interface.
