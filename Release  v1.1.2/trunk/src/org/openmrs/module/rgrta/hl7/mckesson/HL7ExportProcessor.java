/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.rgrta.hl7.mckesson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptSource;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.FormService;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.hibernateBeans.ATDError;
import org.openmrs.module.atd.hibernateBeans.FormAttributeValue;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.chirdlutil.util.Base64;
import org.openmrs.module.chirdlutil.util.FileDateComparator;
import org.openmrs.module.chirdlutil.util.FileListFilter;
import org.openmrs.module.chirdlutil.util.IOUtil;
import org.openmrs.module.rgrta.hibernateBeans.Encounter;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7Export;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportMap;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.module.rgrta.service.EncounterService;
import org.openmrs.module.rgrta.service.RgrtaService;
import org.openmrs.module.rgrta.util.Util;
import org.openmrs.module.sockethl7listener.HL7MessageConstructor;
import org.openmrs.module.sockethl7listener.HL7SocketHandler;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v25.segment.OBX;

/**
 * Processes message in the HL7 inbound queue. Messages are moved into either the archive or error
 * table depending on success or failure of the processing. You may, however, set a global property
 * that causes the processor to ignore messages regarding unknown patients from a non-local HL7
 * source. (i.e. those messages neither go to the archive or the error table.)
 * 
 * @version 1.0
 */
@Transactional
public class HL7ExportProcessor /* implements Runnable */{
	
	private final Log log = LogFactory.getLog(this.getClass());

	
	private static Boolean isRunning = false; // allow only one running
	private String host = null;
	private Integer port = null;
	private Integer socketReadTimeout = null;
	private String resendOption = null;
	private HL7SocketHandler socketHandler;
	private String resendNoAck;
	
	// processor per JVM
	
	/**
	 * Empty constructor (requires context to be set using <code>setContext(Context)</code> method
	 * before any other calls are made)
	 */
	public HL7ExportProcessor() {
		
	}
	
	public HL7ExportProcessor(String host, Integer port, Integer socketReadTimeout, 
		String resendOption) {
		
		this.host = host;
		this.port = port;
		this.socketReadTimeout = socketReadTimeout;
		this.resendOption = resendOption;
		
		
	}
	
