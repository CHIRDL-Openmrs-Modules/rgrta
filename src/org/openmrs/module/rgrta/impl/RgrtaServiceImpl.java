package org.openmrs.module.rgrta.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.FieldType;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.FormService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicService;
import org.openmrs.module.atd.ParameterHandler;
import org.openmrs.module.atd.TeleformTranslator;
import org.openmrs.module.atd.datasource.TeleformExportXMLDatasource;
import org.openmrs.module.atd.hibernateBeans.ATDError;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.atd.hibernateBeans.PatientATD;
import org.openmrs.module.atd.hibernateBeans.PatientState;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.rgrta.RgrtaParameterHandler;
import org.openmrs.module.rgrta.Percentile;
import org.openmrs.module.rgrta.db.RgrtaDAO;
import org.openmrs.module.rgrta.hibernateBeans.Bmiage;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7Export;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportMap;
import org.openmrs.module.rgrta.hibernateBeans.RgrtaHL7ExportStatus;
import org.openmrs.module.rgrta.hibernateBeans.Encounter;
import org.openmrs.module.rgrta.hibernateBeans.Family;
import org.openmrs.module.rgrta.hibernateBeans.Hcageinf;
import org.openmrs.module.rgrta.hibernateBeans.Lenageinf;
import org.openmrs.module.chirdlutil.hibernateBeans.LocationAttributeValue;
import org.openmrs.module.chirdlutil.hibernateBeans.LocationTagAttributeValue;
import org.openmrs.module.chirdlutil.service.ChirdlUtilService;
import org.openmrs.module.rgrta.hibernateBeans.OldRule;
import org.openmrs.module.rgrta.hibernateBeans.PatientFamily;
import org.openmrs.module.atd.hibernateBeans.Statistics;
import org.openmrs.module.rgrta.hibernateBeans.Study;
import org.openmrs.module.rgrta.hibernateBeans.StudyAttributeValue;
import org.openmrs.module.rgrta.service.RgrtaService;
import org.openmrs.module.rgrta.service.EncounterService;
import org.openmrs.module.rgrta.xmlBeans.Field;
import org.openmrs.module.rgrta.xmlBeans.Language;
import org.openmrs.module.rgrta.xmlBeans.LanguageAnswers;
import org.openmrs.module.rgrta.xmlBeans.PWSPromptAnswerErrs;
import org.openmrs.module.rgrta.xmlBeans.PWSPromptAnswers;
import org.openmrs.module.rgrta.xmlBeans.StatsConfig;
import org.openmrs.module.dss.DssElement;
import org.openmrs.module.dss.DssManager;
import org.openmrs.module.dss.hibernateBeans.Rule;
import org.openmrs.module.dss.service.DssService;
import org.openmrs.module.chirdlutil.util.Util;
import org.openmrs.module.chirdlutil.util.XMLUtil;

public class RgrtaServiceImpl implements RgrtaService
{

	private Log log = LogFactory.getLog(this.getClass());

	private RgrtaDAO dao;

	/**
	 * 
	 */
	public RgrtaServiceImpl()
	{
	}

	/**
	 * @return
	 */
	public RgrtaDAO getRgrtaDAO()
	{
		return this.dao;
	}

	/**
	 * @param dao
	 */
	public void setRgrtaDAO(RgrtaDAO dao)
	{
		this.dao = dao;
	}

