package keyconcept.lucene.index;

import java.util.Comparator;
import java.util.Map;
/**
 * @author Sawyer Anderson
 * Used to compare Map.Entry<String, Integer> by value, rather than by key.
 *
 */
public class FreqComparator implements Comparator{
	public int compare (Object e1, Object e2){
		int freq1 = ((Map.Entry<String, Integer>) e1).getValue();
		int freq2 = ((Map.Entry<String, Integer>) e2).getValue();
		
		if (freq1 > freq2) return -1;
		else if (freq1 < freq2) return 1;
		else return 0;
	}
}
