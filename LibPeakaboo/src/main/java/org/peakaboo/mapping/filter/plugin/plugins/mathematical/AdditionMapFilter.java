package org.peakaboo.mapping.filter.plugin.plugins.mathematical;

import org.peakaboo.mapping.filter.model.AreaMap;
import org.peakaboo.mapping.filter.plugin.MapFilterDescriptor;
import org.peakaboo.mapping.filter.plugin.plugins.AbstractMapFilter;

import cyclops.SpectrumCalculations;
import net.sciencestudio.autodialog.model.Parameter;
import net.sciencestudio.autodialog.model.style.editors.RealSpinnerStyle;

public class AdditionMapFilter extends AbstractMapFilter {

	Parameter<Float> added;
	
	@Override
	public String getFilterName() {
		return "Addition";
	}

	@Override
	public String getFilterDescription() {
		return "Adds the given amount to each map";
	}

	@Override
	public MapFilterDescriptor getFilterDescriptor() {
		return MapFilterDescriptor.MATH;
	}

	@Override
	public void initialize() {
		added = new Parameter<>("Amount Added", new RealSpinnerStyle(), 0f, this::validate);
		addParameter(added);
	}

	private boolean validate(Parameter<?> param) {
		return true;
	}
	
	@Override
	public AreaMap filter(AreaMap map) {
		return new AreaMap(SpectrumCalculations.addToList(map.getData(), added.getValue()), map.getSize(), map.getRealDimensions());
	}

	@Override
	public boolean pluginEnabled() {
		return true;
	}

	@Override
	public String pluginVersion() {
		return "1.0";
	}

	@Override
	public String pluginUUID() {
		return "0044766b-d091-42a2-9833-480672a81ee0";
	}

}