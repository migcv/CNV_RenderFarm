package raytracer.pigments;

import java.awt.Color;

import raytracer.Point;


public interface Pigment {
	public Color getColor(Point p);
}
