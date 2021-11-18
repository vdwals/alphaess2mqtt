package app.controllers;

import app.models.api.RunningDataDto;
import app.services.injections.IRunningDataService;
import com.google.inject.Inject;

/**
 * @author Stefano Crespi
 */
public class RunningDataController extends APIController {
    
    @Inject private IRunningDataService runningDataService;
    
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
    
    public void ppv4() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPpv4());
    }
    
    public void ppvSum() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(getPpvSum(runningData));
    }
    
    private double getPpvSum(RunningDataDto runningData) {
        return runningData.getPpv1() + runningData.getPpv2() + runningData.getPpv3() +
               runningData.getPpv4();
    }
    
    public void pMeter1() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPmeter_l1());
    }
    
    public void pMeter2() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPmeter_l2());
    }
    
    public void pMeter3() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPmeter_l3());
    }
    
    public void pMeterSum() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(getGridPower(runningData));
    }
    
    private double getGridPower(RunningDataDto runningData) {
        return runningData.getPmeter_l1() + runningData.getPmeter_l2() + runningData.getPmeter_l3();
    }
    
    public void pMeterDc() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPmeter_dc());
    }
    
    public void soc() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getSoc());
    }
    
    public void pBat() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPbat());
    }
    
    public void varAc() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getVarac());
    }
    
    public void varDc() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getVardc());
    }
    
    public void pReal1() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPreal_l1());
    }
    
    public void pReal2() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPreal_l2());
    }
    
    public void pReal3() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(runningData.getPreal_l3());
    }
    
    public void pRealSum() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(
                runningData.getPreal_l1() + runningData.getPreal_l2() + runningData.getPreal_l3());
    }
    
    public void pvTotalPower() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(getPvTotalPower(runningData));
    }
    
    private double getPvTotalPower(RunningDataDto runningData) {
        return getPpvSum(runningData) + runningData.getPmeter_dc();
    }
    
    public void totalPowerConsumption() {
        RunningDataDto runningData = runningDataService.getRunningData();
        respondWithJson(
                getPvTotalPower(runningData) + getGridPower(runningData) + runningData.getPbat());
    }
}
