package net.xdocc;

import lombok.Data;

import java.io.Serializable;

@Data
public class SrcSet implements Serializable {
	private static final long serialVersionUID = -178753802538671256L;
	final private String src;
    final private String attribute;
}
