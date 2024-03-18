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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CIENTITY_MODIFY;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.exception.attr.AttrNotFoundException;
import neatlogic.framework.cmdb.exception.attr.AttrTargetCiIdNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAttrTargetCiEntityApi extends PrivateApiComponentBase {


    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/attr/targetci/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchattrtargetcientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "nmcaa.getattrapi.input.param.desc.id"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "common.keyword"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "nmcac.searchattrtargetcientityapi.input.param.desc")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchattrtargetcientityapi.output.param.desc")})
    @Description(desc = "nmcac.searchattrtargetcientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long attrId = jsonObj.getLong("attrId");
        String keyword = jsonObj.getString("keyword");
        JSONArray defaultValue = jsonObj.getJSONArray("defaultValue");
        Integer pageSize = jsonObj.getInteger("pageSize");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }

        if (attrVo.getTargetCiId() == null) {
            throw new AttrTargetCiIdNotFoundException(attrVo.getLabel());
        }


        CiEntityVo ciEntityVo = new CiEntityVo();
        if (pageSize != null) {
            ciEntityVo.setPageSize(pageSize);
        }
        ciEntityVo.setCiId(attrVo.getTargetCiId());
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = new ArrayList<>();
            for (int i = 0; i < defaultValue.size(); i++) {
                try {
                    idList.add(defaultValue.getLong(i));
                } catch (Exception ignored) {

                }
            }
            if (CollectionUtils.isNotEmpty(idList)) {
                ciEntityVo.setIdList(idList);
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            ciEntityVo.setName(keyword);
        }
        //不需要多余的属性和关系
        ciEntityVo.setAttrIdList(new ArrayList<Long>() {{
            this.add(0L);
        }});
        ciEntityVo.setRelIdList(new ArrayList<Long>() {{
            this.add(0L);
        }});
        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        JSONArray jsonList = new JSONArray();
        for (CiEntityVo ciEntity : ciEntityList) {
            JSONObject obj = new JSONObject();
            obj.put("id", ciEntity.getId());
            obj.put("name", StringUtils.isNotBlank(ciEntity.getName()) ? ciEntity.getName() : "-");
            jsonList.add(obj);
        }
        return jsonList;
    }

}
