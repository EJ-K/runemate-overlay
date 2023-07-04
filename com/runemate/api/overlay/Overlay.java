package com.runemate.api.overlay;

import static javafx.scene.paint.Color.*;

import com.runemate.api.overlay.render.*;
import com.runemate.game.api.hybrid.entities.*;
import com.runemate.game.api.hybrid.input.*;
import com.runemate.game.api.hybrid.local.Screen;
import com.runemate.game.api.hybrid.local.hud.*;
import com.runemate.game.api.hybrid.local.hud.interfaces.*;
import com.runemate.game.api.hybrid.location.Area;
import com.runemate.game.api.hybrid.location.*;
import com.runemate.game.api.hybrid.location.navigation.Path;
import com.runemate.game.api.hybrid.projection.*;
import com.runemate.game.api.hybrid.region.*;
import com.runemate.game.api.hybrid.util.*;
import com.runemate.game.api.script.framework.*;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.text.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import javafx.animation.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.embed.swing.*;
import javafx.scene.*;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.stage.*;
import javax.annotation.*;

public class Overlay {

    private final AbstractBot bot;

    private OverlayStage impl;

    private final BooleanProperty showing = new SimpleBooleanProperty(false);

    public Overlay(final AbstractBot bot) {
        this.bot = bot;
        showing.addListener((obs, old, show) -> {
            if (show) {
                start();
            } else {
                close();
            }
        });
    }

    private static double[] convert(@Nonnull int[] ints) {
        double[] doubles = new double[ints.length];
        for (int i = 0; i < ints.length; i++) {
            doubles[i] = ints[i];
        }
        return doubles;
    }

    public BooleanProperty showingProperty() {
        return showing;
    }

    public boolean isShowing() {
        return showing.get();
    }

    private void start() {
        Platform.runLater(() -> {
            impl = new OverlayStage();
            impl.start();
        });
    }

    public void close() {
        Platform.runLater(() -> {
            if (impl != null) {
                impl.close();
                impl = null;
            }
        });
    }

    public void show() {
        Platform.runLater(() -> showing.set(true));
    }

    public void destroy() {
        Platform.runLater(() -> showing.set(false));
    }

    private class OverlayStage extends Stage {

        private final DateFormat df = new SimpleDateFormat("HH:mm:ss");
        private final RenderCache shapes = new RenderCache();
        private final DoubleProperty fpsProperty = new SimpleDoubleProperty(0L);
        private final StringProperty timeProperty = new SimpleStringProperty("-");
        private long lastTime = 0L;
        private final IntegerProperty mouseXProperty = new SimpleIntegerProperty(0);
        private final IntegerProperty mouseYProperty = new SimpleIntegerProperty(0);
        private final Map<StringProperty, Callable<String>> textProperties = new HashMap<>();
        private int textFrames = 0;

