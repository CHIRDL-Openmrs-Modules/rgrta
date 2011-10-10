package org.openmrs.module.rgrta.rule;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.openmrs.Concept;
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

public class mostRecentResult implements Rule
{
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
		
    	Result results = Result.emptyResult();
    	Result finalResult = null;
    	int i = 1;
		Object paramObj = "";
		Obs tempObs = null;
		Obs finalobs = null;
		
		while(paramObj != null){
    		paramObj = parameters.get("param"+i);
			if(paramObj instanceof Result){
	    		results = (Result) parameters.get("param"+(i));
	    		
			}else{
				continue;
			}
    		if(results != null){
    			for(Result result:results){
    				if (result != null && result.getResultObject() != null) {
    					tempObs = ((Obs) result.getResultObject());
    					if (tempObs != null){
    						Concept concept = ((Obs) result.getResultObject()).getValueCoded(); 
	    				}
    				}
    		
    				 if(finalResult == null||
    						result.getResultDate().compareTo(finalResult.getResultDate())>0){
    						finalResult = result;
    				 }
    			}	
    		} 
    		i++;
    	}
		if(finalResult == null){
			finalResult = Result.emptyResult();
			return finalResult;
		}
		
		finalobs = ((Obs) finalResult.getResultObject());
		
		
		if (finalResult.toString()== null){
			Double numeric = ((Obs) finalResult.getResultObject()).getValueNumeric();
			
			String txt = ((Obs) finalResult.getResultObject()).getValueText();
			if (numeric == null  || String.valueOf(numeric).equalsIgnoreCase("null") 
					|| String.valueOf(numeric).equalsIgnoreCase("")){
				if (txt == null || txt.trim().equalsIgnoreCase("") || txt.equalsIgnoreCase("null")){
				((Obs) finalResult.getResultObject()).setValueText("Not Available");
					
				} else {
					finalResult.setValueText(txt);
				}
	
			} else {
				finalResult.setValueNumeric(numeric);
				//numeric not null
				finalResult.setValueText(String.valueOf(numeric));
			}
			
			
		} else {
			Double numeric = ((Obs) finalResult.getResultObject()).getValueNumeric();
			String txt = ((Obs) finalResult.getResultObject()).getValueText();
			if (numeric == null){
				if (txt == null || txt.trim().equalsIgnoreCase("") || txt.equalsIgnoreCase("null")){
					finalResult.setValueText("Not Available");
					((Obs) finalResult.getResultObject()).setValueText("Not Available");
				
					
				} else {
			
					finalResult.setValueText(txt);
		
				}
			} else {
				finalResult.setValueNumeric(numeric);
				
				
			}
		}
		
		return  new Result(finalResult);
	}
	
}