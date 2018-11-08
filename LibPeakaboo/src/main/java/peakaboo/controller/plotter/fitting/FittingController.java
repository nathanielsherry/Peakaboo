package peakaboo.controller.plotter.fitting;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import cyclops.Pair;
import cyclops.ReadOnlySpectrum;
import cyclops.util.Mutable;
import eventful.EventfulCache;
import eventful.EventfulEnumListener;
import eventful.EventfulType;
import peakaboo.calibration.CalibrationProfile;
import peakaboo.calibration.CalibrationReference;
import peakaboo.controller.plotter.PlotController;
import peakaboo.controller.plotter.filtering.FilteringController;
import peakaboo.curvefit.curve.fitting.EnergyCalibration;
import peakaboo.curvefit.curve.fitting.FittingResult;
import peakaboo.curvefit.curve.fitting.FittingResultSet;
import peakaboo.curvefit.curve.fitting.FittingSet;
import peakaboo.curvefit.curve.fitting.fitter.CurveFitter;
import peakaboo.curvefit.curve.fitting.fitter.OptimizingCurveFitter;
import peakaboo.curvefit.curve.fitting.solver.FittingSolver;
import peakaboo.curvefit.curve.fitting.solver.OptimizingFittingSolver;
import peakaboo.curvefit.peak.escape.EscapePeakType;
import peakaboo.curvefit.peak.fitting.FittingFunction;
import peakaboo.curvefit.peak.search.PeakProposal;
import peakaboo.curvefit.peak.search.searcher.DerivativePeakSearcher;
import peakaboo.curvefit.peak.table.PeakTable;
import peakaboo.curvefit.peak.transition.TransitionSeries;
import peakaboo.curvefit.peak.transition.TransitionSeriesType;
import peakaboo.datasource.model.components.metadata.Metadata;
import plural.executor.ExecutorSet;


public class FittingController extends EventfulType<Boolean>
{

	FittingModel fittingModel;
	PlotController plot;
	
	
	public FittingController(PlotController plotController)
	{
		this.plot = plotController;
		fittingModel = new FittingModel();
		
		EventfulEnumListener<EventfulCache.CacheEvents> cacheListener = event -> {
			if (event == EventfulCache.CacheEvents.INVALIDATED) {
				updateListeners(false);
			}
		};
		
		fittingModel.selectionResults = new EventfulCache<>(() -> {
			ReadOnlySpectrum data = plot.filtering().getFilteredPlot();
			if (!plot.data().hasDataSet() || data == null) {
				return null;
			}
			System.out.println("fittings pre-solve");
			return getFittingSolver().solve(data, fittingModel.selections, getCurveFitter());
		});
		
		fittingModel.proposalResults = new EventfulCache<>(() -> {
			if (!plot.data().hasDataSet() || plot.currentScan() == null) {
				return null;
			}
			System.out.println("proposals pre-solve");
			return getFittingSolver().solve(getFittingSelectionResults().getResidual(), fittingModel.proposals, getCurveFitter());
		});
		
		fittingModel.selectionResults.addDependency(plot.filtering().getFilteredPlotCache());
		fittingModel.proposalResults.addDependency(fittingModel.selectionResults);
		
		fittingModel.proposalResults.addListener(cacheListener);
		
		
	}
	
	public FittingModel getFittingModel()
	{
		return fittingModel;
	}
	
	private void setUndoPoint(String change)
	{
		plot.history().setUndoPoint(change);
	}
	
	
	public void addTransitionSeries(TransitionSeries e)
	{
		if (e == null) return;
		fittingModel.selections.addTransitionSeries(e);
		setUndoPoint("Add Fitting");
		fittingDataInvalidated();
	}
	
	
	public void moveTransitionSeries(int from, int to) {
		//we'll be removing the item from the list, so if the 
		//destination is greater than the source, decrement it 
		//to make up the difference
		if (to > from) { to--; }
		
		TransitionSeries ts = fittingModel.selections.getFittedTransitionSeries().get(from);
		fittingModel.selections.remove(ts);
		fittingModel.selections.insertTransitionSeries(to, ts);
		
		setUndoPoint("Move Fitting");
		fittingDataInvalidated();
		
	}
	
	public void addAllTransitionSeries(Collection<TransitionSeries> tss)
	{
		for (TransitionSeries ts : tss)
		{
			fittingModel.selections.addTransitionSeries(ts);
		}
		setUndoPoint("Add Fittings");
		fittingDataInvalidated();
	}

	public void clearTransitionSeries()
	{
		
		fittingModel.selections.clear();
		setUndoPoint("Clear Fittings");
		fittingDataInvalidated();
	}

	public void removeTransitionSeries(TransitionSeries e)
	{
		
		fittingModel.selections.remove(e);
		setUndoPoint("Remove Fitting");
		fittingDataInvalidated();
	}

	public List<TransitionSeries> getFittedTransitionSeries()
	{
		return fittingModel.selections.getFittedTransitionSeries();
	}

	public List<TransitionSeries> getUnfittedTransitionSeries()
	{
		final List<TransitionSeries> fitted = getFittedTransitionSeries();
		return PeakTable.SYSTEM.getAll().stream().filter(ts -> (!fitted.contains(ts))).collect(toList());
	}
	
