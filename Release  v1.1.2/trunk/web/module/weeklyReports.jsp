<%@ include file="/WEB-INF/template/include.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ page  import="org.openmrs.web.WebConstants" %>
<%
	pageContext.setAttribute("msg", session.getAttribute(WebConstants.OPENMRS_MSG_ATTR));
	pageContext.setAttribute("msgArgs", session.getAttribute(WebConstants.OPENMRS_MSG_ARGS));
	pageContext.setAttribute("err", session.getAttribute(WebConstants.OPENMRS_ERROR_ATTR));
	pageContext.setAttribute("errArgs", session.getAttribute(WebConstants.OPENMRS_ERROR_ARGS));
	session.removeAttribute(WebConstants.OPENMRS_MSG_ATTR);
	session.removeAttribute(WebConstants.OPENMRS_MSG_ARGS);
	session.removeAttribute(WebConstants.OPENMRS_ERROR_ATTR);
	session.removeAttribute(WebConstants.OPENMRS_ERROR_ARGS);
%>

<html  style="height:100%;"  xmlns="http://www.w3.org/1999/xhtml">
	<head >
		<openmrs:htmlInclude file="/openmrs.css" />
		<openmrs:htmlInclude file="/style.css" />
		<openmrs:htmlInclude file="/openmrs.js" />
			<title>CHICA Greaseboard</title>
		
		<script type="text/javascript">
			/* variable used in js to know the context path */
			var openmrsContextPath = '${pageContext.request.contextPath}';
		</script>
		
		<!--  Page Title : '${pageTitle}' 
			OpenMRS Title: <spring:message code="openmrs.title"/>
		-->
		<c:choose>
			<c:when test="${!empty pageTitle}">
				<title>${pageTitle}</title>
			</c:when>
			<c:otherwise>
				<title><spring:message code="openmrs.title"/></title>
			</c:otherwise>
		</c:choose>		
		<SCRIPT LANGUAGE="JavaScript">
		<!-- Idea by:  Nic Wolfe -->
		<!-- This script and many more are available free online at -->
		<!-- The JavaScript Source!! http://javascript.internet.com -->
		function popUp(URL) {
			day = new Date();
			id = day.getTime();
			eval("page" + id + " = window.open(URL, '" + id + "', 'toolbar=0,scrollbars=0,location=0,statusbar=0,menubar=0,resizable=0,width=250,height=250,left = 312,top = 284');");
		}

		function pagerPopUp(URL) {
			window.open(URL, '', 'toolbar=0, scrollbars=0,location=0,locationbar=0,statusbar=0,menubar=0,resizable=0,width=400,height=100,left = 312,top = 284');
		}
		
		function lookupPatient(){
			document.location.href = "viewPatient.form";
			return false;
		}

                function popupfull(url) 
                {
 		params  = 'width='+screen.width;
 		params += ', height='+screen.height;
 		params += ', top=0, left=0'
 		params += ', fullscreen=no';

 		newwin=window.open(url,'windowname4', params);
 		if (window.focus) {newwin.focus()}
 		return false;
 		}
		</script>
 
</head>

