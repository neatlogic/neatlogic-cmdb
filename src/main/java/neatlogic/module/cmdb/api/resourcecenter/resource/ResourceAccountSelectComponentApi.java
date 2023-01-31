package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountComponentVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceAccountSelectComponentApi extends PrivateApiComponentBase {

    @Resource
    ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/component/select";
    }

    @Override
    public String getName() {
        return "表单扩展选择资源中心账号组件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", isRequired = false, type = ApiParamType.STRING),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", explode = AccountVo[].class, desc = "账号列表"),
    })
    @Description(desc = "表单扩展选择资源中心账号组件")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        AccountComponentVo searchVo = JSON.toJavaObject(paramObj, AccountComponentVo.class);
        List<AccountComponentVo> accountComponentVoList = resourceMapper.searchAccountComponent(searchVo);
        Integer rowNum = resourceMapper.searchAccountComponentCount(searchVo);
        searchVo.setRowNum(rowNum);
        return TableResultUtil.getResult(accountComponentVoList, searchVo);

    }


}
