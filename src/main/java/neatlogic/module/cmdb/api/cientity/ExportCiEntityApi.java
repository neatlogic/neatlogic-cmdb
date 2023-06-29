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

package neatlogic.module.cmdb.api.cientity;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.excel.ExcelBuilder;
import neatlogic.framework.util.excel.SheetBuilder;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportCiEntityApi extends PrivateBinaryStreamApiComponentBase {
    private final static Logger logger = LoggerFactory.getLogger(ExportCiEntityApi.class);
    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/export";
    }

    @Override
    public String getName() {
        return "nmcac.exportcientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "common.keyword"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "nmcac.exportcientityapi.input.param.desc.idlist"),
            @Param(name = "showAttrRelList", type = ApiParamType.JSONARRAY, desc = "nmcac.exportcientityapi.input.param.desc.showattrrellist"),
            @Param(name = "attrFilterList", type = ApiParamType.STRING, desc = "nmcac.exportcientityapi.input.param.desc.attrfilterlist"),
            @Param(name = "relFilterList", type = ApiParamType.JSONARRAY, desc = "nmcac.exportcientityapi.input.param.desc.relfilterlist")
    })
    @Description(desc = "nmcac.exportcientityapi.getname")
    @Override
    //TODO 后续要对数据进行优化防止OOM
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        CiEntityVo ciEntityVo = JSONObject.toJavaObject(jsonObj, CiEntityVo.class);
        JSONArray showAttrRelList = jsonObj.getJSONArray("showAttrRelList");
        Set<String> showAttrRelSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(showAttrRelList)) {
            for (int i = 0; i < showAttrRelList.size(); i++) {
                showAttrRelSet.add(showAttrRelList.getString(i));
            }
        }
        Long ciId = jsonObj.getLong("ciId");
        CiVo ciVo = ciMapper.getCiById(ciId);
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciEntityVo.getCiId());
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
        List<String> headerList = new ArrayList<>();
        headerList.add("id");
        headerList.add("uuid");
        List<String> columnList = new ArrayList<>();
        columnList.add("id");
        columnList.add("uuid");
        List<Long> attrIdList = new ArrayList<>();
        List<Long> relIdList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            for (CiViewVo ciview : ciViewList) {
                switch (ciview.getType()) {
                    case "attr":
                        if (showAttrRelSet.contains("attr_" + ciview.getItemId())) {
                            attrIdList.add(ciview.getItemId());
                            columnList.add("attr_" + ciview.getItemId());
                            headerList.add(ciview.getItemLabel());
                        }
                        break;
                    case "relfrom":
                        if (showAttrRelSet.contains("relfrom_" + ciview.getItemId())) {
                            relIdList.add(ciview.getItemId());
                            columnList.add("relfrom_" + ciview.getItemId());
                            headerList.add(ciview.getItemLabel());
                        }
                        break;
                    case "relto":
                        if (showAttrRelSet.contains("relto_" + ciview.getItemId())) {
                            relIdList.add(ciview.getItemId());
                            columnList.add("relto_" + ciview.getItemId());
                            headerList.add(ciview.getItemLabel());
                        }
                        break;
                }
            }
        }
        //把需要显示的属性和关系设进去，后台会进行自动过滤
        ciEntityVo.setAttrIdList(attrIdList);
        ciEntityVo.setRelIdList(relIdList);

        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
        SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                .withHeadBgColor(HSSFColor.HSSFColorPredefined.DARK_BLUE)
                .withColumnWidth(30)
                .addSheet("数据")
                .withHeaderList(headerList)
                .withColumnList(columnList);
        Workbook workbook = builder.build();

        ciEntityVo.setPageSize(100);
        ciEntityVo.setCurrentPage(1);
        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        while (CollectionUtils.isNotEmpty(ciEntityList)) {
            for (CiEntityVo entity : ciEntityList) {
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("id", entity.getId());
                dataMap.put("uuid", entity.getUuid());
                dataMap.put("name", entity.getName());
                dataMap.put("ciId", entity.getCiId());
                dataMap.put("ciName", entity.getCiName());
                dataMap.put("ciLabel", entity.getCiLabel());
                dataMap.put("type", entity.getTypeId());
                dataMap.put("typeName", entity.getTypeName());
                for (String column : columnList) {
                    if (entity.getAttrEntityData().containsKey(column)) {
                        JSONObject attrObj = entity.getAttrEntityData().getJSONObject(column);
                        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrObj.getString("type"));
                        AttrVo attrVo = new AttrVo();
                        attrVo.setConfig(attrObj.getJSONObject("config"));
                        attrVo.setTargetCiId(attrObj.getLong("targetCiId"));
                        if (handler != null) {
                            JSONArray valueList = attrObj.getJSONArray("valueList");
                            handler.transferValueListToExport(attrVo, valueList);
                            if (CollectionUtils.isNotEmpty(valueList)) {
                                String tmpValue = "";
                                for (int v = 0; v < valueList.size(); v++) {
                                    if (valueList.get(v) != null) {
                                        if (StringUtils.isNotBlank(tmpValue)) {
                                            tmpValue += ",";
                                        }
                                        tmpValue += valueList.getString(v);
                                    }
                                }
                                dataMap.put(column, tmpValue);
                            }
                        }
                    } else if (entity.getRelEntityData().containsKey(column)) {
                        JSONArray valueList = entity.getRelEntityData().getJSONObject(column).getJSONArray("valueList");
                        if (CollectionUtils.isNotEmpty(valueList)) {
                            String relCiEntityName = "";
                            for (int i = 0; i < valueList.size(); i++) {
                                if (StringUtils.isNotBlank(relCiEntityName)) {
                                    relCiEntityName += ",";
                                }
                                relCiEntityName += valueList.getJSONObject(i).getString("ciEntityName");
                            }
                            dataMap.put(column, relCiEntityName);
                        }
                    }
                }
                sheetBuilder.addData(dataMap);
            }
            //如果从外部传入idList，就不需要进一步查询下一页数据了
            if (CollectionUtils.isEmpty(idList)) {
                ciEntityVo.setCurrentPage(ciEntityVo.getCurrentPage() + 1);
                ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            } else {
                break;
            }
        }


        String fileNameEncode = ciVo.getId() + "_" + ciVo.getLabel() + ".xlsx";
        boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
            fileNameEncode = URLEncoder.encode(fileNameEncode, "UTF-8");// IE浏览器
        } else {
            fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        }
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

}
