
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
        Scan
      </TouchableHighlight>
    )
  }
}
```
