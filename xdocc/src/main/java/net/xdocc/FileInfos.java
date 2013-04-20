package net.xdocc;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;

public class FileInfos implements Serializable {

	private static final long serialVersionUID = -3443406953689397220L;
	private final File target;
	private final long targetTimestamp;
	private final long targetSize;
	private final long sourceTimestamp;
	private final long sourceSize;

	public FileInfos(Path target, long targetTimestamp, long targetSize,
			long sourceTimestamp, long sourceSize) {
		this.target = target.toFile();
		this.targetTimestamp = targetTimestamp;
		this.targetSize = targetSize;
		this.sourceTimestamp = sourceTimestamp;
		this.sourceSize = sourceSize;
	}

	public long getTargetSize() {
		return targetSize;
	}

	public long getSourceSize() {
		return sourceSize;
	}

	public File getTarget() {
		return target;
	}

	public long getTargetTimestamp() {
		return targetTimestamp;
	}

	public long getSourceTimestamp() {
		return sourceTimestamp;
	}

	public boolean isTargetDirty(Path target, long targetTimestamp,
			long targetSize) {
		return this.targetTimestamp != targetTimestamp
				|| this.targetSize != targetSize
				|| !this.target.equals(target.toFile());
	}

	public boolean isSourceDirty(long sourceTimestamp, long sourceSize) {
		return this.sourceTimestamp != sourceTimestamp
				|| this.sourceSize != sourceSize;
	}

	@Override
	public int hashCode() {
		return target.hashCode();
	};

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof FileInfos)) {
			return false;
		}
		FileInfos f = (FileInfos) other;
		return f.target.equals(target);
	}

	public FileInfos copy(long sourceTimestamp, long sourceSize) {
		
		return new FileInfos(target.toPath(), targetTimestamp, targetSize, sourceTimestamp, sourceSize);
	}

}
