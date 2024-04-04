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
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class ValidateImportCiApi extends PrivateBinaryStreamApiComponentBase {
    @Resource
    private RelMapper relMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;


    @Override
    public String getToken() {
        return "/cmdb/ci/importvalidate";
    }

    @Override
    public String getName() {
        return "校验导入的模型文件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "fileList", type = ApiParamType.FILE, isRequired = true, desc = "common.file")})
    @Description(desc = "校验导入的模型文件")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        List<MultipartFile> multipartFileList = multipartRequest.getFiles("fileList");
        JSONArray dataList = new JSONArray();
        List<CiVo> newCiList = new ArrayList<>();
        Map<Long, CiVo> newCiMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(multipartFileList)) {
            for (MultipartFile multipartFile : multipartFileList) {
                ZipInputStream zin = new ZipInputStream(multipartFile.getInputStream());
                while (zin.getNextEntry() != null) {
                    ObjectInputStream objectInputStream = new ObjectInputStream(zin);
                    CiVo ciVo = (CiVo) objectInputStream.readObject();
                    if (!newCiMap.containsKey(ciVo.getId())) {
                        newCiList.add(ciVo);
                        newCiMap.put(ciVo.getId(), ciVo);
                    }
                    zin.closeEntry();
                }
                zin.close();
            }
            if (CollectionUtils.isNotEmpty(newCiList)) {
                for (CiVo ciVo : newCiList) {
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("name", ciVo.getName());
                    dataObj.put("label", ciVo.getLabel());
                    dataObj.put("error", new JSONArray());
                    if (StringUtils.isNotBlank(ciVo.getTypeName()) && (ciMapper.getCiTypeByName(ciVo.getTypeName()) == null)) {
                        dataObj.getJSONArray("error").add("模型层级：" + ciVo.getTypeName() + "不存在");
                    }
                    if (ciMapper.getCiBaseInfoById(ciVo.getId()) == null) {
                        dataObj.put("_action", "insert");
                    } else {
                        dataObj.put("_action", "update");
                    }
                    if (ciVo.getParentCiId() != null && !newCiMap.containsKey(ciVo.getParentCiId())) {
                        CiVo parentCiVo = ciMapper.getCiBaseInfoById(ciVo.getParentCiId());
                        if (parentCiVo == null) {
                            dataObj.getJSONArray("error").add("父模型：" + ciVo.getParentCiId() + "不存在");
                            continue;
                        }
                    }
                    //检查关联属性是否存在
                    if (CollectionUtils.isNotEmpty(ciVo.getAttrList())) {
                        JSONArray attrList = new JSONArray();
                        for (AttrVo attrVo : ciVo.getAttrList()) {
                            JSONObject attrObj = new JSONObject();
                            attrObj.put("name", attrVo.getName());
                            attrObj.put("label", attrVo.getLabel());
                            attrObj.put("error", new JSONArray());
                            if (attrMapper.getAttrById(attrVo.getId()) == null) {
                                attrObj.put("_action", "insert");
                            } else {
                                attrObj.put("_action", "update");
                            }
                            if (attrVo.getTargetCiId() != null && !newCiMap.containsKey(attrVo.getTargetCiId())) {
                                CiVo targetCiVo = ciMapper.getCiBaseInfoById(attrVo.getTargetCiId());
                                if (targetCiVo == null) {
                                    attrObj.getJSONArray("error").add("目标模型：" + attrVo.getTargetCiId() + "不存在");
                                }
                            }
                            attrList.add(attrObj);
                        }
                        dataObj.put("attrList", attrList);
                    }
                    //检查关系对端是否存在
                    if (CollectionUtils.isNotEmpty(ciVo.getRelList())) {
                        JSONArray relList = new JSONArray();
                        for (RelVo relVo : ciVo.getRelList()) {
                            JSONObject relObj = new JSONObject();
                            if (relMapper.getRelById(relVo.getId()) == null) {
                                relObj.put("_action", "insert");
                            } else {
                                relObj.put("_action", "update");
                            }
                            relObj.put("fromCiName", relVo.getFromCiName());
                            relObj.put("fromCiLabel", relVo.getFromCiLabel());
                            relObj.put("toCiName", relVo.getToCiName());
                            relObj.put("toCiLabel", relVo.getToCiLabel());
                            relObj.put("toName", relVo.getToName());
                            relObj.put("fromName", relVo.getFromName());
                            relObj.put("toLabel", relVo.getToLabel());
                            relObj.put("fromLabel", relVo.getFromLabel());
                            relObj.put("direction", relVo.getDirection());
                            relObj.put("error", new JSONArray());
                            if (StringUtils.isNotBlank(relVo.getTypeText()) && (relMapper.getRelTypeByName(relVo.getTypeText()) == null)) {
                                relObj.getJSONArray("error").add("类型：" + relVo.getTypeText() + "不存在");
                            }
                            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                if (!newCiMap.containsKey(relVo.getToCiId())) {
                                    CiVo toCiVo = ciMapper.getCiBaseInfoById(relVo.getToCiId());
                                    if (toCiVo == null) {
                                        relObj.getJSONArray("error").add("下游模型：" + relVo.getToCiId() + "不存在");
                                    }
                                }
                            } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                                if (!newCiMap.containsKey(relVo.getFromCiId())) {
                                    CiVo fromCiVo = ciMapper.getCiBaseInfoById(relVo.getFromCiId());
                                    if (fromCiVo == null) {
                                        relObj.getJSONArray("error").add("上游模型：" + relVo.getFromCiId() + "不存在");
                                    }
                                }
                            }
                            relList.add(relObj);
                        }
                        dataObj.put("relList", relList);
                    }
                    dataList.add(dataObj);
                }
            }
        } else {
            throw new ParamNotExistsException("fileList");
        }
        return dataList;
    }

}
