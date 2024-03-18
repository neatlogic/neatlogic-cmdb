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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.sync.CollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncCiCollectionVo;
import neatlogic.framework.cmdb.dto.sync.SyncFieldVo;
import neatlogic.framework.cmdb.exception.sync.CollectionNotFoundException;
import neatlogic.framework.cmdb.exception.sync.SyncCiCollectionNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.sync.SyncMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetConditionFieldApi extends PrivateApiComponentBase {
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SyncMapper syncMapper;

    @Override
    public String getName() {
        return "nmcas.getconditionfieldapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")})
    @Description(desc = "nmcas.getconditionfieldapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        SyncCiCollectionVo ciCollectionVo = syncMapper.getSyncCiCollectionById(id);
        if (ciCollectionVo == null) {
            throw new SyncCiCollectionNotFoundException(id);
        }
        CollectionVo collectionVo = mongoTemplate.findOne(new Query(Criteria.where("name").is(ciCollectionVo.getCollectionName())), CollectionVo.class, "_dictionary");
        if (collectionVo == null) {
            throw new CollectionNotFoundException(ciCollectionVo.getCollectionName());
        }
        List<SyncFieldVo> fieldList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(collectionVo.getFields())) {
            for (int i = 0; i < collectionVo.getFields().size(); i++) {
                JSONObject fieldObj = collectionVo.getFields().getJSONObject(i);
                SyncFieldVo syncFieldVo = JSONObject.toJavaObject(fieldObj, SyncFieldVo.class);
                //有条件表达式的字段才能作为搜索条件
                if (CollectionUtils.isNotEmpty(syncFieldVo.getExpressionList())) {
                    fieldList.add(syncFieldVo);
                }
            }
        }
        fieldList.sort(Comparator.comparing(SyncFieldVo::getName));
        return fieldList;
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/condition/field/get";
    }
}
