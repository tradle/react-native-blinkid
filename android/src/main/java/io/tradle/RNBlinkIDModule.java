
package io.tradle;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Base64;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.microblink.activity.ScanCard;
import com.microblink.hardware.camera.CameraType;
import com.microblink.metadata.MetadataSettings;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.IResultHolder;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.blinkbarcode.usdl.USDLRecognizerSettings;
import com.microblink.recognizers.blinkbarcode.usdl.USDLScanResult;
import com.microblink.recognizers.blinkid.eudl.EUDLCountry;
import com.microblink.recognizers.blinkid.eudl.EUDLRecognitionResult;
import com.microblink.recognizers.blinkid.eudl.EUDLRecognizerSettings;
import com.microblink.recognizers.blinkid.mrtd.MRTDRecognitionResult;
import com.microblink.recognizers.blinkid.mrtd.MRTDRecognizerSettings;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.recognizers.settings.RecognizerSettingsUtils;
import com.microblink.results.date.DateResult;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RNBlinkIDModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

  private final ReactApplicationContext reactContext;
  private final String notSupportedBecause;
  private String licenseKey;
  private static final String E_USER_CANCELED = "RNMBUserCanceledError";
  private static final String E_FAILED_INVALID = "RNMBInvalidResultError";
  private static final String E_FAILED_EMPTY = "RNMBEmptyResultError";
  private static final String E_FAILED_NO_AUTOFOCUS = "RNMBNoAutofocusError";
  private static final String E_DEVELOPER_ERROR = "RNMBDeveloperError";
  private static final String JPEG_DATA_URI_PREFIX = "data:image/jpeg;base64,";
  private static final String TYPE_EUDL = "eudl";
  private static final String TYPE_MRTD = "mrtd";
  private static final String TYPE_USDL = "usdl";
  private static final String KEY_NOT_SUPPORTED_BECAUSE = "notSupportedBecause";
  private static final String KEY_PREFIX_SUPPORTS = "supports";
  private static final int SCAN_REQUEST_CODE = 5792151;
  private static final Map<EUDLCountry, String> eudlCountryToString;
  private static final Map<String, EUDLCountry> countryToEUDLCountry;
  private final Map<String, Object> constants = new HashMap<>();
  static
  {
    eudlCountryToString = new HashMap<>();
    countryToEUDLCountry = new HashMap<>();
    eudlCountryToString.put(EUDLCountry.EUDL_COUNTRY_UK, "UK");
    eudlCountryToString.put(EUDLCountry.EUDL_COUNTRY_GERMANY, "DE");
    eudlCountryToString.put(EUDLCountry.EUDL_COUNTRY_AUSTRIA, "AT");
    countryToEUDLCountry.put("UK", EUDLCountry.EUDL_COUNTRY_UK);
    countryToEUDLCountry.put("DE", EUDLCountry.EUDL_COUNTRY_GERMANY);
    countryToEUDLCountry.put("AT", EUDLCountry.EUDL_COUNTRY_AUSTRIA);
  }

  // scan session variables
  private ReadableMap opts;
  private Promise scanPromise;

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      if (requestCode != SCAN_REQUEST_CODE) return;
      if (resultCode == ScanCard.RESULT_CANCELED) {
        onCanceled();
        return;
      }

      if (resultCode != ScanCard.RESULT_OK || data == null) {
        resetForNextScan();
        return;
      }

      // perform processing of the data here

      // for example, obtain parcelable recognition result
      Bundle extras = data.getExtras();
      RecognitionResults scanResult = data.getParcelableExtra(ScanCard.EXTRAS_RECOGNITION_RESULTS);

      Map<String, Bitmap> images = RNBlinkIDImageManager.get();

      // get array of recognition results
      BaseRecognitionResult[] resultArray = scanResult.getRecognitionResults();
      WritableMap allResults = Arguments.createMap();
      boolean yay = false;
      boolean hasEmpty = false;
      boolean hasInvalid = false;
      for (BaseRecognitionResult baseResult : resultArray) {
        if (!baseResult.isValid()) {
          hasInvalid = true;
          continue;
        }

        if (baseResult.isEmpty()) {
          hasEmpty = true;
          continue;
        }

        String type = null;
        WritableMap personal = Arguments.createMap();
        WritableMap address = Arguments.createMap();
        WritableMap document = Arguments.createMap();
        WritableMap resultsMap = Arguments.createMap();
        if (baseResult instanceof EUDLRecognitionResult) {
          type = TYPE_EUDL;
          EUDLRecognitionResult result = (EUDLRecognitionResult) baseResult;
          IResultHolder resultHolder = result.getResultHolder();
          document.putString("birthData", resultHolder.getString("ownerBirthData"));
          document.putString("dateOfExpiryStr", resultHolder.getString("documentExpiryDate"));
          document.putString("dateOfIssueStr", resultHolder.getString("documentIssueDate"));

          personal.putString("firstName", result.getFirstName());
          personal.putString("lastName", result.getLastName());
          personal.putString("placeOfBirth", result.getPlaceOfBirth());
          personal.putDouble("dateOfBirth", result.getDateOfBirth().getTime());
          document.putString("documentNumber", result.getDriverNumber());
          document.putDouble("dateOfIssue", result.getDocumentIssueDate().getTime());
          document.putDouble("dateOfExpiry", result.getDocumentExpiryDate().getTime());
          document.putString("issuer",  result.getDocumentIssuingAuthority());
          EUDLCountry eudlCountry = result.getCountry();
          if (eudlCountry != EUDLCountry.EUDL_COUNTRY_AUTO) {
            document.putString("country", eudlCountryToString.get(eudlCountry));
          }

          address.putString("full", result.getAddress());
        } else if (baseResult instanceof USDLScanResult) {
          USDLScanResult result = (USDLScanResult) baseResult;
          if (result.isUncertain()) {
            hasInvalid = true;
            continue;
          }

          type = TYPE_USDL;
          personal.putString("firstName", result.getField(USDLScanResult.kCustomerFirstName));
          personal.putString("lastName", result.getField(USDLScanResult.kCustomerFamilyName));
          personal.putString("fullName", result.getField(USDLScanResult.kCustomerFullName));
          personal.putString("dateOfBirth", result.getField(USDLScanResult.kDateOfBirth));
          String sex = result.getField(USDLScanResult.kSex).equalsIgnoreCase("1") ? "M" : "F";
          personal.putString("kPPSex", sex);
          personal.putString("eyeColor", result.getField(USDLScanResult.kEyeColor));
          personal.putString("heightCm", result.getField(USDLScanResult.kHeight));

          address.putString("full", result.getField(USDLScanResult.kFullAddress));
          address.putString("street", result.getField(USDLScanResult.kAddressStreet));
          address.putString("city", result.getField(USDLScanResult.kAddressCity));
          address.putString("state", result.getField(USDLScanResult.kAddressJurisdictionCode));
          address.putString("postalCode", result.getField(USDLScanResult.kAddressJurisdictionCode));

          document.putString("dateOfIssue", result.getField(USDLScanResult.kDocumentIssueDate));
          document.putString("dateOfExpiry", result.getField(USDLScanResult.kDocumentExpirationDate));
          document.putString("issueIdentificationNumber", result.getField(USDLScanResult.kIssuerIdentificationNumber));
          document.putString("jurisdictionVersionNumber", result.getField(USDLScanResult.kJurisdictionVersionNumber));
          document.putString("jurisdictionVehicleClass", result.getField(USDLScanResult.kJurisdictionVehicleClass));
          document.putString("jurisdictionRestrictionCodes", result.getField(USDLScanResult.kJurisdictionRestrictionCodes));
          document.putString("jurisdictionEndorsementCodes", result.getField(USDLScanResult.kJurisdictionEndorsementCodes));
          document.putString("documentNumber", result.getField(USDLScanResult.kCustomerIdNumber));
          // deprecated
          document.putString("customerIdNumber", result.getField(USDLScanResult.kCustomerIdNumber));
        } else if (baseResult instanceof MRTDRecognitionResult) {
          type = TYPE_MRTD;
          MRTDRecognitionResult result = (MRTDRecognitionResult) baseResult;
          if (result.isMRZParsed()) {
            personal.putString("lastName", result.getPrimaryId());
            personal.putString("firstName", result.getSecondaryId());
            personal.putString("nationality", result.getNationality());
            personal.putDouble("dateOfBirth", result.getDateOfBirth().getTime());
            personal.putString("dateOfBirthStr", result.getRawDateOfBirth());
            personal.putString("sex", result.getSex());

            document.putString("issuer", result.getIssuer());
            document.putString("documentNumber", result.getDocumentNumber());
            document.putString("documentCode", result.getDocumentCode());
            document.putDouble("dateOfExpiry", result.getDateOfExpiry().getTime());
            personal.putString("dateOfExpiryStr", result.getRawDateOfExpiry());
            document.putString("opt1", result.getOpt1());
            document.putString("opt2", result.getOpt2());
            document.putString("mrzText", result.getMRZText());
          } else {
            // attempt to parse OCR result by yourself
            // or ask user to try again
            resultsMap.putString("ocr", result.getOcrResult().toString());
          }
        }

        if (type == null) continue;

        // only one result for now
        yay = true;
        resultsMap.putMap("personal", personal);
        resultsMap.putMap("address", address);
        resultsMap.putMap("document", document);
        allResults.putMap(type, resultsMap);
        Bitmap bitmap = images.get(type.toUpperCase());
        if (bitmap == null && type.equalsIgnoreCase(TYPE_USDL)) {
          bitmap = images.get("Success");
        }

        if (bitmap != null) {
          allResults.putMap("image", serializeImage(bitmap));
        }
      }

      if (yay) {
        scanPromise.resolve(allResults);
      } else if (hasInvalid) {
        // not all relevant data was scanned, ask user
        // to try again
        onInvalidResultError();
      } else if (hasEmpty) {
        // not all relevant data was scanned, ask user
        // to try again
        onEmptyError();
      } else {
        onDeveloperError();
//        scanPromise.reject(E_DEVELOPER_ERROR, "This should not happen, please report to react-native-blinkid developers");
      }

      resetForNextScan();
    }
  };

  public RNBlinkIDModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addActivityEventListener(mActivityEventListener);
    RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(reactContext);
    if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
      this.notSupportedBecause = supportStatus.name();
      return;
    }

    this.notSupportedBecause = null;
    if (RecognizerCompatibility.cameraHasAutofocus(CameraType.CAMERA_BACKFACE, reactContext)) {
      supports(TYPE_EUDL, true);
      supports(TYPE_USDL, true);
      supports(TYPE_MRTD, true);
      return;
    }

    EUDLRecognizerSettings eudl = new EUDLRecognizerSettings(EUDLCountry.EUDL_COUNTRY_AUTO);
    USDLRecognizerSettings usdl = new USDLRecognizerSettings();
    MRTDRecognizerSettings mrtd = new MRTDRecognizerSettings();
    RecognizerSettings[] settings = new RecognizerSettings[]{ eudl, usdl, mrtd };
    RecognizerSettings[] supported = RecognizerSettingsUtils.filterOutRecognizersThatRequireAutofocus(settings);
    for (RecognizerSettings setting: supported) {
      if (setting == eudl) {
        supports(TYPE_EUDL, true);
      } else if (setting == usdl) {
        supports(TYPE_USDL, true);
      } else if (setting == mrtd) {
        supports(TYPE_MRTD, true);
      }
    }
  }

  private void supports(String type, boolean does) {
    constants.put(KEY_PREFIX_SUPPORTS + type.toUpperCase(), does);
  }

  @Override
  public void onHostDestroy() {
    resetForNextScan();
  }

  @Override
  public void onHostPause() {
  }

  @Override
  public void onHostResume() {
  }

  @Override
  public String getName() {
    return "RNBlinkID";
  }

  @Override
  public Map<String, Object> getConstants() {
    if (this.notSupportedBecause != null) {
      constants.put(KEY_NOT_SUPPORTED_BECAUSE, this.notSupportedBecause);
    }

    return constants;
  }

  @ReactMethod
  public void setLicenseKey(String licenseKey, final Promise promise) {
    this.licenseKey = licenseKey;
    promise.resolve(null);
  }

