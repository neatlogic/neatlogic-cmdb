/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.ci;

import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.view.ViewConstVo;

import java.util.List;

public interface CiViewMapper {
    List<CiViewVo> getCiViewBaseInfoByCiId(Long ciId);

    List<ViewConstVo> getCiViewConstByCiId(Long ciId);

    List<ViewConstVo> getAllCiViewConstList();

    List<CiViewVo> getCiViewByCiId(CiViewVo ciViewVo);

    int insertCiView(CiViewVo ciViewVo);

    int deleteCiViewByCiId(Long ciId);
}
