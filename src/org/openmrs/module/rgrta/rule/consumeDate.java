package org.openmrs.module.rgrta.rule;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicContext;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.logic.LogicException;
import org.openmrs.logic.LogicService;
import org.openmrs.logic.Rule;
import org.openmrs.logic.result.Result;
import org.openmrs.logic.result.Result.Datatype;
import org.openmrs.logic.rule.RuleParameterInfo;
import org.openmrs.module.atd.hibernateBeans.FormInstance;
import org.openmrs.module.rgrta.util.Util;

public class consumeDate implements Rule
{
	private LogicService logicService = Context.getLogicService();
	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * *
	 * 
	 * @see org.openmrs.logic.rule.Rule#getParameterList()
	 */
	public Set<RuleParameterInfo> getParameterList()
	{
		return null;
	}

	/**
	 * *
	 * 
	 * @see org.openmrs.logic.rule.Rule#getDependencies()
	 */
	public String[] getDependencies()
	{
		return new String[]
		{};
	}

	/**
	 * *
	 * 
	 * @see org.openmrs.logic.rule.Rule#getTTL()
	 */
	public int getTTL()
	{
		return 0; // 60 * 30; // 30 minutes
	}

	/**
	 * *
	 * 
	 * @see org.openmrs.logic.rule.Rule#getDatatype(String)
	 */
	public Datatype getDefaultDatatype()
	{
		return Datatype.CODED;
	}

	public Result eval(LogicContext context, Patient patient,
			Map<String, Object> parameters) throws LogicException
	{
		FormInstance formInstance = null;
		String fieldName = null;
		String conceptName  = null;
		Integer encounterId = null;
		Integer ruleId = null;
		Integer locationTagId = null;

		if (parameters != null)
		{
			formInstance = (FormInstance) parameters.get("formInstance");
			fieldName = (String) parameters.get("fieldName");
			conceptName = (String) parameters.get("concept");
			ruleId = (Integer) parameters.get("ruleId");
			encounterId = (Integer) parameters.get("encounterId");
			locationTagId = (Integer) parameters.get("locationTagId");

			if(conceptName == null)
			{
				return Result.emptyResult();
			}
			
		}

		if (formInstance == null)
		{
			throw new LogicException(
					"The xml datasource requires a formInstanceId");
		}

		LogicCriteria formIdCriteria = new LogicCriteria("formInstance").equalTo(formInstance);

		LogicCriteria fieldNameCriteria = new LogicCriteria(fieldName);
		formIdCriteria = formIdCriteria.and(fieldNameCriteria);

		Result ruleResult = context.read(patient, this.logicService
				.getLogicDataSource("xml"), formIdCriteria);
		
		if (ruleResult == null) {
			
			return Result.emptyResult();
			
		}
		String resultString = ruleResult.toString();
		String resultDateString = resultString.replaceAll(" ","");
		if (resultDateString.equalsIgnoreCase("//")|| resultDateString.equalsIgnoreCase("")){
			return Result.emptyResult();
		}
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
		Date resultDate = null;
		String obsValue = null;
	    
	    //Use Java util to check date validity
		try {
			
			resultDate = sdf.parse(resultDateString);
			
			
		} catch (ParseException e) {
			
				log.error("Error parsing date for consume field " + resultDateString + ". Save date as string");
				obsValue = resultDateString;
		} 
		
		//Date is valid, obsValue is still null.
		//Convert back to string of format YYYY-MM-DD needed by database to convert
		//date in saveObs()
		
		if (obsValue == null){
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			try {
				obsValue = formatter.format(resultDate);
			} catch (Exception e) {
				obsValue = resultDateString;
			}
		}
		 
		ConceptService conceptService = Context.getConceptService();
		Util.saveObs(patient, conceptService.getConceptByName(conceptName),
						encounterId, obsValue,formInstance,
						ruleId,locationTagId);
	
		
		return Result.emptyResult();
	}
}