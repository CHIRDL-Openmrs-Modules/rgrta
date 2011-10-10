package org.openmrs.module.rgrta.rule;

import java.util.ArrayList;
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
import org.openmrs.module.chirdlutil.util.Util;

public class mostRecentResultWithAnswer implements Rule
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
	   // System.out.println("mostRecentResultWithAnswer() parameters: "+parameters);
	    int i = 1;
    	String answer = (String) parameters.get("param" + i);
    	Result results = Result.emptyResult();
    	List<Result>  list = new ArrayList<Result>();
    	Obs finalResult = null;
    	i++;
    	
		Object paramObj = parameters.get("param"+i);

		while(paramObj != null){
			paramObj = parameters.get("param"+i);
			i++;
			if(paramObj instanceof Result){
				results = (Result) paramObj;
			}else{
				continue;
			}
			if(results != null){
				for(Result result:results){
				String name = ((Obs) result.getResultObject()).getValueCoded().getName().getName();
					if(answer != null&&answer.equalsIgnoreCase(name)){
						Obs matchedResult = (Obs) result.getResultObject();
						if(finalResult==null||
								matchedResult.getObsDatetime().compareTo(finalResult.getObsDatetime())>0){
							finalResult = matchedResult;
						}
					}
				}
			}
		}
		
		if(finalResult == null){
			return Result.emptyResult();
		} 
		
		String name = finalResult.getValueCoded().getName().getName();
		String titleCaseName = Util.toProperCase(name);
		System.out.println("title case" + titleCaseName);
		finalResult.getConcept().getName(Context.getLocale()).setName(titleCaseName);
		
		Result finalres = new Result(finalResult);
		list.add(finalres);
		finalres.setResultDate(finalResult.getObsDatetime());
		finalres.setValueText("Not available");
		
		System.out.println("mostRecentResultWithAnswer() FINALresult name: "+ finalResult.getConcept().getName().toString());
		System.out.println("mostRecentResultWithAnswer() FINALresult date: " + finalResult.getObsDatetime().toString());
		
		return new Result(list);
	}
	
}