        public void start() {
            Group group = new Group();
            shapes.addListener((MapChangeListener<Integer, Shape>) change -> {
                if (change.wasRemoved()) {
                    group.getChildren().remove(change.getValueRemoved());
                }
                if (change.wasAdded()) {
                    group.getChildren().add(change.getValueAdded());
                }
            });
            final Label fpsLabel = new Label();
            fpsLabel.setFont(new Font(12));
            fpsLabel.setTextFill(YELLOWGREEN);
            fpsLabel.textProperty().bind(fpsProperty.asString("FPS: %.2f"));
            fpsLabel.setLayoutX(10);
            fpsLabel.setLayoutY(24);
            group.getChildren().add(fpsLabel);
            final Label timeLabel = new Label();
            timeLabel.setFont(new Font(12));
            timeLabel.setTextFill(WHITE);
            timeLabel.textProperty().bind(timeProperty);
            timeLabel.setLayoutX(10);
            timeLabel.setLayoutY(40);
            group.getChildren().add(timeLabel);

            if (bot instanceof OverlayRenderSource) {
                int y = 56;
                for (Callable<String> supplier : ((OverlayRenderSource) bot).texts()) {
                    final Label label = new Label();
                    label.setFont(new Font(12));
                    label.setTextFill(WHITE);
                    textProperties.put(label.textProperty(), supplier);
                    label.setLayoutX(10);
                    label.setLayoutY(y);
                    group.getChildren().add(label);
                    y += 16;
                }
            }

            final Scene scene = new Scene(group, 600, 400, Color.TRANSPARENT);

            try {
                final var image = Resources.getAsBufferedImage(bot, "com/runemate/api/overlay/Mouse.png");
                if (image != null) {
                    final var view = new ImageView(SwingFXUtils.toFXImage(image, null));
                    view.setPreserveRatio(true);
                    view.setFitHeight(20);
                    view.setFitWidth(20);
                    view.xProperty().bind(mouseXProperty);
                    view.yProperty().bind(mouseYProperty);
                    group.getChildren().add(view);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            setScene(scene);
            initStyle(StageStyle.TRANSPARENT);
            setAlwaysOnTop(true);
            show();

            new RenderLoop().start();
        }

        private void fps() {
            long thisTime = System.currentTimeMillis();
            double delta = thisTime - lastTime;
            double fps = 1.0 / delta * 1000;
            lastTime = thisTime;
            fpsProperty.set(fps);
            timeProperty.set(df.format(new Date()));
        }

        private class RenderLoop extends AnimationTimer {
            final Map<String, Object> cache = new HashMap<>();
            java.awt.Shape viewport = null;
            Coordinate regionBase = null;

            @Override
            public void handle(final long now) {
                if (bot.isStopped()) {
                    stop();
                    close();
                    return;
                }
                viewport = null;
                regionBase = null;
                cache.clear();

                try {
                    if (bot instanceof OverlayRenderSource) {
                        final var renderables = ((OverlayRenderSource) bot).renderables();
                        final List<Integer> updatedHashes = new ArrayList<>();
                        for (final var renderable : renderables) {
                            render(renderable, updatedHashes);
                        }
                        shapes.keySet().retainAll(updatedHashes);
                    }

                    final Rectangle bounds = bot.getPlatform().invokeAndWait(Screen::getBounds);
                    if (bounds != null) {
                        if (bounds.width != (int) getWidth()) {
                            setWidth(bounds.width);
                        }
                        if (bounds.height != (int) getHeight()) {
                            setHeight(bounds.height);
                        }
                    }
                    final Point loc = bot.getPlatform().invokeAndWait(Screen::getLocation);
                    if (loc != null) {
                        if (loc.x != (int) getX()) {
                            setX(loc.x);
                        }
                        if (loc.y != (int) getY()) {
                            setY(loc.y);
                        }
                    }
                    final Point mouse = bot.getPlatform().invokeAndWait(Mouse::getPosition);
                    if (mouse != null) {
                        mouseXProperty.set(mouse.x);
                        mouseYProperty.set(mouse.y);
                    }

                    fps();
                    textFrames++;
                    if (textFrames >= 10) {
                        texts();
                        textFrames = 0;
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                    stop();
                    close();
                    return;
                }
            }

            private void texts() {
                textProperties.forEach((property, supplier) -> {
                    try {
                        property.set(supplier.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            private void render(RenderTarget target, List<Integer> updatedHashes) throws Exception {
                final var renderable = target.target();
                if (renderable == null) {
                    return;
                }
                final int hash = renderable.hashCode();
                if (renderable instanceof Model) {
                    final Polygon model = bot.getPlatform().invokeAndWait(() -> ((Model) renderable).projectConvexHull());
                    if (model == null) {
                        return;
                    }
                    updatedHashes.add(hash);
                    shapes.putPolygon(hash, model, target.paint());
                } else if (renderable instanceof Entity) {
                    final Polygon model = bot.getPlatform().invokeAndWait(() -> {
                        final Model m = ((Entity) renderable).getModel();
                        return m == null ? null : m.projectConvexHull();
                    });
                    if (model == null) {
                        return;
                    }
                    updatedHashes.add(hash);
                    shapes.putPolygon(hash, model, target.paint());
                } else if (renderable instanceof InterfaceComponent) {
                    final InterfaceComponent component = (InterfaceComponent) renderable;
                    final Rectangle bounds = bot.getPlatform().invokeAndWait(component::getBounds);
                    if (bounds == null) {
                        return;
                    }
                    updatedHashes.add(hash);
                    shapes.putRectangle(hash, bounds, target.paint());
                } else if (renderable instanceof SpriteItem) {
                    final SpriteItem component = (SpriteItem) renderable;
                    final Rectangle bounds = bot.getPlatform().invokeAndWait(component::getBounds);
                    if (bounds == null) {
                        return;
                    }
                    updatedHashes.add(hash);
                    shapes.putRectangle(hash, bounds, target.paint());
                } else if (renderable instanceof Coordinate) {
                    final Coordinate component = (Coordinate) renderable;
                    final Polygon bounds = projectCoordinate(component);
                    if (bounds == null) {
                        return;
                    }
                    updatedHashes.add(hash);
                    shapes.putPolygon(hash, bounds, target.paint());
                } else if (renderable instanceof Path) {
                    for (var locatable : ((Path) renderable).getVertices()) {
                        final Coordinate component = bot.getPlatform().invokeAndWait(locatable::getPosition);
                        render(new RenderTarget(component, target.paint()), updatedHashes);
                    }
                } else if (renderable instanceof Area) {
                    var area = (Area) renderable;
                    var coords = area.getCoordinates();
                    var shape = new java.awt.geom.Area();
                    for (var c : coords) {
                        var b = projectCoordinate(c);
                        if (b != null) {
                            shape.add(new java.awt.geom.Area(b));
                        }
                    }

                    PathIterator iterator = shape.getPathIterator(null);
                    Polygon polygon = new Polygon();
                    double[] cs = new double[6];
                    while (!iterator.isDone()) {
                        int type = iterator.currentSegment(cs);
                        switch (type) {
                            case PathIterator.SEG_MOVETO:
                            case PathIterator.SEG_LINETO:
                                polygon.addPoint((int) cs[0], (int) cs[1]);
                                break;
                            case PathIterator.SEG_CLOSE:
                                // ignore close segment
                                break;
                            default:
                                throw new AssertionError("Unexpected segment type: " + type);
                        }
                        iterator.next();
                    }

                    var areaHash = area.hashCode();
                    updatedHashes.add(areaHash);
                    shapes.putPolygon(areaHash, polygon, target.paint());
                }
            }

            private Polygon projectCoordinate(Coordinate target) throws Exception {
                return bot.getPlatform().invokeAndWait(() -> {
                    if (viewport == null) {
                        viewport = Projection.getViewport();
                    }
                    if (regionBase == null) {
                        regionBase = Region.getBase();
                    }
                    return target.getBounds(viewport, regionBase, cache);
                });
            }
        }
    }

}
