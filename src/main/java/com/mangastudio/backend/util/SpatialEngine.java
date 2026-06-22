package com.mangastudio.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SpatialEngine {
    private SpatialEngine() {}

    public record NormalizedBox(double x, double y, double width, double height) {}
    public record CanvasProportion(int renderWidth, int renderHeight, double scaleFactor) {}

    public static NormalizedBox normalize(double clientX, double clientY, double boxW, double boxH, double canvasW, double canvasH) {
        if (canvasW <= 0 || canvasH <= 0) return new NormalizedBox(0, 0, 0, 0);
        return new NormalizedBox(
            round4(clientX / canvasW),
            round4(clientY / canvasH),
            round4(boxW / canvasW),
            round4(boxH / canvasH)
        );
    }

    public static CanvasProportion calculateProportions(double originalW, double originalH, int clientContainerW) {
        if (originalH <= 0 || originalW <= 0) return new CanvasProportion(0, 0, 1.0);
        double aspect = originalW / originalH;
        int targetW = (int) Math.min(originalW, clientContainerW);
        int targetH = (int) Math.round(targetW / aspect);
        double scale = round4(targetW / originalW);
        return new CanvasProportion(targetW, targetH, scale);
    }

    private static double round4(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}