	/**
	 * Export hl7 from a single queue entry in the export table
	 * 
	 * @param hl7InQueue queue entry to be processed
	 */
	public void exportHL7(RgrtaHL7Export rgrtaHl7Export) {
		
		RgrtaService RgrtaService = Context.getService(RgrtaService.class);
		ATDService atdService = Context.getService(ATDService.class);
		EncounterService encounterService = Context.getService(EncounterService.class);
		AdministrationService adminService = Context.getAdministrationService();
		LocationService locService = Context.getLocationService();
		socketHandler = new HL7SocketHandler();
		Context.openSession();
		
		try
		{
		
			socketHandler.openSocket(host, port);
			//add socketReadTimeout to scheduler
			
			//Get location of hl7 configuration file from location_tag_attribute_value
			Integer encId = rgrtaHl7Export.getEncounterId();
			Integer hl7ExportQueueId = rgrtaHl7Export.getQueueId();
			FormInstance formInstance = this.getFormInstanceByExportQueueId(hl7ExportQueueId);
			
			//Get the hl7 config file
			String configFileName;
			if (formInstance == null ){
				
				configFileName = IOUtil.formatDirectoryName(adminService
						.getGlobalProperty("rgrta.defaultHl7ConfigFileLocation"));
				
			}else {
				configFileName = getConfigFileLocation( formInstance.getFormId());
			}
			
			if (configFileName == null || configFileName.equalsIgnoreCase("") ) {
				rgrtaHl7Export.setStatus(RgrtaService.getRgrtaExportStatusByName("hl7_config_file_not_found"));
				RgrtaService.saveRgrtaHL7Export(rgrtaHl7Export);
				Integer sessionId = rgrtaHl7Export.getSessionId();
				ATDError ce = new ATDError("Error", "Hl7 Export", 
						"Hl7 export config file not found: " + configFileName, 
						null ,
						 new Date(), sessionId);
				atdService.saveError(ce);
				return;
			}
			
			
			//Construct the message
			
			Properties hl7Properties = Util.getProps(configFileName);
			if (hl7Properties ==null){
				rgrtaHl7Export.setStatus(RgrtaService.getRgrtaExportStatusByName("no_hl7_config_properties"));
				RgrtaService.saveRgrtaHL7Export(rgrtaHl7Export);
				Integer sessionId = rgrtaHl7Export.getSessionId();
				ATDError ce = new ATDError("Error", "Hl7 Export", 
						"HL7 config file found but no properties are present: " + configFileName, 
						null ,
						 new Date(), sessionId);
				atdService.saveError(ce);
				return;
			}
			
		
			Encounter openmrsEncounter = (Encounter) encounterService.getEncounter(encId);
			
			org.openmrs.module.rgrta.hl7.mckesson.HL7MessageConstructor constructor = 
				new org.openmrs.module.rgrta.hl7.mckesson.HL7MessageConstructor(configFileName, null );
			
		
			List<Encounter> queryEncounterList = new ArrayList<Encounter>();
			queryEncounterList.add(openmrsEncounter);
		
			constructor.AddSegmentMSH(openmrsEncounter);
			Patient patient = openmrsEncounter.getPatient();
			if (patient == null){
				Integer sessionId = rgrtaHl7Export.getSessionId();
				ATDError ce = new ATDError("Error", "Hl7 Export", 
						"Cannot export because patient is null. " + openmrsEncounter.getEncounterId(), 
						null ,
						 new Date(), sessionId);
				atdService.saveError(ce);
				return;
			}
			PatientIdentifier identifier = patient.getPatientIdentifier();
			constructor.AddSegmentPID(openmrsEncounter.getPatient());
			constructor.AddSegmentPV1(openmrsEncounter);
			constructor.setAssignAuthority(identifier);
			
		
			if (!addOBXForTiff(constructor, openmrsEncounter , formInstance.getFormId(), null, 
						hl7Properties)){
				rgrtaHl7Export.setStatus(RgrtaService.getRgrtaExportStatusByName("Image_not_found"));
					RgrtaService.saveRgrtaHL7Export(rgrtaHl7Export);
					Integer sessionId = rgrtaHl7Export.getSessionId();
					ATDError ce = new ATDError("Error", "Hl7 Export", 
							"Image not found for hl7 export " + formInstance, 
							null ,
							 new Date(), sessionId);
					atdService.saveError(ce);
					return;
			}
				
			//Send the message
			String message = constructor.getMessage();
			rgrtaHl7Export.setStatus(RgrtaService.getRgrtaExportStatusByName("hl7_sent"));
			rgrtaHl7Export.setDateProcessed(new Date());
			
			
			LocationTag  locTag = locService.getLocationTagByName("Default Location Tag");
			Integer locTagId = locTag.getLocationTagId();
			
			
			Date ackDate = null;
			if (message != null && !message.equals("")){
				ackDate = sendMessage(message, openmrsEncounter, socketHandler);
				if (ackDate != null) { 
					rgrtaHl7Export.setStatus(RgrtaService.getRgrtaExportStatusByName("ACK_received"));
					rgrtaHl7Export.setAckDate(ackDate);
					ConceptService conceptService = Context.getConceptService();
					Concept concept = conceptService.getConcept("atd_sent_to_provider");
					Util.saveObs(patient, concept, encId, formInstance.toString(), formInstance, null, locTagId);
					
				}	else {
					rgrtaHl7Export.setStatus(RgrtaService.getRgrtaExportStatusByName("ACK_not_received"));
				}
				saveMessageFile(message,encId, ackDate);
			}
			
	
			RgrtaService.saveRgrtaHL7Export(rgrtaHl7Export);
	    
		} catch (IOException e ){
			log.error("Error exporting message:" + e);
			
		}catch (Exception e)
		
		{
			Integer sessionId = rgrtaHl7Export.getSessionId();
			String message = e.getMessage();
			ATDError ce = new ATDError("Error", "Hl7 Export", 
					message, "Error creating hl7 export: " + e.getMessage(),
					 new Date(), sessionId);
			atdService.saveError(ce);
			
		} finally
		{
			socketHandler.closeSocket();
			Context.closeSession();
		}

	}	
	
	
	/**
	 * Transform the next pending HL7 inbound queue entry. If there are no pending items in the
	 * queue, this method simply returns quietly.
	 * 
	 * @return true if a queue entry was processed, false if queue was empty
	 */
	public boolean processNextHL7InQueue() {
		
		RgrtaService rgrtaService = Context.getService(RgrtaService.class);
		boolean entryProcessed = false;
		RgrtaHL7Export rgrtaHl7Export = rgrtaService.getNextPendingHL7Export(resendOption, resendNoAck);
		if (rgrtaHl7Export != null) {
			exportHL7(rgrtaHl7Export);
			entryProcessed = true;
		}
		return entryProcessed;
	}
	
	
	/**
	 * Starts up a thread to process all existing HL7InQueue entries
	 */
	public void processHL7InQueue( String host, Integer port, Integer socketReadTimeout, 
			String resendOption ) throws HL7Exception {
		RgrtaService rgrtaService = Context.getService(RgrtaService.class);
		synchronized (isRunning) {
			if (isRunning) {
				log.warn("HL7 processor aborting (another processor already running)");
				return;
			}
			isRunning = true;
		}
		try {
			log.debug("Start processing hl7 in queue");
			while (processNextHL7InQueue( )) {
				// loop until queue is empty
			}
			log.debug("Done processing hl7 in queue");
		}
		finally {
			isRunning = false;
		}
	}
	
