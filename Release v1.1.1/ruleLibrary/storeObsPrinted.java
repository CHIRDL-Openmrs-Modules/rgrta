package org.openmrs.module.rgrta.rule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicContext;
import org.openmrs.logic.LogicException;
import org.openmrs.logic.LogicService;
import org.openmrs.logic.Rule;
import org.openmrs.logic.result.Result;
import org.openmrs.logic.result.Result.Datatype;
import org.openmrs.logic.rule.RuleParameterInfo;
import org.openmrs.module.atd.service.ATDService;
import org.openmrs.module.dss.service.DssService;
import org.openmrs.module.rgrta.service.RgrtaService;

public class storeObsPrinted implements Rule{
	
	private Log log = LogFactory.getLog(this.getClass());
	private LogicService logicService = Context.getLogicService();

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
		
		//System.out.println("begin here" + new Date() );
		ATDService atdService = Context.getService(ATDService.class);
		RgrtaService rgrtaService = Context.getService(RgrtaService.class);
		Integer formInstanceId = null;
		
		
	
		DssService dssService = Context.getService(DssService.class);
		
		String conceptName = (String) parameters.get("param1");
		//System.out.println("concept name is " + conceptName);
		String valueName = (String) parameters.get("param2");
		//System.out.println("value  is " + valueName);
		
		String ruleName = (String) parameters.get("param3");
		//System.out.println("ruleName  is " + ruleName);
		
		List<org.openmrs.module.dss.hibernateBeans.Rule> rules = dssService.getNonPrioritizedRules("Diabetes_Care_Worksheet");
		for (org.openmrs.module.dss.hibernateBeans.Rule rule : rules){
			String tokenName = ((org.openmrs.module.dss.hibernateBeans.Rule) rule).getTokenName();
			//System.out.println("tokenname is " + tokenName);
			if (tokenName != null && ruleName != null && tokenName.equalsIgnoreCase(ruleName)){
			Integer ruleId = rule.getRuleId();
			//System.out.println("ruleId is " + ruleId);
				parameters.put("ruleId", ruleId);
			}
			
		}
		logicService.eval(patient, "storeObs",parameters);
		
		
		/*FormInstance formInstance = (FormInstance) parameters.get("formInstance");
		LocationService locationService = Context.getLocationService();
		Integer formId = null;
		String formName = null;
		Integer ruleId = null;
		Integer locationId = null;
		
		
		if (formInstance != null)
		{
			
			FormService formService = Context.getFormService();
			Form form = formService.getForm(formInstance.getFormId());
			formName = form.getName();
			locationId = formInstance.getLocationId();
		}
		
		if (ruleId != null)
		{
			List<Statistics> statistics = rgrtaService.getStatByIdAndRule(
					formInstanceId, ruleId, formName,locationId);

			if (statistics != null)
			{
				Statistics stat = statistics.get(0);

				if (stat.getObsvId() == null)
				{
					stat.setScannedTimestamp(null);;
					atdService.updateStatistics(stat);
				} 
			}
		} else
		{
			List<Statistics> statistics = rgrtaService.getStatByFormInstance(formInstanceId, formName,locationId);
			Statistics stat = new Statistics();

			stat.setAgeAtVisit(org.openmrs.module.rgrta.util.Util
					.adjustAgeUnits(patient.getBirthdate(), null));
			stat.setEncounterId(encounterId);
			stat.setFormInstanceId(formInstanceId);
			stat.setLocationTagId(locationTagId);
			stat.setFormName(formName);
			stat.setObsvId(obs.getObsId());
			stat.setPatientId(patient.getPatientId());
			stat.setRuleId(ruleId);
			stat.setLocationId(locationId);
			stat.setScannedTimestamp(obsDate);
			String answer = obs.getValueAsString(new Locale("en_US"));
			if (answer == null || answer.trim().equalsIgnoreCase("")){
				answer = obs.getValueText();
			}
			stat.setAnswer(answer);
			
			atdService.createStatistics(stat);
		}
		List<Statistics> statistics = atdService.getsgetStatByFormInstance(formInstanceId, formName,locationId);
		Statistics stat = new Statistics();
		
		stat.setAgeAtVisit(org.openmrs.module.chica.util.Util
				.adjustAgeUnits(patient.getBirthdate(), null));
		stat.setEncounterId(encounterId);
		stat.setFormInstanceId(formInstanceId);
		stat.setLocationTagId(locationTagId);
		stat.setFormName(formName);
		stat.setObsvId(obs.getObsId());
		stat.setPatientId(patient.getPatientId());
		stat.setRuleId(ruleId);
		stat.setLocationId(locationId);
		if(statistics != null&&statistics.size()>0){
			Statistics oldStat = statistics.get(0);
			stat.setPrintedTimestamp(oldStat.getPrintedTimestamp());
			stat.setScannedTimestamp(oldStat.getScannedTimestamp());
		}
		chicaService.createStatistics(stat);*/
		return Result.emptyResult();
		
	}
}
	
	


