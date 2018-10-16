package peakaboo.ui.swing.calibration.referenceplot;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cyclops.Bounds;
import cyclops.Coord;
import cyclops.ISpectrum;
import cyclops.Spectrum;
import cyclops.visualization.Surface;
import cyclops.visualization.backend.awt.GraphicsPanel;
import cyclops.visualization.drawing.DrawingRequest;
import cyclops.visualization.drawing.ViewTransform;
import cyclops.visualization.drawing.painters.axis.AxisPainter;
import cyclops.visualization.drawing.painters.axis.LineAxisPainter;
import cyclops.visualization.drawing.painters.axis.TitleAxisPainter;
import cyclops.visualization.drawing.plot.PlotDrawing;
import cyclops.visualization.drawing.plot.painters.PlotPainter;
import cyclops.visualization.drawing.plot.painters.axis.GridlinePainter;
import cyclops.visualization.drawing.plot.painters.axis.TickMarkAxisPainter;
import cyclops.visualization.drawing.plot.painters.axis.TickMarkAxisPainter.TickFormatter;
import cyclops.visualization.drawing.plot.painters.plot.AreaPainter;
import cyclops.visualization.palette.PaletteColour;
import peakaboo.curvefit.peak.table.Element;
import peakaboo.curvefit.peak.transition.TransitionSeries;
import peakaboo.curvefit.peak.transition.TransitionSeriesType;
import peakaboo.mapping.calibration.CalibrationReference;

public class ReferencePlot extends GraphicsPanel {

	private PlotDrawing plotDrawing;
	private Spectrum data;
	private DrawingRequest dr = new DrawingRequest();
	private List<PlotPainter> plotPainters = new ArrayList<>();
	private List<AxisPainter> axisPainters = new ArrayList<>();
	
	public ReferencePlot(CalibrationReference reference, TransitionSeriesType type) {
		
		int lowest = 0;
		int highest = 1;
		
		List<TransitionSeries> tss = reference.getTransitionSeries(type);
		if (tss.size() >= 2) {
			lowest = tss.get(0).element.ordinal();
			highest = tss.get(tss.size() - 1).element.ordinal();
		}
		
		data = profileToSpectrum(reference.getConcentrations(), type, lowest, highest);
		
		dr.dataHeight = 1;
		dr.dataWidth = data.size();
		dr.drawToVectorSurface = false;
		dr.maxYIntensity = data.max();
		dr.unitSize = 1f;
		dr.viewTransform = ViewTransform.LINEAR;
		
		plotPainters.add(new GridlinePainter(new Bounds<Float>(0f, data.max()*100f)));
		
		plotPainters.add(new AreaPainter(data, 
				new PaletteColour(0xff00897B), 
				new PaletteColour(0xff00796B), 
				new PaletteColour(0xff004D40)
			));
		
	
		axisPainters.add(new TitleAxisPainter(TitleAxisPainter.SCALE_TEXT, "Concentration", null, null, "Element"));
		NumberFormat formatter = new DecimalFormat("0.0E0");
		Function<Integer, String> sensitivityFormatter = i -> formatter.format(i);
		axisPainters.add(new TickMarkAxisPainter(
				new TickFormatter(0f, data.max()*100f, sensitivityFormatter), 
				new TickFormatter((float)lowest, (float)highest, i -> {  
					Element element = Element.values()[i];
					return element.name();
				}), 
				null, 
				new TickFormatter(0f, data.max()*100f, sensitivityFormatter),
				false, 
				false));
		axisPainters.add(new LineAxisPainter(true, true, false, true));
		
	}
	
	@Override
	protected void drawGraphics(Surface backend, Coord<Integer> size) {
	
		backend.setSource(new PaletteColour(0xffffffff));
		backend.rectAt(0, 0, getWidth(), getHeight());
		backend.fill();
		
		dr.imageHeight = getHeight();
		dr.imageWidth = getWidth();
		plotDrawing = new PlotDrawing(backend, dr, plotPainters, axisPainters);	
		plotDrawing.draw();
		
	}

	@Override
	public float getUsedWidth() {
		return getWidth();
	}

	@Override
	public float getUsedWidth(float zoom) {
		return getWidth();
	}

	@Override
	public float getUsedHeight() {
		return getHeight();
	}

	@Override
	public float getUsedHeight(float zoom) {
		return getHeight();
	}


	public static Spectrum profileToSpectrum(Map<TransitionSeries, Float> values, TransitionSeriesType tst, int startOrdinal, int stopOrdinal) {	
		
		Spectrum spectrum = new ISpectrum(stopOrdinal - startOrdinal + 1);
		float value = 0;
		for (int ordinal = startOrdinal; ordinal <= stopOrdinal; ordinal++) {
			TransitionSeries ts = new TransitionSeries(Element.values()[ordinal], tst);
			if (ts != null && values.containsKey(ts)) {
				value = values.get(ts);
			} else {
				//use last value
			}
			int index = ordinal;
			spectrum.set(index - startOrdinal, value);
		}
		
		return spectrum;
	}
	
}
