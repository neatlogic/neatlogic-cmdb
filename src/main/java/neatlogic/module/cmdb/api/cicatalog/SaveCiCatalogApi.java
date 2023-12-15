package neatlogic.module.cmdb.api.cicatalog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.autoexec.exception.AutoexecCatalogRepeatException;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.cmdb.exception.cicatalog.CiCatalogNotFoundException;
import neatlogic.framework.cmdb.exception.cicatalog.CiCatalogNameRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.FieldValidResultVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.IValid;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.RegexUtils;
import neatlogic.module.cmdb.service.cicatalog.CiCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveCiCatalogApi extends PrivateApiComponentBase {

    @Resource
    private CiCatalogService ciCatalogService;

    @Override
    public String getName() {
        return "nmcac.savecicatalogapi.getname";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "common.id"),
            @Param(name = "name", type = ApiParamType.REGEX, rule = RegexUtils.NAME, desc = "common.name", maxLength = 50, isRequired = true, xss = true),
            @Param(name = "parentId", type = ApiParamType.LONG, desc = "common.parentid"),
    })
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "common.id")})
    @Description(desc = "nmcac.savecicatalogapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiCatalogVo ciCatalog = paramObj.toJavaObject(CiCatalogVo.class);
        if (ciCatalog.getParentId() == null) {
            ciCatalog.setParentId(CiCatalogVo.ROOT_ID);
        }
        // 同级下不重复
        if (ciCatalogService.checkCiCatalogNameIsRepeat(ciCatalog) > 0) {
            throw new CiCatalogNameRepeatException(ciCatalog.getName());
        }
        Long id = paramObj.getLong("id");
        if (id != null) {
            if (ciCatalogService.checkCiCatalogIsExists(id) == 0) {
                throw new CiCatalogNotFoundException(id);
            }
        }
        return ciCatalogService.saveCiCatalog(ciCatalog);
    }

    @Override
    public String getToken() {
        return "cmdb/cicatalog/save";
    }

    public IValid name() {
        return value -> {
            CiCatalogVo vo = JSON.toJavaObject(value, CiCatalogVo.class);
            if (vo.getParentId() == null) {
                vo.setParentId(CiCatalogVo.ROOT_ID);
            }
            if (ciCatalogService.checkCiCatalogNameIsRepeat(vo) > 0) {
                return new FieldValidResultVo(new AutoexecCatalogRepeatException(vo.getName()));
            }
            return new FieldValidResultVo();
        };
    }
}
