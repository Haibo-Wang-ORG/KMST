package test;

import kmst.KMST;
import kmst.KMSTPlus;
import kmst.KMSTStar;
import kmst.SequentialScan;
import common.Util;


public class Test {
	public void doTest() {
		new SequentialScan(Util.DATATYPE.BUS).doStatistic();
		new SequentialScan(Util.DATATYPE.TRUCK).doStatistic();
		System.out.println("SEQBeijing");
		new SequentialScan(Util.DATATYPE.BEIJING).doStatistic();
		
		System.out.println("KMST");
		new KMST(Util.DATATYPE.BUS).doStatistic();
		new KMST(Util.DATATYPE.TRUCK).doStatistic();
		System.out.println("KMSTBeijing");
		new KMST(Util.DATATYPE.BEIJING).doStatistic();
		
		System.out.println("KMSTPlus");
		new KMSTPlus(Util.DATATYPE.BUS).doStatistic();
		new KMSTPlus(Util.DATATYPE.TRUCK).doStatistic();
		System.out.println("KMSTPlusBeijing");
		new KMSTPlus(Util.DATATYPE.BEIJING).doStatistic();
		
		System.out.println("KMSTStar");
		new KMSTStar(Util.DATATYPE.BUS).doStatistic();
		new KMSTStar(Util.DATATYPE.TRUCK).doStatistic();
		System.out.println("KMSTStarBeijing");
		new KMSTStar(Util.DATATYPE.BEIJING).doStatistic();
	}
	
	public static void main(String[] args) {
		Test test = new Test();
		test.doTest();
	}
}
