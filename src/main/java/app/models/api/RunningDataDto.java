package app.models.api;

import lombok.Value;

@Value
public class RunningDataDto {
    String  _id;
    String  createtime;
    Integer ev1_chgenergy_real;
    Integer ev1_mode;
    Integer ev1_power;
    Integer ev2_chgenergy_real;
    Integer ev2_mode;
    Integer ev2_power;
    Integer ev3_chgenergy_real;
    Integer ev3_mode;
    Integer ev3_power;
    Integer ev4_chgenergy_real;
    Integer ev4_mode;
    Integer ev4_power;
    Integer factory_flag;
    Integer pbat;
    Integer pmeter_dc;
    Integer pmeter_l1;
    Integer pmeter_l2;
    Integer pmeter_l3;
    Integer poc_meter_l1;
    Integer poc_meter_l2;
    Integer poc_meter_l3;
    Integer ppv1;
    Integer ppv2;
    Integer ppv3;
    Integer ppv4;
    Integer preal_l1;
    Integer preal_l2;
    Integer preal_l3;
    String  sn;
    Double  soc;
    Integer sva;
    String  uploadtime;
    Integer varac;
    Integer vardc;
}
