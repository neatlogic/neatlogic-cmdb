/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.dto.ci.CiAuthVo;

import java.util.List;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: CiMapper
 * @Description: TODO
 */
public interface CiAuthMapper {
    public List<CiAuthVo> getCiAuthByCiId(Long ciId);

    public int insertCiAuth(CiAuthVo ciAuthVo);

    public int deleteCiAuthByCiId(Long ciId);

}
