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

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/18 14:50
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListCiEntityForSelectApi extends PrivateApiComponentBase {
    @Resource
    private CiMapper ciMapper;
    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getToken() {
        return "cmdb/cientity/list/forselect";
    }

    @Override
    public String getName() {
        return "nmcac.listcientityforselectapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciName", type = ApiParamType.STRING, isRequired = true, desc = "term.cmdb.ciname"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "nmcac.listcientityforselectapi.input.param.desc.defaultvalue"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "ocommn.isneedpage")
    })
    @Output({
            @Param(name = "tbodyList", explode = CiEntityVo[].class, desc = "nmcac.listcientityforselectapi.output.param.desc")
    })
    @Description(desc = "nmcac.listcientityforselectapi.description.desc")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiEntityVo ciEntityVo = paramObj.toJavaObject(CiEntityVo.class);
        String ciName = ciEntityVo.getCiName();
        CiVo ciVo = ciMapper.getCiByName(ciName);
        if (ciVo == null) {
            throw new CiNotFoundException(ciName);
        }
        JSONArray defaultValue = ciEntityVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
            ciEntityVo.setPageSize(ciEntityList.size());
            ciEntityVo.setRowNum(ciEntityList.size());
            return TableResultUtil.getResult(ciEntityList, ciEntityVo);
        }
        int rowNum = ciEntityMapper.getCiEntityIdCountByCiIdAndKeyword(ciVo.getId(), ciEntityVo.getKeyword());
        if (rowNum > 0) {
            ciEntityVo.setRowNum(rowNum);
            ciEntityVo.setCiId(ciVo.getId());
            List<Long> idList = ciEntityMapper.getCiEntityIdByCiId(ciEntityVo);
            List<CiEntityVo> ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
            return TableResultUtil.getResult(ciEntityList, ciEntityVo);
        }
        return TableResultUtil.getResult(new ArrayList<>(), ciEntityVo);
    }
}
