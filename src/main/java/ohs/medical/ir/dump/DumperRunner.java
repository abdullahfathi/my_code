package ohs.medical.ir.dump;

import java.io.IOException;

public class DumperRunner {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		{
			TrecCdsDumper dh = new TrecCdsDumper();
			dh.makeRawTextDump();
			dh.makeTextDump();
		}

		{
			ClefEHealthDumper dh = new ClefEHealthDumper();
			dh.makeTextDump();
		}

		{
			OhsumedDumper dh = new OhsumedDumper();
			dh.makeTextDump();
		}

		{
			TrecGenomicsDumper d = new TrecGenomicsDumper();
			d.makeTextDump();
		}
	}

}
