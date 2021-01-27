package org.peakaboo.calibration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.peakaboo.curvefit.curve.fitting.FittingResult;
import org.peakaboo.curvefit.curve.fitting.FittingResultSet;
import org.peakaboo.curvefit.peak.table.Element;
import org.peakaboo.curvefit.peak.transition.ITransitionSeries;
import org.peakaboo.curvefit.peak.transition.PrimaryTransitionSeries;
import org.peakaboo.curvefit.peak.transition.TransitionShell;
import org.peakaboo.framework.cyclops.ReadOnlySpectrum;
import org.peakaboo.framework.cyclops.SpectrumCalculations;
import org.peakaboo.framework.druthers.DruthersStorable;

/*
 * NOTE: Calibration does not use PeakTable TransitionSeries, 
 * it uses blank ones. This is so that the PeakTable does not 
 * limit which transition series it can represent. 
 */
public class CalibrationProfile {

	private static final String CREATION_DATE_UNKNOWN = "-";
	
	private CalibrationReference reference;
	Map<ITransitionSeries, Float> calibrations;
	private String name = "";
	private String creationDate = "";
	List<ITransitionSeries> interpolated;
	
	/**
	 * Create an empty CalibrationProfile
	 */
	public CalibrationProfile() {
		this.reference = CalibrationReference.empty();
		calibrations = new LinkedHashMap<>();
		interpolated = new ArrayList<>();
		name = "Empty Z-Calibration Profile";
		creationDate = LocalDate.now().toString();
	}
	
	public CalibrationProfile(CalibrationReference reference, FittingResultSet sample) {
		this();
		this.reference = reference;
		this.name = "Unknown " + reference.getName() + " Z-Calibration Profile";
				
		if (!sample.getParameters().getCalibration().isZero()) {
		
			//Build profile
			for (FittingResult fit : sample) {
				
				ITransitionSeries ts = fit.getTransitionSeries();
				int channel = sample.getParameters().getCalibration().channelFromEnergy(ts.getStrongestTransition().energyValue);
				
				if (! reference.hasConcentration(ts)) { continue; }
				
				//TODO: Is this the right way to measure sample intensity? Measuring sum rather than strongestTS height?
				if (channel >= fit.getFitChannels()) { continue; }
				float sampleIntensity = fit.getFitSum();
				float referenceValue = reference.getConcentration(ts);
				float calibration = (sampleIntensity / referenceValue) * 1000f;
				
				//don't add if element is being completely suppressed, this only seems 
				//to happen when the fitting solver algorithm incorrectly completely hides it 
				//we'll interpolate it later
				if (calibration < 1f) { continue; }
				if (Float.isInfinite(calibration) || Float.isNaN(calibration)) { continue; }
				calibrations.put(ts, calibration);
			}
			
		}
		
		CalibrationProcessor smoother = new BracketedCalibrationSmoother(0.1f);
		CalibrationProcessor interpolationSmoother = new AggressiveCalibrationSmoother(5);
		CalibrationProcessor interpolator = new LinearCalibrationInterpolator();
		CalibrationProcessor normalizer = new CalibrationNormalizer();
		
		
		
		// interpolate and smooth a copy of this profile, then copy the smooed
		// interpolated values back to this one. We do this so that interpolated values
		// aren't influenced too much by one outlier neighbour.
		
		CalibrationProfile smoothed = new CalibrationProfile(this);
		interpolator.process(smoothed);
		interpolationSmoother.process(smoothed);
		for (ITransitionSeries ts : smoothed.interpolated ) {
			this.interpolated.add(ts);
			this.calibrations.put(ts, smoothed.calibrations.get(ts));
		}
		
		//smooth things just a little bit
		smoother.process(this);
		
		//normalize values against anchor element
		normalizer.process(this);
		
	}
	
	public CalibrationProfile(CalibrationProfile copy) {
		this();
		this.reference = copy.reference;
		this.calibrations = new LinkedHashMap<>(copy.calibrations);
		this.name = copy.name;
		this.interpolated = new ArrayList<>(copy.interpolated);
	}
	

	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LinkedHashMap<ITransitionSeries, Float> getCalibrations() {
		return new LinkedHashMap<>(calibrations);
	}

	public boolean contains(ITransitionSeries ts) {
		return calibrations.keySet().contains(ts);
	}
	
	public float getCalibration(ITransitionSeries ts) {
		return calibrations.get(ts);
	}
	
	public CalibrationReference getReference() {
		return reference;
	}
	
