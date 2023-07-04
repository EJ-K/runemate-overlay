package com.runemate.api.overlay;

import com.runemate.api.overlay.render.*;
import java.util.*;
import java.util.concurrent.*;

public interface OverlayRenderSource {

    Collection<RenderTarget> renderables();

    default Collection<Callable<String>> texts() {
        return List.of();
    }

}
