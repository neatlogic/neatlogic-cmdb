/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.citype;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.CiTypeMapper;
import codedriver.framework.cmdb.dto.ci.CiTypeVo;
import codedriver.module.cmdb.exception.ci.CiTypeIsExistsException;
import codedriver.module.cmdb.exception.ci.CiTypeNameIsBlankException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAllCiTypeApi extends PrivateApiComponentBase {

    @Autowired
    private CiTypeMapper ciTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/citype/saveall";
    }

    @Override
    public String getName() {
        return "批量保存模型类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciTypeList", isRequired = true, explode = CiTypeVo[].class, type = ApiParamType.JSONARRAY,
        desc = "模型类型列表")})
    @Description(desc = "批量保存模型类型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray ciTypeList = jsonObj.getJSONArray("ciTypeList");
        int sort = 1;
        for (int i = 0; i < ciTypeList.size(); i++) {
            JSONObject ciTypeObj = ciTypeList.getJSONObject(i);
            CiTypeVo ciTypeVo = JSONObject.toJavaObject(ciTypeObj, CiTypeVo.class);
            if (ciTypeObj.getBooleanValue("isDeleted")) {
                ciTypeMapper.deleteCiTypeById(ciTypeVo.getId());
            } else {
                if (StringUtils.isBlank(ciTypeVo.getName())) {
                    throw new CiTypeNameIsBlankException();
                }
                if (ciTypeMapper.checkCiTypeNameIsExists(ciTypeVo) > 0) {
                    throw new CiTypeIsExistsException(ciTypeVo.getName());
                }
                ciTypeVo.setSort(sort);
                ciTypeMapper.updateCiType(ciTypeVo);
                sort += 1;
            }
        }
        return null;
    }
}
