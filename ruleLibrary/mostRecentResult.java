package org.openmrs.module.rgrta.rule;

import java.util.Map;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
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
    	Obs obs = null;
    	int i = 1;
		Object paramObj = "";
		String stringValue = "";
		ConceptService conceptService = Context.getConceptService();
		
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
    					obs = ((Obs) result.getResultObject());
    					if (obs != null){
    						obs.getConcept().setDatatype(conceptService.getConceptDatatypeByName("Text"));
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
			return Result.emptyResult();
		}
		if (finalResult.toString() == null) {
			obs.getConcept().setDatatype(conceptService.getConceptDatatypeByName("Text"));
			String value = obs.getValueAsString(Context.getLocale());
			Double num = obs.getValueNumeric();
			String txt = obs.getValueText();
			value = "Not Available";
				//check if numeric
			if (  obs.getValueText() != null  && !obs.getValueText().trim().equalsIgnoreCase("") 
					&& !obs.getValueText().trim().equalsIgnoreCase("null")){
				value = obs.getValueText();
			} else {
				value = obs.getValueNumeric().toString();
			}
			
			finalResult.setValueText(value);
			
		} else {
			if (finalResult.toString().trim().equalsIgnoreCase("")){
				finalResult.setValueText("Not Available");
			}
		}
		/*if (finalResult.toString()== null){
			
			if (finalResult.getDatatype() == Datatype.NUMERIC){
				finalResult.setValueText(String.valueOf(finalResult.toNumber()));
			}
			else {
			String testString = "";
				String str = finalResult.toString();
				String.value
				if (str == ""){
					str = "Not Available";
					
				}
				finalResult.setValueText(str);
				
			}
			
			
		}*/
		
		return finalResult;
	}
	
}