package app.models;

import org.apache.commons.lang3.StringUtils;
import org.javalite.activejdbc.Model;

public class AlphaEssBattery extends Model {
    
    public static final String SN = "sn";
    
    public String getSn() {
        return getString(SN);
    }
}
