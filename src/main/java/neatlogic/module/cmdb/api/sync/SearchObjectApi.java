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

package neatlogic.module.cmdb.api.sync;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.framework.cmdb.dto.sync.ObjectVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.sync.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchObjectApi extends PrivateApiComponentBase {

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "nmcas.searchobjectapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid")})
    @Description(desc = "nmcas.searchobjectapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ObjectVo objectVo = JSONObject.toJavaObject(paramObj, ObjectVo.class);
        int rowNum = objectMapper.searchObjectCount(objectVo);
        List<ObjectVo> objectList = new ArrayList<>();
        if (rowNum > 0) {
            objectVo.setRowNum(rowNum);
            objectList = objectMapper.searchObject(objectVo);
        }
        return TableResultUtil.getResult(objectList, objectVo);
    }

    @Override
    public String getToken() {
        return "/cmdb/sync/object/search";
    }
}