	/**
	 * @should testPSFConsume
	 * @should testPWSConsume
	 */
	public void consume(InputStream input, Patient patient,
			Integer encounterId,FormInstance formInstance,
			Integer sessionId,
			List<FormField> fieldsToConsume,
			Integer locationTagId)
	{
		long totalTime = System.currentTimeMillis();
		long startTime = System.currentTimeMillis();
		try
		{
			DssService dssService = Context
					.getService(DssService.class);
			ATDService atdService = Context
					.getService(ATDService.class);
			ParameterHandler parameterHandler = new RgrtaParameterHandler();

			// check that the medical record number in the xml file and the medical
			// record number of the patient match
			String patientMedRecNumber = patient.getPatientIdentifier()
					.getIdentifier();
			String xmlMedRecNumber = null;
			String xmlMedRecNumber2 = null;
			LogicService logicService = Context.getLogicService();

			TeleformExportXMLDatasource xmlDatasource = (TeleformExportXMLDatasource) logicService
					.getLogicDataSource("xml");
			HashMap<String, org.openmrs.module.atd.xmlBeans.Field> fieldMap = xmlDatasource
					.getParsedFile(formInstance);

			Integer formInstanceId = formInstance.getFormInstanceId();

			if (fieldMap == null)
			{
				try
				{
					formInstance = xmlDatasource.parse(input,
							formInstance,locationTagId);
					fieldMap = xmlDatasource.getParsedFile(formInstance);

				} catch (Exception e1)
				{
					ATDError atdError = new ATDError("Error","XML Parsing", 
							" Error parsing XML file to be consumed. Form Instance Id = " + formInstanceId
							, Util.getStackTrace(e1), new Date(),sessionId);
					atdService.saveError(atdError);
					return;
				}
			}

			startTime = System.currentTimeMillis();


			Integer formId = formInstance.getFormId();
			Integer locationId = formInstance.getLocationId();
			String medRecNumberTag = org.openmrs.module.atd.util.Util
					.getFormAttributeValue(formId, "medRecNumberTag",locationTagId,locationId);

			//MRN
			if (medRecNumberTag!=null&&fieldMap.get(medRecNumberTag) != null)
			{
				xmlMedRecNumber = fieldMap.get(medRecNumberTag).getValue();
			}
			
			
			//Compare form MRNs to patient medical record number
			AdministrationService adminService = Context.getAdministrationService();
			String consumeMRN = null;
			consumeMRN = adminService
					.getGlobalProperty("rgrta.consumeMRN");
			
			if ((consumeMRN != null )
				&& (consumeMRN.trim().equalsIgnoreCase("y") ||
					consumeMRN.trim().equalsIgnoreCase("yes") ||
					consumeMRN.trim().equalsIgnoreCase("1") ||
					consumeMRN.trim().equalsIgnoreCase("true"))
				){
					
				System.out.println("Performing MRN check");
				if (!Util.extractIntFromString(patientMedRecNumber).equalsIgnoreCase(
						Util.extractIntFromString(xmlMedRecNumber)))
				{
					//Compare patient MRN to MRN bar code from back of form.
					if (xmlMedRecNumber2 == null || !Util.extractIntFromString(patientMedRecNumber)
								.equalsIgnoreCase( Util.extractIntFromString(xmlMedRecNumber2))){
						ATDError noMatch = new ATDError("Fatal", "MRN Validity", "Patient MRN" 
								+ " does not match any form MRN bar codes (front or back) " 
								,"\r\n Form instance id: "  + formInstanceId 
								+ "\r\n Patient MRN: " + patientMedRecNumber
								+ " \r\n MRN barcode front: " + xmlMedRecNumber + "\r\n MRN barcode back: "
								+ xmlMedRecNumber2, new Date(), sessionId);
						atdService.saveError(noMatch);
					    return;
						
					} 
					//Patient MRN does not match MRN bar code on front of form, but does match 
					// MRN bar code on back of form.
					ATDError warning = new ATDError("Warning", "MRN Validity", "Patient MRN matches" 
								+ " MRN bar code from back of form only. " 
								,"Form instance id: "  + formInstanceId 
								+ "\r\n Patient MRN: " + patientMedRecNumber
								+ " \r\n MRN barcode front: " + xmlMedRecNumber + "\r\n MRN barcode back: " 
								+ xmlMedRecNumber2, new Date(), sessionId);
					atdService.saveError(warning);
					
					
				}
			}

			startTime = System.currentTimeMillis();
			// make sure storeObs gets loaded before running consume
			// rules
			dssService.loadRule("CREATE_JIT",false);
			dssService.loadRule("storeObs",false);
			dssService.loadRule("ScoreJit", false);

			startTime = System.currentTimeMillis();
			//only consume the question fields for one side of the PSF
			HashMap<String,Field> languageFieldsToConsume = 
				saveAnswers(fieldMap, formInstance,encounterId,patient);
			FormService formService = Context.getFormService();
			Form databaseForm = formService.getForm(formId);
			TeleformTranslator translator = new TeleformTranslator();

			if (fieldsToConsume == null)
			{
				fieldsToConsume = new ArrayList<FormField>();

				for (FormField currField : databaseForm.getOrderedFormFields())
				{
					FormField parentField = currField.getParent();
					String fieldName = currField.getField().getName();
					FieldType currFieldType = currField.getField()
							.getFieldType();
					// only process export fields
					if (currFieldType != null
							&& currFieldType.equals(translator
									.getFieldType("Export Field")))
					{
						if (parentField != null)
						{
							PatientATD patientATD = atdService.getPatientATD(
									formInstance, parentField.getField()
											.getFieldId());

							if (patientATD != null)
							{
								if (databaseForm.getName().equals("PSF"))
								{
									// consume only one side of questions for
									// PSF
									if (languageFieldsToConsume
											.get(fieldName)!=null)
									{
										fieldsToConsume.add(currField);
									}
								} else
								{
									fieldsToConsume.add(currField);
								}
							} else
							{
								// consume all other fields with parents
								fieldsToConsume.add(currField);
							}
						} else
						{
							// consume all other fields
							fieldsToConsume.add(currField);
						}
					}
				}
			}
			System.out.println("rgrtaService.consume: Fields to consume: "+
				(System.currentTimeMillis()-startTime));
			
			startTime = System.currentTimeMillis();
			atdService.consume(input, formInstance, patient, encounterId,
					 null, null, parameterHandler,
					 fieldsToConsume,locationTagId,sessionId);
			System.out.println("rgrtaService.consume: Time of atdService.consume: "+
				(System.currentTimeMillis()-startTime));
		} catch (Exception e)
		{
			log.error(e.getMessage());
			log.error(Util.getStackTrace(e));
		}
	}

