package codedriver.module.cmdb.enums;

public enum BatchImportStatus {
	RUNNING("running", "执行中"), SUCCEED("succeed", "已成功"), FAILED("failed", "已失败"), STOPPED("stopped", "已停止");
	private final String type;
	private final String text;

	private BatchImportStatus(String _type, String _text) {
		this.type = _type;
		this.text = _text;
	}

	public String getValue() {
		return type;
	}

	public String getText() {
		return text;
	}

	public static String getValue(String name) {
		for (BatchImportStatus s : BatchImportStatus.values()) {
			if (s.getValue().equals(name)) {
				return s.getValue();
			}
		}
		return null;
	}

	public static String getText(String name) {
		for (BatchImportStatus s : BatchImportStatus.values()) {
			if (s.getValue().equals(name)) {
				return s.getText();
			}
		}
		return "";
	}
}
