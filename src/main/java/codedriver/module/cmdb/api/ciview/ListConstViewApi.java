/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ciview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.view.ViewConstVo;
import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListConstViewApi extends PrivateApiComponentBase {

    @Resource
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/ciview/listconst";
    }

    @Override
    public String getName() {
        return "获取内部属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "showType", type = ApiParamType.ENUM, rule = "all,list,detail", desc = "显示类型")})
    @Output({@Param(explode = ViewConstVo[].class)})
    @Description(desc = "获取内部属性列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String showType = jsonObj.getString("showType");
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        ciViewVo.addShowType(showType);
        ciViewVo.addShowType(ShowType.ALL.getValue());
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        List<ViewConstVo> constList = ciViewMapper.getCiViewConstByCiId(ciId);
        Set<Long> constSet = new HashSet<>();
        for (CiViewVo ciView : ciViewList) {
            if (ciView.getType().equals("const")) {
                constSet.add(ciView.getItemId());
            }
        }
        constList.removeIf(cons -> !constSet.contains(cons.getId()));
        return constList;
    }
}