	public float calibratedSum(FittingResult fittingResult) {
		ITransitionSeries ts = fittingResult.getTransitionSeries();
		float rawfit = fittingResult.getFitSum();
		return calibrate(rawfit, ts);
	}
	
	public float calibrate(float value, ITransitionSeries ts) {
		if (calibrations.keySet().contains(ts)) {
			float calibration = calibrations.get(ts);
			return value / calibration;
		} else {
			return value;
		}
	}
	
	public float calibrate(FittingResult result) {
		return calibrate(result.getFitSum(), result.getTransitionSeries());
	}
	
	public ReadOnlySpectrum calibrateMap(ReadOnlySpectrum data, ITransitionSeries ts) {
		if (!contains(ts)) {
			return data;
		}
		float calibration = getCalibration(ts);
		return SpectrumCalculations.divideBy(data, calibration);
	}
	
	public boolean isEmpty() {
		return calibrations.size() == 0;
	}
	
	/**
	 * returns a sorted list of TransitionSeries in this profile 
	 */
	public List<ITransitionSeries> getTransitionSeries(TransitionShell tst) {
		List<ITransitionSeries> tss = calibrations
				.keySet()
				.stream()
				.filter(ts -> ts.getShell() == tst)
				.sorted((a, b) -> Integer.compare(a.getElement().ordinal(), b.getElement().ordinal()))
				.collect(Collectors.toList());
		return tss;
	}
	
	public List<ITransitionSeries> getInterpolated() {
		return new ArrayList<>(this.interpolated);
	}
	
	
	public static String save(CalibrationProfile profile) {
		SerializedCalibrationProfile saving = new SerializedCalibrationProfile();
		saving.referenceUUID = profile.reference.getUuid();
		saving.referenceName = profile.reference.getName();
		saving.name = profile.name;
		saving.creationDate = profile.creationDate;
		for (ITransitionSeries ts : profile.calibrations.keySet()) {
			saving.calibrations.put(ts.toIdentifierString(), profile.calibrations.get(ts));
		}
		for (ITransitionSeries ts : profile.interpolated) {
			saving.interpolated.add(ts.toIdentifierString());
		}
		return saving.serialize();
	}
	
	
	public static CalibrationProfile load(Path path) throws IOException {
		return load(new String(Files.readAllBytes(path)));
	}
	
	public static CalibrationProfile load(String yaml) {
		CalibrationProfile profile = new CalibrationProfile();
		SerializedCalibrationProfile loaded = SerializedCalibrationProfile.deserialize(yaml);
		for (String tsidentifier : loaded.calibrations.keySet()) {
			ITransitionSeries ts = ITransitionSeries.get(tsidentifier);
			profile.calibrations.put(ts, loaded.calibrations.get(tsidentifier));
		}
		for (String tsidentifier : loaded.interpolated) {
			ITransitionSeries ts = ITransitionSeries.get(tsidentifier);
			profile.interpolated.add(ts);
		}
		
		profile.reference = CalibrationPluginManager.system().getByUUID(loaded.referenceUUID).create();
		if (profile.reference == null) {
			throw new RuntimeException("Cannot find Calibration Reference '" + loaded.referenceName + "' (" + loaded.referenceUUID + ")");
		}
		
		profile.creationDate = loaded.creationDate;
		if (profile.creationDate != null) {
			profile.creationDate = CREATION_DATE_UNKNOWN;
		}
		
		profile.name = loaded.name;
		if (profile.name == null) {
			profile.name = profile.reference.getName();
		}
		
		return profile;
	}

	public static void main(String[] args) throws IOException {
		
		CalibrationPluginManager.init(new File("/home/nathaniel/Desktop/PBCP/"));
		CalibrationProfile p = CalibrationProfile.load(new File("/home/nathaniel/Desktop/nist610sigray-15.pbcp").toPath());
		
		for (TransitionShell tst : TransitionShell.values()) {
			System.out.println(tst);
			for (Element e : Element.values()) {
				ITransitionSeries ts = new PrimaryTransitionSeries(e, tst);
				if (!p.contains(ts)) { continue; }
				System.out.println(e.atomicNumber() + ", " + p.getCalibration(ts));
			}
		}
		
	}

	
	
	
}


class SerializedCalibrationProfile extends DruthersStorable {
	public String referenceUUID = null;
	public String referenceName = null;
	public String name = null;
	public String creationDate = null;
	public Map<String, Float> calibrations = new LinkedHashMap<>();
	public List<String> interpolated = new ArrayList<>();
}
 