package org.peakaboo.curvefit.peak.search.scoring;

import java.util.HashMap;
import java.util.Map;

import org.peakaboo.curvefit.peak.transition.ITransitionSeries;

public class CompoundFittingScorer implements FittingScorer {

	private Map<FittingScorer, Float> scorers = new HashMap<>();
	private float totalWeight = 0f;
	
	public void add(FittingScorer scorer, float weight) {
		scorers.put(scorer, weight);
		totalWeight += weight;
	}
	
	@Override
	public float score(ITransitionSeries ts) {
		float score = 1f;
		for (FittingScorer scorer : scorers.keySet()) {
			float weight = scorers.get(scorer);
			float thisScore = scorer.score(ts);
			if (Float.isNaN(thisScore)) {
				thisScore = 0;
			}
			score *= thisScore * (weight / totalWeight);
		}
		return score;
	}

}
