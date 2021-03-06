package org.peakaboo.calibration;

import java.util.Map;

import org.peakaboo.curvefit.peak.transition.ITransitionSeries;
import org.peakaboo.curvefit.peak.transition.TransitionShell;

public class CalibrationNormalizer implements CalibrationProcessor {

	@Override
	public void process(CalibrationReference reference, Map<ITransitionSeries, Float> calibrations) {
		Float anchorValue = calibrations.get(reference.getAnchor());
		if (anchorValue == null) return;
		for (TransitionShell tst : TransitionShell.values()) {
			normalize(calibrations, tst, anchorValue);
		}
	}

	private void normalize(Map<ITransitionSeries, Float> calibrations, TransitionShell tst, float against) {
		
		for (ITransitionSeries ts : calibrations.keySet()) {
			if (ts.getShell() != tst) { continue; }
			
			float value = calibrations.get(ts);
			calibrations.put(ts, value/against);			
		}
	}
	
	
}
