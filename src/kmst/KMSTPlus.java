package kmst;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import common.Data;
import common.Statistic;
import common.Trajectory;
import common.Util;

/**
 * 
 * @author uqhwan15
 * @since 2012/02/07
 * @revise 2012/03/27
 */
public class KMSTPlus {
	RTreeDB rtreeDB;
	HashMap<Integer, Float> hashW;
	Util.DATATYPE type;
	
	String weightFile;
	
	public KMSTPlus(Util.DATATYPE type) {
		// Load the r-tree
		rtreeDB = new RTreeDB(type);
		this.type = type;
		
		if (type == Util.DATATYPE.BEIJING) {
			weightFile = Util.bjWeights;
		} else if (type == Util.DATATYPE.BUS) {
			weightFile = Util.busWeights;
		} else {
			weightFile = Util.truckWeights;
		}
		
		// Load the global trajectory weight table
		hashW = new HashMap<Integer, Float>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(weightFile)));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] items = line.split(";");
				hashW.put(Integer.valueOf(items[0]), Float.valueOf(items[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int [] kmstPlus(RTreeDB rtreeDB, Trajectory Q, float epsilon, int K, Statistic statistic) {
		int [] ArrayT = new int[K];
		float [] ArrayW = new float[K];
		for (int i=0; i<K; i++)
			ArrayW[i] = Float.MAX_VALUE;
		
		ArrayList<Traj> listL = generateOrderedCandidates(rtreeDB, Q, epsilon, statistic);
//		System.out.println(listL.size() + " trajectories are found!");
		
		
		int num = 0; // for computing the prune rate
		long time = 0;
		for (Traj traj: listL) {
			long start = System.currentTimeMillis();
			Trajectory trajectory = Util.getTrajectory(traj.tid, type);
			long end = System.currentTimeMillis();
			time += end - start;
			
			num++;
			if (ArrayW[K-1] < traj.weight) { // prune the rest candidates
//				System.out.println("Prune rate:\t" + (listL.size()-num) + "/" + listL.size() + " = " + (float)(listL.size()-num)/listL.size());
				statistic.foundedTrajectoryNum = num;
				statistic.rteePruneRate = (10789-statistic.foundedTrajectoryNum)/(double)10789;
				statistic.pruneRateOverWknn = (double)(listL.size()-num)/listL.size();
				break;
			}
			
			float distance = Util.distance(trajectory, Q, type);
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
				ArrayT[i] = traj.tid;
			}
		}
		statistic.trajectoryReadTime = time;
		
//		for (int i=0; i<K; i++)
//			System.out.println(ArrayT[i] + ":\t" + ArrayW[i]);
		
		return ArrayT;
	}
	
	private ArrayList<Traj> generateOrderedCandidates(RTreeDB rtreeDB, Trajectory Q, float epsilon, Statistic statistic) {
		ArrayList<Data> listP = new ArrayList<Data>();
		HashMap<Integer, Float> MapT = new HashMap<Integer, Float>();
		HashMap<Integer, Float> MapP = new HashMap<Integer, Float>();
		ArrayList<Traj> listT = new ArrayList<Traj>();
		
		long time = 0;
		for (Data data : Q.getSamplePoints()) {
			long start = System.currentTimeMillis();
			listP = rtreeDB.rangeQuery(data.data[0], data.data[2], epsilon, statistic, type); 
			long end = System.currentTimeMillis();
			time += end - start;
			MapP.clear();
			
			for (Data sp: listP) {
				int tid = sp.trajectoryID;
				if (!MapP.containsKey(tid)) {
					MapP.put(tid, sp.weight);
				} else if (MapP.get(tid) < sp.weight) {
					MapP.put(tid, sp.weight);
				}
			}
			
			for (Entry<Integer, Float> entry : MapP.entrySet()) {
				if (!MapT.containsKey(entry.getKey())) {
					MapT.put(entry.getKey(), entry.getValue());
				} else {
					float tempWeight = MapT.get(entry.getKey()) + entry.getValue();
					MapT.put(entry.getKey(), tempWeight);
				}
			}
		}
		statistic.rtreeQueryTime = time;
//		System.out.println("Index IO:\t" + statistic.index_io);
//		System.out.println("Leaf IO:\t" + statistic.leaf_io);
//		System.out.println("IO:\t" + (statistic.leaf_io+statistic.index_io));
		
		for (Entry<Integer, Float> entry : MapT.entrySet()) {
			int tid = entry.getKey();
			float d_e = 1 - MapT.get(tid) / hashW.get(tid); // compute the epsilon-buffer distance
			Traj traj = new Traj();
			traj.tid = tid;
			traj.weight = d_e;
			listT.add(traj);
		}
		
		Collections.sort(listT, new TrajComparator());
		
//		for (Traj traj : listT) {
//			System.out.println(traj.tid + ":\t" + traj.weight);
//		}
		
		return listT;
	}
	
	public void doStatistic() {
		int testNum = 1;
		int K = 20;
		
		try {
			BufferedWriter writer;
			BufferedReader reader;
			float epsilon;
			if (type == Util.DATATYPE.BEIJING) {
				writer = new BufferedWriter(new FileWriter(new File(Util.bjKMSTPlus)));
				reader = new BufferedReader(new FileReader(new File(Util.bjSample)));
				epsilon = Util.bjEPSILON;
			} else if (type == Util.DATATYPE.BUS) {
				writer = new BufferedWriter(new FileWriter(new File(Util.busKMSTPlus)));
				reader = new BufferedReader(new FileReader(new File(Util.busSample)));
				epsilon = Util.busEPSILON;
			} else {
				writer = new BufferedWriter(new FileWriter(new File(Util.truckKMSTPlus)));
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
					kmstPlus(rtreeDB, query, epsilon, K, statistic);
				}
				long end = System.currentTimeMillis();
				long time = (end - start) / testNum;
				writer.write(tid + ";" + statistic.foundedTrajectoryNum + ";" + statistic.rteePruneRate + ";" 
						+ statistic.pruneRateOverWknn + ";" + statistic.rtreeQueryTime + ";" +statistic.rtreeQueryTime/(double)time + ";" 
						+ statistic.trajectoryReadTime + ";" + statistic.trajectoryReadTime/(double)time + ";" + time + ";" 
						+ (statistic.index_io+statistic.leaf_io)/testNum + "\n");
				System.out.println(tid + " processed!");
			}
			
			reader.close();
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public void doStatistic1() {
//		int testNum = 1;
//		int K = 20;
//		
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(Util.bjDirectoryPath + "wknnstar.txt")));
//			BufferedReader reader = new BufferedReader(new FileReader(new File(Util.bjDirectoryPath + "sample.txt")));
//			String line;
//			while ((line = reader.readLine()) != null) {
//				String[] items = line.split(";");
//				int tid = Integer.valueOf(items[0]);
//				Trajectory query = Util.getTrajectory(tid, type);
//				Statistic statistic = new Statistic();
//				long start = System.currentTimeMillis();
//				for (int i=0; i<testNum; i++) {
//					kmstPlus(rtreeDB, query, Util.EPSILON, K, statistic);
//				}
//				long end = System.currentTimeMillis();
//				long time = (end - start) / testNum;
//				writer.write(tid + ";" + statistic.foundedTrajectoryNum + ";" + statistic.rteePruneRate + ";" 
//						+ statistic.pruneRateOverWknn + ";" + statistic.rtreeQueryTime + ";" +statistic.rtreeQueryTime/(double)time + ";" 
//						+ statistic.trajectoryReadTime + ";" + statistic.trajectoryReadTime/(double)time + ";" + time + ";" 
//						+ (statistic.index_io+statistic.leaf_io)/testNum + "\n");
//				System.out.println(tid + " processed!");
//			}
//			reader.close();
//			writer.flush();
//			writer.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	
	public static void main(String[] args) {
//		KMSTPlus kmstPlus = new KMSTPlus(Util.DATATYPE.TRUCK);
		Statistic statistic = new Statistic();
//		kmstPlus.kmstPlus(kmstPlus.rtreeDB, Util.getTrajectory(37, Util.DATATYPE.TRUCK), Util.truckEPSILON, 20, statistic);
		
//		new KMSTPlus(Util.DATATYPE.TRUCK).doStatistic();
//		new KMSTPlus(Util.DATATYPE.BUS).doStatistic();
		
		KMSTPlus bjKmstPlus = new KMSTPlus(Util.DATATYPE.BEIJING);
		long start = System.currentTimeMillis();
		bjKmstPlus.kmstPlus(bjKmstPlus.rtreeDB, Util.getTrajectory(7777, Util.DATATYPE.BEIJING), Util.bjEPSILON, 20, statistic);
		System.out.println(System.currentTimeMillis() - start);
	}
	
}
