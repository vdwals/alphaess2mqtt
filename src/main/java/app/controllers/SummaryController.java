
package app.controllers;

import app.models.api.SummaryDto;
import app.services.SummaryService;
import com.google.inject.Inject;
import org.javalite.activeweb.AppController;
import org.javalite.common.JsonHelper;
import org.springframework.http.HttpStatus;

import static app.utils.Tokens.*;

/**
 * @author Stefano Crespi
 */
public class SummaryController
	extends AppController
{

	@Inject
	private SummaryService summaryService;

	public void index() {
		SummaryDto summary = summaryService.getSummary();
		respondWithJson(JsonHelper.toJsonString(summary));
	}
	
	public void carbon() {
		SummaryDto summary = summaryService.getSummary();
		respondWithJson(JsonHelper.toJsonString(summary.getCarbonNum()));
	}
	
	private void respondWithJson(String s) {
		respond(s).contentType(APPLICATION_JSON).status(HttpStatus.OK.value());
	}
}
