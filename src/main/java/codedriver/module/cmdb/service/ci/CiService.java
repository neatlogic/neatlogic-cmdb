/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.ci;

import org.springframework.transaction.annotation.Transactional;

import codedriver.framework.cmdb.dto.ci.CiVo;

import java.util.List;

public interface CiService {
    CiVo getCiById(Long id);

    @Transactional
    void insertCi(CiVo ciVo);

    @Transactional
    void updateCiUnique(Long ciId, List<Long> attrIdList);

    @Transactional
    void updateCiNameAttrId(CiVo ciVo);

    @Transactional
    void updateCiNameExpression(Long ciId, String nameExpression);

    @Transactional
    void updateCi(CiVo ciVo);

    @Transactional
    int deleteCi(Long ciId);


    CiVo getCiByName(String ciName);
}
