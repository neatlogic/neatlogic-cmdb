/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.ci;

import org.springframework.transaction.annotation.Transactional;

import codedriver.framework.cmdb.dto.ci.CiVo;

public interface CiService {

    @Transactional
    void insertCi(CiVo ciVo);

    @Transactional
    public int deleteCi(Long ciId);

    public CiVo getCiById(Long id);

}
