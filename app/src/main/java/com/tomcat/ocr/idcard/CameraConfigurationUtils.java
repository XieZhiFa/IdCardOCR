package com.tomcat.ocr.idcard;

/**
 * Created by Android on 2019/1/3.
 */

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public final class CameraConfigurationUtils {
    private static final String TAG = "CameraConfiguration";
    private static final Pattern SEMICOLON = Pattern.compile(";");
    private static final int MIN_PREVIEW_PIXELS = 153600;
    private static final float MAX_EXPOSURE_COMPENSATION = 1.5F;
    private static final float MIN_EXPOSURE_COMPENSATION = 0.0F;
    private static final double MAX_ASPECT_DISTORTION = 0.15D;
    private static final int MIN_FPS = 10;
    private static final int MAX_FPS = 20;
    private static final int AREA_PER_1000 = 400;

    private CameraConfigurationUtils() {
    }

    public static void setFocus(Parameters parameters, boolean autoFocus, boolean disableContinuous, boolean safeMode) {
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        String focusMode = null;
        if(autoFocus) {
            if(!safeMode && !disableContinuous) {
                focusMode = findSettableValue("focus mode", supportedFocusModes, new String[]{"continuous-picture", "continuous-video", "auto"});
            } else {
                focusMode = findSettableValue("focus mode", supportedFocusModes, new String[]{"auto"});
            }
        }

        if(!safeMode && focusMode == null) {
            focusMode = findSettableValue("focus mode", supportedFocusModes, new String[]{"macro", "edof"});
        }

        if(focusMode != null) {
            if(focusMode.equals(parameters.getFocusMode())) {
                Log.i("CameraConfiguration", "Focus mode already set to " + focusMode);
            } else {
                parameters.setFocusMode(focusMode);
            }
        }

    }

    public static void setTorch(Parameters parameters, boolean on) {
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        String flashMode;
        if(on) {
            flashMode = findSettableValue("flash mode", supportedFlashModes, new String[]{"torch", "on"});
        } else {
            flashMode = findSettableValue("flash mode", supportedFlashModes, new String[]{"off"});
        }

        if(flashMode != null) {
            if(flashMode.equals(parameters.getFlashMode())) {
                Log.i("CameraConfiguration", "Flash mode already set to " + flashMode);
            } else {
                Log.i("CameraConfiguration", "Setting flash mode to " + flashMode);
                parameters.setFlashMode(flashMode);
            }
        }

    }

    public static void setBestExposure(Parameters parameters, boolean lightOn) {
        int minExposure = parameters.getMinExposureCompensation();
        int maxExposure = parameters.getMaxExposureCompensation();
        float step = parameters.getExposureCompensationStep();
        if((minExposure != 0 || maxExposure != 0) && step > 0.0F) {
            float targetCompensation = lightOn?0.0F:1.5F;
            int compensationSteps = Math.round(targetCompensation / step);
            float actualCompensation = step * (float)compensationSteps;
            compensationSteps = Math.max(Math.min(compensationSteps, maxExposure), minExposure);
            if(parameters.getExposureCompensation() == compensationSteps) {
                Log.i("CameraConfiguration", "Exposure compensation already set to " + compensationSteps + " / " + actualCompensation);
            } else {
                Log.i("CameraConfiguration", "Setting exposure compensation to " + compensationSteps + " / " + actualCompensation);
                parameters.setExposureCompensation(compensationSteps);
            }
        } else {
            Log.i("CameraConfiguration", "Camera does not support exposure compensation");
        }

    }

    public static void setBestPreviewFPS(Parameters parameters) {
        setBestPreviewFPS(parameters, 10, 20);
    }

    public static void setBestPreviewFPS(Parameters parameters, int minFPS, int maxFPS) {
        List<int[]> supportedPreviewFpsRanges = parameters.getSupportedPreviewFpsRange();
        //Log.i("CameraConfiguration", "Supported FPS ranges: " + toString((Collection)supportedPreviewFpsRanges));
        if(supportedPreviewFpsRanges != null && !supportedPreviewFpsRanges.isEmpty()) {
            int[] suitableFPSRange = null;
            Iterator var5 = supportedPreviewFpsRanges.iterator();

            while(var5.hasNext()) {
                int[] fpsRange = (int[])var5.next();
                int thisMin = fpsRange[0];
                int thisMax = fpsRange[1];
                if(thisMin >= minFPS * 1000 && thisMax <= maxFPS * 1000) {
                    suitableFPSRange = fpsRange;
                    break;
                }
            }

            if(suitableFPSRange == null) {
                Log.i("CameraConfiguration", "No suitable FPS range?");
            } else {
                int[] currentFpsRange = new int[2];
                parameters.getPreviewFpsRange(currentFpsRange);
                if(Arrays.equals(currentFpsRange, suitableFPSRange)) {
                    Log.i("CameraConfiguration", "FPS range already set to " + Arrays.toString(suitableFPSRange));
                } else {
                    Log.i("CameraConfiguration", "Setting FPS range to " + Arrays.toString(suitableFPSRange));
                    parameters.setPreviewFpsRange(suitableFPSRange[0], suitableFPSRange[1]);
                }
            }
        }

    }

    public static void setFocusArea(Parameters parameters) {
        if(parameters.getMaxNumFocusAreas() > 0) {
            Log.i("CameraConfiguration", "Old focus areas: " + toString((Iterable)parameters.getFocusAreas()));
            List<Area> middleArea = buildMiddleArea(400);
            Log.i("CameraConfiguration", "Setting focus area to : " + toString((Iterable)middleArea));
            parameters.setFocusAreas(middleArea);
        } else {
            Log.i("CameraConfiguration", "Device does not support focus areas");
        }

    }

    public static void setMetering(Parameters parameters) {
        if(parameters.getMaxNumMeteringAreas() > 0) {
            Log.i("CameraConfiguration", "Old metering areas: " + parameters.getMeteringAreas());
            List<Area> middleArea = buildMiddleArea(400);
            Log.i("CameraConfiguration", "Setting metering area to : " + toString((Iterable)middleArea));
            parameters.setMeteringAreas(middleArea);
        } else {
            Log.i("CameraConfiguration", "Device does not support metering areas");
        }

    }

    private static List<Area> buildMiddleArea(int areaPer1000) {
        return Collections.singletonList(
                new Area(new Rect(-areaPer1000, -areaPer1000, areaPer1000, areaPer1000), 1000));
    }

//    public static void setVideoStabilization(Parameters parameters) {
//        if(parameters.isVideoStabilizationSupported()) {
//            if(parameters.getVideoStabilization()) {
//                Log.i("CameraConfiguration", "Video stabilization already enabled");
//            } else {
//                Log.i("CameraConfiguration", "Enabling video stabilization...");
//                parameters.setVideoStabilization(true);
//            }
//        } else {
//            Log.i("CameraConfiguration", "This device does not support video stabilization");
//        }
//    }

    public static void setBarcodeSceneMode(Parameters parameters) {
        if("barcode".equals(parameters.getSceneMode())) {
            Log.i("CameraConfiguration", "Barcode scene mode already set");
        } else {
            String sceneMode = findSettableValue("scene mode", parameters.getSupportedSceneModes(), new String[]{"barcode"});
            if(sceneMode != null) {
                parameters.setSceneMode(sceneMode);
            }

        }
    }

    public static void setZoom(Parameters parameters, double targetZoomRatio) {
        if(parameters.isZoomSupported()) {
            Integer zoom = indexOfClosestZoom(parameters, targetZoomRatio);
            if(zoom == null) {
                return;
            }

            if(parameters.getZoom() == zoom.intValue()) {
                Log.i("CameraConfiguration", "Zoom is already set to " + zoom);
            } else {
                Log.i("CameraConfiguration", "Setting zoom to " + zoom);
                parameters.setZoom(zoom.intValue());
            }
        } else {
            Log.i("CameraConfiguration", "Zoom is not supported");
        }

    }

    private static Integer indexOfClosestZoom(Parameters parameters, double targetZoomRatio) {
        List<Integer> ratios = parameters.getZoomRatios();
        Log.i("CameraConfiguration", "Zoom ratios: " + ratios);
        int maxZoom = parameters.getMaxZoom();
        if(ratios != null && !ratios.isEmpty() && ratios.size() == maxZoom + 1) {
            double target100 = 100.0D * targetZoomRatio;
            double smallestDiff = 1.0D / 0.0;
            int closestIndex = 0;

            for(int i = 0; i < ratios.size(); ++i) {
                double diff = Math.abs((double)((Integer)ratios.get(i)).intValue() - target100);
                if(diff < smallestDiff) {
                    smallestDiff = diff;
                    closestIndex = i;
                }
            }

            Log.i("CameraConfiguration", "Chose zoom ratio of " + (double)((Integer)ratios.get(closestIndex)).intValue() / 100.0D);
            return Integer.valueOf(closestIndex);
        } else {
            Log.w("CameraConfiguration", "Invalid zoom ratios!");
            return null;
        }
    }

    public static void setInvertColor(Parameters parameters) {
        if("negative".equals(parameters.getColorEffect())) {
            Log.i("CameraConfiguration", "Negative effect already set");
        } else {
            String colorMode = findSettableValue("color effect", parameters.getSupportedColorEffects(), new String[]{"negative"});
            if(colorMode != null) {
                parameters.setColorEffect(colorMode);
            }

        }
    }

    public static Point findBestPreviewSizeValue(Parameters parameters, Point screenResolution) {
        List<Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if(rawSupportedSizes == null) {
            Log.w("CameraConfiguration", "Device returned no supported preview sizes; using default");
            Size defaultSize = parameters.getPreviewSize();
            if(defaultSize == null) {
                throw new IllegalStateException("Parameters contained no preview size!");
            } else {
                return new Point(defaultSize.width, defaultSize.height);
            }
        } else {
            if(Log.isLoggable("CameraConfiguration", 4)) {
                StringBuilder previewSizesString = new StringBuilder();
                Iterator var4 = rawSupportedSizes.iterator();

                while(var4.hasNext()) {
                    Size size = (Size)var4.next();
                    previewSizesString.append(size.width).append('x').append(size.height).append(' ');
                }

                Log.i("CameraConfiguration", "Supported preview sizes: " + previewSizesString);
            }

            double screenAspectRatio = (double)screenResolution.x / (double)screenResolution.y;
            int maxResolution = 0;
            Size maxResPreviewSize = null;
            Iterator var7 = rawSupportedSizes.iterator();

            while(var7.hasNext()) {
                Size size = (Size)var7.next();
                int realWidth = size.width;
                int realHeight = size.height;
                int resolution = realWidth * realHeight;
                if(resolution >= 153600) {
                    boolean isCandidatePortrait = realWidth < realHeight;
                    int maybeFlippedWidth = isCandidatePortrait?realHeight:realWidth;
                    int maybeFlippedHeight = isCandidatePortrait?realWidth:realHeight;
                    double aspectRatio = (double)maybeFlippedWidth / (double)maybeFlippedHeight;
                    double distortion = Math.abs(aspectRatio - screenAspectRatio);
                    if(distortion <= 0.15D) {
                        if(maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                            Point exactPoint = new Point(realWidth, realHeight);
                            Log.i("CameraConfiguration", "Found preview size exactly matching screen size: " + exactPoint);
                            return exactPoint;
                        }

                        if(resolution > maxResolution) {
                            maxResolution = resolution;
                            maxResPreviewSize = size;
                        }
                    }
                }
            }

            if(maxResPreviewSize != null) {
                Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
                Log.i("CameraConfiguration", "Using largest suitable preview size: " + largestSize);
                return largestSize;
            } else {
                Size defaultPreview = parameters.getPreviewSize();
                if(defaultPreview == null) {
                    throw new IllegalStateException("Parameters contained no preview size!");
                } else {
                    Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
                    Log.i("CameraConfiguration", "No suitable preview sizes, using default: " + defaultSize);
                    return defaultSize;
                }
            }
        }
    }

    private static String findSettableValue(String name, Collection<String> supportedValues, String... desiredValues) {
        Log.i("CameraConfiguration", "Requesting " + name + " value from among: " + Arrays.toString(desiredValues));
        Log.i("CameraConfiguration", "Supported " + name + " values: " + supportedValues);
        if(supportedValues != null) {
            String[] var3 = desiredValues;
            int var4 = desiredValues.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String desiredValue = var3[var5];
                if(supportedValues.contains(desiredValue)) {
                    Log.i("CameraConfiguration", "Can set " + name + " to: " + desiredValue);
                    return desiredValue;
                }
            }
        }

        Log.i("CameraConfiguration", "No supported values match");
        return null;
    }

    private static String toString(Collection<int[]> arrays) {
        if(arrays != null && !arrays.isEmpty()) {
            StringBuilder buffer = new StringBuilder();
            buffer.append('[');
            Iterator it = arrays.iterator();

            while(it.hasNext()) {
                buffer.append(Arrays.toString((int[])it.next()));
                if(it.hasNext()) {
                    buffer.append(", ");
                }
            }

            buffer.append(']');
            return buffer.toString();
        } else {
            return "[]";
        }
    }

    private static String toString(Iterable<Area> areas) {
        if(areas == null) {
            return null;
        } else {
            StringBuilder result = new StringBuilder();
            Iterator var2 = areas.iterator();

            while(var2.hasNext()) {
                Area area = (Area)var2.next();
                result.append(area.rect).append(':').append(area.weight).append(' ');
            }

            return result.toString();
        }
    }

    public static String collectStats(Parameters parameters) {
        return collectStats((CharSequence)parameters.flatten());
    }

    public static String collectStats(CharSequence flattenedParams) {
        StringBuilder result = new StringBuilder(1000);
        result.append("BOARD=").append(Build.BOARD).append('\n');
        result.append("BRAND=").append(Build.BRAND).append('\n');
        result.append("CPU_ABI=").append(Build.CPU_ABI).append('\n');
        result.append("DEVICE=").append(Build.DEVICE).append('\n');
        result.append("DISPLAY=").append(Build.DISPLAY).append('\n');
        result.append("FINGERPRINT=").append(Build.FINGERPRINT).append('\n');
        result.append("HOST=").append(Build.HOST).append('\n');
        result.append("ID=").append(Build.ID).append('\n');
        result.append("MANUFACTURER=").append(Build.MANUFACTURER).append('\n');
        result.append("MODEL=").append(Build.MODEL).append('\n');
        result.append("PRODUCT=").append(Build.PRODUCT).append('\n');
        result.append("TAGS=").append(Build.TAGS).append('\n');
        result.append("TIME=").append(Build.TIME).append('\n');
        result.append("TYPE=").append(Build.TYPE).append('\n');
        result.append("USER=").append(Build.USER).append('\n');
        result.append("VERSION.CODENAME=").append(VERSION.CODENAME).append('\n');
        result.append("VERSION.INCREMENTAL=").append(VERSION.INCREMENTAL).append('\n');
        result.append("VERSION.RELEASE=").append(VERSION.RELEASE).append('\n');
        result.append("VERSION.SDK_INT=").append(VERSION.SDK_INT).append('\n');
        if(flattenedParams != null) {
            String[] params = SEMICOLON.split(flattenedParams);
            Arrays.sort(params);
            String[] var3 = params;
            int var4 = params.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String param = var3[var5];
                result.append(param).append('\n');
            }
        }

        return result.toString();
    }
}

