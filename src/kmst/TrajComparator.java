package kmst;

import java.util.Comparator;

/**
 * 
 * @author uqhwan15
 * @since 2012/02/07
 */
public class TrajComparator implements Comparator<Traj> {
	@Override
	public int compare(Traj o1, Traj o2) {
		return (o1.weight > o2.weight) ? 1 : 0;
	}
}
