package codedriver.module.cmdb.api.ci;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.exception.ci.CiAuthException;
import codedriver.module.cmdb.exception.ci.CiHasRelException;
import codedriver.module.cmdb.service.ci.CiAuthService;
import codedriver.module.cmdb.service.ci.CiService;

@Service
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteCiApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private CiService ciService;

    @Autowired
    private CiAuthService ciAuthService;

    @Override
    public String getToken() {
        return "/cmdb/ci/delete";
    }

    @Override
    public String getName() {
        return "删除模型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = CiVo.class)})
    @Description(desc = "删除模型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("id");
        boolean hasAuth = AuthActionChecker.check("CI_MODIFY");
        if (!hasAuth) {
            hasAuth = ciAuthService.hasCiManagePrivilege(ciId);
            if (!hasAuth) {
                throw new CiAuthException();
            }
        }
        // 检查当前模型是否有被引用
        List<CiVo> fromCiList = ciMapper.getCiByToCiId(ciId);
        if (CollectionUtils.isNotEmpty(fromCiList)) {
            throw new CiHasRelException(
                fromCiList.stream().map(cientity -> cientity.getLabel()).collect(Collectors.joining("、")));
        }
        // 删除配置项信息
        ciService.deleteCi(ciId);
        return null;
    }
}
