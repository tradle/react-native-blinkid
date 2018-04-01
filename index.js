import {
  NativeModules,
  Platform
} from 'react-native'

import MRZ from 'mrz'

let { RNBlinkID } = NativeModules
if (Platform.OS === 'ios') {
  RNBlinkID = promisifyAll(RNBlinkID)
}

const { scan, setLicenseKey, notSupportedBecause } = RNBlinkID
const resultProps = ['mrtd', 'usdl', 'eudl']
const validators = {
  mrtd: validateMRTDOptions,
  usdl: validateUSDLOptions,
  eudl: validateEUDLOptions,
  detector: validateDetectorOptions
}

const normalizers = {
  mrtd: normalizeMRTDResult,
  usdl: normalizeUSDLResult,
  eudl: normalizeEUDLResult
}

const SCAN_IN_PROGRESS_ERROR = new Error('RNMBScanInProgress')

module.exports = (function () {
  let licenseKeySet
  let scanInProgress

  if (notSupportedBecause) return { notSupportedBecause }

  return {
    ...RNBlinkID,
    setLicenseKey: key => {
      licenseKeySet = true
      return setLicenseKey(key)
    },
    /**
     * start a scan
     * @param  {Object} opts
     * @param  {Boolean}[base64] - if true, returns base64 image
     * @param  {String} [imagePath] - path to which to save image
     * @param  {String} [licenseKey]
     * @param  {String} [opts.tooltip] - tooltip text for camera view
     * @param  {Object} [opts.mrtd]
     * @param  {Object} [opts.usdl]
     * @param  {Object} [opts.eudl]
     * @param  {Object} [opts.detector]
     * @return {Promise}
     */
    scan: async (opts={}) => {
      if (scanInProgress) {
        throw SCAN_IN_PROGRESS_ERROR
      }

      scanInProgress = true
      try {
        return doScan(opts)
      } finally {
        scanInProgress = false
      }
    }
  }

  async function doScan (opts={}) {
    const { licenseKey } = opts
    if (licenseKey) {
      opts = {
        ...opts,
        licenseKey
      }
    } else if (!licenseKeySet) {
      throw new Error('set or pass in licenseKey first')
    }

    for (let p in opts) {
      let validate = validators[p]
      if (validate) validate(opts[p])
    }

    const result = await scan(opts)
    for (let p in result) {
      let normalize = normalizers[p]
      if (normalize) {
        result[p] = normalize(result[p])
      }
    }

    return result
  }
}())

function validateMRTDOptions (options) {
  // TODO:
}

function validateUSDLOptions (options) {
  // TODO:
}

function validateEUDLOptions (options) {
  // TODO:
}

function validateDetectorOptions (options) {
  // TODO:
}

function normalizeEUDLResult (result) {
  const { personal } = result
  const { birthData } = personal
  if (birthData) {
    const match = birthData.match(/^(\d+\.\d+\.\d+)/)
    if (match) {
      personal.dateOfBirth = match[1]
    }
  }

  normalizeDates(result, parseEUDate)
  return result
}

function normalizeMRTDResult (result) {
  const { personal, document } = result
  const { mrzText } = document
  const mrzParts = mrzText
    .split('\n')
    .map(s => s.trim())
    .filter(s => s)

  const mrz = MRZ.parse(mrzParts)
  const { birthDate, expirationDate } = mrz.fields
  personal.dateOBirthStr = birthDate
  personal.dateOfBirth = dateFromParts(parseYYMMDD(birthDate))
  document.dateOfExpiryStr = expirationDate
  document.dateOfExpiry = dateFromParts(parseYYMMDD(expirationDate))
  return result
}

// courtesy of https://github.com/newtondev/mrz-parser
function parseYYMMDD (str) {
  let d = new Date()
  d.setFullYear(d.getFullYear() + 15)
  let centennial = (""+d.getFullYear()).substring(2, 4)

  let year
  if (str.slice(0, 2) > centennial) {
    year = '19' + str.slice(0, 2)
  } else {
    year = '20' + str.slice(0, 2)
  }

  return {
    year: parseInt(year, 10),
    month: parseInt(str.slice(2, 4), 10),
    day: parseInt(str.slice(4, 6), 10),
    original: str
  }
}

function normalizeUSDLResult (result) {
  normalizeDates(result, parseUSDate)
  return result
}

function normalizeDates (result, normalizer) {
  const { personal, document } = result
  if (typeof personal.dateOfBirth === 'string') {
    personal.dateOfBirth = normalizer(personal.dateOfBirth)
  }

  if (typeof document.dateOfExpiry === 'string') {
    document.dateOfExpiry = normalizer(document.dateOfExpiry)
  }

  if (typeof document.dateOfIssue === 'string') {
    document.dateOfIssue = normalizer(document.dateOfIssue)
  }

  return result
}

function parseEUDate (str) {
  const [day, month, year] = str.split('.')
  return dateFromParts({ day, month, year })
}

function parseUSDate (str) {
  const [month, day, year] = [
    str.slice(0, 2),
    str.slice(2, 4),
    str.slice(4)
  ]

  return dateFromParts({ day, month, year })
}

/**
 * @return {Number} UTC millis
 */
function dateFromParts ({ day, month, year }) {
  year = Number(year)
  month = Number(month) - 1
  day = Number(day)
  return Date.UTC(year, month, day)
}

function promisify (fn) {
  return function (...args) {
    return new Promise((resolve, reject) => {
      args.push((err, res) => {
        if (err) {
          if (typeof err === 'string') {
            err = new Error(err)
          }

          reject(err)
        } else {
          resolve(res)
        }
      })

      fn.apply(this, args)
    })
  }
}

function promisifyAll (obj) {
  const promisified = {}
  for (var p in obj) {
    let val = obj[p]
    if (typeof val === 'function') {
      promisified[p] = promisify(val)
    } else {
      promisified[p] = val
    }
  }

  return promisified
}
