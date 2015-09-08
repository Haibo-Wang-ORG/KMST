package kmst;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import kmst.KMSTStar.Rect;

import spatialindex.rtree.RTree;
import spatialindex.spatialindex.IData;
import spatialindex.spatialindex.IEntry;
import spatialindex.spatialindex.INode;
import spatialindex.spatialindex.IQueryStrategy;
import spatialindex.spatialindex.IShape;
import spatialindex.spatialindex.ISpatialIndex;
import spatialindex.spatialindex.IVisitor;
import spatialindex.spatialindex.Region;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IBuffer;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;
import spatialindex.storagemanager.RandomEvictionsBuffer;

import common.Data;
import common.Statistic;
import common.Util;

/**
 * 
 * @author uqhwan15
 * @since 2012/02/09
 */
public class RTreeDB {
	ISpatialIndex rtree;
	
	public RTreeDB(Util.DATATYPE type) {
		rtree = initializeRtree(type);
	}
	
	/**
	 * If flag is true, then build the rtree first
	 * @param flag
	 */
	public RTreeDB(Util.DATATYPE type, boolean flag) {
		if (flag == true) buildRtree(type);
		rtree = initializeRtree(type);
	}
	
	public ArrayList<Data> rangeQuery(float lon, float lat, float radius, Statistic statistic, Util.DATATYPE type) {
		double[] f1 = new double[2];
		double[] f2 = new double[2];
		f1[0] = lon - radius; f1[1] = lat - radius;
		f2[0] = lon + radius; f2[1] = lat + radius;
		Region r = new Region(f1, f2);
		
		if (type == Util.DATATYPE.BEIJING) {
			MyBJVisitor vis = new MyBJVisitor();rtree.intersectionQuery(r, vis);
			statistic.index_io += vis.m_indexIO;
			statistic.leaf_io += vis.m_leafIO;
			return vis.list;
		} else {
			MyBusTruckVisitor vis = new MyBusTruckVisitor();
			rtree.intersectionQuery(r, vis);
			statistic.index_io += vis.m_indexIO;
			statistic.leaf_io += vis.m_leafIO;
			return vis.list;
		}
	}
	
	public ArrayList<Data> rangeQuery(Rect rect, Statistic statistic, Util.DATATYPE type) {
		double[] f1 = new double[2];
		double[] f2 = new double[2];
		f1[0] = rect.lon1; f1[1] = rect.lat1;
		f2[0] = rect.lon2; f2[1] = rect.lat2;
		Region r = new Region(f1, f2);

		if (type == Util.DATATYPE.BEIJING) {
			MyBJVisitor vis = new MyBJVisitor();rtree.intersectionQuery(r, vis);
			statistic.index_io += vis.m_indexIO;
			statistic.leaf_io += vis.m_leafIO;
			return vis.list;
		} else {
			MyBusTruckVisitor vis = new MyBusTruckVisitor();
			rtree.intersectionQuery(r, vis);
			statistic.index_io += vis.m_indexIO;
			statistic.leaf_io += vis.m_leafIO;
			return vis.list;
		}
	}
	
