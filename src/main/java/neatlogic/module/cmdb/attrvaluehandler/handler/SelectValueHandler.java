/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.attrvaluehandler.handler;

import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


@Service
public class SelectValueHandler implements IAttrValueHandler {

    @Resource
    private CiEntityMapper ciEntityMapper;

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
                ciEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(ciEntityIdList);
            } else {
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setCiId(ciVo.getId());
                ciEntityVo.setIdList(ciEntityIdList);
                ciEntityList = ciEntityMapper.getVirtualCiEntityBaseInfoByIdList(ciEntityVo);
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
            List<CiEntityVo> ciEntityList = null;
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
    public SearchExpression[] getSupportExpression() {
        return new SearchExpression[]{SearchExpression.EQ,
                SearchExpression.NE, SearchExpression.LI, SearchExpression.NL, SearchExpression.NOTNULL, SearchExpression.NULL};
    }

    @Override
    public int getSort() {
        return 5;
    }
}