	private void populateFieldNameArrays(
			HashMap<String, HashMap<String, Field>> languages,
			ArrayList<String> pwsAnswerChoices,
			ArrayList<String> pwsAnswerChoiceErr)
	{
		AdministrationService adminService = Context.getAdministrationService();
		StatsConfig statsConfig = null;
		String statsConfigFile = adminService
				.getGlobalProperty("rgrta.statsConfigFile");

		if(statsConfigFile == null){
			log.error("Could not find statsConfigFile. Please set global property rgrta.statsConfigFile.");
			return;
		}
		try
		{
			InputStream input = new FileInputStream(statsConfigFile);
			statsConfig = (StatsConfig) XMLUtil.deserializeXML(
					StatsConfig.class, input);
		} catch (IOException e1)
		{
			log.error(e1.getMessage());
			log.error(Util.getStackTrace(e1));
			return;
		}

		// process prompt answers
		PWSPromptAnswers pwsPromptAnswers = statsConfig.getPwsPromptAnswers();

		if (pwsPromptAnswers != null)
		{
			ArrayList<org.openmrs.module.rgrta.xmlBeans.Field> fields = pwsPromptAnswers
					.getFields();

			if (fields != null)
			{
				for (org.openmrs.module.rgrta.xmlBeans.Field currField : fields)
				{
					if (currField.getId() != null)
					{
						pwsAnswerChoices.add(currField.getId());
					}
				}
			}
		}

		// process prompt answer errs
		PWSPromptAnswerErrs pwsPromptAnswerErrs = statsConfig
				.getPwsPromptAnswerErrs();

		if (pwsPromptAnswerErrs != null)
		{
			ArrayList<org.openmrs.module.rgrta.xmlBeans.Field> fields = pwsPromptAnswerErrs
					.getFields();

			if (fields != null)
			{
				for (org.openmrs.module.rgrta.xmlBeans.Field currField : fields)
				{
					if (currField.getId() != null)
					{
						pwsAnswerChoiceErr.add(currField.getId());
					}
				}
			}
		}

		LanguageAnswers languageAnswers = statsConfig.getLanguageAnswers();
		org.openmrs.module.rgrta.util.Util.populateFieldNameArrays(languages, languageAnswers);
	}

