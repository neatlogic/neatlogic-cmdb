/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.dto.ci.CiTypeVo;

import java.util.List;

public interface CiTypeMapper {

    Integer getMaxSort();

    int checkCiTypeNameIsExists(CiTypeVo ciTypeVo);

    CiTypeVo getCiTypeById(Long ciTypeId);

    CiTypeVo getCiTypeByName(String ciTypeName);

    List<CiTypeVo> searchCiType(CiTypeVo ciTypeVo);

    int insertCiType(CiTypeVo ciTypeVo);

    int updateCiType(CiTypeVo ciTypeVo);

    int deleteCiTypeById(Long ciTypeId);
}
