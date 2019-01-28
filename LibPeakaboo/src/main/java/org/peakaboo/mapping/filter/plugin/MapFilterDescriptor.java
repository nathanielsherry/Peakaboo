package org.peakaboo.mapping.filter.plugin;

public class MapFilterDescriptor {

	public static MapFilterDescriptor SIZING = new MapFilterDescriptor("Sizing", "Sized");
	public static MapFilterDescriptor SMOOTHING = new MapFilterDescriptor("Smoothing", "Smoothed");
	public static MapFilterDescriptor CLIPPING = new MapFilterDescriptor("Clipping", "Clipped");
	public static MapFilterDescriptor MATH = new MapFilterDescriptor("Mathematical", "Filtered");
	public static MapFilterDescriptor TRANSFORMING = new MapFilterDescriptor("Transforming", "Transformed");
	
	private String group, action;
	
	public MapFilterDescriptor(String group, String action) {
		this.group = group;
		this.action = action;
	}

	public String getGroup() {
		return group;
	}

	public String getAction() {
		return action;
	}
	
	
	
}