	private HashMap<String, Field> saveAnswers(
			HashMap<String, org.openmrs.module.atd.xmlBeans.Field> fieldMap,
			FormInstance formInstance, int encounterId, Patient patient)
	{
		ATDService atdService = Context
				.getService(ATDService.class);
		TeleformTranslator translator = new TeleformTranslator();
		FormService formService = Context.getFormService();
		Integer formId = formInstance.getFormId();
		Form databaseForm = formService.getForm(formId);
		if (databaseForm == null)
		{
			log.error("Could not consume teleform export xml because form "
					+ formId + " does not exist in the database");
			return null;
		}

		ArrayList<String> pwsAnswerChoices = new ArrayList<String>();
		ArrayList<String> pwsAnswerChoiceErr = new ArrayList<String>();

		HashMap<String, HashMap<String, Field>> languageToFieldnames = new HashMap<String, HashMap<String, Field>>();

		this.populateFieldNameArrays(languageToFieldnames, pwsAnswerChoices,
				pwsAnswerChoiceErr);

		HashMap<String, Integer> languageToNumAnswers = new HashMap<String, Integer>();
		HashMap<String, HashMap<Integer, String>> languageToAnswers = new HashMap<String, HashMap<Integer, String>>();

		Rule providerNameRule = new Rule();
		providerNameRule.setTokenName("providerName");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("encounterId", encounterId);
		providerNameRule.setParameters(parameters);

		for (FormField currField : databaseForm.getFormFields())
		{
			FieldType currFieldType = currField.getField().getFieldType();
			// only process export fields
			if (currFieldType != null
					&& currFieldType.equals(translator
							.getFieldType("Export Field")))
			{
				String fieldName = currField.getField().getName();
				String answer = null;
				if (fieldMap.get(fieldName) != null)
				{
					answer = fieldMap.get(fieldName).getValue();
				}
				FormField parentField = currField.getParent();

				// if parent field is not null look at parent
				// field for rule to execute
				if (parentField != null)
				{
					PatientATD patientATD = atdService.getPatientATD(
							formInstance,
							parentField.getField().getFieldId());

					if (patientATD != null)
					{
						Rule rule = patientATD.getRule();

						if (answer == null || answer.length() == 0)
						{
							answer = "NoAnswer";
						}
						Integer ruleId = rule.getRuleId();

						String dsstype = databaseForm.getName();

						if (dsstype.equalsIgnoreCase("PSF"))
						{
							for (String currLanguage : languageToFieldnames
									.keySet())
							{
								HashMap<String, Field> currLanguageArray = languageToFieldnames
										.get(currLanguage);
								HashMap<Integer, String> answers = languageToAnswers
										.get(currLanguage);

								if (answers == null)
								{
									answers = new HashMap<Integer, String>();
									languageToAnswers
											.put(currLanguage, answers);
								}
						
								if (currLanguageArray.get(currField
										.getField().getName())!= null)
								{
									answers.put(ruleId, answer);
									if (!answer.equalsIgnoreCase("NoAnswer"))
									{
										if (languageToNumAnswers
												.get(currLanguage) == null)
										{
											languageToNumAnswers.put(
													currLanguage, 0);
										}

										languageToNumAnswers.put(currLanguage,
												languageToNumAnswers
														.get(currLanguage) + 1);
									}
								}
							}
						}

						if (dsstype.equalsIgnoreCase("PWS"))
						{
							Integer formInstanceId = formInstance.getFormInstanceId();
							Integer locationId = formInstance.getLocationId();
							List<Statistics> statistics = this.getRgrtaDAO()
									.getStatByIdAndRule(formInstanceId,
											ruleId,dsstype,locationId);

							if (statistics != null)
							{
								if (!answer.equalsIgnoreCase("NoAnswer"))
								{
									answer = formatAnswer(answer);
								}
								for (Statistics stat : statistics)
								{
									if (pwsAnswerChoices.contains(currField
											.getField().getName()))
									{
										stat.setAnswer(answer);
									}
									if (pwsAnswerChoiceErr.contains(currField
											.getField().getName()))
									{
										stat.setAnswerErr(answer);
									}

									this.updateStatistics(stat);
								}
							}
						}
					}

				}
			}
		}

		int maxNumAnswers = -1;
		String maxLanguage = null;
		HashMap<Integer, String> maxAnswers = null;

		for (String language : languageToNumAnswers.keySet())
		{
			Integer compareNum = languageToNumAnswers.get(language);

			if (compareNum != null && compareNum > maxNumAnswers)
			{
				maxNumAnswers = compareNum;
				maxLanguage = language;
				maxAnswers = languageToAnswers.get(language);
			}
		}

		String languageResponse = "English";
		if (maxNumAnswers > 0)
		{
			languageResponse = maxLanguage;
			HashMap<Integer, String> answers = maxAnswers;

			for (Integer currRuleId : answers.keySet())
			{
				String answer = answers.get(currRuleId);
				Integer formInstanceId = formInstance.getFormInstanceId();
				Integer locationId = formInstance.getLocationId();
				List<Statistics> statistics = this.getRgrtaDAO()
						.getStatByIdAndRule(formInstanceId, currRuleId, "PSF",locationId);
				if (statistics != null)
				{
					for (Statistics stat : statistics)
					{
						stat.setAnswer(answer);
						stat.setLanguageResponse(languageResponse);

						this.updateStatistics(stat);
					}
				}
			}
		}
		
		String formName = formService.getForm(formId).getName();
		//save language response to preferred language
		//language is determined by maximum number of answers
		//selected for a language on the PSF
		if (languageResponse != null&&formName.equals("PSF")) {
			ObsService obsService = Context.getObsService();
			Obs obs = new Obs();
			
			String conceptName = "preferred_language";
			ConceptService conceptService = Context.getConceptService();
			Concept currConcept = conceptService.getConceptByName(conceptName);
			Concept languageConcept = conceptService.getConceptByName(languageResponse);
			if (currConcept == null || languageConcept == null) {
				log
					.error("Could not save preferred language for concept: " + conceptName + " and language: "
				        + languageResponse);
			} else {
				obs.setValueCoded(languageConcept);
				
				EncounterService encounterService = Context.getService(EncounterService.class);
				Encounter encounter = (Encounter) encounterService.getEncounter(encounterId);
				
				Location location = encounter.getLocation();
				
				obs.setPerson(patient);
				obs.setConcept(currConcept);
				obs.setLocation(location);
				obs.setEncounter(encounter);
				obs.setObsDatetime(new Date());
				obsService.saveObs(obs, null);
			}
		}
		if (maxNumAnswers > 0){
			return languageToFieldnames.get(maxLanguage);
		}else{
			return languageToFieldnames.get("English");
		}
	}

