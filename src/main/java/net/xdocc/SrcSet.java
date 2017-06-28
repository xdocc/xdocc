package net.xdocc;

import lombok.Data;
import lombok.experimental.Wither;

import java.io.Serializable;

@Data
public class SrcSet implements Serializable {
    final private String src;
    final private String attribute;
}
