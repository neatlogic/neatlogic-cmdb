package codedriver.module.cmdb.api.citype;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.CiTypeMapper;
import codedriver.module.cmdb.dto.ci.CiTypeVo;
import codedriver.module.cmdb.exception.ci.CiTypeIsExistsException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiTypeApi extends PrivateApiComponentBase {

    @Autowired
    private CiTypeMapper ciTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/citype/save";
    }

    @Override
    public String getName() {
        return "保存模型类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不存在代表添加"),
        @Param(name = "name", type = ApiParamType.STRING, isRequired = true, xss = true, desc = "名称"),
        @Param(name = "isMenu", type = ApiParamType.INTEGER, desc = "是否在菜单中显示")})
    @Description(desc = "保存模型类型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiTypeVo ciTypeVo = JSONObject.toJavaObject(jsonObj, CiTypeVo.class);
        Long id = jsonObj.getLong("id");
        if (ciTypeMapper.checkCiTypeNameIsExists(ciTypeVo) > 0) {
            throw new CiTypeIsExistsException(ciTypeVo.getName());
        }
        if (id == null) {
            Integer maxsort = ciTypeMapper.getMaxSort();
            if (maxsort == null) {
                maxsort = 1;
            } else {
                maxsort += 1;
            }
            ciTypeVo.setSort(maxsort);
            ciTypeMapper.insertCiType(ciTypeVo);
            return ciTypeVo.getId();
        } else {
            ciTypeMapper.updateCiType(ciTypeVo);
        }
        return null;
    }
}
