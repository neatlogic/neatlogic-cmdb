/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.api.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.FileUtil;
import neatlogic.module.cmdb.service.ci.CiService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportCiApi extends PrivateBinaryStreamApiComponentBase {
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

    @Input({
            //@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "id列表")
    })
    @Description(desc = "nmcac.exportciapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONArray paramIdList = jsonObj.getJSONArray("idList");
        List<Long> idList = new ArrayList<>();
        for (int i = 0; i < paramIdList.size(); i++) {
            idList.add(paramIdList.getLong(i));
        }
        if (idList.size() == 1) {
            CiVo ciVo = ciService.getCiById(idList.get(0));
            if (ciVo != null) {
                //清空所有id信息
                ciVo.setLft(null);
                ciVo.setRht(null);
                //清空所有继承属性和关系
                if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                    ciVo.getAttrList().removeIf(d -> d.getIsExtended().equals(1));
                }
                if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                    ciVo.getRelList().removeIf(d -> d.getIsExtended().equals(1));
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
        } else {
            String fileName = FileUtil.getEncodedFileName("模型导入文件_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".zip");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;fileName=\"" + fileName + "\"");
            ServletOutputStream os = response.getOutputStream();
            ZipOutputStream finalZip = new ZipOutputStream(os);
            for (Long id : idList) {
                CiVo ciVo = ciService.getCiById(id);
                if (ciVo != null) {
                    //清空所有id信息
                    ciVo.setLft(null);
                    ciVo.setRht(null);
                    //清空所有继承属性和关系
                    if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                        ciVo.getAttrList().removeIf(d -> d.getIsExtended().equals(1));
                    }
                    if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                        ciVo.getRelList().removeIf(d -> d.getIsExtended().equals(1));
                    }
                    // 创建一个字节数组输出流，用来存储第一级ZIP文件
                    ByteArrayOutputStream modelBaos = new ByteArrayOutputStream();
                    ZipOutputStream modelZip = new ZipOutputStream(modelBaos);

                    // 创建一个 .model.zip 文件，打包CiVo对象
                    modelZip.putNextEntry(new ZipEntry(ciVo.getName()));
                    ObjectOutputStream oos = new ObjectOutputStream(modelZip);
                    oos.writeObject(ciVo);
                    oos.flush();
                    modelZip.closeEntry();
                    oos.close();

                    // 关闭第一级的ZIP
                    modelZip.close();

                    // 将生成的 .model.zip 作为字节数组
                    byte[] modelZipBytes = modelBaos.toByteArray();
                    // 在最终的ZIP文件中，添加这个 .model.zip 文件
                    ZipEntry finalZipEntry = new ZipEntry(ciVo.getName() + ".model");
                    finalZip.putNextEntry(finalZipEntry);
                    finalZip.write(modelZipBytes);
                    finalZip.closeEntry();
                }
            }
            finalZip.close();
        }
        return null;
    }

}
