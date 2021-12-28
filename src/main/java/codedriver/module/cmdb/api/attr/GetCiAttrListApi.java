/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.attr;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.crossover.IGetCiAttrListApiCrossoverService;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiAttrListApi extends PrivateApiComponentBase implements IGetCiAttrListApiCrossoverService {

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/{ciId}/listattr";
    }

    @Override
    public String getName() {
        return "获取模型属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "显示类型"),
            @Param(name = "allowEdit", type = ApiParamType.ENUM, rule = "1,0", desc = "是否允许编辑"),
            @Param(name = "isSimple", type = ApiParamType.BOOLEAN, rule = "true,false", desc = "是否简单属性")})
    @Output({@Param(name = "Return", explode = AttrVo[].class)})
    @Description(desc = "获取模型属性列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String showType = jsonObj.getString("showType");
        Boolean isSimple = jsonObj.getBoolean("isSimple");
        Integer allowEdit = jsonObj.getInteger("allowEdit");
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        if (StringUtils.isNotBlank(showType)) {
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciId);
            ciViewVo.addShowType(showType);
            ciViewVo.addShowType(ShowType.ALL.getValue());
            List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
            Set<Long> attrSet = new HashSet<>();
            for (CiViewVo ciView : ciViewList) {
                if (ciView.getType().equals("attr")) {
                    attrSet.add(ciView.getItemId());
                }
            }
            attrList.removeIf(attr -> !attrSet.contains(attr.getId()));
        }
        if (allowEdit != null) {
            attrList.removeIf(attr -> (allowEdit.equals(1) && (attr.getAllowEdit() != null && attr.getAllowEdit().equals(0)))
                    || (allowEdit.equals(0) && (attr.getAllowEdit() == null || attr.getAllowEdit().equals(1))));
        }
        if (isSimple != null) {
            attrList.removeIf(attr -> AttrValueHandlerFactory.getHandler(attr.getType()).isSimple() != isSimple);
        }
        return attrList;
    }
}