	private String formatAnswer(String answer)
	{
		final int NUM_ANSWERS = 6;
		String[] answers = new String[NUM_ANSWERS];

		// initialize all answers to N
		for (int i = 0; i < answers.length; i++)
		{
			answers[i] = "X";
		}

		char[] characters = answer.toCharArray();

		// convert the answer string from positional digits (1,2,3,4,5,6)
		// to Y
		for (int i = 0; i < characters.length; i++)
		{
			try
			{
				int intAnswer = Character.getNumericValue(characters[i]);
				int answerPos = intAnswer - 1;

				if (answerPos >= 0 && answerPos < answers.length)
				{
					answers[answerPos] = "Y";
				}
			} catch (Exception e)
			{
				log.error(e.getMessage());
				log.error(Util.getStackTrace(e));
			}
		}

		String newAnswer = "";

		for (String currAnswer : answers)
		{
			newAnswer += currAnswer;
		}

		return newAnswer;
	}

	/**
	 * @should testPSFProduce
	 * @should testPWSProduce
	 */
	public void produce(OutputStream output, PatientState state,
			Patient patient, Integer encounterId, String dssType,
			int maxDssElements,Integer sessionId)
	{
		long totalTime = System.currentTimeMillis();
		long startTime = System.currentTimeMillis();

		DssService dssService = Context
				.getService(DssService.class);
		ATDService atdService = Context
				.getService(ATDService.class);

		DssManager dssManager = new DssManager(patient);
		dssManager.setMaxDssElementsByType(dssType, maxDssElements);
		HashMap<String, Object> baseParameters = new HashMap<String, Object>();
		startTime = System.currentTimeMillis();
		try {
	        dssService.loadRule("CREATE_JIT",false);
	        dssService.loadRule("storeObs",false);
	        dssService.loadRule("ScoreJit", false);
        }
        catch (Exception e) {
	        log.error("load rule failed", e);
        }
		startTime = System.currentTimeMillis();

		FormInstance formInstance = state.getFormInstance();
		atdService.produce(patient, formInstance, output, dssManager,
				encounterId, baseParameters, null,state.getLocationTagId(),sessionId);
		startTime = System.currentTimeMillis();
		Integer formInstanceId = formInstance.getFormInstanceId();
		Integer locationId = formInstance.getLocationId();
		this.saveStats(patient, formInstanceId, dssManager, encounterId,state.getLocationTagId(),locationId);
	}

	private void saveStats(Patient patient, Integer formInstanceId,
			DssManager dssManager, Integer encounterId, 
			Integer locationTagId,Integer locationId)
	{
		HashMap<String, ArrayList<DssElement>> dssElementsByType = dssManager
				.getDssElementsByType();
		EncounterService encounterService = Context
				.getService(EncounterService.class);
		Encounter encounter = (Encounter) encounterService
				.getEncounter(encounterId);
		String type = null;

		if (dssElementsByType == null)
		{
			return;
		}
		Iterator<String> iter = dssElementsByType.keySet().iterator();
		ArrayList<DssElement> dssElements = null;

		while (iter.hasNext())
		{
			type = iter.next();
			dssElements = dssElementsByType.get(type);
			for (int i = 0; i < dssElements.size(); i++)
			{
				DssElement currDssElement = dssElements.get(i);

					this.addStatistics(patient, currDssElement, formInstanceId, i,
							encounter, type,locationTagId,locationId);
				}
			}
		}

	public void updateStatistics(Statistics statistics)
	{
		getRgrtaDAO().updateStatistics(statistics);
	}
	
