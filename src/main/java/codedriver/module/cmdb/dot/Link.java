package codedriver.module.cmdb.dot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class Link {
    private String from;
    private String to;
    private Map<String, Object> propMap = new HashMap<>();

    private Link(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        if (StringUtils.isNotBlank(builder.tailLabel)) {
            propMap.put("taillabel", builder.tailLabel);
        }
        if (StringUtils.isNotBlank(builder.headLabel)) {
            propMap.put("headlabel", builder.headLabel);
        }
        if (StringUtils.isNotBlank(builder.label)) {
            propMap.put("label", builder.label);
        }
        if (builder.labelDistance != null) {
            propMap.put("labeldistance", builder.labelDistance);
        }
        if (StringUtils.isNotBlank(builder.fontcolor)) {
            propMap.put("fontcolor", builder.fontcolor);
        }
        if (builder.fontsize != null) {
            propMap.put("fontsize", builder.fontsize);
        }
    }

    public String toString() {
        Iterator<String> itKey = propMap.keySet().iterator();
        String propString = "";
        while (itKey.hasNext()) {
            String key = itKey.next();
            if (StringUtils.isNotBlank(propString)) {
                propString += ",";
            }
            propString += key + "=\"" + propMap.get(key) + "\"";
        }
        String str = "\"" + this.from + "\"->\"" + this.to + "\"";
        if (StringUtils.isNotBlank(propString)) {
            str += "[" + propString + ",labelangle=\"180\"]";
        }
        return str;
    }

    public static class Builder {
        private String from;
        private String to;
        private String tailLabel;
        private String headLabel;
        private String label;
        private Integer labelDistance;
        private Double fontsize;
        private String fontcolor;

        public Builder(String _from, String _to) {
            this.from = _from;
            this.to = _to;
        }

        public Builder withHeadLabel(String _headLabel) {
            this.headLabel = _headLabel;
            return this;
        }

        public Builder withTailLabel(String _tailLabel) {
            this.tailLabel = _tailLabel;
            return this;
        }

        public Builder withLabel(String _label) {
            this.label = _label;
            return this;
        }

        public Builder withLabelDistance(int _labelDistance) {
            this.labelDistance = _labelDistance;
            return this;
        }

        public Builder setFontSize(double _fontsize) {
            this.fontsize = _fontsize;
            return this;
        }

        public Builder setFontColor(String _fontcolor) {
            this.fontcolor = _fontcolor;
            return this;
        }

        public Link build() {
            return new Link(this);
        }
    }
}
