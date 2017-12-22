package peakaboo.filter.plugin.plugins.noise;



import autodialog.model.Parameter;
import autodialog.model.style.editors.IntegerStyle;
import peakaboo.calculations.Noise;
import peakaboo.filter.model.AbstractSimpleFilter;
import peakaboo.filter.model.FilterType;
import scitypes.ReadOnlySpectrum;
import scitypes.Spectrum;



/**
 * This class is a filter exposing the Wavelet Noise Filter functionality elsewhere in this programme.
 * 
 * @author Nathaniel Sherry, 2009
 */


public final class WaveletNoiseFilter extends AbstractSimpleFilter
{

	private Parameter<Integer> passes;


	public WaveletNoiseFilter()
	{
		super();
	}
	
	@Override
	public String pluginVersion() {
		return "1.0";
	}

	@Override
	public void initialize()
	{
		passes = new Parameter<>("Passes to Transform", new IntegerStyle(), 1, this::validate);
		addParameter(passes);
	}

	@Override
	public String getFilterName()
	{
		return "Wavelet Low-Pass";
	}


	@Override
	public FilterType getFilterType()
	{
		return FilterType.NOISE;
	}


	private boolean validate(Parameter<?> p)
	{
		int passCount;

		// remove largest, least significant passes from the wavelet transform
		// data
		// probably a bad idea to do more than 3 passes, but less than 1 is
		// senseless
		passCount = passes.getValue();
		if (passCount > 8 || passCount < 1) return false;

		return true;
	}


	@Override
	public String getFilterDescription()
	{
		return "The "
				+ getFilterName()
				+ " filter attempts to reduce high-frequency noise by performing a Wavelet transformation on the spectrum. This breaks the data down into sections each representing a different frequency range. The high-frequency regions are then smoothed, and a reverse transform is applied.";
	}


	@Override
	protected ReadOnlySpectrum filterApplyTo(ReadOnlySpectrum data)
	{
		Spectrum result;
		int passCount= passes.getValue();

		result = Noise.FWTLowPassFilter(data, passCount);

		return result;
	}
	
	@Override
	public boolean pluginEnabled()
	{
		return true;
	}


	@Override
	public boolean canFilterSubset()
	{
		return false;
	}


}