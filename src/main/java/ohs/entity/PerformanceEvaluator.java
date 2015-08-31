package ohs.entity;

import java.util.ArrayList;
import java.util.List;

import ohs.io.IOUtils;
import ohs.io.TextFileReader;
import ohs.io.TextFileWriter;
import ohs.ir.eval.Metrics;
import ohs.utils.StrUtils;

public class PerformanceEvaluator {

	public static void main(String[] args) {
		System.out.println("process begins.");
		evaluate();
		System.out.println("process ends.");
	}

	public static void evaluate() {
		int[] num_candidates = { 1, 5, 10 };

		StringBuffer sb = new StringBuffer();
		sb.append("Top-K\tAnswers\tPredictions\tCorrect\tPrecision\tRecall\tF1");

		for (int i = 0; i < num_candidates.length; i++) {
			int top_k = num_candidates[i];

			double num_predicts = 0;
			double num_answers = 0;
			double num_correct = 0;

			TextFileReader reader = new TextFileReader(ENTPath.ODK_TEST_DATA_LABELDED, IOUtils.EUC_KR);
			TextFileWriter writer = new TextFileWriter(ENTPath.DATA_DIR + "odk_test_data_labeled_compact.txt");

			while (reader.hasNext()) {
				List<String> lines = reader.getNextLines();

				String type = DataReader.split(lines.get(1))[0];
				String korInput = DataReader.split(lines.get(3))[1];
				String engInput = DataReader.split(lines.get(5))[1];

				String korAns = "";
				String engAns = "";
				String ansId = "";
				int predict = 0;
				int answer = 0;

				for (int j = 7; j < lines.size(); j++) {
					String[] parts = DataReader.split(lines.get(j));
					int no = Integer.parseInt(parts[0]);
					String id = parts[1];
					String korOutput = parts[2];
					String engOutput = parts[3];

					double score = Double.parseDouble(parts[4]);
					double human = 0;

					if (parts[6].length() > 0) {
						human = Double.parseDouble(parts[6]);
					}

					if (human > 0) {
						korAns = korOutput;
						engAns = engOutput;
						ansId = id;
						answer = no;
					}
				}

				// if (answer == 0) {
				// continue;
				// }

				// if (answer > 0 && predict == 0) {
				// System.out.println(StrUtils.join("\n", lines));
				// System.out.println();
				// }

				num_predicts++;

				if (predict > 0) {
					num_predicts++;
				}

				if (answer > 0) {
					num_answers++;
					if (answer <= top_k) {
						num_correct++;
					}
				}

				String[] outStrs = new String[] { korInput, engInput, ansId, korAns, engAns };

				for (int j = 0; j < outStrs.length; j++) {
					if (outStrs[j].length() == 0) {
						outStrs[j] = "null";
					}
				}

				String output = StrUtils.join("\t", outStrs);
				writer.write(output + "\n");

				// System.out.println(line);
			}
			reader.close();
			writer.close();

			double precision = num_correct / num_predicts;
			double recall = num_correct / num_answers;
			double f1 = Metrics.f1(precision, recall);

			sb.append(String.format("\n%d\t%d\t%d\t%d\t%f\t%f\t%f",

			top_k, (int) num_answers, (int) num_predicts, (int) num_correct, precision, recall, f1));
		}

		System.out.println(sb.toString());

	}

}
