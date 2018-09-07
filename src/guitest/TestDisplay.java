package guitest;

import processing.core.PApplet;

public class TestDisplay extends PApplet {
	public void setup() {
		size(400, 400);
		background(200, 200, 200);
	}
	
	public void draw() {
		fill(255, 255, 0);
		ellipse(width/2, height/2, width - 10, height - 10);
		fill(0, 0, 0);
		ellipse(2*width/6, height/3, 50, 75);
		ellipse(4*width/6, height/3, 50, 75);
		noFill();
		arc(width/2, 2*height/3, 2*width/6, height/6, 0, PI);
	}
}
