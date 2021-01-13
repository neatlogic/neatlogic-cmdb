package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.AttrType;
import codedriver.framework.cmdb.constvalue.InputType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.cischema.CiSchemaHandler;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.exception.ci.CiAuthException;
import codedriver.module.cmdb.exception.ci.CiLabelIsExistsException;
import codedriver.module.cmdb.exception.ci.CiNameIsExistsException;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
@Transactional
public class SaveCiApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;
    @Autowired
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/save";
    }

    @Override
    public String getName() {
        return "保存模型";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表新增模型"),
            @Param(name = "name", type = ApiParamType.STRING, xss = true, isRequired = true, maxLength = 50, desc = "英文名称"),
            @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, maxLength = 100,
                    isRequired = true),
            @Param(name = "description", type = ApiParamType.STRING, desc = "备注", maxLength = 500, xss = true),
            @Param(name = "icon", type = ApiParamType.STRING, desc = "图标"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id", isRequired = true),
            @Param(name = "isMenu", type = ApiParamType.INTEGER, desc = "是否在菜单显示")})
    @Output({@Param(name = "id", type = ApiParamType.STRING, desc = "模型id")})
    @Description(desc = "保存模型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        boolean hasAuth = AuthActionChecker.check("CI_MODIFY");
        CiVo ciVo = JSONObject.toJavaObject(jsonObj, CiVo.class);
        if (ciMapper.checkCiNameIsExists(ciVo) > 0) {
            throw new CiNameIsExistsException(ciVo.getName());
        }
        if (ciMapper.checkCiLabelIsExists(ciVo) > 0) {
            throw new CiLabelIsExistsException(ciVo.getLabel());
        }
        Long ciId = jsonObj.getLong("id");
        if (ciId == null) {
            if (!hasAuth) {
                // 添加模型需要管理员权限
                throw new CiAuthException();
            }
            ciMapper.insertCi(ciVo);
            // 新增模型必须要添加一个name属性
            AttrVo attrVo = new AttrVo();
            attrVo.setName("name");
            attrVo.setLabel("名称");
            attrVo.setType(AttrType.CUSTOM.getValue());
            attrVo.setIsRequired(1);
            attrVo.setIsUnique(1);
            attrVo.setInputType(InputType.MT.getValue());
            // 私有属性，不允许用户删除
            attrVo.setIsPrivate(1);
            attrVo.setCiId(ciVo.getId());
            attrMapper.insertAttr(attrVo);
            //初始化CISCHEMA
            CiSchemaHandler.initCiSchema(ciVo);
        } else {
            // 编辑模型除了管理员权限还要看具体的模型授权
            if (!hasAuth) {
                hasAuth = CiAuthChecker.hasCiManagePrivilege(ciId);
            }
            if (!hasAuth) {
                throw new CiAuthException();
            }
            ciMapper.updateCi(ciVo);
        }
        return ciVo.getId();
    }

}
