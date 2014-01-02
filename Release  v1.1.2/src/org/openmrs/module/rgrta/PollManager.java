package org.openmrs.module.rgrta;

import org.openmrs.api.PersonService;
import org.openmrs.scheduler.TaskDefinition;

public class PollManager
{

	
	protected PersonService personService;

	private  String sourceDirectory;
	private  String archiveDirectory;
	private  String destinationDirectory;
	private  String deleteSource;
	private  String destinationExtension;
	private  String filterExtension;
	private  String archiveExtension;
	
	public PollManager(String sourceDir, String archiveDir, 
			String destDir, String renameExtension, String filterExtension, 
			String archiveExtension)
	{
		this.sourceDirectory = sourceDir;
		this.archiveDirectory = archiveDir;
		this.destinationDirectory = destDir;
		this.filterExtension = filterExtension;
		this.archiveExtension = archiveExtension;
		this.destinationExtension = renameExtension;
	}

	public void setDirectory(String dir)
	{
		
		return;
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public String getArchiveDirectory() {
		return archiveDirectory;
	}

	public void setArchiveDirectory(String archiveDirectory) {
		this.archiveDirectory = archiveDirectory;
	}

	public String getDestinationDirectory() {
		return destinationDirectory;
	}

	public void setDestinationDirectory(String destinationDirectory) {
		this.destinationDirectory = destinationDirectory;
	}

	public String getDeleteSource() {
		return deleteSource;
	}

	public void setDeleteSource(String deleteSource) {
		this.deleteSource = deleteSource;
	}

	public void setRenameExtension(String extension) {
		this.destinationExtension = extension;
	}

	public String getFilterExtension() {
		return filterExtension;
	}

	public void setFilterExtension(String filterExtension) {
		this.filterExtension = filterExtension;
	}

	public String getArchiveExtension() {
		return archiveExtension;
	}

	public void setArchiveExtension(String archiveExtension) {
		this.archiveExtension = archiveExtension;
	}

	public String getRenameExtension() {
		return destinationExtension;
	}

	public String getDestinationExtension() {
		return destinationExtension;
	}

	public void setDestinationExtension(String destinationExtension) {
		this.destinationExtension = destinationExtension;
	}

	
}
