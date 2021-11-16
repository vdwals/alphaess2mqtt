
package app.controllers;

import app.models.api.SummeryDto;
import app.services.SummeryService;
import com.google.inject.Inject;
import org.javalite.common.JsonHelper;
import org.springframework.http.HttpStatus;

/**
 * @author Stefano Crespi
 */
public class SummeryController
	extends APIController
{

	@Inject
	private SummeryService summeryService;

	public void index() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(JsonHelper.toJsonString(summery));
	}
	
	public void carbon() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(JsonHelper.toJsonString(summery.getCarbonNum()));
	}
	
	public void pvToday() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(JsonHelper.toJsonString(summery.getEpvtoday()));
	}
	public void treeNum() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(JsonHelper.toJsonString(summery.getTreeNum()));
	}
	
	private void respondWithJson(String s) {
		respond(s).contentType(getContentType()).status(HttpStatus.OK.value());
	}
}
