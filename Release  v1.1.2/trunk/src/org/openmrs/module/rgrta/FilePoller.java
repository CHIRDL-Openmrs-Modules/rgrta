/**
 * 
 */
package org.openmrs.module.rgrta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicService;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.atd.datasource.TeleformExportXMLDatasource;
import org.openmrs.module.atd.hibernateBeans.FormAttributeValue;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.State;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.chirdlutil.util.IOUtil;
import org.openmrs.module.chirdlutil.util.Util;
import org.openmrs.module.dss.service.DssService;
import org.openmrs.module.sockethl7listener.SimpleServer;

import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.openmrs.util.OpenmrsClassLoader;

/**
 * @author Vibha Anand
 * 
 */

public class FilePoller extends AbstractTask
{
	private Log log = LogFactory.getLog(this.getClass());

	
	private static TaskDefinition taskConfig;
	private PollManager pollManager = null;
	
	@Override
	public void initialize(TaskDefinition config)
	{
		taskConfig = config;
		//unparseableFiles = new HashSet<String>();
		init();
		isInitialized = false;
		
	}
	@Override
	public void execute() 
	{
		Context.openSession();

		try
		{
			processFiles();
			
		} catch (Exception e)
		{
            this.log.error(e.getMessage());
            this.log.error(Util.getStackTrace(e));
		} finally 
		{
			Context.closeSession();
		}	
	}

	
	
	public  void init()
	{
		log.info("Initializing file polling...");

		try
		{
			String sourceDirectory = taskConfig.getProperty("sourceDirectory");
			String destinationDirectory = taskConfig.getProperty("destinationDirectory");
			String archiveDirectory = taskConfig.getProperty("archiveDirectory");
			String destinationExtension = taskConfig.getProperty("destinationExtension");
			String filterExtension = taskConfig.getProperty("filterExtension");
			String archiveExtension = taskConfig.getProperty("archiveExtension");
	
			this.pollManager = new PollManager(sourceDirectory, archiveDirectory,
					destinationDirectory, destinationExtension, filterExtension, archiveExtension);
			
			
			
		} catch (Exception e)
		{
			log.error(e.getMessage());
			log.error(Util.getStackTrace(e));
		}
		isInitialized = true;
		log.info("Finished initializing Teleform monitor.");
	}
	
	@Override
	public void shutdown()
	{
		super.shutdown();
	}
	
	private void renameFile(String filename, String extension){
		if (pollManager.getSourceDirectory() != null) {	
			try {
				String sourceDirName = IOUtil.getDirectoryName(filename);
				String fileNameNoExt =IOUtil.getFilenameWithoutExtension(filename);
				if (extension != null){
					if (!extension.startsWith(".")){
						extension = "." + extension;
					}
				}else {
					extension = "";
				}
				String newFilename = sourceDirName + "\\" + fileNameNoExt + extension;
				IOUtil.renameFile(filename,  newFilename);
			} catch (Exception e) {
				log.error(e.getMessage());
				log.error(Util.getStackTrace(e));
			}
		}
		return;
		
	}
		
	private void copyFile(String filename){
		if (pollManager.getDestinationDirectory() != null){
			try {
					
				String fileNameNoExt =IOUtil.getFilenameWithoutExtension(filename);
				String sourceDirName = IOUtil.getDirectoryName(filename);
				
				
				String destinationExt = pollManager.getDestinationExtension();
				
				if (destinationExt != null){
					if (!destinationExt.startsWith(".")){
						destinationExt = "." + destinationExt;
					}
				}else {
					destinationExt = "";
				}
				String newFilename = fileNameNoExt + destinationExt;
				
				
				String targetLocation = pollManager.getDestinationDirectory()
					+ File.separator + new File(newFilename).getName();
				IOUtil.copyFile(sourceDirName + "\\" +  newFilename,
							targetLocation);	
			} catch (Exception e) {
				log.error(e.getMessage());
				log.error(Util.getStackTrace(e));
			}
		}
		
		return;
			
		}
		
		
		
		
		
		public void processFiles()
		{
			HashSet<String> faxFiles = new HashSet<String>();
			HashSet<String> tiffFiles = new HashSet<String>();
			//look for files
			
			if (pollManager == null) {
				return;
			}
			this.lookForFiles(pollManager.getSourceDirectory(), faxFiles, pollManager.getFilterExtension());
			
			for (String filename : faxFiles){
				try{
					String destinationExt = pollManager.getDestinationExtension();
					renameFile(filename, destinationExt);
				}catch (Exception e){
					log.error(e.getMessage());
					log.error(Util.getStackTrace(e));
				}
			}
			
			this.lookForFiles(pollManager.getSourceDirectory(), tiffFiles, pollManager.getDestinationExtension());
			
			for (String filename : tiffFiles)
			{
				try
				{
					String archiveExt = pollManager.getArchiveExtension();
					copyFile(filename);
					renameFile(filename, archiveExt);
					
				} catch (Exception e)
				{
					log.error(e.getMessage());
					log.error(Util.getStackTrace(e));
				}
			}
		}
		
		private void lookForFiles(String directoryName, HashSet<String> files, String ext)
		{
			//String[] fileExtensions = new String[]{ext};
			 String[] fileExtensions = null;
			 if (ext != null && !ext.equalsIgnoreCase("")){
			    fileExtensions = ext.split(",");
			 }
			if (directoryName != null) {
				
				File[] filesInDirectory = IOUtil.getFilesInDirectory(directoryName, fileExtensions);
				if(filesInDirectory == null){
					return;
				}
				int length = filesInDirectory.length;
				String currFile = null;
	
				for (int i = 0; i < length; i++)
				{
					currFile = filesInDirectory[i].getPath();
					if(currFile != null && currFile.length()>0)
					{
						files.add(currFile);
					}
				}
			}
		}

}
