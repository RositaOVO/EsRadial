package org.esradial.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialCanvasResolutionTest {
    @Test void canvasCoversActualPixelsAtDifferentGuiScales() {
        assertEquals(1,RadialCanvasResolution.scale(200,1,1280,1280));
        assertEquals(2,RadialCanvasResolution.scale(200,1,1280,640));
        assertEquals(3,RadialCanvasResolution.scale(200,1.125,1120,560));
        assertEquals(5,RadialCanvasResolution.scale(200,1,2560,512));
    }
    @Test void veryLargeWheelsHaveABoundedTextureAndInvalidSizesRemainSafe() {
        assertEquals(2,RadialCanvasResolution.scale(1000,1,2560,512));
        assertEquals(1,RadialCanvasResolution.scale(200,Double.NaN,2560,512));
        assertEquals(1,RadialCanvasResolution.scale(200,1,0,0));
    }
}
