package common;

public class Statistic {
	public int index_io;
	public int leaf_io;
	public int foundedTrajectoryNum;
	public double rteePruneRate;
	public long rtreeQueryTime;
	public long trajectoryReadTime;
	public double pruneRateOverWknn;
	
	public Statistic() {
		index_io = 0;
		leaf_io = 0;
	}
}
