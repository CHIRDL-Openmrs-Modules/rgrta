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
		
		ATDService atdService = Context.getService(ATDService.class);
		RgrtaService rgrtaService = Context.getService(RgrtaService.class);
		Integer formInstanceId = null;
		
		
	
		DssService dssService = Context.getService(DssService.class);
		
		String conceptName = (String) parameters.get("param1");
		String valueName = (String) parameters.get("param2");	
		String ruleName = (String) parameters.get("param3");

		
		List<org.openmrs.module.dss.hibernateBeans.Rule> rules = dssService.getNonPrioritizedRules("Diabetes_Care_Worksheet");
		for (org.openmrs.module.dss.hibernateBeans.Rule rule : rules){
			String tokenName = ((org.openmrs.module.dss.hibernateBeans.Rule) rule).getTokenName();
			if (tokenName != null && ruleName != null && tokenName.equalsIgnoreCase(ruleName)){
			Integer ruleId = rule.getRuleId();
			parameters.put("ruleId", ruleId);
			}
			
		}
		logicService.eval(patient, "storeObs",parameters);
		
		return Result.emptyResult();
		
	}
}
	
	


