/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class test extends PrivateApiComponentBase {
    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getName() {
        return "配置项测试";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciEntityId = jsonObj.getLong("ciEntityId");
        /*Long ciId = jsonObj.getLong("ciId");
        CiVo ciVo = ciMapper.getCiById(ciId);
        List<CiVo> ciList = ciMapper.getUpwardCiListByLR(ciVo.getLft(), ciVo.getRht());
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiList(ciList);
        //List<Map<String, Object>> resultMap = ciEntityMapper.searchCiEntity(ciEntityVo);

        List<AttrFilterVo> attrFilterList = new ArrayList<>();
        AttrFilterVo attrFilterVo = new AttrFilterVo();
        attrFilterVo.setAttrId(1L);
        attrFilterVo.setExpression("=");
        attrFilterVo.setValueList(new ArrayList<String>() {{
            this.add("a");
        }});
        attrFilterList.add(attrFilterVo);*/
        // ciEntityMapper.searchCiEntityId(ciList, attrList, attrFilterList, null);
        //ciEntityMapper.searchCiEntity(ciList, attrList, null);
        return ciEntityService.getCiEntityById(ciEntityId);
    }


    @Override
    public String getToken() {
        return "/cmdb/test";
    }
}