<body  style="height:100%;" >
<link href="${pageContext.request.contextPath}/moduleResources/chica/chica.css" type="text/css" rel="stylesheet" />
<table class="reportTable" cellspacing="0">
<tr>
<th>
<form name="input" action="weeklyReports.form" method="post">
<select name="locationName"  onchange="this.form.submit();">
<option>&lt;choose a location&gt;</option>
<c:forEach items="${locations}" var="location">
<option <c:if test="${locationName == location.name}">selected</c:if> value="${location.name}">${location.name}</option>
</c:forEach>
</select>
</form>
</th>
<c:forEach items="${asthmaPrintedMap}" var="item">
<th>${item.key }</th>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;asthma&nbsp;Printed</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${item.value.data }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;asthma&nbsp;scanned</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaScannedMap[item.key].data }</td>
</c:forEach>
</tr>
<tr bgcolor="yellow">
<td style="text-align:left">%&nbsp;of&nbsp;asthma&nbsp;Scanned</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaPercentScannedMap[item.key].data }&nbsp;%</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;scanned&nbsp;asthma&nbsp;w >=1&nbsp;Box&nbsp;Chked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaScannedAnsweredMap[item.key].data }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;scanned&nbsp;asthma&nbsp;w anything&nbsp;marked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaScannedAnythingMarkedMap[item.key].data }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">%&nbsp;of&nbsp;scanned&nbsp;asthma&nbsp;w >=1&nbsp;Box&nbsp;Chked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaPercentScannedAnsweredMap[item.key].data }&nbsp;%</td>
</c:forEach>
</tr>
<tr bgcolor="yellow">
<td style="text-align:left">%&nbsp;of&nbsp;scanned&nbsp;asthma with&nbsp;anything&nbsp;marked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaPercentScannedAnythingMarkedMap[item.key].data }&nbsp;%</td>
</c:forEach>
</tr>
<tr>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>&nbsp;</td>
</c:forEach>
<td>&nbsp;</td>
</tr>
<tr>
<td style="text-align:left">#&nbsp;diabetes&nbsp;Printed</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesPrintedMap[item.key].data }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;diabetes&nbsp;scanned</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesScannedMap[item.key].data }</td>
</c:forEach>
</tr>
<tr bgcolor="yellow">
<td style="text-align:left">%&nbsp;of&nbsp;diabetes&nbsp;Scanned</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesPercentScannedMap[item.key].data }&nbsp;%</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;scanned&nbsp;diabetes&nbsp;w >=1&nbsp;Box&nbsp;Chked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesScannedAnsweredMap[item.key].data }</td>
</c:forEach>
</tr>

<tr>
<td style="text-align:left">#&nbsp;scanned&nbsp;diabetes&nbsp;w anything&nbsp;marked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesScannedAnythingMarkedMap[item.key].data }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">%&nbsp;of&nbsp;scanned&nbsp;diabetes&nbsp;w >=1 Box&nbsp;Chked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesPercentScannedAnsweredMap[item.key].data }&nbsp;%</td>
</c:forEach>
</tr>
<tr bgcolor="yellow">
<td style="text-align:left">%&nbsp;of&nbsp;scanned&nbsp;diabetes with&nbsp;anything&nbsp;marked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesPercentScannedAnythingMarkedMap[item.key].data }&nbsp;%</td>
</c:forEach>
</tr>
<tr>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>&nbsp;</td>
</c:forEach>
<td>&nbsp;</td>
</tr>
<tr>
<td style="text-align:left">#&nbsp;asthma&nbsp;Questions Printed&nbsp;&&nbsp;Scanned</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaQuestionsScannedMap[item.key].data }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;asthma&nbsp;Questions&nbsp;w >=&nbsp;1&nbsp;Box&nbsp;Chked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaQuestionsScannedAnsweredMap[item.key].data }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">%&nbsp;asthma&nbsp;Prompts&nbsp;w&nbsp;Response</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaPercentQuestionsScannedAnsweredMap[item.key].data }&nbsp;%</td>
</c:forEach>
</tr>
<tr bgcolor="yellow">
<td style="text-align:left">%&nbsp;asthma&nbsp;Prompts&nbsp;w Response&nbsp;-&nbsp;adjusted&nbsp;for&nbsp;blanks</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${asthmaPercentQuestionsScannedAnsweredAdjustedMap[item.key].data  }&nbsp;%</td>
</c:forEach>
</tr>
<tr>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>&nbsp;</td>
</c:forEach>
<td>&nbsp;</td>
</tr>
<tr>
<td style="text-align:left">#&nbsp;diabetes&nbsp;Questions Printed&nbsp;&&nbsp;Scanned</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesQuestionsScannedMap[item.key].data  }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">#&nbsp;diabetes&nbsp;Questions&nbsp;w >=&nbsp;1&nbsp;Box&nbsp;Chked</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesQuestionsScannedAnsweredMap[item.key].data  }</td>
</c:forEach>
</tr>
<tr>
<td style="text-align:left">%&nbsp;diabetes&nbsp;Prompts&nbsp;w&nbsp;Response</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesPercentQuestionsScannedAnsweredMap[item.key].data  }&nbsp;%</td>
</c:forEach>
</tr>
<tr bgcolor="yellow">
<td style="text-align:left">%&nbsp;diabetes&nbsp;Prompts&nbsp;w&nbsp;Response -&nbsp;adjusted&nbsp;for&nbsp;blanks</td>
<c:forEach items="${asthmaPrintedMap}" var="item">
<td>${diabetesPercentQuestionsScannedAnsweredAdjustedMap[item.key].data  }&nbsp;%</td>
</c:forEach>
</tr>
</table>
</body>
</html>