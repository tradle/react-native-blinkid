
package io.tradle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.microblink.activity.ScanCard;
import com.microblink.hardware.camera.CameraType;
import com.microblink.image.Image;
import com.microblink.metadata.MetadataSettings;
import com.microblink.recognizers.BaseRecognitionResult;
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
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;

import java.util.ArrayList;

public class RNBlinkIDModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private final String notSupportedBecause;
  private String licenseKey;
  private static final String E_SCAN_FAILED_INVALID = "E_SCAN_FAILED_INVALID";
  private static final String E_SCAN_FAILED_EMPTY = "E_SCAN_FAILED_EMPTY";
  private static final String E_ONE_REQ_AT_A_TIME = "E_ONE_REQ_AT_A_TIME";
  private static final String E_EXPECTED_LICENSE_KEY = "E_EXPECTED_LICENSE_KEY";
  private static final String E_NOT_SUPPORTED = "E_NOT_SUPPORTED";
  private static final String E_DEVELOPER_ERROR = "E_DEVELOPER_ERROR";
  private static final int SCAN_REQUEST_CODE = 5792151;

  // scan session variables
  private ReadableMap opts;
  private Promise scanPromise;
  private Image image;

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      if (requestCode != SCAN_REQUEST_CODE) return;
      if (resultCode != ScanCard.RESULT_OK || data == null) return;

      // perform processing of the data here

      // for example, obtain parcelable recognition result
      Bundle extras = data.getExtras();
      RecognitionResults activityResult = data.getParcelableExtra(ScanCard.EXTRAS_RECOGNITION_RESULTS);
      Parcelable imageResults = data.getParcelableExtra(ScanCard.EXTRAS_IMAGE_METADATA_SETTINGS);

      // get array of recognition results
      BaseRecognitionResult[] resultArray = activityResult.getRecognitionResults();
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

        WritableMap personal = Arguments.createMap();
        WritableMap address = Arguments.createMap();
        WritableMap document = Arguments.createMap();
        WritableMap resultsMap = Arguments.createMap();
        resultsMap.putMap("personal", personal);
        resultsMap.putMap("address", address);
        resultsMap.putMap("document", document);
        if (baseResult instanceof EUDLRecognitionResult) {
          allResults.putMap("eudl", resultsMap);
          EUDLRecognitionResult result = (EUDLRecognitionResult) baseResult;
          personal.putString("firstName", result.getFirstName());
          personal.putString("lastName", result.getLastName());
          personal.putString("placeOfBirth", result.getPlaceOfBirth());
          personal.putDouble("dateOfBirth", result.getDateOfBirth().getTime());
          document.putString("documentNumber", result.getDriverNumber());
          document.putDouble("dateOfIssue", result.getDocumentIssueDate().getTime());
          document.putDouble("dateOfExpiry", result.getDocumentExpiryDate().getTime());
          document.putString("issuer", result.getDocumentIssuingAuthority());
          document.putString("country", result.getCountry().toString());
          address.putString("full", result.getAddress());
        } else if (baseResult instanceof USDLScanResult) {
          USDLScanResult result = (USDLScanResult) baseResult;
          if (result.isUncertain()) {
            hasInvalid = true;
            continue;
          }

          allResults.putMap("usdl", resultsMap);
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
          allResults.putMap("mrtd", resultsMap);
          MRTDRecognitionResult result = (MRTDRecognitionResult) baseResult;
          if (result.isMRZParsed()) {
            personal.putString("lastName", result.getPrimaryId());
            personal.putString("firstName", result.getSecondaryId());
            personal.putString("nationality", result.getNationality());
            personal.putDouble("dateOfBirth", result.getDateOfBirth().getTime());
            personal.putString("sex", result.getSex());

            document.putString("issuer", result.getIssuer());
            document.putString("documentNumber", result.getDocumentNumber());
            document.putString("documentCode", result.getDocumentCode());
            document.putDouble("dateOfExpiry", result.getDateOfExpiry().getTime());
            document.putString("opt1", result.getOpt1());
            document.putString("opt2", result.getOpt2());
            document.putString("mrzText", result.getMRZText());
          } else {
            // attempt to parse OCR result by yourself
            // or ask user to try again
            resultsMap.putString("ocr", result.getOcrResult().toString());
          }
        }
      }

      if (yay) {
        scanPromise.resolve(allResults);
      } else if (hasInvalid) {
        // not all relevant data was scanned, ask user
        // to try again
        scanPromise.reject(E_SCAN_FAILED_INVALID, "Scan failed to extract valid data");
      } else if (hasEmpty) {
        // not all relevant data was scanned, ask user
        // to try again
        scanPromise.reject(E_SCAN_FAILED_EMPTY, "Scan failed");
      } else {
        scanPromise.reject(E_DEVELOPER_ERROR, "This should not happen, please report to react-native-blinkid developers");
      }

      resetForNextScan();
    }
  };

