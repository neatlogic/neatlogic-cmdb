/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.batchimport;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.PropHandlerType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.service.ci.CiService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetImportTemplateApi extends PrivateBinaryStreamApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(GetImportTemplateApi.class);

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
        return "下载配置项导入模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "attrIdList", type = ApiParamType.JSONARRAY, desc = "模型属性ID列表"),
            @Param(name = "relIdList", type = ApiParamType.JSONARRAY, desc = "模型关系ID列表"),
    })
    @Output({})
    @Description(desc = "下载配置项导入模板")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HSSFWorkbook wb = null;
        OutputStream os = null;
        try {
            Long ciId = paramObj.getLong("ciId");
            JSONArray attrIdArray = paramObj.getJSONArray("attrIdList");
            JSONArray relIdArray = paramObj.getJSONArray("relIdList");
            List<Long> attrIdList = null;
            List<Long> relIdList = null;
            if (CollectionUtils.isNotEmpty(attrIdArray)) {
                attrIdList = attrIdArray.toJavaList(Long.class);
            }
            if (CollectionUtils.isNotEmpty(relIdArray)) {
                relIdList = relIdArray.toJavaList(Long.class);
            }

            if (CollectionUtils.isEmpty(attrIdList) && CollectionUtils.isEmpty(relIdList)) {
                throw new RuntimeException("模型属性不能为空");
            }

            CiVo ciVo = ciService.getCiById(ciId);
            if (ciVo == null) {
                throw new CiNotFoundException(ciId);
            }

            wb = new HSSFWorkbook();
            HSSFSheet sheet = wb.createSheet("data");
            String fileName = ciVo.getId() + "_" + ciVo.getLabel() + ".xls";

            int ciTypeAttrListSize = ciVo.getAttrList().size() + ciVo.getRelList().size() + 1;
            // 设置excel每列宽度
            for (int i = 0; i < ciTypeAttrListSize; i++) {
                sheet.setColumnWidth(i, 5000);
            }

            // 创建字体样式
            HSSFFont font = wb.createFont();
            font.setFontName("宋体");
            font.setFontHeightInPoints((short) 10);
            font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());

            // 创建单元格样式
            HSSFPalette palette = wb.getCustomPalette();
            palette.setColorAtIndex((short) 8, (byte) (0xff & 2), (byte) (0xff & 159), (byte) (0xff & 228));
            HSSFCellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setFillForegroundColor((short) 8);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 设置边框
            style.setBottomBorderColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setFont(font);// 设置字体

            // 创建Excel的sheet的一行
            int i = 0;
            HSSFRow row = sheet.createRow(0);
            row.setHeight((short) 300);// 设定行的高度
            HSSFCell idCell = row.createCell(i);
            idCell.setCellStyle(style);
            idCell.setCellValue("id");
            i++;
            idCell = row.createCell(i);
            idCell.setCellStyle(style);
            idCell.setCellValue("uuid");
            i++;
            /* 属性 */
            if (CollectionUtils.isNotEmpty(ciVo.getAttrList()) && CollectionUtils.isNotEmpty(attrIdList)) {
                int validationSheetIndex = 1;
                for (AttrVo attr : ciVo.getAttrList()) {
                    if (attrIdList.contains(attr.getId())) {
                        String label = attr.getLabel();
                        Integer isRequired = attr.getIsRequired();
                        Integer isUnique = attr.getIsUnique();
                        StringBuilder ciTypeAttrNameDesc = new StringBuilder();
                        if (isUnique.equals(1)) {
                            ciTypeAttrNameDesc.append("唯一");
                        }
                        if (isRequired.equals(1)) {
                            if (ciTypeAttrNameDesc.length() > 0) {
                                ciTypeAttrNameDesc.append("|");
                            }
                            ciTypeAttrNameDesc.append("必填");
                        }

                        if (ciTypeAttrNameDesc.length() > 0) {
                            label = label + "[(" + ciTypeAttrNameDesc + ")]";
                        }
                        // 如果是下拉框，则设置数据有效性
                        if (PropHandlerType.SELECT.getValue().equals(attr.getType())) {
                            List<Long> idList = ciEntityMapper.getCiEntityIdByCiId(attr.getTargetCiId());
                            if (CollectionUtils.isNotEmpty(idList)) {
                                List<CiEntityVo> entityVoList = ciEntityMapper.getCiEntityBaseInfoByIdList(idList);
                                if (CollectionUtils.isNotEmpty(entityVoList)) {
                                    List<String> collect = entityVoList.stream().map(CiEntityVo::getName).collect(Collectors.toList());
                                    String[] array = new String[collect.size()];
                                    collect.toArray(array);
                                    addValidationData(wb, sheet, attr.getName(), validationSheetIndex, array, row.getRowNum() + 1, 99999, i, i);
                                    //CellRangeAddressList addressList = new CellRangeAddressList(row.getRowNum() + 1, 99999, i, i);
                                    //DVConstraint dvConstraint = DVConstraint.createExplicitListConstraint(array);
                                    //DataValidation validation = new HSSFDataValidation(addressList, dvConstraint);
                                    //validation.setSuppressDropDownArrow(false);// false时显示下拉箭头(office2007)
                                    //validation.setShowErrorBox(true);
                                    //sheet.addValidationData(validation);
                                    validationSheetIndex++;
                                }
                            }
                        }
                        HSSFCell cell = row.createCell(i);
                        cell.setCellStyle(style);
                        cell.setCellValue(label);
                        i++;
                    }
                }
            }
            /* 关系 */
            if (CollectionUtils.isNotEmpty(ciVo.getRelList()) && CollectionUtils.isNotEmpty(relIdList)) {
                for (RelVo rel : ciVo.getRelList()) {
                    if (relIdList.contains(rel.getId())) {
                        if (rel.getFromCiId().equals(ciVo.getId())) { //当前CI处于from
                            String label = rel.getToLabel();
                            if (rel.getToIsRequired().equals(1)) {
                                label = label + "[(必填)]";
                            }
                            HSSFCell cell = row.createCell(i);
                            cell.setCellStyle(style);
                            cell.setCellValue(label);
                            i++;
                        } else if (rel.getToCiId().equals(ciVo.getId())) {//当前CI处于to
                            String label = rel.getFromLabel();
                            if (rel.getFromIsRequired().equals(1)) {
                                label = label + "[(必填)]";
                            }
                            HSSFCell cell = row.createCell(i);
                            cell.setCellStyle(style);
                            cell.setCellValue(label);
                            i++;
                        }
                    }
                }
            }

            boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
                fileName = URLEncoder.encode(fileName, "UTF-8");// IE浏览器
            } else {
                fileName = new String(fileName.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
            }
            response.setContentType("application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", " attachment; filename=\"" + fileName + "\"");
            os = response.getOutputStream();
            wb.write(os);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            if (os != null) {
                os.flush();
                os.close();
            }
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
