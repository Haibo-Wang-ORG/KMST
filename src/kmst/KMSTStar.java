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
 * @since 2012/02/08
 */
public class KMSTStar {
	public RTreeDB rtreeDB;
	HashMap<Integer, Float> hashW;
	Util.DATATYPE type;
	
	String weightFile;
	
	public KMSTStar(Util.DATATYPE type) {
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
	
	/**
	 * Return the subscripts of the simplified query trajectory
	 * @param Q query trajectory
	 * @param s start subscript
	 * @param t end subscript
	 * @param delta the threshold
	 * @return the subscripts of the simplified query trajectory
	 */
	private ArrayList<Integer> querySimplification(Trajectory Q, int s, int t, float delta) {
		float dmax = 0;
		int index = 0;
		ArrayList<Integer> listP = new ArrayList<Integer>();

		for (int i=s+1; i<=t-1; i++) {
			float d = getPerpendicularDistance(Q, s, t, i); // the perpendicular distance from Q[i]->(Q[s],Q[t])
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if (dmax > delta) {
			ArrayList<Integer> listP1 = querySimplification(Q, s, index, delta);
			ArrayList<Integer> listP2 = querySimplification(Q, index, t, delta);
			listP1.remove(listP1.size()-1);
			for (Integer i : listP1) {
				listP.add(i);
			}
			for (Integer i : listP2) {
				listP.add(i);
			}
		} else {
			listP.add(s);
			listP.add(t);
		}
		
		return listP;
	}
	
	private float getPerpendicularDistance(Trajectory Q, int s, int t, int index) {
		ArrayList<Data> points = Q.getSamplePoints();
		return getPerpendicularDistance(Q, s, t, points.get(index).data[0], points.get(index).data[2]);		
	}
	
	private float getPerpendicularDistance(Trajectory Q, int s, int t, float lon, float lat) {
		ArrayList<Data> points = Q.getSamplePoints();
		float x0 = points.get(s).data[0];
		float y0 = points.get(s).data[2];
		float x1 = points.get(t).data[0];
		float y1 = points.get(t).data[2];
		float x = lon;
		float y = lat;
		
		// the formula is from http://softsurfer.com/Archive/algorithm_0102/algorithm_0102.htm
		double d = Math.abs((y0-y1)*x+(x1-x0)*y+(x0*y1-x1*y0))/Math.sqrt((x1-x0)*(x1-x0) + (y1-y0)*(y1-y0));
		return (float)d;
	}
	
	private boolean isInEpsilonDeltaBuffer(Trajectory Q, int s, int t, Rect rect, float delta, float epsilon) {
		float threshold = (float)Math.sqrt(2) * epsilon + delta;
		
		if (getPerpendicularDistance(Q, s, t, rect.lon1, rect.lat1) < threshold 
				&& getPerpendicularDistance(Q, s, t, rect.lon2, rect.lat1) < threshold
				&& getPerpendicularDistance(Q, s, t, rect.lon2, rect.lat2) < threshold
				&& getPerpendicularDistance(Q, s, t, rect.lon1, rect.lat2) < threshold) {
			return true;
		} else {
			return false;
		}
	}
	
	private Rect getEpsilonBuffer(Trajectory Q, int i, float epsilon) {
		float loni = Q.getSamplePoints().get(i).data[0], lati = Q.getSamplePoints().get(i).data[2];
		Rect rect = new Rect(loni-epsilon, lati-epsilon, loni+epsilon, lati+epsilon);
		return rect;
	}
	
//	private ArrayList<Rect> generateExpandedBuffers(Trajectory Q, float delta, float epsilon) {
//		ArrayList<Rect> listBB = new ArrayList<Rect>();
//		int m = Q.getSamplePoints().size();
//		ArrayList<Integer> listS = querySimplification(Q, 0, m-1, delta);
//		
//		int index = 1;
//		Rect rectC = new Rect(Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
//		for (int i=0; i<m; i++) {
//			if (i < listS.get(index)) {
//				Rect rect = getEpsilonBuffer(Q, i, epsilon);
//				float lon1 = Math.min(rectC.lon1, rect.lon1);
//				float lat1 = Math.min(rectC.lat1, rect.lat1);
//				float lon2 = Math.max(rectC.lon2, rect.lon2);
//				float lat2 = Math.max(rectC.lat2, rect.lat2);
//				Rect rectT = new Rect(lon1, lat1, lon2, lat2); // expanded buffer
//				if (isInEpsilonDeltaBuffer(Q, listS.get(index-1), listS.get(index), rectT, delta, epsilon)) {
//					rectC = rectT;
//				} else {
//					listBB.add(rectC);
//					rectC = rect;
//				}
//			} else {
//				listBB.add(rectC);
//				index ++;
//				rectC = getEpsilonBuffer(Q, i, epsilon);
//			}
//		}
//		listBB.add(getEpsilonBuffer(Q, m-1, epsilon));
//		
//		return listBB;
//	}
	
	private ArrayList<Rect> generateExpandedBuffersAlter(Trajectory Q, float delta, float epsilon) {
		ArrayList<Rect> listBB = new ArrayList<Rect>();
		int m = Q.getSamplePoints().size();
		ArrayList<Integer> listS = querySimplification(Q, 0, m-1, delta);
		
		int index = 1;
		Rect rectC = new Rect(Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
		int startIndex = 0;
		for (int i=0; i<m; i++) {
			if (i < listS.get(index)) {
				Rect rect = getEpsilonBuffer(Q, i, epsilon);
				float lon1 = Math.min(rectC.lon1, rect.lon1);
				float lat1 = Math.min(rectC.lat1, rect.lat1);
				float lon2 = Math.max(rectC.lon2, rect.lon2);
				float lat2 = Math.max(rectC.lat2, rect.lat2);
				Rect rectT = new Rect(lon1, lat1, lon2, lat2); // expanded buffer
				if (isInEpsilonDeltaBuffer(Q, listS.get(index-1), listS.get(index), rectT, delta, epsilon)) {
					rectC = rectT;
				} else {
					rectC.startIndex = startIndex;
					rectC.endIndex = i-1;
					rectC.num = rectC.endIndex - rectC.startIndex + 1;
					listBB.add(rectC);
					rectC = rect;
					startIndex = i;
				}
			} else {
				rectC.startIndex = startIndex;
				rectC.endIndex = i-1;
				rectC.num = rectC.endIndex - rectC.startIndex + 1;
				listBB.add(rectC);
				index ++;
				rectC = getEpsilonBuffer(Q, i, epsilon);
				startIndex = i;
			}
		}
		Rect rect = getEpsilonBuffer(Q, m-1, epsilon);
		rect.startIndex = m-1;
		rect.endIndex = m-1;
		rect.num = 1;
		listBB.add(rect);
		
		return listBB;
	}
	
//	private ArrayList<Traj> generateExpandedCandidates(RTreeDB rtreeDB, Trajectory Q, float delta, float epsilon) {
//		ArrayList<Traj> listT = new ArrayList<Traj>();
//		HashSet<Data> listP = new HashSet<Data>();
//		HashMap<Integer, Float> MapT = new HashMap<Integer, Float>();
//		ArrayList<Rect> listBB = generateExpandedBuffers(Q, delta, epsilon);
//		
//		Statistic statistic = new Statistic();
//		for (Rect rect : listBB) {
//			ArrayList<Data> listPP = rtreeDB.rangeQuery(rect, statistic);
//			for (Data data : listPP) {
//				if (!listP.contains(data)) {
//					listP.add(data);
//				}
//			}
//		}
//		System.out.println("Index IO:\t" + statistic.index_io);
//		System.out.println("Leaf IO:\t" + statistic.leaf_io);
//		System.out.println("IO:\t" + (statistic.leaf_io+statistic.index_io));
//		
//		for (Data data : listP) {
//			int tid = data.trajectoryID;
//			if (!MapT.containsKey(tid)) {
//				MapT.put(tid, data.weight);
//			} else {
//				MapT.put(tid, MapT.get(tid)+data.weight);
//			}
//		}
//		for (Entry<Integer, Float> entry : MapT.entrySet()) {
//			int tid = entry.getKey();
//			float d_e = 1 - MapT.get(tid) / hashW.get(tid); // compute the epsilon-buffer distance
//			Traj traj = new Traj();
//			traj.tid = tid;
//			traj.weight = d_e;
//			listT.add(traj);
//		}
//		
//		Collections.sort(listT, new TrajComparator());
//		
//		return listT;
//	}
	
	private ArrayList<Traj> generateExpandedCandidatesAlter(RTreeDB rtreeDB, Trajectory Q, float delta, float epsilon, Statistic statistic) {
		ArrayList<Traj> listT = new ArrayList<Traj>();
		HashMap<Integer, Float> MapT = new HashMap<Integer, Float>();
		ArrayList<Rect> listBB = generateExpandedBuffersAlter(Q, delta, epsilon);
		
		
		HashMap<Integer, Float> MapTemp = new HashMap<Integer, Float>();
		long time = 0;
		for (Rect rect : listBB) {
			long start = System.currentTimeMillis();
			ArrayList<Data> listPP = rtreeDB.rangeQuery(rect, statistic, type);
			long end = System.currentTimeMillis();
			time += end - start;
			
			MapTemp.clear();
			for (Data data : listPP) {
				int tid = data.trajectoryID;
				if (!MapTemp.containsKey(tid)) {
					MapTemp.put(tid, data.weight);
				} else if (MapTemp.get(tid) < data.weight) {
					MapTemp.put(tid, data.weight);
				}
			}
			for (Entry<Integer, Float> entry : MapTemp.entrySet()) {
				int tid = entry.getKey();
				if (!MapT.containsKey(tid)) {
					MapT.put(tid, entry.getValue()*rect.num);
				} else {
					MapT.put(tid, MapT.get(tid)+entry.getValue()*rect.num);
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
		
		return listT;
	}
	
//	private ArrayList<Traj> generateExpandedCandidatesAlter2(RTreeDB rtreeDB, Trajectory Q, float delta, float epsilon) {
//		ArrayList<Traj> listT = new ArrayList<Traj>();
//		HashMap<Integer, Float> MapT = new HashMap<Integer, Float>();
//		ArrayList<Rect> listBB = generateExpandedBuffersAlter(Q, delta, epsilon);
//		
//		
//		HashMap<Integer, Float[]> MapTemp = new HashMap<Integer, Float[]>();
//		Statistic statistic = new Statistic();
//		for (Rect rect : listBB) {
//			ArrayList<Data> listPP = rtreeDB.rangeQuery(rect, statistic);
//			MapTemp.clear();
//			for (Data data : listPP) {
//				int tid = data.trajectoryID;
//				if (!MapTemp.containsKey(tid)) {
//					Float[] weights = new Float[rect.num];
//					for (int i=0; i<weights.length; i++) {
//						weights[i] = new Float(0);
//					}
//					weights[0] = data.weight;
//					MapTemp.put(tid, weights);
//				} else {
//					Float[] weights = MapTemp.get(tid);
//					for (int i=0; i<weights.length; i++) {
//						if (weights[i] < data.weight) {
//							for (int j=weights.length-1; j>i; j--) {
//								weights[j] = weights[j-1];
//							}
//							weights[i] = data.weight;
//							break;
//						}
//					}
//					MapTemp.put(tid, weights);
//				}
//			}
//			for (Entry<Integer, Float[]> entry : MapTemp.entrySet()) {
//				int tid = entry.getKey();
//				if (!MapT.containsKey(tid)) {
//					MapT.put(tid, getWeightSum(entry.getValue()));
//				} else {
//					MapT.put(tid, MapT.get(tid)+getWeightSum(entry.getValue()));
//				}
//			}
//		}
//		System.out.println("Index IO:\t" + statistic.index_io);
//		System.out.println("Leaf IO:\t" + statistic.leaf_io);
//		System.out.println("IO:\t" + (statistic.leaf_io+statistic.index_io));
//		for (Entry<Integer, Float> entry : MapT.entrySet()) {
//			int tid = entry.getKey();
//			float d_e = 1 - MapT.get(tid) / hashW.get(tid); // compute the epsilon-buffer distance
//			Traj traj = new Traj();
//			traj.tid = tid;
//			traj.weight = d_e;
//			listT.add(traj);
//		}
//		
//		Collections.sort(listT, new TrajComparator());
//		
//		return listT;
//	}
	
//	private boolean lessDistance(Data data, Data point, float epsilon) {
//		float deltaLon = data.data[0] - point.data[0];
//		float deltaLat = data.data[2] - point.data[2];
//		return (Math.abs(deltaLon) < epsilon) && (Math.abs(deltaLat) < epsilon);
//	}
	
//	private ArrayList<Traj> generateExpandedCandidatesAlter3(RTreeDB rtreeDB, Trajectory Q, float delta, float epsilon) {
//		ArrayList<Traj> listT = new ArrayList<Traj>();
//		HashMap<Integer, Float> MapT = new HashMap<Integer, Float>();
//		ArrayList<Rect> listBB = generateExpandedBuffersAlter(Q, delta, epsilon);
//		HashSet<Integer> listTraj = new HashSet<Integer>();
//		
//		HashMap<Integer, Float> MapTemp = new HashMap<Integer, Float>();
//		Statistic statistic = new Statistic();
//		ArrayList<Data> samplePoints = Q.getSamplePoints();
//		for (Rect rect : listBB) {
//			ArrayList<Data> listPP = rtreeDB.rangeQuery(rect, statistic);
//			for (Data data : listPP) {
//				int tid = data.trajectoryID;
//				if (!listTraj.contains(tid)) listTraj.add(tid);
//			}
//			MapTemp.clear();
//			System.out.println(rect);
//			Integer[] trajs = new Integer[listTraj.size()];
//			listTraj.toArray(trajs);
//			System.out.println(trajs.length*rect.num);
//			for (int i=rect.startIndex; i<=rect.endIndex; i++) {
//				for (Integer tid : trajs) {
//					float weight = Float.MIN_VALUE;
//					for (Data data : listPP) {
//						if (data.trajectoryID == tid) {
//							if ((data.weight > weight) && lessDistance(data, samplePoints.get(i), epsilon)) {
//								weight = data.weight;
//							}
//						}
//					}
//					if (!MapTemp.containsKey(tid)) {
//						MapTemp.put(tid, weight);
//					} else {
//						MapTemp.put(tid, MapTemp.get(tid) + weight);
//					}
//				}
//			}
//			
//			for (Entry<Integer, Float> entry : MapTemp.entrySet()) {
//				int tid = entry.getKey();
//				if (!MapT.containsKey(tid)) {
//					MapT.put(tid, entry.getValue());
//				} else {
//					MapT.put(tid, MapT.get(tid)+entry.getValue());
//				}
//			}
//		}
//		System.out.println("Index IO:\t" + statistic.index_io);
//		System.out.println("Leaf IO:\t" + statistic.leaf_io);
//		System.out.println("IO:\t" + (statistic.leaf_io+statistic.index_io));
//		for (Entry<Integer, Float> entry : MapT.entrySet()) {
//			int tid = entry.getKey();
//			float d_e = 1 - MapT.get(tid) / hashW.get(tid); // compute the epsilon-buffer distance
//			Traj traj = new Traj();
//			traj.tid = tid;
//			traj.weight = d_e;
//			listT.add(traj);
//		}
//		
//		Collections.sort(listT, new TrajComparator());
//		
//		return listT;
//	}
	
//	private Float getWeightSum(Float[] weightes) {
//		float weight = 0;
//		for (int i=0; i<weightes.length; i++) {
//			weight += weightes[i];
//		}
//		return weight;
//	}
	
	public int [] kmstStar(RTreeDB rtreeDB, Trajectory Q, float delta, float epsilon, int K, Statistic statistic) {
		int [] ArrayT = new int[K];
		float [] ArrayW = new float[K];
		for (int i=0; i<K; i++)
			ArrayW[i] = Float.MAX_VALUE;
		
//		ArrayList<Traj> listL = generateExpandedCandidates(rtreeDB, Q, delta, epsilon);
		ArrayList<Traj> listL = generateExpandedCandidatesAlter(rtreeDB, Q, delta, epsilon, statistic);
//		ArrayList<Traj> listL = generateExpandedCandidatesAlter2(rtreeDB, Q, delta, epsilon);
//		ArrayList<Traj> listL = generateExpandedCandidatesAlter3(rtreeDB, Q, delta, epsilon);
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
		
	class Rect {
		float lon1, lat1, lon2, lat2;
		int num;
		int startIndex, endIndex;

		public Rect() {
		}

		public Rect(float lon1, float lat1, float lon2, float lat2) {
			this.lon1 = lon1;
			this.lat1 = lat1;
			this.lon2 = lon2;
			this.lat2 = lat2;
		}
		
		public String toString() {
			return "[" + lon1 + "," + lat1 + "], [" + lon2 + "," + lat2 + "] -> " + endIndex + " - " + startIndex + " = "+ num; 
		}
	}
	
	public void doStatistic() {
		int testNum = 1;
		int K = 20;
		
		try {
			BufferedWriter writer;
			BufferedReader reader;
			float epsilon;
			if (type == Util.DATATYPE.BEIJING) {
				writer = new BufferedWriter(new FileWriter(new File(Util.bjKMSTStar)));
				reader = new BufferedReader(new FileReader(new File(Util.bjSample)));
				epsilon = Util.bjEPSILON;
			} else if (type == Util.DATATYPE.BUS) {
				writer = new BufferedWriter(new FileWriter(new File(Util.busKMSTStar)));
				reader = new BufferedReader(new FileReader(new File(Util.busSample)));
				epsilon = Util.busEPSILON;
			} else {
				writer = new BufferedWriter(new FileWriter(new File(Util.truckKMSTStar)));
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
					kmstStar(rtreeDB, query, epsilon/2, epsilon, K, statistic);
				}
				long end = System.currentTimeMillis();
				long time = (end - start) / testNum;
				writer.write(tid + ";" + statistic.foundedTrajectoryNum + ";" + statistic.rteePruneRate + ";" 
						+ statistic.pruneRateOverWknn + ";" + statistic.rtreeQueryTime + ";" 
						+ statistic.rtreeQueryTime/(double)time + ";" + statistic.trajectoryReadTime + ";" 
						+ statistic.trajectoryReadTime/(double)time + ";" + time + ";" + (statistic.index_io+statistic.leaf_io)/testNum + "\n");
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
//			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(Util.bjDirectoryPath + "wknnplus.txt")));
//			BufferedReader reader = new BufferedReader(new FileReader(new File(Util.bjDirectoryPath + "sample.txt")));
//			String line;
//			while ((line = reader.readLine()) != null) {
//				String[] items = line.split(";");
//				int tid = Integer.valueOf(items[0]);
//				Trajectory query = Util.getTrajectory(tid, Util.bjDirectoryPath);
//				Statistic statistic = new Statistic();
//				long start = System.currentTimeMillis();
//				for (int i=0; i<testNum; i++) {
//					kmstStar(rtreeDB, query, Util.EPSILON/2, Util.EPSILON, K, statistic);
//				}
//				long end = System.currentTimeMillis();
//				long time = (end - start) / testNum;
//				writer.write(tid + ";" + statistic.foundedTrajectoryNum + ";" + statistic.rteePruneRate + ";" 
//						+ statistic.pruneRateOverWknn + ";" + statistic.rtreeQueryTime + ";" 
//						+ statistic.rtreeQueryTime/(double)time + ";" + statistic.trajectoryReadTime + ";" 
//						+ statistic.trajectoryReadTime/(double)time + ";" + time + ";" + (statistic.index_io+statistic.leaf_io)/testNum + "\n");
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
//		KMSTStar kmstStar = new KMSTStar(Util.DATATYPE.TRUCK);
		Statistic statistic = new Statistic();
//		kmstStar.kmstStar(kmstStar.rtreeDB, Util.getTrajectory(37, Util.DATATYPE.TRUCK), Util.truckEPSILON/2, Util.truckEPSILON, 20, statistic);
		
//		new KMSTStar(Util.DATATYPE.TRUCK).doStatistic();
//		new KMSTStar(Util.DATATYPE.BUS).doStatistic();
		
		KMSTStar bjKmstStar = new KMSTStar(Util.DATATYPE.BEIJING);
		long start = System.currentTimeMillis();
		bjKmstStar.kmstStar(bjKmstStar.rtreeDB, Util.getTrajectory(7777, Util.DATATYPE.BEIJING), Util.bjEPSILON/2, Util.bjEPSILON, 20, statistic);
		System.out.println(System.currentTimeMillis() - start);
		
//		// Verify the correctness of the simplification algorithm
//		Trajectory trajectory = new Trajectory();
//		// pair [] X = {(1.6, -4),(.5, -3.5), (-0.3, -2.6), (-1.4, -2), (-0.7, -1.5), (0,0), (1,1), (1.7,1.8), (2.6,3.1), (3.0, 3.4), (4.1,3.8), (5, 3.9), (5,5), (5, 5.4), (5, 5.7), (5.2, 6.2), (4.8, 6.7)};
//		double A[] = {1.6, -4,.5, -3.5, -0.3, -2.6, -1.4, -2, -0.7, -1.5, 0,0, 1,1, 1.7,1.8, 2.6,3.1, 3.0, 3.4, 4.1,3.8, 5, 3.9, 5,5, 5, 5.4, 5, 5.7, 5.2, 6.2, 4.8, 6.7};
//		for (int i=0; i<A.length; i+=2) {
//			Data p = new Data();
//			p.data[0] = (float)A[i];
//			p.data[2] = (float)A[i+1];
//			trajectory.addSamplePoint(p);
//		}
//		System.out.println(wknnPlus.querySimplification(trajectory, 0, A.length/2-1, 0.5f)); // should be [0, 3, 8, 11, 16]
//		
//		// Verify the correctness of the simplification algorithm by using real data
//		Trajectory query = Util.getTrajectory(7896, Util.bjDirectoryPath);
//		System.out.println(query.getSamplePoints().size());
//		System.out.println(wknnPlus.querySimplification(query, 0, query.getSamplePoints().size()-1, Util.EPSILON/2).size());
//		
//		// Verify the correctness of the generateExpandedBuffers
//		Trajectory trajectory2 = new Trajectory();
//		double AA[] = {4.8,6.7, 5.2,6.2, 5,5.7, 5,5.4, 5,5, 5,3.9, 4.1,3.8, 3.0,3.4, 2.6,3.1};
//		for (int i=0; i<AA.length; i+=2) {
//			Data p = new Data();
//			p.data[0] = (float)AA[i];
//			p.data[2] = (float)AA[i+1];
//			trajectory2.addSamplePoint(p);
//		}
////		ArrayList<Rect> rects = wknnPlus.generateExpandedBuffers(trajectory2, .5f, 1);
//		ArrayList<Rect> rects = wknnPlus.generateExpandedBuffersAlter(trajectory2, .5f, 1);
//		for (Rect rect : rects) { 
//			System.out.println(rect);
//			/*
//			 * should be :
//			 * [3.8,4],[6.2,7.7]
//			 * [2,2.4],[6,4.9]
//			 * [1.6,2.1],[3.6,4.1]
//			 */
//		}
//		
//		// Verify the correctness of the generateExpandedBuffers by using real data
//		rects = wknnPlus.generateExpandedBuffers(query, Util.EPSILON/2, Util.EPSILON);
//		System.out.println("---Real Data---");
//		for (Rect rect : rects)
//			System.out.println(rect);
//		
//		System.out.println("\n\n---WKNN---");
////		trajectory = Util.getTrajectory(297, Util.bjDirectoryPath);
//		trajectory = Util.getTrajectory(597, Util.bjDirectoryPath);
//		long start = System.currentTimeMillis();
//		wknnPlus.wknnPlus(wknnPlus.rtreeDB, trajectory, Util.EPSILON/2, Util.EPSILON, 50);
//		System.out.println("WKnnPlus takes " + (System.currentTimeMillis()-start));
		
		
	}
	
}
