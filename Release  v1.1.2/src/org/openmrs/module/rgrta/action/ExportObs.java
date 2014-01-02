/**
 * 
 */
package org.openmrs.module.rgrta.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atd.StateManager;
import org.openmrs.module.atd.action.ProcessStateAction;
import org.openmrs.module.atd.hibernateBeans.ATDError;
import org.openmrs.module.atd.hibernateBeans.FormAttributeValue;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.hibernateBeans.Session;
import org.openmrs.module.atd.hibernateBeans.State;
import org.openmrs.module.atd.hibernateBeans.StateAction;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.chirdlutil.util.IOUtil;
import org.openmrs.module.chirdlutil.util.Util;
import org.openmrs.module.rgrta.RgrtaStateActionHandler;
import org.openmrs.module.rgrta.hibernateBeans.Encounter;
import org.openmrs.module.rgrta.hl7.mckesson.HL7MessageConstructor;
import org.openmrs.module.rgrta.service.EncounterService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;




/**
 * @author tmdugan
 *
 */
public class ExportObs implements ProcessStateAction
{

	/* (non-Javadoc)
	 * @see org.openmrs.module.rgrta.action.ProcessStateAction#processAction(org.openmrs.module.atd.hibernateBeans.StateAction, org.openmrs.Patient, org.openmrs.module.atd.hibernateBeans.PatientState, java.util.HashMap)
	 */
	
	private static Log log = LogFactory.getLog(RgrtaStateActionHandler.class);
	
	public void processAction(StateAction stateAction, Patient patient,
			PatientState patientState, HashMap<String, Object> parameters)
	{
	
		ATDService atdService = Context.getService(ATDService.class);
		EncounterService encounterService = Context.getService(EncounterService.class);
		PatientService patientService = Context.getPatientService();
		
		Integer patientId = patient.getPatientId();
		patient = patientService.getPatient(patientId);
		Integer locationTagId = patientState.getLocationTagId();
		Integer locationId = patientState.getLocationId();
		FormInstance formInstance = (FormInstance) parameters.get("formInstance");
		PatientState patientStateWithForm = atdService.getPrevPatientStateByAction(patientState.getSessionId(), patientState.getPatientStateId(), "PRODUCE FORM INSTANCE");
		if (formInstance == null){
			formInstance = patientStateWithForm.getFormInstance();
		}
		
		State currState = patientState.getState();
		Integer sessionId = patientState.getSessionId();
		Session session = atdService.getSession(sessionId);
		Integer encounterId = session.getEncounterId();
		Integer orderRep = 0;
	
		try {
			
			String configFileName = getFileLocation( formInstance.getFormId(), "HL7ConfigFile");
			if (configFileName == null || configFileName.equalsIgnoreCase("") ) {
				log.error("Hl7 configuration file not found");
				ATDError ce = new ATDError("Error", "Hl7 Export", 
						"Hl7 export config file not found: " + configFileName, 
						null ,
						 new Date(), sessionId);
				atdService.saveError(ce);
				return;
			}
			Properties hl7Properties = org.openmrs.module.rgrta.util.Util.getProps(configFileName);
			Encounter openmrsEncounter = (Encounter) encounterService.getEncounter(encounterId);
			HL7MessageConstructor constructor = new HL7MessageConstructor(configFileName, null );
			
			boolean send = assembleMessage(patient,openmrsEncounter,  formInstance, sessionId, hl7Properties, constructor, orderRep);
			if (send) {
				String message = constructor.getMessage();
				saveMessageFile(message, formInstance);
			}
			
		} finally{

			StateManager.endState(patientState);
			RgrtaStateActionHandler.changeState(patient, sessionId, currState,stateAction,
					parameters,locationTagId,locationId);
		}
	}

	public void changeState(PatientState patientState,
			HashMap<String, Object> parameters) {
		//deliberately empty because processAction changes the state
	}
	
