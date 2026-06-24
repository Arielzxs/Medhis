package com.neusoft.his.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("medical_record")
public class MedicalRecord extends BaseEntity {
    private Long patientId;
    private Long doctorId;
    private String diagnosis;
    private String treatmentPlan;
    private String archiveFlag;
}