	private String getFormInstanceIdByExportQueueId(Integer queueId){
		RgrtaService RgrtaService = Context.getService(RgrtaService.class);
		RgrtaHL7ExportMap exportmap =  RgrtaService.getRgrtaExportMapByQueueId(queueId);
		if (exportmap == null){
			return null;
		}
		return exportmap.getValue();
	
	}
	
	
	
	private Integer getFormIdByExportQueueId(Integer queueId){
		String formInstanceIdString = getFormInstanceIdByExportQueueId(queueId);
		Integer formId = null;
		String[] segments = formInstanceIdString.split("_");
		if (segments == null){
			return null;
		}
		String formIdString = segments[1];
		if (formIdString == null){
			return null;
		}
		
		formId = Integer.valueOf(formIdString);
		
		return formId;
	}
	
	private FormInstance getFormInstanceByExportQueueId(Integer queueId){
		String formInstanceIdString = getFormInstanceIdByExportQueueId(queueId);
		Integer formId = null;
		String[] segments = formInstanceIdString.split("_");
		if (segments == null){
			return null;
		}
		FormInstance formInstance = new FormInstance(Integer.valueOf(segments[0]), 
				Integer.valueOf(segments[1]), Integer.valueOf(segments[2]));
		
		return formInstance;
	}
	
	
private String getConfigFileLocation( Integer formId){
		
		
		String filename = null;
		Integer locId = null;
		Integer locTagId = null;
		
		LocationService locService = Context.getLocationService();
		
		
		ATDService atdService = Context.getService(ATDService.class);
		Location loc= locService.getLocation("Default Location");
		LocationTag  locTag = locService.getLocationTagByName("Default Location Tag");
		locId = loc.getLocationId();
		locTagId = locTag.getLocationTagId();
		
		FormAttributeValue formAttrValue = atdService.getFormAttributeValue(formId, "HL7ConfigFile", locTagId, locId);
		if (formAttrValue != null){
			filename = formAttrValue.getValue();
		}
		return filename;
	}
	
	private String getBatteryIdByConcept(Concept batteryConcept){
		
	
				String sourceCode = "";
				Collection<ConceptMap> maps = batteryConcept.getConceptMappings();
				Iterator<ConceptMap>  it = maps.iterator();
				if (it.hasNext()){
					sourceCode = it.next().getSourceCode();
				}
				return sourceCode;
		
	}
	

