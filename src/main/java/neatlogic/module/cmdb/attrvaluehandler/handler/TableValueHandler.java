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

package neatlogic.module.cmdb.attrvaluehandler.handler;

import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.attr.AttrValueIrregularException;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class TableValueHandler implements IAttrValueHandler {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Override
    public String getType() {
        return "table";
    }

    @Override
    public String getName() {
        return "表格";
    }

    @Override
    public boolean isCanSort() {
        return false;
    }

    @Override
    public String getIcon() {
        return "tsfont-chart-table";
    }

    @Override
    public boolean isCanSearch() {
        return true;
    }

    @Override
    public boolean isCanInput() {
        return true;
    }

    @Override
    public boolean isCanImport() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public boolean isNeedTargetCi() {
        return true;
    }

    @Override
    public boolean isNeedConfig() {
        return true;
    }

    @Override
    public boolean isNeedWholeRow() {
        return true;
    }

    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.LI, SearchExpression.NL, SearchExpression.NOTNULL,
                SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 12;
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONArray returnList = new JSONArray();
        if (CollectionUtils.isNotEmpty(valueList)) {
            List<Long> ciEntityIdList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    ciEntityIdList.add(valueList.getLong(i));
                } catch (Exception ignored) {

                }
            }
            if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
                List<CiEntityVo> ciEntityList = ciEntityService.getCiEntityByIdList(attrVo.getTargetCiId(), ciEntityIdList);
                returnList.addAll(ciEntityList);
            }
        }
        return returnList;
    }

    @Override
    public void transferValueListToExport(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList) && attrVo.getTargetCiId() != null) {
            List<Long> ciEntityIdList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    ciEntityIdList.add(valueList.getLong(i));
                } catch (Exception ignored) {

                }
            }
            CiVo ciVo = ciMapper.getCiById(attrVo.getTargetCiId());
            List<CiEntityVo> ciEntityList;
            if (ciVo.getIsVirtual().equals(0)) {
                ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(ciEntityIdList);
            } else {
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setCiId(ciVo.getId());
                ciEntityVo.setIdList(ciEntityIdList);
                ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByIdList(ciEntityVo);
            }
            valueList.clear();
            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                for (CiEntityVo ciEntityVo : ciEntityList) {
                    valueList.add(ciEntityVo.getName());
                }
            }
        }
    }

    @Override
    public boolean valid(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            CiVo ciVo = ciMapper.getCiById(attrVo.getTargetCiId());
            for (int i = 0; i < valueList.size(); i++) {
                String value = valueList.getString(i);
                try {
                    Long id = Long.valueOf(value);
                    CiEntityVo ciEntity = null;
                    if (ciVo.getIsVirtual().equals(0)) {
                        ciEntity = ciEntityMapper.getCiEntityBaseInfoById(id);
                    } else {
                        CiEntityVo ciEntityVo = new CiEntityVo();
                        ciEntityVo.setCiId(ciVo.getId());
                        List<Long> idList = new ArrayList<>();
                        idList.add(id);
                        ciEntityVo.setIdList(idList);
                        List<CiEntityVo> ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByIdList(ciEntityVo);
                        if (CollectionUtils.isNotEmpty(ciEntityList)) {
                            ciEntity = ciEntityList.get(0);
                        }
                    }
                    if (ciEntity == null) {
                        throw new AttrValueIrregularException(attrVo, value);
                    }
                    if (!Objects.equals(ciEntity.getCiId(), attrVo.getTargetCiId())) {
                        throw new AttrValueIrregularException(attrVo, value);
                    }
                } catch (NumberFormatException ex) {
                    throw new AttrValueIrregularException(attrVo, value);
                }
            }
        }
        return true;
    }
}
