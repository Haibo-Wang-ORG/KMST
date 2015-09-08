package common;

import java.util.ArrayList;

/**
 * 
 * @author uqhwan15
 * @since 2012/02/06
 */
public class Trajectory {
	private ArrayList<Data> points;
	
	public Trajectory() {
		points = new ArrayList<Data>();
	}
	
	public Trajectory(ArrayList<Data> points) {
		this.points = points;
	}
	
	public void addSamplePoint(Data point) {
		this.points.add(point);
	}
	
	public ArrayList<Data> getSamplePoints() {
		return this.points;
	}
}
