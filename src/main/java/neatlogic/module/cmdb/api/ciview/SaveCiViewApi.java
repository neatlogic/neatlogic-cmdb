/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.ciview;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.exception.ci.CiAuthException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveCiViewApi extends PrivateApiComponentBase {

    @Autowired
    private CiViewMapper ciViewMapper;


    @Override
    public String getToken() {
        return "/cmdb/ciview/save";
    }

    @Override
    public String getName() {
        return "保存模型显示设置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"), @Param(
            name = "ciViewList", type = ApiParamType.JSONARRAY, isRequired = true,
            desc = "显示设置数据，格式{\"itemId\":属性或关系id,\"itemName\":\"属性或关系名称\",\"showType\":\"all|list|detail|none\",\"allowEdit\":1,\"type\":\"attr|rel\"}")})
    @Description(desc = "保存模型显示设置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        if (!CiAuthChecker.chain().checkCiManagePrivilege(ciId).check()) {
            throw new CiAuthException();
        }
        JSONArray ciViewList = jsonObj.getJSONArray("ciViewList");
        ciViewMapper.deleteCiViewByCiId(ciId);
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            for (int i = 0; i < ciViewList.size(); i++) {
                CiViewVo ciViewVo = JSONObject.toJavaObject(ciViewList.getJSONObject(i), CiViewVo.class);
                ciViewVo.setCiId(ciId);
                ciViewVo.setSort(i + 1);
                ciViewMapper.insertCiView(ciViewVo);
            }
        }
        return null;
    }

}
