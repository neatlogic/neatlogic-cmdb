package codedriver.module.cmdb.service.attr;

import codedriver.module.cmdb.dto.ci.AttrVo;

public interface AttrService {
    void insertAttr(AttrVo attrVo);

    void updateAttr(AttrVo attrVo);

    void deleteAttrById(AttrVo attrVo);
}
