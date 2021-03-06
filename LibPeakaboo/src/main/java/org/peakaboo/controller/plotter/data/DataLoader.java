package org.peakaboo.controller.plotter.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.peakaboo.common.PeakabooLog;
import org.peakaboo.common.Version;
import org.peakaboo.controller.plotter.PlotController;
import org.peakaboo.controller.plotter.SavedSession;
import org.peakaboo.dataset.DatasetReadResult;
import org.peakaboo.dataset.DatasetReadResult.ReadStatus;
import org.peakaboo.datasource.model.datafile.DataFile;
import org.peakaboo.datasource.plugin.DataSourceLookup;
import org.peakaboo.datasource.plugin.DataSourcePlugin;
import org.peakaboo.datasource.plugin.DataSourcePluginManager;
import org.peakaboo.framework.autodialog.model.Group;
import org.peakaboo.framework.bolt.plugin.core.AlphaNumericComparitor;
import org.peakaboo.framework.cyclops.util.StringInput;
import org.peakaboo.framework.plural.executor.ExecutorSet;




public abstract class DataLoader {

	private PlotController controller;
	private List<DataFile> datafiles;
	private String dataSourceUUID = null;
	private List<Object> sessionParameters = null;
	private File sessionFile = null;
	
	//if we're loading a session, we need to do some extra work after loading the dataset
	private Runnable sessionCallback = () -> {}; 
	
	public DataLoader(PlotController controller, List<DataFile> datafiles) {
		this.controller = controller;
		this.datafiles = datafiles;
	}
	
	private void loadWithDataSource(DataSourcePlugin dsp) {
		
		if (datafiles != null) {
			
			ExecutorSet<DatasetReadResult> reading = controller.data().asyncReadFileListAsDataset(datafiles, dsp, result -> {
					
				if (result == null || result.status == ReadStatus.FAILED) {
					onDataSourceLoadFailure(dsp, result);					
				} else {
					onDataSourceLoadSuccess();
				}						
							
			});

			onLoading(reading);
			reading.startWorking();

		}
	}
	
	private void onDataSourceLoadSuccess() {
		sessionCallback.run();
		controller.data().setDataSourceParameters(sessionParameters);
		controller.data().setDataPaths(datafiles);
		
		//Try and set the last-used folder for local UIs to refer to
		//First, check the first data file for its folder
		Optional<File> localDir = datafiles.get(0).localFolder();
		if (localDir.isPresent()) {
			controller.io().setLastFolder(localDir.get());
		} else if (sessionFile != null) {
			controller.io().setLastFolder(sessionFile.getParentFile());
		}
		onSuccess(datafiles, sessionFile);
	}

	private void onDataSourceLoadFailure(DataSourcePlugin dsp, DatasetReadResult result) {
		String message = "\nSource: " + dsp.getFileFormat().getFormatName();
		
		if (result != null && result.message != null) {
			message += "\nMessage: " + result.message;
		}
		if (result != null && result.problem != null) {
			message += "\nProblem: " + result.problem;
		}
		
		if (result != null) {
			PeakabooLog.get().log(Level.WARNING, "Error Opening Data", new RuntimeException(result.message, result.problem));
		} else {
			PeakabooLog.get().log(Level.WARNING, "Error Opening Data", new RuntimeException("Dataset Read Result was null"));
		}
		onFail(datafiles, message);
	}
	
	

	public void load() {
		if (datafiles.isEmpty()) {
			return;
		}
		
		//check if it's a peakaboo session file first
		if (
				datafiles.size() == 1 && 
				datafiles.get(0).addressable() && 
				datafiles.get(0).getFilename().toLowerCase().endsWith(".peakaboo")
			) 
		{
			loadSession();
			return;
		}
		
		/*
		 * look up the data source to use to open this data with we should prefer a
		 * plugin specified by uuid (eg from a reloaded session). If there is no plugin
		 * specified, we look up all formats
		 */
		List<DataSourcePlugin> formats = new ArrayList<>();
		if (dataSourceUUID != null) {
			formats.add(DataSourcePluginManager.system().getByUUID(dataSourceUUID).create());
		}
		if (formats.isEmpty()) {
			List<DataSourcePlugin> candidates =  DataSourcePluginManager.system().newInstances();
			formats = DataSourceLookup.findDataSourcesForFiles(datafiles, candidates);
		}
		
		if (formats.size() > 1) {
			onSelection(formats, this::prompt);
		} else if (formats.isEmpty()) {
			onFail(datafiles, "Could not determine the data format of the selected file(s)");
		} else {
			prompt(formats.get(0));
		}
		
	}
	
