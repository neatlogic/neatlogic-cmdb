/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.attr;

import codedriver.framework.cmdb.dto.ci.AttrVo;

public interface AttrService {
    void insertAttr(AttrVo attrVo);

    void updateAttr(AttrVo attrVo);

    void deleteAttr(AttrVo attrVo);
}
