/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.initialdata.handler;

import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.initialdata.core.IAfterInitialDataImportHandler;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import codedriver.module.cmdb.service.ci.CiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CmdbAfterInitialDataImportHandler implements IAfterInitialDataImportHandler {
    @Resource
    private CiSchemaMapper ciSchemaMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiService ciService;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getModuleId() {
        return "cmdb";
    }

    @Override
    public String getDescription() {
        return "根据模型生成数据表和视图";
    }

    @Override
    public void execute() {
        List<CiVo> ciList = ciMapper.searchCi(new CiVo());
        for (CiVo ciVo : ciList) {
            if (ciVo.getIsVirtual().equals(0)) {
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                ciVo.setAttrList(attrList);
                ciSchemaMapper.initCiTable(ciVo);
            } else {
                //创建视图
                String viewXml = ciMapper.getCiViewXmlById(ciVo.getId());
                if (StringUtils.isNotBlank(viewXml)) {
                    ciVo.setViewXml(viewXml);
                    ciService.buildCiView(ciVo);
                }
            }
        }

    }
}
