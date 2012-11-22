/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * !! Do not reference this file directly !!
 *
 * It is logically a part of camera_metadata.c.  It is broken out for ease of
 * maintaining the tag info.
 *
 * Array assignments are done using specified-index syntax to keep things in
 * sync with camera_metadata_tags.h
 */

const char *camera_metadata_section_names[ANDROID_SECTION_COUNT] = {
    [ANDROID_REQUEST]        = "android.request",
    [ANDROID_LENS]           = "android.lens",
    [ANDROID_LENS_INFO]      = "android.lens.info",
    [ANDROID_SENSOR]         = "android.sensor",
    [ANDROID_SENSOR_INFO]    = "android.sensor.info",
    [ANDROID_FLASH]          = "android.flash",
    [ANDROID_FLASH_INFO]     = "android.flash.info",
    [ANDROID_HOT_PIXEL]      = "android.hotPixel",
    [ANDROID_HOT_PIXEL_INFO] = "android.hotPixel.info",
    [ANDROID_DEMOSAIC]       = "android.demosaic",
    [ANDROID_DEMOSAIC_INFO]  = "android.demosaic.info",
    [ANDROID_NOISE]          = "android.noiseReduction",
    [ANDROID_NOISE_INFO]     = "android.noiseReduction.info",
    [ANDROID_SHADING]        = "android.shadingCorrection",
    [ANDROID_SHADING_INFO]   = "android.shadingCorrection.info",
    [ANDROID_GEOMETRIC]      = "android.geometricCorrection",
    [ANDROID_GEOMETRIC_INFO] = "android.geometricCorrection.info",
    [ANDROID_COLOR]          = "android.colorCorrection",
    [ANDROID_COLOR_INFO]     = "android.colorCorrection.info",
    [ANDROID_TONEMAP]        = "android.tonemap",
    [ANDROID_TONEMAP_INFO]   = "android.tonemap.info",
    [ANDROID_EDGE]           = "android.edge",
    [ANDROID_EDGE_INFO]      = "android.edge.info",
    [ANDROID_SCALER]         = "android.scaler",
    [ANDROID_SCALER_INFO]    = "android.scaler.info",
    [ANDROID_JPEG]           = "android.jpeg",
    [ANDROID_JPEG_INFO]      = "android.jpeg.info",
    [ANDROID_STATS]          = "android.statistics",
    [ANDROID_STATS_INFO]     = "android.statistics.info",
    [ANDROID_CONTROL]        = "android.control",
    [ANDROID_CONTROL_INFO]   = "android.control.info",
    [ANDROID_QUIRKS_INFO]    = "android.quirks.info"
};

