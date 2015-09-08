package kmst;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;

import common.Data;
import common.Statistic;
import common.Trajectory;
import common.Util;

/**
 * 
 * @author uqhwan15
 * @since 2012/02/07
 */
public class KMST {
	RTreeDB rtreeDB;
	Util.DATATYPE type;
	
	public KMST(Util.DATATYPE type) {
		rtreeDB = new RTreeDB(type);
		this.type = type;
	}
	
	public int [] kmst(RTreeDB rtreeDB, Trajectory Q, float epsilon, int K, Statistic statistic) {
		int [] ArrayT = new int[K];
		float [] ArrayW = new float[K];
		for (int i=0; i<K; i++)
			ArrayW[i] = Float.MAX_VALUE;
		
		int TOTAL;
		if (type == Util.DATATYPE.BEIJING) {
			TOTAL = Util.bjNum;
		} else if (type == Util.DATATYPE.BUS) {
			TOTAL = Util.busNum;
		} else {
			TOTAL = Util.truckNum;
		}
		
		HashSet<Integer> listL = generateCandidates(rtreeDB, Q, epsilon, statistic);
		statistic.foundedTrajectoryNum = listL.size();
		statistic.rteePruneRate = (TOTAL-listL.size())/(double)TOTAL;
		long time = 0;
		for (Integer tid: listL) {
			long start = System.currentTimeMillis();
			Trajectory trajectory = Util.getTrajectory(tid, type);
			long end = System.currentTimeMillis();
			time += end - start;
			
			float distance = Util.distance(trajectory, Q, epsilon);
			int i;
			if (distance < ArrayW[K-1]) {
				for (i=0; i<K; i++) {
					if (distance < ArrayW[i]) {
						for (int j=K-1; j>i; j--) {
							ArrayW[j] = ArrayW[j-1];
							ArrayT[j] = ArrayT[j-1];
						}
						break;
					}
				}
				ArrayW[i] = distance;
				ArrayT[i] = tid;
			}
		}
		statistic.trajectoryReadTime = time;
		
//		for (int i=0; i<K; i++)
//			System.out.println(ArrayT[i] + ":\t" + ArrayW[i]);
		
		return ArrayT;
	}
	
	private HashSet<Integer> generateCandidates(RTreeDB rtreeDB, Trajectory Q, float epsilon, Statistic statistic) {
		HashSet<Integer> listL = new HashSet<Integer>();
		ArrayList<Data> listP = new ArrayList<Data>();
		
		long time = 0;
		for (Data data : Q.getSamplePoints()) {
			long start = System.currentTimeMillis();
			listP = rtreeDB.rangeQuery(data.data[0], data.data[2], epsilon, statistic, type);
			long end = System.currentTimeMillis();
			time += end - start;
			
			for (Data sp: listP) {
				int tid = sp.trajectoryID;
				if (!listL.contains(tid))
					listL.add(tid);
			}
		}
		statistic.rtreeQueryTime = time;
		
		return listL;
	}
	
	public static void main(String[] args) {
//		KMST kmst = new KMST(Util.DATATYPE.TRUCK);
		Statistic statistic = new Statistic();
//		kmst.kmst(kmst.rtreeDB, Util.getTrajectory(37, Util.DATATYPE.TRUCK), Util.truckEPSILON, 20, statistic);
		
//		System.out.println("Truck");
//		new KMST(Util.DATATYPE.TRUCK).doStatistic();
//		System.out.println("Bus");
//		new KMST(Util.DATATYPE.BUS).doStatistic();
		
		KMST bjKmst = new KMST(Util.DATATYPE.BEIJING);
		long start = System.currentTimeMillis();
		bjKmst.kmst(bjKmst.rtreeDB, Util.getTrajectory(7777, Util.DATATYPE.BEIJING), Util.bjEPSILON, 20, statistic);
		System.out.println(System.currentTimeMillis() - start);
	}
	
	public void doStatistic() {
		int testNum = 1;
		int K = 20;
		
		try {
			BufferedWriter writer;
			BufferedReader reader;
			float epsilon;
			if (type == Util.DATATYPE.BEIJING) {
				writer = new BufferedWriter(new FileWriter(new File(Util.bjKMST)));
				reader = new BufferedReader(new FileReader(new File(Util.bjSample)));
				epsilon = Util.bjEPSILON;
			} else if (type == Util.DATATYPE.BUS) {
				writer = new BufferedWriter(new FileWriter(new File(Util.busKMST)));
				reader = new BufferedReader(new FileReader(new File(Util.busSample)));
				epsilon = Util.busEPSILON;
			} else {
				writer = new BufferedWriter(new FileWriter(new File(Util.truckKMST)));
				reader = new BufferedReader(new FileReader(new File(Util.truckSample)));
				epsilon = Util.truckEPSILON;
			}
			
			String line;
			while ((line = reader.readLine()) != null) {
				String[] items = line.split(";");
				int tid = Integer.valueOf(items[0]);
				Trajectory query = Util.getTrajectory(tid, type);
				Statistic statistic = new Statistic();
				
				long start = System.currentTimeMillis();
				for (int i=0; i<testNum; i++) {
					kmst(rtreeDB, query, epsilon, K, statistic);
				}
				long end = System.currentTimeMillis();
				long time = (end - start) / testNum;
				writer.write(tid + ";" + statistic.foundedTrajectoryNum + ";" + statistic.rteePruneRate + ";" 
						+ statistic.rtreeQueryTime + ";" +statistic.rtreeQueryTime/(double)time + ";" 
						+ statistic.trajectoryReadTime + ";" + statistic.trajectoryReadTime/(double)time + ";" 
						+ time + ";" + (statistic.index_io+statistic.leaf_io)/testNum + "\n");
				System.out.println(tid + " processed!");
			}
			
			reader.close();
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
