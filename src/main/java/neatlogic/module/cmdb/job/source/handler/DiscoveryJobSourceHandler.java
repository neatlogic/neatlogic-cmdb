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
