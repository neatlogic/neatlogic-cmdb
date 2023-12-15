package neatlogic.module.cmdb.api.cicatalog;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.cicatalog.CiCatalogVo;
import neatlogic.framework.cmdb.exception.cicatalog.CiCatalogNotFoundException;
import neatlogic.framework.cmdb.exception.cicatalog.CiCatalogNameRepeatException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.lrcode.LRCodeManager;
import neatlogic.framework.lrcode.constvalue.MoveType;
import neatlogic.framework.lrcode.exception.MoveTargetNodeIllegalException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.service.cicatalog.CiCatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@Transactional
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class MoveCiCatalogApi extends PrivateApiComponentBase {

    @Resource
    private CiCatalogService ciCatalogService;

    @Override
    public String getName() {
        return "nmcac.movecicatalogapi.getname";
    }

    @Input({
            @Param(name = "id", type = ApiParamType.STRING, isRequired = true, desc = "common.id"),
            @Param(name = "targetId", type = ApiParamType.STRING, isRequired = true, desc = "common.targetid"),
            @Param(name = "moveType", type = ApiParamType.ENUM, rule = "inner,prev,next", isRequired = true, desc = "common.type")
    })
    @Description(desc = "nmcac.movecicatalogapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Long targetId = paramObj.getLong("targetId");
        String moveType = paramObj.getString("moveType");

        if (id.equals(targetId)) {
            throw new MoveTargetNodeIllegalException();
        }
        CiCatalogVo moveCiCatalog = ciCatalogService.getCiCatalog(id);
        if (moveCiCatalog == null) {
            throw new CiCatalogNotFoundException(id);
        }
        if (!targetId.equals(CiCatalogVo.ROOT_ID)) {
            CiCatalogVo targetCiCatalog = ciCatalogService.getCiCatalog(targetId);
            if (targetCiCatalog == null) {
                throw new CiCatalogNotFoundException(targetId);
            }
        }
        LRCodeManager.moveTreeNode("cmdb_ci_catalog", "id", "parent_id", id, MoveType.getMoveType(moveType), targetId);
        moveCiCatalog.setParentId(targetId);
        if (ciCatalogService.checkCiCatalogNameIsRepeat(moveCiCatalog) > 0) {
            throw new CiCatalogNameRepeatException(moveCiCatalog.getName());
        }
        return null;
    }

    @Override
    public String getToken() {
        return "cmdb/cicatalog/move";
    }
}
