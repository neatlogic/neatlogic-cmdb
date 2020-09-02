package codedriver.module.cmdb.attrvaluehandler.handler;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityContentMapper;

/**
 * @Author:chenqiwei
 * @Time:Sep 1, 2020
 * @ClassName: HashValueHandler
 * @Description: 返回超长文本内容
 */
@Service
public class HashValueHandler implements IAttrValueHandler {

    @Autowired
    private AttrEntityContentMapper attrEntityContentMapper;

    @Override
    public String getProtocol() {
        return "hash";
    }

    @Override
    public String getTransferedValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            String hash = DigestUtils.md5DigestAsHex(value.getBytes());
            if (attrEntityContentMapper.checkAttrEntityHashIsExists(hash) <= 0) {
                attrEntityContentMapper.insertAttrEntityContent(hash, value);
            }
            return "{hash}" + hash;
        }
        return "";
    }

    @Override
    public String getActualValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            return attrEntityContentMapper.getAttrEntityContentByHash(value);
        }
        return "";
    }

}