	public void setTransitionSeriesVisibility(TransitionSeries e, boolean show)
	{
		fittingModel.selections.setTransitionSeriesVisibility(e, show);
		setUndoPoint("Fitting Visiblitiy");
		fittingDataInvalidated();
	}

	public boolean getTransitionSeriesVisibility(TransitionSeries e)
	{
		return e.visible;
	}

	public List<TransitionSeries> getVisibleTransitionSeries()
	{
		return getFittedTransitionSeries().stream().filter(ts -> ts.visible).collect(toList());
	}

	public float getTransitionSeriesIntensity(TransitionSeries ts)
	{
		if (getFittingSelectionResults() == null) return 0.0f;

		for (FittingResult result : getFittingSelectionResults().getFits())
		{
			if (result.getTransitionSeries() == ts) {
				float max = result.getFit().max();
				if (Float.isNaN(max)) max = 0f;
				return max;
			}
		}
		return 0.0f;

	}



	public void moveTransitionSeriesUp(List<TransitionSeries> tss)
	{
		fittingModel.selections.moveTransitionSeriesUp(tss);
		setUndoPoint("Move Fitting Up");
		fittingDataInvalidated();
	}
	

	public void moveTransitionSeriesDown(List<TransitionSeries> tss)
	{
		fittingModel.selections.moveTransitionSeriesDown(tss);
		setUndoPoint("Move Fitting Down");
		fittingDataInvalidated();
	}

	public void fittingDataInvalidated()
	{
		// Clear cached values, since they now have to be recalculated
		fittingModel.selectionResults.invalidate();

		// this will call update listener for us
		fittingProposalsInvalidated();

	}

	
	public void addProposedTransitionSeries(TransitionSeries e)
	{
		fittingModel.proposals.addTransitionSeries(e);
		fittingProposalsInvalidated();
	}

	public void removeProposedTransitionSeries(TransitionSeries e)
	{
		fittingModel.proposals.remove(e);
		fittingProposalsInvalidated();
	}

	public void clearProposedTransitionSeries()
	{
		fittingModel.proposals.clear();
		fittingProposalsInvalidated();
	}

	public List<TransitionSeries> getProposedTransitionSeries()
	{
		return fittingModel.proposals.getFittedTransitionSeries();
	}

	public void commitProposedTransitionSeries()
	{
		addAllTransitionSeries(fittingModel.proposals.getFittedTransitionSeries());
		fittingModel.proposals.clear();
		fittingDataInvalidated();
	}

	public void fittingProposalsInvalidated()
	{
		// Clear cached values, since they now have to be recalculated
		fittingModel.proposalResults.invalidate();
	}

	public void setEscapeType(EscapePeakType type)
	{
		fittingModel.selections.getFittingParameters().setEscapeType(type);
		fittingModel.proposals.getFittingParameters().setEscapeType(type);
		
		fittingDataInvalidated();
		
		setUndoPoint("Escape Peaks");
		updateListeners(false);
	}

	public EscapePeakType getEscapeType()
	{
		return fittingModel.selections.getFittingParameters().getEscapeType();
	}
	
	public List<TransitionSeries> proposeTransitionSeriesFromChannel(final int channel, TransitionSeries currentTS)
	{
		
		if (! plot.data().hasDataSet() ) return null;
				
		return PeakProposal.fromChannel(
				plot.filtering().getFilteredPlot(),
				this.getFittingSelections(),
				this.getFittingProposals(),
				this.getCurveFitter(),
				this.getFittingSolver(),
				channel,
				currentTS,
				6
		).stream().map(p -> p.first).collect(Collectors.toList());
	}

	/**
	 * Given a channel, return the existing FittingResult which makes most sense to
	 * 'select' for that channel, or null if there are no good fits.
	 */
	public TransitionSeries selectTransitionSeriesAtChannel(int channel) {      
        float bestValue = 1f;
        FittingResult bestFit = null;

        if (getFittingSelectionResults() == null) {
            return null;
        }
        
		for (FittingResult fit : getFittingSelectionResults()) {
			if (!fit.getFit().inBounds(channel)) {
				continue;
			}
            float value = fit.getFit().get(channel);
            if (value > bestValue) {
                bestValue = value;
                bestFit = fit;
            }
        }
		if (bestFit == null) {
			return null;
		}
		return bestFit.getTransitionSeries();
	}
	
	public boolean canMap()
	{
		return ! (getVisibleTransitionSeries().size() == 0 || plot.data().getDataSet().getScanData().scanCount() == 0);
	}


	public void setFittingParameters(int scanSize, float min, float max)
	{
		fittingModel.selections.getFittingParameters().setCalibration(min, max, scanSize);
		fittingModel.proposals.getFittingParameters().setCalibration(min, max, scanSize);

		//TODO: Why is this here? Are we just resetting it to be sure they stay in sync?
		fittingModel.selections.getFittingParameters().setEscapeType(getEscapeType());
		fittingModel.proposals.getFittingParameters().setEscapeType(getEscapeType());

		
		setUndoPoint("Calibration");
		plot.filtering().filteredDataInvalidated();
	}