	public void createStatistics(Statistics statistics)
	{
		getRgrtaDAO().addStatistics(statistics);
	}

	private void addStatistics(Patient patient, DssElement currDssElement,
			Integer formInstanceId, int questionPosition, Encounter encounter,
			String formName,Integer locationTagId,Integer locationId)
	{
			DssService dssService = Context
					.getService(DssService.class);
			Integer ruleId = currDssElement.getRuleId();
			Rule rule = dssService.getRule(ruleId);

			Statistics statistics = new Statistics();
			statistics.setAgeAtVisit(org.openmrs.module.rgrta.util.Util
					.adjustAgeUnits(patient.getBirthdate(), null));
			statistics.setPriority(rule.getPriority());
			statistics.setFormInstanceId(formInstanceId);
			statistics.setLocationTagId(locationTagId);
			statistics.setPosition(questionPosition + 1);

			statistics.setRuleId(ruleId);
			statistics.setPatientId(patient.getPatientId());
			statistics.setFormName(formName);
			statistics.setEncounterId(encounter.getEncounterId());
			statistics.setLocationId(locationId);

			getRgrtaDAO().addStatistics(statistics);
	}

	public Percentile getWtageinf(double ageMos, int sex)
	{
		return getRgrtaDAO().getWtageinf(ageMos, sex);
	}

	public Bmiage getBmiage(double ageMos, int sex)
	{
		return getRgrtaDAO().getBmiage(ageMos, sex);
	}

	public Hcageinf getHcageinf(double ageMos, int sex)
	{
		return getRgrtaDAO().getHcageinf(ageMos, sex);
	}

	public Lenageinf getLenageinf(double ageMos, int sex)
	{
		return getRgrtaDAO().getLenageinf(ageMos, sex);
	}

	public List<Study> getActiveStudies()
	{
		return getRgrtaDAO().getActiveStudies();
	}

	public List<Statistics> getStatByFormInstance(int formInstanceId,String formName, 
			Integer locationId)
	{
		return getRgrtaDAO().getStatByFormInstance(formInstanceId,formName, locationId);
	}

	public StudyAttributeValue getStudyAttributeValue(Study study,
			String studyAttributeName)
	{
		return getRgrtaDAO().getStudyAttributeValue(study, studyAttributeName);
	}

	public List<Statistics> getStatByIdAndRule(int formInstanceId,int ruleId,String formName, 
			Integer locationId)	{
		return getRgrtaDAO().getStatByIdAndRule(formInstanceId,ruleId,formName,locationId);
	}

	public List<OldRule> getAllOldRules()
	{
		return getRgrtaDAO().getAllOldRules();
	}

	
	public PatientFamily getPatientFamily(Integer patientId)
	{
		return getRgrtaDAO().getPatientFamily(patientId);
	}

	public Family getFamilyByAddress(String address)
	{
		return getRgrtaDAO().getFamilyByAddress(address);

	}
	
	public Family getFamilyByPhone(String phone)
	{
		return getRgrtaDAO().getFamilyByPhone(phone);

	}

	public void savePatientFamily(PatientFamily patientFamily)
	{
		getRgrtaDAO().savePatientFamily(patientFamily);

	}

	public void saveFamily(Family family)
	{
		getRgrtaDAO().saveFamily(family);
	}

	public void updateFamily(Family family)
	{
		getRgrtaDAO().updateFamily(family);
	}
	
	public Obs getStudyArmObs(Integer familyId,Concept studyConcept){
		return getRgrtaDAO().getStudyArmObs(familyId, studyConcept);
	}
	
	public List<String> getInsCategories(){
		return getRgrtaDAO().getInsCategories();
	}
	

	public String getObsvNameByObsvId(String obsvId){
		return getRgrtaDAO().getObsvNameByObsvId(obsvId);
	}
	
	public String getInsCategoryByCarrier(String carrierCode){
		return getRgrtaDAO().getInsCategoryByCarrier(carrierCode);
	}
	
	public String getInsCategoryBySMS(String smsCode){
		return getRgrtaDAO().getInsCategoryBySMS(smsCode);
	}
	
	public String getInsCategoryByInsCode(String insCode){
		return getRgrtaDAO().getInsCategoryByInsCode(insCode);
	}
	
	
	public PatientState getPrevProducePatientState(Integer sessionId, Integer patientStateId ){
		ATDService atdService = Context.getService(ATDService.class);
		return atdService.getPrevPatientStateByAction(sessionId, patientStateId,"PRODUCE FORM INSTANCE");
	}
	
