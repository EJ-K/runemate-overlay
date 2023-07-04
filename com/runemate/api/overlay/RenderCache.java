package com.runemate.api.overlay;

import java.util.*;
import javafx.beans.*;
import javafx.collections.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import org.jetbrains.annotations.*;

public class RenderCache implements ObservableMap<Integer, Shape> {

    private final ObservableMap<Integer, Shape> delegate = FXCollections.observableHashMap();

    public void putRectangle(int hash, java.awt.Rectangle bounds, Paint color) {
        final var rect = (Rectangle) computeIfAbsent(hash, ignored -> new Rectangle());
        rect.setX(bounds.getX());
        rect.setY(bounds.getY());
        rect.setWidth(bounds.getWidth());
        rect.setHeight(bounds.getHeight());
        rect.setStroke(color);
        rect.setFill(Color.TRANSPARENT);
        rect.setStrokeWidth(2.0);
    }

    public void putPolygon(int hash, java.awt.Polygon poly, Paint color) {
        final var polygon = (Polygon) computeIfAbsent(hash, ignored -> new Polygon());
        final var converted = convertPolygon(poly);
        polygon.getPoints().setAll(converted.getPoints());
        polygon.setStroke(color);
        polygon.setFill(Color.TRANSPARENT);
        polygon.setStrokeWidth(2.0);
    }

    private javafx.scene.shape.Polygon convertPolygon(java.awt.Polygon polygon) {
        final double[] points = new double[polygon.npoints * 2];
        for (int i = 0, j = 0; i < polygon.npoints; i++) {
            points[j++] = polygon.xpoints[i];
            points[j++] = polygon.ypoints[i];
        }
        return new javafx.scene.shape.Polygon(points);
    }

    @Override
    public void addListener(final MapChangeListener<? super Integer, ? super Shape> mapChangeListener) {
        delegate.addListener(mapChangeListener);
    }

    @Override
    public void removeListener(final MapChangeListener<? super Integer, ? super Shape> mapChangeListener) {
        delegate.removeListener(mapChangeListener);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Shape get(final Object key) {
        return delegate.get(key);
    }

    @Nullable
    @Override
    public Shape put(final Integer key, final Shape value) {
        return delegate.put(key, value);
    }

    @Override
    public Shape remove(final Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(@NotNull final Map<? extends Integer, ? extends Shape> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @NotNull
    @Override
    public Set<Integer> keySet() {
        return delegate.keySet();
    }

    @NotNull
    @Override
    public Collection<Shape> values() {
        return delegate.values();
    }

    @NotNull
    @Override
    public Set<Entry<Integer, Shape>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public void addListener(final InvalidationListener invalidationListener) {
        delegate.addListener(invalidationListener);
    }

    @Override
    public void removeListener(final InvalidationListener invalidationListener) {
        delegate.removeListener(invalidationListener);
    }
}
