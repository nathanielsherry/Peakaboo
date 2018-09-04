package peakaboo.filter.plugins.advanced;

import net.sciencestudio.autodialog.model.Parameter;
import net.sciencestudio.autodialog.model.SelectionParameter;
import net.sciencestudio.autodialog.model.style.editors.IntegerStyle;
import net.sciencestudio.autodialog.model.style.editors.ListStyle;
import net.sciencestudio.autodialog.model.style.editors.RealStyle;
import peakaboo.filter.model.AbstractSimpleFilter;
import peakaboo.filter.model.FilterType;
import scitypes.ISpectrum;
import scitypes.ReadOnlySpectrum;
import scitypes.SpectrumCalculations;


public class SpectrumNormalizationFilter extends AbstractSimpleFilter
{
	
	private Parameter<Integer> pStartChannel;
	private Parameter<Integer> pEndChannel;
	private Parameter<Float> pHeight;
	private SelectionParameter<String> pMode;
	
	private final String MODE_RANGE = "Channel Range";
	private final String MODE_MAX = "Strongest Channel";
	private final String MODE_SUM = "All Channels";

	@Override
	public String pluginVersion() {
		return "1.0";
	}
	
	@Override
	public void initialize()
	{
		pMode = new SelectionParameter<String>("Mode", new ListStyle<String>(), MODE_RANGE, this::validate);
		pMode.setPossibleValues(MODE_RANGE, MODE_MAX, MODE_SUM);
		addParameter(pMode);
		
		pStartChannel = new Parameter<>("Start Channel", new IntegerStyle(), 1, this::validate);
		addParameter(pStartChannel);
		
		pEndChannel = new Parameter<>("End Channel", new IntegerStyle(), 10, this::validate);
		addParameter(pEndChannel);
		
		pHeight = new Parameter<>("Normalized Intensity", new RealStyle(), 10f, this::validate);
		addParameter(pHeight);
	}
	
	@Override
	public boolean canFilterSubset()
	{
		return false;
	}

	@Override
	protected ReadOnlySpectrum filterApplyTo(ReadOnlySpectrum data)
	{

		String mode = pMode.getValue();
		int startChannel = pStartChannel.getValue()-1;
		int endChannel = pEndChannel.getValue()-1;
		float height = pHeight.getValue().floatValue();
		
		float value=0f;
		switch (mode) {
		case MODE_RANGE:
			if (startChannel >= data.size()) return data;
			if (endChannel <= 0) return data;
			int range = (endChannel - startChannel) + 1;
			value = data.subSpectrum(startChannel, endChannel).sum() / range;
			break;
		case MODE_MAX:
			value = data.max();
			break;
		case MODE_SUM:
			value = data.sum();
			break;
		}

		float ratio = value / height;
		if (ratio == 0f) return new ISpectrum(data.size());
		return SpectrumCalculations.divideBy(data, ratio);
		
		
		
	}

	@Override
	public String getFilterDescription()
	{
		return "The " + getFilterName() + " scales each spectrum's intensity based on the options selected";
	}

	@Override
	public String getFilterName()
	{
		return "Normalizer";
	}

	@Override
	public FilterType getFilterType()
	{
		return FilterType.ADVANCED;
	}


	@Override
	public boolean pluginEnabled()
	{
		return true;
	}

	private boolean validate(Parameter<?> p)
	{
		String mode = pMode.getValue();		
		int startChannel = pStartChannel.getValue();
		int endChannel = pEndChannel.getValue();
		float height = pHeight.getValue().floatValue();
				
		pStartChannel.setEnabled(mode == MODE_RANGE);
		pEndChannel.setEnabled(mode == MODE_RANGE);
		
		
		if (startChannel < 1) return false;
		if (endChannel < 1) return false;
		
		if (endChannel < startChannel) return false;
		
		if (height < 1) return false;
		if (height > 1000000) return false;
		
		return true;
	}

	@Override
	public String pluginUUID() {
		return "b9ec2709-e2d4-4700-9ac9-7d0f5b816f5f";
	}
	
}
