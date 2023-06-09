/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.ci;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.service.ci.CiService;
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
        return "nmcac.exportciapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid")
    })
    @Description(desc = "nmcac.exportciapi.getname")
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
