package org.peakaboo.controller.mapper.rawdata;

import java.util.ArrayList;
import java.util.List;

import org.peakaboo.calibration.CalibrationProfile;
import org.peakaboo.controller.mapper.MapUpdateType;
import org.peakaboo.dataset.DataSet;
import org.peakaboo.framework.cyclops.Bounds;
import org.peakaboo.framework.cyclops.Coord;
import org.peakaboo.framework.cyclops.SISize;
import org.peakaboo.framework.eventful.EventfulType;
import org.peakaboo.mapping.rawmap.RawMapSet;


/**
 * The RawDataController represents the data originally fed into the map
 * component from the plot after mapping has occurred and a {@link RawMapSet}
 * has been generated.
 */
public class RawDataController extends EventfulType<MapUpdateType> {

	RawDataModel mapModel;
		
	public RawDataController() {
		mapModel = new RawDataModel();
	}
	


	/**
	 * Sets the map's data model. dataDimensions, realDimensions, realDimensionsUnits may be null
	 * @param calibrationProfile 
	 */
	public void setMapData(
			RawMapSet data,
			DataSet sourceDataset,
			String datasetName,
			List<Integer> badPoints,
			Coord<Integer> dataDimensions,
			Coord<Bounds<Number>> realDimensions,
			SISize realDimensionsUnits, 
			CalibrationProfile calibrationProfile		
	) {
	
		
		mapModel.mapResults = data;
		mapModel.sourceDataset = sourceDataset;
		mapModel.datasetTitle = datasetName;
		mapModel.badPoints = badPoints;
		
		mapModel.originalDimensions = dataDimensions;
		mapModel.originalDimensionsProvided = dataDimensions != null;
		mapModel.realDimensions = realDimensions;
		mapModel.realDimensionsUnits = realDimensionsUnits;
		mapModel.calibrationProfile = calibrationProfile;

		updateListeners(MapUpdateType.DATA);

	}
	
	public int getMapSize() {
		return mapModel.mapSize();
	}

	
	


	

	

	
	public Coord<Integer> getOriginalDataDimensions() {
		return mapModel.originalDimensions;
	}
	
	public boolean hasOriginalDataDimensions() {
		return mapModel.originalDimensionsProvided;
	}
	
	public int getOriginalDataHeight() {
		return mapModel.originalDimensions.y;
	}
	public int getOriginalDataWidth() {
		return mapModel.originalDimensions.x;
	}
	
	
	



	public List<Integer> getBadPoints() {
		return new ArrayList<>(mapModel.badPoints);
	}


	public String getDatasetTitle() {
		return mapModel.datasetTitle;
	}


	public void setDatasetTitle(String name) {
		mapModel.datasetTitle = name;
	}


	public Coord<Bounds<Number>> getRealDimensions() {
		return mapModel.realDimensions;
	}
	
	
	public SISize getRealDimensionUnits() {
		return mapModel.realDimensionsUnits;
	}
	

	public RawMapSet getMapResultSet() {
		return mapModel.mapResults;
	}

	public CalibrationProfile getCalibrationProfile() {
		return mapModel.calibrationProfile;
	}


	/**
	 * Indicates if the data points in this map can be reliably mapped back to the correct spectra
	 */
	public boolean isReplottable() {
		return mapModel.mapResults.isReplottable();
	}
		
}