	public Double getHighBP(Patient patient, Integer bpPercentile,
			String bpType, org.openmrs.Encounter encounter)
	{
		List<Person> persons = new ArrayList<Person>();
		persons.add(patient);
		ConceptService conceptService = Context.getConceptService();
		Concept concept = conceptService.getConceptByName("HtCentile");
		List<org.openmrs.Encounter> encounters = new ArrayList<org.openmrs.Encounter>();
		encounters.add(encounter);
		Double heightPercentile = null;
		ObsService obsService = Context.getObsService();
		List<Concept> concepts = new ArrayList<Concept>();

		concepts.add(concept);

		List<Obs> obs = obsService
				.getObservations(persons, encounters, concepts, null, null,
						null, null, null, null, null, null, false);
		if (obs != null && obs.size() > 0)
		{
			heightPercentile = obs.get(0).getValueNumeric();
		}

		if (heightPercentile == null)
		{
			return null;
		}
		
		return getHighBP(patient,bpPercentile,bpType,heightPercentile, new Date());
	}
	
	/**
	 * @should checkHighBP
	 */
	public Double getHighBP(Patient patient, Integer bpPercentile, String bpType, 
			Double heightPercentile, Date onDate)
	{
		Integer lowerAge = patient.getAge(onDate);
		
		if(lowerAge > 17){
			return null;
		}
		
		Integer upperAge = lowerAge + 1;
		String sex = patient.getGender();

		Integer lowerPercentile = null;
		Integer upperPercentile = null;

		if (heightPercentile < 5)
		{
			lowerPercentile = 5;
			upperPercentile = 5;
		}

		if (heightPercentile >= 5)
		{
			lowerPercentile = 5;
			upperPercentile = 10;
		}

		if (heightPercentile >= 10)
		{
			lowerPercentile = 10;
			upperPercentile = 25;
		}

		if (heightPercentile >= 25)
		{
			lowerPercentile = 25;
			upperPercentile = 50;
		}

		if (heightPercentile >= 50)
		{
			lowerPercentile = 50;
			upperPercentile = 75;
		}

		if (heightPercentile >= 75)
		{
			lowerPercentile = 75;
			upperPercentile = 90;
		}

		if (heightPercentile >= 90)
		{
			lowerPercentile = 90;
			upperPercentile = 95;
		}

		if (heightPercentile >= 95)
		{
			lowerPercentile = 95;
			upperPercentile = 95;
		}
		
		Integer lowerAgeLowerPercentile = null;
		Integer lowerAgeUpperPercentile = null;
		Integer upperAgeLowerPercentile = null;
		Integer upperAgeUpperPercentile = null;

		if (lowerAge >=1)
		{
			lowerAgeLowerPercentile = getRgrtaDAO().getHighBP(lowerAge,
					sex, bpPercentile, bpType, lowerPercentile);
		}
		if (lowerAge >=1)
		{
			lowerAgeUpperPercentile = getRgrtaDAO().getHighBP(lowerAge,
					sex, bpPercentile, bpType, upperPercentile);
		}
		if (upperAge <=17)
		{
			upperAgeLowerPercentile = getRgrtaDAO().getHighBP(upperAge,
					sex, bpPercentile, bpType, lowerPercentile);
		}
		if (upperAge <=17)
		{
			upperAgeUpperPercentile = getRgrtaDAO().getHighBP(upperAge,
					sex, bpPercentile, bpType, upperPercentile);
		}
		
		Double lowerAgeInter = null;
		
		// prevents division by zero
		Integer denominator = (upperPercentile - lowerPercentile);
		Double firstOperand = (heightPercentile - lowerPercentile);

		if (denominator == 0)
		{
			firstOperand = 0.0;
		} else
		{
			firstOperand = firstOperand / denominator;
		}
		
		//interpolate lowerAgeLowerPercentile and lowerAgeUpperPercentile
		if(lowerAgeLowerPercentile != null && lowerAgeUpperPercentile != null){
					
			lowerAgeInter = (firstOperand * (lowerAgeUpperPercentile - lowerAgeLowerPercentile))
					+ lowerAgeLowerPercentile;
		}
		
		Double upperAgeInter = null;
		
		//interpolate upperAgeLowerPercentile and upperAgeUpperPercentile
		if (upperAgeLowerPercentile != null && upperAgeUpperPercentile != null)
		{
			upperAgeInter = (firstOperand * (upperAgeUpperPercentile - upperAgeLowerPercentile))
					+ upperAgeLowerPercentile;
		}
		
		double fractionalAge = Util.getFractionalAgeInUnits(patient.getBirthdate(), onDate,Util.YEAR_ABBR);
		
		double fraction = fractionalAge - lowerAge;
		
		if(lowerAgeInter == null && upperAgeInter == null){
			if(lowerAgeUpperPercentile != null&&upperAgeUpperPercentile != null){
				upperAgeInter = upperAgeUpperPercentile.doubleValue();
				lowerAgeInter = lowerAgeUpperPercentile.doubleValue();
			}
			if(lowerAgeLowerPercentile != null&&upperAgeLowerPercentile != null){
				upperAgeInter = upperAgeLowerPercentile.doubleValue();
				lowerAgeInter = lowerAgeLowerPercentile.doubleValue();
			}
		}
		
		if(lowerAgeInter == null){
			return Util.round(upperAgeInter,2);
		}
		
		if(upperAgeInter == null){
			return Util.round(lowerAgeInter,2);
		}
		
		return Util.round(lowerAgeInter+((upperAgeInter-lowerAgeInter)*fraction),2);
		}

