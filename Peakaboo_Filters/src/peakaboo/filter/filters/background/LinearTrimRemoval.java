package peakaboo.filter.filters.background;



import peakaboo.calculations.Background;
import peakaboo.filter.BackgroundRemovalFilter;
import peakaboo.filter.Parameter;
import peakaboo.filter.Parameter.ValueType;
import scitypes.Spectrum;
import scitypes.SpectrumCalculations;

/**
 * 
 * This class is a filter exposing the Parabolic Background Removal functionality elsewhere in this programme.
 * 
 * @author Nathaniel Sherry, 2009
 */

public final class LinearTrimRemoval extends BackgroundRemovalFilter
{

	private final int	WIDTH = 1;
	private final int	ITERATIONS = 0;


	public LinearTrimRemoval()
	{
		super();
	}

	@Override
	public void initialize()
	{
		addParameter(WIDTH, new Parameter(ValueType.INTEGER, "Width of Fitting", 100));
		addParameter(ITERATIONS, new Parameter(ValueType.INTEGER, "Iterations", 2));
		
	}

	@Override
	public String getFilterName()
	{
		return "Linear Trim";
	}


	@Override
	protected Spectrum getBackground(Spectrum data, int percent)
	{
		int width = getParameter(WIDTH).intValue();
		int iterations = getParameter(ITERATIONS).intValue();
		
		return SpectrumCalculations.multiplyBy(  Background.calcBackgroundLinearTrim(data, width, iterations), (percent/100.0f));		
	}
	

	@Override
	public boolean validateCustomParameters()
	{
		int width, iterations;

		// parabolas which are too wide are useless, but ones that are too
		// narrow remove good data
		width = getParameter(WIDTH).intValue();
		iterations = getParameter(ITERATIONS).intValue();
		
		
		if (width > 400 || width < 10) return false;
		if (iterations > 20 || iterations <= 0) return false;

		return true;
	}


	@Override
	public String getFilterDescription()
	{
		return "The "
				+ getFilterName()
				+ " filter attempts to determine which portion of the signal is background and remove it. It does this by examining all pairs of points which are n channels apart (ie (1, 10), (2, 11) where n = 10). For each pair of points, any signal which exceeds a straight line connecting the two points is removed.";
	}


	@Override
	public boolean showFilter()
	{
		return true;
	}
	
	
	@Override
	public boolean canFilterSubset()
	{
		return true;
	}

}