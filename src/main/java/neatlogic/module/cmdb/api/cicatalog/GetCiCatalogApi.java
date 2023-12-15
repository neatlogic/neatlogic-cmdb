package neatlogic.module.cmdb.api.cicatalog;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.cmdb.exception.cicatalog.CiCatalogNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.cicatalog.CiCatalogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiCatalogApi extends PrivateApiComponentBase {

    @Resource
    private CiCatalogService ciCatalogService;

    @Override
    public String getName() {
        return "nmcac.getcicatalogapi.getname";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id")
    })
    @Output({
            @Param(explode = CiCatalogVo.class)
    })
    @Description(desc = "nmcac.getcicatalogapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        CiCatalogVo ciCatalog = ciCatalogService.getCiCatalog(id);
        if (ciCatalog == null) {
            throw new CiCatalogNotFoundException(id);
        }
        return ciCatalog;
    }

    @Override
    public String getToken() {
        return "cmdb/cicatalog/get";
    }
}
