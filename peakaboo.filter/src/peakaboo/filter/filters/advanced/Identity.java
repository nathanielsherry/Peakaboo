package peakaboo.filter.filters.advanced;

import peakaboo.filter.filters.AbstractSimpleFilter;
import scitypes.Spectrum;

public class Identity extends AbstractSimpleFilter
{

	@Override
	public boolean canFilterSubset()
	{
		return true;
	}


	@Override
	protected Spectrum filterApplyTo(Spectrum data)
	{
		return data;
	}


	@Override
	public String getFilterDescription()
	{
		return "This filter is the identity function -- it does no processing to the data";
	}


	@Override
	public String getFilterName()
	{
		return "None";
	}


	@Override
	public FilterType getFilterType()
	{
		return FilterType.ADVANCED;
	}


	@Override
	public void initialize()
	{

	}


	@Override
	public boolean pluginEnabled()
	{
		return false;
	}


	@Override
	public boolean validateParameters()
	{
		return true;
	}

}