	public void setMaxEnergy(float max) {
		int dataWidth = plot.data().getDataSet().getAnalysis().channelsPerScan();
		setFittingParameters(dataWidth, getMinEnergy(), max);

		updateListeners(false);
	}

	public float getMaxEnergy()
	{
		return fittingModel.selections.getFittingParameters().getCalibration().getMaxEnergy();
	}

	public void setMinEnergy(float min) {
		int dataWidth = plot.data().getDataSet().getAnalysis().channelsPerScan();
		setFittingParameters(dataWidth, min, getMaxEnergy());
		updateListeners(false);
	}

	
	public float getMinEnergy()
	{
		return fittingModel.selections.getFittingParameters().getCalibration().getMinEnergy();
	}
	
	public EnergyCalibration getEnergyCalibration() {
		return fittingModel.selections.getFittingParameters().getCalibration();
	}

	
	public boolean hasProposalFitting()
	{
		return fittingModel.proposalResults.getValue() != null;
	}

	public boolean hasSelectionFitting()
	{
		return fittingModel.selectionResults.getValue() != null;
	}

	public FittingSet getFittingSelections()
	{
		return fittingModel.selections;
	}

	public FittingSet getFittingProposals()
	{
		return fittingModel.proposals;
	}
	
	public FittingResultSet getFittingProposalResults()
	{
		return fittingModel.proposalResults.getValue();
	}

	public FittingResultSet getFittingSelectionResults()
	{
		return fittingModel.selectionResults.getValue();
	}

	public List<TransitionSeries> getHighlightedTransitionSeries() {
		return fittingModel.highlighted;
	}
	
	public void setHighlightedTransitionSeries(List<TransitionSeries> highlighted) {
		//If the highlight already matches, don't bother
		if (fittingModel.highlighted != null && fittingModel.highlighted.equals(highlighted)) {
			return;
		}
		fittingModel.highlighted = highlighted;
		updateListeners(false);
	}
	
	public float getFWHMBase() {
		return fittingModel.selections.getFittingParameters().getFWHMBase();
	}
	
	public void setFWHMBase(float base) {
		fittingModel.selections.getFittingParameters().setFWMHBase(base);
		fittingModel.proposals.getFittingParameters().setFWMHBase(base);
		fittingDataInvalidated();
		setUndoPoint("Change Peak Shape");
	}

	public void setFittingFunction(Class<? extends FittingFunction> cls) {
		fittingModel.selections.getFittingParameters().setFittingFunction(cls);
		fittingModel.proposals.getFittingParameters().setFittingFunction(cls);
		fittingDataInvalidated();
		setUndoPoint("Change Peak Shape");
	}
	
	public Class<? extends FittingFunction> getFittingFunction() {
		return fittingModel.selections.getFittingParameters().getFittingFunction();
	}
	
	public CurveFitter getCurveFitter() {
		return fittingModel.curveFitter;
	}
	
	
	public void setCurveFitter(CurveFitter curveFitter) {
		this.fittingModel.curveFitter = curveFitter;
		fittingDataInvalidated();
	}



	public FittingSolver getFittingSolver() {
		return this.fittingModel.fittingSolver;
	}
	
	public void setFittingSolver(FittingSolver fittingSolver) {
		this.fittingModel.fittingSolver = fittingSolver;
		fittingDataInvalidated();
	}

	public ExecutorSet<List<TransitionSeries>> autodetectPeaks() {
		DerivativePeakSearcher searcher = new DerivativePeakSearcher();
		ReadOnlySpectrum data = plot.filtering().getFilteredPlot();
		ExecutorSet<List<TransitionSeries>> exec = PeakProposal.search(
				data, 
				searcher, 
				getFittingSelections(), 
				getCurveFitter(), 
				getFittingSolver()
			);
		

		Mutable<Boolean> ran = new Mutable<>(false);
		exec.addListener(() -> {
			if (!exec.getCompleted()) return;
			if (ran.get()) return;
			ran.set(true);
			for (TransitionSeries ts : exec.getResult()) {
				getFittingSelections().addTransitionSeries(ts);
			}
			fittingDataInvalidated();
		});
		
		
		return exec;

		
	}

	public boolean hasAnnotation(TransitionSeries ts) {
		if (!fittingModel.annotations.containsKey(ts)) {
			return false;
		}
		String annotation = getAnnotation(ts);
		if (annotation == null || annotation.trim().length() == 0) {
			return false;
		}
		return true;
	}
	
	public String getAnnotation(TransitionSeries ts) {
		return fittingModel.annotations.get(ts);
	}
	
	public void setAnnotation(TransitionSeries ts, String annotation) {
		if (annotation.trim().length() == 0) {
			fittingModel.annotations.remove(ts);
		} else {
			fittingModel.annotations.put(ts, annotation);
		}
		updateListeners(false);
	}

	public Map<TransitionSeries, String> getAnnotations() {
		return new HashMap<>(fittingModel.annotations);
	}

	public void clearAnnotations() {
		fittingModel.annotations.clear();
		updateListeners(false);
	}

	
	
	
}
