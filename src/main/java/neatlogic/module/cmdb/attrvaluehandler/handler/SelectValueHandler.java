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

package neatlogic.module.cmdb.attrvaluehandler.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.exception.attr.AttrValueIrregularException;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityCachedMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class SelectValueHandler implements IAttrValueHandler {
    private static final Logger logger = LoggerFactory.getLogger(SelectValueHandler.class);

    @Resource
    private CiEntityCachedMapper ciEntityCachedMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getType() {
        return "select";
    }

    @Override
    public String getName() {
        return "下拉框";
    }

    @Override
    public String getIcon() {
        return "tsfont-list";
    }

    @Override
    public boolean isCanSort() {
        return false;
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
        return true;
    }

    @Override
    public boolean isSimple() {
        return true;
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
    public boolean isNameAttr() {
        return true;
    }

    @Override
    public boolean isUniqueAttr() {
        return true;
    }

    @Override
    public boolean isNeedWholeRow() {
        return false;
    }

    @Override
    public JSONArray getActualValueList(AttrVo attrVo, JSONArray valueList) {
        JSONArray actualValueList = new JSONArray();
        if (attrVo.getTargetCiId() != null && CollectionUtils.isNotEmpty(valueList)) {
            List<Long> ciEntityIdList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    ciEntityIdList.add(valueList.getLong(i));
                } catch (Exception ignored) {
                }
            }
            CiVo ciVo = ciMapper.getCiById(attrVo.getTargetCiId());
            List<CiEntityVo> ciEntityList = null;
            if (ciVo.getIsVirtual().equals(0)) {
                if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
                    ciEntityList = ciEntityCachedMapper.getCiEntityBaseInfoByIdList(ciEntityIdList);
                }
            } else {
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setCiId(ciVo.getId());
                ciEntityVo.setIdList(ciEntityIdList);
                ciEntityList = ciEntityCachedMapper.getVirtualCiEntityBaseInfoByIdList(ciEntityVo);
            }
            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                for (CiEntityVo ciEntityVo : ciEntityList) {
                    actualValueList.add(ciEntityVo.getName());
                }
            }
        }
        return actualValueList;
    }

    @Override
    public JSONArray transferValueListToExport(AttrVo attrVo, JSONArray valueList) {
        long time = 0L;
        if (logger.isInfoEnabled()) {
            time = System.currentTimeMillis();
        }
        JSONArray returnValueList = new JSONArray();
        if (CollectionUtils.isNotEmpty(valueList) && attrVo.getTargetCiId() != null) {
            List<Long> ciEntityIdList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    ciEntityIdList.add(valueList.getLong(i));
                } catch (Exception ignored) {

                }
            }
            CiVo ciVo = ciMapper.getCiById(attrVo.getTargetCiId());
            List<CiEntityVo> ciEntityList = null;
            if (ciVo.getIsVirtual().equals(0)) {
                ciEntityList = ciEntityCachedMapper.getCiEntityBaseInfoByIdList(ciEntityIdList);
            } else {
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setCiId(ciVo.getId());
                ciEntityVo.setIdList(ciEntityIdList);
                ciEntityList = ciEntityCachedMapper.getVirtualCiEntityBaseInfoByIdList(ciEntityVo);
            }
            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                for (CiEntityVo ciEntityVo : ciEntityList) {
                    returnValueList.add(ciEntityVo.getName());
                }
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("获取属性{}导出值耗时{}ms", attrVo.getName(), System.currentTimeMillis() - time);
        }
        return returnValueList;
    }


    @Override
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.EQ,
                SearchExpression.NE, SearchExpression.LI, SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 6;
    }

    @Override
    public boolean valid(AttrVo attrVo, JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            CiVo ciVo = ciMapper.getCiById(attrVo.getTargetCiId());
            List<CiVo> childCiList = null;
            for (int i = 0; i < valueList.size(); i++) {
                if (valueList.get(i) instanceof JSONObject) {
                    JSONObject valueObj = valueList.getJSONObject(i);
                    if (!valueObj.containsKey("uuid") && !valueObj.containsKey("id")) {
                        throw new AttrValueIrregularException(attrVo, valueObj.toString());
                    }
                } else {
                    String value = valueList.getString(i);
                    try {
                        Long id = Long.valueOf(value);

                        CiEntityVo ciEntity = null;
                        if (ciVo.getIsVirtual().equals(0)) {
                            ciEntity = ciEntityCachedMapper.getCiEntityBaseInfoById(id);
                        } else {
                            CiEntityVo ciEntityVo = new CiEntityVo();
                            ciEntityVo.setCiId(ciVo.getId());
                            ciEntityVo.setIdList(new ArrayList<Long>() {{
                                this.add(id);
                            }});
                            List<CiEntityVo> ciEntityList = ciEntityCachedMapper.getVirtualCiEntityBaseInfoByIdList(ciEntityVo);
                            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                                ciEntity = ciEntityList.get(0);
                            }
                        }
                        if (ciEntity == null) {
                            throw new AttrValueIrregularException(attrVo, value);
                        }
                        if (!Objects.equals(ciEntity.getCiId(), attrVo.getTargetCiId())) {
                            if (ciVo.getIsAbstract().equals(0)) {
                                throw new AttrValueIrregularException(attrVo, value);
                            } else {
                                //如果目标模型是抽象模型，可能会出现目标配置项的模型id和目标模型id不一致的问题。
                                if (childCiList == null) {
                                    childCiList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                                }
                                if (childCiList.stream().noneMatch(d -> d.getId().equals(attrVo.getTargetCiId()))) {
                                    throw new AttrValueIrregularException(attrVo, value);
                                }
                            }

                        }
                    } catch (NumberFormatException ex) {
                        throw new AttrValueIrregularException(attrVo, value);
                    }
                }
            }
        }
        return true;
    }
}
