/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.tagent.register.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.tagent.dto.TagentVo;
import neatlogic.framework.tagent.register.core.AfterRegisterBase;
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
