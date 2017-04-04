package peakaboo.filter.plugins.mathematical;



import autodialog.model.Parameter;
import autodialog.view.editors.DoubleEditor;
import peakaboo.filter.model.AbstractSimpleFilter;
import peakaboo.filter.model.Filter;
import scitypes.Spectrum;
import scitypes.SpectrumCalculations;


public class Multiply extends AbstractSimpleFilter
{

	private Parameter<Double> amount;
	
	@Override
	public void initialize()
	{
		amount = new Parameter<>("Multiply By", new DoubleEditor(), 1.0);
		addParameter(amount);
		
	}
	
	@Override
	protected Spectrum filterApplyTo(Spectrum data)
	{
		return SpectrumCalculations.multiplyBy(data, amount.getValue().floatValue());
	}


	@Override
	public String getFilterDescription()
	{
		return "The " + getFilterName() + " filter multiplies all points on a spectrum by a constant value.";
	}


	@Override
	public String getFilterName()
	{
		// TODO Auto-generated method stub
		return "Multiply";
	}


	@Override
	public Filter.FilterType getFilterType()
	{
		// TODO Auto-generated method stub
		return Filter.FilterType.MATHEMATICAL;
	}


	@Override
	public boolean validateParameters()
	{
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean pluginEnabled()
	{
		return true;
	}
	
	
	@Override
	public boolean canFilterSubset()
	{
		return true;
	}

}