	private boolean addOBXForTiff(org.openmrs.module.rgrta.hl7.mckesson.HL7MessageConstructor constructor,
			Encounter encounter, Integer formId ,Hashtable<String,
			String> mappings, 
			Properties hl7Properties){
		
		
		boolean obxcreated = false;
		Integer locationTagId = null;
		String formName = null;
		int obsRep = 0;
		
		String obrBatteryName = hl7Properties.getProperty("obr_name");
		String obrBatteryCode = hl7Properties.getProperty("obr__code");
		String obxAlertTitle = hl7Properties.getProperty("obx_title");
		String obxAlertTitleCode = hl7Properties.getProperty("obx_title_code");
		String obxAlertDescripton = hl7Properties.getProperty("obx_alert_description");
		String obxAlert = hl7Properties.getProperty("obx_alert");
		String obxAlertCode = hl7Properties.getProperty("obx_alert_code");
		
		
		
		ATDService atdService = Context.getService(ATDService.class);
		FormService formService = Context.getFormService();
		Form form = formService.getForm(formId);
		if (form != null){
			formName = form.getName();
		}
		
		String formDir = "";
		String hl7Abbreviation = "ED";
		Integer encounterId = encounter.getEncounterId();
		locationTagId = getLocationTagIdByEncounter(encounterId);
		
		
		List<PatientState> patientStates = 
			atdService.getPatientStatesWithFormInstances(formName, encounterId);
		
		FormInstance formInstance = null;
		Integer formInstanceId = null;
		Integer formLocationId = null;
		
		if (patientStates == null){
			return false; 
		}
		
		Iterator<PatientState> psIterator = patientStates.iterator();
		if (psIterator.hasNext()){
			formInstance = psIterator.next().getFormInstance();
			if (formInstance == null){
				return false;
			}
		}
		
		String filename = "";
		String encodedForm = "";
		try {
			formId = formInstance.getFormId();
			formInstanceId = formInstance.getFormInstanceId();
			formLocationId = formInstance.getLocationId();
			
			if (formId == null || locationTagId == null || formLocationId == null
					|| formInstanceId == null){
				return false;
			}
			
			formDir  = IOUtil
						.formatDirectoryName(org.openmrs.module.atd.util.Util
								.getFormAttributeValue(formId,
										"imageDirectory", locationTagId,
										formLocationId));
			

			if (formDir == null || formDir.equals("")){
				return false;
			}
			filename = formLocationId + "-" + formId + "-" + formInstanceId;
			
			//This FilenameFilter will get ALL tifs starting with the filename
			//including of rescan versions nnn_1.tif, nnn_2.tif, etc
			FilenameFilter filtered = new FileListFilter(filename, "pdf");
			File dir = new File(formDir);
			File[] files = dir.listFiles(filtered); 
			if (files == null || files.length == 0){
				return false;
			}
			
			//This FileDateComparator will list in order
			//with newest file first.
			Arrays.sort(files, new FileDateComparator());
		
			encodedForm = encodeForm(files[0]);
				
			int orderRep = 0;
			
			constructor.setFormInstance(formInstance.toString());
			constructor.AddSegmentOBR(encounter, obrBatteryCode, obrBatteryName, orderRep);
			constructor.AddSegmentOBX(obxAlertTitle, obxAlertTitleCode,
						null, null, obxAlertDescripton, null, new Date() , "ST", orderRep, obsRep);
			OBX resultOBX = constructor.AddSegmentOBX(obxAlert, obxAlertCode, 
						null, "", encodedForm, "", new Date() , hl7Abbreviation, orderRep, obsRep + 1);
			if (resultOBX != null){
					obxcreated = true;
			} 
		
		} catch (Exception e) {
			log.error("Exception adding OBX for tiff image. " + e.getMessage());
		}
			
		return obxcreated;
		
	}
	
	private Integer getLocationTagIdByEncounter(Integer encId){
		Integer locationTagId = null;
		String printerLocation = null;
		
		EncounterService encounterService = Context
		.getService(EncounterService.class);
		Encounter encounter = (Encounter) encounterService
			.getEncounter(encId);
		
		try {
			if (encId != null && encounter != null)
			{
				// see if the encounter has a printer location
				// this will give us the location tag id
				printerLocation = encounter.getPrinterLocation();

				// if the printer location is null, pick
				// any location tag id for the given location
				if (printerLocation == null)
				{
					Location location = encounter.getLocation();
					if (location != null)
					{
						Set<LocationTag> tags = location.getTags();

						if (tags != null && tags.size() > 0)
						{
							printerLocation = ((LocationTag) tags.toArray()[0])
									.getTag();
						}
					}
				}
				if (printerLocation != null)
				{
					LocationService locationService = Context
							.getLocationService();
					LocationTag tag = locationService
							.getLocationTagByName(printerLocation);
					if (tag != null)
					{
						locationTagId = tag.getLocationTagId();
					}
				}
				
			}
		} catch (APIException e) {
			log.error("LocationTag api exception: " +  e.getMessage());
		} catch (Exception e){
			log.error(e.getMessage());
		}
		return locationTagId;
	}
	
	private String encodeForm(File file){
			String encodedForm = null;
			try {
				
				FileInputStream fis = new FileInputStream(file);			
				ByteArrayOutputStream bas = new ByteArrayOutputStream();
				
				int c;
				while((c = fis.read()) != -1){
				  bas.write(c);
				}
				encodedForm = Base64.byteArrayToBase64(bas.toByteArray(), false);

				fis.close();
				bas.flush();
				bas.close();	
				
				
			} catch (FileNotFoundException e) {
				log.error("Tiff file not found");
			} catch (IOException e) {
				log.error("Unable to read tiff file.");
			}
			
			return encodedForm;
		
		
	}
	