unsigned int camera_metadata_section_bounds[ANDROID_SECTION_COUNT][2] = {
    [ANDROID_REQUEST]        = { ANDROID_REQUEST_START,
                                 ANDROID_REQUEST_END },
    [ANDROID_LENS]           = { ANDROID_LENS_START,
                                 ANDROID_LENS_END },
    [ANDROID_LENS_INFO]      = { ANDROID_LENS_INFO_START,
                                 ANDROID_LENS_INFO_END },
    [ANDROID_SENSOR]         = { ANDROID_SENSOR_START,
                                 ANDROID_SENSOR_END },
    [ANDROID_SENSOR_INFO]    = { ANDROID_SENSOR_INFO_START,
                                 ANDROID_SENSOR_INFO_END },
    [ANDROID_FLASH]          = { ANDROID_FLASH_START,
                                 ANDROID_FLASH_END },
    [ANDROID_FLASH_INFO]     = { ANDROID_FLASH_INFO_START,
                                 ANDROID_FLASH_INFO_END },
    [ANDROID_HOT_PIXEL]      = { ANDROID_HOT_PIXEL_START,
                                 ANDROID_HOT_PIXEL_END },
    [ANDROID_HOT_PIXEL_INFO] = { ANDROID_HOT_PIXEL_INFO_START,
                                 ANDROID_HOT_PIXEL_INFO_END },
    [ANDROID_DEMOSAIC]       = { ANDROID_DEMOSAIC_START,
                                 ANDROID_DEMOSAIC_END },
    [ANDROID_DEMOSAIC_INFO]  = { ANDROID_DEMOSAIC_INFO_START,
                                 ANDROID_DEMOSAIC_INFO_END },
    [ANDROID_NOISE]          = { ANDROID_NOISE_START,
                                 ANDROID_NOISE_END },
    [ANDROID_NOISE_INFO]     = { ANDROID_NOISE_INFO_START,
                                 ANDROID_NOISE_INFO_END },
    [ANDROID_SHADING]        = { ANDROID_SHADING_START,
                                 ANDROID_SHADING_END },
    [ANDROID_SHADING_INFO]   = { ANDROID_SHADING_INFO_START,
                                 ANDROID_SHADING_INFO_END },
    [ANDROID_GEOMETRIC]      = { ANDROID_GEOMETRIC_START,
                                 ANDROID_GEOMETRIC_END },
    [ANDROID_GEOMETRIC_INFO] = { ANDROID_GEOMETRIC_INFO_START,
                                 ANDROID_GEOMETRIC_INFO_END },
    [ANDROID_COLOR]          = { ANDROID_COLOR_START,
                                 ANDROID_COLOR_END },
    [ANDROID_COLOR_INFO]     = { ANDROID_COLOR_INFO_START,
                                 ANDROID_COLOR_INFO_END },
    [ANDROID_TONEMAP]        = { ANDROID_TONEMAP_START,
                                 ANDROID_TONEMAP_END },
    [ANDROID_TONEMAP_INFO]   = { ANDROID_TONEMAP_INFO_START,
                                 ANDROID_TONEMAP_INFO_END },
    [ANDROID_EDGE]           = { ANDROID_EDGE_START,
                                 ANDROID_EDGE_END },
    [ANDROID_EDGE_INFO]      = { ANDROID_EDGE_INFO_START,
                                 ANDROID_EDGE_INFO_END },
    [ANDROID_SCALER]         = { ANDROID_SCALER_START,
                                 ANDROID_SCALER_END },
    [ANDROID_SCALER_INFO]    = { ANDROID_SCALER_INFO_START,
                                 ANDROID_SCALER_INFO_END },
    [ANDROID_JPEG]           = { ANDROID_JPEG_START,
                                 ANDROID_JPEG_END },
    [ANDROID_JPEG_INFO]      = { ANDROID_JPEG_INFO_START,
                                 ANDROID_JPEG_INFO_END },
    [ANDROID_STATS]          = { ANDROID_STATS_START,
                                 ANDROID_STATS_END },
    [ANDROID_STATS_INFO]     = { ANDROID_STATS_INFO_START,
                                 ANDROID_STATS_INFO_END },
    [ANDROID_CONTROL]        = { ANDROID_CONTROL_START,
                                 ANDROID_CONTROL_END },
    [ANDROID_CONTROL_INFO]   = { ANDROID_CONTROL_INFO_START,
                                 ANDROID_CONTROL_INFO_END },
    [ANDROID_QUIRKS_INFO]    = { ANDROID_QUIRKS_INFO_START,
                                 ANDROID_QUIRKS_INFO_END }
};

