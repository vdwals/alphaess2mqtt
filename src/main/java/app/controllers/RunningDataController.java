package app.controllers;

import app.models.api.RunningDataDto;
import app.models.api.SummeryDto;
import app.services.injections.IRunningDataService;
import com.google.inject.Inject;
import org.javalite.common.JsonHelper;
import org.springframework.http.HttpStatus;

/**
 * @author Stefano Crespi
 */
public class RunningDataController extends APIController {
    
    @Inject
    private IRunningDataService runningDataService;
    
    public void index() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData);
    }
    
    public void ppv1() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPpv1());
    }
    
    public void ppv2() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPpv2());
    }
    
    public void ppv3() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPpv3());
    }
    
    public void ppvSum() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(
                runningData.getPpv1() + runningData.getPpv2() + runningData.getPpv3());
    }
}
