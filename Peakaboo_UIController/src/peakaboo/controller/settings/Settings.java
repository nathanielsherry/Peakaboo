package peakaboo.controller.settings;



import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.ho.yaml.Yaml;

import fava.*;
import fava.datatypes.Pair;
import fava.signatures.FunctionEach;
import fava.signatures.FunctionMap;
import static fava.Fn.*;
import static fava.Functions.*;

import peakaboo.controller.plotter.PlotModel;
import peakaboo.datatypes.DataTypeFactory;
import peakaboo.datatypes.peaktable.Element;
import peakaboo.datatypes.peaktable.TransitionSeries;
import peakaboo.datatypes.peaktable.TransitionSeriesType;
import peakaboo.filters.AbstractFilter;



/**
 * This class is responsible for (de)serialization of preferences. A given data set will have things such as user view
 * preferences, curve fittings, filters, etc.
 * 
 * @author Nathaniel Sherry, 2009
 */

public class Settings
{

	/**
	 * loads preferences from a file, and applies them to the given model
	 * 
	 * @param model
	 *            model to apply the loaded preferences to
	 * @param filename
	 *            name of the preferences file
	 */
	public static void loadPreferences(final PlotModel model, InputStream inStream)
	{

		FileInputStream fin = null;
		//ObjectInputStream in = null;

		Object read;

		SerializedData data = new SerializedData();

		try
		{
			/*in = new ObjectInputStream(inStream);

			read = in.readObject();*/

			data = Yaml.loadType(inStream, SerializedData.class);

			//if (read == null) return;
			//data = (SerializedData) read;

			// load transition series
			model.fittingSelections.clear();

			//for each list of element/transitionseriestype string representation pairs
			//convert that list into a single composited element (or primary if it is of length 1
			//and add the result to the fittings set
			each(data.fittings, new FunctionEach<List<Pair<String, String>>>() {

				
				public void f(List<Pair<String, String>> tspairs)
				{

					List<TransitionSeries> tss = map(
							tspairs,
							new FunctionMap<Pair<String, String>, TransitionSeries>() {

								
								public TransitionSeries f(Pair<String, String> pair)
								{
									return model.peakTable.getTransitionSeries(
											Element.valueOf(pair.first),
											TransitionSeriesType.valueOf(pair.second));
								}
							});

					model.fittingSelections.addTransitionSeries(TransitionSeries.summation(tss));
				}
			});

			/*for (TransitionSeries ts : data.fittings)
			{
				model.fittingSelections.addTransitionSeries(ts);
			}*/

			// load filters
			model.filters.clearFilters();
			for (AbstractFilter f : data.filters)
			{
				model.filters.addFilter(f);
			}

			// read in the drawing request
			model.dr = data.drawingRequest;
			model.viewOptions = data.viewOptions;


		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		return;
	}


	/**
	 * Saves preferences to a file, as read from the given model
	 * 
	 * @param model
	 *            model to read the saved preferences from
	 * @param filename
	 *            name of the preferences file
	 */
	public static void savePreferences(PlotModel model, OutputStream outStream)
	{

		//FileOutputStream fos = null;
		//ObjectOutputStream out = null;

		SerializedData data = new SerializedData();

		List<TransitionSeries> fittedTSs = model.fittingSelections.getFittedTransitionSeries();

		data.fittings =

		map(fittedTSs, new FunctionMap<TransitionSeries, List<Pair<String, String>>>() {

			
			public List<Pair<String, String>> f(TransitionSeries ts)
			{
				return map(
						ts.getBaseTransitionSeries(),
						new FunctionMap<TransitionSeries, Pair<String, String>>() {

							
							public Pair<String, String> f(TransitionSeries ts)
							{
								return ts.toSerializablePair();
							}
						});
			}
		});

		data.filters = DataTypeFactory.<AbstractFilter> list();
		for (AbstractFilter f : model.filters)
		{
			data.filters.add(f);
		}

		data.drawingRequest = model.dr;
		data.viewOptions = model.viewOptions;


		try
		{
			/*out = new ObjectOutputStream(outStream);

			// Write out the SerializedData object
			out.writeObject(data);

			out.close();*/

			OutputStreamWriter osw = new OutputStreamWriter(outStream);
			osw.write(data.toYaml());
			osw.close();

		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}






	}
}
