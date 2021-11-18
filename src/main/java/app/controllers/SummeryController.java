
package app.controllers;

import app.models.api.SummeryDto;
import app.services.SummeryService;
import app.services.injections.ISummeryService;
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
	private ISummeryService summeryService;

	public void index() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(summery);
	}
	
	public void carbon() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(summery.getCarbonNum());
	}
	
	public void pvToday() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(summery.getEpvtoday());
	}
	public void treeNum() {
		SummeryDto summery = summeryService.getSummary();
		respondWithJson(summery.getTreeNum());
	}
}