//  @ReactMethod
//  public void dismiss(final Promise promise) {
//    resetForNextScan();
//    promise.resolve(null);
//  }

  @ReactMethod
  public void scan(ReadableMap opts, final Promise promise) {
    String licenseKey = getString(opts, "licenseKey");
    if (licenseKey == null) {
      licenseKey = this.licenseKey;
    }

    resetForNextScan();
    this.scanPromise = promise;
    this.opts = opts;

    Activity currentActivity = getCurrentActivity();
    Intent intent = new Intent(currentActivity, ScanCard.class);
    intent.putExtra(ScanCard.EXTRAS_LICENSE_KEY, licenseKey);
    intent.putExtra(ScanCard.EXTRAS_CAMERA_TYPE, (Parcelable) CameraType.CAMERA_BACKFACE);

    RecognitionSettings settings = new RecognitionSettings();
    RecognizerSettings[] recognizerSettings = getRecognitionSettings(opts);
    if (!RecognizerCompatibility.cameraHasAutofocus(CameraType.CAMERA_BACKFACE, reactContext)) {
      int length = recognizerSettings.length;
      recognizerSettings = RecognizerSettingsUtils.filterOutRecognizersThatRequireAutofocus(recognizerSettings);
      if (recognizerSettings.length != length) {
        reject(E_FAILED_NO_AUTOFOCUS);
        return;
      }
    }

    settings.setRecognizerSettingsArray(recognizerSettings);
    if (opts.hasKey("timeout")) {
      settings.setNumMsBeforeTimeout(opts.getInt("timeout"));
    }

    intent.putExtra(ScanCard.EXTRAS_RECOGNITION_SETTINGS, settings);
    // pass implementation of image listener that will obtain document images
    intent.putExtra(ScanCard.EXTRAS_IMAGE_LISTENER, new MyImageListener());
    // pass image metadata settings that specifies which images will be obtained
    intent.putExtra(ScanCard.EXTRAS_IMAGE_METADATA_SETTINGS, getImageMetadataSettings(opts));

    // Starting Activity
    currentActivity.startActivityForResult(intent, SCAN_REQUEST_CODE);
  }

  private void onCanceled() {
    reject(E_USER_CANCELED);
    resetForNextScan();
  }

  private void onInvalidResultError() {
    reject(E_FAILED_INVALID);
    resetForNextScan();
  }

  private void onEmptyError() {
    reject(E_FAILED_EMPTY);
    resetForNextScan();
  }

  private void onDeveloperError() {
    reject(E_DEVELOPER_ERROR);
    resetForNextScan();
  }

  private void reject(String message) {
    if (scanPromise != null) {
      scanPromise.reject(null, message);
    }
  }

  public void resetForNextScan() {
    scanPromise = null;
    opts = null;
    RNBlinkIDImageManager.clear();
  }

  private String getString(ReadableMap map, String key) {
    return map.hasKey(key) ? map.getString(key) : null;
  }

  private boolean getBoolean(ReadableMap map, String key) {
    return map.hasKey(key) ? map.getBoolean(key) : false;
  }

  private MetadataSettings.ImageMetadataSettings getImageMetadataSettings(ReadableMap opts) {
    MetadataSettings.ImageMetadataSettings ims = new MetadataSettings.ImageMetadataSettings();
    String imagePath = getString(opts, "imagePath");
    boolean outputBase64 = getBoolean(opts, "base64");
    boolean needImage = imagePath != null || outputBase64;
    if (needImage) {
      // enable returning of dewarped images, if they are available
      ims.setDewarpedImageEnabled(true);
      // enable returning of image that was used to obtain valid scanning result
      if (opts.hasKey(TYPE_USDL)) {
        ims.setSuccessfulScanFrameEnabled(true);
      }
    }

    return ims;
  }

  private RecognizerSettings[] getRecognitionSettings(ReadableMap opts) {
    // TODO: interpret actual settings
    ArrayList<RecognizerSettings> settings = new ArrayList<>();

    if (opts.hasKey(TYPE_USDL)) {
      USDLRecognizerSettings sett = new USDLRecognizerSettings();
      sett.setUncertainScanning(false);
      // disable scanning of barcodes that do not have quiet zone
      // as defined by the standard
      sett.setNullQuietZoneAllowed(false);
      settings.add(sett);
    }

    if (opts.hasKey(TYPE_EUDL)) {
      ReadableMap eudlOpts = opts.getMap(TYPE_EUDL);
      EUDLCountry country = null;
      String countryStr = getString(opts, "issuer");
      if (countryStr != null) {
        countryStr = countryStr.toUpperCase();
        country = countryToEUDLCountry.get(countryStr);
      }

      if (country == null) country = EUDLCountry.EUDL_COUNTRY_AUTO;

      EUDLRecognizerSettings sett = new EUDLRecognizerSettings(country);
      sett.setShowFullDocument(getBoolean(eudlOpts, "showFullDocument"));
      settings.add(sett);
    }

    if (opts.hasKey(TYPE_MRTD)) {
      ReadableMap mrtdOpts = opts.getMap(TYPE_MRTD);
      MRTDRecognizerSettings sett = new MRTDRecognizerSettings();
      sett.setShowFullDocument(getBoolean(mrtdOpts, "showFullDocument"));
      settings.add(sett);
    }

    return settings.toArray(new RecognizerSettings[settings.size()]);
  }

  private WritableMap serializeImage(Bitmap bitmap) {
    WritableMap image = Arguments.createMap();
    int quality = 100;
    if (opts.hasKey("quality")) {
      quality = (int) (opts.getDouble("quality") * 100);
    }

    ByteArrayOutputStream bytes = getBytes(bitmap, quality);
    String imagePath = getString(opts, "imagePath");
    if (imagePath != null) {
      try {
        OutputStream outputStream = new FileOutputStream(imagePath);
        bytes.writeTo(outputStream);
      } catch (IOException i) {
        image.putString("error", i.getLocalizedMessage());
      }
    }

    if (getBoolean(opts, "base64")) {
      String base64 = toBase64(bytes);
      image.putString("base64", base64);
      // original width/height, ignoring compression
      image.putInt("width", bitmap.getWidth());
      image.putInt("height", bitmap.getHeight());
    }

    return image;
  }

  private static ByteArrayOutputStream getBytes (final Bitmap bitmap, final int quality) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
    return byteArrayOutputStream;
  }

  private static String toBase64(final ByteArrayOutputStream byteArrayOutputStream) {
    byte[] byteArray = byteArrayOutputStream.toByteArray();
    return JPEG_DATA_URI_PREFIX + Base64.encodeToString(byteArray, Base64.NO_WRAP);
  }
}
