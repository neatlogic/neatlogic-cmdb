package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.AccountVo;
import codedriver.framework.cmdb.enums.resourcecenter.AccountType;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/1/5 10:51 上午
 */
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceAccountListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getName() {
        return "根据资源id查询关联的账户信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/list";
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "type", type = ApiParamType.ENUM, member = AccountType.class, isRequired = true, desc = "账号类型"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "协议名称"),
    })
    @Output({
            @Param(explode = AccountVo[].class, desc = "账号列表")})
    @Description(desc = "根据资产id获取关联账号信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long resourceId = paramObj.getLong("resourceId");
        if (resourceMapper.checkResourceIsExists(resourceId) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        String protocol = paramObj.getString("protocol");
        if (StringUtils.isNotBlank(protocol)) {
            return resourceAccountMapper.getResourceAccountListByResourceIdAndTypeAndProtocol(resourceId, paramObj.getString("type"), protocol);
        } else {
            return resourceAccountMapper.getResourceAccountListByResourceIdAndType(resourceId, paramObj.getString("type"));
        }
    }

}
