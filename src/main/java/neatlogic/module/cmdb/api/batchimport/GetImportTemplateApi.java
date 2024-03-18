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

package neatlogic.module.cmdb.api.batchimport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrItemVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.enums.PropHandlerType;
import neatlogic.framework.cmdb.exception.ci.CiAttrIsNotNullException;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.$;
import neatlogic.framework.util.excel.ExcelBuilder;
import neatlogic.framework.util.excel.SheetBuilder;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.service.ci.CiService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFDataValidation;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
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
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetImportTemplateApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(GetImportTemplateApi.class);


    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiService ciService;


    @Override
    public String getToken() {
        return "/cmdb/import/template/get";
    }

    @Override
    public String getName() {
        return "nmcab.getimporttemplateapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "attrIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.attridlist"),
            @Param(name = "relIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.relidlist"),
            @Param(name = "globalAttrIdList", type = ApiParamType.JSONARRAY, desc = "term.cmdb.globalattridlist")
    })
    @Description(desc = "nmcab.getimporttemplateapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        int validationOptionSize = 100;// 限制数据有效性列表长度
        Long ciId = paramObj.getLong("ciId");
        JSONArray attrIdArray = paramObj.getJSONArray("attrIdList");
        JSONArray relIdArray = paramObj.getJSONArray("relIdList");
        JSONArray globalAttrIdArray = paramObj.getJSONArray("globalAttrIdList");
        List<Long> attrIdList = null;
        List<Long> relIdList = null;
        List<Long> globalAttrIdList = null;
        if (CollectionUtils.isNotEmpty(attrIdArray)) {
            attrIdList = attrIdArray.toJavaList(Long.class);
        }
        if (CollectionUtils.isNotEmpty(relIdArray)) {
            relIdList = relIdArray.toJavaList(Long.class);
        }
        if (CollectionUtils.isNotEmpty(globalAttrIdArray)) {
            globalAttrIdList = globalAttrIdArray.toJavaList(Long.class);
        }

        if (CollectionUtils.isEmpty(attrIdList) && CollectionUtils.isEmpty(relIdList)) {
            throw new CiAttrIsNotNullException();
        }

        CiVo ciVo = ciService.getCiById(ciId);
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        ciViewVo.setNeedAlias(1);
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }

        List<String> headerList = new ArrayList<>();
        List<String> columnList = new ArrayList<>();
        Map<String, String[]> validationMap = new HashMap<>();
        headerList.add("id");
        headerList.add("uuid");
        columnList.add("id");
        columnList.add("uuid");
        /* 全局属性 */
        if (CollectionUtils.isNotEmpty(ciVo.getGlobalAttrList()) && CollectionUtils.isNotEmpty(globalAttrIdList)) {
            for (GlobalAttrVo attr : ciVo.getGlobalAttrList()) {
                if (globalAttrIdList.contains(attr.getId())) {
                    if (CollectionUtils.isNotEmpty(ciViewList)) {
                        Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().equals("global") && d.getItemId().equals(attr.getId())).findFirst();
                        op.ifPresent(viewVo -> attr.setLabel(viewVo.getAlias()));
                    }
                    String label = attr.getLabel();
                    label = label + "[" + $.t("term.cmdb.globalattr") + "]";
                    headerList.add(label);
                    columnList.add("global_" + attr.getId());
                    // 如果是单值，则设置数据有效性
                    if (attr.getIsMultiple().equals(0) && CollectionUtils.isNotEmpty(attr.getItemList())) {
                        String[] collect = attr.getItemList().stream().map(GlobalAttrItemVo::getValue).toArray(String[]::new);
                        validationMap.put("global_" + attr.getId(), collect);
                    }
                }
            }
        }
        /* 属性 */
        if (CollectionUtils.isNotEmpty(ciVo.getAttrList()) && CollectionUtils.isNotEmpty(attrIdList)) {
            for (AttrVo attr : ciVo.getAttrList()) {
                if (attrIdList.contains(attr.getId())) {
                    if (CollectionUtils.isNotEmpty(ciViewList)) {
                        Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().equals("attr") && d.getItemId().equals(attr.getId())).findFirst();
                        op.ifPresent(viewVo -> attr.setLabel(viewVo.getAlias()));
                    }
                    String label = attr.getLabel();
                    Integer isRequired = attr.getIsRequired();
                    Integer isUnique = attr.getIsUnique();
                    StringBuilder ciTypeAttrNameDesc = new StringBuilder();
                    if (isUnique.equals(1)) {
                        ciTypeAttrNameDesc.append($.t("common.unique"));
                    }
                    if (isRequired.equals(1)) {
                        if (ciTypeAttrNameDesc.length() > 0) {
                            ciTypeAttrNameDesc.append("|");
                        }
                        ciTypeAttrNameDesc.append($.t("common.mustinput"));
                    }

                    if (ciTypeAttrNameDesc.length() > 0) {
                        label = label + "[(" + ciTypeAttrNameDesc + ")]";
                    }
                    headerList.add(label);
                    columnList.add("attr_" + attr.getId());
                    // 如果是下拉框，则设置数据有效性
                    if (PropHandlerType.SELECT.getValue().equals(attr.getType())) {
                        CiVo targetCi = ciMapper.getCiById(attr.getTargetCiId());
                        if (targetCi != null) {
                            // 获取当前属性关联的模型配置项（包括子模型的配置项，限制validationOptionSize条）
                            int ciEntityCount = ciEntityMapper.getDownwardCiEntityCountByLR(targetCi.getLft(), targetCi.getRht());
                            List<CiEntityVo> list = ciEntityMapper.getDownwardCiEntityByLRLimitSize(targetCi.getLft(), targetCi.getRht(), validationOptionSize);
                            if (CollectionUtils.isNotEmpty(list)) {
                                List<String> collect = list.stream().map(CiEntityVo::getName).collect(Collectors.toList());
                                if (ciEntityCount > validationOptionSize) {
                                    collect.add("common.toomanyoption");
                                }
                                String[] array = new String[collect.size()];
                                collect.toArray(array);
                                validationMap.put("attr_" + attr.getId(), array);
                            }
                        }
                    }
                }
            }
        }
        /* 关系 */
        if (CollectionUtils.isNotEmpty(ciVo.getRelList()) && CollectionUtils.isNotEmpty(relIdList)) {
            List<Long> upwardCiIdList = ciVo.getUpwardCiList().stream().map(CiVo::getId).collect(Collectors.toList());
            for (RelVo rel : ciVo.getRelList()) {
                if (relIdList.contains(rel.getId())) {
                    if (CollectionUtils.isNotEmpty(ciViewList)) {
                        Optional<CiViewVo> op = ciViewList.stream().filter(d -> d.getType().startsWith("rel") && d.getItemId().equals(rel.getId())).findFirst();
                        if (op.isPresent()) {
                            CiViewVo view = op.get();
                            if (view.getType().equals("relfrom")) {
                                rel.setToLabel(view.getAlias());
                            } else if (view.getType().equals("relto")) {
                                rel.setFromLabel(view.getAlias());
                            }
                        }
                    }
                    // 关系包括当前CI自身设置的关系与继承过来的关系
                    if (rel.getFromCiId().equals(ciVo.getId())
                            || (CollectionUtils.isNotEmpty(upwardCiIdList) && upwardCiIdList.contains(rel.getFromCiId()))) { //当前CI处于from
                        String label = rel.getToLabel();
                        if (rel.getToIsRequired().equals(1)) {
                            label = label + "[(" + $.t("common.mustinput") + ")]";
                        }
                        headerList.add(label);
                        columnList.add("relfrom_" + rel.getId());
                    } else if (rel.getToCiId().equals(ciVo.getId())
                            || (CollectionUtils.isNotEmpty(upwardCiIdList) && upwardCiIdList.contains(rel.getToCiId()))) {//当前CI处于to
                        String label = rel.getFromLabel();
                        if (rel.getFromIsRequired().equals(1)) {
                            label = label + "[(" + $.t("common.mustinput") + ")]";
                        }
                        headerList.add(label);
                        columnList.add("relto_" + rel.getId());
                    }
                }
            }
        }


        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
        SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                .withHeadBgColor(HSSFColor.HSSFColorPredefined.LIGHT_BLUE)
                .withColumnWidth(30)
                .addSheet("data")
                .withHeaderList(headerList).withColumnList(columnList);
        if (MapUtils.isNotEmpty(validationMap)) {
            for (Map.Entry<String, String[]> entry : validationMap.entrySet()) {
                sheetBuilder.addValidation(entry.getKey(), entry.getValue());
            }
        }
        Workbook workbook = builder.build();
        String fileName = ciVo.getId() + "_" + ciVo.getLabel() + ".xlsx";
        if (request.getHeader("User-Agent").toLowerCase().contains("msie") || request.getHeader("User-Agent").contains("Gecko")) {
            fileName = URLEncoder.encode(fileName, "UTF-8");// IE浏览器
        } else {
            fileName = new String(fileName.replace(" ", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        }
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
            workbook.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 设置数据有效性
     * 数据有效性列表过长，总字符数超过255时，需要将列表放在单独的sheet
     *
     * @param workbook       需要设置数据有效性的workbook
     * @param targetSheet    需要设置需要设置数据有效性的sheet
     * @param sheetName      数据有效性列表sheet名称
     * @param sheetNameIndex 数据有效性列表sheet序号
     * @param sheetData      数据有效性列表
     * @param firstRow       数据有效性开始行
     * @param lastRow        数据有效性结束行
     * @param firstCol       数据有效性开始列
     * @param lastCol        数据有效性结束列
     */
    private void addValidationData(Workbook workbook, Sheet targetSheet, String sheetName, int sheetNameIndex, String[] sheetData, int firstRow, int lastRow, int firstCol, int lastCol) {
        Sheet hidden = workbook.createSheet(sheetName);
        Cell cell;
        for (int i = 0, length = sheetData.length; i < length; i++) {
            String name = sheetData[i];
            Row row = hidden.createRow(i);
            cell = row.createCell(0);
            cell.setCellValue(name);
        }
        Name namedCell = workbook.createName();
        namedCell.setNameName(sheetName);
        namedCell.setRefersToFormula(sheetName + "!$A$1:$A$" + sheetData.length);
        DVConstraint constraint = DVConstraint.createFormulaListConstraint(sheetName);
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, lastRow, firstCol, lastCol);
        DataValidation dataValidation = new HSSFDataValidation(regions, constraint);
        dataValidation.setSuppressDropDownArrow(false);// false时显示下拉箭头(office2007)
        dataValidation.setShowErrorBox(true);
        workbook.setSheetHidden(sheetNameIndex, true);
        targetSheet.addValidationData(dataValidation);
    }
}
