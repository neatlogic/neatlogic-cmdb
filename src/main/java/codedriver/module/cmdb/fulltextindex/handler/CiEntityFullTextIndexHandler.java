/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.fulltextindex.handler;

import codedriver.framework.fulltextindex.core.FullTextIndexHandlerBase;
import codedriver.framework.fulltextindex.core.IFullTextIndexType;
import codedriver.framework.fulltextindex.dto.FullTextIndexVo;
import org.springframework.stereotype.Service;

@Service
public class CiEntityFullTextIndexHandler extends FullTextIndexHandlerBase {
    @Override
    protected String getModuleId() {
        return "cmdb";
    }

    @Override
    protected void myCreateIndex(FullTextIndexVo fullTextIndexVo) {

    }

    @Override
    public IFullTextIndexType getType() {
        return null;
    }

    @Override
    public void rebuildIndex(Boolean isRebuildAll) {

    }
}
