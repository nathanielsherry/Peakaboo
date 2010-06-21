package peakaboo.controller.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import fava.*;
import static fava.Fn.*;
import static fava.Functions.*;

import peakaboo.datatypes.DataTypeFactory;
import peakaboo.datatypes.Spectrum;
import peakaboo.datatypes.peaktable.TransitionSeries;
import peakaboo.mapping.MapResultSet;
import peakaboo.mapping.colours.OverlayColour;

public class SingleMapModel {

	//public List<List<Double>> summedVisibleMaps;
	public List<Pair<TransitionSeries, Spectrum>> resultantData;
	
	private MapResultSet mapResults;
	
	public Map<TransitionSeries, Integer> ratioSide;
	public Map<TransitionSeries, OverlayColour> overlayColour;
	public Map<TransitionSeries, Boolean> visible;
	
	public MapScaleMode mapScaleMode = MapScaleMode.VISIBLE_ELEMENTS;
	
	public MapDisplayMode displayMode;
	
	public SingleMapModel(MapResultSet originalData){
		
		resultantData = null;
		
		this.mapResults = originalData;
		
		displayMode = MapDisplayMode.COMPOSITE;
		
		ratioSide = DataTypeFactory.<TransitionSeries, Integer>map();
		overlayColour = DataTypeFactory.<TransitionSeries, OverlayColour>map();
		visible = DataTypeFactory.<TransitionSeries, Boolean>map();
		
		for (TransitionSeries ts : originalData.getAllTransitionSeries())
		{
			ratioSide.put(ts, 1);
			overlayColour.put(ts, OverlayColour.RED);
			visible.put(ts, true);
		}
		
	}

	
	public List<TransitionSeries> getAllTransitionSeries()
	{
		
		List<TransitionSeries> tsList = filter(visible.keySet(), Functions.<TransitionSeries>bTrue());
		
		Collections.sort(tsList);
		
		return tsList;
	}
	
	public List<TransitionSeries> getVisibleTransitionSeries()
	{
		return filter(getAllTransitionSeries(), new FunctionMap<TransitionSeries, Boolean>() {
			
			@Override
			public Boolean f(TransitionSeries element) {
				return visible.get(element);
			}
		});
	}
	
	public Spectrum sumGivenTransitionSeriesMaps(List<TransitionSeries> list)
	{
		return mapResults.sumGivenTransitionSeriesMaps(list);
	}
	
	public Spectrum getMapForTransitionSeries(TransitionSeries ts)
	{
		List<TransitionSeries> tss = DataTypeFactory.<TransitionSeries>list();
		tss.add(ts);
		return mapResults.sumGivenTransitionSeriesMaps(tss);
	}
	
	public Spectrum sumVisibleTransitionSeriesMaps()
	{	
		return mapResults.sumGivenTransitionSeriesMaps(getVisibleTransitionSeries());
	}
	
	public Spectrum sumAllTransitionSeriesMaps()
	{		
		return mapResults.sumGivenTransitionSeriesMaps(visible.keySet());
	}
	
	public String mapShortTitle(){ return getShortDatasetTitle(getVisibleTransitionSeries()); }
	
	public String mapLongTitle(){ 
	
		switch (displayMode)
		{
			case RATIO:
				String side1Title = mapLongTitle(getTransitionSeriesForRatioSide(1));

				String side2Title = mapLongTitle(getTransitionSeriesForRatioSide(2));

				return side1Title + " ∶ " + side2Title;
				
			default:
				
				return getDatasetTitle(getVisibleTransitionSeries());
				
		}
		
	}
	

	public String mapShortTitle(List<TransitionSeries> list){ return getShortDatasetTitle(list); }
	
	public String mapLongTitle(List<TransitionSeries> list){ return getDatasetTitle(list); }
	

	private String getDatasetTitle(List<TransitionSeries> list)
	{
		String separator = ", ";
		
		List<String> elementNames = map(list, new FunctionMap<TransitionSeries, String>() {
			
			public String f(TransitionSeries ts) {
				return ts.toElementString();
			}
		});

		String title = foldr(elementNames, strcat(", "));
		
		if (title == null) return "-";
		return title;
		
	}
	

	private String getShortDatasetTitle(List<TransitionSeries> list)
	{
		String separator = ", ";
		
		
		List<String> elementNames = map(list, new FunctionMap<TransitionSeries, String>() {
			
			public String f(TransitionSeries ts) {
				return ts.element.toString();
			}
		});
		
		//trim out the duplicated
		elementNames = unique(elementNames);

		String title = foldr(elementNames, strcat(", "));
		
		if (title == null) return "-";
		return title;
		
	}
	
	
	public List<Pair<TransitionSeries, Spectrum>> getTransitionSeriesForColour(final OverlayColour c)
	{
		return filter(
				resultantData,
				new FunctionMap<Pair<TransitionSeries, Spectrum>, Boolean>() {

					@Override
					public Boolean f(Pair<TransitionSeries, Spectrum> element)
					{
						return c.equals(overlayColour.get(element.first)) && visible.get(element.first);
					}
				});
	}
	
	public List<TransitionSeries> getTransitionSeriesForRatioSide(final int side)
	{
		return filter(
				getVisibleTransitionSeries(),
				new FunctionMap<TransitionSeries, Boolean>() {

					@Override
					public Boolean f(TransitionSeries element)
					{
						Integer thisSide = ratioSide.get(element);
						return thisSide == side;
					}
				});
	}
	
	public void discard()
	{
		resultantData.clear();
		ratioSide.clear();
		overlayColour.clear();
		visible.clear();
		
		mapResults = null;
	}
	
}
