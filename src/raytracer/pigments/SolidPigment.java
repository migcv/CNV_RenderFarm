package raytracer.pigments;

import java.awt.Color;

import raytracer.Point;

public class SolidPigment implements Pigment {
	public Color color;

	public SolidPigment(Color color) {
		this.color = color;
	}

	public Color getColor(Point p) {
		return color;
	}

	public String toString() {
		return "solid";
	}
}
