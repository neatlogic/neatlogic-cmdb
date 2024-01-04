package neatlogic.module.cmdb.api.ci.process;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.globalattr.GlobalAttrVo;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiUniqueRuleAttrTypeIrregularException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.process.auth.PROCESS;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.globalattr.GlobalAttrMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = PROCESS.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetCiForProcessMappingApi extends PrivateApiComponentBase {

    @Autowired
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Resource
    private GlobalAttrMapper globalAttrMapper;

    @Autowired
    private CiViewMapper ciViewMapper;

    @Override
    public String getName() {
        return "nmcacp.getciforprocessmappingapi.getname";
    }


    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "term.cmdb.ciid"),
            @Param(name = "rootId", type = ApiParamType.LONG, desc = "term.cmdb.rootciid")
    })
    @Output({
            @Param(explode = CiVo.class)
    })
    @Description(desc = "nmcacp.getciforprocessmappingapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("id");
        CiVo ciVo = ciMapper.getCiById(ciId);
        if (ciVo == null) {
            throw new CiNotFoundException(ciId);
        }
        Long rootId = paramObj.getLong("rootId");
        if (Objects.equals(rootId, ciId)) {
            rootId = null;
        }
        List<Long> rootUpwardCiIdList = new ArrayList<>();
        if (rootId != null) {
            CiVo rootCiVo = ciMapper.getCiById(rootId);
            if (rootCiVo == null) {
                throw new CiNotFoundException(rootId);
            }
            List<CiVo> upwardCiList = ciMapper.getUpwardCiListByLR(rootCiVo.getLft(), rootCiVo.getRht());
            rootUpwardCiIdList = upwardCiList.stream().map(CiVo::getId).collect(Collectors.toList());
        }
        // 唯一规则属性列表
        List<Long> uniqueAttrIdList = ciMapper.getCiUniqueByCiId(ciId);
        ciVo.setUniqueAttrIdList(uniqueAttrIdList);
        // 属性
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        for (AttrVo attrVo : attrList) {
            if (uniqueAttrIdList.contains(attrVo.getId()) && Objects.equals(attrVo.getType(), "expression")) {
                attrVo.setCiLabel(ciVo.getLabel());
                attrVo.setCiName(ciVo.getName());
                throw new CiUniqueRuleAttrTypeIrregularException(attrVo);
            }
        }
        for (int i = attrList.size() - 1; i >= 0; i--) {
            AttrVo attrVo = attrList.get(i);
            if (attrVo.getAllowEdit() != null && attrVo.getAllowEdit().equals(0)) {
                attrList.remove(i);
                continue;
            }
            if (CollectionUtils.isNotEmpty(rootUpwardCiIdList)) {
                if (rootUpwardCiIdList.contains(attrVo.getCiId())) {
                    attrList.remove(i);
                }
            }
        }
        ciVo.setAttrList(attrList);
        // 关系
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        for (int i = relList.size() - 1; i >= 0; i--) {
            RelVo relVo = relList.get(i);
            if (relVo.getAllowEdit() != null && relVo.getAllowEdit().equals(0)) {
                relList.remove(i);
                continue;
            }
            if (CollectionUtils.isNotEmpty(rootUpwardCiIdList)) {
                if (Objects.equals(relVo.getDirection(), "from") && rootUpwardCiIdList.contains(relVo.getFromCiId())) {
                    relList.remove(i);
                } else if (Objects.equals(relVo.getDirection(), "to") && rootUpwardCiIdList.contains(relVo.getToCiId())) {
                    relList.remove(i);
                }
            }
        }
        ciVo.setRelList(relList);
        if (CollectionUtils.isEmpty(rootUpwardCiIdList)) {
            // 全局属性
            GlobalAttrVo globalAttrVo = new GlobalAttrVo();
            globalAttrVo.setIsActive(1);
            List<GlobalAttrVo> globalAttrList = globalAttrMapper.searchGlobalAttr(globalAttrVo);
            ciVo.setGlobalAttrList(globalAttrList);
        }
        // 字段显示设置列表
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciId);
        List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
        ciVo.setViewList(ciViewList);
        return ciVo;
    }

    @Override
    public String getToken() {
        return "cmdb/ci/get/forprocessmapping";
    }
}
