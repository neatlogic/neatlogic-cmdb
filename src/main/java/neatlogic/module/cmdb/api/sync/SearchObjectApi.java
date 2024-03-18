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
