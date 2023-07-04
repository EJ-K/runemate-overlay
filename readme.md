A basic overlay for RuneMate bot development.

## Usage

1. Create the overlay in `onStart` (but only in developer mode, overlays are not allowed on the bot store):

```java
        if (Environment.isDevMode()) {
            Platform.runLater(() -> {
                try {
                    var overlay = new Overlay(this);
                    overlay.show();
                } catch (Exception e) {
                    log.warn("Error adding one of the additional panels", e);
                }
            });
        }
```

2. Implement `OverlayRenderSource` in your main class:

```java
import com.runemate.api.overlay.*;
import com.runemate.api.overlay.render.*;
import java.util.*;

public class MyClass implements OverlayRenderSource {

    @Override
    public Collection<RenderTarget> renderables() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Callable<String>> texts() {
        return Collections.emptyList();
    }
}
```

Example implementation:

```java
    @Override
    public Collection<RenderTarget> renderables() {
        final var res = new ArrayList<RenderTarget>();
        if (tracker.nextPosition() != null) {
            res.add(new RenderTarget(new Coordinate(1, 2, 3), Color.PURPLE));
        }
        return res;
    }

    @Override
    public Collection<Callable<String>> texts() {
        return List.of(
            () -> "Tick: " + tracker.tick(),
            () -> "Prayers: " + prayers.getConstantPrayers(),
            () -> "Boss phase: " + tracker.phase(),
            () -> "Boss animation: " + getPlatform().invokeAndWait(() -> tracker.boss().getAnimationId())
        );
    }
```