	private String getFileLocation( Integer formId, String fileType){
		
		
		String filename = null;
		Integer locId = null;
		Integer locTagId = null;
		
		LocationService locService = Context.getLocationService();
		
		
		ATDService atdService = Context.getService(ATDService.class);
		Location loc= locService.getLocation("Default Location");
		LocationTag  locTag = locService.getLocationTagByName("Default Location Tag");
		locId = loc.getLocationId();
		locTagId = locTag.getLocationTagId();
		
		FormAttributeValue formAttrValue = atdService.getFormAttributeValue(formId, fileType, locTagId, locId);
		if (formAttrValue != null){
			filename = formAttrValue.getValue();
		}
		return filename;
	}


	public void saveMessageFile( String message, FormInstance formInstance){
	
		AdministrationService adminService = Context.getAdministrationService();
		EncounterService es = Context.getService(EncounterService.class);
		String filename =  org.openmrs.module.chirdlutil.util.Util.archiveStamp() + ".hl7";
		String exportDir = getFileLocation( formInstance.getFormId(), "HL7ExportObsDir");
		FileOutputStream exportFile = null;
		
		try
		{
			 exportFile = new FileOutputStream(
					exportDir + "/" + filename);
		} catch (FileNotFoundException e1)
		{
			log.error("Couldn't find file: "+ exportFile + "/" + filename);
		}
		if (exportFile != null && exportFile != null)
		{
			try
			{
	
				ByteArrayInputStream messageStream = new ByteArrayInputStream(
						message.getBytes());
				IOUtil.bufferedReadWrite(messageStream, exportFile);
				exportFile.flush();
				exportFile.close();
			} catch (Exception e)
			{
				try
				{
					exportFile.flush();
					exportFile.close();
				} catch (Exception e1)
				{
				}
				log.error("There was an error writing the hl7 export obs file");
				log.error(e.getMessage());
				log.error(org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
			}
		}
		return;
		
	}

	private boolean assembleMessage(Patient patient,Encounter openmrsEncounter, 
			 FormInstance formInstance, Integer sessionId, Properties hl7Properties, HL7MessageConstructor constructor,
			Integer orderRep){
		
		try {
			
			log.error("begin assembling.");
			
			constructor.AddSegmentMSH(openmrsEncounter);
			constructor.AddSegmentPID(patient);
			constructor.AddSegmentPV1(openmrsEncounter);
			log.error("pv1 done");
			String mapFile = getFileLocation( formInstance.getFormId(), "ConceptMapLocation");
			log.error("file location " + mapFile);
			Document doc = getDocument(mapFile);
			if (doc == null) {
				return false;
			}
			log.error("Doc is not null");
			Hashtable<String, String> mappings = this.loadHashTable(doc);
			String conceptCategory = "";
			log.error("Hash table loaded" );
			
			conceptCategory = doc.getDocumentElement().getAttribute("category");
			log.error("concept category is"  + conceptCategory);
			Integer numberOfOBXSegments = 0;
			
			String obrBatteryName = hl7Properties.getProperty("obr_battery_name");
			log.error("battery name from config is"  + obrBatteryName);
			if (conceptCategory != null && conceptCategory.equalsIgnoreCase(obrBatteryName)) {

				List<Obs> obsList = getObsList(openmrsEncounter, mappings);
				log.error("obsList is"  + obsList.size());
				// Create OBR and OBX segments 
				if (obsList != null && obsList.size() > 0) {
					numberOfOBXSegments = addOBXBlock(constructor, openmrsEncounter,
							obsList, mappings, obrBatteryName, orderRep);
					orderRep++;
					if (numberOfOBXSegments > 0)
						return true;
				}
				// If no observations, do not create message

			}
			
		} catch (Exception e) {
			log.error("Exception exporting obs to INPC. " + Util.getStackTrace(e));
		}
		
		return false;
	}
	
	private Document getDocument(String conceptMapFile) {
		File file = new File(conceptMapFile);
		Document doc = null;
		if (file.exists()) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(file);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			} catch (ParserConfigurationException e) {

				log.error("Unable to parse XML map file");
			} catch (SAXException e) {

				log.error("SAX handler exeception when parsing XML map file");
			}
		}
		return doc;
	}
	
	private Hashtable<String, String> loadHashTable(Document doc) {

		String rgrtaConceptName = null;
		String rmrsConceptName = null;
		NodeList rgrtaNodes = null;
		if (doc != null) {
			rgrtaNodes = doc.getElementsByTagName("RGRTA");
		}
		Hashtable<String, String> conceptMapping = new Hashtable<String, String>();

		for (int i = 0; i < rgrtaNodes.getLength(); i++) {
			Node rgrtaTextNode = rgrtaNodes.item(i).getFirstChild();
			if (rgrtaTextNode != null) {
				rgrtaConceptName = rgrtaTextNode.getNodeValue();
			}
			Node rmrsNode = rgrtaNodes.item(i).getNextSibling();

			if (rmrsNode != null) {
				Node rmrsTextNode = rmrsNode.getFirstChild();
				if (rmrsTextNode != null) {
					rmrsConceptName = rmrsTextNode.getNodeValue();
				}
			}
			conceptMapping.put(rgrtaConceptName, rmrsConceptName);

		}
		return conceptMapping;
	}
	
	private List<Obs> getObsList(Encounter encounter, Hashtable<String, String> table) {

		ConceptService conceptService = Context.getConceptService();
		ObsService obsService = Context.getObsService();
		List<Concept> concepts = new ArrayList<Concept>();
		List<Obs> obsList = null;

		try {

			Enumeration<String> en = table.keys();
			while (en.hasMoreElements()) {
				String chicaConceptName = (String) en.nextElement();
				Concept chicaConcept = conceptService.getConceptByName(chicaConceptName);
				if (chicaConcept != null ) {
					concepts.add(chicaConcept);
				}

			}

			if (concepts.size() > 0) {
				List<org.openmrs.Encounter> encounters = new ArrayList<org.openmrs.Encounter>();
				encounters.add(encounter);

				obsList = obsService.getObservations(null, encounters, null, concepts, null, null,
						null, null, null, null, null, false);
			}
		} catch (Exception e) {
			log.error("Error collecting obs list from concept map.");
		}

		return obsList;

	}
	
	
	
	private int addOBXBlock(HL7MessageConstructor constructor, Encounter encounter,
			List<Obs> obsList, Hashtable<String, String> mappings, String batteryName, int orderRep) {
		// Get all obs for one encounter, where the concept is in the mapping
		// properties xml
		// If an obs for that concept does not exist for an encounter, we do not
		// create an OBX

		Locale locale = new Locale("en_US");
		String units = "";
		String rmrsCode = "";
		String hl7Abbreviation = "";
		ConceptDatatype conceptDatatype = null;
		int obsRep = 0;
		addOBRSegment(constructor, encounter, batteryName, orderRep);

		for (Obs obs : obsList) {
			Concept chicaConcept = obs.getValueCoded();
			ConceptName cname = chicaConcept.getName(locale);
			if (cname != null) {
				String rmrsName = mappings.get(cname.getName());
				if (rmrsName == null)
					continue;
				Concept rmrsConcept = getRMRSConceptByName(rmrsName);
				if (rmrsConcept != null) {
					rmrsCode = getRMRSCodeFromConcept(rmrsConcept);
				}
				String value = obs.getValueAsString(locale);

				if (chicaConcept.isNumeric()) {
					Double obsRounded=null;
					if(obs.getValueNumeric()!=null){
						obsRounded = org.openmrs.module.chirdlutil.util.Util.round(Double
								.valueOf(obs.getValueNumeric()), 1);
					}
					if (obsRounded != null) {
						value = String.valueOf(obsRounded);
					}
				}
				conceptDatatype = rmrsConcept.getDatatype();
				String sourceCode = "";

				if (conceptDatatype != null && conceptDatatype.isCoded()) {
					Concept answer = obs.getValueCoded();

					Collection<ConceptMap> maps = answer.getConceptMappings();
					ConceptMap map = null;
					if (maps != null) {
						Iterator<ConceptMap> it = maps.iterator();
						if (it.hasNext()) {
							// get first
							map = it.next();
						}
						if (map != null) {
							sourceCode = map.getSourceCode();
						}
					}

				}
				hl7Abbreviation = conceptDatatype.getHl7Abbreviation();
				units = getUnits(rmrsConcept);
				Date datetime = obs.getObsDatetime();
				constructor.AddSegmentOBX(rmrsName, rmrsCode, null, sourceCode, value, units,
						datetime, hl7Abbreviation, orderRep, obsRep);
				obsRep++;

			}

		}

		return obsRep;
	}
	
	private String getUnits(Concept concept) {
		String units = "";
		ConceptService cs = Context.getConceptService();
		ConceptDatatype datatype = concept.getDatatype();
		Integer conceptId = concept.getConceptId();
		if (datatype != null && datatype.isNumeric()) {
			ConceptNumeric rmrsNumericConcept = (ConceptNumeric) cs.getConcept(conceptId);
			units = rmrsNumericConcept.getUnits();
		}
		return units;
	}
	
	private void addOBRSegment(HL7MessageConstructor constructor, Encounter openmrsEncounter,
			String batteryName, int orderRep) {

		ConceptService cs = Context.getConceptService();

		String batteryId = null;

		if (batteryName != null) {
			Concept batteryConcept = cs.getConceptByName(batteryName);
			batteryId = getBatteryIdByConcept(batteryConcept);
		}

		constructor.AddSegmentOBR(openmrsEncounter, batteryId, batteryName, orderRep);

	}
	
	private Concept getRMRSConceptByName(String rmrsname) {

		ConceptService cs = Context.getConceptService();
		Concept rmrsConcept = null;

		List<ConceptClass> classList = new ArrayList<ConceptClass>();
		try {
			ConceptClass rmrsClass = cs.getConceptClassByName("RMRS");
			classList.add(rmrsClass);
			List<Concept> concepts = cs.getConceptsByName(rmrsname);
			List<Concept> conceptsWithSameName = new ArrayList<Concept>();
			
			for(Concept concept: concepts){
				if(concept.getConceptClass().getName().equals("RMRS")){
					conceptsWithSameName.add(concept);
				}
			}

			if (conceptsWithSameName.size() > 0) {
				if (conceptsWithSameName.size() > 1) {
					log.error("More than one RMRS concept exist with exact name of " + rmrsname);
				}
				rmrsConcept = conceptsWithSameName.get(0);
			} else {
				log.error("No RMRS class concepts found with exact name of " + rmrsname);
			}
		} catch (APIException e) {
			log.error("ConceptClass api exception." + e.getMessage());

		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return rmrsConcept;
	}
	
	private String getRMRSCodeFromConcept(Concept rmrsConcept) {
		String rmrsCode = "";
		// get the mappings
		// get the source

		Iterator<ConceptMap> mapsIter = rmrsConcept.getConceptMappings().iterator();
		if (mapsIter.hasNext()) {
			rmrsCode = mapsIter.next().getSourceCode();
		}
		return rmrsCode;
	}
	
	private String getBatteryIdByConcept(Concept batteryConcept) {

		String sourceCode = "";
		Collection<ConceptMap> maps = batteryConcept.getConceptMappings();
		Iterator<ConceptMap> it = maps.iterator();
		if (it.hasNext()) {
			sourceCode = it.next().getSourceCode();
		}
		return sourceCode;

	}




}
