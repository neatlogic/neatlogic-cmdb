/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.tagent.register.handler;

import codedriver.framework.tagent.dto.TagentVo;
import codedriver.framework.tagent.register.core.AfterRegisterBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/*
添加tagent数据进mongodb
 */
@Service
public class AddCollectionHandler extends AfterRegisterBase {
    @Resource
    private MongoTemplate mongoTemplate;


    @Override
    public void myExecute(TagentVo tagentVo) {
        if (StringUtils.isNotBlank(tagentVo.getOsType()) && StringUtils.isNotBlank(tagentVo.getIp())) {
            Criteria criteria = new Criteria();
            criteria.andOperator(Criteria.where("MGMT_IP").is(tagentVo.getIp()), Criteria.where("OS_TYPE").is(tagentVo.getOsType()));
            JSONObject oldData = mongoTemplate.findOne(new Query(criteria), JSONObject.class, "COLLECT_OS");
            if (oldData == null) {
                JSONObject dataObj = new JSONObject();
                dataObj.put("OS_TYPE", tagentVo.getOsType());
                dataObj.put("MGMT_IP", tagentVo.getIp());
                dataObj.put("CPU_BITS", tagentVo.getOsbit());
                dataObj.put("HOSTNAME", tagentVo.getName());
                dataObj.put("VERSION", tagentVo.getOsVersion());
                mongoTemplate.insert(dataObj, "COLLECT_OS");
            }
        }
    }
}
