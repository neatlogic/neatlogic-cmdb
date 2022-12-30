/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.customview;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.crossover.ICustomViewCrossoverService;
import codedriver.framework.cmdb.dto.customview.*;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.cmdb.exception.customview.CreateCustomViewFailedException;
import codedriver.framework.cmdb.exception.customview.CustomViewAttrNameIsExistsException;
import codedriver.framework.cmdb.exception.customview.DeleteCustomViewFailedException;
import codedriver.framework.transaction.core.EscapeTransactionJob;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.dao.mapper.tag.CmdbTagMapper;
import codedriver.module.cmdb.utils.CustomViewBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CustomViewServiceImpl implements CustomViewService, ICustomViewCrossoverService {
    @Resource
    private CustomViewMapper customViewMapper;

    @Resource
    private CmdbTagMapper cmdbTagMapper;


    @Override
    public List<CustomViewAttrVo> getCustomViewAttrByCustomViewId(CustomViewAttrVo customViewAttrVo) {
        return customViewMapper.getCustomViewAttrByCustomViewId(customViewAttrVo);
    }

    @Override
    public List<CustomViewConstAttrVo> getCustomViewConstAttrByCustomViewId(CustomViewConstAttrVo customViewConstAttrVo) {
        return customViewMapper.getCustomViewConstAttrByCustomViewId(customViewConstAttrVo);
    }


    @Override
    public void updateCustomViewActive(CustomViewVo customViewVo) {
        customViewMapper.updateCustomViewActive(customViewVo);
    }

    @Override
    public CustomViewVo getCustomViewById(Long id) {
        return customViewMapper.getCustomViewById(id);
    }

    @Override
    public CustomViewVo getCustomViewDetailById(Long id) {
        CustomViewVo customViewVo = customViewMapper.getCustomViewById(id);
        customViewVo.setLinkList(customViewMapper.getCustomViewLinkByCustomViewId(id));
        customViewVo.setCiList(customViewMapper.getCustomViewCiByCustomViewId(id));
        return customViewVo;
    }


    @Override
    public List<CustomViewVo> searchCustomView(CustomViewVo customViewVo) {
        List<CustomViewVo> customViewList = customViewMapper.searchCustomView(customViewVo);
        if (CollectionUtils.isNotEmpty(customViewList)) {
            int rowNum = customViewMapper.searchCustomViewCount(customViewVo);
            customViewVo.setRowNum(rowNum);
        }
        return customViewList;
    }

    @Override
    @Transactional
    public void insertCustomView(CustomViewVo customViewVo) {
        customViewMapper.insertCustomView(customViewVo);
        saveCustomView(customViewVo);
    }

    @Override
    @Transactional
    public void updateCustomView(CustomViewVo customViewVo) {
        customViewMapper.updateCustomView(customViewVo);
        customViewMapper.deleteCustomViewCiByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewAttrByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewConstAttrByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewRelByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewLinkByCustomViewId(customViewVo.getId());
        customViewMapper.deleteCustomViewTagByCustomViewId(customViewVo.getId());
        saveCustomView(customViewVo);
    }

    private void saveCustomView(CustomViewVo customViewVo) {
        if (CollectionUtils.isNotEmpty(customViewVo.getCiList())) {
            for (CustomViewCiVo customViewCiVo : customViewVo.getCiList()) {
                customViewCiVo.setCustomViewId(customViewVo.getId());
                customViewMapper.insertCustomViewCi(customViewCiVo);
                if (CollectionUtils.isNotEmpty(customViewCiVo.getAttrList())) {
                    for (CustomViewAttrVo customViewAttrVo : customViewCiVo.getAttrList()) {
                        if (StringUtils.isNotBlank(customViewAttrVo.getName()) && customViewMapper.checkCustomViewAttrIsExists(customViewAttrVo) > 0) {
                            throw new CustomViewAttrNameIsExistsException(customViewAttrVo);
                        }
                        customViewAttrVo.setCustomViewId(customViewVo.getId());
                        customViewAttrVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                        customViewMapper.insertCustomViewAttr(customViewAttrVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(customViewCiVo.getRelList())) {
                    for (CustomViewRelVo customViewRelVo : customViewCiVo.getRelList()) {
                        customViewRelVo.setCustomViewId(customViewVo.getId());
                        customViewRelVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                        customViewMapper.insertCustomViewRel(customViewRelVo);
                    }
                }
                if (CollectionUtils.isNotEmpty(customViewCiVo.getConstAttrList())) {
                    for (CustomViewConstAttrVo customViewConstAttrVo : customViewCiVo.getConstAttrList()) {
                        customViewConstAttrVo.setCustomViewId(customViewVo.getId());
                        customViewConstAttrVo.setCustomViewCiUuid(customViewCiVo.getUuid());
                        customViewMapper.insertCustomViewConstAttr(customViewConstAttrVo);
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(customViewVo.getLinkList())) {
            for (CustomViewLinkVo customViewLinkVo : customViewVo.getLinkList()) {
                customViewLinkVo.setCustomViewId(customViewVo.getId());
                customViewMapper.insertCustomViewLink(customViewLinkVo);
            }
        }

        if (CollectionUtils.isNotEmpty(customViewVo.getTagList())) {
            for (TagVo tagVo : customViewVo.getTagList()) {
                cmdbTagMapper.insertCmdbTag(tagVo);
                customViewMapper.insertCustomViewTag(customViewVo.getId(), tagVo.getId());
            }
        }

        EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
            CustomViewBuilder builder = new CustomViewBuilder(customViewVo);
            builder.buildView();
        }).execute();
        if (!s.isSucceed()) {
            throw new CreateCustomViewFailedException(s.getError());
        }
    }

    public void buildCustomView(String sql) {
        customViewMapper.buildCustomView(sql);
    }

    @Override
    @Transactional
    public void deleteCustomView(Long id) {
        customViewMapper.deleteCustomViewCiByCustomViewId(id);
        customViewMapper.deleteCustomViewAttrByCustomViewId(id);
        customViewMapper.deleteCustomViewRelByCustomViewId(id);
        customViewMapper.deleteCustomViewLinkByCustomViewId(id);
        customViewMapper.deleteCustomViewTagByCustomViewId(id);
        customViewMapper.deleteCiCustomViewByCustomViewId(id);
        customViewMapper.deleteCustomViewById(id);
        EscapeTransactionJob.State s = new EscapeTransactionJob(() -> {
            customViewMapper.dropCustomView(TenantContext.get().getDataDbName() + ".`customview_" + id + "`");
        }).execute();
        if (!s.isSucceed()) {
            throw new DeleteCustomViewFailedException(s.getError());
        }
    }


}