	private ISpatialIndex initializeRtree(Util.DATATYPE type) {
		try {
			// Create a disk based storage manager.
			PropertySet ps = new PropertySet();
	
//			ps.setProperty("FileName", "./DataSet/Beijing/RTree/trajectory");
				// .idx and .dat extensions will be added.
			if (type == Util.DATATYPE.BEIJING) {
				ps.setProperty("FileName", Util.bjRtree);
			} else if (type == Util.DATATYPE.BUS) {
				ps.setProperty("FileName", Util.busRtree);
			} else {
				ps.setProperty("FileName", Util.truckRtree);
			}
			
	
			IStorageManager diskfile = new DiskStorageManager(ps);
	
			IBuffer file = new RandomEvictionsBuffer(diskfile, 10, false);
				// applies a main memory random buffer on top of the persistent storage manager
				// (LRU buffer, etc can be created the same way).
	
			PropertySet ps2 = new PropertySet();
	
			// If we need to open an existing tree stored in the storage manager, we only
			// have to specify the index identifier as follows
			Integer i = new Integer(1); // INDEX_IDENTIFIER_GOES_HERE (suppose I know that in this case it is equal to 1);
			ps2.setProperty("IndexIdentifier", i);
				// this will try to locate and open an already existing r-tree index from file manager file.
	
			ISpatialIndex tree = new RTree(ps2, file);
			return tree;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void buildRtree(Util.DATATYPE type) {
		// Create a disk based storage manager.
		PropertySet ps = new PropertySet();

		Boolean b = new Boolean(true);
		ps.setProperty("Overwrite", b);
			//overwrite the file if it exists.

		if (type == Util.DATATYPE.BEIJING) {
			ps.setProperty("FileName", Util.bjRtree);
		} else if (type == Util.DATATYPE.BUS) {
			ps.setProperty("FileName", Util.busRtree);
		} else {
			ps.setProperty("FileName", Util.truckRtree);
		}
			// .idx and .dat extensions will be added.

		Integer i = new Integer(4096);
		ps.setProperty("PageSize", i);
			// specify the page size. Since the index may also contain user defined data
			// there is no way to know how big a single node may become. The storage manager
			// will use multiple pages per node if needed. Off course this will slow down performance.

		try {
			IStorageManager diskfile = new DiskStorageManager(ps);
	
			IBuffer file = new RandomEvictionsBuffer(diskfile, 10, false);
				// applies a main memory random buffer on top of the persistent storage manager
				// (LRU buffer, etc can be created the same way).
	
			// Create a new, empty, RTree with dimensionality 2, minimum load 70%, using "file" as
			// the StorageManager and the RSTAR splitting policy.
			PropertySet ps2 = new PropertySet();
	
			Double f = new Double(0.7);
			ps2.setProperty("FillFactor", f);
	
			i = new Integer(100);
			ps2.setProperty("IndexCapacity", i);
			ps2.setProperty("LeafCapacity", i);
				// Index capacity and leaf capacity may be different.
	
			i = new Integer(2);
			ps2.setProperty("Dimension", i);
	
			ISpatialIndex tree = new RTree(ps2, file);
	
			int count = 1;
			int id;
			double x1, x2, y1, y2;
			double lon, lat, epsilon;
			double[] f1 = new double[2];
			double[] f2 = new double[2];
	
			long start = System.currentTimeMillis();
			long tempStart = start;
		
		////////////////////////////////////////////////////
			BufferedReader reader;
			if (type == Util.DATATYPE.BEIJING) {
				reader = new BufferedReader(new FileReader(new File(Util.bjDataFile)));
				epsilon = 1; // set bounding box length for beijing point as 1 meter, which is much less than the beijing threshold Util.bjEPSILON
			} else if (type == Util.DATATYPE.BUS) {
				reader = new BufferedReader(new FileReader(new File(Util.busDataFile)));
				epsilon = 0.0001;
			} else {
				reader = new BufferedReader(new FileReader(new File(Util.truckDataFile)));
				epsilon = 0.0001;
			}
			
			String line;
			if (type == Util.DATATYPE.BEIJING) { // handle beijing data format
				id = 0;
				while ((line = reader.readLine()) != null)
				{
					if (line.trim().equals("")) continue;
					String[] items = line.split(";");
//					id = Integer.valueOf(items[0])*10000+Integer.valueOf(items[1]);
					id ++;
					lon = Double.valueOf(items[2]);
					lat = Double.valueOf(items[3]);
	//				double weight = 1;
					x1 = lon - epsilon;
					y1 = lat - epsilon;
					x2 = lon + epsilon;
					y2 = lat + epsilon;
		
					f1[0] = x1; f1[1] = y1;
					f2[0] = x2; f2[1] = y2;
					Region r = new Region(f1, f2);
		
					String data = line;
					tree.insertData(data.getBytes(), r, id);
					
					if (count++ == 100000) { // commit rtree for every 10000 points
	    				long tempEnd = System.currentTimeMillis();
	    				count = 1;
	    				System.out.println("Insert 100000 points into Rtree:\t" + (tempEnd-tempStart)/1000 + "\tseconds");
	    				tempStart = tempEnd;
	    				System.gc();
	    			}
				}
			} else { // handle bus or truck data format
				id = 0;
				while ((line = reader.readLine()) != null)
				{
					if (line.trim().equals("")) continue;
					String[] items = line.split(";");
					id ++;
					lon = Double.valueOf(items[5]);
					lat = Double.valueOf(items[4]);
	//				double weight = 1;
					x1 = lon - epsilon;
					y1 = lat - epsilon;
					x2 = lon + epsilon;
					y2 = lat + epsilon;
		
					f1[0] = x1; f1[1] = y1;
					f2[0] = x2; f2[1] = y2;
					Region r = new Region(f1, f2);
		
					String data = line;
					tree.insertData(data.getBytes(), r, id);
					
					if (count++ == 100000) { // commit rtree for every 10000 points
	    				long tempEnd = System.currentTimeMillis();
	    				count = 1;
	    				System.out.println("Insert 100000 points into Rtree:\t" + (tempEnd-tempStart)/1000 + "\tseconds");
	    				tempStart = tempEnd;
	    				System.gc();
	    			}
				}
			}
			
			boolean ret = tree.isIndexValid();
			if (ret == false) System.err.println("Structure is INVALID!");

			// flush all pending changes to persistent storage (needed since Java might not call finalize when JVM exits).
			tree.flush();
			
			System.out.println("RTree build time:\t" + (System.currentTimeMillis()-start)/1000 + "\tseconds");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class MyBJVisitor implements IVisitor
	{
		public int m_indexIO = 0;
		public int m_leafIO = 0;
		public ArrayList<Data> list = new ArrayList<Data>();

		public void visitNode(final INode n)
		{
			if (n.isLeaf()) m_leafIO++;
			else m_indexIO++;
		}

		public void visitData(final IData d)
		{
			String[] items = new String(d.getData()).split(";");
			Data data = new Data();
			data.data[0] = data.data[1] = Float.valueOf(items[2]);
			data.data[2] = data.data[3] = Float.valueOf(items[3]);
			data.weight = 1;
			data.trajectoryID = Integer.valueOf(items[0]);
			list.add(data);
		}
	}
	
	class MyBusTruckVisitor implements IVisitor
	{
		public int m_indexIO = 0;
		public int m_leafIO = 0;
		public ArrayList<Data> list = new ArrayList<Data>();

		public void visitNode(final INode n)
		{
			if (n.isLeaf()) m_leafIO++;
			else m_indexIO++;
		}

		public void visitData(final IData d)
		{
			String[] items = new String(d.getData()).split(";");
			Data data = new Data();
			data.data[0] = data.data[1] = Float.valueOf(items[5]);
			data.data[2] = data.data[3] = Float.valueOf(items[4]);
			data.weight = 1;
			data.trajectoryID = Integer.valueOf(items[8]);
			list.add(data);
		}
	}
	
//	public void dothat() {
//		double[] f1 = new double[2];
//		double[] f2 = new double[2];
//		f1[0] = Double.MIN_VALUE; f1[1] = Double.MIN_VALUE;
//		f2[0] = Double.MAX_VALUE; f2[1] = Double.MAX_VALUE;
//		Region r = new Region(f1, f2);
//
//		MyVisitor vis = new MyVisitor();
//		rtree.intersectionQuery(r, vis);
//		
//		MyQueryStrategy qs = new MyQueryStrategy();
//		rtree.queryStrategy(qs);
//
////		System.err.println("Indexed space: " + qs.m_indexedSpace);
//		System.out.println(qs.n);
//		
//		System.out.println(vis.m_indexIO + " + " + vis.m_leafIO + " = " + (vis.m_indexIO+vis.m_leafIO));
//	}
	
	// example of a Strategy pattern.
	// traverses the tree by level.
	class MyQueryStrategy implements IQueryStrategy
	{
		private ArrayList ids = new ArrayList();
		int n = 0;

		public void getNextEntry(IEntry entry, int[] nextEntry, boolean[] hasNext)
		{
			Region r = entry.getShape().getMBR();

//			System.out.println(r.m_pLow[0] + " " + r.m_pLow[1]);
//			System.out.println(r.m_pHigh[0] + " " + r.m_pLow[1]);
//			System.out.println(r.m_pHigh[0] + " " + r.m_pHigh[1]);
//			System.out.println(r.m_pLow[0] + " " + r.m_pHigh[1]);
//			System.out.println(r.m_pLow[0] + " " + r.m_pLow[1]);
//			System.out.println();
//			System.out.println();
				// print node MBRs gnuplot style!

			// traverse only index nodes at levels 2 and higher.
			if (entry instanceof INode && ((INode) entry).getLevel() > 1)
			{
				for (int cChild = 0; cChild < ((INode) entry).getChildrenCount(); cChild++)
				{
					ids.add(new Integer(((INode) entry).getChildIdentifier(cChild)));
					n ++;
				}
			}

			if (! ids.isEmpty())
			{
				nextEntry[0] = ((Integer) ids.remove(0)).intValue();
				hasNext[0] = true;
			}
			else
			{
				hasNext[0] = false;
			}
		}
	};
	
	class MyQueryStrategy2 implements IQueryStrategy
	{
		public Region m_indexedSpace;

		public void getNextEntry(IEntry entry, int[] nextEntry, boolean[] hasNext)
		{
			// the first time we are called, entry points to the root.
			IShape s = entry.getShape();
			m_indexedSpace = s.getMBR();

			// stop after the root.
			hasNext[0] = false;
		}
	}
	
	public static void main(String[] args) {
//		RTreeDB truckDB = new RTreeDB(Util.DATATYPE.TRUCK, true);
////		db.dothat();
//		RTreeDB busDB = new RTreeDB(Util.DATATYPE.BUS, true);
		new RTreeDB(Util.DATATYPE.BEIJING, true);
	}
}
