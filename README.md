
# react-native-blinkid

React Native adapter for MicroBlink's BlinkID SDK

## Usage

```sh
yarn add react-native-blinkid # or npm i --save react-native-blinkid
react-native link react-native-blinkid
```

## iOS

Follow the BlinkID SDK's instructions for installing [their SDK](https://github.com/BlinkID/blinkid-ios/wiki/Getting-started) 

Afterwards, if you used Cocoapods to install, double-check the size of the installed MicroBlink.framework:

```sh
$ find . -name MicroBlink.framework -exec du -sh {} \;
170M  ./iOS/Pods/PPBlinkID/MicroBlink.framework
```

If it's less than 100MB, something's wrong. Clean your Pods cache, make sure `git lfs` got installed, open and close all the doors and windows in your grandmother's house, and try again.

## Android

You need to use SDK 25 in your project (set `compileSdkVersion 25`, `buildToolsVersion '25.0.3'` and `compile 'com.android.support:appcompat-v7:25.+'`).

### Automatic

```sh
react-native link react-native-blinkid
```

### Manual

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import io.tradle.blinkid.RNBlinkIDPackage;` to the imports at the top of the file
  - Add `new RNBlinkIDPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:

```
include ':react-native-blinkid'
project(':react-native-blinkid').projectDir = new File(rootProject.projectDir,  '../node_modules/react-native-blinkid/android')
```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:

```
compile project(':react-native-blinkid')
```

### For both (automatic and manual) installations

Add maven repo `maven { url 'http://maven.microblink.com' }` to `android/build.gradle` allprojects repositories

## Usage
## JS

```js
import { scan, dismiss, setLicenseKey } from 'react-native-blinkid'

setLicenseKey('..your license key from MicroBlink..')

class MyComponent extends React.Component {
  async scan() {
    const result = await scan({
      // more detailed options not yet supported
      mrtd: {} // or usdl: {} or eudl: {}
    })
  }
  render() {
    return (
      <TouchableHighlight>
        onPress={this.scan.bind(this)}>
        <Text>Scan</Text>
      </TouchableHighlight>
    )
  }
}
```