	private void prompt(DataSourcePlugin dsp) {
		Optional<Group> parameters = dsp.getParametersForDataFile(datafiles);
		
		if (parameters.isPresent()) {
			Group dsGroup = parameters.get();
			
			/*
			 * if we've alredy loaded a set of parameters from a session we're opening then
			 * we transfer those values into the values for the data source's Parameters
			 */
			if (sessionParameters != null) {
				try {
					dsGroup.deserialize(sessionParameters);
				} catch (RuntimeException e) {
					PeakabooLog.get().log(Level.WARNING, "Failed to load saved Data Source parameters", e);
				}
			}
			
			onParameters(dsGroup, accepted -> {
				if (accepted) {
					//user accepted, save a copy of the new parameters
					sessionParameters = dsGroup.serialize();
					loadWithDataSource(dsp);
				}
			});
		} else {
			loadWithDataSource(dsp);
		}
	}
	
	

	private void loadSession() {
		try {
			//We don't want users saving a session loaded from /tmp
			if (!datafiles.get(0).writable()) {		
				//TODO: is writable the right thing to ask here? local vs non-local maybe?
				//TODO: maybe in later versions, the UI can inspect this when determining if it can save instead of save-as
				throw new IOException("Cannot load session from read-only source");
			}
			sessionFile = datafiles.get(0).getAndEnsurePath().toFile();
			
			Optional<SavedSession> optSession = controller.readSavedSettings(StringInput.contents(sessionFile));
			
			if (!optSession.isPresent()) {
				onSessionFailure();
				return;
			}
			
			SavedSession session = optSession.get();
			
			
			//chech if the session is from a newer version of Peakaboo, and warn if it is
			Runnable warnVersion = () -> {
				if (AlphaNumericComparitor.compareVersions(Version.longVersionNo, session.version) < 0) {
					onSessionNewer();
				}
			};
			
			List<DataFile> currentPaths = controller.data().getDataPaths();
			List<DataFile> sessionPaths = session.data.filesAsDataPaths();
			
			//Verify all paths exist
			boolean sessionPathsExist = true;
			for (DataFile d : sessionPaths) {
				if (d == null) {
					sessionPathsExist = false;
					break;
				}
				sessionPathsExist &= d.exists();
			}
			
			//If the data files in the saved session are different, offer to load the data set from the new session
			if (sessionPathsExist && !sessionPaths.isEmpty() && !sessionPaths.equals(currentPaths)) {
				
				onSessionHasData(sessionFile, load -> {
					if (load) {
						//they said yes, load the new data, and then apply the session
						//this needs to be done this way b/c loading a new dataset wipes out
						//things like calibration info
						this.datafiles = sessionPaths;
						this.dataSourceUUID = session.data.dataSourcePluginUUID;
						this.sessionParameters = session.data.dataSourceParameters;
						sessionCallback = () -> {
							controller.loadSessionSettings(session, true);	
							warnVersion.run();
						};
						load();
					} else {
						//load the settings w/o the data, then set the file paths back to the current values
						controller.loadSessionSettings(session, true);
						//they said no, reset the stored paths to the old ones
						controller.data().setDataPaths(currentPaths);
						warnVersion.run();
					}
					controller.io().setSessionFile(sessionFile);
				});
				
								
			} else {
				//just load the session, as there is either no data associated with it, or it's the same data
				controller.loadSessionSettings(session, true);
				warnVersion.run();
				controller.io().setSessionFile(sessionFile);
			}
			

			
		} catch (IOException e) {
			PeakabooLog.get().log(Level.SEVERE, "Failed to load session", e);
		}
	}

	public abstract void onLoading(ExecutorSet<DatasetReadResult> job);
	public abstract void onSuccess(List<DataFile> paths, File session);
	public abstract void onFail(List<DataFile> paths, String message);
	public abstract void onParameters(Group parameters, Consumer<Boolean> finished);
	public abstract void onSelection(List<DataSourcePlugin> datasources, Consumer<DataSourcePlugin> selected);
	
	public abstract void onSessionNewer();
	public abstract void onSessionFailure();
	public abstract void onSessionHasData(File sessionFile, Consumer<Boolean> load);
	
}
