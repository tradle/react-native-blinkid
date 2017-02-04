
# react-native-blinkid

React Native adapter for MicroBlink's BlinkID SDK

## Usage

```sh
yarn add react-native-blinkid # or npm i --save react-native-blinkid
react-native link react-native-blinkid
```

## iOS

add to Podfile:

```
target 'yourprojectname' do
  pod 'PPBlinkID', '~> 2.5.1'
end
```

## Android

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
