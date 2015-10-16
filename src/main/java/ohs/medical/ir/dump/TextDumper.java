package ohs.medical.ir.dump;

public abstract class TextDumper {

	protected String inputDirName;

	protected String outputFileName;

	public TextDumper(String inputDir, String outputFileName) {
		this.inputDirName = inputDir;
		this.outputFileName = outputFileName;
	}

	public abstract void dump() throws Exception;

}
