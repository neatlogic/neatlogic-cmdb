package codedriver.module.cmdb.service.cientity;

import java.util.List;

import codedriver.module.cmdb.dto.cientity.CiEntityVo;

public interface CiEntityService {
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

}
