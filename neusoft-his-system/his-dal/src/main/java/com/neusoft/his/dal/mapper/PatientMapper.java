package com.neusoft.his.dal.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.his.dal.entity.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {

    @Select("SELECT * FROM patient WHERE id_card = #{idCard}")
    Patient selectByIdCard(String idCard);
}
