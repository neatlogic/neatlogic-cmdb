/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import java.util.List;

import codedriver.framework.cmdb.dto.ci.CiViewVo;

/**
 * 
 * @Author:chenqiwei
 * @Time:Aug 15, 2020
 * @ClassName: CiMapper
 */
public interface CiViewMapper {

    public List<CiViewVo> getCiViewByCiId(CiViewVo ciViewVo);

    public int insertCiView(CiViewVo ciViewVo);

    public int deleteCiViewByCiId(Long ciId);
}