//  private MetadataListener imageExtractor = new MetadataListener() {
//
//    /**
//     * Called when metadata is available.
//     */
//    @Override
//    public void onMetadataAvailable(Metadata metadata) {
//      if (metadata instanceof ImageMetadata) {
//        ImageMetadata imageMetadata = (ImageMetadata) metadata;
//        image = (imageMetadata).getImage().clone();
//      }
//    }
//  };
//
//  private boolean saveImageToFile (Image image) {
//    Bitmap b = image.convertToBitmap();
//    FileOutputStream fos = null;
//    try {
//      fos = new FileOutputStream(filename);
//      boolean success = b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//      if (success) return true;
//
//      Log.e(this, "Failed to compress bitmap!");
//      if (fos != null) {
//        try {
//          fos.close();
//        } catch (IOException ignored) {
//        } finally {
//          fos = null;
//        }
//
//        new File(filename).delete();
//      }
//    } catch (FileNotFoundException e) {
//      Log.e(this, e, "Failed to save image");
//      return false;
//    } finally {
//      if (fos != null) {
//        try {
//          fos.close();
//        } catch (IOException ignored) {
//        }
//      }
//    }
//  }

  public RNBlinkIDModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(reactContext);
    if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
      this.notSupportedBecause = supportStatus.name();
    } else {
      this.notSupportedBecause = null;
    }
  }

  @Override
  public String getName() {
    return "RNBlinkID";
  }

  @ReactMethod
  public void setKey(String licenseKey, final Promise promise) {
    this.licenseKey = licenseKey;
    promise.resolve(null);
  }

  @ReactMethod
  public void scan(ReadableMap opts, final Promise promise) {
    if (notSupportedBecause != null) {
      promise.reject(E_NOT_SUPPORTED, this.notSupportedBecause);
      return;
    }

    if (scanPromise != null) {
      promise.reject(E_ONE_REQ_AT_A_TIME, "Already running a scan");
      return;
    }

    String licenseKey = getString(opts, "licenseKey");
    if (licenseKey == null) {
      licenseKey = this.licenseKey;
    }

    if (licenseKey == null) {
      promise.reject(E_EXPECTED_LICENSE_KEY, "License key was not provided");
    }

    this.scanPromise = promise;
    this.opts = opts;

    Activity currentActivity = getCurrentActivity();
    Intent intent = new Intent(currentActivity, ScanCard.class);
    intent.putExtra(ScanCard.EXTRAS_LICENSE_KEY, licenseKey);
    intent.putExtra(ScanCard.EXTRAS_CAMERA_TYPE, (Parcelable)CameraType.CAMERA_BACKFACE);

    RecognitionSettings settings = new RecognitionSettings();
    settings.setNumMsBeforeTimeout(10000);
    settings.setRecognizerSettingsArray(getRecognitionSettings(opts));
    intent.putExtra(ScanCard.EXTRAS_RECOGNITION_SETTINGS, settings);

    MetadataSettings.ImageMetadataSettings imageMetadataSettings = getImageMetadataSettings(opts);
    intent.putExtra(ScanCard.EXTRAS_IMAGE_METADATA_SETTINGS, imageMetadataSettings);

    // Starting Activity
    currentActivity.startActivityForResult(intent, SCAN_REQUEST_CODE);
  }

  public void resetForNextScan() {
    scanPromise = null;
    opts = null;
  }

  private String getString(ReadableMap map, String key) {
    return map.hasKey(key) ? map.getString(key) : null;
  }

  private boolean getBoolean(ReadableMap map, String key) {
    return map.hasKey(key) ? map.getBoolean(key) : false;
  }

  private MetadataSettings.ImageMetadataSettings getImageMetadataSettings(ReadableMap opts) {
//    MetadataSettings settings = new MetadataSettings();
    MetadataSettings.ImageMetadataSettings ims = new MetadataSettings.ImageMetadataSettings();

    String imagePath = getString(opts, "imagePath");
    boolean outputBase64 = getBoolean(opts, "base64");
    boolean needImage = imagePath != null || outputBase64;
    if (needImage) {
      // enable returning of dewarped images, if they are available
      ims.setDewarpedImageEnabled(true);
      // enable returning of image that was used to obtain valid scanning result
//      ims.setSuccessfulScanFrameEnabled(true);
    }

    return ims;
//    settings.setImageMetadataSettings(ims);
//    return settings;
  }

  private RecognizerSettings[] getRecognitionSettings(ReadableMap opts) {
    // TODO: interpret actual settings
    ArrayList<RecognizerSettings> settings = new ArrayList<>();

    if (opts.hasKey("usdl")) {
      USDLRecognizerSettings sett = new USDLRecognizerSettings();
      sett.setUncertainScanning(false);
      // disable scanning of barcodes that do not have quiet zone
      // as defined by the standard
      sett.setNullQuietZoneAllowed(false);
      settings.add(sett);
    }

    if (opts.hasKey("eudl")) {
      ReadableMap eudlOpts = opts.getMap("eudl");
      EUDLRecognizerSettings sett = new EUDLRecognizerSettings(EUDLCountry.EUDL_COUNTRY_AUTO);
      sett.setShowFullDocument(getBoolean(eudlOpts, "showFullDocument"));
      settings.add(sett);
    }

    if (opts.hasKey("mrtd")) {
      ReadableMap mrtdOpts = opts.getMap("mrtd");
      MRTDRecognizerSettings sett = new MRTDRecognizerSettings();
      sett.setShowFullDocument(getBoolean(mrtdOpts, "showFullDocument"));
      settings.add(sett);
    }

    RecognizerSettings[] arr = settings.toArray(new RecognizerSettings[settings.size()]);
//    if (!RecognizerCompatibility.cameraHasAutofocus(CameraType.CAMERA_BACKFACE, reactContext)) {
//      arr = RecognizerSettingsUtils.filterOutRecognizersThatRequireAutofocus(arr);
//    }
//
//    if (!RecognizerCompatibility.cameraHasAutofocus(CameraType.CAMERA_FRONTFACE, reactContext)) {
//      arr = RecognizerSettingsUtils.filterOutRecognizersThatRequireAutofocus(arr);
//    }

    return arr;
  }
}
