/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportCiApi extends PrivateBinaryStreamApiComponentBase {
    private final static Logger logger = LoggerFactory.getLogger(ExportCiApi.class);
    @Resource
    private CiService ciService;


    @Override
    public String getToken() {
        return "/cmdb/ci/export";
    }

    @Override
    public String getName() {
        return "导出配置项模型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")
    })
    @Description(desc = "导出配置项模型接口")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        CiVo ciVo = ciService.getCiById(jsonObj.getLong("ciId"));
        if (ciVo != null) {
            //清空所有id信息
            ciVo.setId(null);
            ciVo.setParentCiId(null);
            ciVo.setLft(null);
            ciVo.setRht(null);
            //清空所有继承属性和关系
            if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                ciVo.getAttrList().removeIf(d -> d.getIsExtended().equals(1));
                for (AttrVo attrVo : ciVo.getAttrList()) {
                    attrVo.setCiId(null);
                    attrVo.setId(null);
                }
            }
            if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                ciVo.getRelList().removeIf(d -> d.getIsExtended().equals(1));
                for (RelVo relVo : ciVo.getRelList()) {
                    relVo.setToCiId(null);
                    relVo.setFromCiId(null);
                    relVo.setId(null);
                }
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;fileName=\"" + ciVo.getName() + ".model\"");
            ServletOutputStream os = response.getOutputStream();
            ZipOutputStream zos = new ZipOutputStream(os);
            // 仅打包归档存储
            zos.setMethod(ZipOutputStream.DEFLATED);
            zos.setLevel(0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(ciVo);
            oos.flush();
            oos.close();
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            zos.putNextEntry(new ZipEntry(ciVo.getName()));
            IOUtils.copy(is, zos);
            zos.closeEntry();
            zos.close();
        }

        return null;
    }

}