// Shortcut defines to make succint names for field definitions
#define TIDX(section, tag) \
    [ ANDROID_ ## section ## _ ## tag - ANDROID_ ## section ## _START ]

#define TIIDX(section, tag) \
    [ ANDROID_ ## section ## _ ## tag - ANDROID_ ## section ## _INFO_START ]

tag_info_t android_request[ANDROID_REQUEST_END -
        ANDROID_REQUEST_START] = {
    TIDX(REQUEST, ID)             =
    { "id",            TYPE_INT32 },
    TIDX(REQUEST, TYPE)  =
    { "type",          TYPE_BYTE },
    TIDX(REQUEST, METADATA_MODE)  =
    { "metadataMode",  TYPE_BYTE },
    TIDX(REQUEST, OUTPUT_STREAMS) =
    { "outputStreams", TYPE_BYTE },
    TIDX(REQUEST, INPUT_STREAMS) =
    { "inputStreams", TYPE_BYTE },
    TIDX(REQUEST, FRAME_COUNT)    =
    { "frameCount",    TYPE_INT32 }
};

tag_info_t android_lens[ANDROID_LENS_END -
        ANDROID_LENS_START] = {
    TIDX(LENS, FOCUS_DISTANCE) =
    { "focusDistance",            TYPE_FLOAT },
    TIDX(LENS, APERTURE)       =
    { "aperture",                 TYPE_FLOAT },
    TIDX(LENS, FOCAL_LENGTH)   =
    { "focalLength",              TYPE_FLOAT },
    TIDX(LENS, FILTER_DENSITY) =
    { "filterDensity",            TYPE_FLOAT },
    TIDX(LENS, OPTICAL_STABILIZATION_MODE) =
    { "opticalStabilizationMode", TYPE_BYTE },
    TIDX(LENS, FOCUS_RANGE)    =
    { "focusRange",               TYPE_FLOAT }
};

tag_info_t android_lens_info[ANDROID_LENS_INFO_END -
        ANDROID_LENS_INFO_START] = {
    TIIDX(LENS, MINIMUM_FOCUS_DISTANCE)  =
    { "minimumFocusDistance",               TYPE_FLOAT },
    TIIDX(LENS, HYPERFOCAL_DISTANCE) =
    { "hyperfocalDistance",                 TYPE_FLOAT },
    TIIDX(LENS, AVAILABLE_FOCAL_LENGTHS) =
    { "availableFocalLengths",              TYPE_FLOAT },
    TIIDX(LENS, AVAILABLE_APERTURES) =
    { "availableApertures",                 TYPE_FLOAT },
    TIIDX(LENS, AVAILABLE_FILTER_DENSITY) =
    { "availableFilterDensities",           TYPE_FLOAT },
    TIIDX(LENS, AVAILABLE_OPTICAL_STABILIZATION) =
    { "availableOpticalStabilizationModes", TYPE_BYTE },
    TIIDX(LENS, SHADING_MAP_SIZE) =
    { "shadingMapSize",                     TYPE_INT32 },
    TIIDX(LENS, SHADING_MAP) =
    { "shadingMap",                         TYPE_FLOAT },
    TIIDX(LENS, GEOMETRIC_CORRECTION_MAP_SIZE) =
    { "geometricCorrectionMapSize",         TYPE_INT32 },
    TIIDX(LENS, GEOMETRIC_CORRECTION_MAP) =
    { "geometricCorrectionMap",             TYPE_FLOAT },
    TIIDX(LENS, FACING) =
    { "facing",                             TYPE_BYTE },
    TIIDX(LENS, POSITION) =
    { "position",                           TYPE_FLOAT }
};

tag_info_t android_sensor[ANDROID_SENSOR_END -
        ANDROID_SENSOR_START] = {
    TIDX(SENSOR, EXPOSURE_TIME) =
    { "exposureTime",  TYPE_INT64 },
    TIDX(SENSOR, FRAME_DURATION) =
    { "frameDuration", TYPE_INT64 },
    TIDX(SENSOR, SENSITIVITY) =
    { "sensitivity",   TYPE_INT32 },
    TIDX(SENSOR, TIMESTAMP) =
    { "timestamp",     TYPE_INT64 }
};

tag_info_t android_sensor_info[ANDROID_SENSOR_INFO_END -
        ANDROID_SENSOR_INFO_START] = {
    TIIDX(SENSOR, EXPOSURE_TIME_RANGE) =
    { "exposureTimeRange",      TYPE_INT64 },
    TIIDX(SENSOR, MAX_FRAME_DURATION) =
    { "maxFrameDuration",       TYPE_INT64 },
    TIIDX(SENSOR, AVAILABLE_SENSITIVITIES) =
    { "availableSensitivities", TYPE_INT32 },
    TIIDX(SENSOR, COLOR_FILTER_ARRANGEMENT) =
    { "colorFilterArrangement", TYPE_BYTE },
    TIIDX(SENSOR, PHYSICAL_SIZE) =
    { "physicalSize",           TYPE_FLOAT },
    TIIDX(SENSOR, PIXEL_ARRAY_SIZE) =
    { "pixelArraySize",         TYPE_INT32 },
    TIIDX(SENSOR, ACTIVE_ARRAY_SIZE) =
    { "activeArraySize",        TYPE_INT32 },
    TIIDX(SENSOR, WHITE_LEVEL) =
    { "whiteLevel",             TYPE_INT32 },
    TIIDX(SENSOR, BLACK_LEVEL_PATTERN) =
    { "blackLevelPattern",      TYPE_INT32 },
    TIIDX(SENSOR, COLOR_TRANSFORM_1) =
    { "colorTransform1",        TYPE_RATIONAL },
    TIIDX(SENSOR, COLOR_TRANSFORM_2) =
    { "colorTransform2",        TYPE_RATIONAL },
    TIIDX(SENSOR, REFERENCE_ILLUMINANT_1) =
    { "referenceIlluminant1",   TYPE_BYTE },
    TIIDX(SENSOR, REFERENCE_ILLUMINANT_2) =
    { "referenceIlluminant2",   TYPE_BYTE },
    TIIDX(SENSOR, FORWARD_MATRIX_1) =
    { "forwardMatrix1",         TYPE_RATIONAL },
    TIIDX(SENSOR, FORWARD_MATRIX_2) =
    { "forwardMatrix2",         TYPE_RATIONAL },
    TIIDX(SENSOR, CALIBRATION_TRANSFORM_1) =
    { "calibrationTransform1",  TYPE_RATIONAL },
    TIIDX(SENSOR, CALIBRATION_TRANSFORM_2) =
    { "calibrationTransform2",  TYPE_RATIONAL },
    TIIDX(SENSOR, BASE_GAIN_FACTOR) =
    { "baseGainFactor",         TYPE_RATIONAL },
    TIIDX(SENSOR, MAX_ANALOG_SENSITIVITY) =
    { "maxAnalogSensitivity",   TYPE_INT32 },
    TIIDX(SENSOR, NOISE_MODEL_COEFFICIENTS) =
    { "noiseModelCoefficients", TYPE_FLOAT },
    TIIDX(SENSOR, ORIENTATION) =
    { "orientation",            TYPE_INT32 }
};

tag_info_t android_flash[ANDROID_FLASH_END -
        ANDROID_FLASH_START] = {
    TIDX(FLASH, MODE) =
    { "mode",          TYPE_BYTE },
    TIDX(FLASH, FIRING_POWER) =
    { "firingPower",   TYPE_BYTE },
    TIDX(FLASH, FIRING_TIME) =
    { "firingTime",    TYPE_INT64 }
};

tag_info_t android_flash_info[ANDROID_FLASH_INFO_END -
        ANDROID_FLASH_INFO_START] = {
    TIIDX(FLASH, AVAILABLE) =
    { "available",      TYPE_BYTE },
    TIIDX(FLASH, CHARGE_DURATION) =
    { "chargeDuration", TYPE_INT64 },
};

tag_info_t android_hot_pixel[ANDROID_HOT_PIXEL_END -
        ANDROID_HOT_PIXEL_START] = {
    TIDX(HOT_PIXEL, MODE) =
    { "mode", TYPE_BYTE }
};

tag_info_t android_hot_pixel_info[ANDROID_HOT_PIXEL_INFO_END -
        ANDROID_HOT_PIXEL_INFO_START];

tag_info_t android_demosaic[ANDROID_DEMOSAIC_END -
        ANDROID_DEMOSAIC_START] = {
    TIDX(DEMOSAIC, MODE) =
    { "mode", TYPE_BYTE }
};

tag_info_t android_demosaic_info[ANDROID_DEMOSAIC_INFO_END -
        ANDROID_DEMOSAIC_INFO_START];

tag_info_t android_noise[ANDROID_NOISE_END -
        ANDROID_NOISE_START] = {
    TIDX(NOISE, MODE) =
    { "mode",     TYPE_BYTE },
    TIDX(NOISE, STRENGTH) =
    { "strength", TYPE_BYTE }
};

tag_info_t android_noise_info[ANDROID_NOISE_INFO_END -
        ANDROID_NOISE_INFO_START];

tag_info_t android_shading[ANDROID_SHADING_END -
        ANDROID_SHADING_START] = {
    TIDX(SHADING, MODE) =
    { "mode", TYPE_BYTE }
};

tag_info_t android_shading_info[ANDROID_SHADING_INFO_END -
        ANDROID_SHADING_INFO_START];

tag_info_t android_geometric[ANDROID_GEOMETRIC_END -
        ANDROID_GEOMETRIC_START] = {
    TIDX(GEOMETRIC, MODE) =
    { "mode", TYPE_BYTE }
};

tag_info_t android_geometric_info[ANDROID_GEOMETRIC_INFO_END -
        ANDROID_GEOMETRIC_INFO_START];

tag_info_t android_color[ANDROID_COLOR_END -
        ANDROID_COLOR_START] = {
    TIDX(COLOR, MODE) =
    { "mode",      TYPE_BYTE },
    TIDX(COLOR, TRANSFORM) =
    { "transform", TYPE_FLOAT }
};

tag_info_t android_color_info[ANDROID_COLOR_INFO_END -
        ANDROID_COLOR_INFO_START];

tag_info_t android_tonemap[ANDROID_TONEMAP_END -
        ANDROID_TONEMAP_START] = {
    TIDX(TONEMAP, MODE) =
    { "mode",       TYPE_BYTE },
    TIDX(TONEMAP, CURVE_RED) =
    { "curveRed",   TYPE_FLOAT },
    TIDX(TONEMAP, CURVE_GREEN) =
    { "curveGreen", TYPE_FLOAT },
    TIDX(TONEMAP, CURVE_BLUE) =
    { "curveBlue",  TYPE_FLOAT }
};

tag_info_t android_tonemap_info[ANDROID_TONEMAP_INFO_END -
        ANDROID_TONEMAP_INFO_START] = {
    TIIDX(TONEMAP, MAX_CURVE_POINTS) =
    { "maxCurvePoints", TYPE_INT32 }
};

tag_info_t android_edge[ANDROID_EDGE_END -
        ANDROID_EDGE_START] = {
    TIDX(EDGE, MODE) =
    { "mode",          TYPE_BYTE },
    TIDX(EDGE, STRENGTH) =
    { "strength",      TYPE_BYTE }
};

tag_info_t android_edge_info[ANDROID_EDGE_INFO_END -
        ANDROID_EDGE_INFO_START];

tag_info_t android_scaler[ANDROID_SCALER_END -
        ANDROID_SCALER_START] = {
    TIDX(SCALER, CROP_REGION) =
    { "cropRegion", TYPE_INT32 }
};

tag_info_t android_scaler_info[ANDROID_SCALER_INFO_END -
        ANDROID_SCALER_INFO_START] = {
    TIIDX(SCALER, AVAILABLE_FORMATS) =
    { "availableFormats",          TYPE_INT32 },
    TIIDX(SCALER, AVAILABLE_RAW_SIZES) =
    { "availableRawSizes",         TYPE_INT32 },
    TIIDX(SCALER, AVAILABLE_RAW_MIN_DURATIONS) =
    { "availableRawMinDurations",  TYPE_INT64 },
    TIIDX(SCALER, AVAILABLE_PROCESSED_SIZES) =
    { "availableProcessedSizes",   TYPE_INT32 },
    TIIDX(SCALER, AVAILABLE_PROCESSED_MIN_DURATIONS) =
    { "availableProcessedMinDurations", TYPE_INT64 },
    TIIDX(SCALER, AVAILABLE_JPEG_SIZES) =
    { "availableJpegSizes",        TYPE_INT32 },
    TIIDX(SCALER, AVAILABLE_JPEG_MIN_DURATIONS) =
    { "availableJpegMinDurations", TYPE_INT64 },
    TIIDX(SCALER, AVAILABLE_MAX_ZOOM) =
    { "availableMaxDigitalZoom",   TYPE_INT32 }
};

tag_info_t android_jpeg[ANDROID_JPEG_END -
        ANDROID_JPEG_START] = {
    TIDX(JPEG, QUALITY) =
    { "quality",             TYPE_INT32 },
    TIDX(JPEG, THUMBNAIL_SIZE) =
    { "thumbnailSize",       TYPE_INT32 },
    TIDX(JPEG, THUMBNAIL_QUALITY) =
    { "thumbnailQuality",    TYPE_INT32 },
    TIDX(JPEG, GPS_COORDINATES) =
    { "gpsCoordinates",      TYPE_DOUBLE },
    TIDX(JPEG, GPS_PROCESSING_METHOD) =
    { "gpsProcessingMethod", TYPE_BYTE },
    TIDX(JPEG, GPS_TIMESTAMP) =
    { "gpsTimestamp",        TYPE_INT64 },
    TIDX(JPEG, ORIENTATION) =
    { "orientation",         TYPE_INT32 },
    TIDX(JPEG, SIZE) =
    { "size",                TYPE_INT32 }
};

tag_info_t android_jpeg_info[ANDROID_JPEG_INFO_END -
        ANDROID_JPEG_INFO_START] = {
    TIIDX(JPEG, AVAILABLE_THUMBNAIL_SIZES) =
    { "availableThumbnailSizes", TYPE_INT32 },
    TIIDX(JPEG, MAX_SIZE) =
    { "maxSize", TYPE_INT32 }
};

tag_info_t android_stats[ANDROID_STATS_END -
        ANDROID_STATS_START] = {
    TIDX(STATS, FACE_DETECT_MODE) =
    { "faceDetectMode",   TYPE_BYTE },
    TIDX(STATS, FACE_RECTANGLES) =
    { "faceRectangles",   TYPE_INT32 },
    TIDX(STATS, FACE_SCORES) =
    { "faceScores",       TYPE_BYTE },
    TIDX(STATS, FACE_LANDMARKS) =
    { "faceLandmarks",    TYPE_INT32 },
    TIDX(STATS, FACE_IDS) =
    { "faceIds",          TYPE_INT32 },
    TIDX(STATS, HISTOGRAM_MODE) =
    { "histogramMode",    TYPE_BYTE },
    TIDX(STATS, HISTOGRAM) =
    { "histogram",        TYPE_INT32 },
    TIDX(STATS, SHARPNESS_MAP_MODE) =
    { "sharpnessMapMode", TYPE_BYTE },
    TIDX(STATS, SHARPNESS_MAP) =
    { "sharpnessMap",     TYPE_INT32 }
};

tag_info_t android_stats_info[ANDROID_STATS_INFO_END -
        ANDROID_STATS_INFO_START] = {
    TIIDX(STATS, AVAILABLE_FACE_DETECT_MODES) =
    { "availableFaceDetectModes", TYPE_BYTE },
    TIIDX(STATS, MAX_FACE_COUNT) =
    { "maxFaceCount",             TYPE_INT32 },
    TIIDX(STATS, HISTOGRAM_BUCKET_COUNT) =
    { "histogramBucketCount",     TYPE_INT32 },
    TIIDX(STATS, MAX_HISTOGRAM_COUNT) =
    { "maxHistogramCount",        TYPE_INT32 },
    TIIDX(STATS, SHARPNESS_MAP_SIZE) =
    { "sharpnessMapSize",         TYPE_INT32 },
    TIIDX(STATS, MAX_SHARPNESS_MAP_VALUE) =
    { "maxSharpnessMapValue",     TYPE_INT32 }
};


tag_info_t android_control[ANDROID_CONTROL_END -
        ANDROID_CONTROL_START] = {
    TIDX(CONTROL, CAPTURE_INTENT) =
    { "captureIntent",               TYPE_BYTE },
    TIDX(CONTROL, MODE) =
    { "mode",                        TYPE_BYTE },
    TIDX(CONTROL, EFFECT_MODE) =
    { "effectMode",                  TYPE_BYTE },
    TIDX(CONTROL, SCENE_MODE) =
    { "sceneMode",                   TYPE_BYTE },
    TIDX(CONTROL, VIDEO_STABILIZATION_MODE) =
    { "videoStabilizationMode",      TYPE_BYTE },
    TIDX(CONTROL, AE_MODE) =
    { "aeMode",                      TYPE_BYTE },
    TIDX(CONTROL, AE_LOCK) =
    { "aeLock",                      TYPE_BYTE },
    TIDX(CONTROL, AE_REGIONS) =
    { "aeRegions",                   TYPE_INT32 },
    TIDX(CONTROL, AE_EXP_COMPENSATION) =
    { "aeExposureCompensation",      TYPE_INT32 },
    TIDX(CONTROL, AE_TARGET_FPS_RANGE) =
    { "aeTargetFpsRange",            TYPE_INT32 },
    TIDX(CONTROL, AE_ANTIBANDING_MODE) =
    { "aeAntibandingMode",           TYPE_BYTE },
    TIDX(CONTROL, AE_STATE) =
    { "aeState",                     TYPE_BYTE },
    TIDX(CONTROL, AE_PRECAPTURE_ID) =
    { "aePrecaptureId",              TYPE_INT32},
    TIDX(CONTROL, AWB_MODE) =
    { "awbMode",                     TYPE_BYTE },
    TIDX(CONTROL, AWB_LOCK) =
    { "awbLock",                     TYPE_BYTE },
    TIDX(CONTROL, AWB_REGIONS) =
    { "awbRegions",                  TYPE_INT32 },
    TIDX(CONTROL, AWB_STATE) =
    { "awbState",                    TYPE_BYTE },
    TIDX(CONTROL, AF_MODE) =
    { "afMode",                      TYPE_BYTE },
    TIDX(CONTROL, AF_REGIONS) =
    { "afRegions",                   TYPE_INT32 },
    TIDX(CONTROL, AF_STATE) =
    { "afState",                     TYPE_BYTE },
    TIDX(CONTROL, AF_TRIGGER_ID) =
    { "afTriggerId",                 TYPE_INT32 }
};

tag_info_t android_control_info[ANDROID_CONTROL_INFO_END -
        ANDROID_CONTROL_INFO_START] = {
    TIIDX(CONTROL, AVAILABLE_SCENE_MODES) =
    { "availableSceneModes",         TYPE_BYTE },
    TIIDX(CONTROL, AVAILABLE_EFFECTS) =
    { "availableEffects",            TYPE_BYTE },
    TIIDX(CONTROL, MAX_REGIONS) =
    { "maxRegions",                  TYPE_INT32 },
    TIIDX(CONTROL, AE_AVAILABLE_MODES) =
    { "aeAvailableModes",            TYPE_BYTE },
    TIIDX(CONTROL, AE_EXP_COMPENSATION_STEP) =
    { "aeCompensationStep",          TYPE_RATIONAL },
    TIIDX(CONTROL, AE_EXP_COMPENSATION_RANGE) =
    { "aeCompensationRange",         TYPE_INT32 },
    TIIDX(CONTROL, AE_AVAILABLE_TARGET_FPS_RANGES) =
    { "aeAvailableTargetFpsRanges",  TYPE_INT32 },
    TIIDX(CONTROL, AE_AVAILABLE_ANTIBANDING_MODES) =
    { "aeAvailableAntibandingModes", TYPE_BYTE },
    TIIDX(CONTROL, AWB_AVAILABLE_MODES) =
    { "awbAvailableModes",           TYPE_BYTE },
    TIIDX(CONTROL, AF_AVAILABLE_MODES) =
    { "afAvailableModes",            TYPE_BYTE },
    TIIDX(CONTROL, AVAILABLE_VIDEO_STABILIZATION_MODES) =
    { "availableVideoStabilizationModes", TYPE_BYTE },
    TIIDX(CONTROL, SCENE_MODE_OVERRIDES) =
    { "sceneModeOverrides", TYPE_BYTE }
};

tag_info_t android_quirks_info[ANDROID_QUIRKS_INFO_END -
        ANDROID_QUIRKS_INFO_START] = {
    TIIDX(QUIRKS, TRIGGER_AF_WITH_AUTO) =
    { "triggerAfWithAuto", TYPE_BYTE },
    TIIDX(QUIRKS, USE_ZSL_FORMAT) =
    { "useZslFormat", TYPE_BYTE },
    TIIDX(QUIRKS, METERING_CROP_REGION) =
    { "meteringCropRegion", TYPE_BYTE },
};

#undef TIDX
#undef TIIDX

tag_info_t *tag_info[ANDROID_SECTION_COUNT] = {
    android_request,
    android_lens,
    android_lens_info,
    android_sensor,
    android_sensor_info,
    android_flash,
    android_flash_info,
    android_hot_pixel,
    android_hot_pixel_info,
    android_demosaic,
    android_demosaic_info,
    android_noise,
    android_noise_info,
    android_shading,
    android_shading_info,
    android_geometric,
    android_geometric_info,
    android_color,
    android_color_info,
    android_tonemap,
    android_tonemap_info,
    android_edge,
    android_edge_info,
    android_scaler,
    android_scaler_info,
    android_jpeg,
    android_jpeg_info,
    android_stats,
    android_stats_info,
    android_control,
    android_control_info,
    android_quirks_info
};
