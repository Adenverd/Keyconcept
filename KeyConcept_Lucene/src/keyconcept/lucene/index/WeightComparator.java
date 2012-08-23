package keyconcept.lucene.index;

import java.util.Comparator;
import java.util.Map;

/**
 * @author Sawyer Anderson
 * Used to compare Map.Entry<String, Float> by value, rather than by key
 *
 */
class WeightComparator implements Comparator{
	public int compare (Object e1, Object e2){
		float freq1 = ((Map.Entry<String, Float>) e1).getValue();
		float freq2 = ((Map.Entry<String, Float>) e2).getValue();
		
		if (freq1 > freq2) return -1;
		else if (freq1 < freq2) return 1;
		else return 0;
	}
}