	private void addOBRSegment(HL7MessageConstructor constructor, 
			Encounter openmrsEncounter,  String batteryName, int orderRep){
		
		ConceptService cs = Context.getConceptService();
	
		String batteryId = null;
		
		if (batteryName != null){
			Concept batteryConcept = cs.getConceptByName(batteryName);
			batteryId = getBatteryIdByConcept(batteryConcept);
		}
		
		constructor.AddSegmentOBR(openmrsEncounter, batteryId, batteryName, orderRep);
		
	}
	
	private Concept createConcept(String attachmentBatteryName, String attachmentBatteryCode){
		Concept concept = new Concept();
		ConceptService conceptService = Context.getConceptService();
		ConceptClass conceptClass = conceptService.getConceptClassByName("RMRS");
		ConceptName conceptName = new ConceptName(attachmentBatteryName, new Locale("ENGLISH"));
		ConceptDatatype conceptDatatype = 
			conceptService.getConceptDatatypeByName("Text");
		concept.setConceptClass(conceptClass);
		concept.addName(conceptName);
		concept.setDatatype(conceptDatatype);
		concept.setSet(false);
		
		Concept newConcept = conceptService.saveConcept(concept);
		List<ConceptSource>  sources = conceptService.getAllConceptSources();
		
		
		return newConcept;
	}
	
	/**
	 * Saves message string to archive directory
	 * @param message
	 * @param encid
	 */
	public void saveMessageFile( String message, Integer encid, Date ackDate){
		AdministrationService adminService = Context.getAdministrationService();
		EncounterService es = Context.getService(EncounterService.class);

		
		org.openmrs.Encounter enc = es.getEncounter(encid);
		Patient patient = new Patient();
		patient = enc.getPatient();
		PatientIdentifier pi = patient.getPatientIdentifier();
		String mrn = "";
		String ack = "";
		if (ackDate != null){
			ack = "-ACK";
		}
		
		if (pi != null) mrn = pi.getIdentifier();
		String filename =  org.openmrs.module.chirdlutil.util.Util.archiveStamp() + "_"+ mrn + ack + ".hl7";
		
		String archiveDir = IOUtil.formatDirectoryName(adminService
				.getGlobalProperty("Rgrta.outboundHl7ArchiveDirectory"));
		

		FileOutputStream archiveFile = null;
		try
		{
			archiveFile = new FileOutputStream(
					archiveDir + "/" + filename);
		} catch (FileNotFoundException e1)
		{
			log.error("Couldn't find file: "+archiveDir + "/" + filename);
		}
		if (archiveDir != null && archiveFile != null)
		{
			try
			{

				ByteArrayInputStream messageStream = new ByteArrayInputStream(
						message.getBytes());
				IOUtil.bufferedReadWrite(messageStream, archiveFile);
				archiveFile.flush();
				archiveFile.close();
			} catch (Exception e)
			{
				try
				{
					archiveFile.flush();
					archiveFile.close();
				} catch (Exception e1)
				{
				}
				log.error("There was an error writing the hl7 file");
				log.error(e.getMessage());
				log.error(org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
			}
		}
		return;
		
	}
	private Date sendMessage(String message, Encounter enc, 
			HL7SocketHandler socketHandler ){
		Date ackDate = null;
		
		HL7Outbound hl7b = new HL7Outbound();
		hl7b.setHl7Message(message);
		hl7b.setEncounter(enc);
		hl7b.setAckReceived(null);
		hl7b.setPort(port);
		hl7b.setHost(host);
		
		try {
			if (message != null) {
				hl7b = socketHandler.sendMessage(hl7b, socketReadTimeout);
				
				if (hl7b != null && hl7b.getAckReceived() != null){
					ackDate = hl7b.getAckReceived();
					 log.info("Ack received host:" + host + "; port:" + port 
							 + "- first try. Encounter_id = " + enc.getEncounterId());
				}
					
			}
			
		} catch (Exception e){
			log.error("Error exporting message host:" + host + "; port:" + port 
					+ "- first try. Encounter_id = " + enc.getEncounterId());
			try {
				if (message != null) {
					
					hl7b = socketHandler.sendMessage(hl7b, socketReadTimeout);
					 if (hl7b != null && hl7b.getAckReceived() != null){
						 ackDate = hl7b.getAckReceived();
						 log.info("Ack received host:" + host + "; port:" + port 
								 + "- second try. Encounter_id = " + enc.getEncounterId());
					 }
				}
			} catch (Exception e1) {
				log.error("Error exporting message host:" + host + "; port:" + port 
					+ "- second try. Encounter_id = " + enc.getEncounterId());
			}
		}
		
		
		return ackDate;
	}
	
	
	
	
	
	
	
}
