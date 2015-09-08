package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author uqhwan15
 * @since 2012/02/06
 */
public class Util {
	// Trajectory data directory path
	public static final String bjDirectoryPath = "./DataSet/Beijing/";
	public static final String busDirectoryPath = "./DataSet/Buses/";
	public static final String truckDirectoryPath = "./DataSet/Trucks/";
	
	// Trajectory number in the directory
	public static final int bjNum = 30284;
	public static final int busNum = 145;
	public static final int truckNum = 276;
	
	// Trajectory data file
	public static final String bjDataFile = "./DataSet/Rtree/Data/bjData.txt";
	public static final String busDataFile = "./DataSet/Rtree/Data/busData.txt";
	public static final String truckDataFile = "./DataSet/Rtree/Data/truckData.txt";
	
	// Rtree path
	public static final String bjRtree = "./DataSet/Rtree/bjRtree";
	public static final String busRtree = "./DataSet/Rtree/busRtree";
	public static final String truckRtree = "./DataSet/Rtree/truckRtree";
	
	// Sample trajectory path
	public static final String bjSample = "./DataSet/Samples/beijing.txt";
	public static final String busSample = "./DataSet/Samples/bus.txt";
	public static final String truckSample = "./DataSet/Samples/truck.txt";
	
	// Result path
	public static final String bjKMST = "./DataSet/Results/Beijing/bjKMST.txt";
	public static final String bjKMSTPlus = "./DataSet/Results/Beijing/bjKMSTPlus.txt";
	public static final String bjKMSTStar = "./DataSet/Results/Beijing/bjKMSTStar.txt";
	public static final String bjScan = "./DataSet/Results/Beijing/bjScan.txt";
	
	public static final String busKMST = "./DataSet/Results/Bus/busKMST.txt";
	public static final String busKMSTPlus = "./DataSet/Results/Bus/busKMSTPlus.txt";
	public static final String busKMSTStar = "./DataSet/Results/Bus/busKMSTStar.txt";
	public static final String busScan = "./DataSet/Results/Bus/busScan.txt";
	
	public static final String truckKMST = "./DataSet/Results/Truck/truckKMST.txt";
	public static final String truckKMSTPlus = "./DataSet/Results/Truck/truckKMSTPlus.txt";
	public static final String truckKMSTStar = "./DataSet/Results/Truck/truckKMSTStar.txt";
	public static final String truckScan = "./DataSet/Results/Truck/truckScan.txt";
	
	// Weights path
	public static final String bjWeights = "./DataSet/Weights/bjWeights.txt";
	public static final String busWeights = "./DataSet/Weights/busWeights.txt";
	public static final String truckWeights = "./DataSet/Weights/truckWeights.txt";
	
	// threshold for specific data set
	public static final float bjEPSILON = 50;  
	public static final float busEPSILON = 0.0005f;  
	public static final float truckEPSILON = 0.0005f;  
	
	public static enum DATATYPE {BEIJING, BUS, TRUCK};
	
	// for Data
    public static int DIMENSION = 2;
    public static int BLOCKLENGTH = 512;
    public static int CACHESIZE = 128;
	
	public static final float [][] A = new float[5000][5000]; // store mid-values produced by dynamic programming when computes the HCS value
	
	public static float distance(Trajectory trajectory, Trajectory query, Util.DATATYPE type) {
		float epsilon;
		if (type == Util.DATATYPE.BEIJING) {
			epsilon = Util.bjEPSILON;
		} else if (type == Util.DATATYPE.BUS) {
			epsilon = Util.busEPSILON;
		} else {
			epsilon = Util.truckEPSILON;
		}
		return distance(trajectory, query, epsilon);
	}

	public static float distance(Trajectory trajectory, Trajectory query, float epsilon) {
		float hcs = getHCS(trajectory, query, epsilon);
		
		float weights = 0;
		for (Data point: trajectory.getSamplePoints()) {
			weights += point.weight;
		}
		
		return 1 -  hcs / weights; // compute the normalized distance
	}
	
	private static float getHCS(Trajectory trajectory, Trajectory query, float epsilon) {
		ArrayList<Data> tPs, qPs;
		tPs = trajectory.getSamplePoints();
		qPs = query.getSamplePoints();
		
		
		// quit to match too long sequence ??????
		if ((tPs.size() > 5000) || (qPs.size() > 5000) ) {
			return 0;
		}
		
		for (int i=0; i<= tPs.size(); i++)
			A[i][0] = 0;
		for (int i=0; i<= qPs.size(); i++)
			A[0][i] = 0;
		
		int tlen = tPs.size(), qlen = qPs.size();
		for (int i=1; i<= tPs.size(); i++) {
			for (int j=1; j<= qPs.size(); j++) {
				if (isMatched(tPs.get(tlen-i), qPs.get(qlen-j), epsilon)) {
					A[i][j] = tPs.get(tlen-i).weight + A[i-1][j-1];
				} else {
					A[i][j] = (A[i-1][j] > A[i][j-1]) ? A[i-1][j] : A[i][j-1];
				}
			}
		}
		
		return A[tlen][qlen];
	}
	
	private static boolean isMatched(Data d1, Data d2, float epsilon) {
		if (Math.abs(d1.data[0] - d2.data[0]) <= epsilon && Math.abs(d1.data[2] - d2.data[2]) <= epsilon)
			return true;
		else
			return false;
	}
	
	/**
	 * Read trajectory from the trajectory directory
	 * @param tid
	 * @param type
	 * @return an object of class Trajectory
	 */
	public static Trajectory getTrajectory(int tid, DATATYPE type) {
		Trajectory trajectory = new Trajectory();
		
		String directoryPath;
		if (type == DATATYPE.BEIJING) {
			directoryPath = bjDirectoryPath;
		} else if (type == DATATYPE.BUS) {
			directoryPath = busDirectoryPath;
		} else {
			directoryPath = truckDirectoryPath;
		}
		
		String filePath = directoryPath + tid + ".txt";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
			String line;
			while ((line = reader.readLine()) != null) {
				if (type == DATATYPE.BEIJING) { // handle Beijing data format
					String[] items = line.split(";");
					float lon = Float.valueOf(items[2]);
					float lat = Float.valueOf(items[3]);
					float weight = 1; // weight = Float.valueOf(items[4]);
					
					Data point = new Data();
					point.data = new float[DIMENSION*2];
					point.data[0] = lon; //LX
	    			point.data[1] = lon; //UX
	    			point.data[2] = lat; //LY
	    			point.data[3] = lat; //UY
	    			point.weight = weight;
	    			point.trajectoryID = tid;
					
	    			trajectory.addSamplePoint(point);
				} else { // handle bus or truck data format
					String[] items = line.split(";");
					float lon = Float.valueOf(items[5]);
					float lat = Float.valueOf(items[4]);
					float weight = 1; // weight = Float.valueOf(items[4]);
					
					Data point = new Data();
					point.data = new float[DIMENSION*2];
					point.data[0] = lon; //LX
	    			point.data[1] = lon; //UX
	    			point.data[2] = lat; //LY
	    			point.data[3] = lat; //UY
	    			point.weight = weight;
	    			point.trajectoryID = tid;
					
	    			trajectory.addSamplePoint(point);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return trajectory;
	}
}
