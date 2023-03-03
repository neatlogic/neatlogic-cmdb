/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.tagent.register.handler;

import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.framework.tagent.register.core.AfterRegisterBase;
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


    /**
     * 注册后会送一条数据进mongodb，如果已存在不在写入，用于tagent注册时即使模型还没配置，也能在后续同步时写入配置库
     *
     * @param tagentVo tagent对象
     */
    @Override
    public void myExecute(TagentVo tagentVo) {
        if (StringUtils.isNotBlank(tagentVo.getOsType()) && StringUtils.isNotBlank(tagentVo.getIp())) {
            Criteria criteria = new Criteria();
            criteria.andOperator(Criteria.where("MGMT_IP").is(tagentVo.getIp()));
            Query query = new Query(criteria);
            JSONObject oldData = mongoTemplate.findOne(query, JSONObject.class, "COLLECT_OS");
            JSONObject dataObj = new JSONObject();
            dataObj.put("_OBJ_CATEGORY", "OS");
            dataObj.put("_OBJ_TYPE", tagentVo.getOsType());
            dataObj.put("OS_TYPE", tagentVo.getOsType());
            dataObj.put("MGMT_IP", tagentVo.getIp());
            dataObj.put("CPU_ARCH", tagentVo.getOsbit());
            dataObj.put("HOSTNAME", tagentVo.getName());
            dataObj.put("VERSION", tagentVo.getOsVersion());
            if (oldData == null) {
                mongoTemplate.insert(dataObj, "COLLECT_OS");
            }
        }
    }
}