		public String getDDSTLeaf(String category, Integer ageInDays){
			return getRgrtaDAO().getDDSTLeaf(category, ageInDays);
		}
		
		public List<Statistics> getStatsByEncounterForm(Integer encounterId,String formName){
			return getRgrtaDAO().getStatsByEncounterForm(encounterId, formName);
		}

		public List<Statistics> getStatsByEncounterFormNotPrioritized(Integer encounterId,String formName){
			return getRgrtaDAO().getStatsByEncounterFormNotPrioritized(encounterId, formName);
		}
	
		
		public RgrtaHL7Export insertEncounterToHL7ExportQueue(RgrtaHL7Export export) {
			getRgrtaDAO().insertEncounterToHL7ExportQueue(export);
			return export;
		}

		public List<RgrtaHL7Export> getPendingHL7Exports() {
			return getRgrtaDAO().getPendingHL7Exports();
			
		}
		
		public RgrtaHL7Export getNextPendingHL7Export(String resend, String resendNoAck) {
			return getRgrtaDAO().getNextPendingHL7Export(resend, resendNoAck);
			
		}

		public void saveRgrtaHL7Export(RgrtaHL7Export export) {

			getRgrtaDAO().saveRgrtaHL7Export(export);
			
		}
	
		public List<RgrtaHL7Export> getPendingHL7ExportsByEncounterId(Integer encounterId) {
			return getRgrtaDAO().getPendingHL7ExportsByEncounterId(encounterId);
		}
	
		public List<PatientState> getReprintRescanStatesByEncounter(Integer encounterId, Date optionalDateRestriction, 
				Integer locationTagId,Integer locationId){
			return getRgrtaDAO().getReprintRescanStatesByEncounter(encounterId, optionalDateRestriction, locationTagId,locationId);
		}
		
		public List<String> getPrinterStations(Location location){
			String locationTagAttributeName = "ActivePrinterLocation";
			ChirdlUtilService chirdlUtilService = Context.getService(ChirdlUtilService.class);

			Set<LocationTag> tags = location.getTags();
			List<String>  stationNames = new ArrayList<String>();
			for (LocationTag tag : tags){
				LocationTagAttributeValue locationTagAttributeValue = 
					chirdlUtilService.getLocationTagAttributeValue(tag
						.getLocationTagId(), locationTagAttributeName,location.getLocationId());
				if (locationTagAttributeValue != null)
				{
					String activePrinterLocationString = locationTagAttributeValue
							.getValue();
					//only display active printer locations
					if (activePrinterLocationString.equalsIgnoreCase("true"))
					{
						stationNames.add(tag.getTag());
					}
				}
				
			}
			Collections.sort(stationNames);
			
			return stationNames;
		}
		
		
		public void  saveHL7ExportMap (RgrtaHL7ExportMap map){
			
			getRgrtaDAO().saveHL7ExportMap(map);
			
		}
		
		public RgrtaHL7ExportMap getRgrtaExportMapByQueueId(Integer queue_id){
			return getRgrtaDAO().getRgrtaExportMapByQueueId(queue_id);
		}
		
		public RgrtaHL7ExportStatus getRgrtaExportStatusByName (String name){
			return getRgrtaDAO().getRgrtaExportStatusByName(name);
		}
		
		public RgrtaHL7ExportStatus getRgrtaExportStatusById (Integer id){
			return getRgrtaDAO().getRgrtaExportStatusById( id);
		}
}