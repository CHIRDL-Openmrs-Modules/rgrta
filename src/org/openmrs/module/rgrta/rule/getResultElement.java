package org.openmrs.module.rgrta.rule;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.logic.LogicContext;
import org.openmrs.logic.LogicException;
import org.openmrs.logic.LogicService;
import org.openmrs.logic.Rule;
import org.openmrs.logic.result.Result;
import org.openmrs.logic.result.Result.Datatype;
import org.openmrs.logic.rule.RuleParameterInfo;

/**
 * 
 * Calculates a person's age in years based from their date of birth to the
 * index date
 * 
 */
public class getResultElement implements Rule
{

	private LogicService logicService = Context.getLogicService();

	/**
	 * @see org.openmrs.logic.rule.Rule#eval(org.openmrs.Patient,
	 *      org.openmrs.logic.LogicCriteria)
	 */
	public Result eval(LogicContext context, Patient patient,
			Map<String, Object> parameters) throws LogicException
	{
		
		Integer index = null;
		List<Result> results = null;
		Result distinctResult = null;
	
		if(parameters != null)
		{
			Object param1Obj = parameters.get("param1");
			if (param1Obj != null){
				index = Integer.parseInt((String)param1Obj);
			}
			
			results = (List<Result>) parameters.get("param2");
		}
		
		if (index != null && results != null && index < results.toArray().length)
		{
			distinctResult = (Result) results.toArray()[index];
		
			
		}
		if (distinctResult != null && distinctResult.getResultDate() != null){
			Obs distinctResultObs = (Obs) distinctResult.getResultObject();
			String txt = distinctResultObs.getValueText();
			Double num = distinctResultObs.getValueNumeric();
			
			if (num != null && !String.valueOf(num).equalsIgnoreCase("null")){
				distinctResultObs.setValueText(String.valueOf(distinctResultObs.getValueNumeric()));
			} else {
				distinctResultObs.setValueText(txt);
			}
			
			return new Result(distinctResultObs);
		}
		
		Result result = Result.emptyResult(); 
		
		
		return result;
	}

	/**
	 * @see org.openmrs.logic.rule.Rule#getParameterList()
	 */
	public Set<RuleParameterInfo> getParameterList()
	{
		return null;
	}

	/**
	 * @see org.openmrs.logic.rule.Rule#getDependencies()
	 */
	public String[] getDependencies()
	{
		return new String[]
		{};
	}

	/**
	 * @see org.openmrs.logic.rule.Rule#getTTL()
	 */
	public int getTTL()
	{
		return 0; 
	}

	/**
	 * @see org.openmrs.logic.rule.Rule#getDatatype(String)
	 */
	public Datatype getDefaultDatatype()
	{
		return Datatype.NUMERIC;
	}

}
