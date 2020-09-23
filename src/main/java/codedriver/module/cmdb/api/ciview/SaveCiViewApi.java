package codedriver.module.cmdb.api.ciview;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dto.ci.CiViewVo;
import codedriver.module.cmdb.exception.ci.CiAuthException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;

@Service
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
        desc = "显示设置数据，格式{\"itemId\":属性或关系id,\"itemName\":\"属性或关系名称\",\"showType\":\"all|list|detail|none\",\"type\":\"attr|rel\"}")})
    @Description(desc = "保存模型显示设置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        boolean hasAuth = AuthActionChecker.check("CI_MODIFY");
        if (!hasAuth) {
            hasAuth = CiAuthChecker.hasCiManagePrivilege(ciId);
        }
        if (!hasAuth) {
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
