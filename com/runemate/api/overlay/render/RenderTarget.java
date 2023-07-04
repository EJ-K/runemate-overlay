package com.runemate.api.overlay.render;

import com.runemate.game.api.hybrid.entities.*;
import com.runemate.game.api.hybrid.local.hud.interfaces.*;
import com.runemate.game.api.hybrid.location.*;
import com.runemate.game.api.hybrid.location.navigation.*;
import javafx.scene.paint.*;
import lombok.*;
import lombok.experimental.*;

@Getter
@Accessors(fluent = true)
public class RenderTarget {

    private final Object target;
    private final Paint paint;

    public RenderTarget(final Object target, final Paint paint) {
        this.target = target;
        this.paint = paint;
    }

    public RenderTarget(final Object target) {
        this.target = target;
        this.paint = defaultPaint(target);
    }

    private static Paint defaultPaint(Object target) {
        if (target instanceof LocatableEntity) {
            if (target instanceof Npc) {
                return Color.DODGERBLUE;
            }

            if (target instanceof Player) {
                return Color.BLUE;
            }

            if (target instanceof GroundItem) {
                return Color.GREEN;
            }

            return Color.RED;
        }

        if (target instanceof InterfaceComponent) {
            return Color.HOTPINK;
        }

        if (target instanceof SpriteItem) {
            return Color.YELLOWGREEN;
        }

        if (target instanceof Coordinate) {
            return Color.ORANGE;
        }

        if (target instanceof Path) {
            return Color.LIGHTPINK;
        }

        if (target instanceof Area) {
            return Color.FIREBRICK;
        }

        return Color.BLACK;
    }
}
