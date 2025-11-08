/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.module.cmdb.constvalue.JobSource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class DiscoveryJobSourceHandler implements IAutoexecJobSource {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getValue() {
        return JobSource.DISCOVERY.getValue();
    }

    @Override
    public String getText() {
        return JobSource.DISCOVERY.getText();
    }

    @Override
    public List<AutoexecJobRouteVo> getListByUniqueKeyList(List<String> uniqueKeyList) {
        if (CollectionUtils.isEmpty(uniqueKeyList)) {
            return null;
        }
        List<Long> idList = new ArrayList<>();
        for (String str : uniqueKeyList) {
            idList.add(Long.valueOf(str));
        }
        List<JSONObject> list = mongoTemplate.find(new Query(Criteria.where("id").in(idList)), JSONObject.class, "_discovery_conf");
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<AutoexecJobRouteVo> resultList = new ArrayList<>();
        for (JSONObject jsonObj : list) {
            Long id = jsonObj.getLong("id");
            String name = jsonObj.getString("name");
            JSONObject config = new JSONObject();
            config.put("id", id);
            resultList.add(new AutoexecJobRouteVo(id, name, config));
        }
        return resultList;
    }
}
